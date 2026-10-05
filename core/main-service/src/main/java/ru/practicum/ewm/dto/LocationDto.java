package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationDto {
    @NotNull(message = "Field: Latitude. Error: must not be null. Value: ${validatedValue}")
    @DecimalMin(value = "-90.0", message = "Field: Latitude. Error: have to be not less -90. Value: ${validatedValue}")
    @DecimalMax(value = "90.0", message = "Field: Latitude. Error: have to be not more 90. Value: ${validatedValue}")
    private Float lat;

    @NotNull(message = "Field: Longitude. Error: must not be null. Value: null")
    @DecimalMin(value = "-180.0", message = "Field: Longitude. Error: have to be not less -180. Value: ${validatedValue}")
    @DecimalMax(value = "180.0", message = "Field: Longitude. Error: have to be not more 180. Value: ${validatedValue}")
    private Float lon;
}