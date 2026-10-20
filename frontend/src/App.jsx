import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { api, norm } from "./api.js";
import MapView from "./MapView.jsx";
import WorldMap from "./WorldMap.jsx";
import Ledger from "./Ledger.jsx";
import Drawer from "./Drawer.jsx";
import Login, { AFTER_LOGIN } from "./Login.jsx";
import Passport, { PassportCover } from "./Passport.jsx";
import GlobeIntro, { loadGlobe } from "./Globe.jsx";
import Friends from "./Friends.jsx";
import TopBar from "./TopBar.jsx";
import { go, useRoute } from "./route.js";

// 지도 데이터
//   /data/regions.json (id,n=이름,p=시도,d=SVG path,b=bounds,c=centroid), /data/provs.json  한국 시·군·구 (760x1000)
//   /data/world.json {w, h, countries:[{id,n,d,b,c,s}], land}                                  세계 나라
//   /data/countries/index.json {나라코드: 지역 수}, /data/countries/JP.json {w,h,regions,provs}  다른 나라 지역 (눌렀을 때 받는다)
const withIds = (m) => ({ ...m, byId: Object.fromEntries(m.regions.map((r) => [r.id, r])) });

function useMapData() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  useEffect(() => {
    Promise.all(["/data/regions.json", "/data/provs.json", "/data/world.json", "/data/countries/index.json"]
      .map((u) => fetch(u).then((r) => r.json())))
      .then(([regions, provs, world, mapped]) => setData({
        korea: withIds({ w: 760, h: 1000, regions, provs }),
        world: { ...world, byId: Object.fromEntries(world.countries.map((c) => [c.id, c])) },
        mapped: { ...mapped, KR: regions.length },
      }))
      .catch(setError);
  }, []);
  return { data, error };
}

/** 나라 하나의 지역 지도. 한국은 이미 받아 둔 것, 다른 나라는 처음 열 때 받아서 기억해 둔다 */
function useCountryMap(code, data) {
  const cache = useRef(new Map());
  const [, bump] = useState(0);
  const map = !code || !data ? null : code === "KR" ? data.korea : cache.current.get(code);
  useEffect(() => {
    if (!code || !data || code === "KR" || cache.current.has(code) || !data.mapped[code]) return;
    fetch(`/data/countries/${code}.json`)
      .then((r) => r.json())
      .then((m) => {
        cache.current.set(code, withIds(m));
        bump((n) => n + 1);
      })
      .catch(() => {});
  }, [code, data]);
  return map;
}

function useToast() {
  const [msg, setMsg] = useState(null);
  const timer = useRef();
  const toast = useCallback((m) => {
    setMsg(m);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => setMsg(null), 2600);
  }, []);
  return [msg, toast];
}

const toMap = (list) => new Map(list.map((r) => [r.regionCode, norm(r)]));
const withCount = (m, code, n) => (m?.has(code) ? new Map(m).set(code, { ...m.get(code), commentCount: n }) : m);

/** 팔로우 전·요청 중인 친구: 여권 표지만 */
function FollowGate({ profile, onChange, toast }) {
  const act = (fn, msg) => async () => {
    try {
      onChange(await fn());
      toast(msg);
    } catch (e) {
      toast(e.message);
    }
  };
  const requested = profile.relation === "REQUESTED";
  return (
    <div className="passport-view gate">
      <PassportCover profile={profile}>
        <p className="cover-note">
          {requested
            ? `팔로우 요청을 보냈어요. ${profile.nickname}님이 수락하면 여권을 펼쳐 볼 수 있어요.`
            : `${profile.nickname}님의 여권은 팔로우 요청이 수락되어야 볼 수 있어요.`}
        </p>
        {requested
          ? <button className="btn" onClick={act(() => api.unfollow(profile.passportNo), "요청을 취소했어요")}>요청 취소</button>
          : <button className="btn primary" onClick={act(() => api.follow(profile.passportNo), "팔로우 요청을 보냈어요")}>팔로우 요청</button>}
      </PassportCover>
    </div>
  );
}

const reducedMotion = () => matchMedia("(prefers-reduced-motion: reduce)").matches;

