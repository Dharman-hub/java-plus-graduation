package ru.practicum.ewm.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import ru.practicum.ewm.exception.ValidationException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UsersGetParams {
    List<Long> ids;

    @Builder.Default
    @PositiveOrZero
    private Integer from = 0;

    @Builder.Default
    @Positive
    private Integer size = 10;

    public void setIds(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            this.ids = null;
            return;
        }

        try {
            this.ids = Arrays.stream(idStr.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new ValidationException(
                    "Invalid category id: '" + idStr + "'. Must be a number.");
        }
    }
}