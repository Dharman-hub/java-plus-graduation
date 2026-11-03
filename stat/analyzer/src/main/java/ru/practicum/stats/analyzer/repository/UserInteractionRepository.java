package ru.practicum.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.stats.analyzer.model.UserInteraction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    Optional<UserInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserInteraction> findByUserIdOrderByLastInteractionDesc(Long userId);

    List<UserInteraction> findByEventIdIn(Collection<Long> eventIds);
}