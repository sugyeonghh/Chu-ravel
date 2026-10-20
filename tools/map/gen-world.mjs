// 사용법: cd tools/map && npm install && npm run gen:world (ESM: d3-geo 3 은 require 가 안 된다)
// 입력: world-atlas 50m (Natural Earth 1:50m 나라 경계, ISO 3166-1 숫자 코드) + i18n-iso-countries(한국어 이름)
// 출력: ../../frontend/public/data/world.json, ../../src/main/resources/countries.csv
// 나라 코드는 ISO 3166-1 alpha-2 (JP, US ...). 한국(KR)은 시·군·구로 체크인하므로 countries.csv 에서 뺀다.
import path_ from "node:path";
import fs from "node:fs";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import * as tc from "topojson-client";
import * as ts from "topojson-simplify";
import * as d3 from "d3-geo";
const require = createRequire(import.meta.url);
const __dirname = path_.dirname(fileURLToPath(import.meta.url));
const OUT_STATIC = path_.join(__dirname, "../../frontend/public/data");
const OUT_RES = path_.join(__dirname, "../../src/main/resources");
const topo = require("world-atlas/countries-50m.json");
const iso = require("i18n-iso-countries");
iso.registerLocale(require("i18n-iso-countries/langs/ko.json"));

// 이름이 낯설거나 코드가 없는 곳
const NAME = { TW: "대만", KR: "대한민국", KP: "북한", US: "미국", GB: "영국", RU: "러시아", VA: "바티칸", MK: "북마케도니아", CZ: "체코", SY: "시리아", LA: "라오스", IR: "이란", VE: "베네수엘라", BO: "볼리비아", TZ: "탄자니아", MD: "몰도바", FM: "미크로네시아", PS: "팔레스타인", CD: "콩고민주공화국", CG: "콩고", XK: "코소보", PM: "생피에르 미클롱", CY: "키프로스" };
const EXTRA_CODE = { Kosovo: "XK" };

const simple = ts.simplify(ts.presimplify(topo), 0.03);
const feats = tc.feature(simple, simple.objects.countries).features
  .map((f) => ({ ...f, code: iso.numericToAlpha2(f.id) || EXTRA_CODE[f.properties.name] || null }))
  .filter((f) => f.code !== "AQ"); // 남극은 뺀다

const W = 1000, H = 520;
const proj = d3.geoNaturalEarth1().fitExtent([[8, 8], [W - 8, H - 8]], { type: "FeatureCollection", features: feats });
const path = d3.geoPath(proj).digits(1);
const r1 = (v) => +v.toFixed(1);

// 같은 코드가 여러 조각이면(예: 프랑스령) 한 나라로 합친다
const byCode = new Map(), nocode = [];
for (const f of feats) {
  if (!f.code) { nocode.push(f); continue; }
  if (!byCode.has(f.code)) byCode.set(f.code, []);
  byCode.get(f.code).push(f);
}

const out = [];
for (const [code, fs_] of byCode) {
  const fc = { type: "FeatureCollection", features: fs_ };
  const d = fs_.map((f) => path(f)).join("");
  const b = path.bounds(fc), area = path.area(fc);
  // 가장 큰 조각의 중심을 라벨 위치로
  const main = fs_.reduce((a, f) => (path.area(f) > path.area(a) ? f : a));
  const c = path.centroid(main);
  out.push({ id: code, n: (NAME[code] || iso.getName(code, "ko") || fs_[0].properties.name).replace(/\s*,\s*/g, " "), d, b: b.map((p) => p.map(r1)), c: c.map(r1), s: area < 8 ? 1 : 0 });
}
out.sort((a, b) => a.n.localeCompare(b.n, "ko"));
// 코드 없는 땅(분쟁 지역 등)은 배경으로만 그린다
const land = nocode.map((f) => path(f)).join("");

fs.writeFileSync(path_.join(OUT_STATIC, "world.json"), JSON.stringify({ w: W, h: H, countries: out, land }));
const csv = out.filter((c) => c.id !== "KR").map((c) => `${c.id},${c.n}`);
fs.writeFileSync(path_.join(OUT_RES, "countries.csv"), "code,name\n" + csv.join("\n") + "\n");
console.log("countries", out.length, "small", out.filter((c) => c.s).length,
  "size", (fs.statSync(path_.join(OUT_STATIC, "world.json")).size / 1024).toFixed(0) + "KB");
