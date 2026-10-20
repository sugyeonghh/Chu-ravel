# 츄레블맵

**Churavel Map** — travel 의 "트" 를 "츄"(chu)로 바꾼 이름이에요.

다녀온 지역을 누르면 사진과 날짜를 남기고, 사진은 그 지역 모양으로 지도에 붙어요.
붙인 사진을 누르면 블로그처럼 여행 글을 쓸 수 있어요.

## 실행

JDK 21 과 Node.js 20 이상(npm)이 필요해요. 프론트(React)는 Gradle 이 같이 빌드해요.

```bash
./gradlew bootRun
```

브라우저에서 http://localhost:8080 을 열면 돼요. 기록은 `./data/` 폴더(H2 DB 파일, 사진)에 남아요.

### 프론트 개발

`./gradlew bootRun` 을 켜 둔 채로:

```bash
cd frontend && npm install && npm run dev   # http://localhost:5173
```

### MySQL 로 실행

```bash
export DB_URL="jdbc:mysql://localhost:3306/churavel?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
export DB_USERNAME=churavel DB_PASSWORD=...
export PHOTO_DIR=/var/lib/churavel/photos
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## 지도 데이터

[southkorea-maps](https://github.com/southkorea/southkorea-maps) 의 KOSTAT 2013 시군구 경계를 `tools/map/gen.js` 로 SVG path 로 바꿔 써요.
독도는 실제 크기로는 보이지 않아서 서도·동도를 크게 그렸어요.

개발 메모와 API 는 [CLAUDE.md](CLAUDE.md) 에 있어요.
