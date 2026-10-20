import { fmtDate } from "./util.js";

// 나라 지역 지도 화면은 열차 승차권 테마. 여권(세계)에서 나라로 "타고 들어가는" 느낌.

function hash(s) {
  let h = 7;
  for (const ch of s) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return h;
}

/** 장식용 바코드: 글자마다 같은 무늬 */
export function Barcode({ text, height = 34 }) {
  const bars = [];
  let x = 0, h = hash(text);
  for (let i = 0; i < 46; i++) {
    h = (h * 1103515245 + 12345) >>> 0;
    const w = 1 + (h % 3), gap = 1 + ((h >> 4) % 2);
    bars.push(<rect key={i} x={x} y={0} width={w} height={height} />);
    x += w + gap;
  }
  return <svg className="barcode" viewBox={`0 0 ${x} ${height}`} preserveAspectRatio="none" aria-hidden="true">{bars}</svg>;
}

/** 절취선: 양옆이 동그랗게 파인 점선 */
export function Perf() {
  return <div className="perf" aria-hidden="true" />;
}

/** 정차역처럼 놓인 목표(10곳, 30곳 …). 지나온 구간만큼 선이 채워진다 */
function Line({ n, stops }) {
  const last = stops.reduce((r, m, i) => (n >= m ? i : r), -1);
  const fill = last < 0 ? 0
    : last === stops.length - 1 ? 1
      : (last + (n - stops[last]) / (stops[last + 1] - stops[last])) / (stops.length - 1);
  return (
    <div className="ticket-line">
      <div className="rail"><i style={{ width: fill * 100 + "%" }} /></div>
      <ol>
        {stops.map((m, i) => (
          <li key={m} className={n >= m ? "on" : ""} style={{ left: (i / (stops.length - 1)) * 100 + "%" }}>
            <span>{m}</span>
          </li>
        ))}
      </ol>
    </div>
  );
}

/** 장부 맨 위 승차권: 세계지도 → 이 나라, 탑승(체크인)한 곳 / 전체, 첫 탑승일 */
export default function Ticket({ code, name, n, total, stops, firstDate }) {
  const no = `${code}-${String(n).padStart(3, "0")}`;
  return (
    <section className="ticket">
      <header className="ticket-head">
        <b>승차권</b><span>TICKET</span>
        <small>No. {no}</small>
      </header>
      <div className="ticket-route">
        <div><small>출발 FROM</small><b>세계지도</b><span>WORLD</span></div>
        <svg className="ticket-train" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M6 3h12a3 3 0 0 1 3 3v9a3 3 0 0 1-3 3l2 3h-2.4l-2-3H8.4l-2 3H4l2-3a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3Zm0 3v5h12V6H6Zm1.5 7.5a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3Zm9 0a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3Z" />
        </svg>
        <div className="to"><small>도착 TO</small><b>{name}</b><span>{code}</span></div>
      </div>
      <dl className="ticket-meta">
        <div><dt>탑승 BOARDED</dt><dd>{n}곳</dd></div>
        <div><dt>전체 STOPS</dt><dd>{total}곳</dd></div>
        <div><dt>달성 RATE</dt><dd>{((n / total) * 100).toFixed(1)}%</dd></div>
        <div><dt>첫 탑승 DATE</dt><dd>{firstDate ? fmtDate(firstDate) : "-"}</dd></div>
      </dl>
      <Line n={n} stops={stops} />
      <Perf />
      <footer className="ticket-stub">
        <Barcode text={no} />
        <span>{no}</span>
      </footer>
    </section>
  );
}
