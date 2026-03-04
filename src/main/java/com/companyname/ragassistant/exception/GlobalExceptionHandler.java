package com.companyname.ragassistant.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.companyname.ragassistant.util.RequestIdUtil;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        log.warn("event=not_found request_id={} path={} message={}", RequestIdUtil.current(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(
                ex.getMessage(),
                "NOT_FOUND",
                request
        ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("event=resource_not_found request_id={} path={} message={}", RequestIdUtil.current(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(
                "Not Found",
                "NOT_FOUND",
                request
        ));
    }

    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex, HttpServletRequest request) {
        String message = ex.getMessage();
        if (ex instanceof MethodArgumentNotValidException manve) {
            message = manve.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            if (message.isBlank()) {
                message = "Validation failed";
            }
        }
        log.warn("event=bad_request request_id={} path={} message={}", RequestIdUtil.current(), request.getRequestURI(), message);
        return ResponseEntity.badRequest().body(errorBody(
                message,
                "INVALID_ARGUMENT",
                request
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("event=malformed_json request_id={} path={} message={}", RequestIdUtil.current(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(errorBody(
                "Malformed JSON request body",
                "INVALID_ARGUMENT",
                request
        ));
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleDependencyUnavailable(DependencyUnavailableException ex, HttpServletRequest request) {
        log.error("event=dependency_unavailable request_id={} path={} message={}",
                RequestIdUtil.current(), request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody(
                "Dependency unavailable",
                "DEPENDENCY_UNAVAILABLE",
                request
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleInternal(Exception ex, HttpServletRequest request) {
        log.error("event=internal_error request_id={} path={} message={}",
                RequestIdUtil.current(), request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
                "Internal Server Error",
                "INTERNAL_ERROR",
                request
        ));
    }

    private Map<String, Object> errorBody(String error, String code, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("code", code);
        body.put("request_id", RequestIdUtil.current());
        body.put("path", request.getRequestURI());
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
