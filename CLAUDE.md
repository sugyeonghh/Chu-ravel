# 츄레블맵 (Churavel Map)

여권을 들고 여행하듯, 다녀온 나라와 한국 시·군·구에 사진을 입히고 방문 날짜·블로그 글을 남기는 여행 지도 웹사이트.
한 지역에 [사진 한 장 + 글] 기록을 여러 개 남기고, 댓글은 기록마다 단다.
개인 사이드 프로젝트. 카카오 로그인, 사용자마다 자기 여권(지도). 친구를 팔로우(요청 → 수락)하면 친구 지도를 보고 댓글을 단다.
화면 흐름: 여권 → 세계지도 → 나라를 누르면 그 나라 지역 지도. 지역을 하나라도 체크인한 나라에 입국 도장이 찍힌다.
한국은 230개 시·군·구(229 + 독도), 다른 210개 나라는 1단계 행정구역(일본 47 도도부현, 미국 50주+DC …, 4500여 곳).
지역 지도가 없는 작은 나라(바티칸, 모나코 …)는 세계지도에서 나라 하나로 체크인한다.

## 명령어

```bash
cp .env.example .env        # 처음 한 번: 카카오 REST API 키·Client Secret 채우기 (없으면 서버가 안 뜸)
./gradlew bootRun          # API 서버 http://localhost:8080 (local 프로필, H2 파일 DB). 화면은 안 뜬다 (/ 는 404)
cd frontend && npm run dev   # 개발할 때 여기로 접속: http://localhost:5173 (Vite, /api·/oauth2·/login/oauth2 는 8080 으로 프록시)
./gradlew test             # API 통합 테스트 (H2 메모리 DB, 임시 사진 폴더). npm 필요 없음
./gradlew build            # build/libs/churavel-map-*.jar — 프론트를 빌드해 jar 안에 넣는다 (배포용, 8080 하나로 화면+API)
./gradlew bootJar -PskipFrontend   # 프론트 없이 jar 만 (npm 없이)
cd tools/map && npm install && npm run gen        # 한국 지도 데이터 다시 만들기
cd tools/map && npm run gen:world                 # 세계지도 데이터 다시 만들기
cd tools/map && npm run gen:countries             # 나라별 지역 지도 다시 만들기 (gen:world 다음에. 원본 40MB 를 cache/ 에 받는다)
```

## 스택

- Java 21, Spring Boot 4.1, Gradle(Groovy DSL, wrapper 8.14.3)
- Spring Web MVC, Spring Data JPA(Hibernate), Bean Validation, Spring Security OAuth2 Client(카카오, 세션 쿠키)
- DB: 로컬 H2 파일(`./data/churavel.mv.db`, MODE=MySQL) / 배포 MySQL (`prod` 프로필)
- 사진: 로컬 디스크 `./data/photos` (`PhotoStorage` 인터페이스 뒤에 숨김 → 나중에 S3/R2)
- 프론트: `frontend/` React 19 + Vite 6 (JS, 상태 관리·라우터 라이브러리 없음, 해시 라우팅). Gradle `bootJar` 가 `npm ci` → `npm run build` 후 `frontend/dist` 를 jar 의 `static/`(`BOOT-INF/classes/static`)에 넣는다. 배포 jar 에만 들어가고 `bootRun`·IntelliJ 실행·테스트에는 안 들어간다. Node 20.15 기준이라 Vite 7 은 쓰지 않는다.

## 구조

