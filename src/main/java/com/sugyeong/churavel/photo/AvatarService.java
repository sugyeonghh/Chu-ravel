package com.sugyeong.churavel.photo;

import com.sugyeong.churavel.common.NotFoundException;
import com.sugyeong.churavel.follow.FollowService;
import com.sugyeong.churavel.user.User;
import com.sugyeong.churavel.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 직접 올린 프로필 사진. 사진 파일은 기록 사진과 같은 Photo 로 두지만,
 * 친구 목록·댓글·팔로우 전 여권 표지에도 보여야 해서 지금 누군가의 프로필 사진인 것만 로그인한 누구나 볼 수 있다.
 */
@Service
public class AvatarService {

    private final UserRepository users;
    private final PhotoRepository photos;
    private final PhotoService photoService;
    private final FollowService followService;

    public AvatarService(UserRepository users, PhotoRepository photos, PhotoService photoService, FollowService followService) {
        this.users = users;
        this.photos = photos;
        this.photoService = photoService;
        this.followService = followService;
    }

    /** 새 사진으로 바꾸고 전에 올린 사진은 지운다. */
    @Transactional
    public FollowService.Profile change(Long meId, MultipartFile file) {
        Photo photo = photoService.store(file, meId);
        replace(meId, photo.getId());
        return followService.me(meId);
    }

    /** 올린 사진을 지우고 원래대로 (카카오 사진, 없으면 닉네임 첫 글자). */
    @Transactional
    public FollowService.Profile reset(Long meId) {
        replace(meId, null);
        return followService.me(meId);
    }

    @Transactional(readOnly = true)
    public Photo get(Long photoId) {
        if (!users.existsByProfilePhotoId(photoId)) {
            throw new NotFoundException("사진이 없어요: " + photoId);
        }
        return photos.findById(photoId).orElseThrow(() -> new NotFoundException("사진이 없어요: " + photoId));
    }

    private void replace(Long meId, Long photoId) {
        User me = users.findById(meId).orElseThrow(() -> new NotFoundException("사용자를 찾지 못했어요"));
        Long old = me.changeProfilePhoto(photoId);
        if (old != null) {
            photos.findById(old).ifPresent(photoService::delete);
        }
    }
}
