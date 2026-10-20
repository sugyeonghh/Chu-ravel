package com.sugyeong.churavel.photo;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * 사진 바이트 저장소. 지금은 로컬 디스크(LocalPhotoStorage) 구현만 있고,
 * 배포할 때 S3 / R2 같은 오브젝트 스토리지 구현으로 갈아끼운다.
 */
public interface PhotoStorage {

    /** 저장하고, 나중에 load/delete 할 때 쓸 이름을 돌려준다. */
    String save(InputStream in, String extension);

    Resource load(String storedName);

    void delete(String storedName);
}
