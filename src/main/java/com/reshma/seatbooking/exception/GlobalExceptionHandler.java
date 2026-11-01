package com.reshma.seatbooking.exception;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 409 Conflict: the seat was taken, or another request won the race
    @ExceptionHandler({SeatUnavailableException.class,
            ObjectOptimisticLockingFailureException.class,
            PessimisticLockingFailureException.class})
    public ResponseEntity<Map<String, Object>> conflict(RuntimeException e) {
        return error(HttpStatus.CONFLICT, e instanceof SeatUnavailableException
                ? e.getMessage()
                : "Seat was just taken by another user, please choose another seat");
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", message,
                "timestamp", LocalDateTime.now().toString()));
    }
}
