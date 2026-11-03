package com.example.inspection_management.exception;

import com.example.inspection_management.exception.ConflictException;
import com.example.inspection_management.exception.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFound(
            ResourceNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(
            IllegalStateException ex) {

        return ResponseEntity
                .badRequest()
                .body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        return ResponseEntity
                .badRequest()
                .body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(message);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<String> handleConflict(ConflictException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(exception.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthentication(
            AuthenticationException exception) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(exception.getMessage());
    }

    @ExceptionHandler(InspectionAccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(
            InspectionAccessDeniedException exception) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(exception.getMessage());
    }
}
