import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { api } from "./api.js";
import { fileDate, fmtDate, today } from "./util.js";
import Comments from "./Comments.jsx";
import { Perf } from "./Ticket.jsx";

// 지역 서랍: 이 지역의 기록(사진 + 글)을 카드로 여러 개 보여주고, 카드를 누르면 사진 + 글 + 댓글.
//   list  기록 카드 목록 (+ 새 기록)       entry 기록 하나 (사진·글·댓글)
//   add   새 기록 쓰기                    edit  기록 고치기
// readOnly(친구 지도)면 보기만 한다.

const fail = (toast, verb = "저장하지") => (e) => toast(`${verb} 못했어요: ` + (e.message || e.code));
const excerpt = (s, n = 70) => (s.length > n ? s.slice(0, n).trimEnd() + "…" : s);

/** 숨긴 file input 을 하나 두고 pick(cb) 로 연다 */
function useFilePicker() {
  const ref = useRef();
  const cb = useRef();
  const input = (
    <input type="file" accept="image/*" hidden ref={ref} onChange={(e) => {
      const f = e.target.files[0];
      e.target.value = "";
      if (f) cb.current?.(f);
    }} />
  );
  return [input, (fn) => { cb.current = fn; ref.current.click(); }];
}

/** 사진 고르기 (끌어놓기 / 눌러서). current 는 고치기 전 사진 */
function PhotoDrop({ file, current, onFile, toast }) {
  const [over, setOver] = useState(false);
  const [picker, pick] = useFilePicker();
  const preview = useMemo(() => file && URL.createObjectURL(file), [file]);
  useEffect(() => () => preview && URL.revokeObjectURL(preview), [preview]);
  const take = (f) => (f.type.startsWith("image/") ? onFile(f) : toast("이미지 파일만 올릴 수 있어요"));
  const src = preview || current;
  return (
    <>
      {picker}
      <div className={"drop" + (over ? " over" : "")} tabIndex={0} role="button" aria-label="사진 고르기"
        onClick={() => pick(take)}
        onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); pick(take); } }}
        onDragOver={(e) => { e.preventDefault(); setOver(true); }}
        onDragLeave={() => setOver(false)}
        onDrop={(e) => { e.preventDefault(); setOver(false); const f = e.dataTransfer.files[0]; if (f) take(f); }}>
        {src
          ? <img src={src} alt="고른 사진 미리보기" />
          : <div><strong>여기에 사진을 끌어놓거나 눌러서 고르기</strong><span>사진 한 장에 글 하나를 남겨요</span></div>}
        {src && <span className="drop-cta">사진 바꾸기</span>}
      </div>
    </>
  );
}

