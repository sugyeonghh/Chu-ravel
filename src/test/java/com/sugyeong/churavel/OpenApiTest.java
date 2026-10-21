package com.sugyeong.churavel;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API 명세서 (springdoc): 로그인 없이 열리고, 모든 API 가 들어 있다 */
class OpenApiTest extends ApiTestSupport {

    @Test
    void 명세서에_API_가_모두_있다() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("츄~♡레블맵 API"))
                .andExpect(jsonPath("$.paths['/api/me'].get.summary").value("내 여권"))
                .andExpect(jsonPath("$.paths['/api/visits/{regionCode}/entries'].post").exists())
                .andExpect(jsonPath("$.paths['/api/friends/{passportNo}/follow'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.tags[0]").value("로그인"))
                // 세션 사용자(@AuthenticationPrincipal LoginUser)는 요청 파라미터가 아니다
                .andExpect(content().string(not(containsString("LoginUser"))));
    }

    @Test
    void 응답_코드가_정리돼_있다() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                // API 마다 적은 것
                .andExpect(jsonPath("$.paths['/api/entries/{entryId}'].delete.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/entries/{entryId}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/travelers/{passportNo}/visits'].get.responses['403'].description").value("팔로우 요청이 수락되기 전이에요"))
                // 공통: 로그인 401, 바꾸는 요청 403(CSRF), 오류 본문은 ProblemDetail
                .andExpect(jsonPath("$.paths['/api/visits'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/visits'].get.responses['403']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/comments/{id}'].delete.responses['403'].description").value(containsString("X-XSRF-TOKEN")))
                .andExpect(jsonPath("$.paths['/api/travelers/{passportNo}'].get.responses['404'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                // 성공 응답 본문은 반환 타입에서
                .andExpect(jsonPath("$.paths['/api/me'].get.responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/Profile"))
                // 공개 API 는 로그인(401)·자물쇠가 없다
                .andExpect(jsonPath("$.paths['/api/regions'].get.responses['401']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/regions'].get.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['401'].description").value("아이디 또는 비밀번호가 맞지 않아요"));
    }

    @Test
    void 스웨거_화면이_열린다() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
