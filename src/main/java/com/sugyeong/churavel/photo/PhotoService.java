package com.sugyeong.churavel.photo;

import com.sugyeong.churavel.common.NotFoundException;
import com.sugyeong.churavel.follow.FollowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

@Service
public class PhotoService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "image/heic", "heic"
    );

    private final PhotoRepository photos;
    private final PhotoStorage storage;
    private final FollowService follows;

    public PhotoService(PhotoRepository photos, PhotoStorage storage, FollowService follows) {
        this.photos = photos;
        this.storage = storage;
        this.follows = follows;
    }

    /** 파일을 저장소에 쓰고 메타데이터를 남긴다. 트랜잭션이 롤백되면 쓴 파일도 지운다. */
    @Transactional
    public Photo store(MultipartFile file, Long ownerId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("사진 파일이 비어 있어요");
        }
        String contentType = file.getContentType();
        String ext = contentType == null ? null : EXTENSIONS.get(contentType);
        if (ext == null) {
            throw new IllegalArgumentException("JPG, PNG, WEBP, GIF, HEIC 사진만 올릴 수 있어요");
        }
        String storedName;
        try (var in = file.getInputStream()) {
            storedName = storage.save(in, ext);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        afterRollback(() -> storage.delete(storedName));
        return photos.save(new Photo(ownerId, storedName, file.getOriginalFilename(), contentType, file.getSize()));
    }

    /** DB 행은 지금 지우고, 파일은 커밋이 끝난 뒤 지운다. */
    @Transactional
    public void delete(Photo photo) {
        String storedName = photo.getStoredName();
        photos.delete(photo);
        afterCommit(() -> storage.delete(storedName));
    }

    /** 내 사진 또는 수락된 팔로워로서 볼 수 있는 사진만. 아니면 있어도 없는 것처럼 404. */
    @Transactional(readOnly = true)
    public Photo get(Long id, Long viewerId) {
        return photos.findById(id)
                .filter(p -> follows.canView(viewerId, p.getOwnerId()))
                .orElseThrow(() -> new NotFoundException("사진이 없어요: " + id));
    }

    public org.springframework.core.io.Resource open(Photo photo) {
        return storage.load(photo.getStoredName());
    }

    private static void afterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private static void afterRollback(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    task.run();
                }
            }
        });
    }
}
