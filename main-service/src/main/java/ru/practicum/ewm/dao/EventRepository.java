package ru.practicum.ewm.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Event;

import java.util.List;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long>,
        QuerydslPredicateExecutor<Event> {

    List<Event> findAllByIdIn(Set<Long> ids);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
            "FROM Event e " +
            "WHERE e.category.id = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") Long categoryId);

    @Modifying
    @Query(value =
            "UPDATE events SET confirmed_requests = (confirmed_requests - 1) " +
            "WHERE id = :id AND confirmed_requests > 0",
            nativeQuery = true)
    void updateDecrementConfirmedRequests(@Param("id") Long id);

    @Modifying
    @Query(value =
            "UPDATE events SET confirmed_requests = (confirmed_requests + 1) " +
                    "WHERE id = :id AND confirmed_requests < participant_limit",
            nativeQuery = true)
    int updateIncrementConfirmedRequests(@Param("id") Long id);
}