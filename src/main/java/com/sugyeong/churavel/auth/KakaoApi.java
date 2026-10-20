package com.sugyeong.churavel.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/** 카카오 API 직접 호출 (로그인 자체는 Spring Security 가 한다). */
@Component
public class KakaoApi {

    private static final Logger log = LoggerFactory.getLogger(KakaoApi.class);

    private final RestClient rest;

    public KakaoApi() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.rest = RestClient.builder().baseUrl("https://kapi.kakao.com").requestFactory(factory).build();
    }

    /** 카카오 쪽 액세스·리프레시 토큰을 만료시킨다. 실패해도 우리 로그아웃은 계속한다. */
    public void logout(String accessToken) {
        try {
            rest.post().uri("/v1/user/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("카카오 토큰 만료 요청 실패: {}", e.getMessage());
        }
    }
}
