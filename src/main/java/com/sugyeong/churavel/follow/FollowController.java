package com.sugyeong.churavel.follow;

import com.sugyeong.churavel.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 친구는 여권번호(passportNo)로 가리킨다. */
@Tag(name = "친구", description = "팔로우는 한 방향: 요청 → 상대가 수락하면 상대 지도를 본다")
@RestController
@RequestMapping("/api/friends")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    /** 팔로잉 / 보낸 요청 / 팔로워 / 받은 요청 */
    @Operation(summary = "친구 목록", description = "팔로잉·보낸 요청·팔로워·받은 요청")
    @ApiResponse(responseCode = "200", description = "팔로잉·보낸 요청·팔로워·받은 요청")
    @GetMapping
    public FollowService.Friends friends(@AuthenticationPrincipal LoginUser me) {
        return followService.friends(me.userId());
    }

    @Operation(summary = "팔로우 요청")
    @ApiResponse(responseCode = "200", description = "상대 여권 (relation = REQUESTED, 이미 팔로우 중이면 그대로)")
    @ApiResponse(responseCode = "400", description = "나 자신은 팔로우할 수 없어요")
    @ApiResponse(responseCode = "404", description = "없는 여권번호")
    @PostMapping("/{passportNo}/follow")
    public FollowService.Profile follow(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo) {
        return followService.request(me.userId(), passportNo);
    }

    @Operation(summary = "팔로우 끊기·요청 취소")
    @ApiResponse(responseCode = "200", description = "상대 여권 (relation = NONE)")
    @ApiResponse(responseCode = "404", description = "없는 여권번호")
    @DeleteMapping("/{passportNo}/follow")
    public FollowService.Profile unfollow(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo) {
        return followService.unfollow(me.userId(), passportNo);
    }

    @Operation(summary = "받은 요청 수락")
    @ApiResponse(responseCode = "204", description = "수락했어요. 이제 상대가 내 지도를 봐요")
    @ApiResponse(responseCode = "404", description = "없는 여권번호이거나 그 사람이 보낸 요청이 없어요")
    @PostMapping("/{passportNo}/accept")
    public ResponseEntity<Void> accept(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo) {
        followService.accept(me.userId(), passportNo);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "받은 요청 거절·팔로워 끊기")
    @ApiResponse(responseCode = "204", description = "끊었어요")
    @ApiResponse(responseCode = "404", description = "없는 여권번호")
    @DeleteMapping("/{passportNo}/follower")
    public ResponseEntity<Void> removeFollower(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo) {
        followService.removeFollower(me.userId(), passportNo);
        return ResponseEntity.noContent().build();
    }
}
