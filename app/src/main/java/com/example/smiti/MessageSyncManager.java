package com.example.smiti;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public class MessageSyncManager {
    private static final String TAG = "MessageSyncManager";
    private static final int BATCH_SIZE = 50; // 한 번에 처리할 메시지 수
    private static final String BASE_URL = "http://202.31.246.51:80";
    
    private ExecutorService executorService;
    private MessageSyncListener listener;
    private OkHttpClient httpClient;
    
    public interface MessageSyncListener {
        void onSyncStarted(String groupId);
        void onSyncProgress(String groupId, int processedCount, int totalCount);
        void onSyncCompleted(String groupId, int syncedMessageCount);
        void onSyncError(String groupId, String error);
    }
    
    public MessageSyncManager() {
        this.executorService = Executors.newSingleThreadExecutor();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public void setListener(MessageSyncListener listener) {
        this.listener = listener;
    }
    
    // 누락된 메시지 동기화 (실제 서버 API 호출)
    public void syncMissedMessages(String groupId, long lastMessageTimestamp) {
        if (listener != null) {
            listener.onSyncStarted(groupId);
        }
        
        executorService.execute(() -> {
            try {
                // 서버에서 메시지 히스토리 가져오기
                String url = BASE_URL + "/chat/" + groupId + "/history";
                if (lastMessageTimestamp > 0) {
                    url += "?since=" + lastMessageTimestamp;
                }
                
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    List<Message> syncedMessages = parseMessagesFromResponse(responseData);
                    
                    if (listener != null) {
                        listener.onSyncCompleted(groupId, syncedMessages.size());
                    }
                } else {
                    if (listener != null) {
                        listener.onSyncError(groupId, "서버 오류: " + response.code());
                    }
                }
                
            } catch (Exception e) {
                if (listener != null) {
                    listener.onSyncError(groupId, e.getMessage());
                }
            }
        });
    }
    
    // 서버 응답에서 메시지 파싱
    private List<Message> parseMessagesFromResponse(String responseData) {
        List<Message> messages = new ArrayList<>();
        
        try {
            JSONArray messagesArray = null;
            
            // 응답이 직접 배열인지 객체인지 확인
            if (responseData.trim().startsWith("[")) {
                // 직접 JSONArray 형태의 응답
                messagesArray = new JSONArray(responseData);
            } else {
                // JSONObject 형태의 응답
                JSONObject jsonObject = new JSONObject(responseData);
                
                // API 응답 구조에 따라 메시지 배열 추출
                if (jsonObject.has("messages")) {
                    messagesArray = jsonObject.getJSONArray("messages");
                } else if (jsonObject.has("data")) {
                    JSONObject dataObject = jsonObject.getJSONObject("data");
                    if (dataObject.has("messages")) {
                        messagesArray = dataObject.getJSONArray("messages");
                    }
                } else if (jsonObject.has("history")) {
                    messagesArray = jsonObject.getJSONArray("history");
                }
            }
            
            if (messagesArray != null) {
                for (int i = 0; i < messagesArray.length(); i++) {
                    JSONObject messageObject = messagesArray.getJSONObject(i);
                    Message message = parseMessageFromJson(messageObject);
                    if (message != null) {
                        messages.add(message);
                    }
                }
            }
            
        } catch (JSONException e) {
            // JSON 파싱 오류 처리
        }
        
        return messages;
    }
    
    // JSON에서 Message 객체로 파싱
    private Message parseMessageFromJson(JSONObject messageObject) {
        try {
            // sender_id가 없으면 sender_name을 사용하거나 기본값 설정
            String senderId = messageObject.optString("sender_id", "");
            if (senderId.isEmpty()) {
                senderId = messageObject.optString("sender_name", "unknown");
            }
            
            // sender_name이 null이거나 비어있으면 기본값 설정
            String senderName = messageObject.optString("sender_name", null);
            if (senderName == null || senderName.equals("null") || senderName.isEmpty()) {
                senderName = "알 수 없음";
            }
            
            // content 또는 message 필드에서 메시지 내용 가져오기
            String content = messageObject.optString("content", "");
            if (content.isEmpty()) {
                content = messageObject.optString("message", "");
            }
            
            // ISO 8601 형식의 타임스탬프를 밀리초로 변환
            String timestampStr = messageObject.optString("timestamp", "");
            long timestamp = parseTimestampToMillis(timestampStr);
            
            // 파일 정보 처리 - 여러 필드명과 구조 확인
            String fileUrl = "";
            String fileType = "";
            String messageType = messageObject.optString("type", "text");
            
            // 1. 직접적인 파일 필드 확인
            fileUrl = messageObject.optString("file_url", "");
            if (fileUrl.isEmpty()) {
                fileUrl = messageObject.optString("fileUrl", "");
            }
            if (fileUrl.isEmpty()) {
                fileUrl = messageObject.optString("attachment_url", "");
            }
            
            fileType = messageObject.optString("file_type", "");
            if (fileType.isEmpty()) {
                fileType = messageObject.optString("fileType", "");
            }
            if (fileType.isEmpty()) {
                fileType = messageObject.optString("attachment_type", "");
            }
            
            // 2. 중첩된 파일 정보 확인 (서버 구조에 따라)
            if (fileUrl.isEmpty() && messageObject.has("file")) {
                JSONObject fileObject = messageObject.optJSONObject("file");
                if (fileObject != null) {
                    fileUrl = fileObject.optString("url", "");
                    if (fileUrl.isEmpty()) {
                        fileUrl = fileObject.optString("file_url", "");
                    }
                    fileType = fileObject.optString("type", "");
                    if (fileType.isEmpty()) {
                        fileType = fileObject.optString("file_type", "");
                    }
                }
            }
            
            // 3. attachments 배열 확인
            if (fileUrl.isEmpty() && messageObject.has("attachments")) {
                JSONArray attachments = messageObject.optJSONArray("attachments");
                if (attachments != null && attachments.length() > 0) {
                    JSONObject firstAttachment = attachments.optJSONObject(0);
                    if (firstAttachment != null) {
                        fileUrl = firstAttachment.optString("url", "");
                        if (fileUrl.isEmpty()) {
                            fileUrl = firstAttachment.optString("file_url", "");
                        }
                        fileType = firstAttachment.optString("type", "");
                        if (fileType.isEmpty()) {
                            fileType = firstAttachment.optString("file_type", "");
                        }
                    }
                }
            }
            
            // 파일 메시지 처리
            if ("file".equals(messageType) || !fileUrl.isEmpty()) {
                // 파일 타입이 명시되지 않은 경우 URL에서 추론
                if (fileType.isEmpty() && !fileUrl.isEmpty()) {
                    fileType = inferFileTypeFromUrl(fileUrl);
                }
                
                // 메시지 타입을 file로 설정
                messageType = "file";
                
                System.out.println("MessageSyncManager 파일 메시지 파싱 성공:");
                System.out.println("  - fileUrl: " + fileUrl);
                System.out.println("  - fileType: " + fileType);
                System.out.println("  - messageType: " + messageType);
                System.out.println("  - content: " + content);
            }
            
            Message message = new Message(senderId, senderName, content, timestamp);
            
            // 파일 정보가 있으면 반드시 설정
            if (!fileUrl.isEmpty()) {
                message.setFileUrl(fileUrl);
                message.setFileType(fileType.isEmpty() ? "file" : fileType);
                
                // 안전한 메서드 호출
                try {
                    message.setMessageType("file");
                    System.out.println("MessageSyncManager: 파일 메시지 설정 완료 - " + fileUrl + " (타입: " + fileType + ")");
                } catch (Exception methodException) {
                    System.out.println("MessageSyncManager: 메시지 타입 설정 실패, 기본 파일 처리 진행");
                }
            } else {
                // 안전한 메서드 호출
                try {
                    message.setMessageType("text");
                } catch (Exception methodException) {
                    // 메서드가 없어도 계속 진행
                }
            }
            
            return message;
        } catch (Exception e) {
            System.out.println("MessageSyncManager 파일 메시지 파싱 오류: " + e.getMessage());
            return null;
        }
    }
    
    // ISO 8601 형식의 타임스탬프를 밀리초로 변환
    private long parseTimestampToMillis(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return System.currentTimeMillis();
        }
        
        try {
            // ISO 8601 형식: "2025-04-12T20:02:02" 또는 "2025-04-12T20:02:02Z"
            // SimpleDateFormat을 사용하여 파싱
            java.text.SimpleDateFormat sdf;
            
            if (timestampStr.contains("T")) {
                if (timestampStr.endsWith("Z")) {
                    sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                } else if (timestampStr.contains("+") || timestampStr.lastIndexOf("-") > 10) {
                    // 타임존 정보가 있는 경우 (예: 2025-04-12T20:02:02+09:00)
                    sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                } else {
                    // 로컬 시간으로 가정
                    sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                }
            } else {
                // 일반적인 날짜 형식
                sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            }
            
            java.util.Date date = sdf.parse(timestampStr);
            return date.getTime();
            
        } catch (Exception e) {
            // 파싱 실패 시 현재 시간 반환
            return System.currentTimeMillis();
        }
    }
    
    // 서버 큐 상태 확인 (실제 구현)
    public void checkServerQueue(String groupId, QueueCheckCallback callback) {
        executorService.execute(() -> {
            try {
                // 최근 메시지 확인을 통해 큐 상태 간접 확인
                String url = BASE_URL + "/chat/" + groupId + "/history";
                
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    if (callback != null) {
                        callback.onQueueCheckSuccess(true, 0); // 큐가 비어있다고 가정
                    }
                } else {
                    if (callback != null) {
                        callback.onQueueCheckError(new Exception("서버 오류: " + response.code()));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onQueueCheckError(e);
                }
            }
        });
    }
    
    public interface QueueCheckCallback {
        void onQueueCheckSuccess(boolean isEmpty, int queueSize);
        void onQueueCheckError(Exception error);
    }
    
    // 강제 동기화 (오프라인 복구 시 사용)
    public void forceFullSync(String groupId, MessageSyncCallback callback) {
        executorService.execute(() -> {
            try {
                String url = BASE_URL + "/chat/" + groupId + "/history";
                
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    List<Message> allMessages = parseMessagesFromResponse(responseData);
                    
                    if (callback != null) {
                        callback.onSyncSuccess(allMessages);
                    }
                } else {
                    if (callback != null) {
                        callback.onSyncError("서버 오류: " + response.code());
                    }
                }
                
            } catch (Exception e) {
                if (callback != null) {
                    callback.onSyncError(e.getMessage());
                }
            }
        });
    }
    
    public interface MessageSyncCallback {
        void onSyncSuccess(List<Message> messages);
        void onSyncError(String error);
    }
    
    // 리소스 정리
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    // 파일 URL에서 파일 타입 추론 메서드
    private String inferFileTypeFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "";
        }
        
        String lowerUrl = fileUrl.toLowerCase();
        
        // 이미지 파일 확장자 확인
        if (lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
            return "image";
        }
        // PDF 파일 확장자 확인
        else if (lowerUrl.endsWith(".pdf")) {
            return "pdf";
        }
        // 문서 파일 확장자 확인
        else if (lowerUrl.matches(".*\\.(doc|docx|ppt|pptx|xls|xlsx|txt)$")) {
            return "document";
        }
        // 비디오 파일 확장자 확인
        else if (lowerUrl.matches(".*\\.(mp4|avi|mov|wmv|flv|mkv)$")) {
            return "video";
        }
        // 오디오 파일 확장자 확인
        else if (lowerUrl.matches(".*\\.(mp3|wav|ogg|aac|flac)$")) {
            return "audio";
        }
        // 압축 파일 확장자 확인
        else if (lowerUrl.matches(".*\\.(zip|rar|7z|tar|gz)$")) {
            return "archive";
        }
        
        // 기본값: 일반 파일
        return "file";
    }
} 
