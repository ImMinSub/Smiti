package com.example.smiti.api;
//그룹찾기 요청을 위한 데이터 모델
// 사용자 요청 정보를 포함하여 적합한 그룹을 찾을 때 사용용
public class FindGroupRequest {
    private String email;
    private String smbti;
    private String name;
    private String user_request;
    
    public FindGroupRequest(String email, String smbti, String name, String user_request) {
        this.email = email;
        this.smbti = smbti;
        this.name = name;
        this.user_request = user_request;
    }
    
    // Getter와 Setter
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUser_request() {
        return user_request;
    }
    
    public void setUser_request(String user_request) {
        this.user_request = user_request;
    }
} 