package com.example.smiti;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;

import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.LoginRequest;
import com.example.smiti.api.RetrofitClient;
import com.example.smiti.network.LoginResponse;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_AUTO_LOGIN = "auto_login";
    private static final String KEY_NAME = "name";
    private static final String KEY_MBTI = "mbti";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_LAST_LOGIN = "last_login";

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private CheckBox autoLoginCheckBox;
    private TextView findIdTextView;
    private TextView findPasswordTextView;
    private TextView joinTextView;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // RetrofitClient 초기화
        RetrofitClient.initialize();

        // 뷰 초기화
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        autoLoginCheckBox = findViewById(R.id.auto_login_checkbox);
        findIdTextView = findViewById(R.id.find_id);
        findPasswordTextView = findViewById(R.id.find_password);
        joinTextView = findViewById(R.id.join);
        
        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        // 회원가입 화면에서 전달받은 이메일과 비밀번호 처리
        Intent intent = getIntent();
        String emailFromJoin = intent.getStringExtra("email");
        String passwordFromJoin = intent.getStringExtra("password");
        if (emailFromJoin != null && passwordFromJoin != null) {
            usernameEditText.setText(emailFromJoin);
            passwordEditText.setText(passwordFromJoin);
            // 자동 로그인 체크
            autoLoginCheckBox.setChecked(true);
        } else {
            // 자동 로그인 체크 상태 복원 (UI만)
            boolean autoLogin = sharedPreferences.getBoolean(KEY_AUTO_LOGIN, false);
            Log.d(TAG, "onCreate - 저장된 자동 로그인 상태: " + autoLogin);
            autoLoginCheckBox.setChecked(autoLogin);
            
            if (autoLogin) {
                String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
                String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
                if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                    Log.d(TAG, "onCreate - 자동 로그인 정보 복원 (UI): " + savedEmail);
                    usernameEditText.setText(savedEmail);
                    passwordEditText.setText(savedPassword);
                }
            }
        }

        // 로그인 버튼 클릭 이벤트
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                
                // 간단한 유효성 검사
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 로그인 처리 로직 구현
                loginUser(email, password);
            }
        });

        // ID 찾기 클릭 이벤트
        findIdTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ID 찾기 화면으로 이동
                Intent intent = new Intent(LoginActivity.this, FindIdActivity.class);
                startActivity(intent);
            }
        });

        // 비밀번호 찾기 클릭 이벤트
        findPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 비밀번호 찾기 화면으로 이동
                Intent intent = new Intent(LoginActivity.this, FindPasswordActivity.class);
                startActivity(intent);
            }
        });

        // 회원가입 클릭 이벤트
        joinTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 회원가입 화면으로 이동
                Intent intent = new Intent(LoginActivity.this, JoinActivity.class);
                startActivity(intent);
            }
        });
    }
    
    private void loginUser(String email, String password) {
        // 로그인 버튼 비활성화 (중복 클릭 방지)
        loginButton.setEnabled(false);
        
        // 로딩 표시
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("로그인 중...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // 로그인 API 호출
        LoginRequest request = new LoginRequest(email, password);
        
        Call<ApiResponse> call = RetrofitClient.getApiService().login(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                // 로그인 버튼 다시 활성화
                loginButton.setEnabled(true);
                
                // 로딩 다이얼로그 닫기
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse result = response.body();
                    
                    // 로그인 성공 시 자동 로그인 정보 저장
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    // 기본 사용자 정보 항상 저장
                    editor.putString(KEY_EMAIL, email);
                    if (autoLoginCheckBox.isChecked()) {
                        editor.putString(KEY_PASSWORD, password);
                        editor.putBoolean(KEY_AUTO_LOGIN, true);
                    } else {
                        // 자동 로그인 체크 해제 시 저장된 정보 삭제
                        editor.remove(KEY_PASSWORD);
                        editor.putBoolean(KEY_AUTO_LOGIN, false);
                    }
                    // 로그인 상태 항상 true로 설정 (자동 로그인 체크박스와 상관없이)
                    editor.putBoolean(KEY_IS_LOGGED_IN, true);
                    
                    // 설정 즉시 저장
                    editor.commit();
                    
                    // 사용자 데이터 저장 (API 응답에서 LoginResponse 객체로 변환)
                    try {
                        if (result.getData() != null) {
                            // 서버 응답의 data 필드에서 LoginResponse 객체 생성
                            Gson gson = new Gson();
                            String json = gson.toJson(result.getData());
                            LoginResponse loginResponse = gson.fromJson(json, LoginResponse.class);
                            
                            // 사용자 데이터 저장
                            saveUserData(loginResponse);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "사용자 데이터 변환 오류", e);
                    }
                    
                    Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "API 응답: " + result.getMessage());
                    
                    // 메인 화면으로 이동
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("email", email); // 이메일 정보를 명시적으로 전달
                    Log.d(TAG, "MainActivity로 전달하는 이메일: " + email);
                    startActivity(intent);
                    finish(); // 로그인 화면 종료
                } else {
                    String errorMessage = "로그인 실패";
                    
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorMessage);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error body 읽기 실패", e);
                    }
                    
                    // 응답 코드에 따른 적절한 메시지 표시
                    if (response.code() == 401) {
                        errorMessage = "이메일 또는 비밀번호가 올바르지 않습니다";
                    } else if (response.code() >= 500) {
                        errorMessage = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요";
                    }
                    
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "API 오류: " + response.message() + " (코드: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // 로그인 버튼 다시 활성화
                loginButton.setEnabled(true);
                
                // 로딩 다이얼로그 닫기
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                
                // 오류 유형에 따른 다른 메시지 표시
                String errorMessage;
                if (t instanceof java.net.SocketTimeoutException) {
                    errorMessage = "서버 응답 시간이 초과되었습니다. 네트워크를 확인해주세요";
                } else if (t instanceof java.net.UnknownHostException) {
                    errorMessage = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해주세요";
                } else {
                    errorMessage = "서버 연결 오류: " + t.getMessage();
                }
                
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                Log.e(TAG, "API 호출 실패", t);
                
                // 네트워크 연결 문제인 경우 재시도 버튼 제공
                if (t instanceof java.net.UnknownHostException || t instanceof java.net.SocketTimeoutException) {
                    new AlertDialog.Builder(LoginActivity.this)
                        .setTitle("연결 오류")
                        .setMessage("서버에 연결할 수 없습니다. 다시 시도하시겠습니까?")
                        .setPositiveButton("재시도", (dialog, which) -> loginUser(email, password))
                        .setNegativeButton("취소", null)
                        .show();
                }
            }
        });
    }

    private void saveUserData(LoginResponse loginResponse) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        // 서버 응답에서 사용자 정보 추출
        String userId = loginResponse.getUserId();
        String email = loginResponse.getEmail();
        String name = loginResponse.getName();
        String mbti = loginResponse.getMbti();
        
        // 디버깅을 위한 로그 추가
        Log.d(TAG, "저장할 사용자 정보: userId=" + userId + ", email=" + email + ", name=" + name + ", mbti=" + mbti);
        
        // 모든 필드가 null이 아닌지 확인하고 SharedPreferences에 저장
        if (userId != null) editor.putString(KEY_USER_ID, userId);
        if (email != null) editor.putString(KEY_EMAIL, email);
        if (name != null) editor.putString(KEY_NAME, name);
        if (mbti != null) editor.putString(KEY_MBTI, mbti);
        
        // 자동 로그인 상태 확인 및 저장
        boolean autoLogin = autoLoginCheckBox.isChecked();
        editor.putBoolean(KEY_AUTO_LOGIN, autoLogin);
        
        // 로그인 상태를 true로 설정
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        
        // 마지막 로그인 시간 저장 (선택 사항)
        editor.putLong(KEY_LAST_LOGIN, System.currentTimeMillis());
        
        // 변경사항 커밋 (apply 대신 commit 사용하여 즉시 저장)
        boolean saveSuccess = editor.commit();
        Log.d(TAG, "사용자 정보 저장 " + (saveSuccess ? "성공" : "실패"));
        
        // 저장된 정보 확인을 위한 로그
        SharedPreferences checkPrefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Log.d(TAG, "저장 후 확인 - isLoggedIn: " + checkPrefs.getBoolean(KEY_IS_LOGGED_IN, false));
        Log.d(TAG, "저장 후 확인 - autoLogin: " + checkPrefs.getBoolean(KEY_AUTO_LOGIN, false)); 
        Log.d(TAG, "저장 후 확인 - name: " + checkPrefs.getString(KEY_NAME, "없음"));
        Log.d(TAG, "저장 후 확인 - email: " + checkPrefs.getString(KEY_EMAIL, "없음"));
        Log.d(TAG, "저장 후 확인 - password: " + (checkPrefs.getString(KEY_PASSWORD, "").isEmpty() ? "저장 안됨" : "저장됨"));
        Log.d(TAG, "저장 후 확인 - mbti: " + checkPrefs.getString(KEY_MBTI, "없음"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // onResume에서는 UI 상태만 복원
        boolean autoLogin = sharedPreferences.getBoolean(KEY_AUTO_LOGIN, false);
        autoLoginCheckBox.setChecked(autoLogin);
        
        Log.d(TAG, "onResume - 자동 로그인 체크박스 상태 복원: " + autoLogin);
        
        if (autoLogin) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
            if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                Log.d(TAG, "onResume - 필드 값 복원: " + savedEmail);
                usernameEditText.setText(savedEmail);
                passwordEditText.setText(savedPassword);
            }
        }
    }
} 