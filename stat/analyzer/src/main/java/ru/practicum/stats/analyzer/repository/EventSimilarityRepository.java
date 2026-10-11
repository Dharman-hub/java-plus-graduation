package ru.practicum.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.stats.analyzer.model.EventSimilarity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);

    List<EventSimilarity> findByEventAOrEventB(Long eventA, Long eventB);

    List<EventSimilarity> findByEventAInOrEventBIn(Collection<Long> eventA, Collection<Long> eventB);
}