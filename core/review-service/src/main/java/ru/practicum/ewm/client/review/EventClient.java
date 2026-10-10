package ru.practicum.ewm.client.review;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.EventRequestInfoDto;

import java.util.List;

@FeignClient(name = "event-service", contextId = "reviewEventClient")
public interface EventClient {

    @GetMapping("/internal/events/{eventId}")
    EventRequestInfoDto getEvent(@PathVariable Long eventId);

    @GetMapping("/internal/events")
    List<EventRequestInfoDto> getEvents(@RequestParam List<Long> ids);
}