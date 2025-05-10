package com.example.smiti.api;

public class UpdateSmbtiRequest {
    private String email;
    private String smbti;

    public UpdateSmbtiRequest(String email, String smbti) {
        this.email = email;
        this.smbti = smbti;
    }

    public String getEmail() {
        return email;
    }

    public String getSmbti() {
        return smbti;
    }
} 