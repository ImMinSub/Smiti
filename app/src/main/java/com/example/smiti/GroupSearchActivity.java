package com.example.smiti;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.ApiService;
import com.example.smiti.api.RetrofitClient;
import com.example.smiti.api.FindGroupRequest;
import com.example.smiti.api.CreateGroupRequest;
import com.example.smiti.model.Group;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public class GroupSearchActivity extends AppCompatActivity {

    private static final String TAG = "GroupSearchActivity";
    
    private EditText searchView;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    private List<Group> allGroups = new ArrayList<>();
    private ApiService apiService;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private ImageButton btnFilter;
    private ImageButton btnSearch;
    private AppCompatButton btnAi;
    private LinearLayout emptyResultLayout;
    private TextView tvEmptyMessage;
    private Button btnCreateGroup;
    
    private boolean isAiModeEnabled = false; // AI 모드 활성화 상태
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_DELAY_MS = 2000; // 2초 딜레이

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_search);

        // 시스템 UI 설정
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }

        // 뷰 초기화
        searchView = findViewById(R.id.searchView);
        listView = findViewById(R.id.listView);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnBack = findViewById(R.id.btn_back);
        btnFilter = findViewById(R.id.btn_filter);
        btnSearch = findViewById(R.id.btn_search);
        btnAi = findViewById(R.id.btn_ai);
        progressBar = findViewById(R.id.progress_bar);
        emptyResultLayout = findViewById(R.id.empty_result_layout);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
        btnCreateGroup = findViewById(R.id.btn_create_group);

        // 어댑터 설정
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);

        // API 서비스 초기화
        apiService = RetrofitClient.getApiService();
        
        // 하단 네비게이션 설정 (반드시 findViewById 이후에 호출)
        setupBottomNavigation();

        // 뒤로가기 버튼 리스너
        btnBack.setOnClickListener(v -> {
            finish();
        });

        // 필터 버튼 리스너
        btnFilter.setOnClickListener(v -> {
            Toast.makeText(this, "필터 기능 준비 중입니다.", Toast.LENGTH_SHORT).show();
        });
        
        // 검색 버튼 리스너
        btnSearch.setOnClickListener(v -> {
            String query = searchView.getText().toString();
            if (query != null && !query.trim().isEmpty()) {
                searchGroups(query.trim());
                // 키보드 숨기기
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
            } else {
                allGroups.clear();
                dataList.clear();
                adapter.notifyDataSetChanged();
                showEmptyResult(false);
                Toast.makeText(GroupSearchActivity.this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });
        
        // AI 버튼 리스너
        btnAi.setOnClickListener(v -> {
            isAiModeEnabled = !isAiModeEnabled;
            updateAiButtonState();
            
            // 현재 검색어로 다시 검색 (모드가 변경되었으므로)
            String currentQuery = searchView.getText().toString().trim();
            if (!currentQuery.isEmpty()) {
                searchGroups(currentQuery);
            }
        });
        
        // 그룹 생성 버튼 리스너
        btnCreateGroup.setOnClickListener(v -> {
            // 현재 검색어 가져오기
            String keyword = searchView.getText().toString().trim();
            if (!keyword.isEmpty()) {
                createAiGroup(keyword);
            } else {
                Toast.makeText(GroupSearchActivity.this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // 검색 뷰 리스너 설정 (Enter 키 처리)
        searchView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchView.getText().toString();
                if (query != null && !query.trim().isEmpty()) {
                    searchGroups(query.trim());
                } else {
                    allGroups.clear();
                    dataList.clear();
                    adapter.notifyDataSetChanged();
                    showEmptyResult(false);
                    Toast.makeText(GroupSearchActivity.this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
                }
                // 키보드 숨기기
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        // 텍스트 변경 리스너 설정
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 자동 검색 기능 제거
                if (s.toString().trim().isEmpty()) {
                    allGroups.clear();
                    dataList.clear();
                    adapter.notifyDataSetChanged();
                    showEmptyResult(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // EditText 설정
        searchView.setHint("검색하기"); // 힌트 텍스트 설정
        searchView.requestFocus(); // 자동으로 포커스 설정

        // 목록 아이템 클릭 시 그룹 상세 정보로 이동
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < allGroups.size()) {
                Group selectedGroup = allGroups.get(position);
                Intent intent = new Intent(GroupSearchActivity.this, GroupSearchResultActivity.class);
                intent.putExtra("GROUP_ID", selectedGroup.getId());
                intent.putExtra("GROUP_NAME", selectedGroup.getName());
                startActivity(intent);
            }
        });
        
        // 초기 AI 버튼 상태 설정
        updateAiButtonState();
    }
    
    // AI 버튼 상태 업데이트
    private void updateAiButtonState() {
        if (isAiModeEnabled) {
            btnAi.setBackgroundResource(R.drawable.bg_button_primary);
            btnAi.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            btnAi.setBackgroundResource(R.drawable.bg_button_outline);
            btnAi.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }
    
    // 검색 결과 없음 UI 표시
    private void showEmptyResult(boolean show) {
        if (show) {
            listView.setVisibility(View.GONE);
            emptyResultLayout.setVisibility(View.VISIBLE);
            
            // 검색 결과 없음 메시지 표시
            tvEmptyMessage.setText("검색된 결과가 없습니다.");
            
            // AI 모드일 때만 AI 그룹 생성 버튼 표시
            if (isAiModeEnabled) {
                btnCreateGroup.setVisibility(View.VISIBLE);
            } else {
                btnCreateGroup.setVisibility(View.GONE);
            }
        } else {
            listView.setVisibility(View.VISIBLE);
            emptyResultLayout.setVisibility(View.GONE);
        }
    }
    
    // 하단 네비게이션 설정
    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_search);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.navigation_home) {
                Intent intent = new Intent(GroupSearchActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_search) {
                // 이미 그룹 검색 화면에 있음
                return true;
            } else if (id == R.id.navigation_chat) {
                Intent intent = new Intent(GroupSearchActivity.this, ChatActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_board) {
                Intent intent = new Intent(GroupSearchActivity.this, BoardActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_profile) {
                Intent intent = new Intent(GroupSearchActivity.this, ProfileActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            
            return false;
        });
    }

    // 그룹 검색 메서드
    private void searchGroups(String keyword) {
        Log.d(TAG, "Searching groups with keyword: " + keyword + ", AI mode: " + isAiModeEnabled);
        showLoading(true);
        showEmptyResult(false);
        retryCount = 0; // 재시도 카운트 초기화

        searchGroupsWithRetry(keyword);
    }
    
    // 재시도 로직이 포함된 검색 메서드
    private void searchGroupsWithRetry(String keyword) {
        // 사용자 이메일 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("email", "");
        
        if (userEmail.isEmpty()) {
            showLoading(false);
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // OkHttp 클라이언트 타임아웃 설정 
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();
        
        // Retrofit 클라이언트 재설정
        apiService = RetrofitClient.getCustomApiService(client);

        // SMBTI 궁합 점수를 포함한 그룹 목록 API 호출
        Call<ApiResponse> call;
        if (isAiModeEnabled) {
            // AI 모드에서도 일반 검색과 같은 방식으로 처리
            call = apiService.getGroupsWithSmbtiScore(userEmail);
            // AI 모드에서도 동일한 메시지 처리
            Log.d(TAG, "AI 모드에서 일반 검색 API 사용: " + keyword);
        } else {
            // 일반 검색 모드
            call = apiService.getGroupsWithSmbtiScore(userEmail);
        }
        
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);
                allGroups.clear();
                dataList.clear();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    List<Group> allGroupsFromServer = apiResponse.getGroups();
                    
                    if (allGroupsFromServer != null) {
                        Log.d(TAG, "Received " + allGroupsFromServer.size() + " total groups with SMBTI scores");
                        
                        // 키워드로 필터링 (일반 모드에서만 클라이언트 측 필터링)
                        if (!isAiModeEnabled) {
                            for (Group group : allGroupsFromServer) {
                                String groupName = group.getName();
                                if (groupName == null) {
                                    // API 응답에서 그룹 이름이 누락된 경우 스킵
                                    continue;
                                }
                                
                                // 안전하게 필드 검사 - description이 null일 수 있음
                                boolean nameMatches = groupName.toLowerCase().contains(keyword.toLowerCase());
                                boolean descMatches = false;
                                
                                String description = group.getDescription();
                                if (description != null) {
                                    descMatches = description.toLowerCase().contains(keyword.toLowerCase());
                                }
                                
                                if (nameMatches || descMatches) {
                                    // 검색어가 포함된 그룹만 추가
                                    allGroups.add(group);
                                    double score = group.getMbtiScore();
                                    int roundedScore = (int) Math.round(score);
                                    dataList.add(groupName + " [궁합: " + roundedScore + "점]");
                                    Log.d(TAG, "그룹: " + groupName + ", SMBTI 점수: " + score);
                                }
                            }
                        } else {
                            // AI 모드에서도 검색어로 필터링 처리
                            for (Group group : allGroupsFromServer) {
                                String groupName = group.getName();
                                if (groupName == null) {
                                    // API 응답에서 그룹 이름이 누락된 경우 스킵
                                    continue;
                                }
                                
                                // 안전하게 필드 검사 - description이 null일 수 있음
                                boolean nameMatches = groupName.toLowerCase().contains(keyword.toLowerCase());
                                boolean descMatches = false;
                                
                                String description = group.getDescription();
                                if (description != null) {
                                    descMatches = description.toLowerCase().contains(keyword.toLowerCase());
                                }
                                
                                if (nameMatches || descMatches) {
                                    // 검색어가 포함된 그룹만 추가
                                    allGroups.add(group);
                                    double score = group.getMbtiScore();
                                    int roundedScore = (int) Math.round(score);
                                    dataList.add(groupName + " [궁합: " + roundedScore + "점]");
                                    Log.d(TAG, "그룹: " + groupName + ", SMBTI 점수: " + score);
                                }
                            }
                        }
                        
                        // MBTI 점수에 따라 내림차순 정렬 (높은 점수가 먼저 표시)
                        java.util.Collections.sort(allGroups, (g1, g2) -> {
                            // null 안전 비교
                            double score1 = g1.getMbtiScore();
                            double score2 = g2.getMbtiScore();
                            return Double.compare(score2, score1); // 내림차순 정렬
                        });
                        
                        // 정렬된 그룹에 맞게 dataList도 재구성
                        dataList.clear();
                        for (Group group : allGroups) {
                            String groupName = group.getName();
                            if (groupName != null) {
                                double score = group.getMbtiScore();
                                int roundedScore = (int) Math.round(score);
                                dataList.add(groupName + " [궁합: " + roundedScore + "점]");
                            }
                        }
                        
                        Log.d(TAG, "Filtered to " + allGroups.size() + " groups matching keyword: " + keyword);
                    } else {
                        Log.e(TAG, "Response successful, but groups list is null");
                        Toast.makeText(GroupSearchActivity.this, "데이터 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    int errorCode = response.code();
                    Log.e(TAG, "Error response: " + errorCode);
                    
                    // 서버 오류(5xx)면 재시도
                    if (errorCode >= 500 && retryCount < MAX_RETRY_COUNT) {
                        retryWithDelay(keyword);
                    } else {
                        Toast.makeText(GroupSearchActivity.this, "서버 오류: " + errorCode, Toast.LENGTH_SHORT).show();
                    }
                }
                
                adapter.notifyDataSetChanged();
                
                // 검색 결과가 없는 경우 처리
                if (dataList.isEmpty()) {
                    showEmptyResult(true);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network request failed: " + t.getMessage(), t);
                
                // 네트워크 오류일 경우 재시도
                if (retryCount < MAX_RETRY_COUNT) {
                    retryWithDelay(keyword);
                } else {
                    // 모든 재시도가 실패한 경우
                    allGroups.clear();
                    dataList.clear();
                    adapter.notifyDataSetChanged();
                    
                    // 오류 유형별 다른 메시지 표시
                    String errorMessage;
                    if (t instanceof java.net.SocketTimeoutException) {
                        errorMessage = "서버 응답 시간이 초과되었습니다. 나중에 다시 시도해주세요.";
                    } else if (t instanceof java.net.UnknownHostException) {
                        errorMessage = "인터넷 연결을 확인해주세요.";
                    } else if (t instanceof com.google.gson.JsonSyntaxException) {
                        errorMessage = "데이터 형식 오류가 발생했습니다. 개발팀에 문의해주세요.";
                    } else {
                        errorMessage = "네트워크 연결 오류: " + t.getMessage();
                    }
                    
                    Toast.makeText(GroupSearchActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    showEmptyResult(true); // 오류 시에도 결과 없음 UI 표시
                }
            }
        });
    }
    
    // 지연 후 재시도 메서드
    private void retryWithDelay(final String keyword) {
        retryCount++;
        Log.d(TAG, "재시도 " + retryCount + "/" + MAX_RETRY_COUNT);
        Toast.makeText(GroupSearchActivity.this, 
                "연결 재시도 중... (" + retryCount + "/" + MAX_RETRY_COUNT + ")", 
                Toast.LENGTH_SHORT).show();
                
        new android.os.Handler().postDelayed(() -> {
            if (!isFinishing()) {
                showLoading(true);
                searchGroupsWithRetry(keyword);
            }
        }, RETRY_DELAY_MS);
    }
    
    // 로딩 상태 표시 메서드
    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        
        // 로딩 중에는 검색창 비활성화
        if (searchView != null) {
            searchView.setEnabled(!isLoading);
        }
    }

    // AI를 이용한 그룹 자동 생성 메서드
    private void createAiGroup(String keyword) {
        showLoading(true);
        
        // 사용자 정보 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("email", "");
        String userName = sharedPreferences.getString("name", "사용자");
        
        if (userEmail.isEmpty()) {
            showLoading(false);
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 그룹 생성 요청 객체 생성
        CreateGroupRequest request = new CreateGroupRequest();
        request.setGroup_name(keyword + " 그룹");
        request.setDescription(keyword + "에 관련된 AI가 자동으로 생성한 그룹입니다.");
        request.setEmail(userEmail);
        request.setTopics(keyword);
        request.setUseAi(true); // AI 사용 표시
        
        // API 호출
        Call<ApiResponse> call = apiService.createGroup(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    
                    // 그룹 생성 성공 메시지 표시
                    Toast.makeText(GroupSearchActivity.this, 
                        "AI 그룹이 성공적으로 생성되었습니다!", Toast.LENGTH_LONG).show();
                    
                    // 그룹 검색 결과 갱신
                    searchGroups(keyword);
                } else {
                    int errorCode = response.code();
                    String errorMessage = "그룹 생성에 실패했습니다: " + errorCode;
                    
                    // 응답 바디에서 에러 메시지 추출 시도
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            errorMessage += " - " + errorBody;
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing error body", e);
                        }
                    }
                    
                    Log.e(TAG, errorMessage);
                    Toast.makeText(GroupSearchActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error creating AI group: " + t.getMessage(), t);
                Toast.makeText(GroupSearchActivity.this, 
                    "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
} 