package com.reliaquest.api.exception;

import static com.reliaquest.api.constants.ErrorConstants.*;

import java.util.HashMap;
import java.util.Map;

import com.reliaquest.api.constants.ErrorConstants;
import com.reliaquest.api.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiResponse> handleNotFound(EmployeeNotFoundException ex) {
        log.warn("Employee not found: {}", ex.getMessage());
        ApiResponse apiResponse = new ApiResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiResponse> handleTooManyRequest(TooManyRequestsException ex) {
        log.warn("Too many requests exception: {}", ex.getMessage());
        ApiResponse apiResponse = new ApiResponse(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "10").body(apiResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Validation failed for request: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        response.put("message", "Validation failed");
        response.put("errors", fieldErrors);

        log.error("validation errors: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Constraint violation occured: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            fieldErrors.put(fieldName, errorMessage);
        }

        response.put("message", "Validation failed");
        response.put("errors", fieldErrors);

        log.error("Constraint validation errors: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(EmployeeServiceException.class)
    public ResponseEntity<ApiResponse> handleServiceException(EmployeeServiceException ex) {
        log.error("Employee service exception occurred: {} - Error code: {}", ex.getMessage(), ex.getErrorCode(), ex);
        Map<String, String> error = Map.of("error", ex.getMessage());



        switch (ex.getErrorCode()) {
            case ErrorConstants.INVALID_EMPLOYEE_ID:
                ApiResponse apiResponse = new ApiResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
                log.warn("Bad request due to invalid employee ID: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);

            case INVALID_SEARCH_STRING:
                apiResponse = new ApiResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
                log.warn("Bad request due to invalid search string: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
            default:
                log.error("Internal server error in employee service: {}", ex.getMessage());
                apiResponse = new ApiResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        Map<String, String> error = Map.of("error", "An unexpected error occurred. Please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}
