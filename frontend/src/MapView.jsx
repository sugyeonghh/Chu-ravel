import { memo, useEffect, useImperativeHandle, useRef, useState } from "react";
import usePanZoom from "./usePanZoom.js";

const LIFT_OUT_MS = 260; // .lift transition 보다 살짝 길게

/* ---------- layers (pan/zoom 때 다시 그리지 않도록 memo) ---------- */

const BaseLayer = memo(function BaseLayer({ regions, byId }) {
  const dk = byId.DOKDO;
  return (
    <g>
      {regions.map((r) => <path key={r.id} d={r.d} className="rg" data-id={r.id} />)}
      {dk && <circle cx={(dk.b[0][0] + dk.b[1][0]) / 2} cy={(dk.b[0][1] + dk.b[1][1]) / 2} r={13} data-id="DOKDO" className="hit" />}
    </g>
  );
});

/** 지역(나라) 모양으로 오린 사진 한 장. 사진이 없으면 도장 색으로 칠한다. 세계지도도 쓴다 */
export function Stamp({ r, v, clipId }) {
  const [[x0, y0], [x1, y1]] = r.b;
  return (
    <>
      <clipPath id={clipId}><path d={r.d} /></clipPath>
      {v.photoUrl
        ? <image href={v.photoUrl} x={x0} y={y0} width={x1 - x0} height={y1 - y0} preserveAspectRatio="xMidYMid slice" clipPath={`url(#${clipId})`} />
        : <path d={r.d} style={{ fill: "var(--stamp)" }} />}
      <path d={r.d} className="vs-edge" />
    </>
  );
}

const VisitedLayer = memo(function VisitedLayer({ visits, byId }) {
  return (
    <g>
      {[...visits].map(([id, v]) => byId[id] && (
        <g key={id} className="vs"><Stamp r={byId[id]} v={v} clipId={"cp" + id} /></g>
      ))}
    </g>
  );
});

const ProvLayer = memo(function ProvLayer({ provs, byId }) {
  const dk = byId.DOKDO;
  return (
    <g>
      {dk && <text x={dk.c[0]} y={dk.c[1]} className="dokdo-tag">독도</text>}
      {provs.map((p, i) => <path key={i} d={p.d} className="pv" />)}
    </g>
  );
});

const LabelLayer = memo(function LabelLayer({ regions, visits }) {
  return (
    <g className="lbl">
      {regions.map((r) => <text key={r.id} x={r.c[0]} y={r.c[1]} className={visits.has(r.id) ? "on" : undefined}>{r.n}</text>)}
    </g>
  );
});

/* ---------- hover lift: 마우스를 올린 지역이 살짝 커지며 떠오른다 ---------- */

function Lifted({ r, v, leaving, onGone }) {
  const ref = useRef();
  const [up, setUp] = useState(false);
  // 처음엔 원래 크기로 그린 뒤 다음 프레임에 커지게 해서 transition 이 걸리게 한다
  useEffect(() => {
    const f = requestAnimationFrame(() => {
      ref.current?.getBoundingClientRect();
      setUp(true);
    });
    return () => cancelAnimationFrame(f);
  }, []);
  useEffect(() => {
    if (!leaving) return;
    const t = setTimeout(onGone, LIFT_OUT_MS);
    return () => clearTimeout(t);
  }, [leaving, onGone]);
  const cls = "lift" + (v ? " photo" : " bare") + (up && !leaving ? " up" : "");
  return (
    <g ref={ref} className={cls}>
      {v ? <Stamp r={r} v={v} clipId={"lcp" + r.id} /> : <path d={r.d} className="lift-land" />}
    </g>
  );
}

