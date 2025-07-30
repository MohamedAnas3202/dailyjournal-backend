package com.dailyjournal.dto;

public class LoginResponse {
    private String message;
    private boolean isAdmin;

    public LoginResponse(String message, boolean isAdmin) {
        this.message = message;
        this.isAdmin = isAdmin;
    }

    // getters and setters
}

