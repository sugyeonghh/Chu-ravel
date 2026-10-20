package com.sugyeong.churavel.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.authentication.logout.LogoutHandler;

/** 로그아웃할 때 카카오 토큰을 카카오 쪽에서 만료시키고, 우리 쪽에 들고 있던 토큰도 버린다. 세션을 버리기 전에 돈다. */
public class KakaoLogoutHandler implements LogoutHandler {

    static final String REGISTRATION_ID = "kakao";

    private final OAuth2AuthorizedClientRepository clients;
    private final KakaoApi kakaoApi;

    public KakaoLogoutHandler(OAuth2AuthorizedClientRepository clients, KakaoApi kakaoApi) {
        this.clients = clients;
        this.kakaoApi = kakaoApi;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication == null) {
            return;
        }
        OAuth2AuthorizedClient client = clients.loadAuthorizedClient(REGISTRATION_ID, authentication, request);
        if (client == null) {
            return;
        }
        kakaoApi.logout(client.getAccessToken().getTokenValue());
        clients.removeAuthorizedClient(REGISTRATION_ID, authentication, request, response);
    }
}
