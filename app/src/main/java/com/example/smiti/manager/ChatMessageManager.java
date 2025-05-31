package com.example.smiti.manager;

import android.content.Context;
import android.util.Log;

import com.example.smiti.ChatMessage;
import com.example.smiti.Message;
import com.example.smiti.MessageAdapter;
import com.example.smiti.repository.MessageRepository;
import com.example.smiti.WebSocketService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatMessageManager {
    
    private static final String TAG = "ChatMessageManager";
    
    private final Context context;
    private final MessageAdapter messageAdapter;
    private final List<Message> messageList;
    private final MessageRepository messageRepository;
    private final String currentGroupId;
    private final String currentUserEmail;
    private final String currentUserName;
    
    // 보낸 메시지의 정보를 저장하여 에코 메시지 처리에 사용
    private final Map<String, ChatMessage> sentMessagesMap = new ConcurrentHashMap<>();
    private final Set<String> messageHashSet = new HashSet<>();
    
    public interface MessageSendCallback {
        void onMessageReady(ChatMessage message, String localId);
        void onSendFailed(String error);
    }
    
    public interface MessageProcessCallback {
        void onMessageProcessed(Message message, boolean isEcho, boolean isDuplicate);
        void onMessageRemoved(Message message);
    }
    
    public ChatMessageManager(Context context, MessageAdapter messageAdapter, 
                             List<Message> messageList, MessageRepository messageRepository,
                             String currentGroupId, String currentUserEmail, String currentUserName) {
        this.context = context;
        this.messageAdapter = messageAdapter;
        this.messageList = messageList;
        this.messageRepository = messageRepository;
        this.currentGroupId = currentGroupId;
        this.currentUserEmail = currentUserEmail;
        this.currentUserName = currentUserName;
    }
    
    /**
     * 텍스트 메시지 전송 준비
     */
    public void prepareTextMessage(String messageText, MessageSendCallback callback) {
        if (messageText == null || messageText.trim().isEmpty()) {
            callback.onSendFailed("메시지가 비어있습니다");
            return;
        }
        
        String localId = UUID.randomUUID().toString();
        ChatMessage chatMessage = new ChatMessage(currentUserEmail, currentUserName, messageText.trim());
        chatMessage.setGroupId(currentGroupId);
        chatMessage.setLocalId(localId);
        
        Log.d(TAG, "텍스트 메시지 준비: " + messageText + ", localId=" + localId);
        
        // 원본 메시지 정보 저장 (Echo 처리용)
        sentMessagesMap.put(localId, chatMessage);
        
        callback.onMessageReady(chatMessage, localId);
    }
    
    /**
     * 메시지 처리 (수신된 메시지)
     */
    public void processReceivedMessage(String rawJsonMessage, MessageProcessCallback callback) {
        try {
            ChatMessage chatMessage = ChatMessage.fromJson(rawJsonMessage);
            Log.d(TAG, "메시지 수신: SenderId=[" + chatMessage.getSenderId() + 
                    "], type=[" + chatMessage.getType() + "]");
            
            String receivedLocalId = chatMessage.getLocalId();
            boolean isEchoMessage = false;
            ChatMessage originalMessage = null;
            
            // Echo 메시지 감지
            if (receivedLocalId != null && !receivedLocalId.isEmpty()) {
                originalMessage = sentMessagesMap.get(receivedLocalId);
                isEchoMessage = originalMessage != null;
                
                if (isEchoMessage) {
                    Log.d(TAG, "Echo 메시지 감지: " + receivedLocalId);
                    sentMessagesMap.remove(receivedLocalId);
                }
            }
            
            // Echo가 아니면서 내가 보낸 메시지인 경우 추가 검사
            if (!isEchoMessage && isCurrentUserMessage(chatMessage.getSenderId(), chatMessage.getSenderName())) {
                String messageContent = chatMessage.getContent();
                
                for (Map.Entry<String, ChatMessage> entry : new HashMap<>(sentMessagesMap).entrySet()) {
                    ChatMessage sentMessage = entry.getValue();
                    
                    if (messageContent != null && messageContent.equals(sentMessage.getContent())) {
                        if ("file".equals(sentMessage.getType()) && 
                            sentMessage.getFileUrl() != null && !sentMessage.getFileUrl().isEmpty()) {
                            
                            Log.d(TAG, "내용 기반 파일 Echo 메시지 감지: " + messageContent);
                            chatMessage.setFileUrl(sentMessage.getFileUrl());
                            chatMessage.setFileType(sentMessage.getFileType());
                            chatMessage.setType("file");
                            
                            originalMessage = sentMessage;
                            isEchoMessage = true;
                            sentMessagesMap.remove(entry.getKey());
                            break;
                        }
                    }
                }
            }
            
            Message uiMessage = chatMessage.toUIMessage();
            boolean isMyMessage = isCurrentUserMessage(uiMessage.getSenderId(), uiMessage.getSenderName());
            
            // 메시지 정규화
            final Message finalUiMessage;
            if (isEchoMessage || isMyMessage) {
                finalUiMessage = new Message(currentUserEmail, 
                        currentUserName != null ? currentUserName : "나", 
                        uiMessage.getMessage(), uiMessage.getTimestamp());
                if (chatMessage.getFileUrl() != null && !chatMessage.getFileUrl().isEmpty()) {
                    finalUiMessage.setFileUrl(chatMessage.getFileUrl());
                    finalUiMessage.setFileType(chatMessage.getFileType());
                }
            } else {
                finalUiMessage = uiMessage;
            }
            
            // 중복 검사
            boolean isDuplicate = isDuplicateMessageEnhanced(finalUiMessage);
            
            callback.onMessageProcessed(finalUiMessage, isEchoMessage, isDuplicate);
            
        } catch (JSONException e) {
            Log.e(TAG, "메시지 파싱 오류", e);
        } catch (Exception e) {
            Log.e(TAG, "메시지 처리 중 오류", e);
        }
    }
    
    /**
     * Echo 메시지 처리
     */
    public void handleEchoMessage(Message message) {
        synchronized (messageList) {
            // 기존 동일한 메시지 제거
            messageList.removeIf(existingMessage -> {
                boolean isSameContent = isSameMessageContent(existingMessage, message);
                boolean isMyExistingMessage = isCurrentUserMessage(existingMessage.getSenderId(), existingMessage.getSenderName());
                long timeDiff = Math.abs(existingMessage.getTimestamp() - message.getTimestamp());
                boolean isRecentMessage = timeDiff < 30000; // 30초
                
                if (isSameContent && isMyExistingMessage && isRecentMessage) {
                    Log.d(TAG, "Echo 메시지 처리 - 기존 메시지 제거: " + existingMessage.getMessage());
                    return true;
                }
                return false;
            });
        }
        
        messageAdapter.addMessage(message);
        synchronized (messageList) {
            messageAdapter.notifyDataSetChanged();
        }
        messageRepository.saveMessage(currentGroupId, message);
          Log.d(TAG, "Echo 메시지 처리 완료: " + message.getMessage());
    }
    
    /**
     * 동기화된 메시지 처리 (중복 검사 완화)
     */
    public boolean processSyncedMessage(Message message, MessageProcessCallback callback) {
        if (message == null) {
            return false;
        }

        boolean isDuplicate = false;
        boolean isFileMessage = message.hasFile();
        
        synchronized (messageList) {
            for (Message existingMessage : messageList) {
                // 파일 메시지의 경우 더 관대한 중복 검사
                if (isFileMessage && existingMessage.hasFile()) {
                    if (message.getFileUrl() != null && existingMessage.getFileUrl() != null &&
                        message.getFileUrl().equals(existingMessage.getFileUrl()) &&
                        isSameSender(message, existingMessage)) {
                        
                        long timeDiff = Math.abs(message.getTimestamp() - existingMessage.getTimestamp());
                        if (timeDiff < 2000) { // 동기화 시에는 2초 이내만 중복으로 판단
                            isDuplicate = true;
                            Log.d(TAG, "동기화 중복 파일 메시지 감지: " + message.getFileUrl() + ", 시간차: " + timeDiff + "ms");
                            break;
                        }
                    }
                } 
                // 텍스트 메시지의 경우 기존 로직 유지
                else if (!isFileMessage && !existingMessage.hasFile()) {
                    if (message.getMessage() != null && existingMessage.getMessage() != null &&
                        message.getMessage().trim().equals(existingMessage.getMessage().trim()) &&
                        isSameSender(message, existingMessage)) {
                        
                        long timeDiff = Math.abs(message.getTimestamp() - existingMessage.getTimestamp());
                        if (timeDiff < 30000) { // 텍스트는 30초 이내 중복 판단
                            isDuplicate = true;
                            Log.d(TAG, "동기화 중복 텍스트 메시지 감지: " + message.getMessage() + ", 시간차: " + timeDiff + "ms");
                            break;
                        }
                    }
                }
            }
        }

        if (!isDuplicate) {
            messageAdapter.addMessage(message);
            synchronized (messageList) {
                messageAdapter.notifyDataSetChanged();
            }
            messageRepository.saveMessage(currentGroupId, message);
            Log.d(TAG, "동기화 메시지 추가 완료: " + (isFileMessage ? "파일=" + message.getFileUrl() : "텍스트=" + message.getMessage()));
        }

        if (callback != null) {
            callback.onMessageProcessed(message, false, isDuplicate);
        }

        return !isDuplicate;
    }
    
    /**
     * 내 메시지 중복 검사 및 처리
     */
    public boolean handleMyMessage(Message message) {
        boolean isDuplicate = false;
        synchronized (messageList) {
            for (Message existingMessage : messageList) {
                boolean isMyExistingMessage = isCurrentUserMessage(existingMessage.getSenderId(), existingMessage.getSenderName());
                if (isMyExistingMessage && isSameMessageContent(existingMessage, message)) {
                    long timeDiff = Math.abs(existingMessage.getTimestamp() - message.getTimestamp());
                    if (timeDiff < 60000) { // 1분 이내 중복은 차단
                        isDuplicate = true;
                        Log.d(TAG, "내 메시지 중복 감지, 추가 건너뜀: " + message.getMessage());
                        break;
                    }
                }
            }
        }
        
        if (!isDuplicate) {
            messageAdapter.addMessage(message);
            synchronized (messageList) {
                messageAdapter.notifyDataSetChanged();
            }
            messageRepository.saveMessage(currentGroupId, message);
            Log.d(TAG, "내 메시지 (비Echo) 처리 완료: " + message.getMessage());
        }
        
        return !isDuplicate;
    }
    
    /**
     * 다른 사용자 메시지 처리
     */
    public boolean handleOtherUserMessage(Message message) {
        if (!isDuplicateMessageEnhanced(message)) {
            messageAdapter.addMessage(message);
            synchronized (messageList) {
                messageAdapter.notifyDataSetChanged();
            }
            messageRepository.saveMessage(currentGroupId, message);
            
            if (message.hasFile()) {
                Log.d(TAG, "파일 메시지 추가됨: " + message.getFileUrl());
            }
            
            Log.d(TAG, "다른 사용자 메시지 처리 완료: " + message.getMessage());
            return true;
        } else {
            Log.d(TAG, "중복 메시지 감지, 추가 건너뜀: " + message.getMessage());
            return false;
        }
    }
    
    /**
     * 현재 사용자 메시지인지 판단
     */
    private boolean isCurrentUserMessage(String senderId, String senderName) {
        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            return false;
        }
        
        // 이메일 정확 매칭
        if (senderId != null && currentUserEmail.equalsIgnoreCase(senderId.trim())) {
            return true;
        }
        
        // 이메일 앞부분 매칭
        if (senderId != null && currentUserEmail.contains("@")) {
            String emailPrefix = currentUserEmail.split("@")[0];
            if (emailPrefix.equalsIgnoreCase(senderId.trim())) {
                return true;
            }
        }
        
        // 발신자명으로 판단
        if (senderName != null) {
            String normalizedName = senderName.trim().toLowerCase();
            if (normalizedName.equals("나") || normalizedName.equals("me") || normalizedName.equals("myself")) {
                return true;
            }
            
            if (currentUserEmail.contains("@")) {
                String emailPrefix = currentUserEmail.split("@")[0];
                if (emailPrefix.equalsIgnoreCase(senderName.trim())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 메시지 내용이 동일한지 확인
     */
    private boolean isSameMessageContent(Message msg1, Message msg2) {
        if (msg1 == null || msg2 == null) {
            return false;
        }
        
        String content1 = msg1.getMessage();
        String content2 = msg2.getMessage();
        if (content1 != null) content1 = content1.trim();
        if (content2 != null) content2 = content2.trim();
        
        if (!java.util.Objects.equals(content1, content2)) {
            return false;
        }
        
        String fileUrl1 = msg1.getFileUrl();
        String fileUrl2 = msg2.getFileUrl();
        if (fileUrl1 != null) fileUrl1 = fileUrl1.trim();
        if (fileUrl2 != null) fileUrl2 = fileUrl2.trim();
        
        return java.util.Objects.equals(fileUrl1, fileUrl2);
    }
    
    /**
     * 중복 메시지 감지 (향상된 버전)
     */
    private boolean isDuplicateMessageEnhanced(Message newMessage) {
        if (newMessage == null) {
            return false;
        }
        
        synchronized (messageList) {
            for (Message existingMessage : messageList) {
                // 정확한 동일 메시지 체크
                if (isSameMessageExact(newMessage, existingMessage)) {
                    Log.d(TAG, "정확히 동일한 메시지 중복 감지: " + newMessage.getMessage());
                    return true;
                }
                  // 파일 메시지의 경우 파일 URL + 타임스탬프 기준으로 중복 검사
                if (newMessage.hasFile() && existingMessage.hasFile()) {
                    if (newMessage.getFileUrl() != null && existingMessage.getFileUrl() != null &&
                        newMessage.getFileUrl().equals(existingMessage.getFileUrl()) &&
                        isSameSender(newMessage, existingMessage)) {
                        
                        // 파일 메시지는 타임스탬프도 고려하여 더 엄격하게 중복 검사
                        long timeDiff = Math.abs(newMessage.getTimestamp() - existingMessage.getTimestamp());
                        if (timeDiff < 5000) { // 5초 이내만 중복으로 판단 (파일 메시지는 더 관대하게)
                            Log.d(TAG, "중복 파일 메시지 감지: " + newMessage.getFileUrl() + ", 시간차: " + timeDiff + "ms");
                            return true;
                        } else {
                            Log.d(TAG, "동일한 파일이지만 시간차가 커서 중복이 아님: " + newMessage.getFileUrl() + ", 시간차: " + timeDiff + "ms");
                        }
                    }
                    continue;
                }
                
                // 텍스트 메시지의 경우
                if (!newMessage.hasFile() && !existingMessage.hasFile()) {
                    if (newMessage.getMessage() != null && existingMessage.getMessage() != null &&
                        newMessage.getMessage().trim().equals(existingMessage.getMessage().trim()) &&
                        isSameSender(newMessage, existingMessage)) {
                        
                        long timeDiff = Math.abs(newMessage.getTimestamp() - existingMessage.getTimestamp());
                        if (timeDiff < 60000) { // 1분 이내만 중복으로 판단
                            Log.d(TAG, "중복 텍스트 메시지 감지: " + newMessage.getMessage() + ", 시간차: " + timeDiff + "ms");
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 같은 발신자인지 확인
     */
    private boolean isSameSender(Message msg1, Message msg2) {
        if (msg1 == null || msg2 == null) {
            return false;
        }
        
        String sender1 = normalizeSenderId(msg1.getSenderId(), msg1.getSenderName());
        String sender2 = normalizeSenderId(msg2.getSenderId(), msg2.getSenderName());
        
        return sender1.equals(sender2);
    }
    
    /**
     * 발신자 ID 정규화
     */
    private String normalizeSenderId(String senderId, String senderName) {
        if (isCurrentUserMessage(senderId, senderName)) {
            return currentUserEmail != null ? currentUserEmail.toLowerCase().trim() : "me";
        }
        
        if (senderId != null) {
            return senderId.toLowerCase().trim();
        }
        
        if (senderName != null) {
            return senderName.toLowerCase().trim();
        }
        
        return "unknown";
    }
    
    /**
     * 두 메시지가 정확히 동일한지 확인
     */
    private boolean isSameMessageExact(Message msg1, Message msg2) {
        if (msg1 == null || msg2 == null) {
            return false;
        }
        
        // 타임스탬프 비교
        if (msg1.getTimestamp() != msg2.getTimestamp()) {
            return false;
        }
        
        // 발신자 ID 비교
        String senderId1 = msg1.getSenderId();
        String senderId2 = msg2.getSenderId();
        if (senderId1 != null) senderId1 = senderId1.trim().toLowerCase();
        if (senderId2 != null) senderId2 = senderId2.trim().toLowerCase();
        
        if (!java.util.Objects.equals(senderId1, senderId2)) {
            return false;
        }
        
        // 메시지 내용 비교
        String content1 = msg1.getMessage();
        String content2 = msg2.getMessage();
        if (content1 != null) content1 = content1.trim();
        if (content2 != null) content2 = content2.trim();
        
        if (!java.util.Objects.equals(content1, content2)) {
            return false;
        }
        
        // 파일 URL 비교
        String fileUrl1 = msg1.getFileUrl();
        String fileUrl2 = msg2.getFileUrl();
        if (fileUrl1 != null) fileUrl1 = fileUrl1.trim();
        if (fileUrl2 != null) fileUrl2 = fileUrl2.trim();
        
        return java.util.Objects.equals(fileUrl1, fileUrl2);
    }
    
    /**
     * 메시지 해시 세트 정리
     */
    public void cleanupMessageHashSet() {
        synchronized (messageHashSet) {
            Log.d(TAG, "메시지 해시 세트 정리 시작 - 현재 크기: " + messageHashSet.size());
            
            Set<String> currentHashes = new HashSet<>();
            synchronized (messageList) {
                for (Message message : messageList) {
                    currentHashes.add(generateMessageHashEnhanced(message));
                }
            }
            
            messageHashSet.clear();
            messageHashSet.addAll(currentHashes);
            
            Log.d(TAG, "메시지 해시 세트 정리 완료 - 정리 후 크기: " + messageHashSet.size());
        }
    }
    
    /**
     * 중복 메시지 감지를 위한 향상된 해시 생성
     */
    private String generateMessageHashEnhanced(Message message) {
        if (message == null) {
            return "";
        }
        
        StringBuilder hashBuilder = new StringBuilder();
        hashBuilder.append(message.getSenderId()).append("|");
        hashBuilder.append(message.getMessage()).append("|");
        hashBuilder.append(message.getTimestamp()).append("|");
        
        if (message.hasFile()) {
            hashBuilder.append("FILE|");
            hashBuilder.append(message.getFileUrl() != null ? message.getFileUrl() : "").append("|");
            hashBuilder.append(message.getFileType() != null ? message.getFileType() : "").append("|");
        } else {
            hashBuilder.append("TEXT|");
        }
        
        return hashBuilder.toString();
    }
    
    /**
     * 전송 실패 시 정리
     */
    public void cleanupFailedMessage(String localId) {
        sentMessagesMap.remove(localId);
    }
    
    /**
     * 메시지 목록 정렬
     */
    public void sortMessagesByTimestamp() {
        try {
            synchronized (messageList) {
                messageList.sort((m1, m2) -> {
                    long timestamp1 = m1.getTimestamp();
                    long timestamp2 = m2.getTimestamp();
                    return Long.compare(timestamp1, timestamp2);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "메시지 정렬 오류", e);
        }
    }
} 