package com.example.smiti;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MessagePaginationManager {
    private static final String TAG = "MessagePaginationManager";
    private static final int PAGE_SIZE = 50; // 한 페이지당 메시지 수
    private static final int PRELOAD_THRESHOLD = 10; // 스크롤 시 미리 로드할 임계값
    private static final String BASE_URL = "http://202.31.246.51:80";
    
    private Context context;
    private String groupId;
    private String currentUserEmail;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    
    // 페이지네이션 상태
    private boolean isLoading = false;
    private boolean hasMoreMessages = true;
    private int currentPage = 0;
    private String oldestMessageTimestamp = null;
    
    // 성능 최적화
    private ExecutorService executorService;
    private Handler mainHandler;
    private Set<String> loadedMessageIds; // 중복 방지용
    private OkHttpClient httpClient;
    
    // 콜백 인터페이스
    public interface PaginationCallback {
        void onLoadingStarted();
        void onLoadingFinished();
        void onMessagesLoaded(List<Message> messages, boolean hasMore);
        void onError(String error);
        void onNoMoreMessages();
    }
    
    private PaginationCallback callback;
    
    public MessagePaginationManager(Context context, String groupId, String currentUserEmail,
                                  RecyclerView recyclerView, MessageAdapter messageAdapter,
                                  List<Message> messageList) {
        this.context = context;
        this.groupId = groupId;
        this.currentUserEmail = currentUserEmail;
        this.recyclerView = recyclerView;
        this.messageAdapter = messageAdapter;
        this.messageList = messageList;
        
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.loadedMessageIds = new HashSet<>();
        
        // 타임아웃 증가된 HTTP 클라이언트
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        
        setupScrollListener();
    }
    
    public void setPaginationCallback(PaginationCallback callback) {
        this.callback = callback;
    }
    
    // 스크롤 리스너 설정 (상단 스크롤 시 이전 메시지 로드) - 개선된 버전
    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private int consecutiveEmptyLoads = 0; // 연속 빈 로드 카운터
            
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    
                    // 상단 근처에서 스크롤하고, 로딩 중이 아니며, 더 많은 메시지가 있을 때만 로드
                    // 추가 조건: 최소 메시지 수가 있어야 함 (무한 루프 방지)
                    // 연속 빈 로드가 3회 이상이면 중단
                    if (firstVisibleItem <= PRELOAD_THRESHOLD && 
                        !isLoading && 
                        hasMoreMessages && 
                        totalItemCount > 0 &&
                        dy < 0 && // 위로 스크롤할 때만
                        consecutiveEmptyLoads < 3) { // 연속 빈 로드 제한
                        
                        Log.d(TAG, "페이지네이션 트리거: firstVisible=" + firstVisibleItem + 
                                  ", totalItems=" + totalItemCount + 
                                  ", hasMore=" + hasMoreMessages +
                                  ", consecutiveEmpty=" + consecutiveEmptyLoads);
                        
                        // 이전 로드에서 메시지가 없었다면 카운터 증가
                        loadPreviousMessages();
                    }
                }
            }
            
            // 로드 결과에 따라 연속 빈 로드 카운터 업데이트
            public void onLoadResult(boolean hasNewMessages) {
                if (hasNewMessages) {
                    consecutiveEmptyLoads = 0; // 새 메시지가 있으면 카운터 리셋
                } else {
                    consecutiveEmptyLoads++; // 새 메시지가 없으면 카운터 증가
                    if (consecutiveEmptyLoads >= 3) {
                        Log.d(TAG, "연속 빈 로드 3회 - 페이지네이션 중단");
                        hasMoreMessages = false;
                    }
                }
            }
        });
    }
    
    // 초기 메시지 로드 (최신 메시지부터)
    public void loadInitialMessages() {
        if (isLoading) return;
        
        Log.d(TAG, "초기 메시지 로드 시작");
        currentPage = 0;
        oldestMessageTimestamp = null;
        hasMoreMessages = true;
        loadedMessageIds.clear();
        
        loadMessagesFromServer(true);
    }
    
    // 이전 메시지 로드 (페이지네이션)
    public void loadPreviousMessages() {
        if (isLoading || !hasMoreMessages) return;
        
        Log.d(TAG, "이전 메시지 로드 시작 - 페이지: " + (currentPage + 1));
        loadMessagesFromServer(false);
    }
    
    // 서버에서 메시지 로드
    private void loadMessagesFromServer(boolean isInitial) {
        if (isLoading) return;
        
        isLoading = true;
        if (callback != null) {
            callback.onLoadingStarted();
        }
        
        executorService.execute(() -> {
            try {
                String url = buildApiUrl(isInitial);
                Log.d(TAG, "API 요청: " + url);
                
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "메시지 로드 실패", e);
                        mainHandler.post(() -> {
                            isLoading = false;
                            if (callback != null) {
                                callback.onLoadingFinished();
                                callback.onError("네트워크 오류: " + e.getMessage());
                            }
                        });
                    }
                    
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                String responseData = response.body().string();
                                Log.d(TAG, "서버 응답 받음 - 길이: " + responseData.length());
                                
                                processServerResponse(responseData, isInitial);
                            } else {
                                Log.e(TAG, "서버 오류: " + response.code());
                                mainHandler.post(() -> {
                                    isLoading = false;
                                    if (callback != null) {
                                        callback.onLoadingFinished();
                                        callback.onError("서버 오류: " + response.code());
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "응답 처리 오류", e);
                            mainHandler.post(() -> {
                                isLoading = false;
                                if (callback != null) {
                                    callback.onLoadingFinished();
                                    callback.onError("데이터 처리 오류: " + e.getMessage());
                                }
                            });
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "요청 생성 오류", e);
                mainHandler.post(() -> {
                    isLoading = false;
                    if (callback != null) {
                        callback.onLoadingFinished();
                        callback.onError("요청 생성 오류: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    // API URL 생성
    private String buildApiUrl(boolean isInitial) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_URL).append("/chat/").append(groupId).append("/history");
        
        // 페이지네이션 파라미터 추가
        urlBuilder.append("?limit=").append(PAGE_SIZE);
        
        if (!isInitial && oldestMessageTimestamp != null) {
            urlBuilder.append("&before=").append(oldestMessageTimestamp);
        }
        
        return urlBuilder.toString();
    }
    
    // 서버 응답 처리
    private void processServerResponse(String responseData, boolean isInitial) {
        executorService.execute(() -> {
            try {
                List<Message> newMessages = parseMessagesFromResponse(responseData);
                
                // 중복 제거 및 정렬
                List<Message> filteredMessages = removeDuplicatesAndSort(newMessages);
                
                Log.d(TAG, "파싱된 메시지: " + newMessages.size() + "개, 필터링 후: " + filteredMessages.size() + "개");
                
                // 더 이상 메시지가 없는지 확인
                boolean hasMore = newMessages.size() >= PAGE_SIZE;
                
                // 가장 오래된 메시지의 타임스탬프 업데이트
                if (!filteredMessages.isEmpty()) {
                    Message oldestMessage = filteredMessages.get(0);
                    for (Message msg : filteredMessages) {
                        if (msg.getTimestamp() < oldestMessage.getTimestamp()) {
                            oldestMessage = msg;
                        }
                    }
                    oldestMessageTimestamp = String.valueOf(oldestMessage.getTimestamp());
                }
                
                mainHandler.post(() -> {
                    try {
                        updateMessageList(filteredMessages, isInitial);
                        
                        currentPage++;
                        hasMoreMessages = hasMore;
                        isLoading = false;
                        
                        if (callback != null) {
                            callback.onLoadingFinished();
                            callback.onMessagesLoaded(filteredMessages, hasMore);
                            
                            if (!hasMore) {
                                callback.onNoMoreMessages();
                            }
                        }
                        
                        Log.d(TAG, "메시지 로드 완료 - 페이지: " + currentPage + ", 더 있음: " + hasMore);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "UI 업데이트 오류", e);
                        isLoading = false;
                        if (callback != null) {
                            callback.onLoadingFinished();
                            callback.onError("UI 업데이트 오류: " + e.getMessage());
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "응답 파싱 오류", e);
                mainHandler.post(() -> {
                    isLoading = false;
                    if (callback != null) {
                        callback.onLoadingFinished();
                        callback.onError("데이터 파싱 오류: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    // 서버 응답에서 메시지 파싱
    private List<Message> parseMessagesFromResponse(String responseData) throws JSONException {
        List<Message> messages = new ArrayList<>();
        
        // 서버 응답이 직접 JSONArray인 경우
        JSONArray messagesArray = new JSONArray(responseData);
        
        for (int i = 0; i < messagesArray.length(); i++) {
            JSONObject messageObject = messagesArray.getJSONObject(i);
            Message message = parseMessageFromJson(messageObject);
            if (message != null) {
                messages.add(message);
            }
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
            
            // 타임스탬프 처리 - 서버에서 받은 정확한 시간 사용 (현재 시간 사용 금지)
            String timestampStr = messageObject.optString("timestamp", "");
            long timestamp;
            
            if (!timestampStr.isEmpty()) {
                // 서버에서 받은 타임스탬프를 정확히 파싱
                timestamp = parseTimestampToMillisFixed(timestampStr);
            } else {
                // 타임스탬프가 없는 경우에만 현재 시간 사용 (최후의 수단)
                Log.w(TAG, "타임스탬프가 없는 메시지 - 현재 시간 사용");
                timestamp = System.currentTimeMillis();
            }
            
            // 파일 정보 처리
            String fileUrl = messageObject.optString("file_url", "");
            String fileType = messageObject.optString("file_type", "");
            
            // 현재 사용자 메시지인지 정확히 판단
            boolean isCurrentUser = isCurrentUserMessage(senderId, senderName);
            
            // 현재 사용자 메시지인 경우 발신자 정보 정규화
            if (isCurrentUser) {
                senderId = currentUserEmail; // 현재 사용자 이메일로 통일
                senderName = "나"; // 현재 사용자는 "나"로 표시
            }
            
            Message message = new Message(senderId, senderName, content, timestamp);
            if (!fileUrl.isEmpty()) {
                message.setFileUrl(fileUrl);
                message.setFileType(fileType);
            }
            
            Log.d(TAG, "메시지 파싱 완료: senderId=" + senderId + 
                      ", senderName=" + senderName + 
                      ", isCurrentUser=" + isCurrentUser +
                      ", timestamp=" + timestamp +
                      ", 시간=" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp)));
            
            return message;
        } catch (Exception e) {
            Log.e(TAG, "메시지 파싱 오류", e);
            return null;
        }
    }
    
    // 현재 사용자 메시지인지 정확히 판단하는 메서드
    private boolean isCurrentUserMessage(String senderId, String senderName) {
        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            System.out.println("MessagePaginationManager: 현재 사용자 이메일이 설정되지 않음");
            return false;
        }
        
        // 디버깅을 위한 상세 로그
        System.out.println("=== MessagePaginationManager 메시지 소유권 판단 ===");
        System.out.println("현재 사용자 이메일: [" + currentUserEmail + "]");
        System.out.println("메시지 발신자 ID: [" + senderId + "]");
        System.out.println("메시지 발신자 이름: [" + senderName + "]");
        
        // 1. 이메일 정확 매칭 (대소문자 무시)
        if (senderId != null && currentUserEmail.equalsIgnoreCase(senderId.trim())) {
            System.out.println("✓ 이메일 정확 매칭으로 현재 사용자 메시지 확인됨");
            return true;
        }
        
        // 2. 이메일 앞부분 매칭 (@ 앞부분)
        if (senderId != null && currentUserEmail.contains("@")) {
            String emailPrefix = currentUserEmail.split("@")[0];
            System.out.println("이메일 앞부분 비교: [" + emailPrefix + "] vs [" + senderId.trim() + "]");
            if (emailPrefix.equalsIgnoreCase(senderId.trim())) {
                System.out.println("✓ 이메일 앞부분 매칭으로 현재 사용자 메시지 확인됨");
                return true;
            }
        }
        
        // 3. 발신자명으로 판단
        if (senderName != null) {
            String normalizedName = senderName.trim().toLowerCase();
            System.out.println("발신자명 정규화: [" + normalizedName + "]");
            
            // "나" 또는 "me" 키워드 확인
            if (normalizedName.equals("나") || normalizedName.equals("me") || normalizedName.equals("myself")) {
                System.out.println("✓ 발신자명 키워드로 현재 사용자 메시지 확인됨");
                return true;
            }
            
            // 현재 사용자 이메일의 앞부분과 비교
            if (currentUserEmail.contains("@")) {
                String emailPrefix = currentUserEmail.split("@")[0];
                System.out.println("발신자명과 이메일 앞부분 비교: [" + emailPrefix + "] vs [" + senderName.trim() + "]");
                if (emailPrefix.equalsIgnoreCase(senderName.trim())) {
                    System.out.println("✓ 발신자명과 이메일 앞부분 매칭으로 현재 사용자 메시지 확인됨");
                    return true;
                }
            }
        }
        
        System.out.println("✗ 현재 사용자 메시지가 아님으로 판단됨");
        System.out.println("============================================");
        return false;
    }
    
    // 타임스탬프 파싱 개선 - 정확한 시간 처리
    private long parseTimestampToMillisFixed(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) {
            Log.w(TAG, "타임스탬프가 비어있음 - 현재 시간 사용");
            return System.currentTimeMillis();
        }
        
        try {
            // 1. 이미 밀리초 형태인 경우 (숫자만 있는 경우)
            if (timestampStr.matches("\\d+")) {
                long timestamp = Long.parseLong(timestampStr);
                // 초 단위인지 밀리초 단위인지 판단 (2000년 이후 기준)
                if (timestamp < 946684800000L) { // 2000-01-01 00:00:00 UTC in milliseconds
                    timestamp *= 1000; // 초를 밀리초로 변환
                }
                return timestamp;
            }
            
            // 2. ISO 8601 형식 처리
            if (timestampStr.contains("T")) {
                try {
                    // Java 8+ Time API 사용
                    java.time.Instant instant;
                    
                    if (timestampStr.endsWith("Z")) {
                        // UTC 시간 (예: "2025-01-15T10:30:00Z")
                        instant = java.time.Instant.parse(timestampStr);
                    } else if (timestampStr.contains("+") || timestampStr.lastIndexOf("-") > 10) {
                        // 타임존 정보 포함 (예: "2025-01-15T10:30:00+09:00")
                        instant = java.time.OffsetDateTime.parse(timestampStr).toInstant();
                    } else {
                        // 로컬 시간으로 가정 (예: "2025-01-15T10:30:00")
                        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(timestampStr);
                        instant = localDateTime.atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant();
                    }
                    
                    return instant.toEpochMilli();
                } catch (Exception e) {
                    Log.w(TAG, "Java 8 Time API 파싱 실패, SimpleDateFormat 시도: " + timestampStr);
                }
                
                // SimpleDateFormat으로 fallback
                try {
                    java.text.SimpleDateFormat sdf;
                    if (timestampStr.endsWith("Z")) {
                        sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    } else {
                        sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"));
                    }
                    
                    java.util.Date date = sdf.parse(timestampStr);
                    return date.getTime();
                } catch (Exception e) {
                    Log.w(TAG, "SimpleDateFormat 파싱도 실패: " + timestampStr);
                }
            }
            
            // 3. 일반적인 날짜 형식 처리 (예: "2025-01-15 10:30:00")
            if (timestampStr.contains(" ")) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"));
                    java.util.Date date = sdf.parse(timestampStr);
                    return date.getTime();
                } catch (Exception e) {
                    Log.w(TAG, "일반 날짜 형식 파싱 실패: " + timestampStr);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "타임스탬프 파싱 전체 실패: " + timestampStr, e);
        }
        
        // 모든 파싱 실패 시 현재 시간 반환
        Log.w(TAG, "타임스탬프 파싱 실패, 현재 시간 사용: " + timestampStr);
        return System.currentTimeMillis();
    }
    
    // 중복 제거 및 정렬 (성능 최적화)
    private List<Message> removeDuplicatesAndSort(List<Message> newMessages) {
        List<Message> filteredMessages = new ArrayList<>();
        
        for (Message message : newMessages) {
            String messageId = generateMessageId(message);
            
            // 1. 로드된 메시지 ID 체크
            if (loadedMessageIds.contains(messageId)) {
                Log.d(TAG, "이미 로드된 메시지 스킵: " + messageId);
                continue;
            }
            
            // 2. 현재 메시지 리스트와 정확한 중복 체크 강화
            boolean isDuplicate = false;
            for (Message existingMessage : messageList) {
                if (isSameMessageExact(message, existingMessage)) {
                    isDuplicate = true;
                    Log.d(TAG, "기존 메시지와 중복: " + messageId);
                    break;
                }
                
                // 3. 유사 메시지 체크 추가 (내용과 시간이 비슷한 경우)
                if (isSimilarMessage(message, existingMessage)) {
                    isDuplicate = true;
                    Log.d(TAG, "유사 메시지로 중복 감지: " + messageId);
                    break;
                }
            }
            
            if (!isDuplicate) {
                filteredMessages.add(message);
                loadedMessageIds.add(messageId); // 로드된 메시지 ID 추가
            }
        }
        
        // 시간순 정렬 (오래된 것부터)
        Collections.sort(filteredMessages, (m1, m2) -> 
            Long.compare(m1.getTimestamp(), m2.getTimestamp()));
        
        Log.d(TAG, "중복 제거 완료: 원본 " + newMessages.size() + "개 -> 필터링 후 " + filteredMessages.size() + "개");
        return filteredMessages;
    }
    
    // 유사 메시지 감지 (내용과 시간이 비슷한 경우)
    private boolean isSimilarMessage(Message msg1, Message msg2) {
        if (msg1 == null || msg2 == null) {
            return false;
        }
        
        // 메시지 내용이 동일한지 확인
        String content1 = msg1.getMessage();
        String content2 = msg2.getMessage();
        if (content1 != null) content1 = content1.trim();
        if (content2 != null) content2 = content2.trim();
        
        if (!java.util.Objects.equals(content1, content2)) {
            return false;
        }
        
        // 시간 차이가 5초 이내인지 확인
        long timeDiff = Math.abs(msg1.getTimestamp() - msg2.getTimestamp());
        if (timeDiff > 5000) { // 5초 초과하면 다른 메시지로 판단
            return false;
        }
        
        // 발신자가 모두 현재 사용자인 경우
        boolean isMsg1Mine = isCurrentUserMessage(msg1.getSenderId(), msg1.getSenderName());
        boolean isMsg2Mine = isCurrentUserMessage(msg2.getSenderId(), msg2.getSenderName());
        
        if (isMsg1Mine && isMsg2Mine) {
            System.out.println("MessagePaginationManager: 내가 보낸 메시지 중복 감지: 시간차=" + timeDiff + "ms");
            return true;
        }
        
        return false;
    }
    
    // 향상된 메시지 고유 ID 생성 (중복 방지 강화)
    private String generateMessageId(Message message) {
        StringBuilder idBuilder = new StringBuilder();
        
        // 타임스탬프 (밀리초 단위)
        idBuilder.append(message.getTimestamp());
        idBuilder.append("_");
        
        // 발신자 ID (정규화)
        String senderId = message.getSenderId();
        if (senderId != null) {
            senderId = senderId.trim().toLowerCase();
        }
        idBuilder.append(senderId != null ? senderId : "unknown");
        idBuilder.append("_");
        
        // 메시지 내용 해시 (정규화)
        String content = message.getMessage();
        if (content != null) {
            content = content.trim();
        }
        idBuilder.append(content != null ? content.hashCode() : 0);
        
        // 파일 정보 (있는 경우)
        if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
            idBuilder.append("_file_");
            idBuilder.append(message.getFileUrl().trim().hashCode());
            if (message.getFileType() != null) {
                idBuilder.append("_");
                idBuilder.append(message.getFileType().trim());
            }
        }
        
        return idBuilder.toString();
    }
    
    // 두 메시지가 정확히 동일한지 확인 (강화된 버전)
    private boolean isSameMessageExact(Message msg1, Message msg2) {
        if (msg1 == null || msg2 == null) {
            return false;
        }
        
        // 1. 타임스탬프 비교 (정확히 같아야 함)
        if (msg1.getTimestamp() != msg2.getTimestamp()) {
            return false;
        }
        
        // 2. 발신자 ID 비교 (정규화 후 비교)
        String senderId1 = msg1.getSenderId();
        String senderId2 = msg2.getSenderId();
        if (senderId1 != null) senderId1 = senderId1.trim().toLowerCase();
        if (senderId2 != null) senderId2 = senderId2.trim().toLowerCase();
        
        if (!java.util.Objects.equals(senderId1, senderId2)) {
            return false;
        }
        
        // 3. 메시지 내용 비교 (정규화 후 비교)
        String content1 = msg1.getMessage();
        String content2 = msg2.getMessage();
        if (content1 != null) content1 = content1.trim();
        if (content2 != null) content2 = content2.trim();
        
        if (!java.util.Objects.equals(content1, content2)) {
            return false;
        }
        
        // 4. 파일 URL 비교 (있는 경우)
        String fileUrl1 = msg1.getFileUrl();
        String fileUrl2 = msg2.getFileUrl();
        if (fileUrl1 != null) fileUrl1 = fileUrl1.trim();
        if (fileUrl2 != null) fileUrl2 = fileUrl2.trim();
        
        return java.util.Objects.equals(fileUrl1, fileUrl2);
    }
    
    // 메시지 리스트 업데이트
    private void updateMessageList(List<Message> newMessages, boolean isInitial) {
        if (isInitial) {
            // 초기 로드 시 전체 교체
            messageList.clear();
            messageList.addAll(newMessages);
            messageAdapter.replaceAllMessages(newMessages);
            
            // 최신 메시지로 스크롤
            if (!newMessages.isEmpty()) {
                recyclerView.scrollToPosition(messageList.size() - 1);
            }
        } else {
            // 페이지네이션 시 상단에 추가 - 중복 방지 강화
            List<Message> uniqueNewMessages = new ArrayList<>();
            
            for (Message newMessage : newMessages) {
                boolean isDuplicate = false;
                
                // 기존 메시지와 중복 체크 (타임스탬프 + 발신자 + 내용)
                for (Message existingMessage : messageList) {
                    if (isSameMessage(newMessage, existingMessage)) {
                        isDuplicate = true;
                        Log.d(TAG, "중복 메시지 감지됨: " + newMessage.getMessage().substring(0, Math.min(20, newMessage.getMessage().length())));
                        break;
                    }
                }
                
                if (!isDuplicate) {
                    uniqueNewMessages.add(newMessage);
                }
            }
            
            if (!uniqueNewMessages.isEmpty()) {
                // 메시지 리스트에 추가 (시간순 정렬 유지)
                for (int i = uniqueNewMessages.size() - 1; i >= 0; i--) {
                    messageList.add(0, uniqueNewMessages.get(i));
                }
                
                // 어댑터에 일괄 추가
                messageAdapter.addMessagesAtTop(uniqueNewMessages);
                
                // 스크롤 위치 유지 (새로 추가된 메시지 수만큼 오프셋)
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                    View firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition);
                    int offset = 0;
                    if (firstVisibleView != null) {
                        offset = firstVisibleView.getTop();
                    }
                    
                    // 새로 추가된 메시지 수만큼 위치 조정
                    layoutManager.scrollToPositionWithOffset(
                        firstVisiblePosition + uniqueNewMessages.size(), offset);
                }
                
                Log.d(TAG, "페이지네이션: " + uniqueNewMessages.size() + "개 고유 메시지 추가됨");
            } else {
                Log.d(TAG, "페이지네이션: 모든 메시지가 중복됨, 추가하지 않음");
                // 중복 메시지만 있으면 더 이상 로드할 메시지가 없다고 판단
                hasMoreMessages = false;
            }
        }
    }
    
    // 두 메시지가 동일한지 정확히 비교
    private boolean isSameMessage(Message msg1, Message msg2) {
        if (msg1 == null || msg2 == null) {
            return false;
        }
        
        // 타임스탬프, 발신자, 내용이 모두 같으면 동일한 메시지
        boolean sameTimestamp = msg1.getTimestamp() == msg2.getTimestamp();
        boolean sameSender = (msg1.getSenderId() != null && msg1.getSenderId().equals(msg2.getSenderId()));
        boolean sameContent = (msg1.getMessage() != null && msg1.getMessage().equals(msg2.getMessage()));
        
        return sameTimestamp && sameSender && sameContent;
    }
    
    // 리소스 정리
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (loadedMessageIds != null) {
            loadedMessageIds.clear();
        }
    }
    
    // 상태 확인 메서드들
    public boolean isLoading() {
        return isLoading;
    }
    
    public boolean hasMoreMessages() {
        return hasMoreMessages;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public int getLoadedMessageCount() {
        return loadedMessageIds.size();
    }
} 
