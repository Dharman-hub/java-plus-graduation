package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.service.EventService;
import ru.practicum.ewm.service.ParticipationRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class UserEventsController {

    private final EventService eventService;
    private final ParticipationRequestService requestService;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@Valid @RequestBody NewEventDto request,
                                 @PathVariable Long userId) {
        log.info("POST /users/{}/events with body: {}", userId, request);

        return eventService.addEvent(userId, request);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getOwnEvent(@PathVariable Long userId,
                                 @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}", userId, eventId);

        return eventService.getPrivateEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@Valid @RequestBody UpdateEventUserRequest request,
                                    @PathVariable Long userId,
                                    @PathVariable Long eventId) {
        log.info("Patch /users/{}/events/{} with body: {}", userId, eventId, request);

        return eventService.updateEvent(userId, eventId, request);
    }

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getOwnEvents(
            @PathVariable Long userId,
            @PositiveOrZero(message = "Field: from. Error: must be positive. Value: ${validatedValue}")
            @RequestParam(name = "from", required = false, defaultValue = "0") int from,
            @Positive(message = "Field: size. Error: must be positive. Value: ${validatedValue}")
            @RequestParam(name = "size", required = false, defaultValue = "10") int size) {
        log.info("GET /users/{}/events with params from= {} and size= {}", userId, from, size);

        return eventService.getPrivateEvents(userId, from, size);
    }

    @GetMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getOwnParticipationRequests(@PathVariable Long userId, @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests", userId, eventId);

        return requestService.getRequestsByEventId(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult updateRequestsToOwnEvent(@PathVariable Long userId,
                                                                         @PathVariable Long eventId,
                                                                         @Valid @RequestBody
                                                                             EventRequestStatusUpdateRequest request) {
        log.info("PATCH /users/{}/events/{}/requests with body: {}", userId, eventId, request);

        return requestService.updateOwnParticipationRequests(userId, eventId, request);
    }


}