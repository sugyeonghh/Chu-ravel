package com.sugyeong.churavel.comment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findAllByEntryIdOrderByCreatedAtAsc(Long entryId);

    @EntityGraph(attributePaths = {"visit", "visit.user"})
    Optional<Comment> findWithVisitById(Long id);

    /** 한 사용자의 체크인별 댓글 수: [visitId, count] */
    @Query("select c.visit.id, count(c) from Comment c where c.visit.user.id = :userId group by c.visit.id")
    List<Object[]> countByVisitOfUser(Long userId);

    /** 한 체크인의 기록별 댓글 수: [entryId, count] */
    @Query("select c.entry.id, count(c) from Comment c where c.visit.id = :visitId and c.entry is not null group by c.entry.id")
    List<Object[]> countByEntryOfVisit(Long visitId);
}
