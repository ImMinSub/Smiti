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
    private static final String TAG = "ChatMessage"; // 로그 태그

    private String type;        // 메시지 타입 ("message", "file" 등)
    private String senderId;    // 발신자 이메일 주소
    private String senderName;  // 발신자 이름
    private String content;     // 메시지 내용
    private String timestamp;   // 서버가 생성한 타임스탬프 (ISO 8601 문자열 예상, KST 기준?)
    private String groupId;     // 그룹 ID
    private String fileUrl;     // 첨부 파일 URL
    private String fileType;    // 첨부 파일 타입 ("image", "document" 등)
    private String localId;     // 클라이언트에서 생성한 고유 메시지 ID (에코 확인용)

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

    // 텍스트 메시지 생성자 (클라이언트 -> 서버)
    public ChatMessage(String senderId, String senderName, String content) {
        this.type = "message";
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
    }

    // 파일 메시지 생성자 (클라이언트 -> 서버)
    public ChatMessage(String senderId, String senderName, String content, String fileUrl, String fileType) {
        this.type = "file";
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
    }

    /**
     * JSON 문자열로부터 ChatMessage 객체를 생성합니다. (서버 -> 클라이언트)
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
        Log.d(TAG, "수신된 JSON (KST 시간 예상): " + jsonString); // 로그 메시지 수정

        try {
            message.type = json.optString("type", "message");
            message.senderId = json.optString("sender_id", "");
            message.senderName = json.optString("sender_name", "");
            message.content = json.optString("message", "");
            message.timestamp = json.optString("timestamp", null);
            message.groupId = json.optString("group_id", "");
            message.fileUrl = json.optString("file_url", "");
            message.fileType = json.optString("file_type", "");
            message.localId = json.optString("localId", json.optString("localID", null));

            if (message.senderName == null || message.senderName.trim().isEmpty()) {
                if (message.senderId != null && !message.senderId.isEmpty()) {
                    message.senderName = message.senderId;
                } else {
                    message.senderName = "알 수 없음";
                }
            }
            Log.d(TAG, "최종 파싱 메시지 (KST 시간 예상): senderId=[" + message.senderId +
                    "], timestamp=[" + message.timestamp + "]");


        } catch (Exception e) {
            Log.e(TAG, "JSON 파싱 중 예기치 않은 오류", e);
        }

        return message;
    }

    /**
     * ChatMessage 객체를 JSON 문자열로 변환합니다. (클라이언트 -> 서버)
     */
    public String toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("type", type);
            json.put("sender_id", senderId);
            json.put("sender_name", senderName);
            json.put("message", content);
            // timestamp는 서버에서 추가

            if (groupId != null && !groupId.isEmpty()) {
                json.put("group_id", groupId);
            }
            if (localId != null && !localId.isEmpty()) {
                json.put("localId", localId);
            }
            if (type != null && type.equals("file")) {
                if (fileUrl != null && !fileUrl.isEmpty()) {
                    json.put("file_url", fileUrl);
                }
                if (fileType != null && !fileType.isEmpty()) {
                    json.put("file_type", fileType);
                }
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
        Log.d(TAG, "UI 메시지 변환 (KST 시간 파싱 가정): parsedTimestamp=" + timeInMillis); // 로그 메시지 수정

        if (this.type != null && this.type.equals("file") && this.fileUrl != null && !this.fileUrl.isEmpty()) {
            return new Message(senderId, senderName, content, timeInMillis, fileUrl, fileType);
        } else {
            return new Message(senderId, senderName, content, timeInMillis);
        }
    }

    /**
     * 서버에서 받은 타임스탬프 문자열(ISO 8601 형식, KST 기준 예상)을 long 타입 밀리초로 파싱합니다.
     *
     * @param timestamp 서버에서 받은 타임스탬프 문자열 (예: "2025-04-28T11:03:13.026182")
     * @return 파싱된 밀리초 값 (long, UTC 기준). 실패 시 현재 시간 반환.
     */
    private long parseServerTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            Log.w(TAG, "서버 타임스탬프가 null이거나 비어있음. 현재 시간 사용.");
            return System.currentTimeMillis();
        }
        try {
            // 서버에서 보내는 형식에 맞는 포맷 지정 (마이크로초 6자리)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            // *** 서버 시간이 KST 기준이라고 가정하고 파싱 ***
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul")); // KST 기준 파싱
            Date date = sdf.parse(timestamp);
            // Date.getTime()은 항상 UTC 기준 밀리초를 반환함.
            return date.getTime();
        } catch (ParseException e) {
            // .SSS (밀리초 3자리) 형식 시도
            try {
                SimpleDateFormat sdfMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
                // *** 서버 시간이 KST 기준이라고 가정하고 파싱 ***
                sdfMillis.setTimeZone(TimeZone.getTimeZone("Asia/Seoul")); // KST 기준 파싱
                Date dateMillis = sdfMillis.parse(timestamp);
                Log.w(TAG,"Timestamp 파싱 성공 (.SSS 형식, KST 가정): " + timestamp);
                return dateMillis.getTime();
            } catch (ParseException e2) {
                Log.e(TAG, "서버 Timestamp (ISO 8601, KST 가정) 파싱 오류: " + timestamp + ". 현재 시간 사용.", e);
                return System.currentTimeMillis(); // 최종 실패 시 현재 시간 반환
            }
        } catch (Exception e) {
            Log.e(TAG, "서버 Timestamp 파싱 중 알 수 없는 오류: " + timestamp + ". 현재 시간 사용.", e);
            return System.currentTimeMillis(); // 그 외 예외 발생 시 현재 시간 반환
        }
    }
}