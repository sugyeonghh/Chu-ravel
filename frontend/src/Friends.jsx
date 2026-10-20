import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "./api.js";
import { Avatar } from "./Passport.jsx";
import { go } from "./route.js";
import { shareToKakao } from "./kakao.js";

// 내 여권 아래 친구 패널: 공유 링크, 여권번호로 찾기, 받은 요청, 팔로잉, 팔로워, 보낸 요청

function Person({ t, children }) {
  return (
    <li className="person">
      <button className="person-main" onClick={() => go({ who: t.passportNo })}>
        <Avatar src={t.profileImageUrl} name={t.nickname} />
        <span><b>{t.nickname}</b><small>{t.passportNo}</small></span>
      </button>
      {children}
    </li>
  );
}

/** 공유 버튼 → 카카오톡으로 공유 / 링크 복사. 카카오 키가 없으면 복사만 */
function ShareMenu({ me, link, toast }) {
  const [open, setOpen] = useState(false);
  const [kakaoKey, setKakaoKey] = useState(null);
  const box = useRef();

  useEffect(() => {
    api.config().then((c) => setKakaoKey(c.kakaoJsKey)).catch(() => {});
  }, []);
  useEffect(() => {
    if (!open) return;
    const close = (e) => { if (!box.current?.contains(e.target)) setOpen(false); };
    const esc = (e) => { if (e.key === "Escape") setOpen(false); };
    document.addEventListener("pointerdown", close);
    document.addEventListener("keydown", esc);
    return () => {
      document.removeEventListener("pointerdown", close);
      document.removeEventListener("keydown", esc);
    };
  }, [open]);

  const kakao = async () => {
    setOpen(false);
    try {
      await shareToKakao(kakaoKey, {
        text: `${me.nickname}님의 츄레블맵 여권이에요.\n다녀온 나라와 동네에 찍은 도장을 구경해 보세요!`,
        url: link,
        buttonTitle: "여권 펼쳐 보기",
      });
    } catch (e) {
      toast(e.message);
    }
  };
  const copy = async () => {
    setOpen(false);
    try {
      await navigator.clipboard.writeText(link);
      toast("공유 링크를 복사했어요");
    } catch {
      toast("복사하지 못했어요. 링크를 길게 눌러 직접 복사해 주세요");
    }
  };

  return (
    <div className="share-menu" ref={box}>
      <button className="btn primary" aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen(!open)}>공유</button>
      {open && (
        <div className="share-pop" role="menu">
          {kakaoKey && (
            <button role="menuitem" className="share-kakao" onClick={kakao}>
              <KakaoBubble />카카오톡으로 공유
            </button>
          )}
          <button role="menuitem" onClick={copy}>
            <span className="share-ico" aria-hidden="true">⧉</span>링크 복사
          </button>
        </div>
      )}
    </div>
  );
}

function KakaoBubble() {
  return (
    <svg width="16" height="16" viewBox="0 0 18 18" aria-hidden="true">
      <path fill="currentColor" d="M9 1.5C4.58 1.5 1 4.32 1 7.8c0 2.25 1.5 4.22 3.75 5.34l-.96 3.5c-.08.3.26.54.52.37l4.17-2.76c.17.01.34.02.52.02 4.42 0 8-2.82 8-6.3S13.42 1.5 9 1.5z" />
    </svg>
  );
}

export default function Friends({ me, toast }) {
  const [f, setF] = useState(null);
  const [no, setNo] = useState("");
  const link = `${location.origin}/#/p/${me.passportNo}`;

  const load = useCallback(() => api.friends().then(setF).catch((e) => toast(e.message)), [toast]);
  useEffect(() => { load(); }, [load]);

  const act = (fn, msg) => async () => {
    try {
      await fn();
      if (msg) toast(msg);
      load();
    } catch (e) {
      toast(e.message);
    }
  };
  const find = (e) => {
    e.preventDefault();
    const v = no.trim().toUpperCase();
    if (!/^M\d{8}$/.test(v)) return toast("여권번호는 M 으로 시작하는 9자리예요 (예: M12345678)");
    setNo("");
    go({ who: v });
  };

  return (
    <section className="friends">
      <div className="card share">
        <p className="eyebrow">내 여권 공유하기</p>
        <p className="share-desc">친구에게 링크를 보내면 팔로우 요청을 받을 수 있어요. 내가 수락해야 내 지도가 보여요.</p>
        <div className="share-row">
          <code>{link}</code>
          <ShareMenu me={me} link={link} toast={toast} />
        </div>
        <form className="share-row" onSubmit={find}>
          <input value={no} onChange={(e) => setNo(e.target.value)} placeholder="친구 여권번호 (M12345678)" aria-label="친구 여권번호" />
          <button className="btn">찾기</button>
        </form>
      </div>

      {f && f.incoming.length > 0 && (
        <div className="card">
          <p className="eyebrow">받은 팔로우 요청 {f.incoming.length}</p>
          <ul className="people">
            {f.incoming.map((t) => (
              <Person key={t.passportNo} t={t}>
                <button className="btn primary sm" onClick={act(() => api.accept(t.passportNo), `${t.nickname}님이 이제 내 지도를 볼 수 있어요`)}>수락</button>
                <button className="btn sm" onClick={act(() => api.removeFollower(t.passportNo))}>거절</button>
              </Person>
            ))}
          </ul>
        </div>
      )}

      <div className="card">
        <p className="eyebrow">팔로잉 {f ? f.following.length : ""}</p>
        {f && f.following.length === 0 && <p className="empty">아직 팔로우하는 친구가 없어요. 친구의 여권 링크를 받아 보세요.</p>}
        <ul className="people">
          {f?.following.map((t) => <Person key={t.passportNo} t={t} />)}
          {f?.outgoing.map((t) => (
            <Person key={t.passportNo} t={t}>
              <span className="pending">수락 대기</span>
              <button className="btn sm" onClick={act(() => api.unfollow(t.passportNo), "요청을 취소했어요")}>취소</button>
            </Person>
          ))}
        </ul>
      </div>

      <div className="card">
        <p className="eyebrow">팔로워 {f ? f.followers.length : ""}</p>
        {f && f.followers.length === 0 && <p className="empty">내 지도를 보는 친구가 아직 없어요.</p>}
        <ul className="people">
          {f?.followers.map((t) => (
            <Person key={t.passportNo} t={t}>
              <button className="btn sm" onClick={act(() => api.removeFollower(t.passportNo), `${t.nickname}님은 이제 내 지도를 못 봐요`)}>삭제</button>
            </Person>
          ))}
        </ul>
      </div>
    </section>
  );
}
