package com.sugyeong.churavel.follow;

import com.sugyeong.churavel.user.User;

import java.time.Instant;

/** 친구 목록·댓글 작성자 등 화면에 보여줄 다른 여행자 */
public record Traveler(String passportNo, String nickname, String profileImageUrl, Instant since) {

    public static Traveler of(User u) {
        return of(u, null);
    }

    public static Traveler of(User u, Instant since) {
        return new Traveler(u.getPassportNo(), u.getNickname(), u.getProfileImageUrl(), since);
    }
}
