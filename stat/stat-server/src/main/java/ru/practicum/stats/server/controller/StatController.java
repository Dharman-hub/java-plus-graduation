package ru.practicum.stats.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.exception.ValidationException;
import ru.practicum.stats.server.service.StatService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatController {
    private final StatService service;

    @PostMapping(path = "/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void addStat(@RequestBody EndpointHitDto dto) {
        log.info("Вызван эндпоинт добавления запроса с данными: {}", dto);
        service.addStat(dto);
    }

    @GetMapping(path = "/stats")
    public List<ViewStatsDto> getStats(@RequestParam(required = false) String start,
                                       @RequestParam(required = false) String end,
                                       @RequestParam(defaultValue = "false") boolean unique,
                                       @RequestParam(required = false) List<String> uris) {
        log.info("Вызван эндпоинт на получение списка статистики с параметрами: start = {},\nend = {},\nunique = {},\nuris = {}",
                start, end, unique, uris);

        if (start == null || end == null) {
            log.error("Start or end parameter is missing");
            throw new ValidationException("Start or end parameter is missing");
        }

        String decodedStart = URLDecoder.decode(start, StandardCharsets.UTF_8);
        String decodedEnd = URLDecoder.decode(end, StandardCharsets.UTF_8);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startDate = LocalDateTime.parse(decodedStart, formatter);
        LocalDateTime endDate = LocalDateTime.parse(decodedEnd, formatter);

        uris = (uris == null || uris.isEmpty()) ? null : uris;

        return service.getStats(startDate, endDate, unique, uris);
    }
}
