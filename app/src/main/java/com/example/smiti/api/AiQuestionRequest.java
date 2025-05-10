package com.example.smiti.api;

import com.google.gson.annotations.SerializedName;

/**
 * AI 질의응답 요청을 위한 클래스
 */
public class AiQuestionRequest {
    @SerializedName("question")
    private String question;
    
    public AiQuestionRequest(String question) {
        this.question = question;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
} 