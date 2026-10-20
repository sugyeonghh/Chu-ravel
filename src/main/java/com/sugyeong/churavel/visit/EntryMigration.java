package com.sugyeong.churavel.visit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 한 칸에 사진 1장 + 글 1개이던 예전 체크인을 기록(visit_entry) 하나로 옮긴다. 서버가 뜰 때마다 돌지만 옮길 게 없으면 아무것도 안 한다.
 * - 예전 대표 사진(visit.cover_photo_id) + 글(post) → 첫 기록. 대표 사진은 그대로 그 기록의 사진을 가리킨다
 * - 체크인에 달려 있던 댓글 → 그 첫 기록의 댓글
 * - 다 옮긴 글(post)은 지운다
 * TODO: Flyway 를 들이면 이 클래스는 마이그레이션 스크립트로 옮기고 지운다.
 */
@Component
class EntryMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EntryMigration.class);

    private final JdbcTemplate jdbc;

    EntryMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean hasPost = hasColumn("visit", "post_id") && hasTable("post");
        int moved = jdbc.update(hasPost
                ? """
                  insert into visit_entry (visit_id, photo_id, title, body, visited_on, created_at, updated_at)
                  select v.id, v.cover_photo_id, coalesce(p.title, ''), coalesce(p.body, ''), v.visited_on, v.created_at, v.updated_at
                  from visit v left join post p on p.id = v.post_id
                  where not exists (select 1 from visit_entry e where e.visit_id = v.id)
                  """
                : """
                  insert into visit_entry (visit_id, photo_id, title, body, visited_on, created_at, updated_at)
                  select v.id, v.cover_photo_id, '', '', v.visited_on, v.created_at, v.updated_at
                  from visit v
                  where not exists (select 1 from visit_entry e where e.visit_id = v.id)
                  """);
        int comments = jdbc.update("""
                update comment set entry_id = (select min(e.id) from visit_entry e where e.visit_id = comment.visit_id)
                where entry_id is null
                """);
        int posts = 0;
        if (hasPost) {
            jdbc.update("update visit set post_id = null where post_id is not null");
            posts = jdbc.update("delete from post");
        }
        if (moved + comments + posts > 0) {
            log.info("예전 체크인을 기록으로 옮겼어요: 기록 {}개, 댓글 {}개, 지운 글 {}개", moved, comments, posts);
        }
    }

    private boolean hasTable(String table) {
        Integer n = jdbc.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = ?", Integer.class, table);
        return n != null && n > 0;
    }

    private boolean hasColumn(String table, String column) {
        Integer n = jdbc.queryForObject(
                "select count(*) from information_schema.columns where lower(table_name) = ? and lower(column_name) = ?",
                Integer.class, table, column);
        return n != null && n > 0;
    }
}
