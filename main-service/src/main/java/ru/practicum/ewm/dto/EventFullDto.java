package ru.practicum.ewm.dto;

import lombok.*;
import ru.practicum.ewm.model.EventState;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventFullDto {
    private Long id;
    private String title;
    private String annotation;
    private String description;
    private CategoryDto category;
    private UserShortDto initiator;
    private LocationDto location;
    private Boolean paid;
    private Boolean requestModeration;
    private Integer participantLimit;
    private Integer confirmedRequests;
    private String createdOn;
    private String eventDate;
    private String publishedOn;
    private EventState state;
    private Long views;
}