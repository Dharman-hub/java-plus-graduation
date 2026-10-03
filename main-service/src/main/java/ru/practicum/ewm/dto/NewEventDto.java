package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewEventDto {
    @NotBlank(message = "Field: title. Error: Must not be blank. Value: ${validatedValue}")
    @Size(min = 3, max = 120,
            message = "Field: title. Error: length must be between 3 and 120 characters. Value: ${validatedValue}")
    private String title;

    @NotBlank(message = "Field: annotation. Error: Must not be blank. Value: ${validatedValue}")
    @Size(min = 20, max = 2000,
            message = "Field: annotation. Error: length must be between 20 and 2000 characters. Value: ${validatedValue}")
    private String annotation;

    @NotBlank(message = "Field: description. Error: must not be blank. Value: ${validatedValue}")
    @Size(min = 20, max = 7000,
            message = "Field: description. Error: length must be between 20 and 7000 characters. Value: ${validatedValue}")
    private String description;

    @NotBlank(message = "Field: event. Error: must not be blank. Value: ${validatedValue}")
    @Size(max = 20)
    private String eventDate;

    @NotNull(message = "Field: category. Error: Must not be null. Value: ${validatedValue}")
    @Positive(message = "Category must be positive. Value: ${validatedValue}")
    private Long category;

    @PositiveOrZero(message = "Field: participantLimit. Error: must be positive. Value: ${validatedValue}")
    private Integer participantLimit = 0;

    @NotNull(message = "Field: location. Error: Must not be null. Value: ${validatedValue}")
    @Valid
    private LocationDto location;

    private Boolean paid = false;

    private Boolean requestModeration = true;
}