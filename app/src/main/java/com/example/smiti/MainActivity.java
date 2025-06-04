package com.example.smiti;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.ApiService;
import com.example.smiti.api.RetrofitClient;
import com.example.smiti.model.Todo;
import com.example.smiti.model.TodoCompletionRequest;
import com.example.smiti.adapter.TodoAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements TodoAdapter.OnTodoInteractionListener {

    private static final String TAG = "MainActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final int REQUEST_SMBTI_TEST = 1001;
    
    private TextView nameTextView, mbtiTextView, groupCountTextView;
    private ImageView profileImageView;

    // 할 일 관련 UI 요소 및 데이터
    private RecyclerView todoRecyclerView;
    private TodoAdapter todoAdapter;
    private List<Todo> todoList;
    private CalendarView calendarView;
    private FloatingActionButton fabAddTodo;
    private String userEmail;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 사용자 이메일 로드
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userEmail = prefs.getString("email", "");

        // 개인 정보 뷰 초기화
        initializeProfileViews();
        
        // 개인 정보 로드
        loadProfileData();

        // 할 일 뷰 초기화
        initializeTodoViews();

        // 현재 날짜 설정 (YYYY-MM-DD)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(new Date(calendarView.getDate()));

        // 할 일 목록 로드
        loadTodos(selectedDate);

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
                Intent intent = new Intent(MainActivity.this, HomeDashboardActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.navigation_chat) {
                // 채팅 그룹 목록 화면으로 이동
                Intent intent = new Intent(MainActivity.this, ChatGroupListActivity.class);
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
            Intent intent = new Intent(MainActivity.this, SmbtiIntroActivity.class);
            startActivity(intent);
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
        // 할 일 목록 갱신
        loadTodos(selectedDate);
    }
    
    private void initializeProfileViews() {
        profileImageView = findViewById(R.id.profile_image);
        nameTextView = findViewById(R.id.tv_name);
        mbtiTextView = findViewById(R.id.tv_mbti);
        groupCountTextView = findViewById(R.id.tv_group_count);
    }

    private void initializeTodoViews() {
        todoRecyclerView = findViewById(R.id.recycler_view_todos);
        calendarView = findViewById(R.id.calendarView);
        fabAddTodo = findViewById(R.id.fab_add_todo);

        todoList = new ArrayList<>();
        todoAdapter = new TodoAdapter(todoList, this);
        todoRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        todoRecyclerView.setAdapter(todoAdapter);

        // CalendarView 날짜 변경 리스너
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // 선택된 날짜 업데이트
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            Log.d(TAG, "선택된 날짜: " + selectedDate);
            // 해당 날짜의 할 일 목록 로드
            loadTodos(selectedDate);
        });

        // 할 일 추가 FAB 클릭 리스너
        fabAddTodo.setOnClickListener(v -> showAddTodoDialog());
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
            mbtiTextView.setText("SMBTI: " + (mbti.isEmpty() ? "미설정" : mbti));
            groupCountTextView.setText("소속 그룹 수: " + groupCount + "개");
            
            Log.d(TAG, "로컬에서 불러온 데이터 - 이름: " + name + ", MBTI: " + mbti + ", 그룹: " + groupCount);
        } catch (Exception e) {
            Log.e(TAG, "로컬 사용자 정보 로드 실패", e);
            nameTextView.setText("이름: 사용자");
            mbtiTextView.setText("SMBTI: 미설정");
            groupCountTextView.setText("소속 그룹 수: 0개");
        }
    }
    
    private void loadProfileImageFromFilePath() {
        Log.d(TAG, "프로필 이미지 로드 시도");
        try {
            SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String imagePath = sharedPreferences.getString("profile_image_path", "");
            
            // 먼저 사용자가 직접 설정한 이미지가 있는지 확인
            if (!imagePath.isEmpty()) {
                File imgFile = new File(imagePath);
                if(imgFile.exists()) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    if (myBitmap != null) {
                        profileImageView.setImageBitmap(myBitmap);
                        Log.d(TAG, "저장된 프로필 이미지 로드 성공");
                        return; // 사용자 설정 이미지를 로드했으므로 종료
                    } else {
                        Log.e(TAG, "비트맵 디코딩 실패");
                    }
                } else {
                    Log.e(TAG, "프로필 이미지 파일이 존재하지 않음");
                }
            } else {
                Log.d(TAG, "저장된 프로필 이미지 경로 없음");
            }
            
            // 사용자 설정 이미지가 없으면 SMBTI 이미지 사용
            loadSmbtiProfileImage(sharedPreferences);
            
        } catch (Exception e) {
            Log.e(TAG, "프로필 이미지 로드 중 예외 발생", e);
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }
    
    private void loadSmbtiProfileImage(SharedPreferences sharedPreferences) {
        try {
            String smbti = sharedPreferences.getString("mbti", "");
            if (!smbti.isEmpty()) {
                int smbtiImageResource = com.example.smiti.utils.SmbtiImageUtils.getProfileImageResource(smbti);
                profileImageView.setImageResource(smbtiImageResource);
                Log.d(TAG, "SMBTI 프로필 이미지 로드 성공: " + smbti);
            } else {
                Log.d(TAG, "SMBTI 정보 없음, 기본 이미지 사용");
                profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
            }
        } catch (Exception e) {
            Log.e(TAG, "SMBTI 프로필 이미지 로드 중 오류", e);
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
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
            if (dataToUse.has("smbti") && !dataToUse.isNull("smbti")) {
                String mbti = dataToUse.getString("smbti");
                Log.d(TAG, "서버에서 가져온 SMBTI: " + mbti);
                mbtiTextView.setText("SMBTI: " + mbti);
                
                // MBTI 로컬 저장
                SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("mbti", mbti);
                editor.apply();
            }
            
            // 그룹 수 표시
            if (dataToUse.has("group_count") && !dataToUse.isNull("group_count")) {
                int groupCount = dataToUse.getInt("group_count");
                Log.d(TAG, "서버에서 가져온 그룹 수 (users/me): " + groupCount);
                groupCountTextView.setText("소속 그룹 수: " + groupCount + "개");

                // 그룹 수 로컬 저장
                SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("groupCount", groupCount);
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
                        mbtiTextView.setText("SMBTI: " + smbtiResult);
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

    // 할 일 목록 로드
    private void loadTodos(String dateFilter) {
        if (userEmail.isEmpty()) {
            Log.e(TAG, "사용자 이메일이 없어 할 일을 불러올 수 없습니다.");
            Toast.makeText(this, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        Call<List<Todo>> call;

        if (dateFilter != null && !dateFilter.isEmpty()) {
            call = apiService.getTodosByUserAndDate(userEmail, dateFilter);
        } else {
            call = apiService.getTodosByUserEmail(userEmail);
        }

        call.enqueue(new Callback<List<Todo>>() {
            @Override
            public void onResponse(Call<List<Todo>> call, Response<List<Todo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Todo> fetchedTodos = response.body();
                    if (fetchedTodos != null) {
                        for (Todo todo : fetchedTodos) {
                            Log.d(TAG, "로드된 Todo: ID=" + todo.getTodoId() + ", Task=" + todo.getTask() + ", Completed=" + todo.isCompleted());
                        }
                        todoAdapter.updateTodos(fetchedTodos);
                        Log.d(TAG, "할 일 목록 로드 성공: " + fetchedTodos.size() + "개");
                    } else {
                        todoAdapter.updateTodos(new ArrayList<>()); // 빈 목록으로 업데이트
                        Log.e(TAG, "할 일 목록 서버 오류 또는 데이터 없음: " + response.code());
                        Toast.makeText(MainActivity.this, "할 일 목록 로드 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 서버 오류 또는 데이터 없음
                    todoAdapter.updateTodos(new ArrayList<>()); // 빈 목록으로 업데이트
                    Log.e(TAG, "할 일 목록 서버 오류 또는 데이터 없음: " + response.code());
                    Toast.makeText(MainActivity.this, "할 일 목록 로드 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Todo>> call, Throwable t) {
                Log.e(TAG, "할 일 목록 네트워크 오류: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "할 일 목록 네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                todoAdapter.updateTodos(new ArrayList<>()); // 네트워크 오류 시 빈 목록으로 업데이트
            }
        });
    }

    // 할 일 추가 다이얼로그 표시
    private void showAddTodoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("새 할 일 추가");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("할 일 내용을 입력하세요");
        builder.setView(input);

        builder.setPositiveButton("추가", (dialog, which) -> {
            String task = input.getText().toString().trim();
            if (task.isEmpty()) {
                Toast.makeText(this, "할 일 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            // 현재 선택된 날짜를 due_date로 사용
            addTodo(userEmail, task, selectedDate);
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // 할 일 추가 API 호출
    private void addTodo(String email, String task, String dueDate) {
        ApiService apiService = RetrofitClient.getApiService();
        Todo newTodo = new Todo(email, task, dueDate);
        apiService.addTodo(newTodo).enqueue(new Callback<Todo>() {
            @Override
            public void onResponse(Call<Todo> call, Response<Todo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(MainActivity.this, "할 일이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                    loadTodos(selectedDate);
                } else {
                    Toast.makeText(MainActivity.this, "할 일 추가 실패: 서버 오류 또는 응답 없음", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Todo> call, Throwable t) {
                Log.e(TAG, "할 일 추가 네트워크 오류: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "할 일 추가 네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // OnTodoInteractionListener 구현
    @Override
    public void onTodoCompletedChanged(Todo todo, boolean isCompleted) {
        ApiService apiService = RetrofitClient.getApiService();
        TodoCompletionRequest request = new TodoCompletionRequest(userEmail, isCompleted);

        apiService.updateTodoCompletion(todo.getTodoId(), request).enqueue(new Callback<Todo>() {
            @Override
            public void onResponse(Call<Todo> call, Response<Todo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Todo updatedTodo = response.body();
                    todo.setCompleted(updatedTodo.isCompleted());
                    Toast.makeText(MainActivity.this, "할 일 상태가 변경되었습니다.", Toast.LENGTH_SHORT).show();
                    todoAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "할 일 상태 변경 실패: 서버 오류 또는 응답 없음", Toast.LENGTH_SHORT).show();
                    todo.setCompleted(!isCompleted);
                    todoAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<Todo> call, Throwable t) {
                Log.e(TAG, "할 일 상태 변경 네트워크 오류: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "할 일 상태 변경 네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                todo.setCompleted(!isCompleted);
                todoAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onTodoEdit(Todo todo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("할 일 수정");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(todo.getTask()); // 현재 할 일 내용을 기본값으로 설정
        builder.setView(input);

        builder.setPositiveButton("수정", (dialog, which) -> {
            String updatedTask = input.getText().toString().trim();
            if (updatedTask.isEmpty()) {
                Toast.makeText(this, "할 일 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateTodo(todo, updatedTask);
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateTodo(Todo todo, String updatedTask) {
        ApiService apiService = RetrofitClient.getApiService();
        Todo updatedTodo = new Todo(todo.getTodoId(), userEmail, updatedTask, todo.getDueDate(), todo.isCompleted());

        apiService.updateTodo(todo.getTodoId(), updatedTodo).enqueue(new Callback<Todo>() {
            @Override
            public void onResponse(Call<Todo> call, Response<Todo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Todo responseTodo = response.body();
                    todo.setTask(responseTodo.getTask());
                    todo.setCompleted(responseTodo.isCompleted());
                    todo.setDueDate(responseTodo.getDueDate());
                    Toast.makeText(MainActivity.this, "할 일이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    todoAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "할 일 수정 실패: 서버 오류 또는 응답 없음", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Todo> call, Throwable t) {
                Log.e(TAG, "할 일 수정 네트워크 오류: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "할 일 수정 네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onTodoDelete(Todo todo) {
        new AlertDialog.Builder(this)
                .setTitle("할 일 삭제 확인")
                .setMessage("'" + todo.getTask() + "' 할 일을 정말 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteTodo(todo))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteTodo(Todo todo) {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.deleteTodo(todo.getTodoId(), userEmail).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "할 일이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    loadTodos(selectedDate);
                } else {
                    Log.e(TAG, "할 일 삭제 실패: " + response.code());
                    Toast.makeText(MainActivity.this, "할 일 삭제 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "할 일 삭제 네트워크 오류: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "할 일 삭제 네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
