package com.example.smiti.api;

import java.util.List;
import java.util.Map;

public class AvailableTimesRequest {
    private String email;
    private Map<String, List<String>> available_times;

    public AvailableTimesRequest(String email, Map<String, List<String>> available_times) {
        this.email = email;
        this.available_times = available_times;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Map<String, List<String>> getAvailable_times() {
        return available_times;
    }

    public void setAvailable_times(Map<String, List<String>> available_times) {
        this.available_times = available_times;
    }
} 