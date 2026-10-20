package com.sugyeong.churavel.follow;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    boolean existsByFollowerIdAndFolloweeIdAndStatus(Long followerId, Long followeeId, Follow.Status status);

    /** 내가 팔로우하는(하려는) 사람들 */
    @EntityGraph(attributePaths = "followee")
    List<Follow> findAllByFollowerIdOrderByRequestedAtDesc(Long followerId);

    /** 나를 팔로우하는(하려는) 사람들 */
    @EntityGraph(attributePaths = "follower")
    List<Follow> findAllByFolloweeIdOrderByRequestedAtDesc(Long followeeId);
}
