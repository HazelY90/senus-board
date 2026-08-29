package com.hazely.senusboard.exceptions;

import com.hazely.senusboard.dtos.ErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Converts service status exceptions into consistent API responses. */
@RestControllerAdvice
public class GlobalHandler {

    /** Handles expected request and lookup errors raised by services. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorDto> handleStatus(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatusCode status = ex.getStatusCode();
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String error = resolved == null ? "HTTP " + status.value() : resolved.getReasonPhrase();
        String message = ex.getReason() == null ? error : ex.getReason();
        ErrorDto body = new ErrorDto(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> handleValidation(MethodArgumentNotValidException e){
        var errors=new HashMap<String, String>();
        e.getBindingResult().getFieldErrors().forEach(
                error->{
                    errors.put(error.getField(),error.getDefaultMessage());
                }
        );

        return ResponseEntity.badRequest().body(errors);
    }

    // Handles requests with a missing body or malformed JSON.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(
                Map.of("error", "Request body is missing or malformed.")
        );
    }
}
