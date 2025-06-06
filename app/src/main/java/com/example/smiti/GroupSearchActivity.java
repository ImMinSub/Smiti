package com.example.smiti;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.ApiService;
import com.example.smiti.api.CreateGroupRequest;
import com.example.smiti.api.FindGroupRequest;
import com.example.smiti.api.RetrofitClient;
import com.example.smiti.model.Group;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class GroupSearchActivity extends AppCompatActivity {

    private static final String TAG = "GroupSearchActivity";

    private EditText searchView;
    private ListView listView;
    private GroupAdapter adapter;
    private List<Group> allGroups = new ArrayList<>();
    private ApiService apiService;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private ImageButton btnFilter;
    private ImageButton btnSearch;
    private Spinner searchTypeSpinner;
    private LinearLayout emptyResultLayout;
    private TextView tvEmptyMessage;
    private Button btnCreateGroup;
    private LinearLayout searchSuggestionsLayout;

    private RecyclerView recentSearchesRecyclerView;
    private RecentSearchAdapter recentSearchAdapter;
    private List<String> recentSearchList;
    private TextView btnClearAllRecentSearches;
    private TextView tvNoRecentSearches;
    private SearchHistoryManager searchHistoryManager;

    private boolean isAiModeEnabled = false;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_search);

        // 시스템 UI 설정 (기존 코드 유지)
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


        searchView = findViewById(R.id.searchView);
        listView = findViewById(R.id.listView);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnBack = findViewById(R.id.btn_back);
        btnFilter = findViewById(R.id.btn_filter);
        btnSearch = findViewById(R.id.btn_search);
        searchTypeSpinner = findViewById(R.id.search_type_spinner);
        progressBar = findViewById(R.id.progress_bar);
        emptyResultLayout = findViewById(R.id.empty_result_layout);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
        btnCreateGroup = findViewById(R.id.btn_create_group);
        searchSuggestionsLayout = findViewById(R.id.search_suggestions_layout);

        recentSearchesRecyclerView = findViewById(R.id.recent_searches_recyclerview);
        btnClearAllRecentSearches = findViewById(R.id.btn_clear_all_recent_searches);
        tvNoRecentSearches = findViewById(R.id.tv_no_recent_searches);

        adapter = new GroupAdapter(this, allGroups);
        listView.setAdapter(adapter);

        apiService = RetrofitClient.getApiService();
        setupBottomNavigation();

        searchHistoryManager = new SearchHistoryManager(this);
        recentSearchList = new ArrayList<>();
        setupRecentSearchesRecyclerView();

        searchSuggestionsLayout.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        emptyResultLayout.setVisibility(View.GONE);

        btnBack.setOnClickListener(v -> finish());
        btnFilter.setOnClickListener(v -> Toast.makeText(this, "필터 기능 준비 중입니다.", Toast.LENGTH_SHORT).show());

        btnSearch.setOnClickListener(v -> {
            String query = searchView.getText().toString().trim();
            if (!query.isEmpty()) {
                searchHistoryManager.addSearchTerm(query);
                searchGroups(query);
                hideKeyboard();
            } else {
                Toast.makeText(GroupSearchActivity.this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.search_types, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchTypeSpinner.setAdapter(spinnerAdapter);
        searchTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedMode = parent.getItemAtPosition(position).toString();
                isAiModeEnabled = selectedMode.equals(getResources().getStringArray(R.array.search_types)[1]);
                Log.d(TAG, "Search mode changed. AI Mode: " + isAiModeEnabled);
                String currentQuery = searchView.getText().toString().trim();
                if (!currentQuery.isEmpty()) {
                    searchGroups(currentQuery);
                }
                if (emptyResultLayout.getVisibility() == View.VISIBLE) {
                    showEmptyResult(true);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                isAiModeEnabled = false;
            }
        });

        btnClearAllRecentSearches.setOnClickListener(v -> {
            searchHistoryManager.clearAllRecentSearches();
            loadRecentSearches();
        });

        btnCreateGroup.setOnClickListener(v -> {
            String keyword = searchView.getText().toString().trim();
            if (!keyword.isEmpty()) {
                Intent intent = new Intent(GroupSearchActivity.this, AddCardActivity.class);
                intent.putExtra("SEARCH_KEYWORD", keyword);
                startActivity(intent);
            } else {
                Toast.makeText(GroupSearchActivity.this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        searchView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchView.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchHistoryManager.addSearchTerm(query);
                    searchGroups(query);
                    hideKeyboard();
                    return true;
                } else {
                    Toast.makeText(GroupSearchActivity.this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        });

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    allGroups.clear();
                    adapter.notifyDataSetChanged();
                    listView.setVisibility(View.GONE);
                    emptyResultLayout.setVisibility(View.GONE);
                    searchSuggestionsLayout.setVisibility(View.VISIBLE);
                    loadRecentSearches();
                    showLoading(false);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchView.setHint("검색하기");
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < allGroups.size()) {
                Group selectedGroup = allGroups.get(position);
                Intent intent = new Intent(GroupSearchActivity.this, GroupDetailActivity.class);
                intent.putExtra(GroupDetailActivity.EXTRA_GROUP, (Serializable) selectedGroup); // Group이 Serializable을 구현해야 함
                intent.putExtra(GroupDetailActivity.EXTRA_IS_AI_MODE, isAiModeEnabled);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentSearches();
    }

    private void setupRecentSearchesRecyclerView() {
        recentSearchesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentSearchAdapter = new RecentSearchAdapter(recentSearchList, new RecentSearchAdapter.OnRecentSearchInteractionListener() {
            @Override
            public void onRecentSearchClicked(String query) {
                searchView.setText(query);
                searchView.setSelection(query.length());
                searchGroups(query);
                hideKeyboard();
            }

            @Override
            public void onRecentSearchDeleteClicked(String query) {
                searchHistoryManager.removeSearchTerm(query);
                loadRecentSearches();
            }
        });
        recentSearchesRecyclerView.setAdapter(recentSearchAdapter);
    }

    private void loadRecentSearches() {
        List<String> updatedSearches = searchHistoryManager.getRecentSearches();
        recentSearchAdapter.updateData(updatedSearches);

        if (updatedSearches.isEmpty()) {
            recentSearchesRecyclerView.setVisibility(View.GONE);
            tvNoRecentSearches.setVisibility(View.VISIBLE);
            btnClearAllRecentSearches.setVisibility(View.GONE);
        } else {
            recentSearchesRecyclerView.setVisibility(View.VISIBLE);
            tvNoRecentSearches.setVisibility(View.GONE);
            btnClearAllRecentSearches.setVisibility(View.VISIBLE);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showEmptyResult(boolean show) {
        if (show) {
            listView.setVisibility(View.GONE);
            emptyResultLayout.setVisibility(View.VISIBLE);
            searchSuggestionsLayout.setVisibility(View.VISIBLE);
            loadRecentSearches();
            tvEmptyMessage.setText("검색된 결과가 없습니다.");
            // AI 검색 모드에서도 그룹 생성 버튼을 항상 표시
            btnCreateGroup.setVisibility(View.VISIBLE);
        } else {
            emptyResultLayout.setVisibility(View.GONE);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_search);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(GroupSearchActivity.this, MainActivity.class));
                finish(); return true;
            } else if (id == R.id.navigation_search) {
                return true;
            } else if (id == R.id.navigation_chat) {
                startActivity(new Intent(GroupSearchActivity.this, ChatGroupListActivity.class));
                finish(); return true;
            } else if (id == R.id.navigation_board) {
                startActivity(new Intent(GroupSearchActivity.this, BoardActivity.class));
                finish(); return true;
            } else if (id == R.id.navigation_profile) {
                startActivity(new Intent(GroupSearchActivity.this, ProfileActivity.class));
                finish(); return true;
            }
            return false;
        });
    }

    private void searchGroups(String keyword) {
        Log.d(TAG, "Searching groups with keyword: " + keyword + ", AI mode: " + isAiModeEnabled);

        if (keyword == null || keyword.trim().isEmpty()) {
            Toast.makeText(GroupSearchActivity.this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
            allGroups.clear();
            adapter.notifyDataSetChanged();
            listView.setVisibility(View.GONE);
            emptyResultLayout.setVisibility(View.GONE);
            searchSuggestionsLayout.setVisibility(View.VISIBLE);
            loadRecentSearches();
            showLoading(false);
            return;
        }

        searchSuggestionsLayout.setVisibility(View.GONE);
        listView.setVisibility(View.GONE);
        emptyResultLayout.setVisibility(View.GONE);
        showLoading(true);
        retryCount = 0;
        searchGroupsWithRetry(keyword);
    }

    private void searchGroupsWithRetry(String keyword) {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("email", "");

        if (userEmail.isEmpty()) {
            showLoading(false);
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            showEmptyResult(true);
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
        apiService = RetrofitClient.getCustomApiService(client);

        Call<ApiResponse> call;
        if (isAiModeEnabled) {
            String userSmbti = sharedPreferences.getString("smbti", "");
            String userName = sharedPreferences.getString("name", "");
            FindGroupRequest request = new FindGroupRequest(userEmail, userSmbti, userName, keyword);
            Log.d(TAG, "AI 모드에서 AI 추천 API 사용: " + keyword);
            directAiRecommend(request);
            return;
        } else {
            call = apiService.getGroupsWithSmbtiScore(userEmail);
        }

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);
                allGroups.clear();

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
                            }
                        }
                        java.util.Collections.sort(allGroups, (g1, g2) -> Double.compare(g2.getMbtiScore(), g1.getMbtiScore()));
                    }
                } else {
                    int errorCode = response.code();
                    Log.e(TAG, "Error response: " + errorCode);
                    if (errorCode >= 500 && retryCount < MAX_RETRY_COUNT) {
                        retryWithDelay(keyword);
                        return;
                    } else {
                        Toast.makeText(GroupSearchActivity.this, "서버 오류: " + errorCode, Toast.LENGTH_SHORT).show();
                    }
                }
                adapter.notifyDataSetChanged();
                if (allGroups.isEmpty()) {
                    showEmptyResult(true);
                } else {
                    listView.setVisibility(View.VISIBLE);
                    searchSuggestionsLayout.setVisibility(View.GONE);
                    emptyResultLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network request failed: " + t.getMessage(), t);
                if (retryCount < MAX_RETRY_COUNT) {
                    retryWithDelay(keyword);
                } else {
                    allGroups.clear();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(GroupSearchActivity.this, "네트워크 연결 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    showEmptyResult(true);
                }
            }
        });
    }

    private void retryWithDelay(final String keyword) {
        retryCount++;
        Log.d(TAG, "재시도 " + retryCount + "/" + MAX_RETRY_COUNT);
        Toast.makeText(GroupSearchActivity.this, "연결 재시도 중... (" + retryCount + "/" + MAX_RETRY_COUNT + ")", Toast.LENGTH_SHORT).show();
        new android.os.Handler().postDelayed(() -> {
            if (!isFinishing()) {
                searchGroupsWithRetry(keyword);
            }
        }, RETRY_DELAY_MS);
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (searchView != null) searchView.setEnabled(!isLoading);
    }

    private void showCreateGroupDialog(String keyword) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);
        final EditText groupNameInput = new EditText(this);
        groupNameInput.setHint("그룹명");
        groupNameInput.setText(keyword + " 그룹");
        layout.addView(groupNameInput);
        TextView space = new TextView(this);
        space.setHeight(30);
        layout.addView(space);
        final EditText descriptionInput = new EditText(this);
        descriptionInput.setHint("그룹 설명");
        descriptionInput.setText(keyword + "에 관련된 그룹입니다.");
        layout.addView(descriptionInput);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("그룹 생성");
        builder.setView(layout);
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton("생성", (dialog, which) -> {
            String groupName = groupNameInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            if (groupName.isEmpty()) {
                Toast.makeText(this, "그룹명을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            createCustomGroup(groupName, description);
        });
        builder.show();
    }

    private void createCustomGroup(String groupName, String description) {
        showLoading(true);
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("email", "");
        if (userEmail.isEmpty()) {
            showLoading(false);
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        CreateGroupRequest request = new CreateGroupRequest();
        request.setGroup_name(groupName);
        request.setDescription(description);
        request.setEmail(userEmail);
        request.setTopics(searchView.getText().toString().trim());
        request.setUseAi(false);
        Call<ApiResponse> call = apiService.createGroup(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(GroupSearchActivity.this, "그룹이 성공적으로 생성되었습니다!", Toast.LENGTH_LONG).show();
                    searchGroups(searchView.getText().toString().trim());
                } else {
                    Toast.makeText(GroupSearchActivity.this, "그룹 생성 실패: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(GroupSearchActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void directAiRecommend(FindGroupRequest request) {
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).build();
        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://202.31.246.51:80/").client(client).addConverterFactory(GsonConverterFactory.create(gson)).build();
        RecommendService recommendService = retrofit.create(RecommendService.class);

        recommendService.recommendGroups(request).enqueue(new Callback<List<Group>>() {
            @Override
            public void onResponse(Call<List<Group>> call, Response<List<Group>> response) {
                showLoading(false);
                allGroups.clear();

                if (response.isSuccessful() && response.body() != null) {
                    List<Group> recommendedGroups = response.body();
                    if (recommendedGroups != null && !recommendedGroups.isEmpty()) {
                        allGroups.addAll(recommendedGroups);
                        java.util.Collections.sort(allGroups, (g1, g2) -> Double.compare(g2.getMbtiScore(), g1.getMbtiScore()));
                    }
                } else {
                    Log.e(TAG, "AI 추천 API 오류: " + response.code());
                    Toast.makeText(GroupSearchActivity.this, "AI 추천 결과를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
                adapter.notifyDataSetChanged();
                if (allGroups.isEmpty()) {
                    showEmptyResult(true);
                } else {
                    listView.setVisibility(View.VISIBLE);
                    searchSuggestionsLayout.setVisibility(View.GONE);
                    emptyResultLayout.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(Call<List<Group>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "AI Recommend Network request failed: " + t.getMessage(), t);
                Toast.makeText(GroupSearchActivity.this, "AI 추천 서비스 접속에 실패했습니다.", Toast.LENGTH_LONG).show();
                showEmptyResult(true);
            }
        });
    }

    private interface RecommendService {
        @POST("groups/recommend")
        Call<List<Group>> recommendGroups(@Body FindGroupRequest request);
    }

    private void showJoinGroupDialog(Group group) {
        new AlertDialog.Builder(this)
                .setTitle(group.getName())
                .setMessage("그룹에 가입하시겠습니까?")
                .setPositiveButton("가입", (dialog, which) -> {
                    Toast.makeText(this, group.getName() + " 그룹 가입 요청 (다이얼로그)", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    // --- GroupAdapter 내부 클래스 수정 ---
    private class GroupAdapter extends ArrayAdapter<Group> {
        private Context context;
        private List<Group> groups;

        public GroupAdapter(@NonNull Context context, @NonNull List<Group> groups) {
            super(context, 0, groups);
            this.context = context;
            this.groups = groups;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItemView = convertView;
            ViewHolder holder;

            if (listItemView == null) {
                listItemView = LayoutInflater.from(context).inflate(R.layout.list_item_group_search, parent, false);
                holder = new ViewHolder();
                holder.tvGroupName = listItemView.findViewById(R.id.tv_group_name);
                holder.tvGroupScore = listItemView.findViewById(R.id.tv_group_score);
                holder.btnJoinGroupItem = listItemView.findViewById(R.id.btn_join_group_item);
                listItemView.setTag(holder);
            } else {
                holder = (ViewHolder) listItemView.getTag();
            }

            final Group currentGroup = groups.get(position);

            if (currentGroup != null) {
                holder.tvGroupName.setText(currentGroup.getName());

                int roundedScore = (int) Math.round(currentGroup.getMbtiScore());
                if (GroupSearchActivity.this.isAiModeEnabled) {
                    holder.tvGroupScore.setText("AI 추천: " + roundedScore + "점");
                } else {
                    holder.tvGroupScore.setText("궁합: " + roundedScore + "점");
                }

                holder.btnJoinGroupItem.setOnClickListener(v -> {
                    Intent intent = new Intent(context, GroupDetailActivity.class);

                    intent.putExtra(GroupDetailActivity.EXTRA_GROUP, (Serializable) currentGroup);
                    intent.putExtra(GroupDetailActivity.EXTRA_IS_AI_MODE, GroupSearchActivity.this.isAiModeEnabled);
                    context.startActivity(intent);
                });
            }
            return listItemView;
        }

        private class ViewHolder {
            TextView tvGroupName;
            TextView tvGroupScore;
            Button btnJoinGroupItem;
        }
    }
}
