package com.sugyeong.churavel.visit;

import com.sugyeong.churavel.photo.Photo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDate;

/** 체크인 안의 기록 하나: 사진 한 장 + 그 사진의 글(제목·본문) + 그날 날짜. 댓글은 기록마다 달린다. */
@Entity
@Table(name = "visit_entry")
public class Entry {

    public static final int MAX_TITLE = 200;
    public static final int MAX_BODY = 50_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Visit visit;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photo_id", nullable = false, unique = true)
    private Photo photo;

    @Column(nullable = false, length = MAX_TITLE)
    private String title;

    @Lob
    @Column(nullable = false)
    private String body;

    @Column(nullable = false)
    private LocalDate visitedOn;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Entry() {
    }

    Entry(Visit visit, Photo photo, LocalDate visitedOn, String title, String body) {
        this.visit = visit;
        this.photo = photo;
        this.visitedOn = visitedOn;
        this.title = title;
        this.body = body;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** null 인 값은 그대로 둔다 */
    void edit(LocalDate visitedOn, String title, String body) {
        if (visitedOn != null) this.visitedOn = visitedOn;
        if (title != null) this.title = title;
        if (body != null) this.body = body;
        this.updatedAt = Instant.now();
        visit.summarize();
    }

    /** 바꾸기 전 사진을 돌려준다. 호출한 쪽이 정리한다 */
    Photo replacePhoto(Photo photo) {
        Photo old = this.photo;
        this.photo = photo;
        this.updatedAt = Instant.now();
        visit.summarize();
        return old;
    }

    public Long getId() { return id; }
    public Visit getVisit() { return visit; }
    public Photo getPhoto() { return photo; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public LocalDate getVisitedOn() { return visitedOn; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
