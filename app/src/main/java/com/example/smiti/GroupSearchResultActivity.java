package com.example.smiti;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smiti.adapter.GroupAdapter;
import com.example.smiti.model.Group;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroupSearchResultActivity extends AppCompatActivity implements GroupAdapter.OnGroupClickListener {

    private static final String TAG = "GroupSearchResult";
    private static final String BASE_URL = "http://202.31.246.51:80";
    private static final String PREF_NAME = "LoginPrefs";

    private String searchQuery;
    private String userEmail;
    private String userName;
    private String userSmbti;

    private RecyclerView recyclerView;
    private GroupAdapter groupAdapter;
    private List<Group> groupList;
    private ProgressBar progressBar;
    private TextView searchQueryTextView;
    private LinearLayout noResultsLayout;
    private Button createGroupButton;
    private ImageButton backButton;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 상태 바 설정
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        
        setContentView(R.layout.activity_group_search_result);

        // 검색어 가져오기
        searchQuery = getIntent().getStringExtra("query");
        if (searchQuery == null || searchQuery.isEmpty()) {
            Toast.makeText(this, "검색어가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 사용자 정보 로드
        loadUserData();

        // 뷰 초기화
        initViews();
        
        // 뒤로가기 버튼 설정
        backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> onBackPressed());

        // 검색어 표시
        searchQueryTextView.setText(getString(R.string.search_result_title, searchQuery));

        // 하단 네비게이션 설정
        setupBottomNavigation();

        // RecyclerView 설정
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        groupList = new ArrayList<>();
        groupAdapter = new GroupAdapter(this, groupList, this);
        recyclerView.setAdapter(groupAdapter);

        // 그룹 생성 버튼 설정
        createGroupButton.setOnClickListener(v -> {
            createAIGroup();
        });

        // 검색 시작
        searchGroups();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        searchQueryTextView = findViewById(R.id.tv_search_query);
        noResultsLayout = findViewById(R.id.layout_no_results);
        createGroupButton = findViewById(R.id.btn_create_group);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void loadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userEmail = sharedPreferences.getString("email", "");
        userName = sharedPreferences.getString("name", "사용자");
        userSmbti = sharedPreferences.getString("mbti", "");
    }

    private void searchGroups() {
        showLoading(true);
        
        // 먼저 MBTI 궁합 점수를 포함한 그룹 목록 조회
        String url = BASE_URL + "/groups/smbti-scores?email=" + userEmail;
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(GroupSearchResultActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        android.util.Log.d(TAG, "API 응답: " + responseData);
                        
                        JSONObject jsonObject = new JSONObject(responseData);
                        
                        // 그룹 목록 파싱
                        parseGroups(jsonObject);
                        
                        // 검색어에 맞는 그룹 필터링
                        filterGroupsByQuery();
                        
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(GroupSearchResultActivity.this, "데이터 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(GroupSearchResultActivity.this, "서버 응답 오류: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void parseGroups(JSONObject jsonObject) throws JSONException {
        List<Group> allGroups = new ArrayList<>();
        
        if (jsonObject.has("data")) {
            JSONObject data = jsonObject.getJSONObject("data");
            if (data.has("groups")) {
                JSONArray groupsArray = data.getJSONArray("groups");
                
                for (int i = 0; i < groupsArray.length(); i++) {
                    JSONObject groupObject = groupsArray.getJSONObject(i);
                    
                    String id = groupObject.optString("group_id", "");
                    String name = groupObject.optString("group_name", "");
                    String description = groupObject.optString("description", "그룹 설명이 없습니다.");
                    int memberCount = groupObject.optInt("member_count", 0);
                    String category = groupObject.optString("category", "기타");
                    
                    // compatibility_score 필드 로깅 및 적절한 기본값 설정
                    int mbtiScore;
                    if (groupObject.has("compatibility_score")) {
                        mbtiScore = groupObject.optInt("compatibility_score", 50);
                        android.util.Log.d(TAG, "그룹 " + name + "의 궁합 점수: " + mbtiScore);
                    } else {
                        // 사용자의 SMBTI에 따라 랜덤한 궁합 점수 생성 (테스트용)
                        mbtiScore = new java.util.Random().nextInt(60) + 40; // 40-99 사이의 값
                        android.util.Log.d(TAG, "그룹 " + name + "의 궁합 점수가 없어 임의 생성: " + mbtiScore);
                    }
                    
                    Group group = new Group(id, name, description, memberCount, category, mbtiScore);
                    allGroups.add(group);
                }
            }
        }
        
        this.groupList = allGroups;
    }
    
    private void filterGroupsByQuery() {
        List<Group> filteredGroups = new ArrayList<>();
        
        // 검색어가 그룹 이름, 설명, 카테고리에 포함되어 있으면 필터링
        for (Group group : groupList) {
            if (group.getName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                    group.getDescription().toLowerCase().contains(searchQuery.toLowerCase()) ||
                    group.getCategory().toLowerCase().contains(searchQuery.toLowerCase())) {
                filteredGroups.add(group);
                // MBTI 점수 로깅 추가
                android.util.Log.d(TAG, "필터된 그룹: " + group.getName() + ", MBTI 점수: " + group.getMbtiScore());
            }
        }
        
        // MBTI 점수에 따라 내림차순 정렬 (높은 점수가 먼저 표시)
        java.util.Collections.sort(filteredGroups, (g1, g2) -> Double.compare(g2.getMbtiScore(), g1.getMbtiScore()));
        
        // 필터링된 결과를 UI에 표시
        final List<Group> finalFilteredGroups = filteredGroups;
        runOnUiThread(() -> {
            showLoading(false);
            
            if (finalFilteredGroups.isEmpty()) {
                showNoResults(true);
                android.util.Log.d(TAG, "검색 결과가 없습니다.");
            } else {
                showNoResults(false);
                android.util.Log.d(TAG, "검색 결과 " + finalFilteredGroups.size() + "개 표시");
                groupAdapter.updateData(finalFilteredGroups);
            }
        });
    }
    
    private void createAIGroup() {
        showLoading(true);
        showNoResults(false);
        
        // 사용자 정보 확인
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }
        
        // AI 그룹 추천 및 생성 API 호출
        String url = BASE_URL + "/groups/recommend";
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)  // AI 응답 시간이 길 수 있으므로 타임아웃 증가
                .build();
        
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", userEmail);
            jsonBody.put("smbti", userSmbti != null ? userSmbti : "");
            jsonBody.put("name", userName != null ? userName : "사용자");
            jsonBody.put("user_request", searchQuery);
        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(GroupSearchResultActivity.this, "요청 데이터 생성 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), jsonBody.toString());
        
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(GroupSearchResultActivity.this, "네트워크 오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // 연결 오류 시에도 기본 그룹 생성
                    createDefaultGroup();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : null;
                
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    if (response.isSuccessful() && responseBody != null && !responseBody.isEmpty()) {
                        try {
                            JSONObject jsonObject = new JSONObject(responseBody);
                            
                            // 새 그룹 정보 파싱
                            if (jsonObject.has("data") && !jsonObject.isNull("data")) {
                                JSONObject data = jsonObject.getJSONObject("data");
                                
                                String id = data.optString("group_id", "");
                                String name = data.optString("group_name", "");
                                String description = data.optString("description", "AI가 생성한 그룹입니다.");
                                int memberCount = data.optInt("member_count", 1);
                                String category = data.optString("category", "AI 추천");
                                int mbtiScore = data.optInt("compatibility_score", 90);
                                
                                // 그룹 ID가 유효한지 확인
                                if (id != null && !id.isEmpty() && name != null && !name.isEmpty()) {
                                    Group newGroup = new Group(id, name, description, memberCount, category, mbtiScore);
                                    
                                    List<Group> updatedList = new ArrayList<>();
                                    updatedList.add(newGroup);
                                    groupAdapter.updateData(updatedList);
                                    showNoResults(false);
                                    Toast.makeText(GroupSearchResultActivity.this, "AI가 새 그룹을 추천했습니다!", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                            
                            // 데이터가 없거나 유효하지 않은 경우 기본 그룹 생성
                            createDefaultGroup();
                            
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(GroupSearchResultActivity.this, "데이터 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                            createDefaultGroup();
                        }
                    } else {
                        String errorMessage = "서버 응답 오류: " + response.code();
                        Toast.makeText(GroupSearchResultActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        createDefaultGroup();
                    }
                });
            }
        });
    }
    
    // 기본 그룹 생성 (서버 오류 등으로 AI 그룹 생성이 실패한 경우)
    private void createDefaultGroup() {
        try {
            // 고유한 임시 ID 생성
            String tempId = "local_" + System.currentTimeMillis();
            
            // 검색어 기반 그룹 이름 생성
            String groupName = searchQuery + " 그룹";
            if (groupName.length() > 30) {
                groupName = groupName.substring(0, 27) + "...";
            }
            
            // 기본 그룹 정보 설정
            String description = "'" + searchQuery + "'에 대한 새로운 그룹입니다. 관심 있는 분들과 함께 대화를 나눠보세요.";
            
            Group defaultGroup = new Group(
                    tempId,
                    groupName,
                    description,
                    1,  // 현재 사용자가 첫 번째 멤버
                    "일반",
                    85  // 기본 궁합 점수
            );
            
            List<Group> updatedList = new ArrayList<>();
            updatedList.add(defaultGroup);
            groupAdapter.updateData(updatedList);
            showNoResults(false);
            
            Toast.makeText(GroupSearchResultActivity.this, "새 그룹을 생성했습니다!", Toast.LENGTH_SHORT).show();
            
            // 로컬에서 생성한 그룹 정보를 서버에 등록하는 API 호출 (비동기)
            registerLocalGroup(defaultGroup);
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(GroupSearchResultActivity.this, "그룹 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    
    // 로컬에서 생성한 그룹을 서버에 등록
    private void registerLocalGroup(Group group) {
        String url = BASE_URL + "/groups";
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", userEmail);
            jsonBody.put("group_name", group.getName());
            jsonBody.put("description", group.getDescription());
            jsonBody.put("category", group.getCategory());
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), jsonBody.toString());
        
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 실패해도 UI에는 이미 그룹이 표시되어 있으므로 조용히 처리
                e.printStackTrace();
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        
                        if (jsonResponse.has("data") && !jsonResponse.isNull("data")) {
                            JSONObject data = jsonResponse.getJSONObject("data");
                            final String newGroupId = data.optString("group_id", "");
                            
                            // 새로 할당된 그룹 ID가 있으면 UI 업데이트
                            if (!newGroupId.isEmpty()) {
                                runOnUiThread(() -> {
                                    Group updatedGroup = new Group(
                                            newGroupId,
                                            group.getName(),
                                            group.getDescription(),
                                            group.getMemberCount(),
                                            group.getCategory(),
                                            group.getMbtiScore()
                                    );
                                    
                                    List<Group> currentGroups = new ArrayList<>();
                                    currentGroups.add(updatedGroup);
                                    groupAdapter.updateData(currentGroups);
                                });
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
    
    private void showNoResults(boolean show) {
        noResultsLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onJoinClick(Group group) {
        joinGroup(group.getId());
    }

    @Override
    public void onGroupClick(Group group) {
        // 그룹 상세 정보 조회
        getGroupDetail(group.getId());
    }
    
    private void getGroupDetail(String groupId) {
        showLoading(true);
        
        // 그룹 상세 정보 API 호출
        String url = BASE_URL + "/groups/" + groupId;
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(GroupSearchResultActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : null;
                
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    if (response.isSuccessful() && responseData != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(responseData);
                            
                            if (jsonObject.has("data") && !jsonObject.isNull("data")) {
                                JSONObject data = jsonObject.getJSONObject("data");
                                
                                // 그룹 상세 정보 표시
                                displayGroupDetail(data);
                            } else {
                                Toast.makeText(GroupSearchResultActivity.this, 
                                        "그룹 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(GroupSearchResultActivity.this, 
                                    "데이터 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(GroupSearchResultActivity.this, 
                                "그룹 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    
    private void displayGroupDetail(JSONObject groupData) throws JSONException {
        String groupId = groupData.optString("group_id", "");
        String groupName = groupData.optString("group_name", "");
        String description = groupData.optString("description", "");
        int memberCount = groupData.optInt("member_count", 0);
        
        // 그룹 상세 정보를 Dialog로 표시
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(groupName);
        
        // 메시지 구성
        StringBuilder message = new StringBuilder();
        message.append("설명: ").append(description).append("\n\n");
        message.append("멤버 수: ").append(memberCount).append("명\n");
        
        builder.setMessage(message.toString());
        
        // 그룹 참여 버튼
        builder.setPositiveButton("그룹 참여", (dialog, which) -> {
            joinGroup(groupId);
        });
        
        // 취소 버튼
        builder.setNegativeButton("취소", (dialog, which) -> {
            dialog.dismiss();
        });
        
        builder.show();
    }
    
    private void joinGroup(String groupId) {
        showLoading(true);
        
        // 그룹 참여 API 호출 (업데이트된 경로)
        String url = BASE_URL + "/groups/" + groupId + "/users";
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", userEmail);
            jsonBody.put("group_id", Integer.parseInt(groupId));
            jsonBody.put("group_name", "");  // 선택적 필드
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // groupId가 정수로 변환할 수 없는 형식(예: local_로 시작하는 임시 ID)인 경우 처리
            e.printStackTrace();
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(GroupSearchResultActivity.this, 
                    "유효하지 않은 그룹 ID입니다.", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), jsonBody.toString());
        
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(GroupSearchResultActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (response.isSuccessful()) {
                        Toast.makeText(GroupSearchResultActivity.this, "그룹에 참여했습니다.", Toast.LENGTH_SHORT).show();
                        // 채팅 화면으로 이동
                        Intent intent = new Intent(GroupSearchResultActivity.this, ChatActivity.class);
                        intent.putExtra("group_id", groupId);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(GroupSearchResultActivity.this, "그룹 참여에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // 하단 네비게이션 설정 메서드 추가
    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_search);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.navigation_home) {
                startActivity(new Intent(GroupSearchResultActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_search) {
                // 이미 그룹 검색 관련 화면에 있으므로 아무것도 하지 않음
                return true;
            } else if (id == R.id.navigation_chat) {
                startActivity(new Intent(GroupSearchResultActivity.this, ChatActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_board) {
                startActivity(new Intent(GroupSearchResultActivity.this, BoardActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_profile) {
                startActivity(new Intent(GroupSearchResultActivity.this, ProfileActivity.class));
                finish();
                return true;
            }
            
            return false;
        });
    }
} 