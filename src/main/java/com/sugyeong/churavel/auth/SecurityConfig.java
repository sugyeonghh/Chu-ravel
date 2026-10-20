package com.sugyeong.churavel.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;

/**
 * 카카오 OAuth2 로그인 또는 아이디·비밀번호 로그인(AccountController) + 세션 쿠키.
 * - 로그인: 프론트가 /oauth2/authorization/kakao 로 보내면 카카오 → /login/oauth2/code/kakao → "/" 로 돌아온다.
 *   브라우저에 카카오 로그인이 남아 있어도 매번 카카오 계정을 다시 묻는다 (prompt=login).
 * - 카카오 토큰은 세션에 둔다 (HttpSessionOAuth2AuthorizedClientRepository). 세션이 사라지면 같이 사라진다.
 * - 로그아웃: 카카오 토큰 만료 → 세션·쿠키 삭제 → Clear-Site-Data 로 브라우저 캐시(사진)·저장소까지 지운다.
 * - 아이디 로그인: POST /api/auth/signup·login (JSON) → 같은 세션에 LoginUser 를 넣는다.
 * - /api/** 는 로그인 필요 (지역 코드표, 공개 설정, 아이디 가입·로그인만 공개). 로그인 안 했으면 리다이렉트 대신 401 ProblemDetail.
 * - CSRF: XSRF-TOKEN 쿠키 → 프론트가 X-XSRF-TOKEN 헤더로 돌려보낸다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            KakaoOAuth2UserService kakaoUserService,
                                            ClientRegistrationRepository registrations,
                                            OAuth2AuthorizedClientRepository authorizedClients,
                                            KakaoApi kakaoApi) throws Exception {
        RequestMatcher api = PathPatternRequestMatcher.withDefaults().matcher("/api/**");

        var authorizationRequests = new DefaultOAuth2AuthorizationRequestResolver(
                registrations, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        authorizationRequests.setAuthorizationRequestCustomizer(c -> c.additionalParameters(p -> p.put("prompt", "login")));

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/regions")).permitAll()
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/auth/**")).permitAll()
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/config")).permitAll()
                        .requestMatchers(api).authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(login -> login
                        .authorizationEndpoint(a -> a.authorizationRequestResolver(authorizationRequests))
                        .authorizedClientRepository(authorizedClients)
                        .userInfoEndpoint(info -> info.userService(kakaoUserService))
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/?login=fail"))
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .addLogoutHandler(new KakaoLogoutHandler(authorizedClients, kakaoApi))
                        // ClearSiteDataHeaderWriter 는 HTTPS 요청에만 쓰는데, 브라우저는 http://localhost 에서도 받아 준다
                        .addLogoutHandler((req, res, auth) -> res.setHeader("Clear-Site-Data", "\"cache\", \"cookies\", \"storage\""))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .csrf(csrf -> csrf.spa())
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                (req, res, e) -> problem(res, HttpStatus.UNAUTHORIZED, "로그인이 필요해요"), api)
                        .defaultAccessDeniedHandlerFor(
                                (req, res, e) -> problem(res, HttpStatus.FORBIDDEN, "요청이 거부됐어요. 새로고침한 뒤 다시 해 주세요"), api));
        return http.build();
    }

    /** ApiExceptionHandler 와 같은 ProblemDetail 모양. 필터 단계라 직접 쓴다. */
    private static void problem(HttpServletResponse res, HttpStatus status, String detail) throws IOException {
        res.setStatus(status.value());
        res.setContentType("application/problem+json;charset=UTF-8");
        res.getWriter().write("{\"status\":" + status.value()
                + ",\"title\":\"" + status.getReasonPhrase()
                + "\",\"detail\":\"" + detail + "\"}");
    }
}
