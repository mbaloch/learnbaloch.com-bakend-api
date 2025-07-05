package com.learnbalochi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/logs")
public class ClientLogController {
    private static final Logger logger = LoggerFactory.getLogger(ClientLogController.class);
    
    @Autowired
    private MyFirestoreService firestoreService;
    
    private static final String COLLECTION_NAME = "client_logs";

    @PostMapping("/error")
    public ResponseEntity<Map<String, Object>> logError(@RequestBody ClientLog clientLog) {
        return logMessage(clientLog, ClientLog.LogLevel.ERROR);
    }

    @PostMapping("/warn")
    public ResponseEntity<Map<String, Object>> logWarning(@RequestBody ClientLog clientLog) {
        return logMessage(clientLog, ClientLog.LogLevel.WARN);
    }

    @PostMapping("/info")
    public ResponseEntity<Map<String, Object>> logInfo(@RequestBody ClientLog clientLog) {
        return logMessage(clientLog, ClientLog.LogLevel.INFO);
    }

    @PostMapping("/debug")
    public ResponseEntity<Map<String, Object>> logDebug(@RequestBody ClientLog clientLog) {
        return logMessage(clientLog, ClientLog.LogLevel.DEBUG);
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> logBatch(@RequestBody ClientLog[] clientLogs) {
        try {
            int successCount = 0;
            int errorCount = 0;
            
            for (ClientLog log : clientLogs) {
                try {
                    enrichLogData(log);
                    Map<String, Object> logData = convertToMap(log);
                    firestoreService.addDocumentToCollection(COLLECTION_NAME, logData);
                    successCount++;
                } catch (Exception e) {
                    logger.error("Failed to save log entry: {}", e.getMessage());
                    errorCount++;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("Processed %d logs: %d successful, %d failed", 
                clientLogs.length, successCount, errorCount));
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to process batch logs: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to process batch logs: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<PaginatedResponse> getLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        try {
            // For now, return all logs with basic pagination
            // TODO: Implement filtering by level, userId, date range
            PaginatedResponse response = firestoreService.getDocumentsWithoutContentPaginated(COLLECTION_NAME, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to retrieve logs: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getLogStats(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        try {
            // TODO: Implement log statistics
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalLogs", 0);
            stats.put("errorCount", 0);
            stats.put("warningCount", 0);
            stats.put("infoCount", 0);
            stats.put("debugCount", 0);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to get log stats: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<Map<String, Object>> logMessage(ClientLog clientLog, ClientLog.LogLevel level) {
        try {
            // Set the log level
            clientLog.setLevel(level);
            
            // Enrich the log data with server-side information
            enrichLogData(clientLog);
            
            // Convert to map for Firestore storage
            Map<String, Object> logData = convertToMap(clientLog);
            
            // Save to Firestore
            String documentId = firestoreService.addDocumentToCollection(COLLECTION_NAME, logData);
            
            // Log to server logs as well
            String serverLogMessage = String.format("Client %s: %s (User: %s, URL: %s)", 
                level.name(), clientLog.getMessage(), 
                clientLog.getUserId() != null ? clientLog.getUserId() : "anonymous",
                clientLog.getUrl() != null ? clientLog.getUrl() : "unknown");
            
            switch (level) {
                case ERROR:
                    logger.error(serverLogMessage);
                    break;
                case WARN:
                    logger.warn(serverLogMessage);
                    break;
                case INFO:
                    logger.info(serverLogMessage);
                    break;
                case DEBUG:
                    logger.debug(serverLogMessage);
                    break;
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Log saved successfully");
            response.put("logId", documentId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to save client log: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to save log: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private void enrichLogData(ClientLog clientLog) {
        // Add server-side timestamp if not provided
        if (clientLog.getTimestamp() == null) {
            clientLog.setTimestamp(java.time.Instant.now());
        }
        
        // Add server-side information
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Add user agent if not provided
                if (clientLog.getUserAgent() == null) {
                    clientLog.setUserAgent(request.getHeader("User-Agent"));
                }
                
                // Add URL if not provided
                if (clientLog.getUrl() == null) {
                    clientLog.setUrl(request.getRequestURL().toString());
                }
                
                // Add session ID if not provided
                if (clientLog.getSessionId() == null && request.getSession() != null) {
                    clientLog.setSessionId(request.getSession().getId());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to enrich log data with request information: {}", e.getMessage());
        }
    }

    private Map<String, Object> convertToMap(ClientLog clientLog) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", clientLog.getId());
        map.put("timestamp", clientLog.getTimestamp().toString());
        map.put("level", clientLog.getLevel().name());
        map.put("message", clientLog.getMessage());
        map.put("userId", clientLog.getUserId());
        map.put("userEmail", clientLog.getUserEmail());
        map.put("sessionId", clientLog.getSessionId());
        map.put("url", clientLog.getUrl());
        map.put("userAgent", clientLog.getUserAgent());
        map.put("errorStack", clientLog.getErrorStack());
        map.put("additionalData", clientLog.getAdditionalData());
        map.put("clientVersion", clientLog.getClientVersion());
        map.put("environment", clientLog.getEnvironment());
        map.put("serverTimestamp", java.time.Instant.now().toString());
        
        return map;
    }
} 