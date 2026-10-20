package com.sugyeong.churavel.comment;

import com.sugyeong.churavel.auth.LoginUser;
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
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    public record WriteRequest(Long entryId, String body) {
    }

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentService.CommentResponse> list(@AuthenticationPrincipal LoginUser me, @RequestParam Long entryId) {
        return commentService.list(me.userId(), entryId);
    }

    @PostMapping
    public CommentService.CommentResponse write(@AuthenticationPrincipal LoginUser me, @RequestBody WriteRequest req) {
        if (req.entryId() == null) {
            throw new IllegalArgumentException("어느 기록에 다는 댓글인지 알려 주세요");
        }
        return commentService.write(me.userId(), req.entryId(), req.body());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal LoginUser me, @PathVariable Long id) {
        commentService.delete(me.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
