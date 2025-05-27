package com.example.smiti;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

import com.example.smiti.repository.MessageRepository;

public class WebSocketReconnectionManager implements WebSocketService.WebSocketListener {
    
    private static final String TAG = "WebSocketReconnectionManager";
    private static final String PREF_NAME = "WebSocketPrefs";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    
    private Context context;
    private Activity activity;
    private WebSocketService webSocketService;
    private MessageSyncManager messageSyncManager;
    private ConnectionStateUIManager connectionStateUIManager;
    private MessageRepository messageRepository;
    
    private String currentGroupId;
    private String currentUserEmail;
    private boolean isInitialized = false;
    
    // 콜백 인터페이스
    public interface ReconnectionManagerListener {
        void onMessageReceived(String message);
        void onConnectionEstablished();
        void onConnectionLost();
        void onSyncCompleted(int messageCount);
    }
    
    private ReconnectionManagerListener listener;
    
    public WebSocketReconnectionManager() {
        // 컴포넌트 초기화
        this.webSocketService = new WebSocketService();
        this.messageSyncManager = new MessageSyncManager();
        
        // 리스너 설정
        this.messageSyncManager.setListener(new MessageSyncManager.MessageSyncListener() {
            @Override
            public void onSyncStarted(String groupId) {
                // 동기화 시작 처리
            }
            
            @Override
            public void onSyncProgress(String groupId, int processedCount, int totalCount) {
                // 동기화 진행 처리
            }
            
            @Override
            public void onSyncCompleted(String groupId, int syncedMessageCount) {
                if (listener != null) {
                    listener.onSyncCompleted(syncedMessageCount);
                }
            }
            
            @Override
            public void onSyncError(String groupId, String error) {
                // 동기화 오류 처리
            }
        });
    }
    
    public void setListener(ReconnectionManagerListener listener) {
        this.listener = listener;
    }
    
    // 웹소켓 연결 시작
    public void connect(String groupId, String userEmail) {
        this.currentGroupId = groupId;
        this.currentUserEmail = userEmail;
        
        // 웹소켓 연결
        webSocketService.connect(groupId, userEmail, this);
        isInitialized = true;
    }
    
    // 메시지 전송
    public void sendMessage(String message) {
        if (webSocketService.isConnected()) {
            webSocketService.sendMessage(message);
        }
    }
    
    // 강제 메시지 동기화
    public void forceSyncMessages() {
        if (currentGroupId != null) {
            messageSyncManager.syncMissedMessages(currentGroupId, System.currentTimeMillis());
        }
    }
    
    // 서버 큐 상태 확인
    public void checkServerQueue() {
        if (currentGroupId != null) {
            messageSyncManager.checkServerQueue(currentGroupId, new MessageSyncManager.QueueCheckCallback() {
                @Override
                public void onQueueCheckSuccess(boolean isEmpty, int queueSize) {
                    if (!isEmpty && queueSize > 0) {
                        // 큐에 메시지가 있으면 동기화 수행
                        forceSyncMessages();
                    }
                }
                
                @Override
                public void onQueueCheckError(Exception error) {
                    // 큐 확인 오류 처리
                }
            });
        }
    }
    
    // 연결 해제
    public void disconnect() {
        if (webSocketService != null) {
            webSocketService.disconnect();
        }
        isInitialized = false;
    }
    
    // 연결 상태 확인
    public boolean isConnected() {
        return webSocketService != null && webSocketService.isConnected();
    }
    
    // 연결 상태 반환
    public WebSocketService.ConnectionState getConnectionState() {
        return webSocketService != null ? webSocketService.getConnectionState() : 
               WebSocketService.ConnectionState.DISCONNECTED;
    }
    
    // === WebSocketService.WebSocketListener 구현 ===
    
    @Override
    public void onConnect() {
        // 연결 성공 시 메시지 동기화 수행
        if (currentGroupId != null) {
            messageSyncManager.syncMissedMessages(currentGroupId, System.currentTimeMillis());
        }
        
        if (listener != null) {
            listener.onConnectionEstablished();
        }
    }
    
    @Override
    public void onMessage(String message) {
        if (listener != null) {
            listener.onMessageReceived(message);
        }
    }
    
    @Override
    public void onDisconnect(int code, String reason) {
        if (listener != null) {
            listener.onConnectionLost();
        }
    }
    
    @Override
    public void onError(Exception error) {
        // 오류 처리
    }
    
    @Override
    public void onConnectionStateChanged(WebSocketService.ConnectionState state) {
        // 연결 상태 변경 처리
    }
    
    @Override
    public void onReconnectAttempt(int attempt, int maxAttempts) {
        // 재연결 시도 처리
    }
    
    // 네트워크 상태 변경 처리
    public void onNetworkStateChanged(boolean isConnected) {
        if (isConnected && isInitialized) {
            // 네트워크가 복구되면 재연결 시도
            if (!webSocketService.isConnected() && currentGroupId != null && currentUserEmail != null) {
                webSocketService.connect(currentGroupId, currentUserEmail, this);
            }
        }
    }
    
    // 리소스 정리
    public void cleanup() {
        if (webSocketService != null) {
            webSocketService.disconnect();
        }
        
        if (messageSyncManager != null) {
            messageSyncManager.cleanup();
        }
        
        listener = null;
        isInitialized = false;
    }
} 
