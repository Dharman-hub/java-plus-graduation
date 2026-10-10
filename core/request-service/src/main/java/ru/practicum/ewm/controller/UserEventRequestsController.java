package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.service.ParticipationRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class UserEventRequestsController {

    private final ParticipationRequestService requestService;

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