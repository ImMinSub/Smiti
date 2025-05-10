package com.example.smiti.model;

import com.google.gson.annotations.SerializedName;

public class Group {
    private String id;
    
    @SerializedName("group_name")
    private String name;
    
    private String description;
    private int memberCount;
    private String category;
    
    @SerializedName("score")
    private double mbtiScore;  // MBTI 궁합 점수 (100점 만점) - 소수점 지원을 위해 double로 변경

    public Group(String id, String name, String description, int memberCount, String category, double mbtiScore) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.memberCount = memberCount;
        this.category = category;
        this.mbtiScore = mbtiScore;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public String getCategory() {
        return category;
    }

    public double getMbtiScore() {
        return mbtiScore;
    }
    
    // MBTI 점수를 정수로 표시하기 위한 헬퍼 메소드
    public int getMbtiScoreAsInt() {
        return (int) Math.round(mbtiScore);
    }
    
    // description이 null인 경우를 위한 안전 메소드 추가
    public boolean hasDescription() {
        return description != null && !description.isEmpty();
    }
    
    // 기본 생성자 추가 (Gson을 위해)
    public Group() {
        // 빈 생성자
    }
} 