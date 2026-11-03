package ru.practicum.ewm.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventShortDto {
    private Long id;
    private String title;
    private String annotation;
    private Integer confirmedRequests;
    private CategoryDto category;
    private String eventDate;
    private double rating;
    private UserShortDto initiator;
    private Boolean paid;
}