package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateEventUserRequest {
    @Size(min = 3, max = 120,
            message = "Field: title. Error: length must be between 3 and 120 characters. Value: ${validatedValue}")
    private String title;

    @Size(min = 20, max = 2000,
            message = "Field: annotation. Error: length must be between 20 and 2000 characters. Value: ${validatedValue}")
    private String annotation;

    @Size(min = 20, max = 7000,
            message = "Field: description. Error: length must be between 20 and 7000 characters. Value: ${validatedValue}")
    private String description;

    @Size(max = 20)
    private String eventDate;

    @Positive(message = "Category must be positive. Value: ${validatedValue}")
    private Long category;

    @PositiveOrZero(message = "Field: participantLimit. Error: must be positive. Value: ${validatedValue}")
    private Integer participantLimit;

    private LocationDto location;

    private Boolean paid;

    @Size(max = 15)
    private String stateAction;

    private Boolean requestModeration;
}