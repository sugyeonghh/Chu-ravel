package com.sugyeong.churavel.auth;

import com.sugyeong.churavel.follow.FollowService;
import com.sugyeong.churavel.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 아이디·비밀번호 가입·로그인 (카카오 말고). 성공하면 카카오 로그인과 같은 세션 쿠키가 생기고 내 여권을 돌려준다.
 * 세션에는 카카오 때와 같은 {@link LoginUser} 가 담기므로 다른 API 는 어느 쪽으로 로그인했는지 모른다.
 */
@RestController
@RequestMapping("/api/auth")
public class AccountController {

    public record SignupRequest(String username, String password, String nickname) {
    }

    public record LoginRequest(String username, String password) {
    }

    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
    private final AccountService accountService;
    private final FollowService followService;

    public AccountController(AccountService accountService, FollowService followService) {
        this.accountService = accountService;
        this.followService = followService;
    }

    /**
     * XSRF-TOKEN 쿠키 받기. 쿠키는 토큰을 꺼내 볼 때만 써지는데, 로그인 전 화면은 401 만 받아서 쿠키가 없다.
     * 프론트는 쿠키 없이 POST 하기 전에 여기를 한 번 부른다.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/signup")
    public FollowService.Profile signup(@RequestBody SignupRequest req, HttpServletRequest request, HttpServletResponse response) {
        return signIn(accountService.signup(req.username(), req.password(), req.nickname()), request, response);
    }

    @PostMapping("/login")
    public FollowService.Profile login(@RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        return signIn(accountService.login(req.username(), req.password()), request, response);
    }

    private FollowService.Profile signIn(User user, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId(); // 세션 고정 공격 막기
        }
        LoginUser principal = new LoginUser(user.getId(), user.getNickname(), user.getProfileImageUrl());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return followService.me(user.getId());
    }
}
