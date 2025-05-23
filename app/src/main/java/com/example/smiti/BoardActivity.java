package com.example.smiti;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.smiti.adapter.PostAdapter;
import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.RetrofitClient;
import com.example.smiti.model.Post;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BoardActivity extends AppCompatActivity {

    private static final String TAG = "BoardActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USER_ADMIN_STATUS = "admin";
    private static final int REQUEST_CREATE_POST = 1001;
    private static final int REQUEST_DETAIL_POST = 1002;

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> filteredPostList; // 최종적으로 RecyclerView에 보여질 목록
    private ChipGroup categoryChips;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView adminIndicatorText;

    private String currentCategory = DISPLAY_CATEGORY_ALL; // 화면에 표시되는 카테고리 이름, 기본 "전체"
    private String currentBoardTypeForAPI = API_BOARD_TYPE_ALL_PSEUDO; // API 요청 시 사용될 board_type 또는 플래그
    private String searchQuery = "";

    // "전체" 탭 데이터 관리용
    private Post pinnedNoticePost;           // 상단 고정될 최신 공지 1개
    private List<Post> otherPostsList;       // "전체" 탭에서 고정 공지를 제외한 나머지 글 (자유, 정보)
    // 또는 특정 카테고리 탭 선택 시 해당 카테고리 글 목록

    private AtomicInteger apiCallsCompletedCounter;
    private int totalApiCallsToMakeForALLTab;

    // API 요청 시 사용할 board_type 값 또는 내부 플래그 상수 정의
    private static final String API_BOARD_TYPE_ALL_PSEUDO = "__ALL_CATEGORIES_WITH_PINNED_NOTICE__"; // "전체" 탭 특별 로직 플래그
    private static final String API_BOARD_TYPE_NOTICE = "공지";
    private static final String API_BOARD_TYPE_FREE = "자유";
    private static final String API_BOARD_TYPE_INFO = "정보";

    // 화면 표시용 카테고리 이름 상수 정의
    private static final String DISPLAY_CATEGORY_ALL = "전체";
    private static final String DISPLAY_CATEGORY_NOTICE = "공지사항";
    private static final String DISPLAY_CATEGORY_FREE = "자유게시판";
    private static final String DISPLAY_CATEGORY_INFO = "정보게시판";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);
        Log.d(TAG, "onCreate called");

        initViews();
        setupAdminIndicator();
        setupBottomNavigation();
        setupCategoryChips();
        setupAdapter();
        setupSearchView();
        setupSwipeRefresh();

        loadPostsFromServer(currentBoardTypeForAPI);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        setupAdminIndicator();
        // 필요 시 데이터 새로고침 (예: 글 작성/수정/삭제 후 돌아왔을 때)
        // onActivityResult에서 처리하고 있으므로 여기서는 중복 호출 피할 수 있음
    }

    private void initViews() {
        Log.d(TAG, "initViews called");
        recyclerView = findViewById(R.id.recycler_posts);
        categoryChips = findViewById(R.id.category_chips);
        etSearch = findViewById(R.id.et_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        adminIndicatorText = findViewById(R.id.admin_indicator_text);

        filteredPostList = new ArrayList<>();
        pinnedNoticePost = null;
        otherPostsList = new ArrayList<>();
        apiCallsCompletedCounter = new AtomicInteger(0);

        if (adminIndicatorText == null) {
            Log.e(TAG, "initViews: admin_indicator_text is NULL!");
        }

        FloatingActionButton fabAddPost = findViewById(R.id.fab_add_post);
        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(BoardActivity.this, PostEditActivity.class);
            startActivityForResult(intent, REQUEST_CREATE_POST);
        });
    }

    private void setupAdminIndicator() {
        Log.d(TAG, "setupAdminIndicator called");
        if (adminIndicatorText == null) {
            Log.e(TAG, "setupAdminIndicator: admin_indicator_text is NULL.");
            return;
        }
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int adminStatus = prefs.getInt(KEY_USER_ADMIN_STATUS, -1);
        adminIndicatorText.setVisibility(adminStatus == 1 ? View.VISIBLE : View.GONE);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_board);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent = null;
            if (id == R.id.navigation_home) intent = new Intent(BoardActivity.this, MainActivity.class);
            else if (id == R.id.navigation_search) intent = new Intent(BoardActivity.this, GroupSearchActivity.class);
            else if (id == R.id.navigation_chat) intent = new Intent(BoardActivity.this, ChatActivity.class);
            else if (id == R.id.navigation_board) return true;
            else if (id == R.id.navigation_profile) intent = new Intent(BoardActivity.this, ProfileActivity.class);
            if (intent != null) { startActivity(intent); finish(); return true; }
            return false;
        });
    }

    private void setupCategoryChips() {
        Log.d(TAG, "setupCategoryChips called");
        // XML에서 android:checkedButton="@id/chip_all" 등으로 기본 선택을 해두는 것이 좋음
        int initialCheckedChipId = categoryChips.getCheckedChipId();
        if (initialCheckedChipId != View.NO_ID && findViewById(initialCheckedChipId) instanceof Chip) {
            Chip initialCheckedChip = findViewById(initialCheckedChipId);
            updateCategoryAndBoardType(initialCheckedChip.getText().toString());
        } else {
            setDefaultCategoryAndBoardType(); // 기본 "전체" 탭 선택
        }

        categoryChips.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID && findViewById(checkedId) instanceof Chip) {
                Chip checkedChip = findViewById(checkedId);
                String selectedCategoryText = checkedChip.getText().toString();
                Log.d(TAG, "Chip selected: " + selectedCategoryText);
                updateCategoryAndBoardType(selectedCategoryText);
                loadPostsFromServer(currentBoardTypeForAPI);
            } else if (categoryChips.getCheckedChipId() == View.NO_ID) {
                // 모든 칩 선택이 해제된 경우 (singleSelection="false"이고 사용자가 해제했다면)
                // 강제로 기본 칩("전체")을 다시 선택하도록 함
                Log.w(TAG, "All chips deselected, re-selecting default 'All' chip.");
                setDefaultCategoryAndBoardType();
                loadPostsFromServer(currentBoardTypeForAPI); // 상태 변경 후 로드
            }
        });
    }

    private void setDefaultCategoryAndBoardType() {
        Chip allChip = findViewById(R.id.chip_all); // XML에 정의된 "전체" 칩의 ID (예: @id/chip_all)
        if (allChip != null) {
            if (!allChip.isChecked()) { // 이미 체크되어 있지 않을 때만
                allChip.setChecked(true); // 코드에서 "전체" 칩을 선택 상태로 만듦 (리스너 호출됨)
            } else { // 이미 "전체"가 선택된 상태라면, 상태만 업데이트하고 로드
                updateCategoryAndBoardType(allChip.getText().toString());
            }
        } else {
            Log.e(TAG, "Default chip 'chip_all' not found! Defaulting to '전체' text.");
            updateCategoryAndBoardType(DISPLAY_CATEGORY_ALL);
        }
    }

    private void updateCategoryAndBoardType(String categoryText) {
        currentCategory = categoryText;
        if (categoryText.equals(DISPLAY_CATEGORY_NOTICE)) {
            currentBoardTypeForAPI = API_BOARD_TYPE_NOTICE;
        } else if (categoryText.equals(DISPLAY_CATEGORY_FREE)) {
            currentBoardTypeForAPI = API_BOARD_TYPE_FREE;
        } else if (categoryText.equals(DISPLAY_CATEGORY_INFO)) {
            currentBoardTypeForAPI = API_BOARD_TYPE_INFO;
        } else if (categoryText.equals(DISPLAY_CATEGORY_ALL)) {
            currentBoardTypeForAPI = API_BOARD_TYPE_ALL_PSEUDO;
        } else {
            Log.w(TAG, "Unknown category text: " + categoryText + ". Defaulting to ALL.");
            currentCategory = DISPLAY_CATEGORY_ALL;
            currentBoardTypeForAPI = API_BOARD_TYPE_ALL_PSEUDO;
        }
        Log.i(TAG, "Category updated: currentCategory (Display) = '" + currentCategory + "', currentBoardTypeForAPI (Flag/API) = '" + currentBoardTypeForAPI + "'");
    }

    private void setupAdapter() {
        Log.d(TAG, "setupAdapter called");
        adapter = new PostAdapter(this, filteredPostList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(position -> {
            if (position >= 0 && position < filteredPostList.size()) {
                Post post = filteredPostList.get(position);
                if (post == null) return;
                Intent intent = new Intent(BoardActivity.this, PostDetailActivity.class);
                String postId = post.getId();
                if (postId != null && !postId.isEmpty()) {
                    if (postId.startsWith("temp_")) {
                        Toast.makeText(this, "이 게시글은 아직 동기화되지 않았습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (postId.contains(".")) postId = postId.substring(0, postId.indexOf('.'));
                    intent.putExtra("post_id", postId);
                    startActivityForResult(intent, REQUEST_DETAIL_POST);
                }
            }
        });
    }

    private void setupSearchView() {
        Log.d(TAG, "setupSearchView called");
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                searchQuery = s.toString().trim().toLowerCase();
                btnClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                filterPosts();
            }
        });
        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
    }

    private void setupSwipeRefresh() {
        Log.d(TAG, "setupSwipeRefresh called");
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.i(TAG, "Swipe to refresh triggered.");
            loadPostsFromServer(currentBoardTypeForAPI);
        });
    }

    private void loadPostsFromServer(String boardTypeOrFlag) {
        showLoading(true);
        Log.i(TAG, "loadPostsFromServer initiated. Received boardTypeOrFlag: '" + boardTypeOrFlag + "'");

        if (RetrofitClient.getApiService() == null) {
            Log.e(TAG, "ApiService is null.");
            showLoading(false);
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "네트워크 서비스 초기화 오류", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        pinnedNoticePost = null; // 이전 고정 공지 초기화
        otherPostsList.clear();   // 이전 하단 목록 초기화

        if (API_BOARD_TYPE_ALL_PSEUDO.equals(boardTypeOrFlag)) {
            apiCallsCompletedCounter.set(0);
            totalApiCallsToMakeForALLTab = 3; // 공지(1개), 자유, 정보
            Log.d(TAG, "Fetching for ALL tab. Total API calls: " + totalApiCallsToMakeForALLTab);

            fetchLatestPinnedNoticeInternal(); // 1. 최신 공지 1개
            fetchCategoryPostsForALLTabInternal(API_BOARD_TYPE_FREE); // 2. 자유게시판 글
            fetchCategoryPostsForALLTabInternal(API_BOARD_TYPE_INFO); // 3. 정보게시판 글
        } else { // 특정 카테고리 탭 (공지, 자유, 정보)
            Log.d(TAG, "Fetching single category: " + boardTypeOrFlag);
            Call<ApiResponse> call = RetrofitClient.getApiService().getPosts(1, 20, boardTypeOrFlag);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    showLoading(false);
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (response.isSuccessful() && response.body() != null) {
                        List<Map<String, Object>> postsData = response.body().getPosts();
                        processSingleCategoryResponse(postsData, boardTypeOrFlag);
                    } else {
                        handleServerError(response, boardTypeOrFlag);
                        showEmptyState();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    showLoading(false);
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    Log.e(TAG, "API call failed for " + boardTypeOrFlag, t);
                    Toast.makeText(BoardActivity.this, "네트워크 오류 (" + boardTypeOrFlag + ")", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }
            });
        }
    }

    private void fetchLatestPinnedNoticeInternal() {
        Log.d(TAG, "Fetching latest pinned notice for ALL tab...");
        Call<ApiResponse> noticeCall = RetrofitClient.getApiService().getPosts(1, 1, API_BOARD_TYPE_NOTICE); // 최신 1개
        noticeCall.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> noticeDataList = response.body().getPosts();
                    if (noticeDataList != null && !noticeDataList.isEmpty()) {
                        List<Post> notices = parseRawPostsToPostList(noticeDataList, API_BOARD_TYPE_NOTICE);
                        if (!notices.isEmpty()) {
                            pinnedNoticePost = notices.get(0);
                            Log.i(TAG, "Pinned notice fetched: ID " + (pinnedNoticePost != null ? pinnedNoticePost.getId() : "null"));
                        }
                    } else { pinnedNoticePost = null; }
                } else {
                    Log.e(TAG, "Failed to fetch pinned notice. Code: " + response.code());
                    pinnedNoticePost = null;
                }
                checkAndFinalizeALLTabFetch();
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed for pinned notice.", t);
                pinnedNoticePost = null;
                checkAndFinalizeALLTabFetch();
            }
        });
    }

    private void fetchCategoryPostsForALLTabInternal(final String boardTypeToFetch) {
        Log.d(TAG, "Fetching posts for '" + boardTypeToFetch + "' for ALL tab...");
        Call<ApiResponse> call = RetrofitClient.getApiService().getPosts(1, 20, boardTypeToFetch);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> postsData = response.body().getPosts();
                    if (postsData != null) {
                        List<Post> fetchedPosts = parseRawPostsToPostList(postsData, boardTypeToFetch);
                        synchronized (otherPostsList) {
                            otherPostsList.addAll(fetchedPosts);
                        }
                        Log.d(TAG, "Fetched " + fetchedPosts.size() + " for " + boardTypeToFetch + ". otherPostsList size: " + otherPostsList.size());
                    }
                } else {
                    Log.e(TAG, "Failed to fetch " + boardTypeToFetch + " for ALL tab. Code: " + response.code());
                }
                checkAndFinalizeALLTabFetch();
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed for " + boardTypeToFetch + " for ALL tab.", t);
                checkAndFinalizeALLTabFetch();
            }
        });
    }

    private void checkAndFinalizeALLTabFetch() {
        if (apiCallsCompletedCounter.incrementAndGet() == totalApiCallsToMakeForALLTab) {
            Log.i(TAG, "All data fetches completed for '전체' tab. Pinned: " + (pinnedNoticePost != null) +
                    ", Others: " + otherPostsList.size());

            // otherPostsList 정렬 (최신순)
            Collections.sort(otherPostsList, (p1, p2) -> {
                if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                if (p1.getCreatedAt() == null) return 1;
                if (p2.getCreatedAt() == null) return -1;
                return p2.getCreatedAt().compareTo(p1.getCreatedAt());
            });

            showLoading(false);
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            filterPosts(); // 이제 filterPosts가 pinnedNoticePost와 otherPostsList를 사용
        }
    }

    private void processSingleCategoryResponse(List<Map<String, Object>> postsData, String requestedApiBoardType) {
        otherPostsList.clear(); // 특정 카테고리 조회 시 otherPostsList만 사용
        pinnedNoticePost = null;    // 특정 카테고리 조회 시 고정 공지 없음

        if (postsData != null) {
            otherPostsList.addAll(parseRawPostsToPostList(postsData, requestedApiBoardType));
        } else {
            Log.w(TAG, "No posts data for single category: " + requestedApiBoardType);
        }
        Log.i(TAG, "Processed single category " + requestedApiBoardType + ". otherPostsList size: " + otherPostsList.size());
        filterPosts();
    }

    private List<Post> parseRawPostsToPostList(List<Map<String, Object>> postsData, String sourceApiBoardType) {
        List<Post> parsedList = new ArrayList<>();
        if (postsData == null) return parsedList;

        for (Map<String, Object> postData : postsData) {
            Post post = new Post();
            String postId = getStringFromMap(postData, "id");
            if (postId == null) continue;

            post.setId(postId);
            post.setTitle(getStringFromMap(postData, "title"));
            post.setContent(getStringFromMap(postData, "content"));
            post.setAuthorId(getStringFromMap(postData, "email"));
            post.setAuthorName(getStringFromMap(postData, "name"));
            post.setLikeCount(getIntFromMap(postData, "likes", 0));
            post.setCommentCount(getIntFromMap(postData, new String[]{"comment_count", "comments"}, 0));
            post.setViewCount(getIntFromMap(postData, "views", 0));

            String fileName = getStringFromMap(postData, "file_name");
            if (fileName != null && !fileName.isEmpty()) {
                post.setFileName(fileName);
                post.setFileUrl("http://202.31.246.51:80/board-uploads/" + fileName);
            }
            post.setFileSize(getLongFromMap(postData, "file_size", 0L));

            String createdAtStr = getStringFromMap(postData, "created_at");
            if (createdAtStr != null) post.setCreatedAt(parseDate(createdAtStr));
            else post.setCreatedAt(new Date());

            String categoryFromServerApiValue = getStringFromMap(postData, "board_type");
            String finalDisplayCategory;
            if (categoryFromServerApiValue != null && !categoryFromServerApiValue.isEmpty()) {
                finalDisplayCategory = mapApiBoardTypeToDisplayCategory(categoryFromServerApiValue);
            } else {
                finalDisplayCategory = mapApiBoardTypeToDisplayCategory(sourceApiBoardType);
            }
            post.setCategory(finalDisplayCategory);
            post.setNotice(DISPLAY_CATEGORY_NOTICE.equals(finalDisplayCategory));

            parsedList.add(post);
        }
        return parsedList;
    }

    private void filterPosts() {
        Log.d(TAG, "filterPosts called. CurrentCategory: " + currentCategory + ", Pinned: " + (pinnedNoticePost != null) + ", Others: " + otherPostsList.size());
        filteredPostList.clear();

        // 1. "전체" 탭이고 고정 공지가 있으면 먼저 추가 (검색어 필터링 적용)
        if (currentCategory.equals(DISPLAY_CATEGORY_ALL) && pinnedNoticePost != null) {
            boolean matchesSearchPinned = searchQuery.isEmpty() ||
                    (pinnedNoticePost.getTitle() != null && pinnedNoticePost.getTitle().toLowerCase().contains(searchQuery)) ||
                    (pinnedNoticePost.getAuthorName() != null && pinnedNoticePost.getAuthorName().toLowerCase().contains(searchQuery));
            if (matchesSearchPinned) {
                filteredPostList.add(pinnedNoticePost);
            }
        }

        // 2. otherPostsList (자유/정보 또는 특정 카테고리 글) 필터링하여 추가
        for (Post post : otherPostsList) {
            if (post == null) continue;

            boolean matchesCategory;
            if (currentCategory.equals(DISPLAY_CATEGORY_ALL)) {
                // "전체" 탭의 하단 목록은 otherPostsList (자유, 정보 글)의 모든 글을 포함
                // (otherPostsList는 이미 공지를 제외한 자유/정보 글만 담고 있음)
                matchesCategory = true;
            } else {
                // 특정 카테고리 탭일 경우
                matchesCategory = (post.getCategory() != null && post.getCategory().equalsIgnoreCase(currentCategory));
            }

            boolean matchesSearch = searchQuery.isEmpty() ||
                    (post.getTitle() != null && post.getTitle().toLowerCase().contains(searchQuery)) ||
                    (post.getContent() != null && post.getContent().toLowerCase().contains(searchQuery)) ||
                    (post.getAuthorName() != null && post.getAuthorName().toLowerCase().contains(searchQuery));

            if (matchesCategory && matchesSearch) {
                // "전체" 탭일 때, 이미 고정 공지로 추가된 것과 ID가 같은 글은 중복 추가 방지 (이런일은 없어야함)
                if (currentCategory.equals(DISPLAY_CATEGORY_ALL) && pinnedNoticePost != null && post.getId().equals(pinnedNoticePost.getId())) {
                    continue;
                }
                filteredPostList.add(post);
            }
        }
        Log.d(TAG, "filterPosts: Final filteredPostList size: " + filteredPostList.size());
        updateUI();
    }


    private String mapApiBoardTypeToDisplayCategory(String apiBoardType) {
        if (apiBoardType == null || apiBoardType.trim().isEmpty()) return "기타";
        switch (apiBoardType.toLowerCase().trim()) {
            case API_BOARD_TYPE_NOTICE: return DISPLAY_CATEGORY_NOTICE;
            case API_BOARD_TYPE_FREE:   return DISPLAY_CATEGORY_FREE;
            case API_BOARD_TYPE_INFO:   return DISPLAY_CATEGORY_INFO;
            default: return "기타";
        }
    }

    private String getStringFromMap(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            return String.valueOf(map.get(key));
        }
        return null;
    }

    private int getIntFromMap(Map<String, Object> map, String key, int defaultValue) {
        return getIntFromMap(map, new String[]{key}, defaultValue);
    }

    private int getIntFromMap(Map<String, Object> map, String[] keys, int defaultValue) {
        if (map == null) return defaultValue;
        for (String keyVal : keys) { // 변수명 변경 (key -> keyVal)
            if (map.containsKey(keyVal) && map.get(keyVal) != null) {
                Object value = map.get(keyVal);
                try {
                    if (value instanceof Number) return ((Number) value).intValue();
                    if (value instanceof String && !((String) value).isEmpty())
                        return Double.valueOf((String) value).intValue();
                } catch (NumberFormatException e) { /* Log if needed */ }
            }
        }
        return defaultValue;
    }

    private long getLongFromMap(Map<String, Object> map, String key, long defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            Object value = map.get(key);
            try {
                if (value instanceof Number) return ((Number) value).longValue();
                if (value instanceof String && !((String) value).isEmpty())
                    return Double.valueOf((String) value).longValue();
            } catch (NumberFormatException e) { /* Log if needed */ }
        }
        return defaultValue;
    }

    private Date parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return new Date();
        String[] possibleFormats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss"
        };
        for (String format : possibleFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                if (format.endsWith("'Z'")) sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf.parse(dateString);
            } catch (ParseException e) { /* try next format */ }
        }
        Log.e(TAG, "Failed to parse date: " + dateString);
        return new Date();
    }

    private void showEmptyState() {
        Log.d(TAG, "showEmptyState called");
        // 모든 데이터 리스트를 비우고 UI 업데이트
        if (pinnedNoticePost != null) pinnedNoticePost = null;
        if (otherPostsList != null) otherPostsList.clear();
        if (filteredPostList != null) filteredPostList.clear(); // 이것도 비워야 함
        if (adapter != null) adapter.updateData(filteredPostList); // 어댑터에 빈 리스트 전달
        updateUI(); // tvEmptyState 표시
    }

    private void showLoading(boolean isLoading) {
        Log.d(TAG, "showLoading called with: " + isLoading);
        if (progressBar != null) {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing() && isLoading) {
                // SwipeRefreshLayout이 이미 로딩 중이면 추가 ProgressBar는 숨김
                progressBar.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        }
    }
    private void updateUI() { // 이 함수는 filterPosts 마지막에 호출됨
        Log.d(TAG, "updateUI called. Filtered list size: " + filteredPostList.size());
        if (adapter != null) {
            adapter.updateData(filteredPostList);
        }
        if (tvEmptyState != null) {
            boolean noPinnedNotice = (currentCategory.equals(DISPLAY_CATEGORY_ALL) && pinnedNoticePost == null);
            boolean noOtherPosts = otherPostsList.isEmpty(); // "전체" 탭일때 otherPostsList를, 특정 탭일때도 otherPostsList를 봄

            if (filteredPostList.isEmpty()) { // 최종적으로 화면에 보여줄 글이 없을 때
                tvEmptyState.setVisibility(View.VISIBLE);
                if (!searchQuery.isEmpty()) {
                    tvEmptyState.setText("'" + searchQuery + "'에 대한 검색 결과가 없습니다.");
                } else if (!currentCategory.equals(DISPLAY_CATEGORY_ALL)) {
                    tvEmptyState.setText(currentCategory + " 카테고리에 게시글이 없습니다.");
                } else { // "전체" 탭인데 글이 없는 경우
                    tvEmptyState.setText("게시글이 없습니다.\n첫 번째 게시글을 작성해보세요!");
                }
            } else {
                tvEmptyState.setVisibility(View.GONE);
            }
        }
    }


    private void handleServerError(Response<ApiResponse> response, String requestContext) {
        Log.e(TAG, "Server error for " + requestContext + ". Code: " + response.code() + ", Message: " + response.message());
        // ... (기존 오류 메시지 처리 로직)
        String errorMsgToShow = "서버 오류 (" + requestContext + "): " + response.code();
        if (response.errorBody() != null) {
            try {
                String errorBodyStr = response.errorBody().string();
                Log.e(TAG, "Error Body for " + requestContext + ": " + errorBodyStr);
                // 간단한 오류 메시지 파싱 시도 (선택 사항)
                // if (errorBodyStr.contains("Field required") && errorBodyStr.contains("board_type")) {
                //     errorMsgToShow = "서버에서 board_type 파라미터가 필요합니다.";
                // }
            } catch (IOException e) {
                Log.e(TAG, "Error reading error body for " + requestContext, e);
            }
        }
        Toast.makeText(BoardActivity.this, errorMsgToShow, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if ((requestCode == REQUEST_CREATE_POST || requestCode == REQUEST_DETAIL_POST) && resultCode == RESULT_OK) {
            Log.i(TAG, "Post operation successful, reloading posts for current category: " + currentCategory);
            // 현재 선택된 카테고리(currentBoardTypeForAPI)로 데이터를 다시 로드
            loadPostsFromServer(currentBoardTypeForAPI);
            if (requestCode == REQUEST_CREATE_POST) {
                Toast.makeText(this, "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
