package com.example.smiti;

import android.app.Activity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class ConnectionStateUIManager {
    private static final String TAG = "ConnectionStateUIManager";
    
    private Activity activity;
    private View rootView;
    private ProgressBar syncProgressBar;
    private TextView connectionStatusText;
    private Snackbar currentSnackbar;
    
    public ConnectionStateUIManager(Activity activity, View rootView) {
        this.activity = activity;
        this.rootView = rootView;
        initViews();
    }
    
    private void initViews() {
        // 실제 구현에서는 레이아웃에서 뷰를 찾아야 함
        // 여기서는 예시로 null 체크만 수행
    }
    
    // 동기화 진행 상황 표시 (Signal처럼 동기화 중임을 사용자에게 표시)
    public void showSyncProgress(String groupId) {
        activity.runOnUiThread(() -> {
            if (syncProgressBar != null) {
                syncProgressBar.setVisibility(View.VISIBLE);
            }
            
            showSnackbar("메시지 동기화 중...", Snackbar.LENGTH_INDEFINITE);
        });
    }
    
    // 동기화 완료 알림
    public void onSyncCompleted(String groupId, int syncedMessageCount) {
        activity.runOnUiThread(() -> {
            if (syncProgressBar != null) {
                syncProgressBar.setVisibility(View.GONE);
            }
            
            dismissCurrentSnackbar();
            
            if (syncedMessageCount > 0) {
                showSnackbar(
                    String.format("%d개의 새 메시지가 동기화되었습니다.", syncedMessageCount),
                    Snackbar.LENGTH_LONG
                );
            }
        });
    }
    
    // 연결 상태 변경 표시
    public void onConnectionStateChanged(WebSocketService.ConnectionState state) {
        activity.runOnUiThread(() -> {
            String statusText = getConnectionStatusText(state);
            
            if (connectionStatusText != null) {
                connectionStatusText.setText(statusText);
                connectionStatusText.setVisibility(View.VISIBLE);
            }
            
            // 연결 상태에 따른 UI 업데이트
            switch (state) {
                case CONNECTING:
                    showSnackbar("서버에 연결 중...", Snackbar.LENGTH_SHORT);
                    break;
                    
                case CONNECTED:
                    dismissCurrentSnackbar();
                    if (connectionStatusText != null) {
                        connectionStatusText.setVisibility(View.GONE);
                    }
                    break;
                    
                case RECONNECTING:
                    showSnackbar("연결이 끊어졌습니다. 재연결 중...", Snackbar.LENGTH_INDEFINITE);
                    break;
                    
                case FAILED:
                    showSnackbar("서버 연결에 실패했습니다.", Snackbar.LENGTH_LONG);
                    break;
                    
                case DISCONNECTED:
                    if (connectionStatusText != null) {
                        connectionStatusText.setVisibility(View.GONE);
                    }
                    break;
            }
        });
    }
    
    // 재연결 시도 표시
    public void onReconnectAttempt(int attempt, int maxAttempts) {
        activity.runOnUiThread(() -> {
            String message = String.format("재연결 시도 중... (%d/%d)", attempt, maxAttempts);
            showSnackbar(message, Snackbar.LENGTH_SHORT);
        });
    }
    
    // 동기화 진행률 표시
    public void onSyncProgress(String groupId, int processedCount, int totalCount) {
        activity.runOnUiThread(() -> {
            int progress = (int) ((processedCount / (float) totalCount) * 100);
            String message = String.format("메시지 동기화 중... %d%%", progress);
            
            if (currentSnackbar != null) {
                currentSnackbar.setText(message);
            } else {
                showSnackbar(message, Snackbar.LENGTH_INDEFINITE);
            }
        });
    }
    
    // 동기화 오류 표시
    public void onSyncError(String groupId, String error) {
        activity.runOnUiThread(() -> {
            dismissCurrentSnackbar();
            showSnackbar("메시지 동기화 실패: " + error, Snackbar.LENGTH_LONG);
        });
    }
    
    // 네트워크 상태 변경 표시
    public void onNetworkStateChanged(boolean isConnected) {
        activity.runOnUiThread(() -> {
            if (isConnected) {
                showSnackbar("네트워크가 연결되었습니다.", Snackbar.LENGTH_SHORT);
            } else {
                showSnackbar("네트워크 연결이 끊어졌습니다.", Snackbar.LENGTH_LONG);
            }
        });
    }
    
    // 강제 재연결 버튼 표시
    public void showForceReconnectOption() {
        activity.runOnUiThread(() -> {
            Snackbar snackbar = Snackbar.make(rootView, 
                "연결에 문제가 있습니다.", Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("재연결", v -> {
                // 강제 재연결 콜백 호출
                if (forceReconnectCallback != null) {
                    forceReconnectCallback.onForceReconnect();
                }
            });
            snackbar.show();
            currentSnackbar = snackbar;
        });
    }
    
    // 연결 상태 텍스트 반환
    private String getConnectionStatusText(WebSocketService.ConnectionState state) {
        switch (state) {
            case CONNECTING:
                return "연결 중...";
            case CONNECTED:
                return "연결됨";
            case RECONNECTING:
                return "재연결 중...";
            case FAILED:
                return "연결 실패";
            case DISCONNECTED:
                return "연결 끊김";
            default:
                return "";
        }
    }
    
    // Snackbar 표시
    private void showSnackbar(String message, int duration) {
        dismissCurrentSnackbar();
        currentSnackbar = Snackbar.make(rootView, message, duration);
        currentSnackbar.show();
    }
    
    // 현재 Snackbar 해제
    private void dismissCurrentSnackbar() {
        if (currentSnackbar != null) {
            currentSnackbar.dismiss();
            currentSnackbar = null;
        }
    }
    
    // Toast 표시 (간단한 메시지용)
    public void showToast(String message) {
        activity.runOnUiThread(() -> {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        });
    }
    
    // 강제 재연결 콜백 인터페이스
    public interface ForceReconnectCallback {
        void onForceReconnect();
    }
    
    private ForceReconnectCallback forceReconnectCallback;
    
    public void setForceReconnectCallback(ForceReconnectCallback callback) {
        this.forceReconnectCallback = callback;
    }
    
    // 리소스 정리
    public void cleanup() {
        dismissCurrentSnackbar();
        forceReconnectCallback = null;
    }
} 
