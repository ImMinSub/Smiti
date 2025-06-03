package com.example.smiti.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Group implements Serializable {


    private static final long serialVersionUID = 1L;
    private String id;

    @SerializedName(value = "name", alternate = {"group_name"})
    private String name;

    private String description;
    private int memberCount;
    private String category;

    @SerializedName("score") // 서버 응답 필드명을 다시 "score"로 지정
    private double mbtiScore;  // MBTI 궁합 점수 또는 AI 추천 점수 (소수점 지원)

    @SerializedName("max_members") // JSON 필드명과 일치하도록 어노테이션 추가 (필요시)
    private int max_members;

    @SerializedName("current_members") // JSON 필드명과 일치하도록 어노테이션 추가 (필요시)
    private int current_members;

    // 모든 필드를 초기화하는 생성자 (기존 유지)
    public Group(String id, String name, String description, int memberCount, String category, double mbtiScore, int max_members, int current_members) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.memberCount = memberCount; // 이 필드는 Group 생성 시 필요에 따라 유지
        this.category = category;
        this.mbtiScore = mbtiScore;
        this.max_members = max_members;
        this.current_members = current_members;
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

    public int getMemberCount() { // 기존 memberCount 필드 getter
        return memberCount;
    }

    public String getCategory() {
        return category;
    }

    public double getMbtiScore() {
        return mbtiScore;
    }

    public int getMax_members() { // max_members getter
        return max_members;
    }

    public int getCurrent_members() { // current_members getter
        return current_members;
    }

    // MBTI 점수를 정수로 표시하기 위한 헬퍼 메소드 (기존 유지)
    public int getMbtiScoreAsInt() {
        return (int) Math.round(mbtiScore);
    }

    // description이 null인 경우를 위한 안전 메소드 추가 (기존 유지)
    public boolean hasDescription() {
        return description != null && !description.isEmpty();
    }

    // 기본 생성자 추가 (Gson 및 Serializable을 위해 중요)
    public Group() {
        // 빈 생성자
    }

    // Setter (필요에 따라 추가 - 현재는 Getter 위주)
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setMbtiScore(double mbtiScore) {
        this.mbtiScore = mbtiScore;
    }

    public void setMax_members(int max_members) {
        this.max_members = max_members;
    }

    public void setCurrent_members(int current_members) {
        this.current_members = current_members;
    }
}