export default function App() {
  const { data, error } = useMapData();
  const route = useRoute();
  const [me, setMe] = useState(undefined); // undefined: 확인 중, null: 로그인 안 함, 내 여권 프로필
  const [loginError, setLoginError] = useState(null);
  const [myVisits, setMyVisits] = useState(() => new Map()); // regionCode(시·군·구 또는 나라) -> 체크인
  const [status, setStatus] = useState({ kind: "", text: "저장소 연결 중…" });
  const [friend, setFriend] = useState(null); // {profile, visits(Map|null)} | {error}
  const [selected, setSelected] = useState(null);
  const [hovered, setHovered] = useState(null);
  const [toastMsg, toast] = useToast();
  const mapRef = useRef();
  const pendingSelect = useRef(null);
  // "세계지도 펼치기": spin = 여권 카드들이 왼쪽으로 지나가고 지구본이 돌며 다가오는 중, reveal = 지구본이 평면 지도로 바뀌는 중
  const [opening, setOpening] = useState(false); // false | "spin" | "reveal" (.stage 의 클래스가 된다)
  const [closing, setClosing] = useState(false); // 세계지도 → 여권: 지도가 지구본으로 말려 멀어지고 카드들이 왼쪽에서 다시 들어오는 중

  // 로그인 확인. 공유 링크(#/p/..)로 왔다가 로그인했으면 그 주소로 돌아간다
  useEffect(() => {
    if (new URLSearchParams(location.search).get("login") === "fail") {
      setLoginError("카카오 로그인에 실패했어요. 다시 시도해 주세요");
      history.replaceState(null, "", "/");
    }
    api.me()
      .then((p) => {
        setMe(p);
        try {
          const after = sessionStorage.getItem(AFTER_LOGIN);
          sessionStorage.removeItem(AFTER_LOGIN);
          if (after && after !== location.hash) location.hash = after;
        } catch {}
      })
      .catch((e) => {
        setMe(null);
        if (e.status !== 401) setLoginError(e.message);
      });
  }, []);

  useEffect(() => {
    if (!me) return;
    api.list()
      .then((list) => {
        setMyVisits(toMap(list));
        setStatus({ kind: "ok", text: "서버에 저장됨" });
      })
      .catch((e) => {
        if (e.status === 401) setMe(null);
        else setStatus({ kind: "warn", text: "서버에 연결하지 못했어요: " + e.message });
      });
  }, [me]);

  // 친구 여권 (#/p/여권번호). 내 여권번호면 그냥 내 것
  const who = me && route.who && route.who !== me.passportNo ? route.who : null;

  // 같은 여권의 세계지도에서 여권으로 돌아오면 (경로 표시·뒤로 가기 어느 쪽이든) 여권이 다시 덮인다.
  // effect 에서 하면 세계지도가 한 번 사라졌다 다시 그려지므로, 그리기 전에 바로 정한다.
  const [shown, setShown] = useState({ page: route.page, who });
  if (shown.page !== route.page || shown.who !== who) {
    setShown({ page: route.page, who });
    setClosing(shown.page === "world" && route.page === "passport" && shown.who === who && !reducedMotion());
  }
  useEffect(() => {
    if (!closing) return;
    const t = setTimeout(() => setClosing(false), 1500); // CSS .closing 카드 delay .73s + card-away .7s
    return () => clearTimeout(t);
  }, [closing]);
  const loadFriend = useCallback(async (no) => {
    try {
      const profile = await api.traveler(no);
      const visits = profile.canView ? toMap(await api.travelerVisits(no)) : null;
      setFriend((f) => (f?.no === no ? { no, profile, visits } : f));
    } catch (e) {
      setFriend((f) => (f?.no === no ? { no, error: e.message } : f));
    }
  }, []);
  useEffect(() => {
    if (!who) return setFriend(null);
    setFriend({ no: who });
    loadFriend(who);
  }, [who, loadFriend]);

  // 화면을 옮기면 서랍을 닫는다 (여권 도장을 눌러 온 경우만 그 나라를 연다)
  useEffect(() => {
    setSelected(pendingSelect.current);
    pendingSelect.current = null;
  }, [route.page, route.code, who]);
  // 주소가 세계지도로 바뀐 뒤에 여권을 내린다 (그 전에 내리면 여권이 한 번 깜빡 돌아온다)
  useEffect(() => setOpening(false), [route.page, who]);
  // 여권·세계지도를 보고 있으면 지구본을 미리 받아 둔다 (어느 쪽으로 가든 지구본이 돈다)
  useEffect(() => {
    if (route.page === "passport" || route.page === "world") loadGlobe().catch(() => {});
  }, [route.page]);

  const readOnly = !!who;
  const owner = who ? friend?.profile : me;
  const visits = (who ? friend?.visits : myVisits) || new Map();

  // 체크인은 두 가지: 지역(country 와 code 가 다름)과 나라 하나 통째로(code === country)
  const { counts, wholeVisits, stamps } = useMemo(() => {
    const counts = new Map(), wholeVisits = new Map(), first = new Map();
    for (const [id, v] of visits) {
      if (!v.country) continue;
      if (id === v.country) wholeVisits.set(id, v);
      else counts.set(v.country, (counts.get(v.country) || 0) + 1);
      if (v.date && (!first.has(v.country) || v.date < first.get(v.country))) first.set(v.country, v.date);
    }
    const codes = new Set([...counts.keys(), ...wholeVisits.keys()]);
    const stamps = [...codes].map((code) => ({
      code,
      name: data?.world.byId[code]?.n ?? code,
      date: first.get(code),
      count: counts.get(code) || 0,
      total: data?.mapped[code] || 0,
    })).sort((a, b) => (a.date || "").localeCompare(b.date || ""));
    return { counts, wholeVisits, stamps };
  }, [visits, data]);

  const code = route.page === "country" ? route.code : null;
  const countryMap = useCountryMap(code, data);
  const regionVisits = useMemo(
    () => new Map([...visits].filter(([id, v]) => v.country === code && id !== code)),
    [visits, code]);

  const applyVisit = useCallback((r) => setMyVisits((prev) => new Map(prev).set(r.regionCode, norm(r))), []);
  const removeVisit = useCallback((id) => setMyVisits((prev) => {
    const next = new Map(prev);
    next.delete(id);
    return next;
  }), []);
  const setCommentCount = useCallback((code, n) => {
    if (who) setFriend((f) => ({ ...f, visits: withCount(f?.visits, code, n) }));
    else setMyVisits((prev) => withCount(prev, code, n));
  }, [who]);
  const store = useMemo(() => ({ applyVisit, removeVisit, toast, setCommentCount }),
    [applyVisit, removeVisit, toast, setCommentCount]);

  const openRegion = useCallback((id, { zoom = false } = {}) => {
    if (readOnly && !visits.has(id)) return toast("아직 다녀가지 않은 곳이에요");
    if (zoom) mapRef.current?.zoomTo(id);
    setSelected(id);
  }, [readOnly, visits, toast]);
  const close = useCallback(() => setSelected(null), []);
  /** 여권 도장·세계지도에서 나라를 누르면: 지역 지도가 있으면 그리로, 없으면 세계지도에서 나라 서랍 */
  const openCountry = useCallback((cc) => {
    if (data?.mapped[cc]) return go({ who, page: "country", code: cc });
    pendingSelect.current = cc;
    go({ who, page: "world" });
  }, [data, who]);

  const logout = useCallback(async () => {
    try {
      await api.logout();
    } catch {}
    setSelected(null);
    setMyVisits(new Map());
    setFriend(null);
    setMe(null);
    history.replaceState(null, "", "/");
  }, []);

  const changePhoto = async (file) => {
    try {
      setMe(await api.changePhoto(file));
      toast("프로필 사진을 바꿨어요");
    } catch (e) {
      toast(e.message);
    }
  };
  const resetPhoto = async () => {
    try {
      setMe(await api.resetPhoto());
      toast("프로필 사진을 원래대로 돌렸어요");
    } catch (e) {
      toast(e.message);
    }
  };

  /** 여권 카드들을 왼쪽으로 치우며 지구본을 돌려 한국에서 멈춘 뒤, 밑에 깔아 둔 평면 세계지도로 바꾸고 주소를 바꾼다 */
  const openWorld = () => {
    if (opening) return;
    if (reducedMotion()) return go({ who, page: "world" });
    scrollTo(0, 0);
    setOpening("spin");
  };
  const globeDone = () => {
    setOpening("reveal");
    setTimeout(() => go({ who, page: "world" }), 500); // CSS .reveal 길이
  };

  if (me === null) return <Login error={loginError} onLogin={setMe} />;
  if (error) return <p className="boot">지도 데이터를 불러오지 못했어요. 새로고침해 보세요.</p>;
  if (!data || me === undefined) return <p className="boot">여권을 펼치는 중…</p>;

  const { world, mapped } = data;
  const countryName = code && (world.byId[code]?.n ?? code);
  const worldMap = () => (
    <WorldMap world={world} counts={counts} wholeVisits={wholeVisits} mapped={mapped} readOnly={readOnly}
      onHover={setHovered}
      onEnterCountry={(cc) => go({ who, page: "country", code: cc })}
      onPickCountry={openRegion} />
  );
  let main;
  if (who && !friend?.profile && !friend?.error) {
    main = <p className="boot">여권을 펼치는 중…</p>;
  } else if (who && friend.error) {
    main = (
      <div className="passport-view">
        <div className="card notice"><p>{friend.error}</p><a className="btn" href="#/">내 여권으로</a></div>
      </div>
    );
  } else if (who && !friend.profile.canView) {
    main = <FollowGate profile={friend.profile} toast={toast} onChange={() => loadFriend(who)} />;
  } else if (route.page === "world") {
    main = <div className="stage"><div key="world" className="stage-world">{worldMap()}</div></div>;
  } else if (route.page === "country" && !mapped[code]) {
    main = (
      <div className="passport-view">
        <div className="card notice">
          <p>{countryName}은(는) 지역 지도가 없어서 세계지도에서 나라 하나로 체크인해요.</p>
          <button className="btn" onClick={() => openCountry(code)}>세계지도에서 열기</button>
        </div>
      </div>
    );
  } else if (route.page === "country" && !countryMap) {
    main = <p className="boot">{countryName} 지도를 펼치는 중…</p>;
  } else if (route.page === "country") {
    const whole = wholeVisits.has(code) ? [code, wholeVisits.get(code)] : null;
    main = (
      <div className="app">
        <MapView key={code} ref={mapRef} data={countryMap} title={countryName} visits={regionVisits} selected={selected}
          hovered={hovered} onHover={setHovered} onPick={openRegion} readOnly={readOnly}
          onBack={() => go({ who, page: "world" })} />
        <Ledger data={countryMap} country={{ code, name: countryName }} korea={code === "KR"} visits={regionVisits} whole={whole}
          onOpen={openRegion} onHover={setHovered} toast={toast} readOnly={readOnly}
          status={readOnly ? { kind: "ok", text: `${owner.nickname}님의 지도 · 구경만 할 수 있어요` } : status} />
      </div>
    );
  } else {
    // 세계지도 화면과 같은 .stage > key="world" 자리에 미리 그려 두어, 주소가 바뀌어도 지도를 다시 만들지 않는다
    main = (
      <div className={"stage" + (opening ? " opening " + opening : closing ? " closing" : "")}>
        {(opening || closing) && <div key="world" className="stage-world">{worldMap()}</div>}
        {opening && <GlobeIntro key="globe" counts={counts} wholeVisits={wholeVisits} onDone={globeDone} />}
        {closing && <GlobeIntro key="globe-out" direction="out" counts={counts} wholeVisits={wholeVisits} onDone={() => {}} />}
        <div key="passport" className="stage-passport passport-view">
        <Passport profile={owner} stamps={stamps} isMe={!who}
          onOpenWorld={openWorld}
          onOpenCountry={openCountry} onChangePhoto={changePhoto} onResetPhoto={resetPhoto} />
        {who ? (
          <div className="card friend-bar">
            <span>{owner.nickname}님을 팔로우하고 있어요{owner.followsMe ? " · 서로 팔로우" : ""}</span>
            <button className="btn sm" onClick={async () => {
              try {
                await api.unfollow(who);
                toast("팔로우를 끊었어요");
                loadFriend(who);
              } catch (e) { toast(e.message); }
            }}>팔로우 끊기</button>
          </div>
        ) : (
          <Friends me={me} toast={toast} />
        )}
        </div>
      </div>
    );
  }

  // 서랍: 지역 지도에서는 그 지역, 세계지도(또는 지역 지도의 "나라 전체 기록")에서는 나라 하나
  const wholeCountry = (id) => ({ id, n: world.byId[id].n, p: "나라 전체", isCountry: true });
  const drawerRegion = !selected ? null
    : route.page === "country" ? (selected === code ? wholeCountry(code) : countryMap?.byId[selected] ?? null)
      : route.page === "world" && world.byId[selected] ? wholeCountry(selected)
        : null;

  return (
    <>
      <TopBar me={me} owner={owner} route={route} countryName={countryName} onLogout={logout} />
      {main}
      {drawerRegion && (
        <Drawer key={(who || "me") + selected} region={drawerRegion} visit={visits.get(selected)} owner={who} store={store}
          onClose={close} readOnly={readOnly} ticket={route.page === "country"} />
      )}
      {toastMsg && <div className="toast" role="status">{toastMsg}</div>}
    </>
  );
}
