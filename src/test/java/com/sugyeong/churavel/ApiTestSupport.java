package com.sugyeong.churavel;

import com.sugyeong.churavel.auth.KakaoApi;
import com.sugyeong.churavel.auth.LoginUser;
import com.sugyeong.churavel.user.User;
import com.sugyeong.churavel.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * API 통합 테스트 공통: H2 메모리 DB, 임시 사진 폴더, 가짜 카카오 키.
 * 모든 테스트 클래스가 같은 설정이라 스프링 컨텍스트(와 DB)를 하나로 같이 쓴다 → 클래스마다 kakaoId 를 다르게.
 */
@SpringBootTest
abstract class ApiTestSupport {

    private static final Path PHOTO_DIR;

    static {
        try {
            PHOTO_DIR = Files.createTempDirectory("churavel-test-photos");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        r.add("churavel.storage.photo-dir", PHOTO_DIR::toString);
        r.add("KAKAO_CLIENT_ID", () -> "test-client-id");
        r.add("KAKAO_CLIENT_SECRET", () -> "test-client-secret");
    }

    @Autowired
    WebApplicationContext context;

    @Autowired
    UserRepository users;

    /** 테스트에서 진짜 카카오를 부르지 않는다 */
    @MockitoBean
    KakaoApi kakaoApi;

    MockMvc mvc;

    @BeforeEach
    void setUpMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** 여권번호는 kakaoId 로 만든다 (M + 8자리) */
    User user(long kakaoId, String nickname) {
        return users.findByKakaoId(kakaoId).orElseGet(() ->
                users.save(new User(kakaoId, "M" + String.format("%08d", kakaoId), nickname, null)));
    }

    /** 카카오 로그인한 세션 + CSRF 토큰 */
    static RequestPostProcessor as(User u) {
        var login = oauth2Login().oauth2User(new LoginUser(u.getId(), u.getNickname(), u.getProfileImageUrl()));
        return req -> csrf().postProcessRequest(login.postProcessRequest(req));
    }

    static MockMultipartFile jpeg(String name) {
        return new MockMultipartFile("photo", name, "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, 1, 2, 3});
    }
}
