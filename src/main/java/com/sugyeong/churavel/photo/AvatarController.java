package com.sugyeong.churavel.photo;

import com.sugyeong.churavel.auth.LoginUser;
import com.sugyeong.churavel.follow.FollowService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/** 프로필 사진 바꾸기·원래대로, 보기. 바꾸면 새 id(새 주소)가 생기므로 오래 캐시해도 된다. */
@RestController
public class AvatarController {

    private final AvatarService avatarService;
    private final PhotoService photoService;

    public AvatarController(AvatarService avatarService, PhotoService photoService) {
        this.avatarService = avatarService;
        this.photoService = photoService;
    }

    @PutMapping("/api/me/photo")
    public FollowService.Profile change(@AuthenticationPrincipal LoginUser me, @RequestParam("photo") MultipartFile photo) {
        return avatarService.change(me.userId(), photo);
    }

    @DeleteMapping("/api/me/photo")
    public FollowService.Profile reset(@AuthenticationPrincipal LoginUser me) {
        return avatarService.reset(me.userId());
    }

    @GetMapping("/api/avatars/{id}")
    public ResponseEntity<Resource> get(@PathVariable Long id) {
        Photo photo = avatarService.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
                .body(photoService.open(photo));
    }
}
