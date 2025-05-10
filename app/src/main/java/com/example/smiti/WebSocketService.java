package com.example.smiti;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public class WebSocketService {
    private static final String TAG = "WebSocketService";
    private static final String WS_BASE_URL = "ws://202.31.246.51:80/ws";
    private static final int CONNECTION_TIMEOUT = 10; // 10초 타임아웃
    
    private WebSocketClient webSocketClient;
    private WebSocketListener listener;
    private boolean isConnecting = false;
    
    // 웹소켓 메시지 리스너 인터페이스
    public interface WebSocketListener {
        void onConnect();
        void onMessage(String message);
        void onDisconnect(int code, String reason);
        void onError(Exception error);
    }
    
    // 웹소켓 연결
    public void connect(String groupId, String userEmail, WebSocketListener listener) {
        this.listener = listener;
        
        // 이미 연결 중이면 중복 호출 방지
        if (isConnecting) {
            Log.d(TAG, "이미 웹소켓 연결 진행 중");
            return;
        }
        
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
            
            // 웹소켓 연결 URL 생성: ws://202.31.246.51:80/ws/[그룹ID]/[사용자이메일]
            URI serverUri = new URI(WS_BASE_URL + "/" + groupId + "/" + userEmail);
            Log.d(TAG, "웹소켓 연결 시도: " + serverUri.toString());
            
            // 웹소켓 클라이언트 생성
            webSocketClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "웹소켓 연결됨");
                    isConnecting = false;
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
                    Log.d(TAG, "웹소켓 연결 종료: " + reason);
                    isConnecting = false;
                    if (listener != null) {
                        listener.onDisconnect(code, reason);
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "웹소켓 오류", ex);
                    isConnecting = false;
                    if (listener != null) {
                        listener.onError(ex);
                    }
                }
            };
            
            // 연결 타임아웃 설정
            webSocketClient.setConnectionLostTimeout(CONNECTION_TIMEOUT);
            
            // 웹소켓 연결 시작
            webSocketClient.connect();
            
        } catch (URISyntaxException e) {
            Log.e(TAG, "잘못된 웹소켓 URI", e);
            isConnecting = false;
            if (listener != null) {
                listener.onError(e);
            }
        } catch (Exception e) {
            Log.e(TAG, "웹소켓 연결 중 예상치 못한 오류", e);
            isConnecting = false;
            if (listener != null) {
                listener.onError(e);
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
    
    // 연결 종료
    public void disconnect() {
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
    }
    
    // 연결 상태 확인
    public boolean isConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }
    
    // 연결 중인지 확인
    public boolean isConnecting() {
        return isConnecting;
    }
} 