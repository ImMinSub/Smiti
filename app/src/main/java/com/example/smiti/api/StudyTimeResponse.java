package com.example.smiti.api;

import java.util.List;
import java.util.Map;

public class StudyTimeResponse {
    private Map<String, List<String>> availableTimes; // 사용 가능한 시간대
    private List<String> recommendedTimes; // 추천된 시간대
    
    public StudyTimeResponse() {
    }
    
    public Map<String, List<String>> getAvailableTimes() {
        return availableTimes;
    }
    
    public void setAvailableTimes(Map<String, List<String>> availableTimes) {
        this.availableTimes = availableTimes;
    }
    
    public List<String> getRecommendedTimes() {
        return recommendedTimes;
    }
    
    public void setRecommendedTimes(List<String> recommendedTimes) {
        this.recommendedTimes = recommendedTimes;
    }
    
    @Override
    public String toString() {
        return "StudyTimeResponse{" +
                "availableTimes=" + availableTimes +
                ", recommendedTimes=" + recommendedTimes +
                '}';
    }
} 