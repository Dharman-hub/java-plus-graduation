package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventRequestInfoDto;
import ru.practicum.ewm.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventRequestInfoDto getEvent(@PathVariable Long eventId) {
        return eventService.getEventForRequest(eventId);
    }

    @PostMapping("/{eventId}/confirmed-requests")
    public void changeConfirmedRequests(@PathVariable Long eventId,
                                        @RequestParam Integer delta) {
        eventService.changeConfirmedRequests(eventId, delta);
    }

    @GetMapping
    public List<EventRequestInfoDto> getEvents(@RequestParam List<Long> ids) {
        return eventService.getEventsForInternal(ids);
    }
}