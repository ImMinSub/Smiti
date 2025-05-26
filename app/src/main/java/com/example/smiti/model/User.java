package com.example.smiti.model;

public class User {
    private String name;
    private String email;
    private String smbti;
    
    public User() {
        // 기본 생성자
    }
    
    public User(String name, String email, String smbti) {
        this.name = name;
        this.email = email;
        this.smbti = smbti;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getSmbti() {
        return smbti;
    }
    
    public void setSmbti(String smbti) {
        this.smbti = smbti;
    }
} 
