package com.example.smiti.api;

import com.google.gson.annotations.SerializedName;

public class AdminRequest {
    @SerializedName("email")
    private String email;

    public AdminRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
} 