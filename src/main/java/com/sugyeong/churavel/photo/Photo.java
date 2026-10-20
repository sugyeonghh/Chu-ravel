package com.sugyeong.churavel.photo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** 업로드된 사진 한 장의 메타데이터. 실제 바이트는 PhotoStorage 가 보관한다. */
@Entity
@Table(name = "photo")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 올린 사용자 id. 본인만 볼 수 있다. */
    @Column(nullable = false)
    private Long ownerId;

    /** 저장소 안에서의 파일 이름 (UUID + 확장자) */
    @Column(nullable = false, unique = true, length = 80)
    private String storedName;

    @Column(length = 255)
    private String originalName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private Instant createdAt;

    protected Photo() {
    }

    public Photo(Long ownerId, String storedName, String originalName, String contentType, long sizeBytes) {
        this.ownerId = ownerId;
        this.storedName = storedName;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getStoredName() { return storedName; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getCreatedAt() { return createdAt; }
}
