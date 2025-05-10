package com.example.smiti.api;

public class ChatSummaryResponse {
    private String summary; // AI가 생성한 채팅 요약
    private String timestamp; // 요약 생성 시간
    
    public ChatSummaryResponse() {
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "ChatSummaryResponse{" +
                "summary='" + summary + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
} 