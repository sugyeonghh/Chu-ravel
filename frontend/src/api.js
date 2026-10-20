// Spring Boot API. 오류는 ProblemDetail 의 detail(한국어 문장)을 그대로 Error 메시지로 쓴다.
// 로그인은 세션 쿠키 (카카오 또는 아이디·비밀번호). 바꾸는 요청에는 XSRF-TOKEN 쿠키 값을 X-XSRF-TOKEN 헤더로 같이 보낸다.
import { shrink } from "./util.js";

export const LOGIN_URL = "/oauth2/authorization/kakao";

function cookie(name) {
  const m = document.cookie.match(new RegExp("(?:^|; )" + name + "=([^;]*)"));
  return m ? decodeURIComponent(m[1]) : null;
}

async function call(method, url, body) {
  const opt = { method, headers: {} };
  if (body instanceof FormData) opt.body = body;
  else if (body !== undefined) {
    opt.headers["Content-Type"] = "application/json";
    opt.body = JSON.stringify(body);
  }
  if (method !== "GET") {
    // 로그인 전 화면은 401 만 받아서 쿠키가 아직 없다 → 한 번 받아 온다
    if (!cookie("XSRF-TOKEN")) await fetch("/api/auth/csrf");
    const xsrf = cookie("XSRF-TOKEN");
    if (xsrf) opt.headers["X-XSRF-TOKEN"] = xsrf;
  }
  const res = await fetch(url, opt);
  if (!res.ok) {
    let msg = res.status + " " + res.statusText;
    try {
      const p = await res.json();
      if (p.detail) msg = p.detail;
    } catch {}
    const err = new Error(msg);
    err.status = res.status;
    throw err;
  }
  return res.status === 204 ? null : res.json();
}

/** 기록 보내기: 바뀐 것만 담는다 (사진은 줄여서) */
async function entryForm({ file, date, title, body }) {
  const fd = new FormData();
  if (file) fd.append("photo", await shrink(file), "photo.jpg");
  if (date) fd.append("visitedOn", date);
  if (title !== undefined) fd.append("title", title);
  if (body !== undefined) fd.append("body", body);
  return fd;
}

/** 서버 응답(체크인 요약) → 화면에서 쓰는 모양. date = 처음 간 날, photoUrl·title = 가장 최근 기록 */
export const norm = (r) => ({
  id: r.id,
  country: r.country,
  commentCount: r.commentCount || 0,
  entryCount: r.entryCount || 0,
  date: r.visitedOn,
  photoUrl: r.photoUrl,
  title: r.title || "",
  updatedAt: Date.parse(r.updatedAt),
});

export const api = {
  me: () => call("GET", "/api/me"),
  config: () => call("GET", "/api/config"), // {kakaoJsKey}
  logout: () => call("POST", "/api/logout"),
  // 프로필 사진 (긴 변 800px 로 줄여서). 둘 다 바뀐 내 여권을 돌려준다
  changePhoto: async (file) => {
    const fd = new FormData();
    fd.append("photo", await shrink(file, 800), "avatar.jpg");
    return call("PUT", "/api/me/photo", fd);
  },
  resetPhoto: () => call("DELETE", "/api/me/photo"),
  // 아이디·비밀번호. 성공하면 세션이 생기고 내 여권(me 와 같은 모양)이 온다
  login: (username, password) => call("POST", "/api/auth/login", { username, password }),
  signup: (username, password, nickname) => call("POST", "/api/auth/signup", { username, password, nickname }),
  // 내 체크인 (code = 지역 코드). 한 칸 안에 기록(사진+글)이 여러 개
  list: () => call("GET", "/api/visits"),
  visit: (code) => call("GET", `/api/visits/${code}`),
  addEntry: async (code, entry) => call("POST", `/api/visits/${code}/entries`, await entryForm(entry)),
  editEntry: async (id, entry) => call("PUT", `/api/entries/${id}`, await entryForm(entry)),
  /** 남은 기록이 있으면 체크인을, 마지막 기록이었으면 null 을 돌려준다 */
  deleteEntry: (id) => call("DELETE", `/api/entries/${id}`),
  remove: (code) => call("DELETE", `/api/visits/${code}`),

  // 다른 여행자 (여권번호 no)
  traveler: (no) => call("GET", `/api/travelers/${encodeURIComponent(no)}`),
  travelerVisits: (no) => call("GET", `/api/travelers/${encodeURIComponent(no)}/visits`),
  travelerVisit: (no, code) => call("GET", `/api/travelers/${encodeURIComponent(no)}/visits/${code}`),

  // 팔로우
  friends: () => call("GET", "/api/friends"),
  follow: (no) => call("POST", `/api/friends/${encodeURIComponent(no)}/follow`),
  unfollow: (no) => call("DELETE", `/api/friends/${encodeURIComponent(no)}/follow`),
  accept: (no) => call("POST", `/api/friends/${encodeURIComponent(no)}/accept`),
  removeFollower: (no) => call("DELETE", `/api/friends/${encodeURIComponent(no)}/follower`),

  // 댓글 (기록 id 로)
  comments: (entryId) => call("GET", `/api/comments?entryId=${entryId}`),
  writeComment: (entryId, body) => call("POST", "/api/comments", { entryId, body }),
  deleteComment: (id) => call("DELETE", `/api/comments/${id}`),
};
