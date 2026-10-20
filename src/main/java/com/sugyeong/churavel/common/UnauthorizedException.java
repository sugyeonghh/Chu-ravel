package com.sugyeong.churavel.common;

/** 아이디·비밀번호가 맞지 않을 때. 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
