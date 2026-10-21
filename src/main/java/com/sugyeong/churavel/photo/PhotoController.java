package com.sugyeong.churavel.photo;

import com.sugyeong.churavel.auth.LoginUser;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "체크인·기록", description = "내 지도: 지역(칸)마다 체크인 1건, 그 안에 기록(사진 1장 + 글) 여러 개")
@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    /** 사진은 바뀌면 새 id 가 생기므로 오래 캐시해도 된다. */
    @Operation(summary = "기록 사진 보기", description = "내 사진 또는 수락된 팔로워만, 아니면 404")
    @ApiResponse(responseCode = "200", description = "사진 바이트 (immutable 캐시)", content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "404", description = "없거나 볼 수 없는 사진 (남의 사진은 수락된 팔로워만)")
    @GetMapping("/{id}")
    public ResponseEntity<Resource> get(@AuthenticationPrincipal LoginUser me, @PathVariable Long id) {
        Photo photo = photoService.get(id, me.userId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
                .body(photoService.open(photo));
    }
}
