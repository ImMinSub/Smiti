package com.example.smiti;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
    
    // 스크롤 리스너 설정 (상단 스크롤 시 이전 메시지 로드)
    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                    
                    // 상단 근처에서 스크롤하고, 로딩 중이 아니며, 더 많은 메시지가 있을 때
                    if (firstVisibleItem <= PRELOAD_THRESHOLD && !isLoading && hasMoreMessages) {
                        loadPreviousMessages();
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
            String senderId = messageObject.optString("sender_id", "");
            if (senderId.isEmpty()) {
                senderId = messageObject.optString("sender_name", "unknown");
            }
            
            String senderName = messageObject.optString("sender_name", null);
            if (senderName == null || senderName.equals("null") || senderName.isEmpty()) {
                senderName = "알 수 없음";
            }
            
            String content = messageObject.optString("content", "");
            if (content.isEmpty()) {
                content = messageObject.optString("message", "");
            }
            
            String timestampStr = messageObject.optString("timestamp", "");
            long timestamp = parseTimestampToMillis(timestampStr);
            
            String fileUrl = messageObject.optString("file_url", "");
            String fileType = messageObject.optString("file_type", "");
            
            // 현재 사용자인지 확인
            boolean isCurrentUser = currentUserEmail != null && currentUserEmail.equals(senderId);
            if (isCurrentUser) {
                senderName = "나";
            }
            
            Message message = new Message(senderId, senderName, content, timestamp);
            if (!fileUrl.isEmpty()) {
                message.setFileUrl(fileUrl);
                message.setFileType(fileType);
            }
            
            return message;
            
        } catch (Exception e) {
            Log.e(TAG, "메시지 파싱 오류", e);
            return null;
        }
    }
    
    // 타임스탬프 파싱
    private long parseTimestampToMillis(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return System.currentTimeMillis();
        }
        
        try {
            // ISO 8601 형식 파싱
            if (timestampStr.contains("T")) {
                java.time.Instant instant = java.time.Instant.parse(timestampStr);
                return instant.toEpochMilli();
            } else {
                // 숫자 형식인 경우
                return Long.parseLong(timestampStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "타임스탬프 파싱 실패: " + timestampStr, e);
            return System.currentTimeMillis();
        }
    }
    
    // 중복 제거 및 정렬 (성능 최적화)
    private List<Message> removeDuplicatesAndSort(List<Message> newMessages) {
        List<Message> filteredMessages = new ArrayList<>();
        
        for (Message message : newMessages) {
            String messageId = generateMessageId(message);
            if (!loadedMessageIds.contains(messageId)) {
                loadedMessageIds.add(messageId);
                filteredMessages.add(message);
            }
        }
        
        // 타임스탬프 순으로 정렬
        Collections.sort(filteredMessages, (m1, m2) -> 
            Long.compare(m1.getTimestamp(), m2.getTimestamp()));
        
        return filteredMessages;
    }
    
    // 메시지 고유 ID 생성
    private String generateMessageId(Message message) {
        return message.getTimestamp() + "_" + message.getSenderId() + "_" + 
               message.getMessage().hashCode();
    }
    
    // 메시지 리스트 업데이트
    private void updateMessageList(List<Message> newMessages, boolean isInitial) {
        if (isInitial) {
            // 초기 로드 시 전체 교체
            messageList.clear();
            messageList.addAll(newMessages);
            messageAdapter.notifyDataSetChanged();
            
            // 최신 메시지로 스크롤
            if (!newMessages.isEmpty()) {
                recyclerView.scrollToPosition(messageList.size() - 1);
            }
        } else {
            // 페이지네이션 시 상단에 추가
            int insertPosition = 0;
            for (Message message : newMessages) {
                messageList.add(insertPosition, message);
                insertPosition++;
            }
            
            // 효율적인 UI 업데이트
            messageAdapter.notifyItemRangeInserted(0, newMessages.size());
            
            // 스크롤 위치 유지
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (layoutManager != null) {
                layoutManager.scrollToPositionWithOffset(newMessages.size(), 0);
            }
        }
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
