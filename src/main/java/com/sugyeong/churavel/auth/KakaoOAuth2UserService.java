package com.sugyeong.churavel.auth;

import com.sugyeong.churavel.user.PassportNumbers;
import com.sugyeong.churavel.user.User;
import com.sugyeong.churavel.user.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 카카오 사용자 정보(/v2/user/me)를 받아 우리 사용자로 가입·갱신하고 {@link LoginUser} 로 바꾼다.
 * 응답 모양: { id, kakao_account: { profile: { nickname, profile_image_url } }, properties: { nickname, profile_image } }
 * DefaultOAuth2UserService 를 상속하면 @Transactional 프록시가 final 메서드에서 경고를 내서, 안에 들고 위임한다.
 */
@Service
public class KakaoOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String DEFAULT_NICKNAME = "여행자";

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> kakao = new DefaultOAuth2UserService();
    private final UserRepository users;
    private final PassportNumbers passportNumbers;

    public KakaoOAuth2UserService(UserRepository users, PassportNumbers passportNumbers) {
        this.users = users;
        this.passportNumbers = passportNumbers;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User kakaoUser = kakao.loadUser(request);
        Long kakaoId = ((Number) kakaoUser.getAttributes().get("id")).longValue();

        Map<String, Object> profile = map(map(kakaoUser.getAttributes().get("kakao_account")).get("profile"));
        Map<String, Object> properties = map(kakaoUser.getAttributes().get("properties"));
        String nickname = firstText(profile.get("nickname"), properties.get("nickname"), DEFAULT_NICKNAME);
        String image = firstText(profile.get("profile_image_url"), properties.get("profile_image"), null);
        if (image != null && image.startsWith("http://")) {
            image = "https://" + image.substring("http://".length()); // 카카오 CDN 이 http 로 줄 때가 있다 (https 페이지에서 막힘)
        }
        String profileImage = image;

        User user = users.findByKakaoId(kakaoId)
                .map(u -> {
                    u.loggedIn(nickname, profileImage);
                    if (u.getPassportNo() == null) {
                        u.issuePassport(passportNumbers.next());
                    }
                    return u;
                })
                .orElseGet(() -> users.save(new User(kakaoId, passportNumbers.next(), nickname, profileImage)));
        return new LoginUser(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static String firstText(Object a, Object b, String fallback) {
        if (a instanceof String s && !s.isBlank()) return s;
        if (b instanceof String s && !s.isBlank()) return s;
        return fallback;
    }
}
