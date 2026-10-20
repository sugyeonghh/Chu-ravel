package com.sugyeong.churavel.auth;

import com.sugyeong.churavel.follow.FollowService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final FollowService followService;

    public MeController(FollowService followService) {
        this.followService = followService;
    }

    /**
     * 내 여권 (닉네임·사진·여권번호·발급일). 로그인 안 했으면 401 (SecurityConfig) → 프론트가 로그인 화면을 보여준다.
     * 여권번호는 로그인할 때 발급되므로 여기서는 항상 있다.
     */
    @GetMapping("/api/me")
    public FollowService.Profile me(@AuthenticationPrincipal LoginUser me) {
        return followService.me(me.userId());
    }
}
