package com.sugyeong.churavel.photo;

import com.sugyeong.churavel.auth.LoginUser;
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

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    /** 사진은 바뀌면 새 id 가 생기므로 오래 캐시해도 된다. */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> get(@AuthenticationPrincipal LoginUser me, @PathVariable Long id) {
        Photo photo = photoService.get(id, me.userId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
                .body(photoService.open(photo));
    }
}
