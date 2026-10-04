package ru.practicum.ewm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.model.EventSort;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSearchParams {

    private String text;

    private List<Long> categories;

    private Boolean paid;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rangeStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rangeEnd;

    @Builder.Default
    private Boolean onlyAvailable = false;

    private EventSort sort;

    @Builder.Default
    private Integer from = 0;

    @Builder.Default
    private Integer size = 10;

    public void setSort(String sort) {
        if (sort != null) {
            this.sort = EventSort.fromString(sort);
        }
    }

    public void setSort(EventSort sort) {
        if (sort != null) {
            this.sort = sort;
        }
    }

    public void setCategories(String categoriesStr) {
        if (categoriesStr == null || categoriesStr.isBlank()) {
            this.categories = null;
            return;
        }

        try {
            this.categories = Arrays.stream(categoriesStr.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new ValidationException(
                    "Invalid category id: '" + categoriesStr + "'. Must be a number.");
        }
    }

    public void setCategories(List<Long> categories) {
        this.categories = categories;
    }
}