package com.sugyeong.churavel.visit;

import com.sugyeong.churavel.photo.Photo;
import com.sugyeong.churavel.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 한 지역 체크인. 사용자마다 지역당 최대 1건 (user_id + region_code unique).
 * 안에 [사진 + 글] 기록(Entry)이 여러 개 있다. 기록이 하나도 없으면 체크인도 없다.
 * coverPhoto·visitedOn 은 기록에서 뽑은 요약이다: 지도에 붙는 사진 = 가장 최근 기록의 사진, 방문일 = 처음 간 날.
 */
@Entity
@Table(name = "visit", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "region_code"}))
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 16)
    private String regionCode;

    /** 처음 간 날 (기록 중 가장 이른 날) */
    @Column(nullable = false)
    private LocalDate visitedOn;

    /** 지도에 붙는 사진 (가장 최근 기록의 사진을 가리킨다) */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cover_photo_id", nullable = false, unique = true)
    private Photo coverPhoto;

    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Entry> entries = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Visit() {
    }

    /** 첫 기록과 함께 만든다 */
    public Visit(User user, String regionCode, Photo photo, LocalDate visitedOn, String title, String body) {
        this.user = user;
        this.regionCode = regionCode;
        this.createdAt = Instant.now();
        addEntry(photo, visitedOn, title, body);
    }

    public Entry addEntry(Photo photo, LocalDate visitedOn, String title, String body) {
        Entry entry = new Entry(this, photo, visitedOn, title, body);
        entries.add(entry);
        summarize();
        return entry;
    }

    /** 기록을 뺀다. 남은 기록이 없으면 false — 호출한 쪽이 체크인을 지운다 */
    public boolean removeEntry(Entry entry) {
        entries.remove(entry);
        if (entries.isEmpty()) {
            return false;
        }
        summarize();
        return true;
    }

    /** 기록을 고친 뒤 요약(대표 사진·방문일)을 다시 맞춘다 */
    void summarize() {
        Entry latest = entries.stream()
                .max(Comparator.comparing(Entry::getVisitedOn).thenComparing(Entry::getCreatedAt))
                .orElseThrow();
        this.coverPhoto = latest.getPhoto();
        this.visitedOn = entries.stream().map(Entry::getVisitedOn).min(Comparator.naturalOrder()).orElseThrow();
        this.updatedAt = Instant.now();
    }

    /** 보여줄 순서: 최근에 간 날부터 */
    public List<Entry> getEntriesNewestFirst() {
        return entries.stream()
                .sorted(Comparator.comparing(Entry::getVisitedOn).thenComparing(Entry::getCreatedAt).reversed())
                .toList();
    }

    public Entry getCoverEntry() {
        return getEntriesNewestFirst().getFirst();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getRegionCode() { return regionCode; }
    public LocalDate getVisitedOn() { return visitedOn; }
    public Photo getCoverPhoto() { return coverPhoto; }
    public List<Entry> getEntries() { return entries; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
