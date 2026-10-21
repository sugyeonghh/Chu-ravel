import { memo } from "react";
import usePanZoom from "./usePanZoom.js";
import { Stamp } from "./MapView.jsx";

// 세계지도: world.json (w, h, countries:[{id, n, d, b, c, s}], land). s=1 인 작은 나라는 점으로도 그린다.
// 지역 지도가 있는 나라(mapped)를 누르면 그 나라 지역 지도로 들어가고, 없는 나라는 나라 하나로 체크인한다.

const Countries = memo(function Countries({ countries, land }) {
  return (
    <g>
      <path d={land} className="ct-land" />
      {countries.map((c) => <path key={c.id} d={c.d} className="ct" data-id={c.id} />)}
      {countries.filter((c) => c.s).map((c) => <circle key={c.id} cx={c.c[0]} cy={c.c[1]} r={2.4} className="ct-dot" data-id={c.id} />)}
    </g>
  );
});

/** 체크인한 지역 수에 따라 진하기를 5단계로 */
const LEVELS = [1, 2, 5, 10, 20];
export const levelOf = (n) => LEVELS.filter((m) => n >= m).length;

/** 지역을 하나라도 체크인한 나라는 지역 수만큼 진하게 칠한다 */
const Stamped = memo(function Stamped({ counts, byId }) {
  return (
    <g>
      {[...counts].map(([id, n]) => {
        const c = byId[id];
        if (!c) return null;
        const cls = "on h" + levelOf(n);
        return c.s
          ? <circle key={id} cx={c.c[0]} cy={c.c[1]} r={2.6} className={"ct-dot " + cls} data-id={id} />
          : <path key={id} d={c.d} className={"ct " + cls} data-id={id} />;
      })}
    </g>
  );
});

function Legend() {
  return (
    <div className="legend" aria-label="체크인한 지역 수">
      {LEVELS.map((m, i) => (
        <span key={m}><i className={"h" + (i + 1)} />{i === LEVELS.length - 1 ? m + "+" : m === LEVELS[i + 1] - 1 ? m : `${m}–${LEVELS[i + 1] - 1}`}</span>
      ))}
      <span>곳</span>
    </div>
  );
}

/** 나라 하나로 남긴 체크인은 나라 모양으로 오린 사진 */
const Whole = memo(function Whole({ visits, byId }) {
  return (
    <g>
      {[...visits].map(([id, v]) => {
        const c = byId[id];
        if (!c) return null;
        return c.s
          ? <circle key={id} cx={c.c[0]} cy={c.c[1]} r={2.6} className="ct-dot on" data-id={id} />
          : <g key={id} className="vs" data-id={id}><Stamp r={c} v={v} clipId={"wcp" + id} /></g>;
      })}
    </g>
  );
});

/**
 * counts: 나라코드 -> 체크인한 지역 수, wholeVisits: 나라 하나로 남긴 체크인(Map), mapped: 나라코드 -> 지역 수(지역 지도가 있는 나라)
 */
export default function WorldMap({ world, counts, wholeVisits, mapped, onPickCountry, onEnterCountry, onHover, readOnly }) {
  const full = { x: 0, y: 0, w: world.w, h: world.h };
  const pz = usePanZoom(full, {
    minW: 40,
    wheelNeedsCtrl: true,
    onHover,
    onPick: (id) => (mapped[id] ? onEnterCountry(id) : onPickCountry(id)),
  });
  const { tip, dragging, k } = pz;
  const tipCountry = tip && world.byId[tip.id];
  const tipVisit = tip && wholeVisits.get(tip.id);
  const tipCount = tip && counts.get(tip.id);
  const stamped = new Set([...counts.keys(), ...wholeVisits.keys()]).size;

  // 지도는 창 너비를 꽉 채운다 (높이는 지도 비율대로, 화면보다 크면 페이지가 스크롤된다).
  // 좁은 화면에서는 최소 높이를 지키고 양옆을 잘라 보여준다 (slice, 끌어서 옆을 본다). 제목·범례·안내는 지도를 가리지 않게 바깥에
  return (
    <section className="world-view">
      <header className="world-head">
        <div>
          <h1>세계지도</h1>
          <p>{stamped}개국 도장 · {readOnly ? "도장 찍힌 나라를 눌러 구경해요" : "나라를 눌러 지역에 체크인해요"}</p>
        </div>
        <Legend />
      </header>
      <div className="mapwrap world" ref={pz.wrapRef}>
        <svg {...pz.svgProps} preserveAspectRatio="xMidYMid slice" className={dragging ? "dragging" : ""} style={{ "--k": k }}
          role="img" aria-label="세계지도">
          <Countries countries={world.countries} land={world.land} />
          <Stamped counts={counts} byId={world.byId} />
          <Whole visits={wholeVisits} byId={world.byId} />
        </svg>
        <div className="zoom world-zoom">
          <button onClick={() => pz.zoomAt(0.7)} aria-label="확대">+</button>
          <button onClick={() => pz.zoomAt(1 / 0.7)} aria-label="축소">−</button>
          <button onClick={pz.reset} aria-label="전체 보기">⤢</button>
        </div>
        {tipCountry && (
          <div className={"tip" + (tipVisit ? " rich" : "")} style={{ left: tip.x, top: tip.y }}>
            {tipVisit?.photoUrl && <img src={tipVisit.photoUrl} alt="" />}
            <div>
              <b>{tipCountry.n}</b>
              <small>
                {mapped[tip.id]
                  ? `${tipCount ? tipCount + "/" : ""}${mapped[tip.id]}곳 · 눌러서 지역 지도로`
                  : tipVisit ? (tipVisit.title || tipVisit.date) : "나라 하나로 체크인"}
              </small>
            </div>
          </div>
        )}
      </div>
      <p className="world-hint">나라를 누르면 지역 지도가 열려요 · 끌어서 옮기고 Ctrl(⌘)+스크롤·핀치로 확대</p>
    </section>
  );
}
