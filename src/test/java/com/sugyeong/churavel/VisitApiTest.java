package com.sugyeong.churavel;

import com.jayway.jsonpath.JsonPath;
import com.sugyeong.churavel.auth.LoginUser;
import com.sugyeong.churavel.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 체크인 안의 기록(사진+글) 여러 개, 로그인·로그아웃 */
class VisitApiTest extends ApiTestSupport {

    @Autowired
    ClientRegistrationRepository registrations;

    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        alice = user(1001L, "앨리스");
        bob = user(1002L, "밥");
    }

    private static MockMultipartHttpServletRequestBuilder newEntry(String code) {
        return multipart(HttpMethod.POST, "/api/visits/{code}/entries", code);
    }

    private static MockMultipartHttpServletRequestBuilder editEntry(Object id) {
        return multipart(HttpMethod.PUT, "/api/entries/{id}", id);
    }

    @Test
    void 한_지역에_사진과_글을_여러개_올리고_고치고_지운다() throws Exception {
        // 강릉시(3203) 첫 기록 → 체크인이 생긴다
        String first = mvc.perform(newEntry("3203").file(jpeg("anmok.jpg"))
                        .param("visitedOn", "2026-08-15").param("title", "안목해변 커피").param("body", "바다 보면서 커피")
                        .with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionName").value("강릉시"))
                .andExpect(jsonPath("$.province").value("강원"))
                .andExpect(jsonPath("$.entryCount").value(1))
                .andExpect(jsonPath("$.entries[0].title").value("안목해변 커피"))
                .andExpect(jsonPath("$.entries[0].visitedOn").value("2026-08-15"))
                .andReturn().getResponse().getContentAsString();
        Integer e1 = JsonPath.read(first, "$.entries[0].id");
        String photo1 = JsonPath.read(first, "$.entries[0].photoUrl");
        mvc.perform(get(photo1).with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));

        // 두 번째 기록 (더 나중 날짜) → 지도 사진은 최근 것, 방문일은 처음 간 날
        String second = mvc.perform(newEntry("3203").file(jpeg("ojukheon.jpg"))
                        .param("visitedOn", "2026-09-01").param("title", "오죽헌")
                        .with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryCount").value(2))
                .andExpect(jsonPath("$.visitedOn").value("2026-08-15"))
                .andExpect(jsonPath("$.title").value("오죽헌"))
                .andExpect(jsonPath("$.entries[0].title").value("오죽헌"))   // 최근 것부터
                .andExpect(jsonPath("$.entries[1].title").value("안목해변 커피"))
                .andReturn().getResponse().getContentAsString();
        Integer e2 = JsonPath.read(second, "$.entries[0].id");
        String photo2 = JsonPath.read(second, "$.entries[0].photoUrl");
        assertThat((String) JsonPath.read(second, "$.photoUrl")).isEqualTo(photo2);

        // 첫 기록 고치기: 제목만 (본문은 그대로)
        mvc.perform(editEntry(e1).param("title", "안목해변").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[1].title").value("안목해변"))
                .andExpect(jsonPath("$.entries[1].body").value("바다 보면서 커피"));

        // 첫 기록 사진 바꾸기 → 옛 사진은 사라진다
        String swapped = mvc.perform(editEntry(e1).file(jpeg("anmok2.jpg")).with(as(alice)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(swapped, "$.entries[1].photoUrl")).isNotEqualTo(photo1);
        mvc.perform(get(photo1).with(as(alice))).andExpect(status().isNotFound());

        // 두 번째 기록 지우기 → 지도 사진이 첫 기록 사진으로
        String afterDelete = mvc.perform(delete("/api/entries/{id}", e2).with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryCount").value(1))
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(afterDelete, "$.photoUrl")).isEqualTo(JsonPath.read(swapped, "$.entries[1].photoUrl"));
        mvc.perform(get(photo2).with(as(alice))).andExpect(status().isNotFound());

        mvc.perform(get("/api/visits").with(as(alice)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].entryCount").value(1))
                .andExpect(jsonPath("$[0].entries").doesNotExist());

        // 마지막 기록을 지우면 체크인도 사라진다
        mvc.perform(delete("/api/entries/{id}", e1).with(as(alice))).andExpect(status().isNoContent());
        mvc.perform(get("/api/visits/3203").with(as(alice))).andExpect(status().isNotFound());
    }

    @Test
    void 체크인을_통째로_지우면_기록과_사진이_모두_사라진다() throws Exception {
        String r1 = mvc.perform(newEntry("1102").file(jpeg("a.jpg")).with(as(alice))).andReturn().getResponse().getContentAsString();
        String r2 = mvc.perform(newEntry("1102").file(jpeg("b.jpg")).with(as(alice))).andReturn().getResponse().getContentAsString();
        mvc.perform(delete("/api/visits/1102").with(as(alice))).andExpect(status().isNoContent());
        mvc.perform(get((String) JsonPath.read(r1, "$.entries[0].photoUrl")).with(as(alice))).andExpect(status().isNotFound());
        mvc.perform(get((String) JsonPath.read(r2, "$.entries[0].photoUrl")).with(as(alice))).andExpect(status().isNotFound());
    }

    @Test
    void 지도는_사용자마다_따로다() throws Exception {
        String body = mvc.perform(newEntry("1101").file(jpeg("a.jpg")).with(as(alice)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String photoUrl = JsonPath.read(body, "$.photoUrl");
        Integer entryId = JsonPath.read(body, "$.entries[0].id");

        // 밥에게는 안 보이고, 사진도 못 보고, 기록도 못 고친다
        mvc.perform(get("/api/visits/1101").with(as(bob))).andExpect(status().isNotFound());
        mvc.perform(get(photoUrl).with(as(bob))).andExpect(status().isNotFound());
        mvc.perform(editEntry(entryId).param("title", "해킹").with(as(bob))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/entries/{id}", entryId).with(as(bob))).andExpect(status().isNotFound());

        // 밥도 같은 지역에 따로 기록할 수 있다
        mvc.perform(newEntry("1101").file(jpeg("b.jpg")).with(as(bob))).andExpect(status().isOk());
        mvc.perform(get(photoUrl).with(as(alice))).andExpect(status().isOk());
    }

    @Test
    void 로그인_안_하면_401() throws Exception {
        mvc.perform(get("/api/visits"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("로그인이 필요해요"));
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        // 지역 코드표는 공개
        mvc.perform(get("/api/regions")).andExpect(status().isOk());
    }

    @Test
    void CSRF_토큰이_없으면_403() throws Exception {
        var login = oauth2Login().oauth2User(new LoginUser(alice.getId(), alice.getNickname(), null));
        mvc.perform(newEntry("1101").file(jpeg("x.jpg")).with(login))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void 카카오_로그인은_매번_계정을_다시_묻는다() throws Exception {
        mvc.perform(get("/oauth2/authorization/kakao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("prompt=login")));
    }

    @Test
    void 로그아웃하면_카카오_토큰과_세션과_브라우저_데이터를_모두_지운다() throws Exception {
        // 로그인하면 HttpSessionOAuth2AuthorizedClientRepository 가 세션에 카카오 토큰을 둔다
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "kakao-access-token",
                Instant.now(), Instant.now().plusSeconds(3600));
        var kakaoClient = new OAuth2AuthorizedClient(registrations.findByRegistrationId("kakao"), alice.getId().toString(), token);
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionOAuth2AuthorizedClientRepository.class.getName() + ".AUTHORIZED_CLIENTS",
                new HashMap<>(Map.of("kakao", kakaoClient)));

        mvc.perform(post("/api/logout").session(session).with(as(alice)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("JSESSIONID", 0))
                .andExpect(header().string("Clear-Site-Data", containsString("\"cache\"")));
        assertThat(session.isInvalid()).isTrue();
        verify(kakaoApi).logout("kakao-access-token");
    }

    @Test
    void 내_정보() throws Exception {
        mvc.perform(get("/api/me").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("앨리스"));
    }

    @Test
    void 독도도_체크인할_수_있다() throws Exception {
        mvc.perform(newEntry("DOKDO").file(jpeg("dokdo.jpg")).with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionName").value("독도"));
    }

    @Test
    void 기록에_사진이_없으면_400() throws Exception {
        mvc.perform(newEntry("1111").param("visitedOn", "2026-01-01").param("title", "사진 없음").with(as(alice)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void 없는_지역은_404() throws Exception {
        mvc.perform(newEntry("9999").file(jpeg("x.jpg")).with(as(alice)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 이미지가_아니면_400() throws Exception {
        var txt = new MockMultipartFile("photo", "a.txt", "text/plain", "hello".getBytes());
        mvc.perform(newEntry("1111").file(txt).with(as(alice)))
                .andExpect(status().isBadRequest());
    }
}
