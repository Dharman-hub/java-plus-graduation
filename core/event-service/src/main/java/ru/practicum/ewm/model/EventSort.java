package ru.practicum.ewm.model;

import ru.practicum.ewm.exception.ValidationException;

public enum EventSort {
    EVENT_DATE,
    VIEWS;

    public static EventSort fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return EventSort.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Incorrect sort parameter: '" + value +
                            "'. Allowed values: EVENT_DATE, VIEWS"
            );
        }
    }
}