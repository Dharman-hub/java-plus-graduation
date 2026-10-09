package ru.practicum.ewm.client.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventRequestInfoDto;

@FeignClient(name = "event-service", contextId = "requestEventClient")
public interface EventClient {

    @GetMapping("/internal/events/{eventId}")
    EventRequestInfoDto getEvent(@PathVariable Long eventId);

    @PostMapping("/internal/events/{eventId}/confirmed-requests")
    void changeConfirmedRequests(@PathVariable Long eventId,
                                 @RequestParam Integer delta);
}