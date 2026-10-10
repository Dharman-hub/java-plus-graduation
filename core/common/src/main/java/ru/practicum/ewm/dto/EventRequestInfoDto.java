package ru.practicum.ewm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestInfoDto {
    private Long id;
    private Long initiatorId;
    private String title;
    private String state;
    private Integer participantLimit;
    private Integer confirmedRequests;
    private Boolean requestModeration;
}