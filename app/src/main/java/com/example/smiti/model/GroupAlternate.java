package com.example.smiti.model;

import com.google.gson.annotations.SerializedName;

public class GroupAlternate {
    private String id;
    
    @SerializedName("name") // API 응답에서 그룹 이름이 'name'으로 오므로 지정
    private String name;
    
    private int max_members; // max_members 필드 추가
    private int current_members; // current_members 필드 추가
    
    // 기타 필드가 API 응답에 포함된다면 여기에 추가합니다.
    // 예: private String description;
    // 예: @SerializedName("member_count") private int memberCount;

    // Getter 메소드
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // max_members getter 추가
    public int getMax_members() {
        return max_members;
    }

    // current_members getter 추가
    public int getCurrent_members() {
        return current_members;
    }

    // 필요에 따라 다른 필드의 Getter 메소드를 추가합니다.

    // Gson이 객체를 생성하기 위한 기본 생성자
    public GroupAlternate() {
    }
} 