package com.sugyeong.churavel.comment;

import com.sugyeong.churavel.user.User;
import com.sugyeong.churavel.visit.Entry;
import com.sugyeong.churavel.visit.Visit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * 기록(사진+글) 하나에 달린 댓글. 기록·체크인을 지우면 DB 가 같이 지운다 (ON DELETE CASCADE).
 * visit 은 기록의 체크인 (권한 확인·체크인별 개수용). 기록이 생기기 전 댓글은 EntryMigration 이 첫 기록에 붙였다.
 */
@Entity
@Table(name = "comment")
public class Comment {

    public static final int MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Visit visit;

    /** DB 에서는 null 을 허용한다 (이 칼럼이 생기기 전 댓글이 있을 수 있어서). 새 댓글은 항상 있다 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Entry entry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = MAX_LENGTH)
    private String body;

    @Column(nullable = false)
    private Instant createdAt;

    protected Comment() {
    }

    public Comment(Entry entry, User author, String body) {
        this.visit = entry.getVisit();
        this.entry = entry;
        this.author = author;
        this.body = body;
        this.createdAt = Instant.now();
    }

    /** 댓글 쓴 사람, 또는 그 체크인의 주인 */
    public boolean canDelete(Long userId) {
        return author.getId().equals(userId) || visit.getUser().getId().equals(userId);
    }

    public Long getId() { return id; }
    public Visit getVisit() { return visit; }
    public Entry getEntry() { return entry; }
    public User getAuthor() { return author; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
