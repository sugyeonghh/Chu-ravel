import { useEffect, useState } from "react";

// 화면 주소는 해시로 둔다 (서버는 / 하나만 index.html 로 주면 된다. 공유 링크도 #/p/여권번호)
//   #/            내 여권          #/p/M12345678         친구 여권
//   #/world       내 세계지도      #/p/M12345678/world   친구 세계지도
//   #/c/JP        내 일본 지도     #/p/M12345678/c/JP    친구 일본 지도   (#/kr 은 #/c/KR 과 같다)
export function parse(hash) {
  const parts = hash.replace(/^#\/?/, "").split("/").filter(Boolean);
  let who = null;
  if (parts[0] === "p" && parts[1]) {
    who = decodeURIComponent(parts[1]).toUpperCase();
    parts.splice(0, 2);
  }
  if (parts[0] === "kr") return { who, page: "country", code: "KR" };
  if (parts[0] === "c" && /^[A-Za-z]{2}$/.test(parts[1] || "")) return { who, page: "country", code: parts[1].toUpperCase() };
  return { who, page: parts[0] === "world" ? "world" : "passport" };
}

export function href({ who = null, page = "passport", code }) {
  const base = who ? `#/p/${encodeURIComponent(who)}` : "#";
  if (page === "world") return base + "/world";
  if (page === "country") return `${base}/c/${code}`;
  return base + "/";
}

export function go(route) {
  location.hash = href(route);
}

export function useRoute() {
  const [route, setRoute] = useState(() => parse(location.hash));
  useEffect(() => {
    const on = () => setRoute(parse(location.hash));
    window.addEventListener("hashchange", on);
    return () => window.removeEventListener("hashchange", on);
  }, []);
  return route;
}
