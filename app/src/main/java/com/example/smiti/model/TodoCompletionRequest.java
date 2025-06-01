package com.example.smiti.model;

import com.google.gson.annotations.SerializedName;

public class TodoCompletionRequest {
    @SerializedName("user_email")
    private String userEmail;

    @SerializedName("is_completed")
    private boolean isCompleted;

    public TodoCompletionRequest(String userEmail, boolean isCompleted) {
        this.userEmail = userEmail;
        this.isCompleted = isCompleted;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
} 