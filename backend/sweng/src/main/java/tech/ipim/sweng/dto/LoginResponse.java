package tech.ipim.sweng.dto;

import java.time.LocalDateTime;

public class LoginResponse {
    
    private boolean success;
    private String message;
    private String token;
    private UserDto user;
    private LocalDateTime loginTime;
    
    // Costruttori
    public LoginResponse() {}
    
    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.loginTime = LocalDateTime.now();
    }
    
    public LoginResponse(boolean success, String message, String token, UserDto user) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
        this.loginTime = LocalDateTime.now();
    }
    
    // Factory methods
    public static LoginResponse success(String token, UserDto user) {
        return new LoginResponse(true, "Login effettuato con successo", token, user);
    }
    
    public static LoginResponse error(String message) {
        return new LoginResponse(false, message);
    }
    
    // Getters e Setters
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
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public UserDto getUser() {
        return user;
    }
    
    public void setUser(UserDto user) {
        this.user = user;
    }
    
    public LocalDateTime getLoginTime() {
        return loginTime;
    }
    
    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
}