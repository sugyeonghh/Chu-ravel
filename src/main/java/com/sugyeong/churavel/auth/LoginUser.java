package com.sugyeong.churavel.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 세션에 담기는 로그인 사용자. 컨트롤러에서 {@code @AuthenticationPrincipal LoginUser me} 로 받는다.
 * 카카오 응답 전체 대신 우리 사용자 id 와 화면에 보여줄 프로필만 들고 있다.
 */
public record LoginUser(Long userId, String nickname, String profileImageUrl) implements OAuth2User, Serializable {

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of("userId", userId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
