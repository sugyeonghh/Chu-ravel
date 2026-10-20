package com.sugyeong.churavel.visit;

import com.sugyeong.churavel.region.Region;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 체크인 하나. 목록(GET /api/visits)에서는 entries 가 null 인 요약이고, 한 칸을 열면(GET /api/visits/{code}) 기록이 같이 온다.
 * - regionCode 는 시·군·구 / 나라 지역 / 나라 코드, country 는 그 나라 코드
 * - visitedOn 은 처음 간 날, photoUrl·title 은 가장 최근 기록의 것(지도에 붙는 사진)
 */
public record VisitResponse(
        Long id,
        String regionCode,
        String regionName,
        String province,
        String country,
        LocalDate visitedOn,
        String photoUrl,
        String title,
        int entryCount,
        long commentCount,
        Instant updatedAt,
        List<EntryResponse> entries
) {

    /** 사진 한 장 + 글 */
    public record EntryResponse(Long id, String photoUrl, String title, String body, LocalDate visitedOn,
                                long commentCount, Instant createdAt, Instant updatedAt) {
        static EntryResponse of(Entry e, long commentCount) {
            return new EntryResponse(e.getId(), VisitResponse.photoUrl(e), e.getTitle(), e.getBody(), e.getVisitedOn(),
                    commentCount, e.getCreatedAt(), e.getUpdatedAt());
        }
    }

    static VisitResponse summary(Visit v, Region region, long commentCount) {
        return of(v, region, commentCount, null);
    }

    static VisitResponse detail(Visit v, Region region, Map<Long, Long> commentsByEntry) {
        var entries = v.getEntriesNewestFirst().stream()
                .map(e -> EntryResponse.of(e, commentsByEntry.getOrDefault(e.getId(), 0L)))
                .toList();
        long total = commentsByEntry.values().stream().mapToLong(Long::longValue).sum();
        return of(v, region, total, entries);
    }

    private static VisitResponse of(Visit v, Region region, long commentCount, List<EntryResponse> entries) {
        Entry cover = v.getCoverEntry();
        return new VisitResponse(
                v.getId(),
                v.getRegionCode(),
                region.name(),
                region.province(),
                region.country(),
                v.getVisitedOn(),
                VisitResponse.photoUrl(cover),
                cover.getTitle(),
                v.getEntries().size(),
                commentCount,
                v.getUpdatedAt(),
                entries
        );
    }

    private static String photoUrl(Entry e) {
        return "/api/photos/" + e.getPhoto().getId();
    }
}
