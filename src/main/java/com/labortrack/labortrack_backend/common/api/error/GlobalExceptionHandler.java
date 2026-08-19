package com.labortrack.labortrack_backend.common.api.error;

import com.labortrack.labortrack_backend.common.exception.DuplicateResourceException;
import com.labortrack.labortrack_backend.common.exception.InvalidWorkSessionStateException;
import com.labortrack.labortrack_backend.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Request body contains malformed or unreadable JSON",
                request.getRequestURI(),
                null
        );
    }

    /**
     * This method-exception handles invalid request parameter values, such
     * as when a parameter cannot be converted to the expected type. If
     * The expected type is an enum, the response also shows the list of
     * allowed enum values.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        // default error message
        String message = "Invalid value for request parameter: " + ex.getName();

        // if the expected parameter is an enum, give a more detail error message
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            Object[] allowedValues = ex.getRequiredType().getEnumConstants();

            message =
                    "Invalid value '" + ex.getValue()
                    + "' for parameter '" + ex.getName()
                    + "'. Allowed values: "
                    + java.util.Arrays.toString(allowedValues);
        }

        // return a standard 400 bad request API error response
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request){
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResourceException(
                    DuplicateResourceException ex,
                    HttpServletRequest request){
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(InvalidWorkSessionStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidWorkSessionStateException(
                    InvalidWorkSessionStateException ex,
                    HttpServletRequest request){
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
                    MethodArgumentNotValidException ex,
                    HttpServletRequest request){
        // including field errors for a more generic error response
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for(FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request.getRequestURI(),
                validationErrors
        );
    }

    // HELPER METHODS
    ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                validationErrors
        );
        return ResponseEntity.status(status).body(response);
    }
}
