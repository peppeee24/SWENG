package tech.ipim.sweng.dto;

import java.time.LocalDateTime;

public class RegistrationResponse {

    private boolean success;
    private String message;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;

    public RegistrationResponse() {}

    public RegistrationResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public RegistrationResponse(boolean success, String message, Long userId, String username, LocalDateTime createdAt) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.username = username;
        this.createdAt = createdAt;
    }

    // Factory methods per creare risposte standard
    public static RegistrationResponse success(Long userId, String username, LocalDateTime createdAt) {
        return new RegistrationResponse(true, "Registrazione completata con successo", userId, username, createdAt);
    }

    public static RegistrationResponse error(String message) {
        return new RegistrationResponse(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}