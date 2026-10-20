import { LOGIN_URL } from "./api.js";
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

export default function Login({ error }) {
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
      </PassportCover>
    </main>
  );
}
