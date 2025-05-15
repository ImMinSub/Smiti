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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BoardActivity extends AppCompatActivity {

    private static final String TAG = "BoardActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final int REQUEST_CREATE_POST = 1001;
    private static final int REQUEST_DETAIL_POST = 1002;
    
    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> postList;
    private List<Post> filteredPostList;
    private ChipGroup categoryChips;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    
    private String currentCategory = "전체"; // 기본 카테고리
    private String boardType = "자유"; // 기본 게시판 유형
    private String searchQuery = ""; // 검색어
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        // 초기화
        initViews();
        setupBottomNavigation();
        setupCategoryChips();
        setupAdapter();
        setupSearchView();
        setupSwipeRefresh();
        
        // 서버에서 게시글 데이터 로드
        loadPostsFromServer();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보여질 때 최신 데이터 로드
        loadPostsFromServer();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recycler_posts);
        categoryChips = findViewById(R.id.category_chips);
        etSearch = findViewById(R.id.et_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        
        FloatingActionButton fabAddPost = findViewById(R.id.fab_add_post);
        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(BoardActivity.this, PostEditActivity.class);
            startActivityForResult(intent, REQUEST_CREATE_POST);
        });
    }
    
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_board);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.navigation_home) {
                Intent intent = new Intent(BoardActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_search) {
                // 그룹 검색 화면으로 이동
                Intent intent = new Intent(BoardActivity.this, GroupSearchActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_chat) {
                // 채팅 화면으로 이동
                Intent intent = new Intent(BoardActivity.this, ChatActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.navigation_board) {
                // 이미 게시판 화면에 있음
                return true;
            } else if (id == R.id.navigation_profile) {
                // 프로필 화면으로 이동
                Intent intent = new Intent(BoardActivity.this, ProfileActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            
            return false;
        });
    }
    
    private void setupCategoryChips() {
        categoryChips.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_all) {
                currentCategory = "전체";
            } else if (checkedId == R.id.chip_free) {
                currentCategory = Post.CATEGORY_FREE;
                boardType = "자유";
            } else if (checkedId == R.id.chip_notice) {
                currentCategory = Post.CATEGORY_NOTICE;
                boardType = "공지";
            } else if (checkedId == R.id.chip_info) {
                currentCategory = "정보";
                boardType = "정보";
            }
            loadPostsFromServer();
        });
    }
    
    private void setupAdapter() {
        postList = new ArrayList<>();
        filteredPostList = new ArrayList<>();
        adapter = new PostAdapter(this, filteredPostList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        // 아이템 클릭 리스너 설정
        adapter.setOnItemClickListener(position -> {
            Post post = filteredPostList.get(position);
            // 게시글 상세 화면으로 이동
            Intent intent = new Intent(BoardActivity.this, PostDetailActivity.class);
            try {
                String postId = post.getId();
                // id가 null이 아닌지 체크
                if (postId != null) {
                    // 임시 ID인지 체크 ("temp_"로 시작하는지)
                    if (postId.startsWith("temp_")) {
                        // 임시 ID인 경우는 서버에서 실제 데이터를 가져와야 함
                        Toast.makeText(this, "이 게시글은 아직 서버에 동기화되지 않았습니다", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // id가 숫자로 오는 경우에 대한 처리
                    if (postId.contains(".")) {
                        // 소수점이 있는 경우 (예: 289.0) 정수 부분만 사용
                        String cleanId = postId.substring(0, postId.indexOf('.'));
                        intent.putExtra("post_id", cleanId);
                    } else {
                        intent.putExtra("post_id", postId);
                    }
                    
                    // ID 값 로그 출력
                    Log.d(TAG, "게시글 상세로 이동: ID = " + postId);
                    
                    startActivityForResult(intent, REQUEST_DETAIL_POST);
                } else {
                    // ID가 null인 경우
                    Toast.makeText(this, "게시글 ID가 없습니다.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "게시글 ID가 null입니다");
                }
            } catch (Exception e) {
                Log.e(TAG, "게시글 ID 처리 오류: " + e.getMessage(), e);
                Toast.makeText(this, "게시글 정보 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupSearchView() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s.toString().trim().toLowerCase();
                btnClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                filterPosts();
            }
        });
        
        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            searchQuery = "";
            btnClearSearch.setVisibility(View.GONE);
            filterPosts();
        });
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent
        );
        swipeRefreshLayout.setOnRefreshListener(this::loadPostsFromServer);
    }
    
    private void filterPosts() {
        if (postList == null || postList.isEmpty()) {
            filteredPostList.clear();
            updateUI();
            return;
        }
        
        filteredPostList.clear();
        
        // 카테고리 및 검색어로 필터링
        for (Post post : postList) {
            boolean matchesCategory = currentCategory.equals("전체") || post.getCategory().equals(currentCategory);
            boolean matchesSearch = searchQuery.isEmpty() ||
                    post.getTitle().toLowerCase().contains(searchQuery) ||
                    post.getContent().toLowerCase().contains(searchQuery) ||
                    post.getAuthorName().toLowerCase().contains(searchQuery);
            
            if (matchesCategory && matchesSearch) {
                filteredPostList.add(post);
            }
        }
        
        updateUI();
    }
    
    private void updateUI() {
        adapter.updateData(filteredPostList);
        
        // 빈 상태 표시
        if (filteredPostList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            if (!searchQuery.isEmpty()) {
                tvEmptyState.setText("검색 결과가 없습니다.");
            } else if (!currentCategory.equals("전체")) {
                tvEmptyState.setText(currentCategory + " 카테고리에 게시글이 없습니다.");
            } else {
                tvEmptyState.setText("게시글이 없습니다.\n첫 번째 게시글을 작성해보세요!");
            }
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }
    
    private void loadPostsFromServer() {
        // 진행 중 표시
        showLoading(true);
        
        try {
            // 기본값으로 1페이지, 5개씩 표시 (데이터 크기 감소)
            Call<ApiResponse> call = RetrofitClient.getApiService().getPosts(1, 5, boardType);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    showLoading(false);
                    
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        
                        // 디버깅용 로그 추가
                        Log.d(TAG, "API 응답: " + response.body().toString());
                        
                        // API 응답 처리
                        if (apiResponse.getPosts() != null) {
                            // posts 배열이 직접 있는 경우
                            List<Map<String, Object>> posts = apiResponse.getPosts();
                            Log.d(TAG, "게시글 목록 (posts): 개수=" + posts.size());
                            
                            postList.clear();
                            processPostsData(posts);
                        } else if (apiResponse.getData() != null) {
                            postList.clear();
                            
                            try {
                                if (apiResponse.getData() instanceof List) {
                                    // 직접 List로 반환되는 경우
                                    List<Map<String, Object>> postsData = (List<Map<String, Object>>) apiResponse.getData();
                                    Log.d(TAG, "게시글 목록 (data/List): 개수=" + postsData.size());
                                    
                                    processPostsData(postsData);
                                } else if (apiResponse.getData() instanceof Map) {
                                    // "posts" 필드로 감싸진 경우 처리
                                    Map<String, Object> responseMap = (Map<String, Object>) apiResponse.getData();
                                    Log.d(TAG, "응답 맵 키: " + String.join(", ", responseMap.keySet()));
                                    
                                    if (responseMap.containsKey("posts")) {
                                        List<Map<String, Object>> postsData = (List<Map<String, Object>>) responseMap.get("posts");
                                        Log.d(TAG, "게시글 목록 (data/Map/posts): 개수=" + postsData.size());
                                        
                                        processPostsData(postsData);
                                    }
                                }
                            } catch (ClassCastException e) {
                                Log.e(TAG, "데이터 형식 변환 오류: " + e.getMessage(), e);
                                Toast.makeText(BoardActivity.this, "데이터 형식 오류", Toast.LENGTH_SHORT).show();
                                showEmptyState();
                            }
                        } else {
                            Toast.makeText(BoardActivity.this, "게시글 로드 실패: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            showEmptyState();
                        }
                    } else {
                        Toast.makeText(BoardActivity.this, "서버 응답 오류", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                }
                
                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    showLoading(false);
                    
                    // 특정 오류 타입에 따른 다른 메시지 표시
                    String errorMessage = "네트워크 오류";
                    if (t.getMessage() != null) {
                        Log.e(TAG, "API 호출 실패: " + t.getMessage(), t);
                        if (t.getMessage().contains("unexpected end of stream")) {
                            errorMessage = "서버 응답 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
                            // 5초 후에 자동으로 다시 시도
                            new android.os.Handler().postDelayed(() -> {
                                if (!isFinishing()) {
                                    Toast.makeText(BoardActivity.this, "게시글 다시 불러오는 중...", Toast.LENGTH_SHORT).show();
                                    loadPostsFromServer();
                                }
                            }, 5000);
                        } else if (t.getMessage().contains("timeout")) {
                            errorMessage = "서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.";
                        } else if (t.getMessage().contains("Failed to connect")) {
                            errorMessage = "서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.";
                        }
                    }
                    Toast.makeText(BoardActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }
            });
        } catch (Exception e) {
            showLoading(false);
            Log.e(TAG, "API 호출 중 예외 발생: " + e.getMessage(), e);
            Toast.makeText(this, "게시글을 불러오는 중 오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            showEmptyState();
        }
    }
    
    // 게시글 데이터 처리를 위한 별도 메서드
    private void processPostsData(List<Map<String, Object>> postsData) {
        postList.clear();
        
        // 게시글 데이터가 있으면 처리
        if (postsData != null && !postsData.isEmpty()) {
            for (Map<String, Object> postData : postsData) {
                try {
                    Post post = new Post();
                    
                    // ID 설정 - 다양한 필드명 시도
                    String postId = null;
                    // 가능한 ID 필드명 배열
                    String[] possibleIdFields = {"post_id", "id", "postId", "_id", "post_number"};
                    
                    // 모든 가능한 필드명을 시도
                    for (String idField : possibleIdFields) {
                        if (postData.get(idField) != null) {
                            postId = String.valueOf(postData.get(idField));
                            Log.d(TAG, "게시글 ID 필드 발견: " + idField + ", 값: " + postId);
                            break;
                        }
                    }
                    
                    // 어떤 필드도 발견되지 않으면 임시 ID 생성 (테스트용)
                    if (postId == null) {
                        // 로그에 전체 데이터 출력
                        Log.e(TAG, "게시글 ID를 찾을 수 없음. 데이터: " + postData.toString());
                        // 현재 시간을 기준으로 임시 ID 생성
                        postId = "temp_" + System.currentTimeMillis();
                    }
                    
                    post.setId(postId);
                    
                    // 제목 설정
                    if (postData.get("title") != null) {
                        post.setTitle((String) postData.get("title"));
                    }
                    
                    // 내용 설정
                    if (postData.get("content") != null) {
                        post.setContent((String) postData.get("content"));
                    }
                    
                    // 작성자 ID 설정
                    if (postData.get("email") != null) {
                        post.setAuthorId((String) postData.get("email"));
                    }
                    
                    // 작성자 이름 설정
                    if (postData.get("name") != null) {
                        post.setAuthorName((String) postData.get("name"));
                    }
                    
                    // 카테고리 설정
                    if (postData.get("board_type") != null) {
                        post.setCategory((String) postData.get("board_type"));
                    }
                    
                    // 좋아요 수 설정
                    if (postData.get("likes") != null) {
                        try {
                            if (postData.get("likes") instanceof Integer) {
                                post.setLikeCount((Integer) postData.get("likes"));
                            } else if (postData.get("likes") instanceof Double) {
                                post.setLikeCount(((Double) postData.get("likes")).intValue());
                            } else {
                                post.setLikeCount(Integer.parseInt(postData.get("likes").toString()));
                            }
                        } catch (NumberFormatException e) {
                            post.setLikeCount(0);
                            Log.e(TAG, "좋아요 수 파싱 오류: " + e.getMessage());
                        }
                    } else {
                        // 좋아요 수가 없는 경우 0으로 설정
                        post.setLikeCount(0);
                    }
                    
                    // 댓글 수 설정
                    if (postData.get("comments") != null) {
                        try {
                            if (postData.get("comments") instanceof Integer) {
                                post.setCommentCount((Integer) postData.get("comments"));
                            } else if (postData.get("comments") instanceof Double) {
                                post.setCommentCount(((Double) postData.get("comments")).intValue());
                            } else {
                                post.setCommentCount(Integer.parseInt(postData.get("comments").toString()));
                            }
                        } catch (NumberFormatException e) {
                            post.setCommentCount(0);
                            Log.e(TAG, "댓글 수 파싱 오류: " + e.getMessage());
                        }
                    } else if (postData.get("comment_count") != null) {
                        // 서버 응답 형식이 변경되었을 경우 (comment_count 필드도 확인)
                        try {
                            if (postData.get("comment_count") instanceof Integer) {
                                post.setCommentCount((Integer) postData.get("comment_count"));
                            } else if (postData.get("comment_count") instanceof Double) {
                                post.setCommentCount(((Double) postData.get("comment_count")).intValue());
                            } else {
                                post.setCommentCount(Integer.parseInt(postData.get("comment_count").toString()));
                            }
                        } catch (NumberFormatException e) {
                            post.setCommentCount(0);
                            Log.e(TAG, "댓글 수 파싱 오류 (comment_count): " + e.getMessage());
                        }
                    } else {
                        // 댓글 수가 없는 경우 0으로 설정
                        post.setCommentCount(0);
                    }
                    
                    // 조회수 설정
                    if (postData.get("views") != null) {
                        try {
                            if (postData.get("views") instanceof Integer) {
                                post.setViewCount((Integer) postData.get("views"));
                            } else if (postData.get("views") instanceof Double) {
                                post.setViewCount(((Double) postData.get("views")).intValue());
                            } else {
                                post.setViewCount(Integer.parseInt(postData.get("views").toString()));
                            }
                        } catch (NumberFormatException e) {
                            post.setViewCount(0);
                            Log.e(TAG, "조회수 파싱 오류: " + e.getMessage());
                        }
                    } else {
                        // 조회수가 없는 경우 0으로 설정
                        post.setViewCount(0);
                    }
                    
                    // 파일 이름 설정
                    if (postData.get("file_name") != null) {
                        post.setFileName((String) postData.get("file_name"));
                        post.setFileUrl("http://202.31.246.51:80/board-uploads/" + post.getFileName());
                    }
                    
                    // 파일 크기 설정
                    if (postData.get("file_size") != null) {
                        try {
                            if (postData.get("file_size") instanceof Integer) {
                                post.setFileSize((Integer) postData.get("file_size"));
                            } else if (postData.get("file_size") instanceof Double) {
                                post.setFileSize(((Double) postData.get("file_size")).longValue());
                            } else if (postData.get("file_size") instanceof String) {
                                post.setFileSize(Long.parseLong((String) postData.get("file_size")));
                            } else {
                                post.setFileSize(Long.parseLong(postData.get("file_size").toString()));
                            }
                        } catch (NumberFormatException e) {
                            post.setFileSize(0);
                            Log.e(TAG, "파일 크기 파싱 오류: " + e.getMessage());
                        }
                    }
                    
                    // 생성일 설정
                    if (postData.get("created_at") != null) {
                        try {
                            String createdAtStr = (String) postData.get("created_at");
                            // 날짜 포맷 수정: 서버 날짜 형식에 맞게 조정
                            SimpleDateFormat serverFormat;
                            
                            // 여러 가능한 날짜 형식을 시도
                            if (createdAtStr.contains(".")) {
                                // 밀리초가 포함된 형식 (yyyy-MM-dd'T'HH:mm:ss.SSS'Z')
                                serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                            } else {
                                // 밀리초가 없는 형식 (yyyy-MM-dd'T'HH:mm:ss)
                                serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                            }
                            
                            serverFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            Date createdAt = serverFormat.parse(createdAtStr);
                            post.setCreatedAt(createdAt);
                        } catch (Exception e) {
                            Log.e(TAG, "날짜 파싱 오류: " + e.getMessage());
                            // 오류 시 현재 시간으로 설정
                            post.setCreatedAt(new Date());
                        }
                    } else {
                        // 시간이 없는 경우 현재 시간으로 설정
                        post.setCreatedAt(new Date());
                    }
                    
                    // 공지사항 여부 설정
                    post.setNotice("공지".equals(post.getCategory()));
                    
                    postList.add(post);
                } catch (Exception e) {
                    Log.e(TAG, "게시글 데이터 처리 오류: " + e.getMessage(), e);
                }
            }
        }
        
        // 카테고리 필터링
        filterPosts();
    }
    
    private void showEmptyState() {
        // API 호출 실패시 빈 상태로 표시
        postList.clear();
        filteredPostList.clear();
        updateUI();
    }
    
    private void showLoading(boolean isLoading) {
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_CREATE_POST || requestCode == REQUEST_DETAIL_POST) && resultCode == RESULT_OK) {
            // 게시글 작성 후 목록 새로고침
            loadPostsFromServer();
            if (requestCode == REQUEST_CREATE_POST) {
                Toast.makeText(this, "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
} 
