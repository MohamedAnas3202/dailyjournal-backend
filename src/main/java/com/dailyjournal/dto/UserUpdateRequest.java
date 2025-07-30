package com.dailyjournal.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String name;
    private String email;
    private String password;     // new password
    private String oldPassword;  // optional: validate before update
}
