package com.sugyeong.churavel.visit;

import com.sugyeong.churavel.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "체크인·기록", description = "내 지도: 지역(칸)마다 체크인 1건, 그 안에 기록(사진 1장 + 글) 여러 개")
@RestController
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @Operation(summary = "내 체크인 목록", description = "지도에 칠할 요약 (처음 간 날, 최근 기록의 사진·제목, 기록·댓글 수)")
    @ApiResponse(responseCode = "200", description = "체크인 요약 목록")
    @GetMapping("/api/visits")
    public List<VisitResponse> list(@AuthenticationPrincipal LoginUser me) {
        return visitService.list(me.userId());
    }

    /** 한 칸과 그 기록들 */
    @Operation(summary = "한 칸 + 기록들", description = "regionCode = 한국 시·군·구 코드(3203), 다른 나라 지역 코드(JPN-1860) 또는 나라 코드(JP). 기록은 최근 것부터")
    @ApiResponse(responseCode = "200", description = "체크인 + 기록들")
    @ApiResponse(responseCode = "404", description = "없는 지역 코드이거나 아직 체크인하지 않았어요")
    @GetMapping("/api/visits/{regionCode}")
    public VisitResponse get(@AuthenticationPrincipal LoginUser me, @PathVariable String regionCode) {
        return visitService.get(me.userId(), regionCode);
    }

    /** multipart: photo(필수), visitedOn(yyyy-MM-dd), title, body */
    @Operation(summary = "기록 더하기", description = "처음이면 체크인도 생긴다. photo 필수. KR 은 404 (한국은 시·군·구로)")
    @ApiResponse(responseCode = "200", description = "기록이 더해진 체크인")
    @ApiResponse(responseCode = "400", description = "사진이 없거나 형식이 안 맞거나, 제목·본문이 너무 길어요")
    @ApiResponse(responseCode = "404", description = "없는 지역 코드 (KR 포함)")
    @ApiResponse(responseCode = "413", description = "사진이 20MB 를 넘어요")
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
    @Operation(summary = "기록 고치기", description = "바꿀 것만 보낸다")
    @ApiResponse(responseCode = "200", description = "고친 기록이 담긴 체크인")
    @ApiResponse(responseCode = "400", description = "사진 형식이 안 맞거나 제목·본문이 너무 길어요")
    @ApiResponse(responseCode = "404", description = "내 기록이 아니거나 없어요")
    @ApiResponse(responseCode = "413", description = "사진이 20MB 를 넘어요")
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
    @Operation(summary = "기록 지우기", description = "남은 기록이 있으면 200 + 체크인, 마지막 기록이었으면 204 (체크인도 사라짐)")
    @ApiResponse(responseCode = "200", description = "남은 기록이 있어서 체크인을 돌려줘요")
    @ApiResponse(responseCode = "204", description = "마지막 기록이라 체크인도 사라졌어요")
    @ApiResponse(responseCode = "404", description = "내 기록이 아니거나 없어요")
    @DeleteMapping("/api/entries/{entryId}")
    public ResponseEntity<VisitResponse> deleteEntry(@AuthenticationPrincipal LoginUser me, @PathVariable Long entryId) {
        return visitService.deleteEntry(me.userId(), entryId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** 체크인 통째로 지우기 */
    @Operation(summary = "체크인 통째로 지우기", description = "기록·사진·댓글 모두")
    @ApiResponse(responseCode = "204", description = "지웠어요")
    @ApiResponse(responseCode = "404", description = "없는 지역 코드이거나 아직 체크인하지 않았어요")
    @DeleteMapping("/api/visits/{regionCode}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal LoginUser me, @PathVariable String regionCode) {
        visitService.delete(me.userId(), regionCode);
        return ResponseEntity.noContent().build();
    }
}
