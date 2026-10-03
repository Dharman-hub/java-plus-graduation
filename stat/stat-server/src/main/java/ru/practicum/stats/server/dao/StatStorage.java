package ru.practicum.stats.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.stats.server.model.Stat;
import ru.practicum.stats.server.model.ViewStat;

import java.time.LocalDateTime;
import java.util.List;

public interface StatStorage extends JpaRepository<Stat, Long> {

    @Query("SELECT s.app.name AS appName, s.uri AS uri, " +
            "CASE WHEN :unique = true THEN COUNT(DISTINCT s.ip) ELSE COUNT(s.id) END AS hits " +
            "FROM Stat s " +
            "WHERE s.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR s.uri IN :uris) " +
            "GROUP BY s.app.name, s.uri " +
            "ORDER BY hits DESC")
    List<ViewStat> getStats(@Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end,
                            @Param("unique") boolean unique,
                            @Param("uris") List<String> uris);
}