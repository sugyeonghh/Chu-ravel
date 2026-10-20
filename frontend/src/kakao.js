// 카카오톡 공유 (Kakao JavaScript SDK). 처음 누를 때 SDK 를 받아 init 한다.
// 카카오 디벨로퍼스 > 앱 > 플랫폼 > Web 사이트 도메인에 지금 주소(http://localhost:5173 등)가 있어야 링크가 열린다.
const SDK_URL = "https://t1.kakaocdn.net/kakao_js_sdk/2.8.4/kakao.min.js";
const SDK_INTEGRITY = "sha384-BGX7Wb16GmRFagri4bLJV2DgipVZaU2AI812GZtHtHzzDQJnfHasDXN5houxv45w";

let loading = null;

function load(key) {
  if (!loading) {
    loading = new Promise((ok, no) => {
      const s = document.createElement("script");
      s.src = SDK_URL;
      s.integrity = SDK_INTEGRITY;
      s.crossOrigin = "anonymous";
      s.onload = () => ok(window.Kakao);
      s.onerror = () => {
        loading = null;
        no(new Error("카카오 공유를 불러오지 못했어요. 잠시 뒤 다시 해 주세요"));
      };
      document.head.appendChild(s);
    });
  }
  return loading.then((Kakao) => {
    if (!Kakao.isInitialized()) Kakao.init(key);
    return Kakao;
  });
}

/** 카카오톡 친구·채팅방 고르는 창을 띄운다 */
export async function shareToKakao(key, { text, url, buttonTitle }) {
  const Kakao = await load(key);
  Kakao.Share.sendDefault({
    objectType: "text",
    text,
    link: { webUrl: url, mobileWebUrl: url },
    buttonTitle,
  });
}
