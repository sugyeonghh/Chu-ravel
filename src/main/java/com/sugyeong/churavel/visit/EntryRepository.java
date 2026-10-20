package com.sugyeong.churavel.visit;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    @EntityGraph(attributePaths = {"visit", "visit.user", "visit.entries", "photo"})
    Optional<Entry> findWithVisitById(Long id);
}
