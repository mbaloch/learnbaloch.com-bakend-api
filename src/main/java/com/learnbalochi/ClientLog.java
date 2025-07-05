package com.learnbalochi;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

public class ClientLog {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("level")
    private LogLevel level;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("user_email")
    private String userEmail;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("user_agent")
    private String userAgent;
    
    @JsonProperty("error_stack")
    private String errorStack;
    
    @JsonProperty("additional_data")
    private Map<String, Object> additionalData;
    
    @JsonProperty("client_version")
    private String clientVersion;
    
    @JsonProperty("environment")
    private String environment;

    public enum LogLevel {
        ERROR, WARN, INFO, DEBUG
    }

    // Default constructor
    public ClientLog() {
        this.timestamp = Instant.now();
    }

    // Constructor with required fields
    public ClientLog(LogLevel level, String message) {
        this();
        this.level = level;
        this.message = message;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public LogLevel getLevel() { return level; }
    public void setLevel(LogLevel level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getErrorStack() { return errorStack; }
    public void setErrorStack(String errorStack) { this.errorStack = errorStack; }

    public Map<String, Object> getAdditionalData() { return additionalData; }
    public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }

    public String getClientVersion() { return clientVersion; }
    public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
} 