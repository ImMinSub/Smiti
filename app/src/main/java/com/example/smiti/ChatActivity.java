package com.example.smiti;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import com.example.smiti.repository.MessageRepository;
import com.example.smiti.adapter.MemberAdapter;
import com.example.smiti.model.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity implements WebSocketService.WebSocketListener {

    private static final String TAG = "ChatActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USER_ID = "user_id"; // 내부 고유 ID 키
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email"; // 로그인 ID (이메일) 키

    private static final String BASE_URL = "http://202.31.246.51:80";
    private static final String DEFAULT_GROUP_ID = "2"; // 기본 그룹 ID
    
    private static final int PERMISSION_REQUEST_STORAGE = 1001;

    private RecyclerView recyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton attachButton;
    private BottomNavigationView bottomNavigationView;
    private View rootView;
    
    // 사이드바 관련 변수들
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView membersRecyclerView;
    private MemberAdapter memberAdapter;
    private List<User> memberList;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private String currentUserId; // 내부 고유 ID
    private String currentUserName;
    private String currentUserEmail; // 로그인 ID (이메일), sender_id로 사용

    private WebSocketService webSocketService;
    private boolean isConnectionMessageShown = false;
    
    // 네트워크 상태 관련 변수들
    private boolean wasOffline = false;
    private BroadcastReceiver networkReceiver;

    // 파일 선택 결과를 처리하는 런처
    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadFileAndSendMessage(uri);
                }
            });
    
    // 권한 요청 결과를 처리하는 런처
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showSnackbar("저장소 접근 권한이 허용되었습니다.");
                } else {
                    showSnackbar("파일을 다운로드하려면 저장소 접근 권한이 필요합니다.");
                }
            });

    private String currentGroupId = DEFAULT_GROUP_ID;
    // 보낸 메시지의 localId를 저장하여 에코 메시지를 식별하기 위한 Set
    private Set<String> sentMessageIds = new HashSet<>();

    private MessageRepository messageRepository; // 메시지 저장소
    
    // 메시지 동기화 관련 변수들
    private android.os.Handler syncHandler;
    private Runnable syncRunnable;
    private static final long SYNC_INTERVAL = 30000; // 30초마다 동기화
    
    // 새로운 웹소켓 재연결 솔루션 관련 변수들
    private MessageSyncManager messageSyncManager;
    private ConnectionStateUIManager connectionStateUIManager;

    // 페이지네이션 매니저 추가
    private MessagePaginationManager paginationManager;
    private boolean isPaginationEnabled = false;
    
    // 대용량 데이터 처리 설정
    private static final int LARGE_CHAT_THRESHOLD = 500; // 대용량 채팅방 기준 (메시지 수)
    private static final long EXTENDED_TIMEOUT = 90000; // 확장된 타임아웃 (90초)

    // 성능 최적화된 중복 메시지 감지 (해시 기반)
    private Set<String> messageHashSet = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        try {
            // 전송된 메시지 ID 추적을 위한 LinkedHashSet 초기화 (메모리 효율성)
            sentMessageIds = new LinkedHashSet<String>() {
                @Override
                public boolean add(String e) {
                    if (size() >= 100) {
                        Iterator<String> it = iterator();
                        for (int i = 0; i < 10 && it.hasNext(); i++) {
                            it.next();
                            it.remove();
                        }
                    }
                    return super.add(e);
                }
            };

            // 메시지 저장소 초기화
            messageRepository = new MessageRepository(this);

            loadUserData(); // 사용자 정보 로드

            // 뷰 초기화
            initViews();

            // 리사이클러뷰 설정
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            messageList = new ArrayList<>();
            // 어댑터 생성 (현재 사용자 식별자로 이메일 사용)
            messageAdapter = new MessageAdapter(this, messageList, currentUserEmail);
            recyclerView.setAdapter(messageAdapter);

            // 버튼 리스너 설정
            setupListeners();

            setupBottomNavigation(); // 하단 네비게이션 설정

            // 인텐트에서 그룹 ID 가져오기
            String groupId = getIntent().getStringExtra("group_id");
            if (groupId != null && !groupId.isEmpty()) {
                currentGroupId = groupId;
            }
            
            // 페이지네이션 매니저 초기화
            initializePaginationManager();
             
            // 저장된 메시지 로드 (웹소켓 연결 전에 먼저 로드)
            loadStoredMessagesOptimized();

            // 파일 다운로드를 위한 권한 체크
            checkStoragePermission();
            
            // 네트워크 상태 감지 설정
            setupNetworkReceiver();
            
            // 정기적인 메시지 동기화 설정
            setupPeriodicSync();

        } catch (Exception e) {
            Log.e(TAG, "onCreate 오류", e);
            showUserFriendlyError("앱 초기화 중 오류가 발생했습니다", e);
        }
    }
    
    // 페이지네이션 매니저 초기화
    private void initializePaginationManager() {
        try {
            paginationManager = new MessagePaginationManager(
                this, currentGroupId, currentUserEmail, 
                recyclerView, messageAdapter, messageList
            );
            
            paginationManager.setPaginationCallback(new MessagePaginationManager.PaginationCallback() {
                @Override
                public void onLoadingStarted() {
                    runOnUiThread(() -> {
                        messageAdapter.showLoadingIndicator("이전 메시지를 불러오는 중...");
                        Log.d(TAG, "페이지네이션 로딩 시작");
                    });
                }
                
                @Override
                public void onLoadingFinished() {
                    runOnUiThread(() -> {
                        messageAdapter.hideLoadingIndicator();
                        Log.d(TAG, "페이지네이션 로딩 완료");
                    });
                }
                
                @Override
                public void onMessagesLoaded(List<Message> messages, boolean hasMore) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "페이지네이션으로 " + messages.size() + "개 메시지 로드됨, 더 있음: " + hasMore);
                        if (!messages.isEmpty()) {
                            // 페이지네이션으로 로드된 메시지들을 상단에 추가 (날짜 구분선 포함)
                            messageAdapter.addMessagesAtTop(messages);
                            showSnackbar(messages.size() + "개의 이전 메시지를 불러왔습니다");
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "페이지네이션 오류: " + error);
                        showUserFriendlyError("메시지 로딩 중 오류가 발생했습니다", new Exception(error));
                    });
                }
                
                @Override
                public void onNoMoreMessages() {
                    runOnUiThread(() -> {
                        Log.d(TAG, "더 이상 로드할 메시지가 없음");
                        showSnackbar("모든 메시지를 불러왔습니다");
                    });
                }
            });
            
            Log.d(TAG, "페이지네이션 매니저 초기화 완료");
            
        } catch (Exception e) {
            Log.e(TAG, "페이지네이션 매니저 초기화 실패", e);
            showUserFriendlyError("메시지 로딩 시스템 초기화 실패", e);
        }
    }

    // 최적화된 메시지 로드
    private void loadStoredMessagesOptimized() {
        // 먼저 메시지 수를 확인하여 대용량 채팅방인지 판단
        messageRepository.getMessageCount(currentGroupId, new MessageRepository.MessageCountCallback() {
            @Override
            public void onCountLoaded(int messageCount) {
                runOnUiThread(() -> {
                    Log.d(TAG, "그룹 " + currentGroupId + "의 메시지 수: " + messageCount);
                    
                    if (messageCount > LARGE_CHAT_THRESHOLD) {
                        // 대용량 채팅방 - 페이지네이션 사용
                        Log.d(TAG, "대용량 채팅방 감지 - 페이지네이션 모드 활성화");
                        isPaginationEnabled = true;
                        showSnackbar("대용량 채팅방입니다. 메시지를 점진적으로 불러옵니다.");
                        
                        // 페이지네이션으로 초기 메시지 로드
                        if (paginationManager != null) {
                            paginationManager.loadInitialMessages();
                        }
                    } else {
                        // 일반 채팅방 - 기존 방식 사용
                        Log.d(TAG, "일반 채팅방 - 전체 로드 모드");
                        isPaginationEnabled = false;
                        loadStoredMessages();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "메시지 수 확인 실패: " + error);
                runOnUiThread(() -> {
                    // 오류 시 기본 방식으로 로드
                    isPaginationEnabled = false;
                    loadStoredMessages();
                });
            }
        });
    }
    
    // 사용자 친화적 오류 메시지 표시
    private void showUserFriendlyError(String userMessage, Exception error) {
        String detailedMessage = userMessage;
        String solution = "";
        
        if (error != null) {
            String errorMsg = error.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("timeout") || errorMsg.contains("시간 초과")) {
                    solution = "네트워크 연결을 확인하고 잠시 후 다시 시도해주세요.";
                } else if (errorMsg.contains("network") || errorMsg.contains("네트워크")) {
                    solution = "인터넷 연결을 확인해주세요.";
                } else if (errorMsg.contains("server") || errorMsg.contains("서버")) {
                    solution = "서버에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.";
                } else if (errorMsg.contains("memory") || errorMsg.contains("메모리")) {
                    solution = "메모리 부족입니다. 다른 앱을 종료하고 다시 시도해주세요.";
                } else {
                    solution = "앱을 재시작하거나 잠시 후 다시 시도해주세요.";
                }
            }
        }
        
        String fullMessage = detailedMessage;
        if (!solution.isEmpty()) {
            fullMessage += "\n\n해결 방법: " + solution;
        }
        
        // 토스트 대신 스낵바로 더 자세한 정보 제공
        showSnackbar(fullMessage);
        Log.e(TAG, userMessage, error);
    }

    // 저장된 메시지 로드
    private void loadStoredMessages() {
        messageRepository.loadMessagesForGroup(currentGroupId, new MessageRepository.MessageLoadCallback() {
            @Override
            public void onMessagesLoaded(List<Message> messages) {
                runOnUiThread(() -> {
                    messageList.clear();
                    messageList.addAll(messages);
                    messageAdapter.notifyDataSetChanged();
                    
                    if (!messages.isEmpty()) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                        Log.d(TAG, "저장된 메시지 " + messages.size() + "개 로드됨");
                    }
                    
                    // 저장된 메시지 로드 완료 후 서버에서 최신 메시지 동기화
                    syncMessagesFromServer();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "저장된 메시지 로드 실패: " + error);
                // 메시지 로드 실패해도 동기화 시도
                runOnUiThread(() -> syncMessagesFromServer());
            }
        });
    }
    
    // 저장소 접근 권한 체크
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 이상에서는 스코프드 스토리지로 인해 Downloads 폴더 접근 시 권한 필요 없음
            return;
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            
            // 사용자에게 권한이 필요한 이유 설명
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, 
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle("저장소 접근 권한 필요")
                        .setMessage("파일을 다운로드하려면 저장소 접근 권한이 필요합니다.")
                        .setPositiveButton("확인", (dialog, which) -> {
                            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        })
                        .setNegativeButton("취소", null)
                        .show();
            } else {
                // 권한 요청
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackbar("저장소 접근 권한이 허용되었습니다.");
            } else {
                showSnackbar("파일을 다운로드하려면 저장소 접근 권한이 필요합니다.");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 앱이 다시 활성화될 때 메시지 동기화 및 웹소켓 연결 확인
        try {
            // 네트워크가 연결되어 있으면 누락된 메시지 동기화 수행
            if (isNetworkConnected()) {
                // 앱 재시작 시 누락된 메시지가 있을 수 있으므로 강제 동기화
                forceSyncMessages();
            }
            
            // 웹소켓 연결 확인 및 재연결 시도
            if (webSocketService != null && !webSocketService.isConnected()) {
                webSocketService.connect(currentGroupId, currentUserEmail, this);
            }
        } catch (Exception e) {
            // 로그 출력 (컴파일 오류 방지를 위해 System.out 사용)
            System.out.println("앱 재시작 시 오류: " + e.getMessage());
            showSnackbar("채팅 재연결 실패");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketService != null) {
            webSocketService.disconnect();
        }
        // 메시지 저장소 정리 (추가)
        if (messageRepository != null) {
            messageRepository.cleanup();
        }
        // 네트워크 리시버 해제
        if (networkReceiver != null) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (IllegalArgumentException e) {
                // 이미 해제된 경우 무시
            }
        }
        // 정기적 동기화 중지
        stopPeriodicSync();
    }

    // 웹소켓 서비스 초기화 및 연결 시작
    private void initWebSocket() {
        try {
            webSocketService = new WebSocketService();
            webSocketService.connect(currentGroupId, currentUserEmail, this);
        } catch (Exception e) {
            Log.e(TAG, "웹소켓 초기화 오류", e);
            Toast.makeText(this, "채팅 연결 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 하단 네비게이션 뷰 설정
    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_chat);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(ChatActivity.this, MainActivity.class));
                finish(); return true;
            } else if (id == R.id.navigation_search) {
                startActivity(new Intent(ChatActivity.this, GroupSearchActivity.class));
                finish(); return true;
            } else if (id == R.id.navigation_chat) {
                return true; // 현재 화면이므로 아무 작업 안함
            } else if (id == R.id.navigation_board) {
                startActivity(new Intent(ChatActivity.this, BoardActivity.class));
                finish(); return true;
            } else if (id == R.id.navigation_profile) {
                startActivity(new Intent(ChatActivity.this, ProfileActivity.class));
                finish(); return true;
            }
            return false;
        });
    }

    // SharedPreferences에서 사용자 정보 로드
    private void loadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        currentUserId = sharedPreferences.getString(KEY_USER_ID, "user_" + System.currentTimeMillis());
        currentUserName = sharedPreferences.getString(KEY_NAME, "사용자");
        currentUserEmail = sharedPreferences.getString(KEY_EMAIL, "user@example.com"); // 이메일 로드

        Log.d(TAG, "사용자 정보 로드: id=" + currentUserId + ", name=" + currentUserName + ", email=" + currentUserEmail);

        // 어댑터에 사용자 이메일 업데이트 (어댑터가 생성된 후 호출)
        if (messageAdapter != null) {
            messageAdapter.updateCurrentUserIdentifier(currentUserEmail);
        }

        // API를 통해 최신 사용자 정보 가져오기 (선택 사항)
        if (currentUserEmail != null && !currentUserEmail.isEmpty()) {
            fetchUserInfo(currentUserEmail);
        }
    }

    // API 서버에서 사용자 정보(주로 이름)를 가져오는 메서드
    private void fetchUserInfo(String email) {
        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "/users/me?email=" + email;
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "사용자 정보 가져오기 실패", e);
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String updatedUserName = currentUserName; // 기본값은 현재 이름 유지

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        String apiUserName = jsonObject.optString("name", "");
                        if (!apiUserName.isEmpty()) {
                            updatedUserName = apiUserName; // API에서 받은 이름으로 업데이트
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "사용자 정보 파싱 오류", e);
                    }
                } else {
                    Log.e(TAG, "사용자 정보 가져오기 실패: " + response.code());
                }

                // 최종적으로 사용자 이름 업데이트
                currentUserName = updatedUserName;
                Log.d(TAG, "API 사용자 정보 업데이트 후: name=" + currentUserName);
            }
        });
    }

    // 텍스트 메시지 전송 처리
    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();

        if (!messageText.isEmpty()) {
            String localId = UUID.randomUUID().toString(); // 메시지 구별을 위한 로컬 ID 생성

            // ChatMessage 객체 생성 (서버 시간 사용하므로 timestamp는 설정 안 함)
            ChatMessage chatMessage = new ChatMessage(currentUserEmail, currentUserName, messageText);
            chatMessage.setGroupId(currentGroupId);
            chatMessage.setLocalId(localId); // 로컬 ID 설정

            Log.d(TAG, "메시지 발신 준비: 내용=" + messageText +
                    ", 발신자 ID(Email)=[" + currentUserEmail + "]" +
                    ", localId=" + localId);

            sentMessageIds.add(localId); // 에코 메시지 확인을 위해 ID 저장

            if (webSocketService != null && webSocketService.isConnected()) {
                Log.d(TAG, "메시지 전송 (서버 시간 사용 예정): " + messageText + ", 발신자 ID: " + currentUserEmail);
                webSocketService.sendMessage(chatMessage.toJson()); // JSON 메시지 전송
                messageEditText.setText(""); // 입력창 비우기
                // UI 즉시 업데이트는 제거됨. 서버에서 에코 메시지를 받으면 그때 UI 업데이트.
            } else {
                showSnackbar("서버 연결 안됨");
                sentMessageIds.remove(localId); // 전송 실패 시 저장한 ID 제거
            }
        }
    }

    // 파일(이미지) 업로드 및 메시지 전송 처리
    private void uploadFileAndSendMessage(Uri fileUri) {
        // 파일 업로드를 위한 OkHttpClient 생성
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    
        // 파일 업로드 요청 생성
        String uploadUrl = BASE_URL + "/posts"; // 게시글 업로드 엔드포인트 활용

        try {
            // 파일 타입 가져오기
            String mimeType = getContentResolver().getType(fileUri);
            String fileType = "image"; // 기본값
            String tempFilename = "uploaded_file"; // 기본 파일명
            
            // 파일의 MIME 타입에 따라 처리
            if (mimeType != null) {
                if (mimeType.startsWith("image/")) {
                    fileType = "image";
                    tempFilename = "uploaded_image." + getMimeExtension(mimeType);
                } else if (mimeType.equals("application/pdf")) {
                    fileType = "pdf";
                    tempFilename = "uploaded_document.pdf";
                } else {
                    fileType = "file";
                    tempFilename = "uploaded_file";
                }
            }
            
            // 파일명 가져오기 시도
            String displayName = getFileDisplayName(fileUri);
            if (displayName != null && !displayName.isEmpty()) {
                tempFilename = displayName;
            }
            
            final String filename = tempFilename; // final 변수로 선언
            final String finalFileType = fileType;
            
            // ContentResolver를 사용하여 Uri로부터 InputStream 열기
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                Log.e(TAG, "Uri로부터 InputStream을 열 수 없습니다: " + fileUri);
                runOnUiThread(() -> showSnackbar("파일을 열 수 없습니다."));
                return;
            }

            // InputStream을 byte[] 배열로 변환
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] fileBytes = byteBuffer.toByteArray();

            // byte[] 배열로부터 RequestBody 생성
            RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType != null ? mimeType : "application/octet-stream"), fileBytes);
            
            // 게시글 업로드 엔드포인트와 동일한 형식으로 multipart 요청 생성
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("email", currentUserEmail != null ? currentUserEmail : "")
                    .addFormDataPart("board_type", "chat")
                    .addFormDataPart("title", "Chat File")
                    .addFormDataPart("content", "File uploaded from chat")
                    .addFormDataPart("file", filename, fileBody)
                    .build();
    
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build();
    
            // 파일 업로드 요청 실행
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "파일 업로드 실패", e);
                    runOnUiThread(() -> showSnackbar("파일 업로드 실패"));
                    closeStream(inputStream); // 스트림 닫기
                }
    
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            
                            // 게시글 업로드 응답에서 파일 정보 추출
                            String fileUrl = "";
                            if (jsonResponse.has("data")) {
                                JSONObject data = jsonResponse.getJSONObject("data");
                                if (data.has("file_url")) {
                                    fileUrl = data.getString("file_url");
                                } else if (data.has("fileUrl")) {
                                    fileUrl = data.getString("fileUrl");
                                }
                            }
                            
                            // 파일 URL이 없으면 기본 경로 생성 (파일명 기반)
                            if (fileUrl.isEmpty()) {
                                fileUrl = BASE_URL + "/board-uploads/" + filename;
                            }
                            
                            // 상대 경로를 절대 경로로 변환
                            if (fileUrl.startsWith("/")) {
                                fileUrl = BASE_URL + fileUrl;
                            }
    
                            if (!fileUrl.isEmpty()) {
                                // 파일 URL을 사용해 메시지 전송
                                sendMessageWithFileUrl(fileUrl, finalFileType);
                            } else {
                                Log.e(TAG, "서버 응답에 URL이 없음: " + responseData);
                                runOnUiThread(() -> showSnackbar("파일 URL을 가져오지 못했습니다."));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "파일 업로드 응답 파싱 오류", e);
                            runOnUiThread(() -> showSnackbar("서버 응답 처리 중 오류가 발생했습니다."));
                        }
                    } else {
                        String errorBody = "";
                        try {
                            if (response.body() != null) {
                                errorBody = response.body().string();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "에러 응답 읽기 실패", e);
                        }
                        
                        Log.e(TAG, "파일 업로드 서버 오류: " + response.code() + ", 응답: " + errorBody);
                        
                        final String errorMessage;
                        switch (response.code()) {
                            case 404:
                                errorMessage = "파일 업로드 경로를 찾을 수 없습니다 (404)";
                                break;
                            case 405:
                                errorMessage = "허용되지 않은 요청 방식입니다 (405)";
                                break;
                            case 413:
                                errorMessage = "파일이 너무 큽니다 (413)";
                                break;
                            case 401:
                            case 403:
                                errorMessage = "파일 업로드 권한이 없습니다 (" + response.code() + ")";
                                break;
                            case 500:
                            case 502:
                            case 503:
                                errorMessage = "서버 내부 오류 (" + response.code() + ")";
                                break;
                            default:
                                errorMessage = "서버 오류로 파일 업로드 실패 (" + response.code() + ")";
                        }
                        
                        runOnUiThread(() -> showSnackbar(errorMessage));
                    }
                    closeStream(inputStream); // 스트림 닫기
                }
            });

        } catch (FileNotFoundException e) {
            Log.e(TAG, "파일을 찾을 수 없습니다: " + fileUri, e);
            runOnUiThread(() -> showSnackbar("파일을 찾을 수 없습니다."));
        } catch (IOException e) {
            Log.e(TAG, "파일 읽기 오류: " + fileUri, e);
            runOnUiThread(() -> showSnackbar("파일 읽기 중 오류 발생"));
        }
    }

    // MIME 타입에서 확장자 추출
    private String getMimeExtension(String mimeType) {
        switch (mimeType) {
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            default:
                return "dat";
        }
    }
    
    // 파일명 가져오기
    private String getFileDisplayName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "파일명 가져오기 실패", e);
            }
        }
        return result;
    }

    // InputStream을 안전하게 닫는 유틸리티 메서드
    private void closeStream(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(TAG, "InputStream 닫기 오류", e);
            }
        }
    }
    
    // 파일 URL을 사용해 메시지 생성 및 전송
    private void sendMessageWithFileUrl(String fileUrl, String fileType) {
        // 파일 전송 시 적절한 메시지 텍스트 설정
        String messageText = "파일을 전송했습니다.";
        
        String localId = UUID.randomUUID().toString();
    
        ChatMessage chatMessage = new ChatMessage(currentUserEmail, currentUserName, messageText, fileUrl, fileType);
        chatMessage.setGroupId(currentGroupId);
        chatMessage.setLocalId(localId);
    
        sentMessageIds.add(localId);
    
        if (webSocketService != null && webSocketService.isConnected()) {
            webSocketService.sendMessage(chatMessage.toJson());
            Log.d(TAG, "파일 메시지 전송: fileUrl=" + fileUrl + ", fileType=" + fileType + ", localId=" + localId + ", message=" + messageText);
        } else {
            showSnackbar("서버 연결 안됨");
            sentMessageIds.remove(localId);
        }
    }

    // WebSocketListener 인터페이스 구현: 연결 성공 시 호출
    @Override
    public void onConnect() {
        runOnUiThread(() -> {
            if (!isConnectionMessageShown) {
                showSnackbar("채팅 서버 연결됨");
                isConnectionMessageShown = true;
            }
        });
    }

    // WebSocketListener 인터페이스 구현: 메시지 수신 시 호출 - 스레드 안전 버전
    @Override
    public void onMessage(String rawJsonMessage) {
        try {
            ChatMessage chatMessage = ChatMessage.fromJson(rawJsonMessage);
            Log.d(TAG, "메시지 수신: SenderId=[" + chatMessage.getSenderId() +
                    "], type=[" + chatMessage.getType() +
                    "], fileUrl=[" + chatMessage.getFileUrl() + "]");

            String receivedLocalId = chatMessage.getLocalId();
            final boolean isEchoMessage; // final로 선언
            
            // sentMessageIds 접근을 스레드 안전하게 처리
            synchronized (sentMessageIds) {
                isEchoMessage = receivedLocalId != null && !receivedLocalId.isEmpty() && 
                               sentMessageIds.contains(receivedLocalId);
                
                if (isEchoMessage) {
                    Log.d(TAG, "내가 보낸 메시지(Echo) 감지 - LocalId: " + receivedLocalId);
                    sentMessageIds.remove(receivedLocalId);
                }
            }

            runOnUiThread(() -> {
                Message uiMessage = chatMessage.toUIMessage();
                
                // 메시지 소유권 정확히 판단
                boolean isMyMessage = isCurrentUserMessage(uiMessage.getSenderId(), uiMessage.getSenderName());
                
                Log.d(TAG, "=== 메시지 처리 상세 정보 ===");
                Log.d(TAG, "수신된 메시지 발신자 ID: [" + uiMessage.getSenderId() + "]");
                Log.d(TAG, "수신된 메시지 발신자 이름: [" + uiMessage.getSenderName() + "]");
                Log.d(TAG, "현재 사용자 이메일: [" + currentUserEmail + "]");
                Log.d(TAG, "Echo 메시지 여부: " + isEchoMessage);
                Log.d(TAG, "내 메시지 판단 결과: " + isMyMessage);
                Log.d(TAG, "LocalId: " + receivedLocalId);
                
                // Echo 메시지이거나 내가 보낸 메시지인 경우 발신자 정보 정규화
                final Message finalUiMessage; // final 변수로 선언
                if (isEchoMessage || isMyMessage) {
                    // 내 메시지로 정규화
                    finalUiMessage = new Message(currentUserEmail, currentUserName != null ? currentUserName : "나", 
                                          uiMessage.getMessage(), uiMessage.getTimestamp());
                    if (chatMessage.getFileUrl() != null && !chatMessage.getFileUrl().isEmpty()) {
                        finalUiMessage.setFileUrl(chatMessage.getFileUrl());
                        finalUiMessage.setFileType(chatMessage.getFileType());
                    }
                    Log.d(TAG, "내 메시지로 정규화 완료");
                } else {
                    finalUiMessage = uiMessage;
                }
                
                // 강화된 중복 메시지 체크 - Echo 메시지는 항상 추가하되, 기존 메시지와 중복되면 제거
                if (isEchoMessage) {
                    // Echo 메시지인 경우: 기존에 동일한 메시지가 있으면 제거하고 새로 추가
                    synchronized (messageList) {
                        // 기존 메시지 중 동일한 내용의 메시지 제거
                        messageList.removeIf(existingMessage -> 
                            isSameMessageContent(existingMessage, uiMessage) && 
                            Math.abs(existingMessage.getTimestamp() - uiMessage.getTimestamp()) < 10000); // 10초 이내
                    }
                    
                    // Echo 메시지 추가
                    messageAdapter.addMessage(uiMessage);
                    synchronized (messageList) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                    messageRepository.saveMessage(currentGroupId, uiMessage);
                    Log.d(TAG, "Echo 메시지 처리 완료: " + uiMessage.getMessage().substring(0, Math.min(20, uiMessage.getMessage().length())));
                    
                } else if (!isDuplicateMessageEnhanced(uiMessage)) {
                    // 일반 메시지: 중복이 아닌 경우에만 추가
                    messageAdapter.addMessage(uiMessage);
                    
                    synchronized (messageList) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                    
                    messageRepository.saveMessage(currentGroupId, uiMessage);
                    Log.d(TAG, "새 메시지 추가됨: 수신 - " + uiMessage.getMessage().substring(0, Math.min(20, uiMessage.getMessage().length())));
                } else {
                    Log.d(TAG, "중복 메시지 무시됨: " + uiMessage.getMessage().substring(0, Math.min(20, uiMessage.getMessage().length())));
                }
                
                Log.d(TAG, "========================");
            });

        } catch (JSONException e) {
            Log.e(TAG, "메시지 파싱 오류", e);
        } catch (Exception e) {
            Log.e(TAG, "메시지 처리 중 오류", e);
        }
    }
    
    // 메시지 내용이 동일한지 확인하는 메서드 추가
    private boolean isSameMessageContent(Message msg1, Message msg2) {
        if (msg1 == null || msg2 == null) {
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
        
        // 파일 URL 비교 (있는 경우)
        String fileUrl1 = msg1.getFileUrl();
        String fileUrl2 = msg2.getFileUrl();
        if (fileUrl1 != null) fileUrl1 = fileUrl1.trim();
        if (fileUrl2 != null) fileUrl2 = fileUrl2.trim();
        
        return java.util.Objects.equals(fileUrl1, fileUrl2);
    }

    // WebSocketListener 인터페이스 구현: 연결 종료 시 호출
    @Override
    public void onDisconnect(int code, String reason) {
        runOnUiThread(() -> {
            // ping/pong 타임아웃으로 인한 연결 끊김은 사용자에게 알리지 않음
            if (!reason.contains("pong in time")) {
                showSnackbar("서버 연결 종료: " + reason);
            } else {
                Log.d(TAG, "웹소켓 ping/pong 타임아웃으로 연결 종료됨 (자동 재연결 시도 중)");
            }
            isConnectionMessageShown = false;
        });
    }

    // WebSocketListener 인터페이스 구현: 오류 발생 시 호출
    @Override
    public void onError(Exception error) {
        runOnUiThread(() -> {
            Log.e(TAG, "WebSocket 오류", error);
            showSnackbar("연결 오류 발생");
        });
    }

    // WebSocketListener 인터페이스 구현: 연결 상태 변경 시 호출
    @Override
    public void onConnectionStateChanged(WebSocketService.ConnectionState state) {
        runOnUiThread(() -> {
            Log.d(TAG, "연결 상태 변경: " + state);
            switch (state) {
                case CONNECTING:
                    showSnackbar("서버에 연결 중...");
                    break;
                case CONNECTED:
                    showSnackbar("서버에 연결되었습니다.");
                    break;
                case RECONNECTING:
                    showSnackbar("연결이 끊어졌습니다. 재연결 중...");
                    break;
                case FAILED:
                    showSnackbar("서버 연결에 실패했습니다.");
                    break;
                case DISCONNECTED:
                    // 의도적 연결 해제는 메시지 표시하지 않음
                    break;
            }
        });
    }

    // WebSocketListener 인터페이스 구현: 재연결 시도 시 호출
    @Override
    public void onReconnectAttempt(int attempt, int maxAttempts) {
        runOnUiThread(() -> {
            Log.d(TAG, "재연결 시도: " + attempt + "/" + maxAttempts);
            showSnackbar("재연결 시도 중... (" + attempt + "/" + maxAttempts + ")");
        });
    }

    // 화면 하단에 Snackbar 메시지 표시
    private void showSnackbar(String message) {
        try {
            // 복잡한 Snackbar 대신 간단한 Toast 사용
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Toast 표시 중 오류", e);
        }
    }

    // 서버에 채팅 내용 요약 요청
    private void requestChatSummary() {
        showSnackbar("채팅 요약 생성 중...");
        // 타임아웃 설정된 OkHttpClient 사용
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS) // 요약 생성 시간을 고려하여 길게 설정
                .build();

        // 요청 본문 생성 (그룹 ID 포함)
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("group_id", currentGroupId);
        } catch (JSONException e) {
            Log.e(TAG, "요약 요청 JSON 생성 오류", e);
            showSnackbar("요약 요청 생성 중 오류 발생");
            return;
        }

        RequestBody requestBody = RequestBody.create(
                jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));

        // POST 요청 생성
        Request request = new Request.Builder()
                .url(BASE_URL + "/chat/summary")
                .post(requestBody)
                .build();

        // 비동기 요청 실행
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "채팅 요약 요청 실패", e);
                runOnUiThread(() -> showSnackbar("네트워크 오류로 요약 실패"));
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        final String summary = jsonObject.optString("summary", "요약을 생성할 수 없습니다.");
                        // 성공 시 요약 내용을 다이얼로그로 표시
                        runOnUiThread(() -> showSummaryDialog(summary));
                    } catch (JSONException e) {
                        Log.e(TAG, "요약 응답 파싱 오류", e);
                        runOnUiThread(() -> showSnackbar("요약 응답 처리 오류"));
                    }
                } else {
                    Log.e(TAG, "채팅 요약 서버 오류: " + response.code());
                    runOnUiThread(() -> showSnackbar("서버 오류로 요약 실패 (" + response.code() + ")"));
                }
            }
        });
    }

    // 요약 결과를 보여주는 다이얼로그 표시
    private void showSummaryDialog(String summary) {
        new AlertDialog.Builder(this)
                .setTitle("대화 요약")
                .setMessage(summary)
                .setPositiveButton("확인", null)
                .show();
    }

    private void initViews() {
        rootView = findViewById(android.R.id.content);
        recyclerView = findViewById(R.id.recyclerView);
        messageEditText = findViewById(R.id.edit_message);
        sendButton = findViewById(R.id.send_button);
        attachButton = findViewById(R.id.attach_button);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // 사이드바 관련 뷰 초기화
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        membersRecyclerView = findViewById(R.id.members_recycler_view);
        
        // 멤버 목록 초기화
        memberList = new ArrayList<>();
        memberAdapter = new MemberAdapter(memberList);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        membersRecyclerView.setAdapter(memberAdapter);
    }
    
    private void setupListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
        attachButton.setOnClickListener(v -> showFileTypeSelectionDialog());
        
        ImageButton summaryButton = findViewById(R.id.summary_button);
        if (summaryButton != null) {
            summaryButton.setOnClickListener(v -> requestChatSummary());
        }
        
        // 햄버거 메뉴 버튼 리스너
        ImageButton menuButton = findViewById(R.id.menu_button);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> {
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawer(navigationView);
                } else {
                    drawerLayout.openDrawer(navigationView);
                    // 사이드바가 열릴 때 멤버 목록 로드
                    loadGroupMembers();
                }
            });
        }
    }

    // 상단 메뉴에 파일 타입 선택 옵션 추가
    private void showFileTypeSelectionDialog() {
        final CharSequence[] items = {"이미지", "PDF 문서", "모든 파일"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("업로드할 파일 유형 선택");
        builder.setItems(items, (dialog, which) -> {
            switch(which) {
                case 0: // 이미지
                    getContent.launch("image/*");
                    break;
                case 1: // PDF
                    getContent.launch("application/pdf");
                    break;
                case 2: // 모든 파일
                    getContent.launch("*/*");
                    break;
            }
        });
        builder.show();
    }
    
    // 서버에서 최신 메시지 동기화 (웹소켓 연결과 독립적으로 실행) - 스레드 안전 버전
    private void syncMessagesFromServer() {
        // 페이지네이션 모드에서는 별도 동기화 로직 사용
        if (isPaginationEnabled && paginationManager != null) {
            Log.d(TAG, "페이지네이션 모드에서는 실시간 동기화만 수행");
            // 웹소켓이 연결되지 않았다면 연결 시도
            if (webSocketService == null || !webSocketService.isConnected()) {
                initWebSocket();
            }
            return;
        }
        
        // 기존 동기화 로직 (일반 모드)
        if (messageSyncManager == null) {
            messageSyncManager = new MessageSyncManager();
        }
        
        String lastTimestamp = getLastMessageTimestamp();
        Log.d(TAG, "서버에서 메시지 동기화 시작 - 마지막 타임스탬프: " + lastTimestamp);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(EXTENDED_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(EXTENDED_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
                .writeTimeout(EXTENDED_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
        
        String url = BASE_URL + "/chat/" + currentGroupId + "/history";
        if (lastTimestamp != null && !lastTimestamp.isEmpty()) {
            url += "?after=" + lastTimestamp;
        }
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "메시지 동기화 서버 오류", e);
                runOnUiThread(() -> {
                    showUserFriendlyError("메시지 동기화 실패", e);
                    // 첫 로드 시에만 웹소켓 연결
                    synchronized (messageList) {
                        if (messageList.isEmpty()) {
                            initWebSocket();
                        }
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        Log.d(TAG, "서버 응답 받음: " + responseData.substring(0, Math.min(200, responseData.length())));
                        
                        // 서버 응답이 직접 JSONArray인 경우 처리
                        JSONArray messagesArray = new JSONArray(responseData);
                        List<Message> newMessages = new ArrayList<>();
                        
                        for (int i = 0; i < messagesArray.length(); i++) {
                            JSONObject messageObject = messagesArray.getJSONObject(i);
                            Message message = parseMessageFromJson(messageObject);
                            
                            if (message != null && !isDuplicateMessageEnhanced(message)) {
                                newMessages.add(message);
                            }
                        }
                        
                        if (!newMessages.isEmpty()) {
                            runOnUiThread(() -> {
                                // 새 메시지들을 리스트에 추가 (스레드 안전)
                                synchronized (messageList) {
                                    for (Message message : newMessages) {
                                        messageList.add(message);
                                        // 데이터베이스에도 저장
                                        messageRepository.saveMessage(currentGroupId, message);
                                    }
                                    
                                    // 메시지 목록을 시간순으로 정렬
                                    sortMessagesByTimestamp();
                                }
                                
                                messageAdapter.notifyDataSetChanged();
                                recyclerView.scrollToPosition(messageList.size() - 1);
                                
                                showSnackbar("새 메시지 " + newMessages.size() + "개 동기화됨");
                            });
                        } else {
                            Log.d(TAG, "동기화할 새 메시지가 없음");
                            // 첫 로드 시에만 웹소켓 연결
                            synchronized (messageList) {
                                if (messageList.isEmpty()) {
                                    runOnUiThread(() -> initWebSocket());
                                }
                            }
                        }
                        
                    } else {
                        Log.e(TAG, "메시지 동기화 서버 오류: " + response.code());
                        if (response.code() != 404) {
                            runOnUiThread(() -> showUserFriendlyError("서버 연결 오류 (코드: " + response.code() + ")", 
                                    new Exception("HTTP " + response.code())));
                        }
                        
                        // 첫 로드 시에만 웹소켓 연결
                        synchronized (messageList) {
                            if (messageList.isEmpty()) {
                                runOnUiThread(() -> initWebSocket());
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "메시지 동기화 응답 파싱 오류", e);
                    runOnUiThread(() -> {
                        showUserFriendlyError("메시지 데이터 처리 오류", e);
                        // 첫 로드 시에만 웹소켓 연결
                        synchronized (messageList) {
                            if (messageList.isEmpty()) {
                                initWebSocket();
                            }
                        }
                    });
                }
            }
        });
    }
    
    // 마지막 메시지의 타임스탬프 가져오기
    private String getLastMessageTimestamp() {
        if (messageList != null && !messageList.isEmpty()) {
            Message lastMessage = messageList.get(messageList.size() - 1);
            return String.valueOf(lastMessage.getTimestamp());
        }
        return null;
    }
    
    // JSON에서 Message 객체로 파싱
    private Message parseMessageFromJson(JSONObject messageObject) {
        try {
            // sender_id 처리 - 이메일 형태로 정규화
            String senderId = messageObject.optString("sender_id", "");
            if (senderId.isEmpty()) {
                senderId = messageObject.optString("sender_name", "unknown");
            }
            
            // sender_name 처리
            String senderName = messageObject.optString("sender_name", null);
            if (senderName == null || senderName.equals("null") || senderName.isEmpty()) {
                senderName = "알 수 없음";
            }
            
            // 메시지 내용 처리
            String content = messageObject.optString("content", "");
            if (content.isEmpty()) {
                content = messageObject.optString("message", "");
            }
            
            // 타임스탬프 처리 - 서버에서 받은 정확한 시간 사용
            String timestampStr = messageObject.optString("timestamp", "");
            long timestamp = parseTimestampToMillisFixed(timestampStr);
            
            // 파일 정보 처리
            String fileUrl = messageObject.optString("file_url", "");
            String fileType = messageObject.optString("file_type", "");
            
            // 현재 사용자 메시지인지 정확히 판단
            boolean isCurrentUser = isCurrentUserMessage(senderId, senderName);
            
            // 현재 사용자 메시지인 경우 발신자 정보 정규화
            if (isCurrentUser) {
                senderId = currentUserEmail; // 현재 사용자 이메일로 통일
                senderName = currentUserName != null ? currentUserName : "나"; // 현재 사용자 이름 사용
            }
            
            Message message = new Message(senderId, senderName, content, timestamp);
            if (!fileUrl.isEmpty()) {
                message.setFileUrl(fileUrl);
                message.setFileType(fileType);
            }
            
            Log.d(TAG, "메시지 파싱: senderId=" + senderId + 
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
            Log.w(TAG, "현재 사용자 이메일이 설정되지 않음");
            return false;
        }
        
        // 디버깅을 위한 상세 로그
        Log.d(TAG, "=== 메시지 소유권 판단 ===");
        Log.d(TAG, "현재 사용자 이메일: [" + currentUserEmail + "]");
        Log.d(TAG, "메시지 발신자 ID: [" + senderId + "]");
        Log.d(TAG, "메시지 발신자 이름: [" + senderName + "]");
        Log.d(TAG, "그룹 ID: [" + currentGroupId + "]");
        
        // 1. 이메일 정확 매칭 (대소문자 무시)
        if (senderId != null && currentUserEmail.equalsIgnoreCase(senderId.trim())) {
            Log.d(TAG, "✓ 이메일 정확 매칭으로 현재 사용자 메시지 확인됨");
            return true;
        }
        
        // 2. 이메일 앞부분 매칭 (@ 앞부분)
        if (senderId != null && currentUserEmail.contains("@")) {
            String emailPrefix = currentUserEmail.split("@")[0];
            Log.d(TAG, "이메일 앞부분 비교: [" + emailPrefix + "] vs [" + senderId.trim() + "]");
            if (emailPrefix.equalsIgnoreCase(senderId.trim())) {
                Log.d(TAG, "✓ 이메일 앞부분 매칭으로 현재 사용자 메시지 확인됨");
                return true;
            }
        }
        
        // 3. 발신자명으로 판단
        if (senderName != null) {
            String normalizedName = senderName.trim().toLowerCase();
            Log.d(TAG, "발신자명 정규화: [" + normalizedName + "]");
            
            // "나" 또는 "me" 키워드 확인
            if (normalizedName.equals("나") || normalizedName.equals("me") || normalizedName.equals("myself")) {
                Log.d(TAG, "✓ 발신자명 키워드로 현재 사용자 메시지 확인됨");
                return true;
            }
            
            // 현재 사용자 이메일의 앞부분과 비교
            if (currentUserEmail.contains("@")) {
                String emailPrefix = currentUserEmail.split("@")[0];
                Log.d(TAG, "발신자명과 이메일 앞부분 비교: [" + emailPrefix + "] vs [" + senderName.trim() + "]");
                if (emailPrefix.equalsIgnoreCase(senderName.trim())) {
                    Log.d(TAG, "✓ 발신자명과 이메일 앞부분 매칭으로 현재 사용자 메시지 확인됨");
                    return true;
                }
            }
        }
        
        // 4. 추가 매칭 로직 (더 관대한 방식)
        if (senderId != null && currentUserEmail != null) {
            // 공백 제거 후 부분 문자열 매칭
            String cleanSenderId = senderId.replaceAll("\\s+", "").toLowerCase();
            String cleanCurrentEmail = currentUserEmail.replaceAll("\\s+", "").toLowerCase();
            
            Log.d(TAG, "공백 제거 후 비교: [" + cleanCurrentEmail + "] contains [" + cleanSenderId + "]");
            
            if (cleanCurrentEmail.contains(cleanSenderId) || cleanSenderId.contains(cleanCurrentEmail)) {
                Log.d(TAG, "✓ 부분 문자열 매칭으로 현재 사용자 메시지 확인됨");
                return true;
            }
        }
        
        // 5. 사용자 이름과 이메일 교차 매칭
        if (currentUserName != null && senderId != null) {
            Log.d(TAG, "사용자 이름과 발신자 ID 교차 비교: [" + currentUserName + "] vs [" + senderId + "]");
            if (currentUserName.trim().equalsIgnoreCase(senderId.trim())) {
                Log.d(TAG, "✓ 사용자 이름과 발신자 ID 매칭으로 현재 사용자 메시지 확인됨");
                return true;
            }
        }
        
        Log.d(TAG, "✗ 현재 사용자 메시지가 아님으로 판단됨");
        Log.d(TAG, "========================");
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
    
    // 강화된 중복 메시지 감지 (정확도 향상) - 스레드 안전 버전
    private boolean isDuplicateMessageEnhanced(Message newMessage) {
        String newMessageHash = generateMessageHashEnhanced(newMessage);
        
        // 1. 해시 기반 빠른 체크 (스레드 안전)
        synchronized (messageHashSet) {
            if (messageHashSet.contains(newMessageHash)) {
                Log.d(TAG, "해시 기반 중복 메시지 감지: " + newMessageHash);
                return true;
            }
        }
        
        // 2. 기존 메시지 리스트와 정확한 비교 (스레드 안전)
        synchronized (messageList) {
            // 리스트 복사본을 만들어서 안전하게 순회
            List<Message> messageListCopy = new ArrayList<>(messageList);
            for (Message existingMessage : messageListCopy) {
                if (isSameMessageExact(newMessage, existingMessage)) {
                    Log.d(TAG, "정확한 비교로 중복 메시지 감지");
                    return true;
                }
                
                // 추가: 내용과 시간이 비슷한 메시지 체크 (중복 방지 강화)
                if (isSimilarMessage(newMessage, existingMessage)) {
                    Log.d(TAG, "유사 메시지로 중복 감지: 새 메시지=[" + newMessage.getSenderId() + 
                              "], 기존 메시지=[" + existingMessage.getSenderId() + "]");
                    return true;
                }
            }
        }
        
        // 3. 새 메시지 해시 추가 (스레드 안전)
        synchronized (messageHashSet) {
            messageHashSet.add(newMessageHash);
            
            // 4. 메모리 관리
            if (messageHashSet.size() > 2000) {
                cleanupMessageHashSet();
            }
        }
        
        return false;
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
        
        // 시간 차이가 5초 이내인지 확인 (동일한 메시지가 거의 동시에 온 경우)
        long timeDiff = Math.abs(msg1.getTimestamp() - msg2.getTimestamp());
        if (timeDiff > 5000) { // 5초 초과하면 다른 메시지로 판단
            return false;
        }
        
        // 발신자가 모두 현재 사용자인 경우 (내가 보낸 메시지가 중복으로 표시되는 경우)
        boolean isMsg1Mine = isCurrentUserMessage(msg1.getSenderId(), msg1.getSenderName());
        boolean isMsg2Mine = isCurrentUserMessage(msg2.getSenderId(), msg2.getSenderName());
        
        if (isMsg1Mine && isMsg2Mine) {
            Log.d(TAG, "내가 보낸 메시지 중복 감지: 시간차=" + timeDiff + "ms");
            return true;
        }
        
        return false;
    }
    
    // 향상된 메시지 고유 해시 생성
    private String generateMessageHashEnhanced(Message message) {
        StringBuilder hashBuilder = new StringBuilder();
        
        // 타임스탬프 (밀리초 단위)
        hashBuilder.append(message.getTimestamp());
        hashBuilder.append("_");
        
        // 발신자 ID (정규화)
        String senderId = message.getSenderId();
        if (senderId != null) {
            senderId = senderId.trim().toLowerCase();
        }
        hashBuilder.append(senderId != null ? senderId : "unknown");
        hashBuilder.append("_");
        
        // 메시지 내용 (정규화)
        String content = message.getMessage();
        if (content != null) {
            content = content.trim();
        }
        hashBuilder.append(content != null ? content.hashCode() : 0);
        
        // 파일 정보 (있는 경우)
        if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
            hashBuilder.append("_file_");
            hashBuilder.append(message.getFileUrl().trim().hashCode());
            if (message.getFileType() != null) {
                hashBuilder.append("_");
                hashBuilder.append(message.getFileType().trim());
            }
        }
        
        return hashBuilder.toString();
    }
    
    // 두 메시지가 정확히 동일한지 확인
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
    
    // 그룹 멤버 목록을 로드하는 메소드
    private void loadGroupMembers() {
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
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "멤버 목록을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                });
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
                            List<User> members = new ArrayList<>();
                            
                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject userObject = usersArray.getJSONObject(i);
                                String name = userObject.optString("name", "알 수 없음");
                                String email = userObject.optString("email", "");
                                String smbti = userObject.optString("smbti", "");
                                
                                members.add(new User(name, email, smbti));
                            }
                            
                            runOnUiThread(() -> {
                                memberList.clear();
                                memberList.addAll(members);
                                memberAdapter.updateMembers(memberList);
                                Log.d(TAG, "멤버 목록 업데이트 완료: " + members.size() + "명");
                            });
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "그룹 멤버 응답 파싱 오류", e);
                        runOnUiThread(() -> {
                            Toast.makeText(ChatActivity.this, "멤버 정보 처리 중 오류 발생", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.e(TAG, "그룹 멤버 조회 서버 오류: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "서버 오류: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    // 네트워크 상태 감지 설정
    private void setupNetworkReceiver() {
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                    boolean isConnected = isNetworkConnected();
                    
                    if (isConnected && wasOffline) {
                        // 오프라인에서 온라인으로 전환된 경우
                        System.out.println("네트워크 연결 복구됨 - 강화된 메시지 동기화 시작");
                        wasOffline = false;
                        
                        // 네트워크 안정화를 위해 잠시 대기 후 강화된 동기화 실행
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            // 오프라인 중 놓친 메시지들을 강제로 동기화
                            System.out.println("오프라인 복구 - 전체 메시지 동기화 시작");
                            showSnackbar("오프라인 중 놓친 메시지를 확인하는 중...");
                            
                            // 1. 기본 동기화 수행
                            syncMessagesFromServer();
                            
                            // 2. 전체 동기화로 누락된 메시지 확실히 복구
                            performFullMessageSync();
                            
                            // 3. 웹소켓 재연결 시도
                            if (webSocketService == null || !webSocketService.isConnected()) {
                                initWebSocket();
                            }
                        }, 3000); // 3초 대기 (네트워크 안정화)
                        
                    } else if (!isConnected) {
                        // 온라인에서 오프라인으로 전환된 경우
                        System.out.println("네트워크 연결 끊어짐");
                        wasOffline = true;
                        if (webSocketService != null) {
                            webSocketService.disconnect();
                        }
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
        
        // 초기 네트워크 상태 확인
        wasOffline = !isNetworkConnected();
    }
    
    // 네트워크 연결 상태 확인
    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = 
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    // 정기적인 메시지 동기화 설정
    private void setupPeriodicSync() {
        syncHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                // 네트워크가 연결되어 있을 때만 동기화 실행
                if (isNetworkConnected()) {
                    syncMessagesFromServer();
                }
                // 다음 동기화 예약
                syncHandler.postDelayed(this, SYNC_INTERVAL);
            }
        };
        // 첫 번째 동기화 시작
        syncHandler.postDelayed(syncRunnable, SYNC_INTERVAL);
    }
    
    // 정기적인 메시지 동기화 중지
    private void stopPeriodicSync() {
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }
    }
    
    // 즉시 메시지 동기화 실행 (수동 호출용)
    private void forceSyncMessages() {
        if (isNetworkConnected()) {
            Log.d(TAG, "강제 메시지 동기화 시작");
            showSnackbar("메시지 동기화 중...");
            
            // 개선된 동기화: 마지막 메시지 이후의 모든 메시지 가져오기
            syncMessagesFromServer();
            
            // 추가로 전체 동기화도 수행 (오프라인 중 누락된 메시지 확실히 복구)
            performFullMessageSync();
        } else {
            showSnackbar("네트워크 연결을 확인해주세요");
        }
    }
    
    // 전체 메시지 동기화 (오프라인 복구용)
    private void performFullMessageSync() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build();
        
        // 전체 메시지 히스토리 가져오기 (타임스탬프 제한 없음)
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
                runOnUiThread(() -> showSnackbar("메시지 동기화 실패"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        Log.d(TAG, "전체 메시지 동기화 응답 받음");
                        
                        JSONArray messagesArray = null;
                        
                        // 응답이 직접 배열인지 객체인지 확인
                        if (responseData.trim().startsWith("[")) {
                            // 직접 JSONArray 형태의 응답
                            messagesArray = new JSONArray(responseData);
                        } else {
                            // JSONObject 형태의 응답
                            JSONObject jsonObject = new JSONObject(responseData);
                            
                            // API 응답 구조에 따라 메시지 배열 추출
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
                            
                            runOnUiThread(() -> {
                                // 서버 메시지와 로컬 메시지 비교하여 누락된 메시지 찾기
                                List<Message> missingMessages = findMissingMessages(serverMessages);
                                
                                if (!missingMessages.isEmpty()) {
                                    Log.d(TAG, "누락된 메시지 " + missingMessages.size() + "개 발견");
                                    
                                    // 누락된 메시지들을 UI에 추가
                                    for (Message message : missingMessages) {
                                        messageAdapter.addMessage(message);
                                        // 데이터베이스에도 저장
                                        messageRepository.saveMessage(currentGroupId, message);
                                    }
                                    
                                    // 메시지 목록을 시간순으로 정렬
                                    sortMessagesByTimestamp();
                                    messageAdapter.notifyDataSetChanged();
                                    recyclerView.scrollToPosition(messageList.size() - 1);
                                    
                                    showSnackbar("누락된 메시지 " + missingMessages.size() + "개 복구됨");
                                } else {
                                    Log.d(TAG, "누락된 메시지 없음");
                                    showSnackbar("모든 메시지가 최신 상태입니다");
                                }
                            });
                        }
                        
                    } catch (JSONException e) {
                        Log.e(TAG, "전체 메시지 동기화 응답 파싱 오류", e);
                        runOnUiThread(() -> showSnackbar("메시지 동기화 처리 오류"));
                    }
                } else {
                    Log.e(TAG, "전체 메시지 동기화 서버 오류: " + response.code());
                    if (response.code() != 404) {
                        runOnUiThread(() -> showSnackbar("메시지 동기화 실패: " + response.code()));
                    }
                }
            }
        });
    }
    
    // 서버 메시지와 로컬 메시지를 비교하여 누락된 메시지 찾기
    private List<Message> findMissingMessages(List<Message> serverMessages) {
        List<Message> missingMessages = new ArrayList<>();
        
        for (Message serverMessage : serverMessages) {
            boolean found = false;
            
            // 로컬 메시지 목록에서 동일한 메시지가 있는지 확인
            for (Message localMessage : messageList) {
                if (isSameMessageExact(serverMessage, localMessage)) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                missingMessages.add(serverMessage);
            }
        }
        
        return missingMessages;
    }
    
    // 메시지 목록을 타임스탬프 순으로 정렬
    private void sortMessagesByTimestamp() {
        try {
            messageList.sort((m1, m2) -> {
                long timestamp1 = m1.getTimestamp();
                long timestamp2 = m2.getTimestamp();
                return Long.compare(timestamp1, timestamp2);
            });
        } catch (Exception e) {
            System.out.println("메시지 정렬 오류: " + e.getMessage());
        }
    }

    // 메시지 해시 세트 정리 (메모리 최적화) - 스레드 안전 버전
    private void cleanupMessageHashSet() {
        synchronized (messageHashSet) {
            Log.d(TAG, "메시지 해시 세트 정리 시작 - 현재 크기: " + messageHashSet.size());
            
            // 현재 메시지 리스트의 해시만 유지
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
}
