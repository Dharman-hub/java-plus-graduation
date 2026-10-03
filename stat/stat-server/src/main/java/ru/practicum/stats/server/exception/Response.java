package ru.practicum.stats.server.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class Response {
    private final HttpStatus httpStatus;
    private final String header;
    private final String message;
    private final String stackTrace;
}
