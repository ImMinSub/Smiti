package com.example.smiti;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.HashSet;

import org.json.JSONObject;

public class WebSocketService {
    private static final String TAG = "WebSocketService";
    private static final String WS_BASE_URL = "ws://202.31.246.51:80/ws";
    private static final int CONNECTION_TIMEOUT = 30; // 30초로 단축 (빠른 실패)
    private static final int INITIAL_RECONNECT_DELAY = 2000; // 초기 재연결 지연 2초
    private static final int MAX_RECONNECT_DELAY = 15000; // 최대 재연결 지연 15초로 단축
    private static final int MAX_RECONNECT_ATTEMPTS = 5; // 재연결 시도 횟수 감소
    
    private WebSocketClient webSocketClient;
    private WebSocketListener listener;
    private boolean isConnecting = false;
    private boolean shouldReconnect = true;
    private String lastGroupId;
    private String lastUserEmail;
    
    // 재연결 관리
    private AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private long lastReconnectTime = 0;
    private android.os.Handler reconnectHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable reconnectRunnable;
    
    // 중복 메시지 방지를 위한 최근 메시지 추적
    private Set<String> recentMessageHashes = new HashSet<>();
    private static final int MAX_RECENT_MESSAGES = 100;
    
    // 연결 상태 관리
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        FAILED
    }
    
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    
    // 웹소켓 메시지 리스너 인터페이스
    public interface WebSocketListener {
        void onConnect();
        void onMessage(String message);
        void onDisconnect(int code, String reason);
        void onError(Exception error);
        void onConnectionStateChanged(ConnectionState state);
        void onReconnectAttempt(int attempt, int maxAttempts);
    }
    
    // 웹소켓 연결
    public void connect(String groupId, String userEmail, WebSocketListener listener) {
        this.listener = listener;
        this.lastGroupId = groupId;
        this.lastUserEmail = userEmail;
        this.shouldReconnect = true;
        
        // 재연결 카운터 리셋 (새로운 연결 시도)
        if (connectionState == ConnectionState.DISCONNECTED) {
            reconnectAttempts.set(0);
        }
        
        // 이미 연결 중이면 중복 호출 방지
        if (isConnecting) {
            Log.d(TAG, "이미 웹소켓 연결 진행 중");
            return;
        }
        
        // 기존 재연결 작업 취소
        cancelReconnectTask();
        
        // 이미 연결된 웹소켓이 있으면 닫기
        if (webSocketClient != null) {
            try {
                webSocketClient.close();
            } catch (Exception e) {
                Log.e(TAG, "웹소켓 닫기 오류", e);
            }
        }
        
        // 이메일이 null이거나 비어있으면 기본값 사용
        if (userEmail == null || userEmail.isEmpty()) {
            userEmail = "guest_" + System.currentTimeMillis() + "@example.com";
            Log.w(TAG, "사용자 이메일 없음, 게스트 이메일 사용: " + userEmail);
        }
        
        // GroupId가 null이거나 비어있으면 기본값 사용
        if (groupId == null || groupId.isEmpty()) {
            groupId = "1";
            Log.w(TAG, "그룹 ID 없음, 기본 그룹 ID 사용: " + groupId);
        }
        
        try {
            isConnecting = true;
            setConnectionState(ConnectionState.CONNECTING);
            
            // 웹소켓 연결 URL 생성: ws://202.31.246.51:80/ws/[그룹ID]/[사용자이메일]
            URI serverUri = new URI(WS_BASE_URL + "/" + groupId + "/" + userEmail);
            Log.d(TAG, "웹소켓 연결 시도: " + serverUri.toString());
            
            // 웹소켓 클라이언트 생성
            webSocketClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "웹소켓 연결됨");
                    isConnecting = false;
                    reconnectAttempts.set(0); // 성공적으로 연결되면 재연결 카운터 리셋
                    setConnectionState(ConnectionState.CONNECTED);
                    
                    if (listener != null) {
                        listener.onConnect();
                    }
                }
                
                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "메시지 수신: " + message);
                    
                    // 메시지 구조 자세히 로깅 (길이와 형식 검사)
                    try {
                        JSONObject json = new JSONObject(message);
                        StringBuilder fields = new StringBuilder();
                        fields.append("메시지 필드: ");
                        
                        if (json.has("type")) fields.append("type=").append(json.opt("type")).append(", ");
                        if (json.has("message_type")) fields.append("message_type=").append(json.opt("message_type")).append(", ");
                        if (json.has("sender_id")) fields.append("sender_id=").append(json.opt("sender_id")).append(", ");
                        if (json.has("senderId")) fields.append("senderId=").append(json.opt("senderId")).append(", ");
                        if (json.has("sender_name")) fields.append("sender_name=").append(json.opt("sender_name")).append(", ");
                        if (json.has("senderName")) fields.append("senderName=").append(json.opt("senderName")).append(", ");
                        if (json.has("content")) fields.append("content=").append(json.opt("content")).append(", ");
                        if (json.has("message")) fields.append("message=").append(json.opt("message")).append(", ");
                        
                        Log.d(TAG, fields.toString());
                    } catch (Exception e) {
                        Log.e(TAG, "JSON 파싱 실패: " + e.getMessage());
                    }
                    
                    if (listener != null) {
                        listener.onMessage(message);
                    }
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "웹소켓 연결 종료: " + reason + " (코드: " + code + ", 원격: " + remote + ")");
                    isConnecting = false;
                    
                    // 연결 상태 업데이트
                    if (shouldReconnect && remote) {
                        setConnectionState(ConnectionState.RECONNECTING);
                        scheduleReconnect();
                    } else {
                        setConnectionState(ConnectionState.DISCONNECTED);
                    }
                    
                    if (listener != null) {
                        listener.onDisconnect(code, reason);
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "웹소켓 오류", ex);
                    isConnecting = false;
                    setConnectionState(ConnectionState.FAILED);
                    
                    if (listener != null) {
                        listener.onError(ex);
                    }
                    
                    // 오류 발생 시에도 재연결 시도
                    if (shouldReconnect) {
                        setConnectionState(ConnectionState.RECONNECTING);
                        scheduleReconnect();
                    }
                }
            };
            
            // 연결 타임아웃 설정 (30초로 단축)
            webSocketClient.setConnectionLostTimeout(CONNECTION_TIMEOUT);
            
            // 웹소켓 연결 시작
            webSocketClient.connect();
            
        } catch (URISyntaxException e) {
            Log.e(TAG, "잘못된 웹소켓 URI", e);
            isConnecting = false;
            setConnectionState(ConnectionState.FAILED);
            if (listener != null) {
                listener.onError(e);
            }
        } catch (Exception e) {
            Log.e(TAG, "웹소켓 연결 중 예상치 못한 오류", e);
            isConnecting = false;
            setConnectionState(ConnectionState.FAILED);
            if (listener != null) {
                listener.onError(e);
            }
        }
    }
    
    // 지수 백오프를 사용한 재연결 스케줄링
    private void scheduleReconnect() {
        if (!shouldReconnect || reconnectAttempts.get() >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "재연결 중단: shouldReconnect=" + shouldReconnect + 
                      ", attempts=" + reconnectAttempts.get());
            setConnectionState(ConnectionState.FAILED);
            return;
        }
        
        int currentAttempt = reconnectAttempts.incrementAndGet();
        
        // 지수 백오프 계산 (2초, 4초, 8초, 16초, ... 최대 15초)
        long delay = Math.min(INITIAL_RECONNECT_DELAY * (1L << (currentAttempt - 1)), MAX_RECONNECT_DELAY);
        
        // 마지막 재연결 시도로부터 최소 간격 보장
        long timeSinceLastAttempt = System.currentTimeMillis() - lastReconnectTime;
        if (timeSinceLastAttempt < delay) {
            delay = delay - timeSinceLastAttempt + 1000; // 추가 1초 대기
        }
        
        Log.d(TAG, "재연결 예약: " + currentAttempt + "/" + MAX_RECONNECT_ATTEMPTS + 
                  " 시도, " + delay + "ms 후");
        
        if (listener != null) {
            listener.onReconnectAttempt(currentAttempt, MAX_RECONNECT_ATTEMPTS);
        }
        
        cancelReconnectTask();
        reconnectRunnable = () -> {
            if (shouldReconnect && listener != null) {
                lastReconnectTime = System.currentTimeMillis();
                Log.d(TAG, "자동 재연결 시도: " + currentAttempt + "/" + MAX_RECONNECT_ATTEMPTS);
                connect(lastGroupId, lastUserEmail, listener);
            }
        };
        
        reconnectHandler.postDelayed(reconnectRunnable, delay);
    }
    
    // 재연결 작업 취소
    private void cancelReconnectTask() {
        if (reconnectRunnable != null) {
            reconnectHandler.removeCallbacks(reconnectRunnable);
            reconnectRunnable = null;
        }
    }
    
    // 연결 상태 변경
    private void setConnectionState(ConnectionState newState) {
        if (connectionState != newState) {
            connectionState = newState;
            Log.d(TAG, "연결 상태 변경: " + newState);
            if (listener != null) {
                listener.onConnectionStateChanged(newState);
            }
        }
    }
    
    // 메시지 전송
    public void sendMessage(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            try {
                Log.d(TAG, "메시지 전송: " + message);
                
                // 메시지 구조 검증
                try {
                    JSONObject json = new JSONObject(message);
                    Log.d(TAG, "전송 메시지 유효성: 그룹ID=" + 
                          (json.has("group_id") ? json.opt("group_id") : "없음") +
                          ", 내용길이=" + 
                          (json.has("message") ? json.optString("message").length() : 
                          (json.has("content") ? json.optString("content").length() : 0)));
                } catch (Exception e) {
                    Log.e(TAG, "전송 메시지 JSON 검증 실패: " + e.getMessage());
                }
                
                webSocketClient.send(message);
            } catch (Exception e) {
                Log.e(TAG, "메시지 전송 오류", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        } else {
            Log.e(TAG, "웹소켓이 연결되지 않았습니다");
        }
    }
    
    // 서버 메시지 큐 상태 확인
    public void checkServerQueue(String groupId, QueueCheckCallback callback) {
        // 실제 구현에서는 HTTP 요청으로 서버 큐 상태를 확인
        // 여기서는 간단한 웹소켓 메시지로 구현
        if (isConnected()) {
            try {
                JSONObject request = new JSONObject();
                request.put("type", "queue_check");
                request.put("group_id", groupId);
                sendMessage(request.toString());
                
                // 콜백은 onMessage에서 처리
                if (callback != null) {
                    callback.onQueueCheckSent();
                }
            } catch (Exception e) {
                Log.e(TAG, "큐 상태 확인 요청 실패", e);
                if (callback != null) {
                    callback.onQueueCheckError(e);
                }
            }
        } else {
            if (callback != null) {
                callback.onQueueCheckError(new Exception("웹소켓이 연결되지 않음"));
            }
        }
    }
    
    public interface QueueCheckCallback {
        void onQueueCheckSent();
        void onQueueCheckError(Exception error);
    }
    
    // 강제 재연결
    public void forceReconnect() {
        Log.d(TAG, "강제 재연결 시도");
        reconnectAttempts.set(0); // 재연결 카운터 리셋
        disconnect();
        if (lastGroupId != null && lastUserEmail != null && listener != null) {
            connect(lastGroupId, lastUserEmail, listener);
        }
    }
    
    // 연결 종료
    public void disconnect() {
        shouldReconnect = false; // 자동 재연결 비활성화
        cancelReconnectTask();
        
        if (webSocketClient != null) {
            try {
                webSocketClient.close();
            } catch (Exception e) {
                Log.e(TAG, "웹소켓 연결 종료 오류", e);
            } finally {
                webSocketClient = null;
                isConnecting = false;
            }
        }
        
        setConnectionState(ConnectionState.DISCONNECTED);
    }
    
    // 연결 상태 확인
    public boolean isConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }
    
    // 연결 중인지 확인
    public boolean isConnecting() {
        return isConnecting;
    }
    
    // 현재 연결 상태 반환
    public ConnectionState getConnectionState() {
        return connectionState;
    }
    
    // 재연결 시도 횟수 반환
    public int getReconnectAttempts() {
        return reconnectAttempts.get();
    }
} 
