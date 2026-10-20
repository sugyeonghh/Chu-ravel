import { memo, useMemo } from "react";
import Ticket from "./Ticket.jsx";

const PROV_ORDER = ["서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"];
const MILESTONES = [10, 30, 50, 100, 150, 200];

function Search({ regions, onOpen, toast, korea }) {
  const go = (input, { quiet = false } = {}) => {
    const v = input.value.trim();
    const r = regions.find((r) => r.p + " " + r.n === v) || (!quiet && (regions.find((r) => r.n === v) || regions.find((r) => r.n.startsWith(v))));
    if (r) {
      onOpen(r.id, { zoom: true });
      input.value = "";
    } else if (v && !quiet) toast("찾는 지역이 없어요. 지역 이름으로 검색해보세요");
  };
  return (
    <section className="card stub search">
      <p className="eyebrow">지역 찾기 · SEARCH</p>
      {/* datalist 에서 고르면 바로 열고, 직접 친 이름은 Enter 로 찾는다 */}
      <input list="rlist" placeholder={korea ? "예: 강릉시, 종로구" : `예: ${regions[0]?.n ?? ""}`} autoComplete="off"
        onChange={(e) => go(e.target, { quiet: true })}
        onKeyDown={(e) => e.key === "Enter" && go(e.target)} />
      <datalist id="rlist">
        {regions.map((r) => <option key={r.id} value={`${r.p} ${r.n}`} />)}
      </datalist>
    </section>
  );
}

/**
 * 지역 지도 오른쪽 장부 (승차권 테마): 승차권(개수·진행), 검색, 최근 기록, (한국만) 시·도별.
 * whole: 이 나라를 지역 대신 나라 하나로 남긴 예전 기록 [code, visit] — 있으면 따로 보여준다.
 */
export default memo(function Ledger({ data, country, korea, visits, whole, status, onOpen, onHover, toast, readOnly }) {
  const { regions, byId } = data;
  const n = visits.size, T = regions.length;
  const milestones = [...MILESTONES.filter((m) => m < T), T];

  const perProv = useMemo(() => {
    if (!korea) return [];
    const pc = {};
    regions.forEach((r) => { (pc[r.p] ??= [0, 0])[1]++; });
    visits.forEach((v, id) => { const r = byId[id]; if (r) pc[r.p][0]++; });
    return PROV_ORDER.filter((p) => pc[p]).map((p) => [p, ...pc[p]]);
  }, [korea, regions, byId, visits]);

  const firstDate = useMemo(() => [...visits.values()].map((v) => v.date).filter(Boolean).sort()[0], [visits]);

  const recent = useMemo(() => [...visits.entries()]
    .sort((a, b) => (b[1].date || "").localeCompare(a[1].date || "") || (b[1].updatedAt || 0) - (a[1].updatedAt || 0))
    .slice(0, 9), [visits]);

  return (
    <aside className="ledger">
      <Ticket code={country.code} name={country.name} n={n} total={T} stops={milestones} firstDate={firstDate} />
      {whole && (
        <section className="card stub">
          <p className="eyebrow">나라 전체 기록 · WHOLE</p>
          <button className="thumb wide" onClick={() => onOpen(whole[0])}>
            <span className="thumb-img"><img src={whole[1].photoUrl} alt="" loading="lazy" /></span>
            <div>지역을 고르기 전에 남긴 기록<small>{whole[1].date || ""}</small></div>
          </button>
        </section>
      )}
      <Search regions={regions} onOpen={onOpen} toast={toast} korea={korea} />
      <section className="card stub">
        <p className="eyebrow">최근 탑승 · BOARDING LOG</p>
        {recent.length > 0 ? (
          <div className="recent">
            {recent.map(([id, v]) => (
              <button key={id} className="thumb" onClick={() => onOpen(id, { zoom: true })}
                onMouseEnter={() => onHover(id)} onMouseLeave={() => onHover(null)}>
                <span className="thumb-img"><img src={v.photoUrl} alt="" loading="lazy" /></span>
                <div>{byId[id].n}<small>{v.date || ""}</small></div>
              </button>
            ))}
          </div>
        ) : (
          <p className="empty">{readOnly ? "아직 붙인 사진이 없어요." : "아직 붙인 사진이 없어요. 지도에서 다녀온 지역을 눌러 첫 사진을 올려보세요."}</p>
        )}
      </section>
      {korea && (
        <section className="card stub">
          <p className="eyebrow">노선별 · 시·도</p>
          <div className="provs">
            {perProv.map(([p, a, b]) => [
              <span key={p + "n"}>{p}</span>,
              <div key={p + "b"} className="bar"><i style={{ width: (a / b) * 100 + "%" }} /></div>,
              <span key={p + "c"} className="n">{a}/{b}</span>,
            ])}
          </div>
        </section>
      )}
      <div className={"status " + status.kind}><i /><span>{status.text}</span></div>
    </aside>
  );
});
