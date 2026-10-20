package com.sugyeong.churavel;

import com.sugyeong.churavel.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 사진 1장 + 글 1개이던 예전 체크인(visit.post_id → post)이 기록 하나로 옮겨지는지 */
class EntryMigrationTest extends ApiTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void dropLegacy() {
        jdbc.execute("alter table visit drop column if exists post_id");
        jdbc.execute("drop table if exists post");
    }

    @Test
    void 예전_체크인의_사진과_글과_댓글이_첫_기록으로_옮겨진다() throws Exception {
        User owner = user(3001L, "옛사람");
        User friend = user(3002L, "옛친구");
        // 예전 구조를 만든다: post 테이블, visit.post_id, 기록 없는 체크인, 체크인에만 달린 댓글
        jdbc.execute("create table post (id bigint auto_increment primary key, title varchar(200) not null, body clob not null, updated_at timestamp not null)");
        jdbc.execute("alter table visit add column post_id bigint");
        jdbc.update("insert into photo (owner_id, stored_name, original_name, content_type, size_bytes, created_at) values (?, 'legacy.jpg', 'legacy.jpg', 'image/jpeg', 5, current_timestamp)", owner.getId());
        Long photoId = jdbc.queryForObject("select id from photo where stored_name = 'legacy.jpg'", Long.class);
        jdbc.update("insert into post (title, body, updated_at) values ('예전 제목', '예전 본문', current_timestamp)");
        Long postId = jdbc.queryForObject("select max(id) from post", Long.class);
        jdbc.update("insert into visit (user_id, region_code, visited_on, cover_photo_id, post_id, created_at, updated_at) values (?, '2601', date '2025-05-05', ?, ?, current_timestamp, current_timestamp)",
                owner.getId(), photoId, postId);
        Long visitId = jdbc.queryForObject("select id from visit where region_code = '2601' and user_id = ?", Long.class, owner.getId());
        jdbc.update("insert into comment (visit_id, author_id, body, created_at) values (?, ?, '예전 댓글', current_timestamp)", visitId, friend.getId());

        ((ApplicationRunner) context.getBean("entryMigration")).run(null);

        mvc.perform(get("/api/visits/2601").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryCount").value(1))
                .andExpect(jsonPath("$.visitedOn").value("2025-05-05"))
                .andExpect(jsonPath("$.photoUrl").value("/api/photos/" + photoId))
                .andExpect(jsonPath("$.entries[0].title").value("예전 제목"))
                .andExpect(jsonPath("$.entries[0].body").value("예전 본문"))
                .andExpect(jsonPath("$.entries[0].commentCount").value(1));
        assertThat(jdbc.queryForObject("select count(*) from post", Integer.class)).isZero();

        // 한 번 더 돌아도 그대로
        ((ApplicationRunner) context.getBean("entryMigration")).run(null);
        assertThat(jdbc.queryForObject("select count(*) from visit_entry where visit_id = ?", Integer.class, visitId)).isEqualTo(1);
    }
}
