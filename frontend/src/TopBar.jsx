import { Avatar } from "./Passport.jsx";
import { href } from "./route.js";

// 맨 위 막대: 여권 › 세계지도 › 일본 (친구 것을 보는 중이면 "준님의 여권") + 내 사진·로그아웃
export default function TopBar({ me, owner, route, countryName, onLogout }) {
  const who = route.who && route.who !== me.passportNo ? route.who : null;
  const crumbs = [
    { key: "passport", to: { who }, label: who ? `${owner?.nickname ?? "친구"}님의 여권` : "내 여권" },
    route.page !== "passport" && { key: "world", to: { who, page: "world" }, label: "세계지도" },
    route.page === "country" && { key: "country", label: countryName || route.code },
  ].filter(Boolean);

  return (
    <nav className="topbar">
      <a className="logo" href={href({})}>츄~♡레블맵</a>
      <ol className="crumbs">
        {crumbs.map((c, i) => (
          <li key={c.key}>
            {i < crumbs.length - 1 ? <a href={href(c.to)}>{c.label}</a> : <span aria-current="page">{c.label}</span>}
          </li>
        ))}
      </ol>
      {who && <a className="btn sm mine" href={href({})}>내 여권</a>}
      <div className="me">
        <Avatar src={me.profileImageUrl} name={me.nickname} />
        <span className="me-name">{me.nickname}</span>
        <button className="btn sm" onClick={onLogout}>로그아웃</button>
      </div>
    </nav>
  );
}
