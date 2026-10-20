package com.sugyeong.churavel.comment;

import com.sugyeong.churavel.common.ForbiddenException;
import com.sugyeong.churavel.common.NotFoundException;
import com.sugyeong.churavel.follow.FollowService;
import com.sugyeong.churavel.follow.Traveler;
import com.sugyeong.churavel.user.UserRepository;
import com.sugyeong.churavel.visit.Entry;
import com.sugyeong.churavel.visit.EntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 댓글은 기록(사진+글)마다 달린다. 그 지도를 볼 수 있는 사람(주인 + 수락된 팔로워)만 읽고 쓴다. */
@Service
public class CommentService {

    public record CommentResponse(Long id, String body, Instant createdAt, Traveler author, boolean deletable) {
    }

    private final CommentRepository comments;
    private final EntryRepository entries;
    private final UserRepository users;
    private final FollowService follows;

    public CommentService(CommentRepository comments, EntryRepository entries, UserRepository users, FollowService follows) {
        this.comments = comments;
        this.entries = entries;
        this.users = users;
        this.follows = follows;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long viewerId, Long entryId) {
        Entry entry = viewableEntry(viewerId, entryId);
        Long ownerId = entry.getVisit().getUser().getId();
        return comments.findAllByEntryIdOrderByCreatedAtAsc(entryId).stream()
                .map(c -> toResponse(c, viewerId, ownerId))
                .toList();
    }

    @Transactional
    public CommentResponse write(Long viewerId, Long entryId, String body) {
        String text = body == null ? "" : body.strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("댓글 내용을 써 주세요");
        }
        if (text.length() > Comment.MAX_LENGTH) {
            throw new IllegalArgumentException("댓글은 " + Comment.MAX_LENGTH + "자까지 쓸 수 있어요");
        }
        Entry entry = viewableEntry(viewerId, entryId);
        Comment saved = comments.save(new Comment(entry, users.getReferenceById(viewerId), text));
        return toResponse(saved, viewerId, entry.getVisit().getUser().getId());
    }

    @Transactional
    public void delete(Long viewerId, Long commentId) {
        Comment comment = comments.findWithVisitById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글이 없어요: " + commentId));
        follows.requireView(viewerId, comment.getVisit().getUser().getId());
        if (!comment.canDelete(viewerId)) {
            throw new ForbiddenException("내 댓글이나 내 지도에 달린 댓글만 지울 수 있어요");
        }
        comments.delete(comment);
    }

    /** 사용자 체크인별 댓글 수 (지도 목록에 같이 내려준다) */
    @Transactional(readOnly = true)
    public Map<Long, Long> countsByVisit(Long ownerId) {
        return toMap(comments.countByVisitOfUser(ownerId));
    }

    /** 체크인 하나의 기록별 댓글 수 */
    @Transactional(readOnly = true)
    public Map<Long, Long> countsByEntry(Long visitId) {
        return toMap(comments.countByEntryOfVisit(visitId));
    }

    private static Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private Entry viewableEntry(Long viewerId, Long entryId) {
        Entry entry = entries.findWithVisitById(entryId).orElseThrow(() -> new NotFoundException("기록이 없어요: " + entryId));
        follows.requireView(viewerId, entry.getVisit().getUser().getId());
        return entry;
    }

    private static CommentResponse toResponse(Comment c, Long viewerId, Long ownerId) {
        boolean deletable = c.getAuthor().getId().equals(viewerId) || ownerId.equals(viewerId);
        return new CommentResponse(c.getId(), c.getBody(), c.getCreatedAt(), Traveler.of(c.getAuthor()), deletable);
    }
}
