package com.nectum.tradingv25.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error", e);
        ErrorResponse error = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .build();
        return ResponseEntity.internalServerError().body(error);
    }

    @ExceptionHandler(InvalidIndicatorException.class)
    public ResponseEntity<ErrorResponse> handleInvalidIndicator(InvalidIndicatorException e) {
        log.error("Invalid indicator error", e);
        ErrorResponse error = ErrorResponse.builder()
                .code("INVALID_INDICATOR")
                .message(e.getMessage())
                .build();
        return ResponseEntity.badRequest().body(error);
    }
}