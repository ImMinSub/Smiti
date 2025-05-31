package com.example.smiti.model;

// API 요청 본문에 사용될 JoinGroupRequest 모델 클래스
public class JoinGroupRequest {
    private String email;
    private int group_id;
    private String group_name;

    public JoinGroupRequest(String email, int group_id, String group_name) {
        this.email = email;
        this.group_id = group_id;
        this.group_name = group_name;
    }

    public String getEmail() {
        return email;
    }

    public int getGroup_id() {
        return group_id;
    }

    public String getGroup_name() {
        return group_name;
    }
} 