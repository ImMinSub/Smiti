package com.example.smiti.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.smiti.Message;
import com.example.smiti.database.MessageDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;

/**
 * 메시지 데이터 관리를 위한 저장소 클래스
 */
public class MessageRepository {
    private static final String TAG = "MessageRepository";
    private static final int MAX_MESSAGES_PER_GROUP = 1000; // 그룹당 최대 메시지 수
    
    private final MessageDatabase messageDatabase;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    public MessageRepository(Context context) {
        messageDatabase = MessageDatabase.getInstance(context);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 메시지 로드 결과를 받기 위한 콜백 인터페이스
     */
    public interface MessageLoadCallback {
        void onMessagesLoaded(List<Message> messages);
        void onError(String error);
    }
    
    /**
     * 메시지 저장 결과를 받기 위한 콜백 인터페이스
     */
    public interface SaveCallback {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * 특정 그룹의 메시지를 비동기적으로 로드합니다.
     */
    public void loadMessagesForGroup(String groupId, MessageLoadCallback callback) {
        executor.execute(() -> {
            try {
                List<Message> messages = messageDatabase.getMessagesForGroup(groupId);
                
                // 메인 스레드에서 콜백 실행
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onMessagesLoaded(messages);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "메시지 로드 실패: " + groupId, e);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("메시지 로드 실패: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 새 메시지를 비동기적으로 저장합니다.
     */
    public void saveMessage(String groupId, Message message) {
        executor.execute(() -> {
            try {
                long result = messageDatabase.insertMessage(groupId, message);
                
                if (result != -1) {
                    // 메시지 수가 너무 많으면 오래된 메시지 정리
                    int messageCount = messageDatabase.getMessageCountForGroup(groupId);
                    if (messageCount > MAX_MESSAGES_PER_GROUP) {
                        messageDatabase.deleteOldMessages(groupId, MAX_MESSAGES_PER_GROUP / 2);
                        Log.d(TAG, "그룹 " + groupId + "의 오래된 메시지 정리됨");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "메시지 저장 실패: " + groupId, e);
            }
        });
    }
    
    /**
     * 새 메시지를 비동기적으로 저장합니다 (콜백 포함).
     */
    public void saveMessage(Message message, SaveCallback callback) {
        executor.execute(() -> {
            try {
                String groupId = message.getGroupId();
                if (groupId == null) {
                    groupId = "default";
                }
                
                long result = messageDatabase.insertMessage(groupId, message);
                
                if (result != -1) {
                    // 메시지 수가 너무 많으면 오래된 메시지 정리
                    int messageCount = messageDatabase.getMessageCountForGroup(groupId);
                    if (messageCount > MAX_MESSAGES_PER_GROUP) {
                        messageDatabase.deleteOldMessages(groupId, MAX_MESSAGES_PER_GROUP / 2);
                        Log.d(TAG, "그룹 " + groupId + "의 오래된 메시지 정리됨");
                    }
                    
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onError("메시지 저장 실패");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "메시지 저장 실패", e);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("메시지 저장 실패: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 특정 그룹의 메시지 개수를 가져옵니다.
     */
    public void getMessageCount(String groupId, MessageCountCallback callback) {
        executor.execute(() -> {
            try {
                int count = messageDatabase.getMessageCountForGroup(groupId);
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onCountLoaded(count);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "메시지 개수 조회 실패: " + groupId, e);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("개수 조회 실패: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 메시지 개수 조회 콜백 인터페이스
     */
    public interface MessageCountCallback {
        void onCountLoaded(int count);
        void onError(String error);
    }
    
    /**
     * 메시지 검색 기능
     */
    public void searchMessages(String groupId, String searchQuery, MessageLoadCallback callback) {
        executor.execute(() -> {
            try {
                // 간단한 검색 구현 (실제로는 데이터베이스에서 LIKE 쿼리 사용)
                List<Message> allMessages = messageDatabase.getMessagesForGroup(groupId);
                List<Message> searchResults = new ArrayList<>();
                
                String lowerQuery = searchQuery.toLowerCase();
                for (Message message : allMessages) {
                    if (message.getMessage() != null && 
                        message.getMessage().toLowerCase().contains(lowerQuery)) {
                        searchResults.add(message);
                    }
                }
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onMessagesLoaded(searchResults);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "메시지 검색 실패: " + groupId, e);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("검색 실패: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 리소스 정리
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
