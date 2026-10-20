import { useEffect, useRef } from "react";
import { geoGraticule10, geoOrthographic, geoPath } from "d3-geo";
import { levelOf } from "./WorldMap.jsx";

// 여권 ↔ 세계지도 사이의 지구본 연출. globe.json (gen-globe.mjs): 경위도 나라 경계. 캔버스에 정사영으로 그린다
// (매 프레임 SVG 를 다시 만들면 느려서).
// - in  (세계지도 펼치기): 멀리서 돌며 다가와 한국에서 부드럽게 멈추고, 핀이 한 번 퍼진 뒤 onDone → 평면 세계지도로
// - out (여권으로 돌아오기): 한국을 보던 지구본이 돌아서며 멀어지고 onDone

const KOREA = [127.8, 36.4];
const SPIN_DEG = 300; // 멈추기 전에 도는 경도
const MS = { in: 1800, out: 1300 };
const HOLD_MS = 450; // in: 한국에서 멈춘 뒤 핀이 퍼지는 시간
// 처음과 끝이 모두 느린 곡선. 끝만 느린 곡선(ease-out)은 앞에서 거의 다 와 버려 "오다가 멈춘" 것처럼 보인다
const easeInOut = (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);

let globeData = null;
/** 여권·세계지도 화면에서 미리 받아 둔다 (누르자마자 돌게) */
export function loadGlobe() {
  if (!globeData) {
    globeData = fetch("/data/globe.json")
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(r.status))))
      .catch((e) => {
        globeData = null;
        throw e;
      });
  }
  return globeData;
}

