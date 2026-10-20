package com.sugyeong.churavel.common;

/** 로그인은 했지만 볼 수 없는 것 (팔로우 수락 전 친구 지도, 남의 댓글 지우기 등). 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
