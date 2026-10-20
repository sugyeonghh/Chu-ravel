// 사용법: cd tools/map && npm run gen:globe
// 입력: world-atlas 110m (Natural Earth 1:110m 나라 경계). "세계지도 펼치기" 지구본 연출에만 쓰므로 거친 지도로 충분하다.
// 출력: ../../frontend/public/data/globe.json  {countries:[{id(ISO alpha-2 또는 null), g(GeoJSON geometry, 경위도 소수 1자리)}]}
// world.json 은 이미 평면으로 투영한 SVG path 라 지구본(정사영)으로 돌릴 수 없어서 경위도를 따로 둔다.
import path_ from "node:path";
import fs from "node:fs";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import * as tc from "topojson-client";
const require = createRequire(import.meta.url);
const __dirname = path_.dirname(fileURLToPath(import.meta.url));
const OUT = path_.join(__dirname, "../../frontend/public/data/globe.json");
const topo = require("world-atlas/countries-110m.json");
const iso = require("i18n-iso-countries");
const EXTRA_CODE = { Kosovo: "XK" };

const r1 = (v) => Math.round(v * 10) / 10;
const round = (c) => (typeof c[0] === "number" ? [r1(c[0]), r1(c[1])] : c.map(round));

const countries = tc.feature(topo, topo.objects.countries).features
  .map((f) => ({ id: iso.numericToAlpha2(f.id) || EXTRA_CODE[f.properties.name] || null, g: { type: f.geometry.type, coordinates: round(f.geometry.coordinates) } }))
  .filter((c) => c.id !== "AQ");

fs.writeFileSync(OUT, JSON.stringify({ countries }));
console.log("globe", countries.length, (fs.statSync(OUT).size / 1024).toFixed(0) + "KB");
