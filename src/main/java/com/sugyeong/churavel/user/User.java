package com.sugyeong.churavel.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** 카카오나 아이디·비밀번호로 로그인한 사용자. 사용자마다 자기 지도(체크인 목록)를 가진다. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 카카오 회원번호 (/v2/user/me 의 id). 아이디로 가입한 사용자는 null */
    @Column(unique = true)
    private Long kakaoId;

    /** 로그인 아이디 (영문 소문자·숫자·밑줄). 카카오로 가입한 사용자는 null */
    @Column(unique = true, length = 20)
    private String username;

    /** 비밀번호 해시 ({bcrypt}…). 아이디로 가입한 사용자만 */
    @Column(length = 100)
    private String passwordHash;

    /**
     * 여권번호 (M + 숫자 8자리). 친구 찾기·공유 링크에 쓰는 공개 id.
     * 이 칼럼이 생기기 전에 가입한 사용자는 다음 로그인 때 받으므로 DB 에서는 null 을 허용한다.
     */
    @Column(unique = true, length = 12)
    private String passportNo;

    @Column(nullable = false, length = 100)
    private String nickname;

    /** 카카오 프로필 사진 URL. 로그인할 때마다 카카오에서 다시 받는다 */
    @Column(length = 500)
    private String profileImageUrl;

    /** 직접 올린 프로필 사진 (photo.id). 있으면 카카오 사진 대신 보여준다 */
    private Long profilePhotoId;

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

    /** 아이디·비밀번호로 가입. 프로필 사진은 없다. */
    public static User withPassword(String username, String passwordHash, String passportNo, String nickname) {
        User user = new User(null, passportNo, nickname, null);
        user.username = username;
        user.passwordHash = passwordHash;
        return user;
    }

    /** 로그인할 때마다 카카오 프로필을 다시 받아 둔다. */
    public void loggedIn(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.lastLoginAt = Instant.now();
    }

    /** 아이디로 로그인했을 때. 프로필은 그대로 둔다. */
    public void loggedIn() {
        this.lastLoginAt = Instant.now();
    }

    /** 직접 올린 사진으로 바꾸거나(photoId) 지운다(null). 전에 올려 둔 사진 id 를 돌려준다 (지울 수 있게). */
    public Long changeProfilePhoto(Long photoId) {
        Long old = this.profilePhotoId;
        this.profilePhotoId = photoId;
        return old;
    }

    /** 여권번호가 없던 옛 사용자에게 한 번만 발급한다. */
    public void issuePassport(String passportNo) {
        if (this.passportNo == null) {
            this.passportNo = passportNo;
        }
    }

    public Long getId() { return id; }
    public Long getKakaoId() { return kakaoId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getPassportNo() { return passportNo; }
    public String getNickname() { return nickname; }
    /** 화면에 보여줄 프로필 사진: 직접 올린 사진 → 카카오 사진 → 없음(null) */
    public String getProfileImageUrl() {
        return profilePhotoId != null ? "/api/avatars/" + profilePhotoId : profileImageUrl;
    }
    public Long getProfilePhotoId() { return profilePhotoId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
}
