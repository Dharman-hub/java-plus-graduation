package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.RequestClient;
import ru.practicum.ewm.client.UserClient;
import ru.practicum.ewm.dao.CategoryRepository;
import ru.practicum.ewm.dao.EventCustomRepository;
import ru.practicum.ewm.dao.EventRepository;
import ru.practicum.ewm.dao.LocationRepository;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.stats.client.AnalyzerGrpcClient;
import ru.practicum.stats.client.CollectorGrpcClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);
    private final EventRepository eventRepository;
    private final EventCustomRepository eventCustomRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final UserClient userClient;
    private final AnalyzerGrpcClient analyzerGrpcClient;
    private final CollectorGrpcClient collectorGrpcClient;
    private final RequestClient requestClient;

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        UserDto initiator = userClient.getUser(userId);
        Category category = getCategoryByIdWithValidation(dto.getCategory());
        Location location = getLocation(dto.getLocation().getLat(), dto.getLocation().getLon());
        LocalDateTime eventDate = getEventDateWithValidation(dto.getEventDate());

        Event newEvent = Event.builder()
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .eventDate(eventDate)
                .category(category)
                .initiatorId(userId)
                .location(location)
                .confirmedRequests(0)
                .participantLimit(dto.getParticipantLimit())
                .paid(dto.getPaid())
                .requestModeration(dto.getRequestModeration())
                .created(LocalDateTime.now())
                .state(EventState.PENDING)
                .build();
        log.info("Запись в базу данных объекта Event: {}", newEvent);

        newEvent = eventRepository.save(newEvent);
        log.info("id объекта: {}", newEvent.getId());

        return EventMapper.toFullDto(newEvent, 0L, initiator);
    }

    @Override
    public EventFullDto getPrivateEvent(Long userId, Long eventId) {
    Event event = getEventIfExistWithOwnerValidation(eventId, userId);

    UserDto initiator = userClient.getUser(event.getInitiatorId());

    return EventMapper.toFullDto(event, getRating(eventId), initiator);
    }

    @Override
    public EventFullDto updateEvent(Long userId, Long eventId,
                                    UpdateEventUserRequest request) {
        Event event = getEventIfExistWithOwnerValidation(eventId, userId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Event must not be published");
        }

        updateEventFieldsFromRequest(event, request);

        log.info("Запись в базу данных обновленного объекта Event: {}", event);
        eventRepository.save(event);

        UserDto initiator = userClient.getUser(event.getInitiatorId());

        return EventMapper.toFullDto(event, getRating(eventId), initiator);
    }

    @Override
    public List<EventShortDto> getPrivateEvents(long userId, int from, int size) {
    UserDto initiator = userClient.getUser(userId);

    List<Event> events = eventCustomRepository.findUserEventsWithPagination(userId, from, size);

    if (events.isEmpty()) {
        return Collections.emptyList();
    }

    List<Long> eventIds = events.stream()
            .map(Event::getId)
            .toList();

    Map<Long, Double> ratings = analyzerGrpcClient.getInteractionsCount(eventIds)
            .collect(Collectors.toMap(
                    recommendation -> recommendation.getEventId(),
                    recommendation -> recommendation.getScore()
            ));

    return events.stream()
            .map(event -> EventMapper.toShortDto(
                    event,
                    ratings.getOrDefault(event.getId(), 0.0),
                    initiator
            ))
            .toList();
    }

    @Override
    public List<EventShortDto> getPublicEvents(EventSearchParams params,
                                           HttpServletRequest request) {
    LocalDateTime rangeStart = params.getRangeStart();
    LocalDateTime rangeEnd = params.getRangeEnd();

    if (rangeStart == null && rangeEnd == null) {
        rangeStart = LocalDateTime.now();
    }

    if (rangeStart != null
            && rangeEnd != null
            && rangeStart.isAfter(rangeEnd)) {
        throw new ValidationException("Дата начала не может быть позже даты окончания");
    }

    List<Event> events = eventCustomRepository.findPublicEventsWithPagination(
            params, rangeStart, rangeEnd
    );

    if (events.isEmpty()) {
        return Collections.emptyList();
    }

    List<Long> eventIds = events.stream()
            .map(Event::getId)
            .toList();

    Map<Long, Double> ratings = analyzerGrpcClient.getInteractionsCount(eventIds)
            .collect(Collectors.toMap(
                    recommendation -> recommendation.getEventId(),
                    recommendation -> recommendation.getScore()
            ));

    Map<Long, UserDto> initiators = getInitiatorsByEvents(events);

    List<EventShortDto> result = events.stream()
            .map(event -> EventMapper.toShortDto(
                    event,
                    ratings.getOrDefault(event.getId(), 0.0),
                    initiators.get(event.getInitiatorId())
            ))
            .toList();

    if (params.getSort() == EventSort.VIEWS) {
        return result.stream()
                .sorted(Comparator.comparingDouble(EventShortDto::getRating).reversed())
                .toList();
    }

    return result;
    }

    @Override
    public EventFullDto getPublicEventById(Long id, long userId) {
    Event event = eventRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));

    if (event.getState() != EventState.PUBLISHED) {
        throw new NotFoundException("Событие с id=" + id + " не найдено");
    }

    collectorGrpcClient.sendUserAction(userId, id, ActionTypeProto.ACTION_VIEW);

    UserDto initiator = userClient.getUser(event.getInitiatorId());

    return EventMapper.toFullDto(event, getRating(id), initiator);
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(AdminEventSearchParams params) {
    boolean usersEmpty = isEmpty(params.getUsers());
    boolean statesEmpty = isEmpty(params.getStates());
    boolean categoriesEmpty = isEmpty(params.getCategories());

    List<Long> users = getIdsOrDefault(params.getUsers());
    List<String> states = getStatesOrDefault(params.getStates());
    List<Long> categories = getIdsOrDefault(params.getCategories());

    LocalDateTime rangeStart = getRangeStart(params.getRangeStart());
    LocalDateTime rangeEnd = getRangeEnd(params.getRangeEnd());

    List<Event> events = eventCustomRepository.findEventsByAdminFilters(
            params, users, states, categories,
            rangeStart, rangeEnd,
            usersEmpty, statesEmpty, categoriesEmpty
    );

    if (events.isEmpty()) {
        return Collections.emptyList();
    }

    List<Long> eventIds = events.stream()
            .map(Event::getId)
            .toList();

    Map<Long, Double> ratings = analyzerGrpcClient.getInteractionsCount(eventIds)
            .collect(Collectors.toMap(
                    recommendation -> recommendation.getEventId(),
                    recommendation -> recommendation.getScore()
            ));

    Map<Long, UserDto> initiators = getInitiatorsByEvents(events);

    return events.stream()
            .map(event -> EventMapper.toFullDto(
                    event,
                    ratings.getOrDefault(event.getId(), 0.0),
                    initiators.get(event.getInitiatorId())
            ))
            .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventIfExist(eventId);

        if (request.getEventDate() != null) {
            LocalDateTime eventDate = parseDateTimeOrNull(request.getEventDate());
            validateAdminEventDate(eventDate);
            event.setEventDate(eventDate);
        }

        if (request.getCategory() != null) {
            Category category = getCategoryByIdWithValidation(request.getCategory());
            event.setCategory(category);
        }

        if (request.getLocation() != null
                && request.getLocation().getLat() != null
                && request.getLocation().getLon() != null) {
            Location location = getLocation(request.getLocation().getLat(), request.getLocation().getLon());
            event.setLocation(location);
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getStateAction() != null) {
            updateAdminState(event, request.getStateAction());
        }

        log.info("Запись в базу данных обновленного администратором события: {}", event);
        eventRepository.save(event);

        UserDto initiator = userClient.getUser(event.getInitiatorId());

        return EventMapper.toFullDto(event, getRating(eventId), initiator);
    }

    private Location getLocation(Float Lat, Float Lon) {
        return locationRepository.findByLatAndLon(Lat, Lon)
                .orElseGet(() -> locationRepository.save(new Location(Lat, Lon)));
    }

    private Event getEventIfExistWithOwnerValidation(long eventId, long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Field: eventId. Error: event не найден. Value: " + eventId));
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Field: userId. Error: Initiator has another id. Value: " + userId);
        }
        return event;
    }

    private Category getCategoryByIdWithValidation(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Категория с id=" + id + " не найдена"));
    }

    private LocalDateTime getEventDateWithValidation(String date) {
        LocalDateTime eventDate;
        if (!date.isBlank()) {
            eventDate = LocalDateTime.parse(date, FORMATTER);

            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Field: eventDate. Error: Начало события должно быть позже" +
                        LocalDateTime.now().plusHours(2) + ". Value: " + eventDate);
            }
        } else {
            eventDate = null;
        }
        return eventDate;
    }

    private EventState validateStateAction(String stateAction, EventState eventState) {
        switch (stateAction.trim().toUpperCase()) {
            case "SEND_TO_REVIEW" -> {
                if (eventState != EventState.CANCELED) {
                    throw new ConflictException("Only pending or canceled events can be changed");
                }
                return EventState.PENDING;
            }
            case "CANCEL_REVIEW" -> {
                if (eventState != EventState.PENDING) {
                    throw new ConflictException("Only pending or canceled events can be changed");
                }
                return EventState.CANCELED;
            }
            default -> throw new ValidationException("Field: stateAction. Error: must be SEND_TO_REVIEW or " +
                    "CANCEL_REVIEW. Value: " + stateAction);
        }
    }



    private boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private List<Long> getIdsOrDefault(List<Long> ids) {
        return isEmpty(ids) ? List.of(-1L) : ids;
    }

    private List<String> getStatesOrDefault(List<String> states) {
        if (isEmpty(states)) {
            return List.of(EventState.PENDING.name());
        }

        return states.stream()
                .map(state -> EventState.from(state.trim().toUpperCase()).name())
                .toList();
    }

    private LocalDateTime parseDateTimeOrNull(String date) {
        if (date == null) {
            return null;
        }

        if (date.isBlank()) {
            throw new ValidationException("Field: date. Error: date must not be blank");
        }

        try {
            return LocalDateTime.parse(date, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Field: date. Error: date must have format yyyy-MM-dd HH:mm:ss. Value: "
                    + date);
        }
    }

    private Event getEventIfExist(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Field: eventId. Error: event не найден. Value: " + eventId));
    }

    private void updateAdminState(Event event, String stateAction) {
        switch (stateAction.trim().toUpperCase()) {
            case "PUBLISH_EVENT" -> {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish the event because it's not in the right state: "
                            + event.getState());
                }

                validateAdminEventDate(event.getEventDate());
                event.setState(EventState.PUBLISHED);
                event.setPublished(LocalDateTime.now());
            }
            case "REJECT_EVENT" -> {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject the event because it's already published");
                }

                event.setState(EventState.CANCELED);
            }
            default -> throw new ValidationException("Field: stateAction. Error: must be PUBLISH_EVENT or "
                    + "REJECT_EVENT. Value: " + stateAction);
        }
    }

    private void validateAdminEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Field: eventDate. Error: Начало события должно быть не ранее чем через час. "
                    + "Value: " + eventDate);
        }
    }

    private LocalDateTime getRangeStart(String rangeStart) {
        if (rangeStart == null) {
            return LocalDateTime.of(1900, 1, 1, 0, 0);
        }

        return parseDateTimeOrNull(rangeStart);
    }

    private LocalDateTime getRangeEnd(String rangeEnd) {
        if (rangeEnd == null) {
            return LocalDateTime.of(3000, 1, 1, 0, 0);
        }

        return parseDateTimeOrNull(rangeEnd);
    }

    private void updateEventFieldsFromRequest(Event event, UpdateEventUserRequest request) {
        if (request.getEventDate() != null) {
            LocalDateTime eventDate = getEventDateWithValidation(request.getEventDate());
            event.setEventDate(eventDate);
        }

        if (request.getCategory() != null) {
            Category category = getCategoryByIdWithValidation(request.getCategory());
            event.setCategory(category);
        }

        if (request.getLocation() != null) {
            if (request.getLocation().getLat() != null && request.getLocation().getLon() != null) {
                Location location = getLocation(request.getLocation().getLat(), request.getLocation().getLon());
                event.setLocation(location);
            }
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getStateAction() != null) {
            EventState state = validateStateAction(request.getStateAction(), event.getState());
            event.setState(state);
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
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

    @Override
    public EventRequestInfoDto getEventForRequest(Long eventId) {
        Event event = getEventIfExist(eventId);

        return new EventRequestInfoDto(
                event.getId(),
                event.getInitiatorId(),
                event.getTitle(),
                event.getState().name(),
                event.getParticipantLimit(),
                event.getConfirmedRequests(),
                event.getRequestModeration()
        );
    }

    @Override
    @Transactional
    public void changeConfirmedRequests(Long eventId, Integer delta) {
        Event event = getEventIfExist(eventId);

        event.setConfirmedRequests(event.getConfirmedRequests() + delta);

        eventRepository.save(event);
    }

    @Override
    public List<EventRequestInfoDto> getEventsForInternal(List<Long> ids) {
        return eventRepository.findAllById(ids).stream()
                .map(event -> new EventRequestInfoDto(
                        event.getId(),
                        event.getInitiatorId(),
                        event.getTitle(),
                        event.getState().name(),
                        event.getParticipantLimit(),
                        event.getConfirmedRequests(),
                        event.getRequestModeration()
                ))
                .toList();
    }

    private double getRating(long eventId) {
    return analyzerGrpcClient.getInteractionsCount(List.of(eventId))
            .findFirst()
            .map(recommendation -> recommendation.getScore())
            .orElse(0.0);
    }

    @Override
    public List<EventShortDto> getRecommendations(long userId) {
    List<Long> recommendedIds = analyzerGrpcClient.getRecommendationsForUser(userId, 10)
            .map(recommendation -> recommendation.getEventId())
            .distinct()
            .toList();

    if (recommendedIds.isEmpty()) {
        return Collections.emptyList();
    }

    Map<Long, Event> eventsById = eventRepository.findAllById(recommendedIds).stream()
            .filter(event -> event.getState() == EventState.PUBLISHED)
            .collect(Collectors.toMap(Event::getId, event -> event));

    List<Event> events = recommendedIds.stream()
            .map(eventsById::get)
            .filter(Objects::nonNull)
            .toList();

    if (events.isEmpty()) {
        return Collections.emptyList();
    }

    List<Long> eventIds = events.stream()
            .map(Event::getId)
            .toList();

    Map<Long, Double> ratings = analyzerGrpcClient.getInteractionsCount(eventIds)
            .collect(Collectors.toMap(
                    recommendation -> recommendation.getEventId(),
                    recommendation -> recommendation.getScore()
            ));

    Map<Long, UserDto> initiators = getInitiatorsByEvents(events);

    return events.stream()
            .map(event -> EventMapper.toShortDto(
                    event,
                    ratings.getOrDefault(event.getId(), 0.0),
                    initiators.get(event.getInitiatorId())
            ))
            .toList();
    }

    @Override
    public void likeEvent(Long eventId, long userId) {
    Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException(
                    "Событие с id=" + eventId + " не найдено"
            ));

    if (event.getState() != EventState.PUBLISHED
            || !event.getEventDate().isBefore(LocalDateTime.now())) {
        throw new ValidationException(
                "Можно лайкать только завершившиеся опубликованные мероприятия"
        );
    }

    boolean attended = requestClient.getUserRequests(userId).stream()
            .anyMatch(request ->
                    eventId.equals(request.getEvent())
                            && "CONFIRMED".equals(request.getStatus())
            );

    if (!attended) {
        throw new ValidationException(
                "Пользователь не посещал мероприятие с id=" + eventId
        );
    }

    collectorGrpcClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }
}