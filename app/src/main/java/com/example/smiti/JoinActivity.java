package com.example.smiti;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smiti.api.AddUserRequest;
import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JoinActivity extends AppCompatActivity {

    private static final String TAG = "JoinActivity";
    
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText rePasswordEditText;
    private EditText nameEditText;
    private Button joinButton;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        // 뷰 초기화
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        rePasswordEditText = findViewById(R.id.re_password);
        nameEditText = findViewById(R.id.name);
        joinButton = findViewById(R.id.join_button);
        backButton = findViewById(R.id.back_button);

        // 뒤로가기 버튼 클릭 이벤트
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // 회원가입 버튼 클릭 이벤트
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 입력값 가져오기
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String rePassword = rePasswordEditText.getText().toString();
                String name = nameEditText.getText().toString();
                
                // 간단한 유효성 검사
                if (email.isEmpty() || password.isEmpty() || rePassword.isEmpty()
                        || name.isEmpty()) {
                    Toast.makeText(JoinActivity.this, "필수 항목을 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 이메일 형식 검사
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(JoinActivity.this, "올바른 이메일 형식이 아닙니다", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 비밀번호 일치 여부 확인
                if (!password.equals(rePassword)) {
                    Toast.makeText(JoinActivity.this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 서버에 회원가입 요청
                registerUser(email, password, name, "미설정");
            }
        });
    }
    
    private void registerUser(String email, String password, String name, String mbti) {
        // 회원가입 API 호출
        AddUserRequest request = new AddUserRequest(email, password, name, mbti);
        
        Call<ApiResponse> call = RetrofitClient.getApiService().registerUser(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse result = response.body();
                    Toast.makeText(JoinActivity.this, "회원가입 성공!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "API 응답: " + result.getMessage());
                    
                    // 로그인 화면으로 이동하면서 이메일과 비밀번호 전달
                    Intent intent = new Intent(JoinActivity.this, LoginActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("password", password);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(JoinActivity.this, "회원가입 실패: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "API 오류: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toast.makeText(JoinActivity.this, "서버 연결 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "API 호출 실패", t);
            }
        });
    }
} 