package com.sugyeong.churavel;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 아이디·비밀번호 가입·로그인 */
class AccountApiTest extends ApiTestSupport {

    private MockHttpSession signup(String username, String password, String nickname) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(nickname))
                .andExpect(jsonPath("$.passportNo").isString())
                .andReturn().getRequest().getSession();
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void 가입하면_바로_로그인된다() throws Exception {
        MockHttpSession session = signup("traveler01", "password123", "여행가");
        mvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("여행가"))
                .andExpect(jsonPath("$.relation").value("SELF"));
        mvc.perform(get("/api/visits").session(session)).andExpect(status().isOk());
    }

    @Test
    void 아이디와_비밀번호로_로그인한다() throws Exception {
        signup("traveler02", "password123", "둘째");
        MockHttpSession session = (MockHttpSession) login("Traveler02 ", "password123")  // 대문자·공백은 정리한다
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("둘째"))
                .andReturn().getRequest().getSession();
        mvc.perform(get("/api/me").session(session)).andExpect(jsonPath("$.nickname").value("둘째"));
    }

    @Test
    void 틀린_비밀번호나_없는_아이디는_401() throws Exception {
        signup("traveler03", "password123", "셋째");
        login("traveler03", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("아이디 또는 비밀번호가 맞지 않아요"));
        login("nobody_here", "password123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("아이디 또는 비밀번호가 맞지 않아요"));
    }

    @Test
    void 가입_규칙() throws Exception {
        signup("traveler04", "password123", "넷째");
        String[][] bad = {
                {"traveler04", "password123", "또넷째", "이미 쓰고 있는 아이디예요"},
                {"ab", "password123", "짧은", "아이디는 영문 소문자·숫자·밑줄(_)로 4~20자예요"},
                {"한글아이디", "password123", "한글", "아이디는 영문 소문자·숫자·밑줄(_)로 4~20자예요"},
                {"traveler05", "short", "짧은비번", "비밀번호는 8자 이상이에요"},
                {"traveler06", "password123", " ", "닉네임은 1~20자예요"},
        };
        for (String[] b : bad) {
            mvc.perform(post("/api/auth/signup").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + b[0] + "\",\"password\":\"" + b[1] + "\",\"nickname\":\"" + b[2] + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(b[3]));
        }
    }

    @Test
    void 카카오_사용자는_비밀번호로_로그인할_수_없다() throws Exception {
        user(4001L, "카카오친구");
        login("4001", "password123").andExpect(status().isUnauthorized());
    }

    @Test
    void 로그인_전에도_CSRF_토큰을_받을_수_있다() throws Exception {
        // 로그인 화면은 /api/me(401) 만 받아서 XSRF-TOKEN 쿠키가 없다 → 가입·로그인 POST 전에 /api/auth/csrf 로 받는다.
        // 쿠키 자체는 여기서 확인하지 않는다: 테스트의 csrf() 가 필터의 토큰 저장소를 세션용으로 바꿔 끼워서.
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isNoContent());
    }

    @Test
    void 공개_설정은_로그인_없이_받는다() throws Exception {
        // 테스트에는 KAKAO_JS_KEY 가 없다 → null (프론트는 카카오 공유를 숨긴다)
        mvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kakaoJsKey").doesNotExist());
    }
}
