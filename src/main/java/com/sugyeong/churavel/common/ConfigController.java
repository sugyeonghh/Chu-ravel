package com.sugyeong.churavel.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트가 런타임에 받는 공개 설정 (로그인 없이). 다시 빌드하지 않고 .env·환경변수만 바꿔 쓰려고 서버에서 내려준다.
 * kakaoJsKey: 카카오톡 공유용 JavaScript 키. 브라우저에 드러나는 공개 키라 비밀이 아니다. 비어 있으면 카카오 공유를 숨긴다.
 */
@RestController
public class ConfigController {

    public record Config(String kakaoJsKey) {
    }

    private final Config config;

    public ConfigController(@Value("${churavel.kakao.js-key:}") String kakaoJsKey) {
        this.config = new Config(kakaoJsKey.isBlank() ? null : kakaoJsKey);
    }

    @GetMapping("/api/config")
    public Config config() {
        return config;
    }
}
