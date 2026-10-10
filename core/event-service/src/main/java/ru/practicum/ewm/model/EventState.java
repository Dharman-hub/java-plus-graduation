package ru.practicum.ewm.model;

import lombok.extern.slf4j.Slf4j;
import ru.practicum.ewm.exception.ValidationException;

@Slf4j
public enum EventState {
    PUBLISHED,
    PENDING,
    CANCELED;

    public static EventState from(String type) {
        return switch (type) {
            case "PUBLISHED" -> PUBLISHED;
            case "PENDING" -> PENDING;
            case "CANCELED" -> CANCELED;
            default -> {
                log.error("Непредвиденная ошибка конвертации EventState из {}", type);
                throw new ValidationException("Ошибка конвертации EventState");
            }
        };
    }
}