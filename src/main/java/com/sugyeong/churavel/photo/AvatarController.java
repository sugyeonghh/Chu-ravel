package com.sugyeong.churavel.photo;

import com.sugyeong.churavel.auth.LoginUser;
import com.sugyeong.churavel.follow.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "내 여권", description = "내 프로필·프로필 사진")
@RestController
public class AvatarController {

    private final AvatarService avatarService;
    private final PhotoService photoService;

    public AvatarController(AvatarService avatarService, PhotoService photoService) {
        this.avatarService = avatarService;
        this.photoService = photoService;
    }

    @Operation(summary = "프로필 사진 바꾸기", description = "multipart photo. 전에 올린 사진은 지운다")
    @ApiResponse(responseCode = "200", description = "바뀐 내 여권")
    @ApiResponse(responseCode = "400", description = "사진 파일이 비었거나 JPG·PNG·WEBP·GIF·HEIC 가 아니에요")
    @ApiResponse(responseCode = "413", description = "사진이 20MB 를 넘어요")
    @PutMapping(value = "/api/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FollowService.Profile change(@AuthenticationPrincipal LoginUser me, @RequestParam("photo") MultipartFile photo) {
        return avatarService.change(me.userId(), photo);
    }

    @Operation(summary = "프로필 사진 원래대로", description = "올린 사진을 지우고 카카오 사진(없으면 닉네임 첫 글자)으로")
    @ApiResponse(responseCode = "200", description = "바뀐 내 여권")
    @DeleteMapping("/api/me/photo")
    public FollowService.Profile reset(@AuthenticationPrincipal LoginUser me) {
        return avatarService.reset(me.userId());
    }

    @Operation(summary = "프로필 사진 보기", description = "지금 누군가의 프로필 사진인 것만, 로그인한 누구나")
    @ApiResponse(responseCode = "200", description = "사진 바이트 (immutable 캐시)", content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "404", description = "없거나 지금 누구의 프로필 사진도 아니에요")
    @GetMapping("/api/avatars/{id}")
    public ResponseEntity<Resource> get(@PathVariable Long id) {
        Photo photo = avatarService.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
                .body(photoService.open(photo));
    }
}
