package ru.practicum.ewm.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.UserClient;
import ru.practicum.ewm.dao.CompilationRepository;
import ru.practicum.ewm.dao.EventRepository;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.CompilationsGetParams;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.QCompilation;
import ru.practicum.stats.client.AnalyzerGrpcClient;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final AnalyzerGrpcClient analyzerGrpcClient;
    private final UserClient userClient;

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

        List<Event> allEvents = compilations.stream()
                .flatMap(compilation -> compilation.getEvents().stream())
                .distinct()
                .toList();

        Map<Long, Double> ratingsByEventId = getRatingsByEvents(allEvents);
        Map<Long, UserDto> initiators = getInitiatorsByEvents(allEvents);

        return compilations.stream()
                .map(comp -> CompilationMapper.toCompilationDto(
                        comp,
                        ratingsByEventId,
                        initiators
                ))
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

    private Map<Long, Double> getRatingsByEvents(Collection<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .distinct()
                .toList();

        return analyzerGrpcClient.getInteractionsCount(eventIds)
                .collect(Collectors.toMap(
                        recommendation -> recommendation.getEventId(),
                        recommendation -> recommendation.getScore()
                ));
    }

    private CompilationDto getDto(Compilation compilation) {
        Map<Long, Double> ratingsByEventId = getRatingsByEvents(compilation.getEvents());
        Map<Long, UserDto> initiators = getInitiatorsByEvents(compilation.getEvents());

        return CompilationMapper.toCompilationDto(
                compilation,
                ratingsByEventId,
                initiators
        );
    }

    private Map<Long, UserDto> getInitiatorsByEvents(Collection<Event> events) {
        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userClient.getUsers(userIds).stream()
                .collect(Collectors.toMap(UserDto::getId, user -> user));
    }
}