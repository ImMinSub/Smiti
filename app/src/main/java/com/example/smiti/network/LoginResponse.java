package com.example.smiti.network;

import com.google.gson.annotations.SerializedName; // @SerializedName 임포트 추가

/**
 * 로그인 응답을 처리하기 위한 모델 클래스
 */
public class LoginResponse {

    @SerializedName("user_id") // 서버 응답 JSON의 필드명이 "user_id"라면
    private String userId; // Java 변수명 규칙에 따라 userId로 변경 (선택 사항이지만 권장)

    @SerializedName("email")
    private String email;

    @SerializedName("name")
    private String name;

    @SerializedName("mbti")
    private String mbti;

    @SerializedName("admin") // 서버 응답 JSON의 "admin" 필드와 매핑
    private int admin;       // 서버 응답이 정수 1 또는 0으로 오므로 int 타입

    // Gson이 객체 생성 시 사용할 수 있도록 기본 생성자가 있는 것이 좋습니다.
    public LoginResponse() {
    }

    // 모든 필드를 포함하는 생성자 (선택 사항, 주로 테스트나 수동 객체 생성 시 사용)
    public LoginResponse(String userId, String email, String name, String mbti, int admin) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.mbti = mbti;
        this.admin = admin;
    }

    // Getter 메소드
    public String getUserId() {
        return userId;
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

    public int getAdmin() { // admin 필드의 getter 추가
        return admin;
    }

    // Setter 메소드 (일반적으로 응답 DTO에는 Setter가 필수는 아니지만, 필요에 따라 추가)
    public void setUserId(String userId) {
        this.userId = userId;
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

    public void setAdmin(int admin) { // admin 필드의 setter 추가
        this.admin = admin;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", mbti='" + mbti + '\'' +
                ", admin=" + admin +
                '}';
    }
}
