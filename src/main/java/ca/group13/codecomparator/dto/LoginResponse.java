package ca.group13.codecomparator.dto;

import java.util.Map;

public class LoginResponse {
    private boolean success;
    private String token;
    private String role;
    private Map<String, Object> user;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String token, String role, Map<String, Object> user) {
        this.success = success;
        this.token = token;
        this.role = role;
        this.user = user;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Map<String, Object> getUser() {
        return user;
    }

    public void setUser(Map<String, Object> user) {
        this.user = user;
    }
}