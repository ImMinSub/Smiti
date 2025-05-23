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
import androidx.appcompat.app.AlertDialog;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.converter.gson.GsonConverterFactory;

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
                // 다이얼로그를 보여주어 사용자가 직접 그룹명과 설명을 입력할 수 있게 함
                showCreateGroupDialog(keyword);
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
            
            // 일반 모드일 때만 그룹 생성 버튼 표시 (AI 모드에서는 표시하지 않음)
            if (!isAiModeEnabled) {
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
                Intent intent = new Intent(GroupSearchActivity.this, ChatGroupListActivity.class);
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
            // AI 모드에서는 groups/recommend API 사용
            String userSmbti = sharedPreferences.getString("smbti", "");
            String userName = sharedPreferences.getString("name", "");
            FindGroupRequest request = new FindGroupRequest(userEmail, userSmbti, userName, keyword);
            
            // AI 모드일 때 직접 배열 처리 방식으로 전환
            call = apiService.getGroupsWithSmbtiScore(userEmail);
            Log.d(TAG, "AI 모드에서 AI 추천 API 사용: " + keyword);
            
            // 별도로 AI 추천 API 직접 호출 처리
            directAiRecommend(request);
            return;
        } else {
            // 일반 검색 모드
            call = apiService.getGroupsWithSmbtiScore(userEmail);
        }
        
        // 일반 모드 호출 처리 (기존 코드)
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
                        
                        // 검색 결과 처리
                        if (!isAiModeEnabled) {
                            // 일반 모드에서는 클라이언트 측 필터링
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
                            // AI 모드에서는 서버가 이미 추천한 그룹을 모두 추가
                            for (Group group : allGroupsFromServer) {
                                String groupName = group.getName();
                                if (groupName == null) {
                                    continue;
                                }
                                
                                allGroups.add(group);
                                double score = group.getMbtiScore();
                                int roundedScore = (int) Math.round(score);
                                dataList.add(groupName + " [AI 추천: " + roundedScore + "점]");
                                Log.d(TAG, "AI 추천 그룹: " + groupName + ", 점수: " + score);
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
                                if (isAiModeEnabled) {
                                    dataList.add(groupName + " [AI 추천: " + roundedScore + "점]");
                                } else {
                                    dataList.add(groupName + " [궁합: " + roundedScore + "점]");
                                }
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

    // 그룹 생성 다이얼로그를 표시하는 메서드
    private void showCreateGroupDialog(String keyword) {
        // 다이얼로그용 레이아웃 생성
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        // 그룹명 입력창
        final EditText groupNameInput = new EditText(this);
        groupNameInput.setHint("그룹명");
        groupNameInput.setText(keyword + " 그룹"); // 기본값으로 검색어 + 그룹 설정
        layout.addView(groupNameInput);

        // 여백 추가
        TextView space = new TextView(this);
        space.setHeight(30);
        layout.addView(space);

        // 그룹 설명 입력창
        final EditText descriptionInput = new EditText(this);
        descriptionInput.setHint("그룹 설명");
        descriptionInput.setText(keyword + "에 관련된 그룹입니다."); // 기본값 설정
        layout.addView(descriptionInput);

        // AlertDialog 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("그룹 생성");
        builder.setView(layout);
        
        // 취소 버튼
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
        
        // 생성 버튼
        builder.setPositiveButton("생성", (dialog, which) -> {
            String groupName = groupNameInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            
            if (groupName.isEmpty()) {
                Toast.makeText(this, "그룹명을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 직접 입력한 정보로 그룹 생성
            createCustomGroup(groupName, description);
        });
        
        builder.show();
    }
    
    // 사용자가 입력한 정보로 그룹을 생성하는 메서드
    private void createCustomGroup(String groupName, String description) {
        showLoading(true);
        
        // 사용자 정보 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("email", "");
        
        if (userEmail.isEmpty()) {
            showLoading(false);
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 그룹 생성 요청 객체 생성
        CreateGroupRequest request = new CreateGroupRequest();
        request.setGroup_name(groupName);
        request.setDescription(description);
        request.setEmail(userEmail);
        request.setTopics(searchView.getText().toString().trim());
        request.setUseAi(false); // AI 사용하지 않음
        
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
                        "그룹이 성공적으로 생성되었습니다!", Toast.LENGTH_LONG).show();
                    
                    // 그룹 검색 결과 갱신
                    searchGroups(searchView.getText().toString().trim());
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
                Log.e(TAG, "Network error creating group: " + t.getMessage(), t);
                Toast.makeText(GroupSearchActivity.this, 
                    "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // AI 추천 API를 별도 처리하는 메서드
    private void directAiRecommend(FindGroupRequest request) {
        // 이 메서드는 배열 응답을 직접 처리합니다
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
                
        // GSON 설정 - 배열 응답을 처리하기 위한 특별 설정
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
                
        // recommendGroup 엔드포인트에 대한 별도 Retrofit 인스턴스 생성
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://202.31.246.51:80/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        
        // Group 배열을 직접 반환하는 임시 인터페이스 생성
        RecommendService recommendService = retrofit.create(RecommendService.class);
        
        // API 호출
        recommendService.recommendGroups(request).enqueue(new Callback<List<Group>>() {
            @Override
            public void onResponse(Call<List<Group>> call, Response<List<Group>> response) {
                showLoading(false);
                allGroups.clear();
                dataList.clear();
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Group> recommendedGroups = response.body();
                    
                    if (recommendedGroups != null && !recommendedGroups.isEmpty()) {
                        // 추천 그룹 처리
                        allGroups.addAll(recommendedGroups);
                        
                        // UI 업데이트
                        for (Group group : allGroups) {
                            String groupName = group.getName();
                            if (groupName != null) {
                                double score = group.getMbtiScore();
                                int roundedScore = (int) Math.round(score);
                                dataList.add(groupName + " [AI 추천: " + roundedScore + "점]");
                            }
                        }
                        
                        // MBTI 점수 기준 내림차순 정렬
                        java.util.Collections.sort(allGroups, (g1, g2) -> {
                            double score1 = g1.getMbtiScore();
                            double score2 = g2.getMbtiScore();
                            return Double.compare(score2, score1);
                        });
                        
                        // 정렬된 그룹에 맞게 dataList 재구성
                        dataList.clear();
                        for (Group group : allGroups) {
                            String groupName = group.getName();
                            if (groupName != null) {
                                double score = group.getMbtiScore();
                                int roundedScore = (int) Math.round(score);
                                dataList.add(groupName + " [AI 추천: " + roundedScore + "점]");
                            }
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                    
                    // 결과가 없을 경우
                    if (dataList.isEmpty()) {
                        showEmptyResult(true);
                    }
                } else {
                    Log.e(TAG, "AI 추천 API 오류: " + response.code());
                    Toast.makeText(GroupSearchActivity.this, "AI 추천 결과를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    showEmptyResult(true);
                }
            }
            
            @Override
            public void onFailure(Call<List<Group>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network request failed: " + t.getMessage(), t);
                
                if (retryCount < MAX_RETRY_COUNT) {
                    retryCount++;
                    Log.d(TAG, "재시도 " + retryCount + "/" + MAX_RETRY_COUNT);
                    
                    // 일반 검색으로 폴백
                    Toast.makeText(GroupSearchActivity.this, "AI 추천 서비스 접속 실패. 일반 검색을 사용합니다.", Toast.LENGTH_SHORT).show();
                    isAiModeEnabled = false;
                    updateAiButtonState();
                    
                    // 일반 검색 실행
                    String keyword = request.getUser_request();
                    Call<ApiResponse> fallbackCall = apiService.getGroupsWithSmbtiScore(request.getEmail());
                    fallbackCall.enqueue(new Callback<ApiResponse>() {
                        @Override
                        public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                            showLoading(false);
                            allGroups.clear();
                            dataList.clear();

                            if (response.isSuccessful() && response.body() != null) {
                                ApiResponse apiResponse = response.body();
                                List<Group> allGroupsFromServer = apiResponse.getGroups();
                                
                                if (allGroupsFromServer != null) {
                                    for (Group group : allGroupsFromServer) {
                                        String groupName = group.getName();
                                        if (groupName == null) continue;
                                        
                                        boolean nameMatches = groupName.toLowerCase().contains(keyword.toLowerCase());
                                        boolean descMatches = false;
                                        
                                        String description = group.getDescription();
                                        if (description != null) {
                                            descMatches = description.toLowerCase().contains(keyword.toLowerCase());
                                        }
                                        
                                        if (nameMatches || descMatches) {
                                            allGroups.add(group);
                                            double score = group.getMbtiScore();
                                            int roundedScore = (int) Math.round(score);
                                            dataList.add(groupName + " [궁합: " + roundedScore + "점]");
                                        }
                                    }
                                    
                                    // 정렬
                                    java.util.Collections.sort(allGroups, (g1, g2) -> 
                                        Double.compare(g2.getMbtiScore(), g1.getMbtiScore()));
                                    
                                    // 데이터 업데이트
                                    dataList.clear();
                                    for (Group group : allGroups) {
                                        String groupName = group.getName();
                                        if (groupName != null) {
                                            double score = group.getMbtiScore();
                                            int roundedScore = (int) Math.round(score);
                                            dataList.add(groupName + " [궁합: " + roundedScore + "점]");
                                        }
                                    }
                                }
                                
                                adapter.notifyDataSetChanged();
                                
                                if (dataList.isEmpty()) {
                                    showEmptyResult(true);
                                }
                            } else {
                                showEmptyResult(true);
                            }
                        }
                        
                        @Override
                        public void onFailure(Call<ApiResponse> call, Throwable t) {
                            showLoading(false);
                            showEmptyResult(true);
                        }
                    });
                } else {
                    // 최대 재시도 횟수 초과
                    Toast.makeText(GroupSearchActivity.this, "네트워크 연결 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    showEmptyResult(true);
                }
            }
        });
    }
    
    // 배열 응답을 처리하기 위한 임시 인터페이스
    private interface RecommendService {
        @POST("groups/recommend")
        Call<List<Group>> recommendGroups(@Body FindGroupRequest request);
    }
} 
