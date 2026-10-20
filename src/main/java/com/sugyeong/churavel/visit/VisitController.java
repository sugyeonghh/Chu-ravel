package com.sugyeong.churavel.visit;

import com.sugyeong.churavel.auth.LoginUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/** 내 체크인. 한 칸(code) 안에 기록(사진+글)이 여러 개, 기록은 entry id 로 가리킨다 */
@RestController
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @GetMapping("/api/visits")
    public List<VisitResponse> list(@AuthenticationPrincipal LoginUser me) {
        return visitService.list(me.userId());
    }

    /** 한 칸과 그 기록들 */
    @GetMapping("/api/visits/{regionCode}")
    public VisitResponse get(@AuthenticationPrincipal LoginUser me, @PathVariable String regionCode) {
        return visitService.get(me.userId(), regionCode);
    }

    /** multipart: photo(필수), visitedOn(yyyy-MM-dd), title, body */
    @PostMapping(value = "/api/visits/{regionCode}/entries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VisitResponse addEntry(
            @AuthenticationPrincipal LoginUser me,
            @PathVariable String regionCode,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "visitedOn", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitedOn,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "body", required = false) String body) {
        return visitService.addEntry(me.userId(), regionCode, photo, visitedOn, title, body);
    }

    /** multipart: 바꿀 것만 (photo, visitedOn, title, body) */
    @PutMapping(value = "/api/entries/{entryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VisitResponse editEntry(
            @AuthenticationPrincipal LoginUser me,
            @PathVariable Long entryId,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "visitedOn", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitedOn,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "body", required = false) String body) {
        return visitService.editEntry(me.userId(), entryId, photo, visitedOn, title, body);
    }

    /** 기록 지우기. 남은 기록이 있으면 200 + 체크인, 마지막이었으면 204 (체크인도 사라짐) */
    @DeleteMapping("/api/entries/{entryId}")
    public ResponseEntity<VisitResponse> deleteEntry(@AuthenticationPrincipal LoginUser me, @PathVariable Long entryId) {
        return visitService.deleteEntry(me.userId(), entryId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** 체크인 통째로 지우기 */
    @DeleteMapping("/api/visits/{regionCode}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal LoginUser me, @PathVariable String regionCode) {
        visitService.delete(me.userId(), regionCode);
        return ResponseEntity.noContent().build();
    }
}
