package com.sugyeong.churavel.visit;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    @EntityGraph(attributePaths = {"entries", "entries.photo"})
    Optional<Visit> findByUserIdAndRegionCode(Long userId, String regionCode);

    @EntityGraph(attributePaths = {"entries", "entries.photo"})
    List<Visit> findAllByUserIdOrderByVisitedOnDescUpdatedAtDesc(Long userId);
}
