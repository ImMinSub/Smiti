package com.example.smiti;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

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

    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private String currentUserId; // 내부 고유 ID
    private String currentUserName;
    private String currentUserEmail; // 로그인 ID (이메일), sender_id로 사용

    private WebSocketService webSocketService;
    private boolean isConnectionMessageShown = false;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        try {
            // 보낸 메시지 ID Set 초기화 (최대 100개 유지, 오래된 순으로 제거)
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

            initWebSocket(); // 웹소켓 초기화 및 연결
            
            // 파일 다운로드를 위한 권한 체크
            checkStoragePermission();

        } catch (Exception e) {
            Log.e(TAG, "onCreate 오류", e);
            Toast.makeText(this, "앱 초기화 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
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
        // 앱이 다시 활성화될 때 웹소켓 연결 확인 및 재연결 시도
        try {
            if (webSocketService != null && !webSocketService.isConnected()) {
                webSocketService.connect(currentGroupId, currentUserEmail, this);
            }
        } catch (Exception e) {
            Log.e(TAG, "웹소켓 재연결 오류", e);
            Toast.makeText(this, "채팅 재연결 실패", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티 종료 시 웹소켓 연결 해제
        if (webSocketService != null) {
            webSocketService.disconnect();
        }
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
        String uploadUrl = BASE_URL + "/uploads/{fileName}"; // 서버에서 사용하는 올바른 업로드 경로

        try {
            // 파일 타입 가져오기
            String mimeType = getContentResolver().getType(fileUri);
            String fileType = "image"; // 기본값
            String filename = "uploaded_file"; // 기본 파일명
            
            // 파일의 MIME 타입에 따라 처리
            if (mimeType != null) {
                if (mimeType.startsWith("image/")) {
                    fileType = "image";
                    filename = "uploaded_image." + getMimeExtension(mimeType);
                } else if (mimeType.equals("application/pdf")) {
                    fileType = "pdf";
                    filename = "uploaded_document.pdf";
                } else {
                    fileType = "file";
                    filename = "uploaded_file";
                }
            }
            
            // 파일명 가져오기 시도
            String displayName = getFileDisplayName(fileUri);
            if (displayName != null && !displayName.isEmpty()) {
                filename = displayName;
            }
            
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
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
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
                        // 서버에서 file_url 또는 fileUrl 키로 URL을 반환할 수 있음
                        String fileUrl = jsonResponse.optString("file_url", 
                                        jsonResponse.optString("fileUrl", 
                                        jsonResponse.optString("url", ""))); // 서버에서 반환된 파일 URL
    
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
        String messageText = "";  // 빈 문자열로 변경하여 메시지 텍스트 제거
        
        String localId = UUID.randomUUID().toString();
    
        ChatMessage chatMessage = new ChatMessage(currentUserEmail, currentUserName, messageText, fileUrl, fileType);
        chatMessage.setGroupId(currentGroupId);
        chatMessage.setLocalId(localId);
    
        sentMessageIds.add(localId);
    
        if (webSocketService != null && webSocketService.isConnected()) {
            webSocketService.sendMessage(chatMessage.toJson());
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

    // WebSocketListener 인터페이스 구현: 메시지 수신 시 호출
    @Override
    public void onMessage(String rawJsonMessage) {
        try {
            // 메시지 파싱 (서버 시간 포함)
            ChatMessage chatMessage = ChatMessage.fromJson(rawJsonMessage);
            Log.d(TAG, "메시지 수신 (서버 시간): SenderId=[" + chatMessage.getSenderId() +
                    "], SenderName=[" + chatMessage.getSenderName() +
                    "], localId=[" + chatMessage.getLocalId() + 
                    "], type=[" + chatMessage.getType() +
                    "], fileType=[" + chatMessage.getFileType() +
                    "], fileUrl=[" + chatMessage.getFileUrl() + "]");

            // localId로 에코 메시지(내가 보낸 메시지) 확인
            String receivedLocalId = chatMessage.getLocalId();
            if (receivedLocalId != null && !receivedLocalId.isEmpty() && sentMessageIds.contains(receivedLocalId)) {
                Log.d(TAG, "내가 보낸 메시지(Echo) 감지 (localId) - UI 업데이트 실행");
                sentMessageIds.remove(receivedLocalId); // 확인된 ID는 제거
            }

            // 모든 메시지(내 에코 메시지 포함)를 UI에 추가
            runOnUiThread(() -> {
                Message uiMessage = chatMessage.toUIMessage(); // UI용 Message 객체로 변환 (서버 시간 사용)
                Log.d(TAG, "UI에 메시지 추가 (서버 시간): 발신자ID=[" + uiMessage.getSenderId() +
                        "], 발신자이름=[" + uiMessage.getSenderName() + 
                        "], 파일타입=[" + uiMessage.getFileType() +
                        "], 파일URL=[" + uiMessage.getFileUrl() + "]");
                messageAdapter.addMessage(uiMessage); // 어댑터에 추가
                recyclerView.scrollToPosition(messageList.size() - 1); // 맨 아래로 스크롤
            });

        } catch (JSONException e) {
            Log.e(TAG, "메시지 파싱 오류", e);
        } catch (Exception e) {
            Log.e(TAG, "메시지 처리 중 오류", e);
        }
    }

    // WebSocketListener 인터페이스 구현: 연결 종료 시 호출
    @Override
    public void onDisconnect(int code, String reason) {
        runOnUiThread(() -> {
            showSnackbar("서버 연결 종료: " + reason);
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

    // 화면 하단에 Snackbar 메시지 표시
    private void showSnackbar(String message) {
        try {
            if (rootView == null) {
                rootView = findViewById(android.R.id.content);
                if (rootView == null) {
                    // 최후의 수단으로 Toast 사용
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            // 채팅 입력창을 앵커로 지정 (edit_message가 입력창의 ID라면)
            View anchor = messageEditText != null ? messageEditText : rootView;
            Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).setAnchorView(anchor);
            snackbar.show();
        } catch (Exception e) {
            Log.e(TAG, "Snackbar 표시 중 오류", e);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); // Snackbar 실패 시 Toast 사용
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
    }
    
    private void setupListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
        attachButton.setOnClickListener(v -> showFileTypeSelectionDialog());
        
        ImageButton summaryButton = findViewById(R.id.summary_button);
        if (summaryButton != null) {
            summaryButton.setOnClickListener(v -> requestChatSummary());
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
}
