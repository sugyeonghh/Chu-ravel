package com.sugyeong.churavel.follow;

import com.sugyeong.churavel.auth.LoginUser;
import com.sugyeong.churavel.visit.VisitResponse;
import com.sugyeong.churavel.visit.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 다른 여행자의 여권(공유 링크로 들어오는 곳)과, 수락된 팔로워에게만 보이는 지도. */
@Tag(name = "친구", description = "팔로우는 한 방향: 요청 → 상대가 수락하면 상대 지도를 본다")
@RestController
@RequestMapping("/api/travelers")
public class TravelerController {

    private final FollowService followService;
    private final VisitService visitService;

    public TravelerController(FollowService followService, VisitService visitService) {
        this.followService = followService;
        this.visitService = visitService;
    }

    /** 여권 표지: 누구나(로그인한 사람) 볼 수 있다. 지도를 볼 수 있는지는 canView 로 알려 준다. */
    @Operation(summary = "남의 여권 표지", description = "relation(SELF/FOLLOWING/REQUESTED/NONE), followsMe, canView")
    @ApiResponse(responseCode = "200", description = "여권 표지 (canView 가 false 면 지도는 못 봐요)")
    @ApiResponse(responseCode = "404", description = "없는 여권번호")
    @GetMapping("/{passportNo}")
    public FollowService.Profile profile(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo) {
        return followService.profile(me.userId(), passportNo);
    }

    /** 체크인 목록: 나 자신이거나 수락된 팔로워만. 아니면 403. */
    @Operation(summary = "남의 체크인 목록", description = "수락된 팔로워만, 아니면 403")
    @ApiResponse(responseCode = "200", description = "체크인 요약 목록")
    @ApiResponse(responseCode = "403", description = "팔로우 요청이 수락되기 전이에요")
    @ApiResponse(responseCode = "404", description = "없는 여권번호")
    @GetMapping("/{passportNo}/visits")
    public List<VisitResponse> visits(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo) {
        return visitService.list(followService.viewable(me.userId(), passportNo).getId());
    }

    /** 한 칸과 그 기록들: 나 자신이거나 수락된 팔로워만 */
    @Operation(summary = "남의 한 칸 + 기록들", description = "수락된 팔로워만")
    @ApiResponse(responseCode = "200", description = "체크인 + 기록들")
    @ApiResponse(responseCode = "403", description = "팔로우 요청이 수락되기 전이에요")
    @ApiResponse(responseCode = "404", description = "없는 여권번호·지역 코드이거나 체크인이 없어요")
    @GetMapping("/{passportNo}/visits/{regionCode}")
    public VisitResponse visit(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo,
                               @PathVariable String regionCode) {
        return visitService.get(followService.viewable(me.userId(), passportNo).getId(), regionCode);
    }
}
