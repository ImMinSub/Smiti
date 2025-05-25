package com.example.smiti;

import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ChatMessage {
    private static final String TAG = "ChatMessage";

    private String type;        // 메시지 타입 ("message", "file" 등)
    private String senderId;    // 발신자 이메일 주소
    private String senderName;  // 발신자 이름
    private String content;     // 메시지 내용
    private String timestamp;   // 서버가 생성한 타임스탬프
    private String groupId;     // 그룹 ID
    private String fileUrl;     // 첨부 파일 URL
    private String fileType;    // 첨부 파일 타입 ("image", "document" 등)
    private String localId;     // 클라이언트에서 생성한 고유 메시지 ID

    // Getters and Setters
    public String getLocalId() { return localId; }
    public void setLocalId(String localId) { this.localId = localId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    // 빈 생성자
    public ChatMessage() {
    }

    // 텍스트 메시지 생성자
    public ChatMessage(String senderId, String senderName, String content) {
        this.type = "message";
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
    }

    // 파일 메시지 생성자 - 타입을 "file"로 고정
    public ChatMessage(String senderId, String senderName, String content, String fileUrl, String fileType) {
        this.type = "file";  // 중요: 파일 메시지는 반드시 "file" 타입
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
    }

    /**
     * JSON 문자열로부터 ChatMessage 객체를 생성합니다.
     */
    public static ChatMessage fromJson(String jsonString) throws JSONException {
        JSONObject json;
        try {
            json = new JSONObject(jsonString);
        } catch (JSONException e) {
            Log.e(TAG, "Invalid JSON string: " + jsonString, e);
            throw e;
        }

        ChatMessage message = new ChatMessage();
        Log.d(TAG, "수신된 JSON: " + jsonString);

        try {
            message.type = json.optString("type", "message");
            message.senderId = json.optString("sender_id", "");
            message.senderName = json.optString("sender_name", "");
            message.content = json.optString("message", "");
            message.timestamp = json.optString("timestamp", null);
            message.groupId = json.optString("group_id", "");
            message.fileUrl = json.optString("file_url", json.optString("fileUrl", ""));
            message.fileType = json.optString("file_type", json.optString("fileType", ""));
            message.localId = json.optString("localId", json.optString("localID", null));

            // 파일 URL이 있으면 자동으로 파일 타입으로 설정
            if (message.fileUrl != null && !message.fileUrl.isEmpty() && 
                (message.type == null || message.type.equals("message") || message.type.equals("text"))) {
                Log.w(TAG, "파일 URL이 있지만 타입이 '" + message.type + "'입니다. 'file'로 수정합니다.");
                message.type = "file";
            }

            if (message.senderName == null || message.senderName.trim().isEmpty()) {
                if (message.senderId != null && !message.senderId.isEmpty()) {
                    message.senderName = message.senderId;
                } else {
                    message.senderName = "알 수 없음";
                }
            }
            
            Log.d(TAG, "최종 파싱 메시지: senderId=[" + message.senderId +
                    "], type=[" + message.type +
                    "], fileUrl=[" + (message.fileUrl != null ? message.fileUrl : "없음") + "]");

        } catch (Exception e) {
            Log.e(TAG, "JSON 파싱 중 예기치 않은 오류", e);
        }

        return message;
    }

    /**
     * ChatMessage 객체를 JSON 문자열로 변환합니다.
     */
    public String toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("type", type);
            json.put("sender_id", senderId);
            json.put("sender_name", senderName);
            json.put("message", content);

            if (groupId != null && !groupId.isEmpty()) {
                json.put("group_id", groupId);
            }
            if (localId != null && !localId.isEmpty()) {
                json.put("localId", localId);
            }
            
            // 파일 메시지인 경우 파일 정보 추가
            if ("file".equals(type)) {
                if (fileUrl != null && !fileUrl.isEmpty()) {
                    json.put("file_url", fileUrl);
                    json.put("fileUrl", fileUrl); // 서버 호환성을 위한 중복 키
                }
                if (fileType != null && !fileType.isEmpty()) {
                    json.put("file_type", fileType);
                    json.put("fileType", fileType); // 서버 호환성을 위한 중복 키
                }
                
                Log.d(TAG, "파일 메시지 JSON 생성: type=file, fileUrl=" + fileUrl + ", fileType=" + fileType);
            }
            
            return json.toString();
        } catch (JSONException e) {
            Log.e(TAG, "JSON 생성 오류", e);
            return "{}";
        }
    }

    /**
     * ChatMessage 객체를 UI 어댑터에서 사용하는 Message 객체로 변환합니다.
     */
    public Message toUIMessage() {
        long timeInMillis = parseServerTimestamp(this.timestamp);

        if ("file".equals(this.type) && this.fileUrl != null && !this.fileUrl.isEmpty()) {
            return new Message(senderId, senderName, content, timeInMillis, fileUrl, fileType);
        } else {
            return new Message(senderId, senderName, content, timeInMillis);
        }
    }

    /**
     * 서버에서 받은 타임스탬프 문자열을 long 타입 밀리초로 파싱합니다.
     */
    private long parseServerTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            Log.w(TAG, "서버 타임스탬프가 null이거나 비어있음. 현재 시간 사용.");
            return System.currentTimeMillis();
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            Date date = sdf.parse(timestamp);
            return date.getTime();
        } catch (ParseException e) {
            try {
                SimpleDateFormat sdfMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
                sdfMillis.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                Date dateMillis = sdfMillis.parse(timestamp);
                return dateMillis.getTime();
            } catch (ParseException e2) {
                Log.e(TAG, "서버 Timestamp 파싱 오류: " + timestamp + ". 현재 시간 사용.", e);
                return System.currentTimeMillis();
            }
        } catch (Exception e) {
            Log.e(TAG, "서버 Timestamp 파싱 중 알 수 없는 오류: " + timestamp + ". 현재 시간 사용.", e);
            return System.currentTimeMillis();
        }
    }
}
