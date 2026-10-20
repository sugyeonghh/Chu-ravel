package com.sugyeong.churavel.follow;

import com.sugyeong.churavel.auth.LoginUser;
import com.sugyeong.churavel.visit.VisitResponse;
import com.sugyeong.churavel.visit.VisitService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 다른 여행자의 여권(공유 링크로 들어오는 곳)과, 수락된 팔로워에게만 보이는 지도. */
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
    @GetMapping("/{passportNo}")
    public FollowService.Profile profile(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo) {
        return followService.profile(me.userId(), passportNo);
    }

    /** 체크인 목록: 나 자신이거나 수락된 팔로워만. 아니면 403. */
    @GetMapping("/{passportNo}/visits")
    public List<VisitResponse> visits(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo) {
        return visitService.list(followService.viewable(me.userId(), passportNo).getId());
    }

    /** 한 칸과 그 기록들: 나 자신이거나 수락된 팔로워만 */
    @GetMapping("/{passportNo}/visits/{regionCode}")
    public VisitResponse visit(@AuthenticationPrincipal LoginUser me, @PathVariable String passportNo,
                               @PathVariable String regionCode) {
        return visitService.get(followService.viewable(me.userId(), passportNo).getId(), regionCode);
    }
}
