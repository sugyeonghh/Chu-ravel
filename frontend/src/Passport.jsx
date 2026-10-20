import { useRef, useState } from "react";
import { fmtDate } from "./util.js";

// 여권: 왼쪽은 신원 정보면, 오른쪽은 입국 도장(체크인한 나라)면.
// 남의 여권은 지도를 볼 수 있을 때(수락된 팔로워)만 펼쳐지고, 아니면 표지만 보인다.

const INKS = ["#d64545", "#2f6db5", "#2e8b57", "#8e44ad", "#d9822b", "#1b8a8a"];
const SHAPES = ["round", "rect", "oval"];

function hash(s) {
  let h = 7;
  for (const ch of s) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return h;
}

/** 나라 코드마다 잉크 색·모양·기울기가 늘 같은 도장 */
function VisaStamp({ code, name, date, sub, onClick }) {
  const h = hash(code);
  const style = { "--ink": INKS[h % INKS.length], transform: `rotate(${(h % 23) - 11}deg)` };
  return (
    <button className={"visa " + SHAPES[(h >> 3) % SHAPES.length]} style={style} onClick={onClick} title={name}>
      <small>입국 · ARRIVAL</small>
      <b>{name}</b>
      <span>{sub || code}</span>
      {date && <em>{fmtDate(date)}</em>}
    </button>
  );
}

function Avatar({ src, name, className = "" }) {
  return src
    ? <img className={"avatar " + className} src={src} alt="" referrerPolicy="no-referrer" />
    : <span className={"avatar " + className} aria-hidden="true">{name.slice(0, 1)}</span>;
}

/** 기계 판독 줄 (장식). 닉네임은 한글이라 영문 자리는 CHURAVEL 로 채운다 */
function mrz(p) {
  const pad = (s, n) => (s + "<".repeat(n)).slice(0, n);
  const ymd = (p.issuedOn || "").replaceAll("-", "").slice(2);
  return [pad("P<KOR" + "CHURAVEL<<TRAVELER", 44), pad(p.passportNo + "<KOR" + ymd + "<<<<<<<<<<<<<<<<<<<<", 44)];
}

export function PassportCover({ profile, children }) {
  return (
    <div className="cover">
      <p className="cover-top">대한민국 · REPUBLIC OF KOREA</p>
      <div className="emblem" aria-hidden="true">✦</div>
      <h2>여권<small>PASSPORT</small></h2>
      {profile && (
        <div className="cover-owner">
          <Avatar src={profile.profileImageUrl} name={profile.nickname} />
          <b>{profile.nickname}</b>
          <span>{profile.passportNo}</span>
        </div>
      )}
      {children}
    </div>
  );
}

/** 신원면 사진. 내 여권이면 눌러서 바꾸고, 직접 올린 사진이면 원래대로 되돌린다 */
function IdPhoto({ profile, editable, onChangePhoto, onResetPhoto }) {
  const input = useRef();
  const [busy, setBusy] = useState(false);
  const photo = <Avatar src={profile.profileImageUrl} name={profile.nickname} className="id-photo" />;
  if (!editable) return photo;

  const run = async (task) => {
    setBusy(true);
    try {
      await task();
    } finally {
      setBusy(false);
    }
  };
  const custom = profile.profileImageUrl?.startsWith("/api/avatars/");
  return (
    <div className="id-photo-edit">
      <button type="button" className="id-photo-btn" disabled={busy} onClick={() => input.current.click()}
              aria-label="프로필 사진 바꾸기">
        {photo}
        <span className="id-photo-hint">{busy ? "올리는 중…" : "사진 바꾸기"}</span>
      </button>
      <input type="file" accept="image/*" hidden ref={input} onChange={(e) => {
        const file = e.target.files[0];
        e.target.value = "";
        if (file) run(() => onChangePhoto(file));
      }} />
      {custom && (
        <button type="button" className="id-photo-reset" disabled={busy} onClick={() => run(onResetPhoto)}>원래대로</button>
      )}
    </div>
  );
}

/** stamps: [{code, name, date(처음 간 날), count(지역 수), total(그 나라 지역 수)}] — 처음 간 순서 */
export default function Passport({ profile, stamps, isMe, onOpenWorld, onOpenCountry, onChangePhoto, onResetPhoto }) {
  const countryCount = stamps.length;
  const regionCount = stamps.reduce((sum, st) => sum + st.count, 0);
  const [l1, l2] = mrz(profile);

  return (
    <div className="book">
      <section className="page id-page">
        <header className="page-head">
          <span>대한민국 REPUBLIC OF KOREA</span>
          <b>여권 PASSPORT</b>
        </header>
        <div className="id-body">
          <IdPhoto profile={profile} editable={isMe} onChangePhoto={onChangePhoto} onResetPhoto={onResetPhoto} />
          <dl className="id-fields">
            <div><dt>종류 Type</dt><dd>PM</dd></div>
            <div><dt>발행국 Country</dt><dd>KOR</dd></div>
            <div className="wide"><dt>여권번호 Passport No.</dt><dd className="mono">{profile.passportNo}</dd></div>
            <div className="wide"><dt>성명 Name</dt><dd className="name">{profile.nickname}</dd></div>
            <div><dt>발급일 Issued</dt><dd>{fmtDate(profile.issuedOn)}</dd></div>
            <div><dt>도장 Stamps</dt><dd>{countryCount}개국</dd></div>
          </dl>
        </div>
        <div className="mrz" aria-hidden="true"><div>{l1}</div><div>{l2}</div></div>
      </section>

      <section className="page visa-page">
        <header className="page-head">
          <span>사증 · VISAS</span>
          <b>{countryCount}개국 · {regionCount}곳</b>
        </header>
        <div className="visas">
          {stamps.map((st) => (
            <VisaStamp key={st.code} code={st.code} name={st.name} date={st.date}
              sub={st.total ? `${st.count}/${st.total}곳` : st.code} onClick={() => onOpenCountry(st.code)} />
          ))}
          {countryCount === 0 && (
            <p className="visas-empty">
              {isMe ? "아직 찍힌 도장이 없어요. 세계지도를 펼쳐 첫 도장을 찍어 보세요" : "아직 찍힌 도장이 없어요"}
            </p>
          )}
        </div>
        <button className="btn primary open-world" onClick={onOpenWorld}>세계지도 펼치기 →</button>
      </section>
    </div>
  );
}

export { Avatar };
