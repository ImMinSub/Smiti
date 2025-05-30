package com.example.smiti;

import androidx.appcompat.app.AppCompatActivity;
// import androidx.cardview.widget.CardView;

import android.app.AlertDialog; // AlertDialog 추가
import android.content.DialogInterface; // DialogInterface 추가
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType; // EditText InputType 추가
import android.util.Log;
import android.view.View;
import android.widget.EditText; // EditText 추가
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String PROFILE_IMAGE_FILENAME = "profile_image.jpg";
    private static final String KEY_QUOTE = "quote"; // 상태 메시지 SharedPreferences 키
    private static final int REQUEST_CODE_GALLERY = 1;

    private ImageView profileImageView;
    private TextView tvProfileName;
    private TextView tvProfileQuote; // 상태 메시지 TextView
    private TextView tvStudyTimeValue;
    private ImageButton btnEditStudyTime;

    private RelativeLayout layoutStudyAlgo;
    private RelativeLayout layoutStudyJava;

    private RelativeLayout btnProfileNotificationSettings;
    private RelativeLayout btnProfileEditProfile;
    private RelativeLayout btnProfileStudyEnvSettings;
    private RelativeLayout btnProfileAccountManagement;
    private RelativeLayout btnProfileBlockedAccounts;
    private RelativeLayout btnProfileLogout;

    private final String[] DAYS = {"월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"};
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
        profileImageView = findViewById(R.id.profile_image);
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileQuote = findViewById(R.id.tv_profile_quote);
        tvStudyTimeValue = findViewById(R.id.tv_study_time_value);
        btnEditStudyTime = findViewById(R.id.btn_edit_study_time);

        layoutStudyAlgo = findViewById(R.id.layout_study_algo);
        layoutStudyJava = findViewById(R.id.layout_study_java);

        btnProfileNotificationSettings = findViewById(R.id.btn_profile_notification_settings);
        btnProfileEditProfile = findViewById(R.id.btn_profile_edit_profile);
        btnProfileStudyEnvSettings = findViewById(R.id.btn_profile_study_env_settings);
        btnProfileAccountManagement = findViewById(R.id.btn_profile_account_management);
        btnProfileBlockedAccounts = findViewById(R.id.btn_profile_blocked_accounts);
        btnProfileLogout = findViewById(R.id.btn_profile_logout);
    }

    private void setupClickListeners() {
        profileImageView.setOnClickListener(v -> openGallery());

        // 상태 메시지 TextView 클릭 리스너 추가
        tvProfileQuote.setOnClickListener(v -> showStatusMessageDialog());

        btnEditStudyTime.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, StudyTimeActivity.class);
            startActivity(intent);
        });

        layoutStudyAlgo.setOnClickListener(v -> showToast("알고리즘 마스터 스터디 클릭됨"));
        layoutStudyJava.setOnClickListener(v -> showToast("JAVA 스터디 클릭됨"));

        btnProfileNotificationSettings.setOnClickListener(v -> showToast("알림 설정 클릭됨"));
        btnProfileEditProfile.setOnClickListener(v -> showToast("프로필 수정 클릭됨 (프로필 이미지 변경은 이미지 직접 클릭)"));
        btnProfileStudyEnvSettings.setOnClickListener(v -> showToast("스터디 환경 설정 페이지로 이동합니다."));
        btnProfileAccountManagement.setOnClickListener(v -> showToast("계정 관리 페이지로 이동합니다."));
        btnProfileBlockedAccounts.setOnClickListener(v -> showToast("차단된 계정 페이지로 이동합니다."));
        btnProfileLogout.setOnClickListener(v -> logout());
    }

    // 상태 메시지 입력 다이얼로그 표시
    private void showStatusMessageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("상태 메시지 설정");

        // EditText를 다이얼로그에 추가
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("상태 메시지를 입력하세요...");
        // 현재 상태 메시지를 EditText에 기본값으로 설정
        input.setText(tvProfileQuote.getText().toString().equals("상태 메시지를 설정해주세요.") ? "" : tvProfileQuote.getText().toString());
        builder.setView(input);

        // 저장 버튼 설정
        builder.setPositiveButton("저장", (dialog, which) -> {
            String newMessage = input.getText().toString().trim();
            if (newMessage.isEmpty()) {
                // 비어있다면 기본 메시지로 설정하거나, 사용자에게 알림
                tvProfileQuote.setText("상태 메시지를 설정해주세요.");
                saveStatusMessage("상태 메시지를 설정해주세요."); // 또는 "" 저장 후 load 시 기본값 처리
            } else {
                tvProfileQuote.setText(newMessage);
                saveStatusMessage(newMessage);
            }
        });

        // 취소 버튼 설정
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // 상태 메시지를 SharedPreferences에 저장
    private void saveStatusMessage(String message) {
        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_QUOTE, message);
        editor.apply();
        Log.d(TAG, "상태 메시지 저장됨: " + message);
    }


    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                return true;
            } else if (id == R.id.navigation_chat) {
                startActivity(new Intent(ProfileActivity.this, ChatGroupListActivity.class));
                return true;
            } else if (id == R.id.navigation_board) {
                startActivity(new Intent(ProfileActivity.this, BoardActivity.class));
                return true;
            } else if (id == R.id.navigation_profile) {
                return true;
            }
            return false;
        });
    }

    private void loadProfileData() {
        loadUserDataFromLocal(); // 상태 메시지 로드를 포함
        loadProfileImageFromFilePath();
        loadStudyTimesFromLocal();

        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String email = sharedPreferences.getString("email", "");

        if (email != null && !email.isEmpty()) {
            fetchUserDataFromServer(email);
            fetchUserGroupsFromServer(email);
            fetchStudyTimesFromServer(email);
        } else {
            showToast("사용자 정보를 불러올 수 없습니다");
            tvProfileName.setText("사용자");
            // 이메일 정보가 없을 때 기본 상태 메시지 설정
            if (tvProfileQuote.getText().toString().isEmpty() || tvProfileQuote.getText().toString().equals("프로필을 완성해주세요.")) {
                tvProfileQuote.setText(sharedPreferences.getString(KEY_QUOTE, "상태 메시지를 설정해주세요."));
            }
        }
    }

    private void loadUserDataFromLocal() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String name = sharedPreferences.getString("name", "사용자");
        String mbti = sharedPreferences.getString("mbti", ""); // MBTI는 저장만 하고 직접 표시는 안 함
        String quote = sharedPreferences.getString(KEY_QUOTE, "상태 메시지를 설정해주세요."); // 저장된 상태 메시지 또는 기본값

        tvProfileName.setText(name);
        tvProfileQuote.setText(quote); // TextView에 상태 메시지 설정

        Log.d(TAG, "로컬에서 불러온 데이터 - 이름: " + name + ", MBTI: " + mbti + ", 인용구: " + quote);
    }

    private void loadProfileImageFromFilePath() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String imagePath = sharedPreferences.getString("profile_image_path", "");
        if (!imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if(imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (myBitmap != null) {
                    profileImageView.setImageBitmap(myBitmap);
                } else {
                    profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
                }
            } else {
                profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
            }
        } else {
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void fetchUserDataFromServer(String email) {
        new Thread(() -> {
            try {
                URL url = new URL("http://202.31.246.51:80/users/me?email=" + URLEncoder.encode(email, "UTF-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder responseBuilder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                    }
                    final String responseData = responseBuilder.toString();
                    try {
                        JSONObject responseJson = new JSONObject(responseData);
                        runOnUiThread(() -> updateUIWithUserData(responseJson));
                    } catch (Exception e) {
                        Log.e(TAG, "JSON 파싱 오류 (사용자): " + e.getMessage(), e);
                    }
                } else {
                    Log.e(TAG, "서버 오류 (사용자): " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "서버 통신 예외 발생 (사용자): " + e.getMessage(), e);
            }
        }).start();
    }

    private void updateUIWithUserData(JSONObject userData) {
        try {
            JSONObject dataToUse = userData.optJSONObject("data");
            if (dataToUse == null) dataToUse = userData;

            String name = dataToUse.optString("name", tvProfileName.getText().toString());
            String email = dataToUse.optString("email", getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString("email", ""));
            String mbti = dataToUse.optString("mbti", getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString("mbti", ""));

            // 서버 응답에 상태 메시지(quote) 필드가 있다면 가져오고, 없다면 기존 SharedPreferences 값 또는 TextView 값 유지
            String serverQuote = dataToUse.optString(KEY_QUOTE); // 서버에서 오는 키가 "quote"라고 가정
            String currentQuote = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(KEY_QUOTE, "상태 메시지를 설정해주세요.");

            String finalQuote = currentQuote; // 기본적으로 로컬/현재 값 유지
            if (serverQuote != null && !serverQuote.isEmpty()){ // 서버에서 유효한 값을 주면
                finalQuote = serverQuote; // 서버 값으로 업데이트
            }

            tvProfileName.setText(name);
            tvProfileQuote.setText(finalQuote); // 최종 결정된 인용구로 UI 업데이트

            SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
            editor.putString("name", name);
            editor.putString("email", email);
            editor.putString("mbti", mbti);
            editor.putString(KEY_QUOTE, finalQuote); // 최종 결정된 인용구 저장
            editor.apply();

            Log.d(TAG, "서버 데이터로 UI 업데이트 완료 - 이름: " + name + ", 인용구: " + finalQuote);

        } catch (Exception e) {
            Log.e(TAG, "UI 업데이트 오류 (사용자): " + e.getMessage(), e);
        }
    }

    // fetchUserGroupsFromServer, updateGroupCountToPrefs, loadStudyTimesFromLocal,
    // fetchStudyTimesFromServer, saveStudyTimesToLocal, formatTimeString,
    // getDayKoreanShort, openGallery, onActivityResult, copyImageToInternalStorage,
    // saveProfileImagePath, logout, showToast 메소드는 이전과 동일하게 유지합니다.
    // (간결성을 위해 이전 답변과 동일한 부분은 생략하고, 수정된 부분 위주로 표시했습니다.)

    private void fetchUserGroupsFromServer(String email) {
        new Thread(() -> {
            try {
                URL url = new URL("http://202.31.246.51:80/users/me/groups?email=" + URLEncoder.encode(email, "UTF-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                // ... (timeout 등 설정) ...
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder responseBuilder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                    }
                    JSONObject responseJson = new JSONObject(responseBuilder.toString());
                    updateGroupCountToPrefs(responseJson);
                }
            } catch (Exception e) { Log.e(TAG, "그룹 정보 조회 오류", e); }
        }).start();
    }

    private void updateGroupCountToPrefs(JSONObject groupData) {
        try {
            JSONObject dataToUse = groupData.optJSONObject("data");
            if (dataToUse == null) dataToUse = groupData;
            int groupCount = 0;
            if (dataToUse.has("groups") && dataToUse.get("groups") instanceof org.json.JSONArray) {
                groupCount = dataToUse.optJSONArray("groups").length();
            } else if (dataToUse.has("count")) {
                groupCount = dataToUse.optInt("count", 0);
            } else if (dataToUse.has("groupCount")) {
                groupCount = dataToUse.optInt("groupCount", 0);
            }
            SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
            editor.putInt("groupCount", groupCount);
            editor.apply();
        } catch (Exception e) { Log.e(TAG, "그룹 수 저장 오류", e); }
    }


    private void loadStudyTimesFromLocal() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean hasStudyTimes = sharedPreferences.getBoolean("has_study_times", false);
        if (hasStudyTimes) {
            StringBuilder studyTimeInfo = new StringBuilder();
            int displayedDays = 0;
            for (String dayKey : DAY_KEYS) {
                String dayName = getDayKoreanShort(dayKey);
                String timeString = sharedPreferences.getString("study_time_" + dayKey, "");
                if (!timeString.isEmpty()) {
                    if (displayedDays > 0) studyTimeInfo.append("\n");
                    studyTimeInfo.append(dayName).append(" ").append(formatTimeString(timeString));
                    displayedDays++;
                }
            }
            if (displayedDays > 0) {
                tvStudyTimeValue.setText(studyTimeInfo.toString().trim());
            } else {
                tvStudyTimeValue.setText("설정된 스터디 시간이 없습니다.");
            }
        } else {
            tvStudyTimeValue.setText("스터디 가능 시간을 설정해주세요.");
        }
    }

    private String getDayKoreanShort(String dayKey) {
        for(int i=0; i < DAY_KEYS.length; i++) {
            if (DAY_KEYS[i].equalsIgnoreCase(dayKey)) {
                return DAYS[i].substring(0,1);
            }
        }
        return "";
    }

    private void fetchStudyTimesFromServer(String email) {
        RetrofitClient.getApiService().getAvailableTimes(email).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    Object dataField = apiResponse.getData();
                    if (dataField instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataMap = (Map<String, Object>) dataField;
                        Object availableTimesObj = dataMap.getOrDefault("available_times", dataMap.get("availableTimes"));
                        if (availableTimesObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, List<String>> availableTimes = (Map<String, List<String>>) availableTimesObj;
                            saveStudyTimesToLocal(availableTimes);
                            loadStudyTimesFromLocal();
                        } else { Log.d(TAG, "available_times 필드가 Map이 아님");}
                    } else { Log.d(TAG, "API 응답 data 필드가 Map이 아님");}
                } else { Log.e(TAG, "스터디 시간 조회 실패: " + response.code());}
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) { Log.e(TAG, "스터디 시간 API 호출 실패", t);}
        });
    }

    private void saveStudyTimesToLocal(Map<String, List<String>> studyTimes) {
        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
        boolean timesFound = false;
        for (String dayKey : DAY_KEYS) {
            List<String> times = studyTimes.get(dayKey);
            if (times != null && !times.isEmpty()) {
                editor.putString("study_time_" + dayKey, String.join(",", times));
                timesFound = true;
            } else {
                editor.putString("study_time_" + dayKey, "");
            }
        }
        editor.putBoolean("has_study_times", timesFound);
        editor.apply();
    }

    private String formatTimeString(String timeString) {
        if (timeString == null || timeString.isEmpty()) return "";
        return timeString.replace(",", ", ").replace("~", "-");
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try {
            startActivityForResult(intent, REQUEST_CODE_GALLERY);
        } catch (Exception e) {
            showToast("갤러리 앱을 열 수 없습니다. 권한을 확인해주세요.");
            Log.e(TAG, "갤러리 열기 실패", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK && data != null) {
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
                } catch (IOException e) {
                    Log.e(TAG, "이미지 복사 중 IO 오류", e);
                    showToast("이미지 처리 중 오류가 발생했습니다.");
                } catch (Exception e) {
                    Log.e(TAG, "이미지 처리 중 일반 오류", e);
                    showToast("이미지 처리 중 알 수 없는 오류가 발생했습니다.");
                }
            }
        }
    }

    private String copyImageToInternalStorage(Uri uri) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        File destinationFile;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File directory = getFilesDir();
            destinationFile = new File(directory, PROFILE_IMAGE_FILENAME);

            outputStream = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[4 * 1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            return destinationFile.getAbsolutePath();
        } finally {
            if (inputStream != null) try { inputStream.close(); } catch (IOException e) { Log.e(TAG, "InputStream 닫기 오류", e); }
            if (outputStream != null) try { outputStream.close(); } catch (IOException e) { Log.e(TAG, "OutputStream 닫기 오류", e); }
        }
    }

    private void saveProfileImagePath(String imagePath) {
        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
        editor.putString("profile_image_path", imagePath);
        editor.apply();
    }

    private void logout() {
        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        showToast("로그아웃 되었습니다");
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
