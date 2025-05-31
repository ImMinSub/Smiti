package com.example.smiti.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.smiti.Message;
import com.example.smiti.repository.MessageRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatNetworkManager {
      private static final String TAG = "ChatNetworkManager";
    private static final String BASE_URL = "http://202.31.246.51:80";
    private static final long EXTENDED_TIMEOUT = 90000; // 90초
    private static final long BASE_SYNC_INTERVAL = 60000; // 기본 60초 (30초에서 증가)
    private static final long MAX_SYNC_INTERVAL = 300000; // 최대 5분
    private static final long MIN_SYNC_INTERVAL = 30000; // 최소 30초
    
    private final Context context;
    private final MessageRepository messageRepository;
    private final String currentGroupId;
    
    private BroadcastReceiver networkReceiver;
    private boolean wasOffline = false;
    
    // 지능형 동기화 시스템
    private Handler syncHandler;
    private Runnable syncRunnable;
    private long currentSyncInterval = BASE_SYNC_INTERVAL;
    private long lastSyncTime = 0;
    private long lastActivityTime = 0;
    private boolean isSyncInProgress = false;
    private int consecutiveEmptySync = 0;
    
    public interface NetworkCallback {
        void onNetworkConnected();
        void onNetworkDisconnected();
        void onSyncCompleted(List<Message> newMessages);
        void onSyncFailed(String error);
        void onSummaryReceived(String summary);
        void onSummaryFailed(String error);
        void onMembersLoaded(List<Object> members); // User 객체 대신 Object 사용
        void onMembersLoadFailed(String error);
    }
    
    private NetworkCallback networkCallback;
    
    public ChatNetworkManager(Context context, MessageRepository messageRepository, 
                             String currentGroupId) {
        this.context = context;
        this.messageRepository = messageRepository;
        this.currentGroupId = currentGroupId;
    }
    
    public void setNetworkCallback(NetworkCallback callback) {
        this.networkCallback = callback;
    }
    
    /**
     * 네트워크 상태 감지 설정
     */
    public void setupNetworkReceiver() {
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                    boolean isConnected = isNetworkConnected();
                    
                    if (isConnected && wasOffline) {
                        // 오프라인에서 온라인으로 전환
                        Log.d(TAG, "네트워크 연결 복구됨 - 메시지 동기화 시작");
                        wasOffline = false;
                        
                        if (networkCallback != null) {
                            networkCallback.onNetworkConnected();
                        }
                        
                        // 네트워크 안정화를 위해 잠시 대기 후 동기화
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            performFullMessageSync();
                        }, 3000); // 3초 대기
                        
                    } else if (!isConnected) {
                        // 온라인에서 오프라인으로 전환
                        Log.d(TAG, "네트워크 연결 끊어짐");
                        wasOffline = true;
                        
                        if (networkCallback != null) {
                            networkCallback.onNetworkDisconnected();
                        }
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(networkReceiver, filter);
        
        // 초기 네트워크 상태 확인
        wasOffline = !isNetworkConnected();
    }
    
    /**
     * 네트워크 연결 상태 확인
     */
    public boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
      /**
     * 지능형 메시지 동기화 설정
     * - 적응형 동기화 간격
     * - 중복 동기화 방지
     * - 활동 기반 동기화
     */
    public void setupPeriodicSync() {
        Log.d(TAG, "지능형 동기화 시스템 시작");
        syncHandler = new Handler(Looper.getMainLooper());
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                // 중복 동기화 방지
                if (isSyncInProgress) {
                    Log.d(TAG, "동기화 진행 중이므로 건너뜀");
                    scheduleNextSync();
                    return;
                }
                
                // 네트워크 연결 확인
                if (!isNetworkConnected()) {
                    Log.d(TAG, "네트워크 미연결로 동기화 건너뜀");
                    scheduleNextSync();
                    return;
                }
                
                // 최근 동기화 시간 확인 (너무 빈번한 동기화 방지)
                long timeSinceLastSync = System.currentTimeMillis() - lastSyncTime;
                if (timeSinceLastSync < MIN_SYNC_INTERVAL) {
                    Log.d(TAG, "최소 동기화 간격(" + MIN_SYNC_INTERVAL/1000 + "초) 미충족으로 건너뜀");
                    scheduleNextSync();
                    return;
                }
                
                Log.d(TAG, "지능형 동기화 실행 - 간격: " + (currentSyncInterval/1000) + "초");
                smartSyncMessagesFromServer();
            }
        };
        
        // 첫 번째 동기화는 10초 후 시작 (즉시 시작 방지)
        syncHandler.postDelayed(syncRunnable, 10000);
        lastActivityTime = System.currentTimeMillis();
    }
      /**
     * 정기적인 메시지 동기화 중지
     */
    public void stopPeriodicSync() {
        Log.d(TAG, "지능형 동기화 시스템 중지");
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }
        isSyncInProgress = false;
    }
    
    /**
     * 다음 동기화 일정 예약 (적응형 간격)
     */
    private void scheduleNextSync() {
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.postDelayed(syncRunnable, currentSyncInterval);
        }
    }
    
    /**
     * 동기화 간격 조정 (적응형)
     */
    private void adjustSyncInterval(boolean hasNewMessages) {
        if (hasNewMessages) {
            // 새 메시지가 있으면 간격을 줄임
            currentSyncInterval = Math.max(MIN_SYNC_INTERVAL, currentSyncInterval - 15000);
            consecutiveEmptySync = 0;
            Log.d(TAG, "새 메시지 발견 - 동기화 간격 단축: " + (currentSyncInterval/1000) + "초");
        } else {
            // 새 메시지가 없으면 간격을 늘림
            consecutiveEmptySync++;
            if (consecutiveEmptySync >= 3) {
                currentSyncInterval = Math.min(MAX_SYNC_INTERVAL, currentSyncInterval + 30000);
                Log.d(TAG, "연속 빈 동기화(" + consecutiveEmptySync + "회) - 간격 연장: " + (currentSyncInterval/1000) + "초");
            }
        }
    }
    
    /**
     * 활동 기반 동기화 트리거
     */
    public void notifyUserActivity() {
        lastActivityTime = System.currentTimeMillis();
        
        // 사용자가 활동 중이고 마지막 동기화가 1분 이상 전이면 즉시 동기화
        long timeSinceLastSync = System.currentTimeMillis() - lastSyncTime;
        if (timeSinceLastSync > 60000 && !isSyncInProgress) {
            Log.d(TAG, "사용자 활동 감지 - 즉시 동기화 실행");
            smartSyncMessagesFromServer();
        }
    }    /**
     * 지능형 서버 메시지 동기화
     * - 중복 방지
     * - 성능 모니터링
     * - 적응형 간격 조정
     */
    public void smartSyncMessagesFromServer() {
        // 중복 실행 방지
        if (isSyncInProgress) {
            Log.d(TAG, "동기화가 이미 진행 중입니다");
            return;
        }
        
        isSyncInProgress = true;
        lastSyncTime = System.currentTimeMillis();
        
        if (!isNetworkConnected()) {
            Log.d(TAG, "네트워크 연결 없음 - 동기화 취소");
            isSyncInProgress = false;
            scheduleNextSync();
            if (networkCallback != null) {
                networkCallback.onSyncFailed("네트워크 연결이 없습니다");
            }
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(EXTENDED_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(EXTENDED_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(EXTENDED_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();

        String url = BASE_URL + "/chat/" + currentGroupId + "/history";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "지능형 동기화 실패", e);
                isSyncInProgress = false;
                scheduleNextSync();
                if (networkCallback != null) {
                    networkCallback.onSyncFailed("서버 연결 실패: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        Log.d(TAG, "지능형 동기화 응답 받음");

                        JSONArray messagesArray = new JSONArray(responseData);
                        List<Message> newMessages = new ArrayList<>();

                        for (int i = 0; i < messagesArray.length(); i++) {
                            JSONObject messageObject = messagesArray.getJSONObject(i);
                            Message message = parseMessageFromJson(messageObject);

                            if (message != null) {
                                newMessages.add(message);
                            }
                        }
                        
                        boolean hasNewMessages = !newMessages.isEmpty();
                        Log.d(TAG, "지능형 동기화 완료 - 새 메시지: " + newMessages.size() + "개");
                        
                        // 적응형 간격 조정
                        adjustSyncInterval(hasNewMessages);

                        if (networkCallback != null) {
                            networkCallback.onSyncCompleted(newMessages);
                        }

                    } else {
                        Log.e(TAG, "지능형 동기화 서버 오류: " + response.code());
                        adjustSyncInterval(false);
                        if (networkCallback != null) {
                            networkCallback.onSyncFailed("서버 오류: " + response.code());
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "지능형 동기화 응답 파싱 오류", e);
                    adjustSyncInterval(false);
                    if (networkCallback != null) {
                        networkCallback.onSyncFailed("응답 파싱 실패");
                    }                } finally {
                    isSyncInProgress = false;
                    scheduleNextSync();
                }
            }
        });
    }
    
    /**
     * 기존 동기화 메서드 (호환성 유지)
     * 지능형 동기화로 리다이렉트
     */
    public void syncMessagesFromServer() {
        Log.d(TAG, "기존 동기화 메서드 호출 -> 지능형 동기화로 전환");
        smartSyncMessagesFromServer();
    }
    
    /**
     * 전체 메시지 동기화 (오프라인 복구용)
     */
    public void performFullMessageSync() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build();
        
        String url = BASE_URL + "/chat/" + currentGroupId + "/history";
        
        Log.d(TAG, "전체 메시지 동기화 요청: " + url);
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "전체 메시지 동기화 실패", e);
                if (networkCallback != null) {
                    networkCallback.onSyncFailed("전체 동기화 실패");
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        Log.d(TAG, "전체 메시지 동기화 응답 받음");
                        
                        JSONArray messagesArray = null;
                        
                        if (responseData.trim().startsWith("[")) {
                            messagesArray = new JSONArray(responseData);
                        } else {
                            JSONObject jsonObject = new JSONObject(responseData);
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
                            List<Message> serverMessages = new ArrayList<>();
                            
                            for (int i = 0; i < messagesArray.length(); i++) {
                                JSONObject messageObject = messagesArray.getJSONObject(i);
                                Message message = parseMessageFromJson(messageObject);
                                if (message != null) {
                                    serverMessages.add(message);
                                }
                            }
                            
                            if (networkCallback != null) {
                                networkCallback.onSyncCompleted(serverMessages);
                            }
                        }
                        
                    } catch (JSONException e) {
                        Log.e(TAG, "전체 메시지 동기화 응답 파싱 오류", e);
                        if (networkCallback != null) {
                            networkCallback.onSyncFailed("응답 파싱 실패");
                        }
                    }
                } else {
                    Log.e(TAG, "전체 메시지 동기화 서버 오류: " + response.code());
                    if (networkCallback != null) {
                        networkCallback.onSyncFailed("서버 오류: " + response.code());
                    }
                }
            }
        });
    }
    
    /**
     * 채팅 요약 요청
     */
    public void requestChatSummary() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("group_id", currentGroupId);
        } catch (JSONException e) {
            Log.e(TAG, "요약 요청 JSON 생성 오류", e);
            if (networkCallback != null) {
                networkCallback.onSummaryFailed("요청 생성 실패");
            }
            return;
        }

        RequestBody requestBody = RequestBody.create(
                jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(BASE_URL + "/chat/summary")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "채팅 요약 요청 실패", e);
                if (networkCallback != null) {
                    networkCallback.onSummaryFailed("네트워크 오류");
                }
            }

            @Override 
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        final String summary = jsonObject.optString("summary", "요약을 생성할 수 없습니다.");
                        if (networkCallback != null) {
                            networkCallback.onSummaryReceived(summary);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "요약 응답 파싱 오류", e);
                        if (networkCallback != null) {
                            networkCallback.onSummaryFailed("응답 처리 오류");
                        }
                    }
                } else {
                    Log.e(TAG, "채팅 요약 서버 오류: " + response.code());
                    if (networkCallback != null) {
                        networkCallback.onSummaryFailed("서버 오류: " + response.code());
                    }
                }
            }
        });
    }
    
    /**
     * 그룹 멤버 로드
     */
    public void loadGroupMembers() {
        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "/groups/" + currentGroupId + "/users";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "그룹 멤버 조회 실패", e);
                if (networkCallback != null) {
                    networkCallback.onMembersLoadFailed("멤버 목록 로드 실패");
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        Log.d(TAG, "그룹 멤버 응답: " + responseData);
                        
                        JSONObject jsonObject = new JSONObject(responseData);
                        if (jsonObject.has("users")) {
                            JSONArray usersArray = jsonObject.getJSONArray("users");
                            List<Object> members = new ArrayList<>();
                            
                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject userObject = usersArray.getJSONObject(i);
                                // User 객체 대신 JSONObject를 그대로 전달
                                members.add(userObject);
                            }
                            
                            if (networkCallback != null) {
                                networkCallback.onMembersLoaded(members);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "그룹 멤버 응답 파싱 오류", e);
                        if (networkCallback != null) {
                            networkCallback.onMembersLoadFailed("응답 파싱 실패");
                        }
                    }
                } else {
                    Log.e(TAG, "그룹 멤버 조회 서버 오류: " + response.code());
                    if (networkCallback != null) {
                        networkCallback.onMembersLoadFailed("서버 오류: " + response.code());
                    }
                }
            }
        });
    }
    
    /**
     * JSON에서 Message 객체로 파싱 (간소화된 버전)
     */
    private Message parseMessageFromJson(JSONObject messageObject) {
        try {
            String senderId = messageObject.optString("sender_id", "");
            String senderName = messageObject.optString("sender_name", "알 수 없음");
            String content = messageObject.optString("content", "");
            if (content.isEmpty()) {
                content = messageObject.optString("message", "");
            }
            
            String timestampStr = messageObject.optString("timestamp", "");
            long timestamp = parseTimestamp(timestampStr);
            
            String fileUrl = messageObject.optString("file_url", "");
            String fileType = messageObject.optString("file_type", "");
            
            Message message = new Message(senderId, senderName, content, timestamp);
            
            if (!fileUrl.isEmpty()) {
                message.setFileUrl(fileUrl);
                message.setFileType(fileType.isEmpty() ? "file" : fileType);
                message.setMessageType("file");
            } else {
                message.setMessageType("text");
            }
            
            return message;
        } catch (Exception e) {
            Log.e(TAG, "메시지 파싱 오류", e);
            return null;
        }
    }
    
    /**
     * 타임스탬프 파싱
     */
    private long parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return System.currentTimeMillis();
        }
        
        try {
            if (timestampStr.matches("\\d+")) {
                long timestamp = Long.parseLong(timestampStr);
                if (timestamp < 946684800000L) { // 2000년 이전이면 초 단위로 가정
                    timestamp *= 1000;
                }
                return timestamp;
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "타임스탬프 파싱 실패: " + timestampStr);
        }
        
        return System.currentTimeMillis();
    }
    
    /**
     * 네트워크 리시버 해제
     */
    public void cleanup() {
        if (networkReceiver != null) {
            try {
                context.unregisterReceiver(networkReceiver);
            } catch (IllegalArgumentException e) {
                // 이미 해제된 경우 무시
            }
        }
        stopPeriodicSync();
    }
} 