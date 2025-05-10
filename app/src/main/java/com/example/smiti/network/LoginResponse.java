package com.example.smiti.network;

/**
 * 로그인 응답을 처리하기 위한 모델 클래스
 */
public class LoginResponse {
    private String user_id; // 서버 응답의 JSON 필드명과 일치
    private String email;
    private String name;
    private String mbti;
    
    // 생성자
    public LoginResponse(String user_id, String email, String name, String mbti) {
        this.user_id = user_id;
        this.email = email;
        this.name = name;
        this.mbti = mbti;
    }
    
    // Getter 메소드
    public String getUserId() {
        return user_id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getName() {
        return name;
    }
    
    public String getMbti() {
        return mbti;
    }
    
    // Setter 메소드
    public void setUserId(String user_id) {
        this.user_id = user_id;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setMbti(String mbti) {
        this.mbti = mbti;
    }
} 