import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const API = "http://localhost:8080";

// npm run dev → http://localhost:5173, /api 와 카카오 로그인 경로는 bootRun 중인 Spring Boot(8080)로 넘긴다.
// 로그인 경로는 Host 헤더(localhost:5173)를 그대로 넘겨서, 카카오 redirect-uri 와 로그인 뒤 이동이 5173 으로 잡히게 한다.
// npm run build → dist/ (Gradle processResources 가 jar 의 static/ 에 넣는다)
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        target: API,
        // 서버가 꺼져 있으면 그냥 500 대신, 화면에 보여줄 문장을 ProblemDetail 로 돌려준다
        configure: (proxy) => proxy.on("error", (err, req, res) => {
          if (res.headersSent || res.writableEnded) return;
          res.writeHead(503, { "Content-Type": "application/problem+json" });
          res.end(JSON.stringify({ status: 503, detail: `API 서버(${API})가 꺼져 있어요. ./gradlew bootRun 을 먼저 켜 주세요` }));
        }),
      },
      // 문자열로 쓰면 Vite 가 changeOrigin: true 로 Host 를 8080 으로 바꿔 버린다
      "/oauth2": { target: API, changeOrigin: false },
      "/login/oauth2": { target: API, changeOrigin: false },
      // API 명세서: http://localhost:5173/swagger-ui.html (앱과 같은 주소라 로그인 쿠키·XSRF 쿠키를 그대로 쓴다)
      "/swagger-ui": API,
      "/v3/api-docs": API,
    },
  },
});
