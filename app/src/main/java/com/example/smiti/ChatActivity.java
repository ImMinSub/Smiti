package com.example.smiti;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smiti.manager.ChatFileManager;
import com.example.smiti.manager.ChatMessageManager;
import com.example.smiti.manager.ChatNetworkManager;
import com.example.smiti.manager.ChatUIManager;
import com.example.smiti.model.User;
import com.example.smiti.repository.MessageRepository;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements 
        WebSocketService.WebSocketListener,
        ChatUIManager.UICallback,
        ChatFileManager.FileUploadCallback,
        ChatMessageManager.MessageSendCallback,
        ChatMessageManager.MessageProcessCallback,
        ChatNetworkManager.NetworkCallback {

    private static final String TAG = "ChatActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String DEFAULT_GROUP_ID = "2";

    // 매니저 클래스들
    private ChatUIManager uiManager;
    private ChatMessageManager messageManager;
    private ChatFileManager fileManager;
    private ChatNetworkManager networkManager;
    
    // 기본 필드들
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private MessageRepository messageRepository;
    private WebSocketService webSocketService;
    private MessagePaginationManager paginationManager;
    
    // 사용자 정보
    private String currentUserId;
    private String currentUserName;
    private String currentUserEmail;
    private String currentGroupId = DEFAULT_GROUP_ID;
    
    // 멤버 관련
    private List<User> memberList;
    
    // 파일 선택 런처
    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::handleFileSelected);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        try {
            // 기본 초기화
            initializeBasicComponents();
            
            // 매니저 초기화
            initializeManagers();
            
            // UI 설정
            setupUI();
            
            // 데이터 로드
            loadInitialData();
            
            // 네트워크 설정
            setupNetwork();
            
        } catch (Exception e) {
            Log.e(TAG, "onCreate 오류", e);
            uiManager.showToast("앱 초기화 중 오류가 발생했습니다");
        }
    }
    
    /**
     * 기본 컴포넌트 초기화
     */
    private void initializeBasicComponents() {
        // 사용자 정보 로드
        loadUserData();
        
        // 인텐트에서 그룹 ID 가져오기
        String groupId = getIntent().getStringExtra("group_id");
        if (groupId != null && !groupId.isEmpty()) {
            currentGroupId = groupId;
        }
        
        // 메시지 저장소 초기화
        messageRepository = new MessageRepository(this);
        
        // 메시지 리스트 초기화
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList, currentUserEmail);
        
        // 멤버 리스트 초기화
        memberList = new ArrayList<>();
    }
    
    /**
     * 매니저 클래스들 초기화
     */
    private void initializeManagers() {
        // UI 매니저
        uiManager = new ChatUIManager(this);
        
        // 메시지 매니저
        messageManager = new ChatMessageManager(
            this, messageAdapter, messageList, messageRepository,
            currentGroupId, currentUserEmail, currentUserName
        );
        
        // 파일 매니저
        fileManager = new ChatFileManager(this);
        
        // 네트워크 매니저
        networkManager = new ChatNetworkManager(this, messageRepository, currentGroupId);
        networkManager.setNetworkCallback(this);
        
        // 페이지네이션 매니저
        initializePaginationManager();
    }
    
    /**
     * UI 설정
     */
    private void setupUI() {
        uiManager.initViews();
        uiManager.setupRecyclerView(messageAdapter);
        uiManager.setupMembersRecyclerView(memberList);
        uiManager.setupListeners(this);
        uiManager.setupBottomNavigation();
    }
    
    /**
     * 초기 데이터 로드
     */
    private void loadInitialData() {
        // 저장된 메시지 로드
        loadStoredMessagesOptimized();
        
        // 그룹 멤버 로드
        networkManager.loadGroupMembers();
    }
    
    /**
     * 네트워크 설정
     */
    private void setupNetwork() {
        networkManager.setupNetworkReceiver();
        networkManager.setupPeriodicSync();
    }
    
    /**
     * 페이지네이션 매니저 초기화
     */
    private void initializePaginationManager() {
        try {
            paginationManager = new MessagePaginationManager(
                this, currentGroupId, currentUserEmail, currentUserName,
                uiManager.getRecyclerView(), messageAdapter, messageList
            );
            
            paginationManager.setPaginationCallback(new MessagePaginationManager.PaginationCallback() {
                @Override
                public void onLoadingStarted() {
                    runOnUiThread(() -> {
                        messageAdapter.showLoadingIndicator("이전 메시지를 불러오는 중...");
                    });
                }
                
                @Override
                public void onLoadingFinished() {
                    runOnUiThread(() -> {
                        messageAdapter.hideLoadingIndicator();
                    });
                }
                
                @Override
                public void onMessagesLoaded(List<Message> messages, boolean hasMore) {
                    runOnUiThread(() -> {
                        if (!messages.isEmpty()) {
                            messageAdapter.addMessagesAtTop(messages);
                            uiManager.showToast(messages.size() + "개의 이전 메시지를 불러왔습니다");
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "페이지네이션 오류: " + error);
                        uiManager.showToast("메시지 로딩 중 오류가 발생했습니다");
                    });
                }
                
                @Override
                public void onNoMoreMessages() {
                    runOnUiThread(() -> {
                        uiManager.showToast("모든 메시지를 불러왔습니다");
                    });
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "페이지네이션 매니저 초기화 실패", e);
        }
    }
    
    /**
     * 최적화된 메시지 로드
     */
    private void loadStoredMessagesOptimized() {
        messageRepository.getMessageCount(currentGroupId, new MessageRepository.MessageCountCallback() {
            @Override
            public void onCountLoaded(int messageCount) {
                runOnUiThread(() -> {
                    if (messageCount > 500) { // 대용량 채팅방
                        Log.d(TAG, "대용량 채팅방 감지 - 페이지네이션 모드");
                        uiManager.showToast("대용량 채팅방입니다. 메시지를 점진적으로 불러옵니다.");
                        if (paginationManager != null) {
                            paginationManager.loadInitialMessages();
                        }
                    } else {
                        loadStoredMessages();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> loadStoredMessages());
            }
        });
    }
    
    /**
     * 저장된 메시지 로드
     */
    private void loadStoredMessages() {
        messageRepository.loadMessagesForGroup(currentGroupId, new MessageRepository.MessageLoadCallback() {
            @Override
            public void onMessagesLoaded(List<Message> messages) {
                runOnUiThread(() -> {
                    messageList.clear();
                    messageList.addAll(messages);
                    messageAdapter.notifyDataSetChanged();
                    
                    if (!messages.isEmpty()) {
                        uiManager.scrollToBottom(messageList.size());
                    }
                    
                    // 웹소켓 연결
                    initWebSocket();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "저장된 메시지 로드 실패: " + error);
                runOnUiThread(() -> initWebSocket());
            }
        });
    }
    
    /**
     * 사용자 정보 로드
     */
    private void loadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        currentUserId = sharedPreferences.getString(KEY_USER_ID, "user_" + System.currentTimeMillis());
        currentUserName = sharedPreferences.getString(KEY_NAME, "사용자");
        currentUserEmail = sharedPreferences.getString(KEY_EMAIL, "user@example.com");

        Log.d(TAG, "사용자 정보 로드: " + currentUserEmail);
    }
    
    /**
     * 웹소켓 초기화
     */
    private void initWebSocket() {
        try {
            webSocketService = new WebSocketService();
            webSocketService.connect(currentGroupId, currentUserEmail, this);
        } catch (Exception e) {
            Log.e(TAG, "웹소켓 초기화 오류", e);
            uiManager.showToast("채팅 연결 실패");
        }
    }
    
    // ===============================
    // UICallback 구현
    // ===============================
      @Override
    public void onSendButtonClick() {
        String messageText = uiManager.getMessageText();
        if (!messageText.isEmpty()) {
            // 사용자 활동 알림 (지능형 동기화)
            networkManager.notifyUserActivity();
            messageManager.prepareTextMessage(messageText, this);
        }
    }
      @Override
    public void onAttachButtonClick() {
        // 사용자 활동 알림 (지능형 동기화)
        networkManager.notifyUserActivity();
        uiManager.showFileTypeSelectionDialog(this);
    }
    
    @Override
    public void onSummaryButtonClick() {
        // 사용자 활동 알림 (지능형 동기화)
        networkManager.notifyUserActivity();
        uiManager.showToast("채팅 요약 생성 중...");
        networkManager.requestChatSummary();
    }
    
    @Override
    public void onMenuButtonClick() {
        // 사용자 활동 알림 (지능형 동기화)
        networkManager.notifyUserActivity();
        uiManager.toggleDrawer();
        if (uiManager.isDrawerOpen()) {
            networkManager.loadGroupMembers();
        }
    }
    
    @Override
    public void onFileTypeSelected(String mimeType) {
        getContent.launch(mimeType);
    }
    
    // ===============================
    // MessageSendCallback 구현
    // ===============================
    
    @Override
    public void onMessageReady(ChatMessage message, String localId) {
        if (webSocketService != null && webSocketService.isConnected()) {
            webSocketService.sendMessage(message.toJson());
            uiManager.clearMessageText();
        } else {
            messageManager.cleanupFailedMessage(localId);
            uiManager.showToast("서버 연결이 필요합니다");
        }
    }
    
    @Override
    public void onSendFailed(String error) {
        uiManager.showToast(error);
    }
    
    // ===============================
    // MessageProcessCallback 구현
    // ===============================
    
    @Override
    public void onMessageProcessed(Message message, boolean isEcho, boolean isDuplicate) {
        if (isDuplicate) {
            return; // 중복 메시지는 무시
        }
        
        runOnUiThread(() -> {
            if (isEcho) {
                messageManager.handleEchoMessage(message);
            } else {
                boolean isMyMessage = message.getSenderId().equals(currentUserEmail);
                if (isMyMessage) {
                    messageManager.handleMyMessage(message);
                } else {
                    messageManager.handleOtherUserMessage(message);
                }
            }
            uiManager.scrollToBottom(messageList.size());
        });
    }
    
    @Override
    public void onMessageRemoved(Message message) {
        // 필요시 구현
    }
    
    // ===============================
    // FileUploadCallback 구현
    // ===============================
    
    @Override
    public void onFileReadSuccess(String fileName, byte[] fileBytes) {
        fileManager.sendFileViaWebSocket(fileName, fileBytes, webSocketService, this);
    }
    
    @Override
    public void onFileReadFailed(String error) {
        uiManager.showToast(error);
    }
    
    @Override
    public void onUploadProgress(String message) {
        uiManager.showToast(message);
    }
    
    @Override
    public void onUploadSuccess(String message) {
        uiManager.showToast(message);
    }
    
    @Override
    public void onUploadFailed(String error) {
        uiManager.showToast(error);
    }
    
    // ===============================
    // NetworkCallback 구현
    // ===============================
    
    @Override
    public void onNetworkConnected() {
        runOnUiThread(() -> {
            uiManager.showToast("네트워크 연결 복구됨");
            if (webSocketService == null || !webSocketService.isConnected()) {
                initWebSocket();
            }
        });
    }
    
    @Override
    public void onNetworkDisconnected() {
        runOnUiThread(() -> {
            uiManager.showToast("네트워크 연결이 끊어졌습니다");
        });
    }
      @Override
    public void onSyncCompleted(List<Message> newMessages) {
        runOnUiThread(() -> {
            if (!newMessages.isEmpty()) {
                int addedCount = 0;
                for (Message message : newMessages) {
                    // 동기화 전용 메시지 처리 메서드 사용
                    if (messageManager.processSyncedMessage(message, this)) {
                        addedCount++;
                    }
                }
                
                if (addedCount > 0) {
                    messageManager.sortMessagesByTimestamp();
                    messageAdapter.notifyDataSetChanged();
                    uiManager.scrollToBottom(messageList.size());
                    uiManager.showToast("새 메시지 " + addedCount + "개 동기화됨");
                    Log.d(TAG, "동기화 완료: " + addedCount + "개 메시지 추가 (총 " + newMessages.size() + "개 중)");
                } else {
                    Log.d(TAG, "동기화 완료: 새로운 메시지 없음 (총 " + newMessages.size() + "개 확인)");
                }
            }
        });
    }
    
    @Override
    public void onSyncFailed(String error) {
        runOnUiThread(() -> {
            Log.e(TAG, "동기화 실패: " + error);
        });
    }
    
    @Override
    public void onSummaryReceived(String summary) {
        runOnUiThread(() -> {
            uiManager.showSummaryDialog(summary);
        });
    }
    
    @Override
    public void onSummaryFailed(String error) {
        runOnUiThread(() -> {
            uiManager.showToast("요약 생성 실패: " + error);
        });
    }
    
    @Override
    public void onMembersLoaded(List<Object> members) {
        runOnUiThread(() -> {
            List<User> userList = new ArrayList<>();
            for (Object member : members) {
                if (member instanceof JSONObject) {
                    JSONObject userObj = (JSONObject) member;
                    String name = userObj.optString("name", "알 수 없음");
                    String email = userObj.optString("email", "");
                    String smbti = userObj.optString("smbti", "");
                    userList.add(new User(name, email, smbti));
                }
            }
            
            memberList.clear();
            memberList.addAll(userList);
            uiManager.updateMemberAdapter(memberList);
        });
    }
    
    @Override
    public void onMembersLoadFailed(String error) {
        runOnUiThread(() -> {
            uiManager.showToast("멤버 목록 로드 실패");
        });
    }
    
    // ===============================
    // WebSocketListener 구현
    // ===============================
    
    @Override
    public void onConnect() {
        runOnUiThread(() -> {
            uiManager.showToast("채팅 서버 연결됨");
        });
    }
    
    @Override
    public void onMessage(String rawJsonMessage) {
        messageManager.processReceivedMessage(rawJsonMessage, this);
    }
    
    @Override
    public void onDisconnect(int code, String reason) {
        runOnUiThread(() -> {
            if (!reason.contains("pong in time")) {
                uiManager.showToast("서버 연결 종료: " + reason);
            }
        });
    }
    
    @Override
    public void onError(Exception error) {
        runOnUiThread(() -> {
            Log.e(TAG, "WebSocket 오류", error);
            uiManager.showToast("연결 오류 발생");
        });
    }
    
    @Override
    public void onConnectionStateChanged(WebSocketService.ConnectionState state) {
        runOnUiThread(() -> {
            switch (state) {
                case CONNECTING:
                    uiManager.showToast("서버에 연결 중...");
                    break;
                case CONNECTED:
                    uiManager.showToast("서버에 연결되었습니다.");
                    break;
                case RECONNECTING:
                    uiManager.showToast("재연결 중...");
                    break;
                case FAILED:
                    uiManager.showToast("서버 연결에 실패했습니다.");
                    break;
                case DISCONNECTED:
                    break;
            }
        });
    }
    
    @Override
    public void onReconnectAttempt(int attempt, int maxAttempts) {
        runOnUiThread(() -> {
            uiManager.showToast("재연결 시도 중... (" + attempt + "/" + maxAttempts + ")");
        });
    }
    
    // ===============================
    // 파일 선택 처리
    // ===============================
    
    private void handleFileSelected(Uri fileUri) {
        if (fileUri != null) {
            fileManager.uploadFile(fileUri, this);
        }
    }
    
    // ===============================
    // 생명주기 메서드
    // ===============================
      @Override
    protected void onResume() {
        super.onResume();
        // 사용자 활동 알림
        networkManager.notifyUserActivity();
        
        // 지능형 동기화 사용 - 필요시에만 동기화
        if (networkManager.isNetworkConnected()) {
            networkManager.smartSyncMessagesFromServer();
        }
        
        if (webSocketService != null && !webSocketService.isConnected()) {
            webSocketService.connect(currentGroupId, currentUserEmail, this);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketService != null) {
            webSocketService.disconnect();
        }
        if (messageRepository != null) {
            messageRepository.cleanup();
        }
        if (networkManager != null) {
            networkManager.cleanup();
        }
    }
} 
