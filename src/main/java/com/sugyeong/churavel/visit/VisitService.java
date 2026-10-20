package com.sugyeong.churavel.visit;

import com.sugyeong.churavel.comment.CommentService;
import com.sugyeong.churavel.common.NotFoundException;
import com.sugyeong.churavel.photo.Photo;
import com.sugyeong.churavel.photo.PhotoService;
import com.sugyeong.churavel.region.Region;
import com.sugyeong.churavel.region.RegionCatalog;
import com.sugyeong.churavel.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 체크인과 그 안의 기록(사진+글). 모든 메서드는 userId 의 지도 안에서만 동작한다
 * (남의 지도를 읽을 때는 부르는 쪽이 FollowService 로 먼저 확인한다).
 */
@Service
public class VisitService {

    private final VisitRepository visits;
    private final EntryRepository entries;
    private final PhotoService photoService;
    private final RegionCatalog regions;
    private final UserRepository users;
    private final CommentService comments;

    public VisitService(VisitRepository visits, EntryRepository entries, PhotoService photoService, RegionCatalog regions,
                        UserRepository users, CommentService comments) {
        this.visits = visits;
        this.entries = entries;
        this.photoService = photoService;
        this.regions = regions;
        this.users = users;
        this.comments = comments;
    }

    @Transactional(readOnly = true)
    public List<VisitResponse> list(Long userId) {
        Map<Long, Long> counts = comments.countsByVisit(userId);
        return visits.findAllByUserIdOrderByVisitedOnDescUpdatedAtDesc(userId).stream()
                .map(v -> VisitResponse.summary(v, regions.get(v.getRegionCode()), counts.getOrDefault(v.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public VisitResponse get(Long userId, String regionCode) {
        return detail(find(userId, regionCode));
    }

    /** 기록 하나 더하기 (사진 필수). 그 지역에 처음이면 체크인도 같이 생긴다. 날짜가 없으면 오늘 */
    @Transactional
    public VisitResponse addEntry(Long userId, String regionCode, MultipartFile photo, LocalDate visitedOn,
                                  String title, String body) {
        Region region = regions.get(regionCode); // 한국(KR)은 목록에 없다: 시·군·구로 체크인한다
        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException(region.name() + " 기록에는 사진이 필요해요");
        }
        String t = text(title, Entry.MAX_TITLE, "제목"), b = text(body, Entry.MAX_BODY, "본문");
        LocalDate date = visitedOn != null ? visitedOn : LocalDate.now();
        Photo stored = photoService.store(photo, userId);

        Visit visit = visits.findByUserIdAndRegionCode(userId, regionCode).orElse(null);
        if (visit == null) {
            visit = visits.save(new Visit(users.getReferenceById(userId), regionCode, stored, date, t, b));
        } else {
            visit.addEntry(stored, date, t, b);
        }
        visits.flush(); // 새 기록 id 를 받아서 돌려준다
        return detail(visit);
    }

    /** 기록 고치기. 넘어온 값만 바꾼다 (사진·날짜·제목·본문) */
    @Transactional
    public VisitResponse editEntry(Long userId, Long entryId, MultipartFile photo, LocalDate visitedOn,
                                   String title, String body) {
        Entry entry = findEntry(userId, entryId);
        entry.edit(visitedOn,
                title == null ? null : text(title, Entry.MAX_TITLE, "제목"),
                body == null ? null : text(body, Entry.MAX_BODY, "본문"));
        if (photo != null && !photo.isEmpty()) {
            Photo old = entry.replacePhoto(photoService.store(photo, userId));
            visits.flush(); // FK 가 새 사진을 가리킨 뒤에 옛 사진 행을 지운다
            photoService.delete(old);
        }
        return detail(entry.getVisit());
    }

    /** 기록 지우기. 마지막 기록이었으면 체크인도 사라지고 빈 값을 돌려준다 */
    @Transactional
    public Optional<VisitResponse> deleteEntry(Long userId, Long entryId) {
        Entry entry = findEntry(userId, entryId);
        Visit visit = entry.getVisit();
        Photo photo = entry.getPhoto();
        if (!visit.removeEntry(entry)) {
            visits.delete(visit);
            visits.flush();
            photoService.delete(photo);
            return Optional.empty();
        }
        visits.flush(); // 대표 사진이 다른 기록으로 바뀐 뒤에 사진 행을 지운다
        photoService.delete(photo);
        return Optional.of(detail(visit));
    }

    /** 체크인 통째로 지우기 (기록·사진·댓글 모두) */
    @Transactional
    public void delete(Long userId, String regionCode) {
        Visit visit = find(userId, regionCode);
        List<Photo> photos = visit.getEntries().stream().map(Entry::getPhoto).toList();
        visits.delete(visit);
        visits.flush();
        photos.forEach(photoService::delete);
    }

    private Visit find(Long userId, String regionCode) {
        Region region = regions.get(regionCode); // 없는 지역 코드면 404
        return visits.findByUserIdAndRegionCode(userId, regionCode)
                .orElseThrow(() -> new NotFoundException("아직 " + region.name() + "에 체크인하지 않았어요"));
    }

    private Entry findEntry(Long userId, Long entryId) {
        return entries.findWithVisitById(entryId)
                .filter(e -> e.getVisit().getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("기록이 없어요: " + entryId));
    }

    private VisitResponse detail(Visit v) {
        return VisitResponse.detail(v, regions.get(v.getRegionCode()), comments.countsByEntry(v.getId()));
    }

    private static String text(String s, int max, String what) {
        String t = s == null ? "" : s.strip();
        if (t.length() > max) {
            throw new IllegalArgumentException(what + "은 " + String.format("%,d", max) + "자까지 쓸 수 있어요");
        }
        return t;
    }
}
