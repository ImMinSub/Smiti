package com.example.smiti;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.example.smiti.MessageAdapter.OnQuestionButtonClickListener;

public class ChatbotActivity extends AppCompatActivity {
    
    private static final String TAG = "ChatbotActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "name";
    
    // 서버 API 엔드포인트
    private static final String BASE_URL = "http://202.31.246.51:80";
    private static final String AI_ASK_ENDPOINT = "/ai/ask";
    
    private RecyclerView recyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigationView;
    
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    
    private String currentUserId;
    private String currentUserName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);
        
        // 사용자 정보 가져오기
        loadUserData();
        
        // 뷰 초기화
        recyclerView = findViewById(R.id.recyclerView);
        messageEditText = findViewById(R.id.edit_message);
        sendButton = findViewById(R.id.send_button);
        progressBar = findViewById(R.id.progress_bar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // RecyclerView 설정
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList, currentUserId);
        recyclerView.setAdapter(messageAdapter);
        
        // 버튼 클릭 이벤트 리스너 설정
        messageAdapter.setOnQuestionButtonClickListener(new OnQuestionButtonClickListener() {
            @Override
            public void onQuestionButtonClick(String question) {
                // 질문 버튼이 클릭되었을 때 처리
                messageEditText.setText(question);
                sendMessageToChatbot();
            }
        });
        
        // 메시지 전송 버튼 클릭 이벤트
        sendButton.setOnClickListener(v -> sendMessageToChatbot());
        
        // 바텀 네비게이션 설정
        setupBottomNavigation();
        
        // 시작 메시지 추가
        addSystemMessage("안녕하세요! 무엇을 도와드릴까요?");
    }
    
    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_chat);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.navigation_home) {
                finish();
                return true;
            } else if (id == R.id.navigation_chat) {
                // 이미 챗봇 화면에 있음
                return true;
            } else if (id == R.id.navigation_profile) {
                finish();
                return true;
            }
            
            return false;
        });
    }
    
    private void loadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        currentUserId = sharedPreferences.getString(KEY_USER_ID, "user_" + System.currentTimeMillis());
        currentUserName = sharedPreferences.getString(KEY_NAME, "사용자");
        
        Log.d(TAG, "사용자 정보 로드: id=" + currentUserId + ", name=" + currentUserName);
    }
    
    private void sendMessageToChatbot() {
        String messageContent = messageEditText.getText().toString().trim();
        if (messageContent.isEmpty()) {
            Toast.makeText(this, "메시지를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 사용자 메시지 추가
        Message userMessage = new Message();
        userMessage.setSenderId(currentUserId);
        userMessage.setMessage(messageContent);
        userMessage.setSenderName(currentUserName);
        userMessage.setTimestamp(new Date().getTime());


        messageList.add(userMessage);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
        
        // 입력창 비우기
        messageEditText.setText("");
        
        // 로딩 표시
        progressBar.setVisibility(View.VISIBLE);
        
        // AI 응답 요청
        requestAiResponse(messageContent);
    }
    
    private void requestAiResponse(String question) {
        // OkHttp 클라이언트 생성 - 타임아웃 설정 크게 증가
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)  // 연결 타임아웃 60초
            .readTimeout(120, TimeUnit.SECONDS)    // 읽기 타임아웃 120초 (AI 응답 생성에 시간 필요)
            .writeTimeout(30, TimeUnit.SECONDS)    // 쓰기 타임아웃 30초
            .build();
        
        // 요청 본문 생성
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("question", question);
        } catch (JSONException e) {
            Log.e(TAG, "JSON 생성 오류", e);
            showError("요청 생성 중 오류가 발생했습니다");
            return;
        }
        
        // 요청 본문 설정
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), jsonBody.toString());
        
        // 요청 생성
        Request request = new Request.Builder()
                .url(BASE_URL + AI_ASK_ENDPOINT)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();
        
        // 비동기 요청 실행
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "AI 응답 요청 실패", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showError("네트워크 오류가 발생했습니다");
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        String aiResponse = jsonResponse.getString("answer");
                        
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            addBotMessage(aiResponse);
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "응답 파싱 오류", e);
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            showError("응답 처리 중 오류가 발생했습니다");
                        });
                    }
                } else {
                    Log.e(TAG, "서버 오류: " + response.code());
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        showError("서버 오류: " + response.code());
                    });
                }
            }
        });
    }
    
    private void addBotMessage(String content) {
        Message botMessage = new Message();
        botMessage.setSenderId("bot");
        botMessage.setMessage(content);
        botMessage.setSenderName("챗봇");
        botMessage.setTimestamp(new Date().getTime());
        
        messageList.add(botMessage);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }
    
    private void addSystemMessage(String content) {
        Message systemMessage = new Message();
        systemMessage.setSenderId("system");
        systemMessage.setMessage(content);
        systemMessage.setSenderName("챗봇");
        systemMessage.setTimestamp(new Date().getTime());

        messageList.add(systemMessage);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        addSystemMessage("오류: " + message);
    }
} 