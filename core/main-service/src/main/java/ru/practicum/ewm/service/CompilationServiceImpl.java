package ru.practicum.ewm.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dao.CompilationRepository;
import ru.practicum.ewm.dao.EventRepository;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.CompilationsGetParams;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.QCompilation;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final StatClient statClient;

    @Override
    @Transactional
    public CompilationDto save(NewCompilationDto newCompilationDto) {
        log.info("Добавление новой подборки событий с названием: {}", newCompilationDto.getTitle());
        Set<Event> event = new HashSet<>();

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            event = new HashSet<>(eventRepository.findAllByIdIn(newCompilationDto.getEvents()));
        }

        Compilation compilation = Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned())
                .events(event)
                .build();

        Compilation savedCompilation = compilationRepository.save(compilation);

        return getDto(savedCompilation);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        log.info("Удаление подборки событий с id: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена");
        }

        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        log.info("Обновление информации о подборке событий с id: {}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка id=" + compId + " не найдена"));

        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }

        if (updateCompilationRequest.getTitle() != null) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }

        if (updateCompilationRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllByIdIn(updateCompilationRequest.getEvents()));
            compilation.setEvents(events);
        }

        log.info("Обновление подборки: {}", compilation);
        Compilation updatedCompilation = compilationRepository.save(compilation);
        return getDto(updatedCompilation);
    }

    @Override
    public List<CompilationDto> getCompilations(CompilationsGetParams params) {
        log.info("Получение подборок событий (pinned={}, from={}, size={})",
                params.getPinned(), params.getFrom(), params.getSize());
        int from = params.getFrom();
        int size = params.getSize();

        Pageable pageable = PageRequest.of(0, from + size);
        BooleanExpression predicate = buildPredicate(params);
        Page<Compilation> page = compilationRepository.findAll(predicate, pageable);

        List<Compilation> compilations = page.getContent().stream()
                .skip(from)
                .limit(size)
                .toList();

        Set<Long> allEventIds = compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .map(Event::getId)
                .collect(Collectors.toSet());

        List<String> uris = allEventIds.stream()
                .map(e -> "/events/" + e)
                .toList();
        Map<Long, Long> viewsByEventId = getStatsByUris(uris);

        return compilations.stream()
                .map(comp -> CompilationMapper.toCompilationDto(comp, viewsByEventId))
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки событий с id: {}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        return getDto(compilation);
    }

    private BooleanExpression buildPredicate(CompilationsGetParams params) {
        QCompilation compilation = QCompilation.compilation;
        BooleanExpression predicate = null;

        if (params.getPinned() != null) {
            predicate = compilation.pinned.eq(params.getPinned());
        }

        return predicate == null ? compilation.isNotNull() : predicate;
    }

    private Map<Long, Long> getStatsByUris(List<String> uris) {

        List<ViewStatsDto> stats = statClient.getStat(LocalDateTime.MIN, LocalDateTime.MAX, uris, false);

        if (stats == null || stats.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> viewsMap = new LinkedHashMap<>();
        for (ViewStatsDto dto : stats) {
            String uri = dto.getUri();
            Long id = Long.parseLong(
                    uri.substring(
                            uri.lastIndexOf('/') + 1));
            viewsMap.put(id, dto.getHits());
        }

        return viewsMap;
    }

    private CompilationDto getDto(Compilation compilation) {
        Set<Long> allEventIds = compilation.getEvents().stream()
                .map(Event::getId)
                .collect(Collectors.toSet());

        List<String> uris = allEventIds.stream()
                .map(e -> "/events/" + e)
                .toList();
        Map<Long, Long> viewsByEventId = getStatsByUris(uris);

        return CompilationMapper.toCompilationDto(compilation, viewsByEventId);
    }
}