package ru.practicum.ewm.model;

import lombok.extern.slf4j.Slf4j;
import ru.practicum.ewm.exception.ValidationException;

@Slf4j
public enum RequestStatus {
    REJECTED,
    PENDING,
    CONFIRMED,
    CANCELED;

    public static RequestStatus from(String type) {
        return switch (type) {
            case "REJECTED" -> REJECTED;
            case "PENDING" -> PENDING;
            case "CONFIRMED" -> CONFIRMED;
            case "CANCELED" -> CANCELED;
            default -> {
                log.error("Непредвиденная ошибка конвертации RequestStatus из {}", type);
                throw new ValidationException("Ошибка конвертации RequestStatus");
            }
        };
    }
}