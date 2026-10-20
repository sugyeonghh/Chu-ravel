// 사용법: cd tools/map && npm install && npm run gen:countries
// 입력: cache/ne_10m_admin_1_states_provinces.geojson (Natural Earth 1:10m 1단계 행정구역, 퍼블릭 도메인)
//       없으면 nvkelso/natural-earth-vector 에서 받는다 (약 40MB, git 에는 안 올린다)
// 출력: ../../frontend/public/data/countries/{나라코드}.json  나라별 지역 지도 {w, h, regions:[{id,n,p,d,b,c}], provs:[{d}]}
//       ../../frontend/public/data/countries/index.json       {나라코드: 지역 수} — 세계지도가 눌러 들어갈 수 있는 나라
//       ../../src/main/resources/subdivisions.csv             code,name,country — 서버 검증용
// 한국(KR)은 시·군·구 지도(gen.js)를 쓰므로 여기서 뺀다. 지역이 1개뿐인 나라는 나라 단위 체크인으로 둔다.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { execFileSync } from "node:child_process";
import * as topo from "topojson-server";
import * as tc from "topojson-client";
import * as ts from "topojson-simplify";
import * as d3 from "d3-geo";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const SRC = path.join(__dirname, "cache/ne_10m_admin_1_states_provinces.geojson");
const SRC_URL = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_10m_admin_1_states_provinces.geojson";
const OUT_DIR = path.join(__dirname, "../../frontend/public/data/countries");
const OUT_RES = path.join(__dirname, "../../src/main/resources");

const W = 760, H = 1000, PAD = 10;  // 한국 지도와 같은 크기 안에 맞춘다
const MAX_POINTS = 10000;           // 나라 하나에 남길 점 수 (파일 크기)
const DEG = Math.PI / 180;

if (!fs.existsSync(SRC)) {
  fs.mkdirSync(path.dirname(SRC), { recursive: true });
  console.log("downloading", SRC_URL);
  execFileSync("curl", ["-sSL", "-o", SRC, SRC_URL], { stdio: "inherit" });
}
const world = JSON.parse(fs.readFileSync(path.join(__dirname, "../../frontend/public/data/world.json"), "utf8"));
const countryName = Object.fromEntries(world.countries.map((c) => [c.id, c.n]));
const src = JSON.parse(fs.readFileSync(SRC, "utf8"));

const byCountry = new Map();
for (const f of src.features) {
  const cc = f.properties.iso_a2;
  if (!/^[A-Z]{2}$/.test(cc) || cc === "KR" || !countryName[cc]) continue;
  if (!byCountry.has(cc)) byCountry.set(cc, []);
  byCountry.get(cc).push(f);
}

const polys = (g) => (g.type === "Polygon" ? [g.coordinates] : g.type === "MultiPolygon" ? g.coordinates : []);
const polyFeature = (p) => ({ type: "Feature", geometry: { type: "Polygon", coordinates: p } });

/** 본토에서 너무 먼 조각(해외 영토, 먼 섬)은 뺀다. 거리 한도는 본토 크기에 비례 (러시아 칼리닌그라드는 남는다) */
function trimFarParts(features) {
  // 지역을 모두 합친 뒤 가장 큰 덩어리를 본토로 본다 (지역 하나만 보면 프랑스령 기아나가 본토로 잡힌다)
  const t = topo.topology({ r: { type: "FeatureCollection", features } });
  const merged = tc.merge(t, t.objects.r.geometries);
  const all = polys(merged).map((p) => polyFeature(p));
  const main = all.reduce((a, b) => (d3.geoArea(b) > d3.geoArea(a) ? b : a));
  const center = d3.geoCentroid(main);
  const mainRadius = Math.max(...main.geometry.coordinates[0].map((pt) => d3.geoDistance(center, pt)));
  const limit = Math.max(28 * DEG, mainRadius * 1.5);
  const kept = [];
  for (const f of features) {
    const ps = polys(f.geometry).filter((p) => d3.geoDistance(center, d3.geoCentroid(polyFeature(p))) <= limit);
    if (ps.length) kept.push({ ...f, geometry: { type: "MultiPolygon", coordinates: ps } });
  }
  return { kept, center };
}

