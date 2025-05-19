package com.example.smiti;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.RetrofitClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.RetrofitClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String PROFILE_IMAGE_FILENAME = "profile_image.jpg";

    private ImageButton albumButton;
    private TextView nameTextView, mbtiTextView, groupCountTextView;
    private TextView studyTimeTextView;
    private Button activityLogButton, groupSettingButton, accountManagementButton;
    private Button blockedAccountsButton, logoutButton;
    private ImageButton notificationButton, settingsButton;

    // 요일 한글 이름 정의
    private final String[] DAYS = {"월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"};
    // 영어로 된 요일 (API 요청용)
    private final String[] DAY_KEYS = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeViews();
        setupClickListeners();
        setupBottomNavigation();
        loadProfileData();
    }

    private void initializeViews() {
        // 상단 버튼
        notificationButton = findViewById(R.id.btn_notification);
        settingsButton = findViewById(R.id.btn_settings);

        // 중단 섹션
        albumButton = findViewById(R.id.btn_album);
        nameTextView = findViewById(R.id.tv_name);
        mbtiTextView = findViewById(R.id.tv_mbti);
        groupCountTextView = findViewById(R.id.tv_group_count);
        studyTimeTextView = findViewById(R.id.tv_study_time);

        // 하단 메뉴 버튼
        activityLogButton = findViewById(R.id.btn_activity_log);
        groupSettingButton = findViewById(R.id.btn_group_setting);
        accountManagementButton = findViewById(R.id.btn_account_management);
        blockedAccountsButton = findViewById(R.id.btn_blocked_accounts);
        logoutButton = findViewById(R.id.btn_logout);
    }

    private void setupClickListeners() {
        // 상단 버튼 이벤트
        notificationButton.setOnClickListener(v -> showToast("알림 버튼 클릭됨"));
        settingsButton.setOnClickListener(v -> showToast("설정 버튼 클릭됨"));

        // 프로필 이미지 클릭 (갤러리 열기)
        albumButton.setOnClickListener(v -> openGallery());

        // 하단 메뉴 버튼 이벤트
        activityLogButton.setOnClickListener(v -> {
            // 스터디 가능 시간 화면으로 이동
            Intent intent = new Intent(ProfileActivity.this, StudyTimeActivity.class);
            startActivity(intent);
        });
        groupSettingButton.setOnClickListener(v -> showToast("그룹 설정 페이지로 이동합니다."));
        accountManagementButton.setOnClickListener(v -> showToast("계정 관리 페이지로 이동합니다."));
        blockedAccountsButton.setOnClickListener(v -> showToast("차단된 계정 페이지로 이동합니다."));
        logoutButton.setOnClickListener(v -> logout());
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.navigation_home) {
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_chat) {
                Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_profile) {
                // 이미 프로필 화면에 있음
                return true;
            }
            
            return false;
        });
    }

    private void loadProfileData() {
        // 먼저 저장된 데이터로 UI 초기화
        loadUserDataFromLocal();
        loadProfileImageFromFilePath();
        loadStudyTimesFromLocal();
        
        // 서버에서 최신 데이터 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String email = sharedPreferences.getString("email", "");
        
        if (email != null && !email.isEmpty()) {
            fetchUserDataFromServer(email);
            fetchUserGroupsFromServer(email);  // 그룹 정보도 가져오기
            fetchStudyTimesFromServer(email);  // 서버에서 스터디 가능 시간 가져오기
        } else {
            showToast("사용자 정보를 불러올 수 없습니다");
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
                        albumButton.setImageBitmap(myBitmap);
                        Log.d(TAG, "저장된 프로필 이미지 로드 성공");
                    } else {
                        Log.e(TAG, "비트맵 디코딩 실패");
                        albumButton.setImageResource(R.drawable.ic_profile);
                    }
                } else {
                    Log.e(TAG, "프로필 이미지 파일이 존재하지 않음");
                    albumButton.setImageResource(R.drawable.ic_profile);
                }
            } else {
                Log.d(TAG, "저장된 프로필 이미지 경로 없음");
                albumButton.setImageResource(R.drawable.ic_profile);
            }
        } catch (Exception e) {
            Log.e(TAG, "프로필 이미지 로드 중 예외 발생", e);
            albumButton.setImageResource(R.drawable.ic_profile);
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

                // GET 요청이므로 본문 데이터 전송이 필요 없음
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
                        runOnUiThread(() -> showToast("응답 데이터 처리 오류"));
                    }
                } else {
                    Log.e(TAG, "서버 오류: " + responseCode);
                    runOnUiThread(() -> showToast("서버 연결 실패: " + responseCode));
                }
            } catch (Exception e) {
                Log.e(TAG, "서버 통신 예외 발생: " + e.getMessage(), e);
                runOnUiThread(() -> showToast("서버 연결 오류"));
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
                    userData.optJSONObject("data") : userData;
            
            if (dataToUse == null) {
                Log.e(TAG, "데이터 객체가 null입니다");
                return;
            }
            
            // 사용자 정보 추출
            String name = dataToUse.optString("name", "");
            String email = dataToUse.optString("email", "");
            
            // MBTI 값 찾기 - 여러 가능한 필드명 검색
            String personalityTypeValue = "";
            String[] possibleFields = {
                "personalityType", "mbti", "smbti", "personality_type", "MBTI", "SMBTI", 
                "personality", "personality_mbti", "mbti_type", "type"
            };
            
            for (String field : possibleFields) {
                if (dataToUse.has(field)) {
                    String value = dataToUse.optString(field, "");
                    if (!value.isEmpty()) {
                        personalityTypeValue = value;
                        break;
                    }
                }
            }
            
            int groupCountValue = dataToUse.optInt("groupCount", 0);

            Log.d(TAG, "서버에서 받은 이름: " + name);
            Log.d(TAG, "서버에서 받은 이메일: " + email);
            Log.d(TAG, "서버에서 받은 SMBTI: " + personalityTypeValue);
            Log.d(TAG, "서버에서 받은 그룹 수: " + groupCountValue);

            // UI 업데이트
            if (!name.isEmpty()) {
                nameTextView.setText("이름: " + name);
                
                // 서버에서 가져온 정보 로컬에 저장
                SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("name", name);
                if (!email.isEmpty()) editor.putString("email", email);
                if (!personalityTypeValue.isEmpty()) editor.putString("mbti", personalityTypeValue);
                editor.putInt("groupCount", groupCountValue);
                editor.apply();
            }

            if (!personalityTypeValue.isEmpty()) {
                mbtiTextView.setText("MBTI: " + personalityTypeValue);
            } else {
                // MBTI 값이 없으면 기존 값 유지 (로컬에 저장된 값이 있을 수 있음)
                SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                String savedMbti = sharedPreferences.getString("mbti", "");
                
                if (!savedMbti.isEmpty()) {
                    mbtiTextView.setText("MBTI: " + savedMbti);
                } else {
                    mbtiTextView.setText("MBTI: 미설정");
                }
            }

            groupCountTextView.setText("소속 그룹 수: " + groupCountValue + "개");
            
            Log.d(TAG, "UI 업데이트 완료");
        } catch (Exception e) {
            Log.e(TAG, "UI 업데이트 오류: " + e.getMessage(), e);
            showToast("UI 업데이트 실패");
        }
    }
    
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try {
            startActivityForResult(intent, 1);
        } catch (Exception e) {
            showToast("갤러리 앱을 열 수 없습니다.");
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    String imagePath = copyImageToInternalStorage(imageUri);
                    if (imagePath != null) {
                        saveProfileImagePath(imagePath);
                        loadProfileImageFromFilePath();
                        showToast("프로필 이미지가 변경되었습니다.");
                    } else {
                        showToast("이미지 저장에 실패했습니다.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "이미지 처리 오류", e);
                    showToast("이미지 처리 중 오류가 발생했습니다.");
                }
            }
        }
    }
    
    private String copyImageToInternalStorage(Uri uri) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            File directory = getFilesDir();
            File destinationFile = new File(directory, PROFILE_IMAGE_FILENAME);

            outputStream = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            return destinationFile.getAbsolutePath();
        } finally {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
        }
    }
    
    private void saveProfileImagePath(String imagePath) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("profile_image_path", imagePath);
        editor.apply();
    }
    
    private void logout() {
        // 로그인 정보 삭제
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
        
        showToast("로그아웃 되었습니다");
        
        // 로그인 화면으로 이동
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void fetchUserGroupsFromServer(String email) {
        Log.d(TAG, "서버에 사용자 그룹 정보 요청 시작: " + email);
        
        new Thread(() -> {
            try {
                // URL 설정 - GET 요청을 위한 쿼리 파라미터 추가
                URL url = new URL("http://202.31.246.51:80/users/me/groups?email=" + URLEncoder.encode(email, "UTF-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 요청 설정
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                // GET 요청이므로 본문 데이터 전송이 필요 없음
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
                        runOnUiThread(() -> showToast("그룹 데이터 처리 오류"));
                    }
                } else {
                    Log.e(TAG, "서버 오류: " + responseCode);
                    runOnUiThread(() -> showToast("그룹 정보 조회 실패: " + responseCode));
                }
            } catch (Exception e) {
                Log.e(TAG, "서버 통신 예외 발생: " + e.getMessage(), e);
                runOnUiThread(() -> showToast("그룹 정보 조회 오류"));
            }
        }).start();
    }
    
    private void updateGroupCountUI(JSONObject groupData) {
        try {
            Log.d(TAG, "그룹 정보 UI 업데이트 시작");
            
            if (groupData == null) {
                Log.e(TAG, "groupData가 null입니다");
                return;
            }
            
            // 데이터 추출 (data 필드 또는 최상위)
            JSONObject dataToUse = groupData.has("data") ? 
                    groupData.optJSONObject("data") : groupData;
            
            if (dataToUse == null) {
                Log.e(TAG, "데이터 객체가 null입니다");
                return;
            }
            
            // 그룹 배열 추출
            int groupCount = 0;
            if (dataToUse.has("groups")) {
                try {
                    org.json.JSONArray groupsArray = dataToUse.getJSONArray("groups");
                    groupCount = groupsArray.length();
                } catch (Exception e) {
                    Log.e(TAG, "그룹 배열 추출 오류", e);
                }
            }
            
            Log.d(TAG, "서버에서 받은 그룹 수: " + groupCount);

            // UI 업데이트 및 로컬 저장
            groupCountTextView.setText("소속 그룹 수: " + groupCount + "개");
            
            SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("groupCount", groupCount);
            editor.apply();
            
            Log.d(TAG, "그룹 정보 UI 업데이트 완료");
        } catch (Exception e) {
            Log.e(TAG, "그룹 UI 업데이트 오류: " + e.getMessage(), e);
            showToast("그룹 UI 업데이트 실패");
        }
    }

    // 로컬에 저장된 스터디 가능 시간 로드
    private void loadStudyTimesFromLocal() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean hasStudyTimes = sharedPreferences.getBoolean("has_study_times", false);
        
        if (hasStudyTimes) {
            // 스터디 시간이 있는 요일 확인
            StringBuilder studyTimeInfo = new StringBuilder("스터디 가능 시간:\n");
            boolean hasAnyTimes = false;
            
            for (int i = 0; i < DAY_KEYS.length; i++) {
                String dayKey = DAY_KEYS[i];
                String dayName = DAYS[i];
                String timeString = sharedPreferences.getString("study_time_" + dayKey, "");
                
                if (!timeString.isEmpty()) {
                    studyTimeInfo.append(dayName).append(": ").append(formatTimeString(timeString)).append("\n");
                    hasAnyTimes = true;
                }
            }
            
            if (hasAnyTimes) {
                studyTimeTextView.setVisibility(View.VISIBLE);
                studyTimeTextView.setText(studyTimeInfo.toString());
            } else {
                studyTimeTextView.setVisibility(View.GONE);
            }
        } else {
            // 저장된 스터디 시간이 없음
            studyTimeTextView.setVisibility(View.GONE);
        }
    }
    
    // 서버에서 스터디 가능 시간 가져오기
    private void fetchStudyTimesFromServer(String email) {
        RetrofitClient.getApiService().getAvailableTimes(email).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    
                    try {
                        // 응답 데이터에서 available_times 필드 찾기
                        Object availableTimesObj = null;
                        
                        if (apiResponse.getData() != null) {
                            Map<String, Object> data = (Map<String, Object>) apiResponse.getData();
                            if (data.containsKey("available_times")) {
                                availableTimesObj = data.get("available_times");
                            }
                        }
                        
                        if (availableTimesObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, List<String>> availableTimes = (Map<String, List<String>>) availableTimesObj;
                            
                            // 서버에서 받은 스터디 시간을 로컬에 저장
                            saveStudyTimesToLocal(availableTimes);
                            
                            // UI 업데이트
                            loadStudyTimesFromLocal();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "스터디 시간 파싱 오류: " + e.getMessage());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "스터디 시간 조회 실패: " + t.getMessage());
            }
        });
    }
    
    // 스터디 시간 정보를 로컬에 저장
    private void saveStudyTimesToLocal(Map<String, List<String>> studyTimes) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // 요일별 스터디 시간 정보 저장
        for (String dayKey : DAY_KEYS) {
            List<String> times = studyTimes.get(dayKey);
            if (times != null && !times.isEmpty()) {
                // 쉼표로 구분된 문자열로 변환 (예: "09:00~10:00,14:00~16:00")
                editor.putString("study_time_" + dayKey, String.join(",", times));
            } else {
                editor.putString("study_time_" + dayKey, "");
            }
        }
        
        // 스터디 시간 설정 여부 플래그 저장
        editor.putBoolean("has_study_times", true);
        editor.apply();
    }
    
    // 시간 문자열 포맷팅 (09:00~10:00,14:00~16:00 -> 09:00~10:00, 14:00~16:00)
    private String formatTimeString(String timeString) {
        return timeString.replace(",", ", ");
    }
} 
