package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.practicum.ewm.model.RequestStatus;

import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventRequestStatusUpdateRequest {
    @NotEmpty(message = "Field: requestIds. Error: must not be empty. Value: ${validatedValue}")
    private List<Long> requestIds;

    @NotNull(message = "Field: status. Error: must not be null. Value: ${validatedValue}")
    private RequestStatus status;
}