/** 새 기록 / 기록 고치기 */
function EntryForm({ region, entry, isFirst, onSaved, onCancel, toast }) {
  const [file, setFile] = useState(null);
  const [date, setDate] = useState(entry?.visitedOn ?? today());
  const [title, setTitle] = useState(entry?.title ?? "");
  const [body, setBody] = useState(entry?.body ?? "");
  const [saving, setSaving] = useState(false);

  const take = (f) => {
    setFile(f);
    if (!entry) setDate(fileDate(f));
  };
  const save = async () => {
    setSaving(true);
    try {
      const data = { file, date: date || today(), title: title.trim(), body: body.trim() };
      const r = entry ? await api.editEntry(entry.id, data) : await api.addEntry(region.id, data);
      toast(entry ? "기록을 고쳤어요"
        : isFirst ? (region.isCountry ? region.n + " 입국 도장을 찍었어요!" : region.n + "에 체크인했어요!")
          : "기록을 더했어요");
      onSaved(r, entry ? entry.id : r.entries.reduce((a, e) => (e.id > a.id ? e : a)).id);
    } catch (e) {
      fail(toast)(e);
      setSaving(false);
    }
  };

  return (
    <>
      <PhotoDrop file={file} current={entry?.photoUrl} onFile={take} toast={toast} />
      <div className="field">
        <label htmlFor="eDate">방문한 날</label>
        <input type="date" id="eDate" value={date} max="2100-12-31" onChange={(e) => setDate(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor="eTitle">제목</label>
        <input id="eTitle" className="title-in" placeholder={`${region.n}에서의 하루`} value={title} maxLength={200}
          onChange={(e) => setTitle(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor="eBody">글</label>
        <textarea id="eBody" placeholder="무엇을 보고, 먹고, 느꼈나요? 빈 줄로 문단을 나눠요." value={body}
          onChange={(e) => setBody(e.target.value)} />
      </div>
      <div className="row">
        <button className="btn primary" disabled={(!entry && !file) || saving} onClick={save}>
          {saving ? "올리는 중…" : entry ? "저장" : isFirst ? "체크인하기" : "기록 더하기"}
        </button>
        {onCancel && <button className="btn" onClick={onCancel}>취소</button>}
      </div>
    </>
  );
}

/** 기록 카드 목록 */
function EntryList({ detail, readOnly, onOpen, onAdd, onRemoveAll }) {
  const [confirm, setConfirm] = useState(false);
  return (
    <>
      <p className="meta">기록 {detail.entries.length}개 · 처음 간 날 {fmtDate(detail.visitedOn)}</p>
      <div className="entries">
        {!readOnly && (
          <button className="entry-card add" onClick={onAdd}>
            <span className="entry-plus" aria-hidden="true">＋</span>
            <b>사진 더하기</b>
            <small>사진 한 장에 글 하나</small>
          </button>
        )}
        {detail.entries.map((e) => (
          <button key={e.id} className="entry-card" onClick={() => onOpen(e.id)}>
            <span className="entry-img"><img src={e.photoUrl} alt="" loading="lazy" /></span>
            <span className="entry-text">
              <b>{e.title || "제목 없는 기록"}</b>
              {e.body && <span>{excerpt(e.body)}</span>}
              <small>{fmtDate(e.visitedOn)}{e.commentCount > 0 && <em>💬 {e.commentCount}</em>}</small>
            </span>
          </button>
        ))}
      </div>
      {!readOnly && (confirm ? (
        <div className="confirm">
          <span>이 지역의 기록 {detail.entries.length}개와 사진·댓글을 모두 지울까요? 되돌릴 수 없어요.</span>
          <div className="row">
            <button className="btn danger" onClick={onRemoveAll}>모두 지우기</button>
            <button className="btn" onClick={() => setConfirm(false)}>취소</button>
          </div>
        </div>
      ) : (
        <button className="btn danger subtle" onClick={() => setConfirm(true)}>체크인 통째로 지우기</button>
      ))}
    </>
  );
}

/** 기록 하나: 사진 + 글 + 댓글 */
function EntryView({ entry, readOnly, onEdit, onDelete, toast, onCount }) {
  const [confirm, setConfirm] = useState(false);
  return (
    <>
      <article className="post">
        <img className="post-photo" src={entry.photoUrl} alt="" />
        <h3>{entry.title || "제목 없는 기록"}</h3>
        <div className="meta">{fmtDate(entry.visitedOn)} 방문</div>
        {entry.body && (
          <div className="body">
            {entry.body.split(/\n{2,}/).filter(Boolean).map((p, i) => <p key={i}>{p}</p>)}
          </div>
        )}
      </article>
      {!readOnly && (confirm ? (
        <div className="confirm">
          <span>이 사진과 글, 댓글을 지울까요? 되돌릴 수 없어요.</span>
          <div className="row">
            <button className="btn danger" onClick={onDelete}>지우기</button>
            <button className="btn" onClick={() => setConfirm(false)}>취소</button>
          </div>
        </div>
      ) : (
        <div className="row">
          <button className="btn" onClick={onEdit}>고치기</button>
          <button className="btn danger" onClick={() => setConfirm(true)}>기록 지우기</button>
        </div>
      ))}
      <section className="entry-comments">
        <p className="eyebrow">댓글</p>
        <Comments entryId={entry.id} toast={toast} onCount={onCount} />
      </section>
    </>
  );
}

/**
 * region: {id, n, p, isCountry?}, visit: 체크인 요약(없으면 아직 안 간 곳), owner: 친구 여권번호(친구 지도일 때)
 * readOnly: 친구 지도. ticket: 나라 지역 지도 화면(승차권 테마)
 */
export default function Drawer({ region, visit, owner, store, onClose, readOnly, ticket }) {
  const [detail, setDetail] = useState(null);
  const [view, setView] = useState(() => (visit ? { mode: "list" } : { mode: "add" }));
  const closeRef = useRef();
  const scrollRef = useRef();

  useEffect(() => {
    closeRef.current?.focus();
    const onKey = (e) => e.key === "Escape" && onClose();
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  // 이미 간 곳이면 기록을 받아 온다 (기록이 하나면 바로 연다)
  useEffect(() => {
    if (!visit) return;
    (owner ? api.travelerVisit(owner, region.id) : api.visit(region.id))
      .then((d) => {
        setDetail(d);
        if (d.entries.length === 1) setView({ mode: "entry", id: d.entries[0].id });
      })
      .catch(fail(store.toast, "불러오지"));
  }, [region.id, owner]); // 서랍은 칸마다 새로 열리므로(key) 처음 한 번만

  const go = (v) => {
    setView(v);
    scrollRef.current?.scrollTo(0, 0);
  };
  const saved = (d, entryId) => {
    setDetail(d);
    store.applyVisit(d);
    go({ mode: "entry", id: entryId });
  };
  const deleteEntry = async (id) => {
    try {
      const d = await api.deleteEntry(id);
      if (!d) {
        store.removeVisit(region.id);
        store.toast("마지막 기록을 지워서 체크인도 지웠어요");
        return onClose();
      }
      setDetail(d);
      store.applyVisit(d);
      store.toast("기록을 지웠어요");
      go({ mode: "list" });
    } catch (e) { fail(store.toast, "지우지")(e); }
  };
  const removeAll = async () => {
    try {
      await api.remove(region.id);
      store.removeVisit(region.id);
      store.toast("기록을 모두 지웠어요");
      onClose();
    } catch (e) { fail(store.toast, "지우지")(e); }
  };
  const setCount = useCallback((entryId, n) => setDetail((d) => {
    if (!d) return d;
    const entries = d.entries.map((e) => (e.id === entryId ? { ...e, commentCount: n } : e));
    const total = entries.reduce((s, e) => s + e.commentCount, 0);
    store.setCommentCount(region.id, total);
    return { ...d, entries, commentCount: total };
  }), [store, region.id]);

  const entry = detail && view.id != null ? detail.entries.find((e) => e.id === view.id) : null;
  const canGoBack = view.mode !== "list" && detail;

  let body;
  if (view.mode === "add") {
    body = <EntryForm region={region} isFirst={!detail} toast={store.toast} onSaved={saved}
      onCancel={detail ? () => go({ mode: "list" }) : null} />;
  } else if (!detail) {
    body = <p className="empty">기록을 불러오는 중…</p>;
  } else if (view.mode === "list" || !entry) {
    body = <EntryList detail={detail} readOnly={readOnly} onOpen={(id) => go({ mode: "entry", id })}
      onAdd={() => go({ mode: "add" })} onRemoveAll={removeAll} />;
  } else if (view.mode === "edit") {
    body = <EntryForm key={entry.id} region={region} entry={entry} toast={store.toast} onSaved={saved}
      onCancel={() => go({ mode: "entry", id: entry.id })} />;
  } else {
    body = <EntryView key={entry.id} entry={entry} readOnly={readOnly} toast={store.toast}
      onEdit={() => go({ mode: "edit", id: entry.id })} onDelete={() => deleteEntry(entry.id)}
      onCount={(n) => setCount(entry.id, n)} />;
  }

  return (
    <>
      <div className="scrim" onClick={onClose} />
      <section className={"drawer" + (ticket ? " ticket-drawer" : "")} aria-modal="true" role="dialog" aria-labelledby="dName">
        <header className="dh">
          <div>
            <p className="eyebrow">{ticket ? `도착 TO · ${region.p}` : region.p}</p>
            <h2 id="dName">{region.n}</h2>
          </div>
          <button className="x" ref={closeRef} onClick={onClose} aria-label="닫기">×</button>
        </header>
        {ticket && <Perf />}
        {canGoBack && (
          <nav className="dnav">
            <button className="back" onClick={() => go({ mode: "list" })}>← 기록 {detail.entries.length}개 모두 보기</button>
          </nav>
        )}
        <div className="db" ref={scrollRef}>{body}</div>
      </section>
    </>
  );
}
