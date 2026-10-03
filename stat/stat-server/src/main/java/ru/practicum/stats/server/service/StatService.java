package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.dao.AppStorage;
import ru.practicum.stats.server.dao.StatStorage;
import ru.practicum.stats.server.exception.ValidationException;
import ru.practicum.stats.server.model.App;
import ru.practicum.stats.server.model.Stat;
import ru.practicum.stats.server.model.ViewStat;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatService {
    private final StatStorage statStorage;
    private final AppStorage appStorage;

    @Transactional
    public void addStat(EndpointHitDto dto) {
        if (dto == null) {
            throw new RuntimeException("Attempt to add a null record to the database");
        }

        App app = appStorage.findByName(dto.getApp())
                .orElseGet(() -> appStorage.save(new App(dto.getApp())));

        Stat newStatRecoding = Stat.builder()
                .app(app)
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();

        log.info("Запись в базу данных объекта Stat: {}", newStatRecoding);
        newStatRecoding = statStorage.save(newStatRecoding);

        log.info("id объекта: {}", newStatRecoding.getId());
    }

    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, boolean unique, List<String> uris) {

        if (end.isBefore(start)) {
            throw new ValidationException("start must be before end");
        }
        List<ViewStat> views = statStorage.getStats(start, end, unique, uris);

        List<ViewStatsDto> dtos = views.stream()
                .map(view -> ViewStatsDto.builder()
                        .app(view.getAppName())
                        .uri(view.getUri())
                        .hits(view.getHits())
                        .build())
                .toList();

        log.info("Список статистики сформированный в ответ на запрос: {}", dtos);

        return dtos;
    }
}
