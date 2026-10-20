package com.sugyeong.churavel.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** 카카오로 로그인한 사용자. 사용자마다 자기 지도(체크인 목록)를 가진다. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 카카오 회원번호 (/v2/user/me 의 id) */
    @Column(nullable = false, unique = true)
    private Long kakaoId;

    /**
     * 여권번호 (M + 숫자 8자리). 친구 찾기·공유 링크에 쓰는 공개 id.
     * 이 칼럼이 생기기 전에 가입한 사용자는 다음 로그인 때 받으므로 DB 에서는 null 을 허용한다.
     */
    @Column(unique = true, length = 12)
    private String passportNo;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant lastLoginAt;

    protected User() {
    }

    public User(Long kakaoId, String passportNo, String nickname, String profileImageUrl) {
        this.kakaoId = kakaoId;
        this.passportNo = passportNo;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = Instant.now();
        this.lastLoginAt = this.createdAt;
    }

    /** 로그인할 때마다 카카오 프로필을 다시 받아 둔다. */
    public void loggedIn(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.lastLoginAt = Instant.now();
    }

    /** 여권번호가 없던 옛 사용자에게 한 번만 발급한다. */
    public void issuePassport(String passportNo) {
        if (this.passportNo == null) {
            this.passportNo = passportNo;
        }
    }

    public Long getId() { return id; }
    public Long getKakaoId() { return kakaoId; }
    public String getPassportNo() { return passportNo; }
    public String getNickname() { return nickname; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
}
