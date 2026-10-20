import { useState } from "react";
import { api, LOGIN_URL } from "./api.js";
import { PassportCover } from "./Passport.jsx";

/** 로그인하고 돌아오면 이 주소(친구 공유 링크 등)로 다시 간다 */
export const AFTER_LOGIN = "churavel.afterLogin";

// 카카오 로그인 버튼은 카카오 디자인 가이드(노란 배경 #FEE500, 말풍선 심볼, "카카오 로그인")를 따른다.
function KakaoSymbol() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
      <path fill="#000" d="M9 1.5C4.58 1.5 1 4.32 1 7.8c0 2.25 1.5 4.22 3.75 5.34l-.96 3.5c-.08.3.26.54.52.37l4.17-2.76c.17.01.34.02.52.02 4.42 0 8-2.82 8-6.3S13.42 1.5 9 1.5z" />
    </svg>
  );
}

/** 아이디·비밀번호 로그인 / 가입. 카카오와 따로인 계정이다 */
function IdLogin({ onLogin }) {
  const [mode, setMode] = useState("login"); // login | signup
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const signup = mode === "signup";

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      onLogin(signup ? await api.signup(username, password, nickname) : await api.login(username, password));
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };
  const toggle = () => {
    setMode(signup ? "login" : "signup");
    setError(null);
  };

  return (
    <form className="id-login" onSubmit={submit}>
      <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="아이디" aria-label="아이디"
             autoComplete="username" autoCapitalize="none" spellCheck={false} maxLength={20} required />
      <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
             placeholder={signup ? "비밀번호 (8자 이상)" : "비밀번호"} aria-label="비밀번호"
             autoComplete={signup ? "new-password" : "current-password"} minLength={signup ? 8 : undefined} required />
      {signup && (
        <input value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="여권에 적을 이름 (닉네임)"
               aria-label="닉네임" autoComplete="nickname" maxLength={20} required />
      )}
      <button className="btn primary" disabled={busy}>{signup ? "여권 만들기" : "아이디로 로그인"}</button>
      {error && <p className="login-error" role="alert">{error}</p>}
      <button type="button" className="id-toggle" onClick={toggle}>
        {signup ? "이미 여권이 있어요 · 로그인" : "아이디가 없어요 · 여권 만들기"}
      </button>
    </form>
  );
}

export default function Login({ error, onLogin }) {
  const remember = () => {
    try {
      if (location.hash.length > 2) sessionStorage.setItem(AFTER_LOGIN, location.hash);
    } catch {}
  };
  return (
    <main className="login">
      <PassportCover>
        <p className="cover-note">츄레블맵 여권을 들고<br />다녀온 나라와 동네에 도장을 찍어요</p>
        <a className="kakao-btn" href={LOGIN_URL} onClick={remember}>
          <KakaoSymbol />
          <span>카카오 로그인</span>
        </a>
        {error && <p className="login-error" role="alert">{error}</p>}
        <p className="login-or"><span>또는</span></p>
        <IdLogin onLogin={onLogin} />
      </PassportCover>
    </main>
  );
}
