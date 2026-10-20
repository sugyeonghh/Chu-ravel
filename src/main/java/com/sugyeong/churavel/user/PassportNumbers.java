package com.sugyeong.churavel.user;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** 여권번호 발급기. 한국 여권처럼 M + 숫자 8자리, 겹치지 않을 때까지 다시 뽑는다. */
@Component
public class PassportNumbers {

    private final SecureRandom random = new SecureRandom();
    private final UserRepository users;

    public PassportNumbers(UserRepository users) {
        this.users = users;
    }

    public String next() {
        String no;
        do {
            no = "M" + String.format("%08d", random.nextInt(100_000_000));
        } while (users.existsByPassportNo(no));
        return no;
    }
}
