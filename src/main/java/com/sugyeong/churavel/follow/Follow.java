package com.sugyeong.churavel.follow;

import com.sugyeong.churavel.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * follower 가 followee 의 지도를 보고 싶다는 관계. followee 가 수락해야(ACCEPTED) 볼 수 있다.
 * 한 방향이다: 내가 친구를 팔로우해도 친구가 내 지도를 보려면 따로 팔로우해야 한다.
 */
@Entity
@Table(name = "follow", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "followee_id"}))
public class Follow {

    public enum Status { PENDING, ACCEPTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false)
    private User followee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant acceptedAt;

    protected Follow() {
    }

    public Follow(User follower, User followee) {
        this.follower = follower;
        this.followee = followee;
        this.status = Status.PENDING;
        this.requestedAt = Instant.now();
    }

    public void accept() {
        if (status == Status.PENDING) {
            status = Status.ACCEPTED;
            acceptedAt = Instant.now();
        }
    }

    public boolean isAccepted() {
        return status == Status.ACCEPTED;
    }

    public Long getId() { return id; }
    public User getFollower() { return follower; }
    public User getFollowee() { return followee; }
    public Status getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
}
