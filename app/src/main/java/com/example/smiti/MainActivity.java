package com.example.smiti;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final int REQUEST_SMBTI_TEST = 1001;
    
    private TextView nameTextView, mbtiTextView, groupCountTextView;
    private ImageView profileImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 개인 정보 뷰 초기화
        initializeProfileViews();
        
        // 개인 정보 로드
        loadProfileData();

        // 버튼들(LinearLayout)
        LinearLayout smitiTestBtn = findViewById(R.id.smiti_test_btn);
        LinearLayout adminBtn = findViewById(R.id.management_btn);
        LinearLayout chatTalkBtn = findViewById(R.id.chat_btn);

        // BottomNavigationView 설정
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.navigation_home) {
                // 이미 홈 화면에 있음
                return true;
            } else if (id == R.id.navigation_search) {
                // 그룹 검색 화면으로 이동
                Intent intent = new Intent(MainActivity.this, GroupSearchActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.navigation_chat) {
                // 채팅 화면으로 이동
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.navigation_board) {
                // 게시판 화면으로 이동
                Intent intent = new Intent(MainActivity.this, BoardActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.navigation_profile) {
                // 프로필 화면으로 이동
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            
            return false;
        });

        
        // 버튼 클릭 이벤트 설정
        smitiTestBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SmbtiTestActivity.class);
            startActivityForResult(intent, REQUEST_SMBTI_TEST);
        });
        adminBtn.setOnClickListener(v -> Toast.makeText(MainActivity.this, "관리 기능 실행", Toast.LENGTH_SHORT).show());
        chatTalkBtn.setOnClickListener(v -> {
            // 챗봇 화면으로 이동
            Intent intent = new Intent(MainActivity.this, ChatbotActivity.class);
            startActivity(intent);
        });
        
        // 프로필 이미지 클릭 시 프로필 화면으로 이동
        profileImageView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 프로필 정보 갱신
        loadProfileData();
    }
    
    private void initializeProfileViews() {
        profileImageView = findViewById(R.id.profile_image);
        nameTextView = findViewById(R.id.tv_name);
        mbtiTextView = findViewById(R.id.tv_mbti);
        groupCountTextView = findViewById(R.id.tv_group_count);
    }
    
    private void loadProfileData() {
        // 로컬에서 사용자 정보 불러오기
        loadUserDataFromLocal();
        loadProfileImageFromFilePath();
        
        // 서버에서 최신 데이터 불러오기
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String email = sharedPreferences.getString("email", "");
        
        if (email != null && !email.isEmpty()) {
            fetchUserDataFromServer(email);
            fetchUserGroupsFromServer(email);
        }
    }
    
    private void loadUserDataFromLocal() {
        Log.d(TAG, "로컬 저장소에서 사용자 정보 불러오기");
        try {
            SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String name = sharedPreferences.getString("name", "사용자");
            String email = sharedPreferences.getString("email", "");
            String mbti = sharedPreferences.getString("mbti", "");
            int groupCount = sharedPreferences.getInt("groupCount", 0);

            nameTextView.setText("이름: " + name);
            mbtiTextView.setText("MBTI: " + (mbti.isEmpty() ? "미설정" : mbti));
            groupCountTextView.setText("소속 그룹 수: " + groupCount + "개");
            
            Log.d(TAG, "로컬에서 불러온 데이터 - 이름: " + name + ", MBTI: " + mbti + ", 그룹: " + groupCount);
        } catch (Exception e) {
            Log.e(TAG, "로컬 사용자 정보 로드 실패", e);
            nameTextView.setText("이름: 사용자");
            mbtiTextView.setText("MBTI: 미설정");
            groupCountTextView.setText("소속 그룹 수: 0개");
        }
    }
    
    private void loadProfileImageFromFilePath() {
        Log.d(TAG, "프로필 이미지 로드 시도");
        try {
            SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String imagePath = sharedPreferences.getString("profile_image_path", "");
            if (!imagePath.isEmpty()) {
                File imgFile = new File(imagePath);
                if(imgFile.exists()) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    if (myBitmap != null) {
                        profileImageView.setImageBitmap(myBitmap);
                        Log.d(TAG, "저장된 프로필 이미지 로드 성공");
                    } else {
                        Log.e(TAG, "비트맵 디코딩 실패");
                        profileImageView.setImageResource(R.drawable.ic_profile);
                    }
                } else {
                    Log.e(TAG, "프로필 이미지 파일이 존재하지 않음");
                    profileImageView.setImageResource(R.drawable.ic_profile);
                }
            } else {
                Log.d(TAG, "저장된 프로필 이미지 경로 없음");
                profileImageView.setImageResource(R.drawable.ic_profile);
            }
        } catch (Exception e) {
            Log.e(TAG, "프로필 이미지 로드 중 예외 발생", e);
            profileImageView.setImageResource(R.drawable.ic_profile);
        }
    }
    
    private void fetchUserDataFromServer(String email) {
        Log.d(TAG, "서버에 사용자 정보 요청 시작: " + email);
        
        new Thread(() -> {
            try {
                // URL 설정 - GET 요청을 위한 쿼리 파라미터 추가
                URL url = new URL("http://202.31.246.51:80/users/me?email=" + URLEncoder.encode(email, "UTF-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 요청 설정
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                Log.d(TAG, "서버 요청 URL: " + url.toString());

                // 응답 처리
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "서버 응답 코드: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder responseBuilder = new StringBuilder();
                    
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                    }
                    
                    final String responseData = responseBuilder.toString();
                    Log.d(TAG, "서버 응답 데이터: " + responseData);

                    // JSON 응답 파싱 및 UI 업데이트
                    try {
                        JSONObject responseJson = new JSONObject(responseData);
                        runOnUiThread(() -> updateUIWithUserData(responseJson));
                    } catch (Exception e) {
                        Log.e(TAG, "JSON 파싱 오류: " + e.getMessage(), e);
                    }
                } else {
                    Log.e(TAG, "서버 오류: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "서버 통신 예외 발생: " + e.getMessage(), e);
            }
        }).start();
    }
    
    private void updateUIWithUserData(JSONObject userData) {
        try {
            Log.d(TAG, "UI 업데이트 시작");
            
            if (userData == null) {
                Log.e(TAG, "userData가 null입니다");
                return;
            }
            
            // 데이터 추출 (data 필드 또는 최상위)
            JSONObject dataToUse = userData.has("data") ? 
                    userData.getJSONObject("data") : userData;
            
            // 이름 표시
            if (dataToUse.has("name")) {
                String name = dataToUse.getString("name");
                Log.d(TAG, "서버에서 가져온 이름: " + name);
                nameTextView.setText("이름: " + name);
                
                // 이름 로컬 저장
                SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("name", name);
                editor.apply();
            }
            
            // MBTI 표시
            if (dataToUse.has("mbti") && !dataToUse.isNull("mbti")) {
                String mbti = dataToUse.getString("mbti");
                Log.d(TAG, "서버에서 가져온 MBTI: " + mbti);
                mbtiTextView.setText("MBTI: " + mbti);
                
                // MBTI 로컬 저장
                SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("mbti", mbti);
                editor.apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "UI 업데이트 오류: " + e.getMessage(), e);
        }
    }
    
    private void fetchUserGroupsFromServer(String email) {
        Log.d(TAG, "서버에 사용자 그룹 정보 요청 시작: " + email);
        
        new Thread(() -> {
            try {
                // URL 설정 - GET 요청을 위한 쿼리 파라미터 추가
                URL url = new URL("http://202.31.246.51:80/groups/user?email=" + URLEncoder.encode(email, "UTF-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 요청 설정
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                Log.d(TAG, "서버 요청 URL: " + url.toString());

                // 응답 처리
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "서버 응답 코드: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder responseBuilder = new StringBuilder();
                    
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                    }
                    
                    final String responseData = responseBuilder.toString();
                    Log.d(TAG, "서버 응답 데이터: " + responseData);

                    // JSON 응답 파싱 및 UI 업데이트
                    try {
                        JSONObject responseJson = new JSONObject(responseData);
                        runOnUiThread(() -> updateGroupCountUI(responseJson));
                    } catch (Exception e) {
                        Log.e(TAG, "JSON 파싱 오류: " + e.getMessage(), e);
                    }
                } else {
                    Log.e(TAG, "서버 오류: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "서버 통신 예외 발생: " + e.getMessage(), e);
            }
        }).start();
    }
    
    private void updateGroupCountUI(JSONObject groupData) {
        try {
            if (groupData.has("data") && !groupData.isNull("data")) {
                JSONObject data = groupData.getJSONObject("data");
                if (data.has("groups") && !data.isNull("groups")) {
                    org.json.JSONArray groups = data.getJSONArray("groups");
                    int groupCount = groups.length();
                    
                    Log.d(TAG, "서버에서 가져온 그룹 수: " + groupCount);
                    groupCountTextView.setText("소속 그룹 수: " + groupCount + "개");
                    
                    // 그룹 수 로컬 저장
                    SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("groupCount", groupCount);
                    editor.apply();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "그룹 정보 업데이트 오류: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        try {
            if (requestCode == REQUEST_SMBTI_TEST && resultCode == RESULT_OK && data != null) {
                String smbtiResult = data.getStringExtra("smbti_result");
                if (smbtiResult != null && !smbtiResult.isEmpty()) {
                    // SMBTI 결과 저장
                    try {
                        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("mbti", smbtiResult);
                        editor.apply();
                        Log.d(TAG, "SMBTI 결과 저장 성공: " + smbtiResult);
                        
                        // UI 업데이트
                        mbtiTextView.setText("MBTI: " + smbtiResult);
                    } catch (Exception e) {
                        Log.e(TAG, "SMBTI 결과 저장 실패: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onActivityResult 오류: " + e.getMessage());
        }
    }
    
    // 로그아웃 기능
    private void logout() {
        // 로그인 정보 삭제
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // 모든 저장된 데이터 삭제
        editor.commit(); // apply 대신 commit 사용
        
        // 로그아웃 메시지 표시
        Toast.makeText(MainActivity.this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();
        
        // 로그인 화면으로 이동
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // 액티비티 스택 초기화
        startActivity(intent);
        finish();
    }
}
