package com.sugyeong.churavel.auth;

import com.sugyeong.churavel.common.UnauthorizedException;
import com.sugyeong.churavel.user.PassportNumbers;
import com.sugyeong.churavel.user.User;
import com.sugyeong.churavel.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 아이디·비밀번호 가입과 로그인. 카카오 계정과는 따로인 계정이다 (같은 사람이라도 지도가 둘).
 * 비밀번호는 {bcrypt} 해시로만 둔다.
 */
@Service
public class AccountService {

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9_]{4,20}");
    private static final int NICKNAME_MAX = 20;
    private static final String WRONG = "아이디 또는 비밀번호가 맞지 않아요";

    private final PasswordEncoder passwords = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    /** 없는 아이디여도 비밀번호를 한 번 맞춰 봐서, 응답 시간으로 아이디가 있는지 알 수 없게 한다 */
    private final String dummyHash = passwords.encode("churavel-dummy-password");
    private final UserRepository users;
    private final PassportNumbers passportNumbers;

    public AccountService(UserRepository users, PassportNumbers passportNumbers) {
        this.users = users;
        this.passportNumbers = passportNumbers;
    }

    @Transactional
    public User signup(String username, String password, String nickname) {
        String id = normalize(username);
        if (!USERNAME.matcher(id).matches()) {
            throw new IllegalArgumentException("아이디는 영문 소문자·숫자·밑줄(_)로 4~20자예요");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이에요");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) { // bcrypt 한도
            throw new IllegalArgumentException("비밀번호가 너무 길어요");
        }
        String name = nickname == null ? "" : nickname.strip();
        if (name.isEmpty() || name.length() > NICKNAME_MAX) {
            throw new IllegalArgumentException("닉네임은 1~" + NICKNAME_MAX + "자예요");
        }
        if (users.existsByUsername(id)) {
            throw new IllegalArgumentException("이미 쓰고 있는 아이디예요");
        }
        try {
            return users.saveAndFlush(User.withPassword(id, passwords.encode(password), passportNumbers.next(), name));
        } catch (DataIntegrityViolationException e) { // 같은 아이디로 동시에 가입
            throw new IllegalArgumentException("이미 쓰고 있는 아이디예요");
        }
    }

    @Transactional
    public User login(String username, String password) {
        User user = users.findByUsername(normalize(username)).orElse(null);
        String raw = password == null ? "" : password;
        if (user == null || user.getPasswordHash() == null) {
            passwords.matches(raw, dummyHash);
            throw new UnauthorizedException(WRONG);
        }
        if (!passwords.matches(raw, user.getPasswordHash())) {
            throw new UnauthorizedException(WRONG);
        }
        user.loggedIn();
        return user;
    }

    private static String normalize(String username) {
        return username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
    }
}
