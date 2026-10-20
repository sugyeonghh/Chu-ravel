package com.sugyeong.churavel.user;

import com.sugyeong.churavel.common.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByKakaoId(Long kakaoId);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByPassportNo(String passportNo);

    boolean existsByPassportNo(String passportNo);

    boolean existsByProfilePhotoId(Long profilePhotoId);

    default User getByPassportNo(String passportNo) {
        return findByPassportNo(passportNo)
                .orElseThrow(() -> new NotFoundException("여권번호가 " + passportNo + "인 여행자를 찾지 못했어요"));
    }
}
