package com.eletroflow.backend.web;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException exception) {
        return Map.of(
                "timestamp", OffsetDateTime.now(),
                "message", exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleIllegalState(IllegalStateException exception) {
        return Map.of(
                "timestamp", OffsetDateTime.now(),
                "message", exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException exception) {
        return Map.of(
                "timestamp", OffsetDateTime.now(),
                "message", "Validation failed"
        );
    }
}