const LiftLayer = memo(function LiftLayer({ id, byId, visits }) {
  const [items, setItems] = useState([]); // [{id, leaving}]
  useEffect(() => {
    setItems((prev) => {
      const rest = prev.filter((it) => it.id !== id).map((it) => (it.leaving ? it : { ...it, leaving: true }));
      return id && byId[id] ? [...rest, { id, leaving: false }] : rest;
    });
  }, [id, byId]);
  const goneRef = useRef({});
  const gone = (key) => (goneRef.current[key] ??= () => {
    delete goneRef.current[key];
    setItems((prev) => prev.filter((it) => !(it.id === key && it.leaving)));
  });
  return (
    <g className="lifts">
      {items.map((it) => (
        <Lifted key={it.id} r={byId[it.id]} v={visits.get(it.id)} leaving={it.leaving} onGone={gone(it.id)} />
      ))}
    </g>
  );
});

/* ---------- map ---------- */

/**
 * 나라 하나의 지역 지도 (한국 시·군·구, 일본 도도부현 …). data = {w, h, regions, provs, byId}.
 * 나라가 바뀌면 viewBox 크기가 달라지므로 부르는 쪽에서 key 로 새로 만든다.
 */
export default function MapView({ ref, data, title, visits, selected, hovered, onHover, onPick, readOnly, onBack }) {
  const { regions, provs, byId } = data;
  const pz = usePanZoom({ x: 0, y: 0, w: data.w, h: data.h }, { minW: data.w / 15, onHover, onPick });
  const { tip, dragging, k } = pz;
  useImperativeHandle(ref, () => ({ zoomTo: (id) => byId[id] && pz.zoomToBox(byId[id].b) }));

  const sel = selected && byId[selected];
  const tipRegion = tip && byId[tip.id], tipVisit = tip && visits.get(tip.id);

  return (
    <div className="mapwrap" ref={pz.wrapRef}>
      <svg
        {...pz.svgProps}
        className={(k > 2.2 ? "zoomed" : "") + (dragging ? " dragging" : "")}
        style={{ "--k": k, aspectRatio: `${data.w}/${data.h}` }}
        role="img"
        aria-label={title + " 지역 지도"}
      >
        <defs>
          <pattern id="foil" width="5" height="5" patternUnits="userSpaceOnUse" patternTransform="rotate(35)">
            <rect width="5" height="5" style={{ fill: "var(--land)" }} />
            <line x1="0" y1="0" x2="0" y2="5" style={{ stroke: "var(--land-hatch)" }} strokeWidth="1.4" />
          </pattern>
        </defs>
        <BaseLayer regions={regions} byId={byId} />
        <VisitedLayer visits={visits} byId={byId} />
        <ProvLayer provs={provs} byId={byId} />
        <LiftLayer id={dragging ? null : hovered} byId={byId} visits={visits} />
        {sel && <path d={sel.d} className="sel-edge" />}
        <LabelLayer regions={regions} visits={visits} />
      </svg>
      <div className="mapbar">
        <div className="brand ticket-tag">
          <button className="back" onClick={onBack}>← 세계지도로 환승</button>
          <small className="tag-label">{title}행 승차권</small>
          <h1>{title}</h1>
          <p>{readOnly ? "사진이 붙은 역을 눌러 구경해요" : "다녀온 곳을 눌러 사진을 붙여요"}</p>
        </div>
        <div className="zoom">
          <button onClick={() => pz.zoomAt(0.7)} aria-label="확대">+</button>
          <button onClick={() => pz.zoomAt(1 / 0.7)} aria-label="축소">−</button>
          <button onClick={pz.reset} aria-label="전체 보기">⤢</button>
        </div>
      </div>
      {tipRegion && (
        <div className={"tip" + (tipVisit ? " rich" : "")} style={{ left: tip.x, top: tip.y }}>
          {tipVisit?.photoUrl && <img src={tipVisit.photoUrl} alt="" />}
          <div>
            <b>{tipRegion.p} {tipRegion.n}</b>
            {tipVisit && <small>{tipVisit.title || tipVisit.date || ""}{tipVisit.title && tipVisit.date ? " · " + tipVisit.date : ""}</small>}
          </div>
        </div>
      )}
      {visits.size === 0 && !readOnly && <div className="hint">지역을 누르면 사진과 날짜를 남길 수 있어요 · 스크롤·핀치로 확대</div>}
    </div>
  );
}