```
src/main/java/com/sugyeong/churavel/
  auth/     SecurityConfig, KakaoOAuth2UserService(카카오 → User 가입·갱신·여권번호 발급), LoginUser(세션 principal),
            MeController(GET /api/me), KakaoApi(카카오 토큰 만료), KakaoLogoutHandler. 카카오 토큰은 세션에만 둔다
  user/     User(kakaoId, 여권번호 passportNo, 닉네임, 프로필 사진), PassportNumbers(M + 숫자 8자리 발급)
  follow/   Follow(follower → followee, PENDING/ACCEPTED), FollowService(canView/requireView, 요청·수락·끊기),
            FollowController(/api/friends), TravelerController(/api/travelers: 남의 여권·지도), Traveler(record)
  comment/  Comment(기록마다 여러 개, ON DELETE CASCADE), CommentService, CommentController(/api/comments)
  region/   Region(code,name,province,country), RegionCatalog(regions.csv + subdivisions.csv + countries.csv), GET /api/regions
  visit/    Visit(체크인, 사용자·칸당 1건) ─ 1:N ─ Entry(기록: 사진 1장 + 제목·본문 + 날짜), VisitService, VisitController,
            EntryMigration(예전 사진1+글1 체크인을 첫 기록으로 옮김, 서버 뜰 때)
  photo/    Photo(메타, ownerId), PhotoStorage/LocalPhotoStorage(파일), PhotoService, GET /api/photos/{id}
  common/   NotFoundException, ForbiddenException, ApiExceptionHandler(ProblemDetail)
src/main/resources/
  regions.csv               한국 지역 코드표 (code,name,province) — gen.js
  subdivisions.csv          다른 나라 지역 코드표 (code,name,country) — gen-countries.mjs
  countries.csv             나라 코드표 (code,name, 한국 제외) — gen-world.mjs
frontend/
  public/data/regions.json  한국 지도 SVG path (id,n,p,d,b,c)
  public/data/provs.json    시·도 경계선
  public/data/world.json    세계지도 {w,h,countries:[{id,n,d,b,c,s(작은 나라=점)}],land(코드 없는 땅)}
  public/data/countries/    나라별 지역 지도 {나라코드}.json {w,h,regions,provs(바깥 경계)} + index.json {나라코드: 지역 수}. 눌렀을 때 받는다
  src/App.jsx               로그인 확인, 지도 데이터, 내/친구 체크인(visits Map), 화면 고르기, 서랍, 토스트
  src/route.js              해시 라우팅: #/ · #/world · #/c/{나라코드}(#/kr = #/c/KR), 친구는 #/p/{여권번호}/… (= 공유 링크)
  src/TopBar.jsx            여권 › 세계지도 › 일본 + 내 사진·로그아웃
  src/Login.jsx             여권 표지 + 카카오 로그인 (공유 링크로 왔으면 로그인 뒤 그 주소로)
  src/Passport.jsx          여권 신원면 + 입국 도장면, PassportCover(표지), Avatar
  src/Friends.jsx           공유 링크, 여권번호로 찾기, 받은 요청·팔로잉·팔로워
  src/WorldMap.jsx          세계지도 (지역 지도가 있는 나라 → 그 나라 지도, 없는 나라 → 나라 하나로 체크인).
                            체크인한 지역 수로 나라 색 5단계 (1 · 2–4 · 5–9 · 10–19 · 20+, CSS --h1~--h5)
  src/MapView.jsx           나라 하나의 지역 지도 (한국 시·군·구, 일본 도도부현 …): 레이어(memo), 호버 lift, Stamp
  src/usePanZoom.js         두 지도가 같이 쓰는 줌·팬·핀치·탭·툴팁
  src/Ledger.jsx            지역 지도 오른쪽 장부(승차권, 검색, 최근 기록, 한국만 시·도별, 나라 하나로 남긴 예전 기록)
  src/Ticket.jsx            나라 지역 지도 화면의 열차 승차권 (출발 세계지도 → 도착 나라, 정차역=목표 개수, 절취선, 바코드)
                            테마: 여권·세계지도는 여권(남색·금박), 나라 지역 지도·그 서랍은 승차권(청록 --tk)
  src/Drawer.jsx            칸 서랍: 기록 카드 목록 → 누르면 사진 + 글 + 댓글, 새 기록·고치기. readOnly 면 친구 것 구경만
  src/Comments.jsx          기록 하나의 댓글
  src/api.js, util.js       API 호출, 사진 줄이기·날짜
tools/map/                  gen.js(한국, muni.json KOSTAT 2013), gen-world.mjs(world-atlas, i18n-iso-countries 한국어 이름),
                            gen-countries.mjs(Natural Earth 10m admin-1, name_ko. 해외 영토 등 본토에서 먼 조각은 뺀다, 미국은 AlbersUsa)
```

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/oauth2/authorization/kakao` | 카카오 로그인 시작 (`prompt=login`: 매번 계정 다시 확인) → 성공하면 `/`, 실패하면 `/?login=fail` |
| GET | `/api/me` | 내 여권 `{passportNo, nickname, profileImageUrl, issuedOn, relation:"SELF", ...}` (안 했으면 401) |
| POST | `/api/logout` | 로그아웃 (204). 카카오 토큰 만료 → 세션·쿠키 삭제 → `Clear-Site-Data`(캐시·쿠키·저장소) |
| GET | `/api/visits` | 내 체크인 요약 목록 `{id, regionCode, country, visitedOn(처음 간 날), photoUrl·title(최근 기록), entryCount, commentCount}` |
| GET | `/api/visits/{code}` | 한 칸 + `entries:[{id, photoUrl, title, body, visitedOn, commentCount}]` (최근 것부터). code = 한국 시·군·구 코드, 다른 나라 지역 코드(`JPN-1860`), 또는 나라 코드 |
| POST | `/api/visits/{code}/entries` | 기록 더하기. multipart `photo`(필수), `visitedOn`, `title`, `body`. 처음이면 체크인도 생긴다. `KR` 은 404 |
| PUT | `/api/entries/{id}` | 기록 고치기. multipart, 바꿀 것만 (`photo`, `visitedOn`, `title`, `body`) |
| DELETE | `/api/entries/{id}` | 기록 지우기 → 200 + 체크인, 마지막 기록이었으면 204 (체크인도 사라짐) |
| DELETE | `/api/visits/{code}` | 체크인 통째로 (기록·사진·댓글 모두) |
| GET | `/api/photos/{id}` | 사진 바이트 (immutable 캐시). 내 사진·수락된 팔로워만, 아니면 404 |
| GET | `/api/travelers/{passportNo}` | 남의 여권 표지 `{relation: SELF/FOLLOWING/REQUESTED/NONE, followsMe, canView}` |
| GET | `/api/travelers/{passportNo}/visits` | 남의 체크인 목록. 수락된 팔로워만, 아니면 403 |
| GET | `/api/travelers/{passportNo}/visits/{code}` | 남의 한 칸 + 기록들. 수락된 팔로워만 |
| GET | `/api/friends` | `{following, outgoing(보낸 요청), followers, incoming(받은 요청)}` |
| POST·DELETE | `/api/friends/{passportNo}/follow` | 팔로우 요청 / 끊기·요청 취소 |
| POST | `/api/friends/{passportNo}/accept` | 받은 요청 수락 (204) |
| DELETE | `/api/friends/{passportNo}/follower` | 받은 요청 거절 / 팔로워 끊기 (204) |
| GET | `/api/comments?entryId=` | 기록 하나의 댓글 `[{id, body, createdAt, author, deletable}]` |
| POST | `/api/comments` | JSON `{entryId, body}` (500자까지) |
| DELETE | `/api/comments/{id}` | 내 댓글 또는 내 체크인에 달린 댓글 |
| GET | `/api/regions` | 지역·나라 코드표 (로그인 없이 공개) |

`/api/regions` 말고는 모두 로그인 필요 → 안 했으면 401 `"로그인이 필요해요"`. PUT·POST·DELETE 는 `XSRF-TOKEN` 쿠키 값을 `X-XSRF-TOKEN` 헤더로 보내야 한다 (`api.js` 가 처리, 없으면 403).
오류는 모두 `ProblemDetail` JSON. 프론트는 `detail` 을 토스트로 그대로 보여주므로 **detail 은 사용자에게 보여줄 한국어 문장**으로 쓴다.

## 규칙

- 한국 지역 코드 = KOSTAT 2013 시군구 코드 앞 4자리. 일반구가 있는 시(수원·천안 등)는 시 하나로 합쳤고, 청원군은 청주시(3304)에 합쳤다. 독도는 `DOKDO`.
- 다른 나라 지역 코드 = Natural Earth `adm1_code` (`JPN-1860` = 도쿄도). ISO 3166-2 는 겹치는 게 있어서 안 쓴다.
- 나라 코드 = ISO 3166-1 alpha-2 (`JP`, `US`). 코소보는 `XK`. 체크인의 `country` 로 어느 나라 것인지 안다. code == country 면 나라 하나 통째로 남긴 기록 (지역 지도가 없는 나라, 또는 지역 지도가 생기기 전 기록). 한국(`KR`)은 나라로 체크인하지 않는다.
- 지역·나라는 DB 테이블이 아니라 `RegionCatalog`(csv). 바꾸려면 gen*.js 를 고치고 다시 생성한다.
- 엔티티는 setter 없이 의미 있는 메서드로 바꾼다 (`addEntry`, `removeEntry`, `Entry.edit`, `replacePhoto`, `accept`, `issuePassport`).
- `Visit.coverPhoto`·`visitedOn` 은 기록에서 뽑은 요약이다 (`Visit.summarize`): 지도 사진 = 가장 최근 기록의 사진, 방문일 = 가장 이른 기록의 날. 기록이 0개인 체크인은 없다.
- 사진 파일 삭제는 트랜잭션 커밋 뒤(`afterCommit`), 저장 후 롤백되면 파일도 지운다 (`PhotoService`).
- 사진은 프론트에서 긴 변 1600px JPEG 로 줄여 올린다. 서버 업로드 한도 20MB.
- `open-in-view: false`. 연관 로딩은 `@EntityGraph` 로.
- 컨트롤러는 `@AuthenticationPrincipal LoginUser me` 로 사용자를 받는다. 내 것은 `userId` 범위 안에서만 조회하고, 남의 것(지도·사진·댓글)은 반드시 `FollowService.requireView/canView` 를 거친다. 남의 사진·체크인은 404, 팔로우 수락 전 지도는 403.
- 팔로우는 한 방향이다. 내가 팔로우해도 상대가 내 지도를 보려면 따로 요청 → 내가 수락.
- 여권번호(`M` + 8자리)는 공개 id 다. 내부 id(`users.id`)는 API 에 내보내지 않는다.
- 비밀값(카카오 키)은 `.env`(git 제외) 또는 환경변수. `application.yml` 이 `optional:file:.env[.properties]` 로 읽는다.
- 카카오 디벨로퍼스 Redirect URI: `http://localhost:5173/login/oauth2/code/kakao`, `http://localhost:8080/login/oauth2/code/kakao` (+ 배포 도메인).
- 사용자에게 보이는 문구는 한국어 해요체.

## 로드맵 (다음 후보)

1. 기록 하나에 사진 여러 장 / 대표 사진 고르기
2. Flyway 도입 → prod `ddl-auto: validate`
3. 세션 저장소(재시작해도 로그인 유지: Spring Session JDBC), 팔로우 요청 알림
4. 사진 저장소 S3/R2 구현, 썸네일 생성
5. 더 잘게: 나라별 2단계 지역(일본 시정촌 등) — 지금은 한국만 시·군·구, 다른 나라는 1단계
