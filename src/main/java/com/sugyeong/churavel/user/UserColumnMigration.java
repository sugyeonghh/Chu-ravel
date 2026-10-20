package com.sugyeong.churavel.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 아이디로 가입한 사용자는 카카오 회원번호가 없다. ddl-auto: update 는 이미 있는 칼럼의 NOT NULL 을 풀지 않아서
 * 예전 DB 의 users.kakao_id 를 서버가 뜰 때 한 번 null 허용으로 바꾼다.
 * TODO: Flyway 를 들이면 이 클래스는 마이그레이션 스크립트로 옮기고 지운다.
 */
@Component
class UserColumnMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserColumnMigration.class);

    private final JdbcTemplate jdbc;

    UserColumnMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        String nullable = jdbc.query(
                "select is_nullable from information_schema.columns where lower(table_name) = 'users' and lower(column_name) = 'kakao_id'",
                rs -> rs.next() ? rs.getString(1) : null);
        if ("NO".equalsIgnoreCase(nullable)) {
            jdbc.execute("alter table users modify kakao_id bigint null"); // MySQL, H2(MODE=MySQL) 둘 다
            log.info("users.kakao_id 를 null 허용으로 바꿨어요 (아이디 가입용)");
        }
    }
}
