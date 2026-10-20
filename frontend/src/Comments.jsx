import { useEffect, useState } from "react";
import { api } from "./api.js";
import { Avatar } from "./Passport.jsx";

const MAX = 500;

function ago(iso) {
  const s = (Date.now() - Date.parse(iso)) / 1000;
  if (s < 60) return "방금";
  if (s < 3600) return Math.floor(s / 60) + "분 전";
  if (s < 86400) return Math.floor(s / 3600) + "시간 전";
  if (s < 86400 * 7) return Math.floor(s / 86400) + "일 전";
  return new Date(iso).toLocaleDateString("ko-KR");
}

/** 기록(사진+글) 하나의 댓글. 지도 주인과 수락된 팔로워만 이 화면에 올 수 있다 */
export default function Comments({ entryId, toast, onCount }) {
  const [list, setList] = useState(null);
  const [text, setText] = useState("");
  const [sending, setSending] = useState(false);

  useEffect(() => {
    api.comments(entryId).then(setList).catch((e) => toast(e.message));
  }, [entryId, toast]);

  const update = (next) => {
    setList(next);
    onCount?.(next.length);
  };
  const send = async (e) => {
    e.preventDefault();
    if (!text.trim()) return;
    setSending(true);
    try {
      const c = await api.writeComment(entryId, text.trim());
      update([...list, c]);
      setText("");
    } catch (err) {
      toast(err.message);
    } finally {
      setSending(false);
    }
  };
  const remove = async (id) => {
    try {
      await api.deleteComment(id);
      update(list.filter((c) => c.id !== id));
    } catch (err) {
      toast(err.message);
    }
  };

  return (
    <div className="comments">
      {list === null ? (
        <p className="empty">댓글을 불러오는 중…</p>
      ) : list.length === 0 ? (
        <p className="empty">아직 댓글이 없어요. 첫 댓글을 남겨 보세요.</p>
      ) : (
        <ul>
          {list.map((c) => (
            <li key={c.id} className="comment">
              <Avatar src={c.author.profileImageUrl} name={c.author.nickname} />
              <div>
                <p className="comment-head"><b>{c.author.nickname}</b><small>{ago(c.createdAt)}</small></p>
                <p className="comment-body">{c.body}</p>
              </div>
              {c.deletable && <button className="comment-del" onClick={() => remove(c.id)} aria-label="댓글 지우기">×</button>}
            </li>
          ))}
        </ul>
      )}
      <form className="comment-form" onSubmit={send}>
        <textarea value={text} maxLength={MAX} rows={2} placeholder="댓글 남기기"
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => { if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) send(e); }} />
        <div className="row">
          <small className="meta">{text.length}/{MAX}</small>
          <button className="btn primary sm" disabled={sending || !text.trim()}>등록</button>
        </div>
      </form>
    </div>
  );
}
