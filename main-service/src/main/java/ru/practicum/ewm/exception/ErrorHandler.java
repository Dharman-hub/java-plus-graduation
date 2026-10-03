package ru.practicum.ewm.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(final NotFoundException ex) {
        log.error("Получен статус 404 Not Found: {}", ex.getMessage(), ex);
        return ApiError.builder()
                .errors(getStackTrace(ex))
                .message(ex.getMessage())
                .reason("Требуемая сущность не найдена.")
                .status("NOT_FOUND")
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ValidationException.class,
            MissingServletRequestParameterException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestException(final Exception ex) {
        String reason = "Неправильно созданный запрос.";
        String errorMessage;

        if (ex instanceof MethodArgumentNotValidException validationEx) {
            log.error("Получен статус 400 Bad Request (валидация полей): {}", ex.getMessage(), ex);

            errorMessage = validationEx.getBindingResult().getFieldErrors().stream()
                    .map(error -> String.format("Field: %s. Error: %s. Value: %s",
                            error.getField(), error.getDefaultMessage(), error.getRejectedValue()))
                    .collect(Collectors.joining("; "));
        } else {
            log.error("Получен статус 400 Bad Request (валидация параметров): {}", ex.getMessage(), ex);
            errorMessage = ex.getMessage();
        }

        return ApiError.builder()
                .errors(getStackTrace(ex))
                .message(errorMessage)
                .reason(reason)
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConflictException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(final Exception ex) {
        String reason;

        if (ex instanceof DataIntegrityViolationException) {
            log.error("Получен статус 409 Conflict (нарушение целостности данных): {}", ex.getMessage(), ex);
            reason = "Нарушены данные.";
        } else if (ex instanceof ConflictException) {
            log.error("Получен статус 409 Conflict (бизнес-логика): {}", ex.getMessage(), ex);
            reason = "Неправильные условия для запроса.";
        } else {
            log.error("Получен статус 409 Conflict: {}", ex.getMessage(), ex);
            reason = "Конфликт запроса.";
        }

        return ApiError.builder()
                .errors(getStackTrace(ex))
                .message(ex.getMessage())
                .reason(reason)
                .status("CONFLICT")
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleThrowable(final Throwable ex) {
        log.error("Получен статус 500 Internal Server Error: {}", ex.getMessage(), ex);
        return ApiError.builder()
                .errors(getStackTrace(ex))
                .message(ex.getMessage())
                .reason("Внутренняя ошибка сервера.")
                .status("INTERNAL_SERVER_ERROR")
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    private List<String> getStackTrace(Throwable throwable) {
        return Arrays.stream(throwable.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());
    }
}