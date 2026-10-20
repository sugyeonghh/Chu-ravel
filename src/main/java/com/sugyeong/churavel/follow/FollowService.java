package com.sugyeong.churavel.follow;

import com.sugyeong.churavel.common.ForbiddenException;
import com.sugyeong.churavel.common.NotFoundException;
import com.sugyeong.churavel.user.User;
import com.sugyeong.churavel.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 팔로우 요청 → 수락. 수락된 팔로워만 상대의 지도·사진·댓글을 볼 수 있다.
 * 남의 데이터를 볼 때는 항상 {@link #requireView} 를 거친다.
 */
@Service
public class FollowService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public enum Relation { SELF, FOLLOWING, REQUESTED, NONE }

    /** 여권 표지에 보이는 정보. canView 가 false 면 지도는 못 본다. */
    public record Profile(String passportNo, String nickname, String profileImageUrl, LocalDate issuedOn,
                          Relation relation, boolean followsMe, boolean canView) {
    }

    public record Friends(List<Traveler> following, List<Traveler> outgoing,
                          List<Traveler> followers, List<Traveler> incoming) {
    }

    private final FollowRepository follows;
    private final UserRepository users;

    public FollowService(FollowRepository follows, UserRepository users) {
        this.follows = follows;
        this.users = users;
    }

    public boolean canView(Long viewerId, Long ownerId) {
        return viewerId.equals(ownerId)
                || follows.existsByFollowerIdAndFolloweeIdAndStatus(viewerId, ownerId, Follow.Status.ACCEPTED);
    }

    public void requireView(Long viewerId, Long ownerId) {
        if (!canView(viewerId, ownerId)) {
            throw new ForbiddenException("팔로우 요청이 수락되면 볼 수 있어요");
        }
    }

    /** 여권번호로 상대를 찾아, 내 지도를 볼 수 있으면 그 사용자를 돌려준다. */
    @Transactional(readOnly = true)
    public User viewable(Long viewerId, String passportNo) {
        User owner = users.getByPassportNo(passportNo);
        requireView(viewerId, owner.getId());
        return owner;
    }

    @Transactional(readOnly = true)
    public Profile me(Long meId) {
        return profile(meId, users.findById(meId).orElseThrow(() -> new NotFoundException("사용자를 찾지 못했어요")));
    }

    @Transactional(readOnly = true)
    public Profile profile(Long meId, String passportNo) {
        User other = users.getByPassportNo(passportNo);
        return profile(meId, other);
    }

    private Profile profile(Long meId, User other) {
        Relation relation;
        if (other.getId().equals(meId)) {
            relation = Relation.SELF;
        } else {
            relation = follows.findByFollowerIdAndFolloweeId(meId, other.getId())
                    .map(f -> f.isAccepted() ? Relation.FOLLOWING : Relation.REQUESTED)
                    .orElse(Relation.NONE);
        }
        boolean followsMe = follows.existsByFollowerIdAndFolloweeIdAndStatus(other.getId(), meId, Follow.Status.ACCEPTED);
        return new Profile(other.getPassportNo(), other.getNickname(), other.getProfileImageUrl(),
                LocalDate.ofInstant(other.getCreatedAt(), SEOUL), relation, followsMe,
                relation == Relation.SELF || relation == Relation.FOLLOWING);
    }

    /** 팔로우 요청을 보낸다. 이미 보냈거나 팔로우 중이면 그대로 둔다. */
    @Transactional
    public Profile request(Long meId, String passportNo) {
        User other = users.getByPassportNo(passportNo);
        if (other.getId().equals(meId)) {
            throw new IllegalArgumentException("나 자신은 팔로우할 수 없어요");
        }
        if (follows.findByFollowerIdAndFolloweeId(meId, other.getId()).isEmpty()) {
            follows.save(new Follow(users.getReferenceById(meId), other));
        }
        return profile(meId, other);
    }

    /** 팔로우 끊기 / 보낸 요청 취소 */
    @Transactional
    public Profile unfollow(Long meId, String passportNo) {
        User other = users.getByPassportNo(passportNo);
        follows.findByFollowerIdAndFolloweeId(meId, other.getId()).ifPresent(follows::delete);
        return profile(meId, other);
    }

    /** 받은 요청 수락 */
    @Transactional
    public void accept(Long meId, String passportNo) {
        User other = users.getByPassportNo(passportNo);
        follows.findByFollowerIdAndFolloweeId(other.getId(), meId)
                .orElseThrow(() -> new NotFoundException(other.getNickname() + "님이 보낸 팔로우 요청이 없어요"))
                .accept();
    }

    /** 받은 요청 거절 / 팔로워 끊기 */
    @Transactional
    public void removeFollower(Long meId, String passportNo) {
        User other = users.getByPassportNo(passportNo);
        follows.findByFollowerIdAndFolloweeId(other.getId(), meId).ifPresent(follows::delete);
    }

    @Transactional(readOnly = true)
    public Friends friends(Long meId) {
        var mine = follows.findAllByFollowerIdOrderByRequestedAtDesc(meId);
        var theirs = follows.findAllByFolloweeIdOrderByRequestedAtDesc(meId);
        return new Friends(
                mine.stream().filter(Follow::isAccepted).map(f -> Traveler.of(f.getFollowee(), f.getAcceptedAt())).toList(),
                mine.stream().filter(f -> !f.isAccepted()).map(f -> Traveler.of(f.getFollowee(), f.getRequestedAt())).toList(),
                theirs.stream().filter(Follow::isAccepted).map(f -> Traveler.of(f.getFollower(), f.getAcceptedAt())).toList(),
                theirs.stream().filter(f -> !f.isAccepted()).map(f -> Traveler.of(f.getFollower(), f.getRequestedAt())).toList());
    }
}
