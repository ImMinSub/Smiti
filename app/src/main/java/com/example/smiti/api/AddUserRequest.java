package com.example.smiti.api;
//새로운 사용자 등록용용
public class AddUserRequest {
    private String email;
    private String password;
    private String name;
    private String smbti;
    
    public AddUserRequest(String email, String password, String name, String smbti) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.smbti = smbti;
    }
    
    // Getter와 Setter
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSmbti() {
        return smbti;
    }
    
    public void setSmbti(String smbti) {
        this.smbti = smbti;
    }
} 