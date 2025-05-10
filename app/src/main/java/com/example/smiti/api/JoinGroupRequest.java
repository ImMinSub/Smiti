package com.example.smiti.api;

/**
 * 그룹 참여 요청을 위한 모델 클래스
 * 참고: 그룹 ID는 이제 URL 경로에 포함되지만, 
 * 백엔드 API와의 호환성을 위해 group_id 필드는 유지
 */
public class JoinGroupRequest {
    private String email;
    private int group_id;
    private String group_name;

    public JoinGroupRequest(String email, int group_id, String group_name) {
        this.email = email;
        this.group_id = group_id;
        this.group_name = group_name;
    }
    
    /**
     * 새로운 API 형식에 맞춘 생성자 (그룹 ID는 URL 경로에 포함)
     */
    public JoinGroupRequest(String email, String group_name) {
        this.email = email;
        this.group_name = group_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getGroup_id() {
        return group_id;
    }

    public void setGroup_id(int group_id) {
        this.group_id = group_id;
    }

    public String getGroup_name() {
        return group_name;
    }

    public void setGroup_name(String group_name) {
        this.group_name = group_name;
    }
}