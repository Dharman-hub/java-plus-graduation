package ru.practicum.stats.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.stats.server.model.App;

import java.util.Optional;

public interface AppStorage extends JpaRepository<App, Long> {
    Optional<App> findByName(String name);
}
