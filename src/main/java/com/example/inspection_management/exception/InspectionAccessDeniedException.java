package com.example.inspection_management.exception;

public class InspectionAccessDeniedException extends RuntimeException {

    public InspectionAccessDeniedException(String message) {
        super(message);
    }
}