function projectionFor(cc, fc, center) {
  if (cc === "US") return d3.geoAlbersUsa(); // 알래스카·하와이를 옆으로 옮겨 그린다
  const [[, lat0], [, lat1]] = d3.geoBounds(fc);
  const tall = lat1 - lat0 > 25 || Math.abs(center[1]) > 55;
  return tall
    ? d3.geoConicConformal().rotate([-center[0], 0]).parallels([lat0 + (lat1 - lat0) / 4, lat1 - (lat1 - lat0) / 4])
    : d3.geoMercator().rotate([-center[0], 0]);
}

/** 점이 MAX_POINTS 개쯤 남도록 줄인다 (경계를 공유하는 이웃 지역은 같이 줄어 틈이 안 생긴다) */
function simplified(features) {
  const t = topo.topology({ r: { type: "FeatureCollection", features } });
  const pre = ts.presimplify(t);
  const weights = pre.arcs.flatMap((a) => a.map((p) => p[2])).filter((w) => w > 0 && Number.isFinite(w)).sort((a, b) => b - a);
  const min = weights.length > MAX_POINTS ? weights[MAX_POINTS] : 0;
  const s = ts.simplify(pre, min);
  return { t: s, fc: tc.feature(s, s.objects.r) };
}

const r1 = (v) => +v.toFixed(1);
const cleanId = (s) => s.replace(/[^A-Za-z0-9-]/g, "").toUpperCase();
fs.mkdirSync(OUT_DIR, { recursive: true });
for (const f of fs.readdirSync(OUT_DIR)) fs.unlinkSync(path.join(OUT_DIR, f));

const index = {}, csv = [], seen = new Set();
let total = 0;
for (const [cc, feats] of [...byCountry].sort()) {
  if (feats.length < 2) continue;
  // 미국은 알래스카가 가장 큰 조각이라 본토 기준이 틀어진다. AlbersUsa 가 알래스카·하와이를 옮겨 주므로 자르지 않는다
  const { kept, center } = cc === "US" ? { kept: feats, center: null } : trimFarParts(feats);
  if (kept.length < 2) continue;
  const { t, fc } = simplified(kept);
  const proj = projectionFor(cc, fc, center).fitExtent([[PAD, PAD], [W - PAD, H - PAD]], fc);
  const pathGen = d3.geoPath(proj).digits(1);
  // 실제로 그려진 영역에 캔버스를 딱 맞춘다
  const [[x0, y0], [x1, y1]] = pathGen.bounds(fc);
  const [tx, ty] = proj.translate();
  proj.translate([tx - x0 + PAD, ty - y0 + PAD]);
  const w = Math.ceil(x1 - x0 + PAD * 2), h = Math.ceil(y1 - y0 + PAD * 2);

  const name = countryName[cc];
  const regions = [];
  for (const f of fc.features) {
    const d = pathGen(f);
    if (!d) continue;
    const id = cleanId(f.properties.adm1_code);
    if (seen.has(id)) throw new Error("duplicate id " + id);
    seen.add(id);
    const pr = f.properties;
    const n = (pr.name_ko || pr.name_en || pr.name || pr.woe_name || pr.gn_name || id).replace(/\s*,\s*/g, " ");
    regions.push({ id, n, p: name, d, b: pathGen.bounds(f).map((pt) => pt.map(r1)), c: pathGen.centroid(f).map(r1) });
    csv.push(`${id},${n},${cc}`);
  }
  if (regions.length < 2) continue;
  regions.sort((a, b) => a.n.localeCompare(b.n, "ko"));
  // 나라 바깥 경계선 (이웃 지역과 맞닿지 않은 선)
  const outline = pathGen(tc.mesh(t, t.objects.r, (a, b) => a === b));
  const file = path.join(OUT_DIR, cc + ".json");
  fs.writeFileSync(file, JSON.stringify({ w, h, regions, provs: [{ d: outline }] }));
  index[cc] = regions.length;
  total += fs.statSync(file).size;
}
fs.writeFileSync(path.join(OUT_DIR, "index.json"), JSON.stringify(index));
fs.writeFileSync(path.join(OUT_RES, "subdivisions.csv"), "code,name,country\n" + csv.join("\n") + "\n");
console.log("countries", Object.keys(index).length, "regions", csv.length, "total", (total / 1024 / 1024).toFixed(1) + "MB",
  "largest", Object.keys(index).map((c) => [c, fs.statSync(path.join(OUT_DIR, c + ".json")).size]).sort((a, b) => b[1] - a[1]).slice(0, 5).map(([c, s]) => c + ":" + (s / 1024).toFixed(0) + "KB").join(" "));
