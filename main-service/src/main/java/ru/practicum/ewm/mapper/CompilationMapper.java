package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.UserShortDto;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

public class CompilationMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static CompilationDto toCompilationDto(Compilation compilation,
                                                  Map<Long, Long> viewsMap) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(compilation.getEvents().stream()
                        .map(event -> toEventShortDto(event, viewsMap))
                        .collect(Collectors.toList()))
                .build();
    }

    private static EventShortDto toEventShortDto(Event event,
                                                 Map<Long, Long> viewsMap) {
        Long eventId = event.getId();

        return EventShortDto.builder()
                .id(eventId)
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .eventDate(event.getEventDate().format(FORMATTER))
                .paid(event.getPaid())
                .category(CategoryMapper.toDto(event.getCategory()))
                .initiator(new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName()))
                .views(viewsMap.getOrDefault(eventId, 0L))
                .confirmedRequests(event.getConfirmedRequests())
                .build();
    }
}