/** counts: 나라코드 -> 체크인한 지역 수, wholeVisits: 나라 하나로 남긴 체크인 — 세계지도와 같은 색으로 칠한다 */
export default function GlobeIntro({ counts, wholeVisits, direction = "in", onDone }) {
  const canvas = useRef();
  const done = useRef(onDone);
  done.current = onDone;

  useEffect(() => {
    let raf = 0, cancelled = false;
    loadGlobe().then((data) => {
      if (cancelled) return;
      const cv = canvas.current;
      const size = cv.clientWidth, dpr = window.devicePixelRatio || 1;
      cv.width = cv.height = Math.round(size * dpr);
      const ctx = cv.getContext("2d");
      ctx.scale(dpr, dpr);

      const css = getComputedStyle(document.documentElement);
      const color = (name) => css.getPropertyValue(name).trim();
      const fills = data.countries.map((c) => {
        const n = c.id && counts.get(c.id);
        return n ? color("--h" + levelOf(n)) : c.id && wholeVisits.has(c.id) ? color("--h3") : color("--land");
      });
      const sea = color("--sea-line"), line = color("--land-line"), pin = color("--accent"), ink = color("--stamp-ink");
      const font = color("--f-display");

      const r = size / 2;
      const proj = geoOrthographic().translate([r, r]).clipAngle(90).precision(0.6);
      const path = geoPath(proj, ctx);
      const graticule = geoGraticule10();
      const sphere = { type: "Sphere" };
      const korea = data.countries.findIndex((c) => c.id === "KR");

      /** e: 0 = 멀리서 다른 쪽을 보는 지구본, 1 = 가까이서 한국을 보는 지구본. pulse: 0~1 핀이 퍼지는 물결 */
      const draw = (e, pulse) => {
        const k = 0.3 + 0.58 * e; // 빛 테두리까지 캔버스 안에 들어오게 0.88 까지
        proj.scale(r * k).rotate([-(KOREA[0] - SPIN_DEG * (1 - e)), -KOREA[1] * e + 8 * (1 - e)]);
        const R = r * k;
        ctx.clearRect(0, 0, size, size);

        // 대기 빛 (캔버스 밖으로 번지지 않게 그라디언트로) + 바다
        const glow = ctx.createRadialGradient(r, r, R * 0.96, r, r, R * 1.12);
        glow.addColorStop(0, "rgba(47,109,181,.28)");
        glow.addColorStop(1, "rgba(47,109,181,0)");
        ctx.beginPath(); ctx.arc(r, r, R * 1.12, 0, Math.PI * 2);
        ctx.fillStyle = glow; ctx.fill();
        ctx.beginPath(); path(sphere);
        ctx.fillStyle = sea; ctx.fill();

        ctx.beginPath(); path(graticule);
        ctx.strokeStyle = "rgba(255,255,255,.55)"; ctx.lineWidth = 0.6; ctx.stroke();

        ctx.lineWidth = 0.5; ctx.strokeStyle = line;
        data.countries.forEach((c, i) => {
          ctx.beginPath(); path(c.g);
          ctx.fillStyle = fills[i]; ctx.fill(); ctx.stroke();
        });

        // 둥글게 보이도록 빛(왼쪽 위)과 그림자(오른쪽 아래)
        const shade = ctx.createRadialGradient(r - R * 0.4, r - R * 0.45, R * 0.1, r, r, R);
        shade.addColorStop(0, "rgba(255,255,255,.35)");
        shade.addColorStop(0.55, "rgba(255,255,255,0)");
        shade.addColorStop(1, "rgba(29,52,97,.28)");
        ctx.beginPath(); path(sphere);
        ctx.fillStyle = shade; ctx.fill();
        ctx.strokeStyle = "rgba(29,52,97,.25)"; ctx.lineWidth = 1; ctx.stroke();

        // 한국: 거의 다 왔을 때 핀·이름이 떠오른다 (빛 위에 그려 또렷하게)
        if (korea >= 0 && e > 0.75) {
          const a = (e - 0.75) / 0.25;
          const [x, y] = proj(KOREA);
          ctx.save();
          ctx.globalAlpha = a;
          ctx.beginPath(); path(data.countries[korea].g);
          ctx.strokeStyle = ink; ctx.lineWidth = 1.4; ctx.stroke();
          ctx.beginPath(); ctx.arc(x, y, 10 + 12 * a, 0, Math.PI * 2);
          ctx.strokeStyle = pin; ctx.globalAlpha = a * 0.45; ctx.lineWidth = 3; ctx.stroke();
          if (pulse > 0) {
            ctx.beginPath(); ctx.arc(x, y, 22 + 34 * pulse, 0, Math.PI * 2);
            ctx.globalAlpha = 0.55 * (1 - pulse); ctx.lineWidth = 2; ctx.stroke();
          }
          ctx.globalAlpha = a;
          ctx.beginPath(); ctx.arc(x, y, 6, 0, Math.PI * 2);
          ctx.fillStyle = pin; ctx.strokeStyle = "#fff"; ctx.lineWidth = 2.5;
          ctx.fill(); ctx.stroke();
          ctx.font = `${Math.round(R * 0.07)}px ${font}`;
          ctx.textBaseline = "middle";
          ctx.lineJoin = "round"; ctx.lineWidth = 4; ctx.strokeStyle = "#fff"; ctx.fillStyle = ink;
          ctx.strokeText("대한민국", x + 14, y - 14);
          ctx.fillText("대한민국", x + 14, y - 14);
          ctx.restore();
        }
      };

      const spin = MS[direction];
      const total = direction === "in" ? spin + HOLD_MS : spin;
      const start = performance.now();
      const frame = (now) => {
        const t = now - start;
        const p = easeInOut(Math.min(1, Math.max(0, t / spin)));
        if (direction === "in") draw(p, t > spin ? Math.min(1, (t - spin) / HOLD_MS) : 0);
        else draw(1 - p, 0);
        if (t < total) raf = requestAnimationFrame(frame);
        else done.current();
      };
      raf = requestAnimationFrame(frame);
    }).catch(() => {
      if (!cancelled) done.current(); // 지구본을 못 받으면 연출 없이 넘어간다
    });
    return () => {
      cancelled = true;
      cancelAnimationFrame(raf);
    };
  }, []); // 한 번만 돈다 (색·방향은 시작할 때 것으로)

  return <canvas ref={canvas} className={"globe " + direction} aria-hidden="true" />;
}
