package com.sugyeong.churavel.follow;

import com.sugyeong.churavel.auth.LoginUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 친구는 여권번호(passportNo)로 가리킨다. */
@RestController
@RequestMapping("/api/friends")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    /** 팔로잉 / 보낸 요청 / 팔로워 / 받은 요청 */
    @GetMapping
    public FollowService.Friends friends(@AuthenticationPrincipal LoginUser me) {
        return followService.friends(me.userId());
    }

    @PostMapping("/{passportNo}/follow")
    public FollowService.Profile follow(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo) {
        return followService.request(me.userId(), passportNo);
    }

    @DeleteMapping("/{passportNo}/follow")
    public FollowService.Profile unfollow(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo) {
        return followService.unfollow(me.userId(), passportNo);
    }

    @PostMapping("/{passportNo}/accept")
    public ResponseEntity<Void> accept(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo) {
        followService.accept(me.userId(), passportNo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{passportNo}/follower")
    public ResponseEntity<Void> removeFollower(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo) {
        followService.removeFollower(me.userId(), passportNo);
        return ResponseEntity.noContent().build();
    }
}
