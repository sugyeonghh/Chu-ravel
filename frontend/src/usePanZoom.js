import { useEffect, useRef, useState } from "react";

/**
 * SVG 지도 줌·팬·핀치·탭. 한국 지도와 세계지도가 같이 쓴다.
 * 지도 칸은 data-id 를 단 요소. 마우스를 올리면 onHover(id) + tip, 끌지 않고 누르면 onPick(id).
 * full: 전체 viewBox {x, y, w, h}. minW: 가장 많이 확대했을 때의 viewBox 너비.
 */
export default function usePanZoom(full, { minW, onHover, onPick }) {
  const svgRef = useRef();
  const wrapRef = useRef();
  const vbRef = useRef(full);
  const [vb, setVbState] = useState(full);
  const [tip, setTip] = useState(null); // {id, x, y}
  const [dragging, setDragging] = useState(false);
  const cb = useRef();
  cb.current = { onHover, onPick };

  function setVB(v) {
    const w = Math.max(minW, Math.min(full.w * 1.1, v.w)), h = (w * full.h) / full.w;
    const cx = v.x + v.w / 2, cy = v.y + v.h / 2;
    let x = cx - w / 2, y = cy - h / 2;
    x = Math.max(full.x - w * 0.5, Math.min(full.x + full.w - w * 0.5, x));
    y = Math.max(full.y - h * 0.5, Math.min(full.y + full.h - h * 0.5, y));
    vbRef.current = { x, y, w, h };
    setVbState(vbRef.current);
  }
  function toSvg(cx, cy) {
    const svg = svgRef.current, p = svg.createSVGPoint();
    p.x = cx; p.y = cy;
    return p.matrixTransform(svg.getScreenCTM().inverse());
  }
  function zoomAt(f, cx, cy) {
    const vb = vbRef.current;
    const p = cx == null ? { x: vb.x + vb.w / 2, y: vb.y + vb.h / 2 } : toSvg(cx, cy);
    setVB({ x: p.x - (p.x - vb.x) * f, y: p.y - (p.y - vb.y) * f, w: vb.w * f, h: vb.h * f });
  }
  /** bounds [[x0,y0],[x1,y1]] 가 화면에 넉넉히 들어오게 */
  function zoomToBox([[x0, y0], [x1, y1]], pad = 2.6) {
    const w = Math.max((x1 - x0) * pad, ((y1 - y0) * pad * full.w) / full.h, minW * 2.2), h = (w * full.h) / full.w;
    setVB({ x: (x0 + x1) / 2 - w / 2, y: (y0 + y1) / 2 - h / 2, w, h });
  }
  const reset = () => setVB(full);

  // wheel 은 preventDefault 를 해야 해서 passive:false 로 직접 단다
  useEffect(() => {
    const svg = svgRef.current;
    const onWheel = (e) => { e.preventDefault(); zoomAt(Math.exp(e.deltaY * 0.0015), e.clientX, e.clientY); };
    svg.addEventListener("wheel", onWheel, { passive: false });
    return () => svg.removeEventListener("wheel", onWheel);
  }, []);

  const g = useRef({ ptrs: new Map(), downId: null, moved: 0, pinch0: null }).current;
  const idAt = (e) => e.target.closest?.("[data-id]")?.dataset.id ?? null;

  function onPointerDown(e) {
    g.ptrs.set(e.pointerId, { x: e.clientX, y: e.clientY });
    if (g.ptrs.size === 1) { g.downId = idAt(e); g.moved = 0; }
    if (g.ptrs.size === 2) {
      const [a, b] = [...g.ptrs.values()];
      g.pinch0 = { d: Math.hypot(a.x - b.x, a.y - b.y), w: vbRef.current.w };
      g.moved = 99;
    }
  }
  function onPointerMove(e) {
    if (!g.ptrs.has(e.pointerId)) {
      if (e.pointerType !== "mouse") return;
      const id = idAt(e);
      cb.current.onHover?.(id);
      if (id) {
        const box = wrapRef.current.getBoundingClientRect();
        setTip({ id, x: e.clientX - box.left, y: e.clientY - box.top });
      } else setTip(null);
      return;
    }
    const prev = g.ptrs.get(e.pointerId), cur = { x: e.clientX, y: e.clientY };
    g.ptrs.set(e.pointerId, cur);
    if (g.ptrs.size === 2 && g.pinch0) {
      const [a, b] = [...g.ptrs.values()];
      const d = Math.hypot(a.x - b.x, a.y - b.y);
      zoomAt((g.pinch0.w * g.pinch0.d) / d / vbRef.current.w, (a.x + b.x) / 2, (a.y + b.y) / 2);
      return;
    }
    g.moved += Math.abs(cur.x - prev.x) + Math.abs(cur.y - prev.y);
    if (g.moved > 6) {
      const svg = svgRef.current;
      if (!svg.hasPointerCapture(e.pointerId)) svg.setPointerCapture(e.pointerId);
      setDragging(true); setTip(null); cb.current.onHover?.(null);
      const s = svg.getScreenCTM().a, vb = vbRef.current;
      setVB({ ...vb, x: vb.x - (cur.x - prev.x) / s, y: vb.y - (cur.y - prev.y) / s });
    }
  }
  function endPtr(e) {
    const single = g.ptrs.size === 1;
    g.ptrs.delete(e.pointerId);
    if (g.ptrs.size < 2) g.pinch0 = null;
    setDragging(false);
    if (single && g.moved <= 6 && g.downId && e.type === "pointerup") cb.current.onPick?.(g.downId);
    if (g.ptrs.size === 0) g.downId = null;
  }
  function onPointerLeave() { setTip(null); cb.current.onHover?.(null); }

  return {
    svgRef, wrapRef, vb, tip, dragging, k: full.w / vb.w,
    zoomAt, zoomToBox, reset,
    svgProps: {
      ref: svgRef,
      viewBox: `${vb.x} ${vb.y} ${vb.w} ${vb.h}`,
      onPointerDown, onPointerMove, onPointerUp: endPtr, onPointerCancel: endPtr, onPointerLeave,
    },
  };
}
