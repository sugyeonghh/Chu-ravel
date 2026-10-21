package com.sugyeong.churavel.auth;

import com.sugyeong.churavel.follow.FollowService;
import com.sugyeong.churavel.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "로그인", description = "아이디·비밀번호 가입·로그인. 카카오 로그인은 브라우저로 /oauth2/authorization/kakao 를 연다")
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
    @Operation(summary = "CSRF 쿠키 받기", description = "XSRF-TOKEN 쿠키를 받는다 (204). PUT·POST·DELETE 는 이 값을 X-XSRF-TOKEN 헤더로 보내야 한다")
    @ApiResponse(responseCode = "204", description = "XSRF-TOKEN 쿠키를 심었어요")
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(@Parameter(hidden = true) CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "아이디로 가입", description = "영문 소문자·숫자·_ 4~20자 아이디, 8자~72바이트 비밀번호, 1~20자 닉네임. 성공하면 바로 로그인되고 내 여권을 돌려준다")
    @ApiResponse(responseCode = "200", description = "가입하고 로그인됐어요. 내 여권")
    @ApiResponse(responseCode = "400", description = "아이디·비밀번호·닉네임 규칙에 안 맞거나 이미 쓰고 있는 아이디")
    @PostMapping("/signup")
    public FollowService.Profile signup(@RequestBody SignupRequest req, HttpServletRequest request, HttpServletResponse response) {
        return signIn(accountService.signup(req.username(), req.password(), req.nickname()), request, response);
    }

    @Operation(summary = "아이디로 로그인", description = "성공하면 세션 쿠키가 생기고 내 여권을 돌려준다. 틀리면 401")
    @ApiResponse(responseCode = "200", description = "로그인됐어요. 내 여권")
    @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호가 맞지 않아요")
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
