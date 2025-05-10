package com.example.smiti.api;

/**
 * 그룹 생성 요청을 위한 모델 클래스
 */
public class CreateGroupRequest {
    private String group_name;
    private String description;
    private String email;
    private String topics;
    private boolean useAi;

    public CreateGroupRequest() {
    }

    public String getGroup_name() {
        return group_name;
    }

    public void setGroup_name(String group_name) {
        this.group_name = group_name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    public boolean isUseAi() {
        return useAi;
    }

    public void setUseAi(boolean useAi) {
        this.useAi = useAi;
    }
} 