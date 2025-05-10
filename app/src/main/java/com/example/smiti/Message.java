package com.example.smiti;

import android.util.Log;

public class Message {
    private static final String TAG = "Message";
    private String messageId;
    private String senderId;
    private String senderName;
    private String message;
    private long timestamp;  // 타임스탬프를 long 타입으로 변경 
    private String fileUrl;
    private String fileType; // "image", "document", etc.
    
    // 기본 생성자 (Firebase 등의 데이터베이스에서 필요)
    public Message() {
    }
    
    // 텍스트 메시지용 생성자
    public Message(String senderId, String senderName, String message, String timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = cleanMessage(message, senderName);
        try {
            this.timestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            this.timestamp = System.currentTimeMillis();
        }
        Log.d(TAG, "메시지 생성: senderId=" + senderId + ", String 타임스탬프=" + timestamp);
    }
    
    // 수정된 텍스트 메시지용 생성자 (타임스탬프를 long으로 받음)
    public Message(String senderId, String senderName, String message, long timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = cleanMessage(message, senderName);
        this.timestamp = timestamp;
        Log.d(TAG, "메시지 생성: senderId=" + senderId + ", long 타임스탬프=" + timestamp);
    }
    
    // 파일 첨부 메시지용 생성자
    public Message(String senderId, String senderName, String message, String timestamp, String fileUrl, String fileType) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = cleanMessage(message, senderName);
        try {
            this.timestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            this.timestamp = System.currentTimeMillis();
        }
        this.fileUrl = fileUrl;
        this.fileType = fileType;
    }
    
    // 수정된 파일 첨부 메시지용 생성자 (타임스탬프를 long으로 받음)
    public Message(String senderId, String senderName, String message, long timestamp, String fileUrl, String fileType) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = cleanMessage(message, senderName);
        this.timestamp = timestamp;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
    }
    
    // 메시지 텍스트에서 사용자 이름 접두사 제거
    private String cleanMessage(String message, String senderName) {
        if (message == null) {
            return "";
        }
        
        // "사용자명: 메시지내용" 형식 처리
        if (message.contains(":")) {
            // 정확한 "사용자명:" 패턴 확인
            if (senderName != null && !senderName.isEmpty() && message.startsWith(senderName + ":")) {
                return message.substring(senderName.length() + 1).trim();
            }
            
            // 일반적인 "이름:" 패턴 확인 (첫 번째 콜론 이후의 내용만 추출)
            int colonIndex = message.indexOf(':');
            if (colonIndex > 0) {
                return message.substring(colonIndex + 1).trim();
            }
        }
        
        return message;
    }
    
    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
        Log.d(TAG, "발신자 ID 설정: " + senderId);
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = cleanMessage(message, this.senderName);
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    // 역호환성을 위한 메서드
    public void setTimestamp(String timestamp) {
        try {
            this.timestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public boolean hasFile() {
        return fileUrl != null && !fileUrl.isEmpty();
    }
} 