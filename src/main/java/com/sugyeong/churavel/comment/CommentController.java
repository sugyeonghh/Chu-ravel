package com.sugyeong.churavel.comment;

import com.sugyeong.churavel.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 댓글은 기록(사진+글) id 로 가리킨다. */
@Tag(name = "댓글", description = "기록마다 다는 댓글")
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    public record WriteRequest(Long entryId, String body) {
    }

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "기록의 댓글 목록")
    @ApiResponse(responseCode = "200", description = "댓글 목록 (오래된 것부터)")
    @ApiResponse(responseCode = "403", description = "팔로우 요청이 수락되기 전이에요")
    @ApiResponse(responseCode = "404", description = "없는 기록")
    @GetMapping
    public List<CommentService.CommentResponse> list(@AuthenticationPrincipal LoginUser me, @RequestParam Long entryId) {
        return commentService.list(me.userId(), entryId);
    }

    @Operation(summary = "댓글 달기", description = "500자까지")
    @ApiResponse(responseCode = "200", description = "단 댓글")
    @ApiResponse(responseCode = "400", description = "entryId 가 없거나, 내용이 비었거나 500자를 넘어요")
    @ApiResponse(responseCode = "403", description = "팔로우 요청이 수락되기 전이에요")
    @ApiResponse(responseCode = "404", description = "없는 기록")
    @PostMapping
    public CommentService.CommentResponse write(@AuthenticationPrincipal LoginUser me, @RequestBody WriteRequest req) {
        if (req.entryId() == null) {
            throw new IllegalArgumentException("어느 기록에 다는 댓글인지 알려 주세요");
        }
        return commentService.write(me.userId(), req.entryId(), req.body());
    }

    @Operation(summary = "댓글 지우기", description = "내 댓글 또는 내 체크인에 달린 댓글")
    @ApiResponse(responseCode = "204", description = "지웠어요")
    @ApiResponse(responseCode = "403", description = "내 댓글도, 내 지도에 달린 댓글도 아니에요")
    @ApiResponse(responseCode = "404", description = "없는 댓글")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal LoginUser me, @PathVariable Long id) {
        commentService.delete(me.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
