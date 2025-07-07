package com.learnbalochi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://learnbalochi.com",
        "https://learnbalochi.com", 
        "http://localhost:4200",
        "http://localhost:4100",
        "http://localhost:8084"
    );
    
    private HttpHeaders createCorsHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        
        String origin = request.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            headers.add("Access-Control-Allow-Origin", origin);
        } else {
            // Fallback to first allowed origin for development
            headers.add("Access-Control-Allow-Origin", "http://localhost:8084");
        }
        
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "*");
        headers.add("Access-Control-Allow-Credentials", "true");
        
        return headers;
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "File too large. Maximum upload size exceeded.");
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        HttpHeaders headers = createCorsHeaders(request);
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .headers(headers)
                .body(error);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(MultipartException exc) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "File upload error: " + exc.getMessage());
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        HttpHeaders headers = createCorsHeaders(request);
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .headers(headers)
                .body(error);
    }
} 