package com.example.smiti;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smiti.adapter.CommentAdapter;
import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.PostRequest;
import com.example.smiti.api.RetrofitClient;
import com.example.smiti.model.Comment;
import com.example.smiti.model.Post;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailActivity extends AppCompatActivity {

    private static final String TAG = "PostDetailActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final int REQUEST_EDIT_POST = 1001;

    private TextView tvTitle, tvContent, tvAuthor, tvDate, tvCategory, tvFileName;
    private TextView btnDownload;
    private ProgressBar progressBar;
    private View file_container;
    
    // 좋아요/싫어요 UI 요소
    private Button btnLike, btnDislike;
    private TextView tvLikeCount, tvDislikeCount;
    
    // 댓글 관련 UI 요소
    private RecyclerView recyclerComments;
    private EditText etComment;
    private Button btnSubmitComment;
    private CommentAdapter commentAdapter;

    private String postId;
    private Post post;
    private String userEmail;
    private String userName;
    private int likeCount = 0;
    private int dislikeCount = 0;
    private boolean hasLiked = false;
    private boolean hasDisliked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // 초기화
        initViews();
        setupToolbar();

        // 사용자 정보 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userEmail = sharedPreferences.getString("email", "");
        userName = sharedPreferences.getString("name", "");

        // 인텐트에서 게시글 ID 가져오기
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("post_id")) {
            postId = intent.getStringExtra("post_id");
            loadPostDetails(postId);
        } else {
            Toast.makeText(this, "게시글 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 파일 관련 뷰 초기화
        tvFileName = findViewById(R.id.tv_file_name);
        file_container = findViewById(R.id.file_container);
        
        // 댓글 어댑터 초기화
        commentAdapter = new CommentAdapter(this, userEmail);
        recyclerComments.setAdapter(commentAdapter);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        
        // 댓글 삭제 리스너 설정
        commentAdapter.setOnCommentDeleteListener(commentId -> deleteComment(commentId));
        
        // 좋아요/싫어요 버튼 리스너 설정
        setupReactionButtons();
        
        // 댓글 제출 버튼 리스너 설정
        btnSubmitComment.setOnClickListener(v -> submitComment());
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvContent = findViewById(R.id.tv_content);
        tvAuthor = findViewById(R.id.tv_author);
        tvDate = findViewById(R.id.tv_date);
        tvCategory = findViewById(R.id.tv_category);
        btnDownload = findViewById(R.id.btn_download);
        progressBar = findViewById(R.id.progress_bar);
        
        // 좋아요/싫어요 관련 뷰
        btnLike = findViewById(R.id.btn_like);
        btnDislike = findViewById(R.id.btn_dislike);
        tvLikeCount = findViewById(R.id.tv_like_count);
        tvDislikeCount = findViewById(R.id.tv_dislike_count);
        
        // 댓글 관련 뷰
        recyclerComments = findViewById(R.id.recycler_comments);
        etComment = findViewById(R.id.et_comment);
        btnSubmitComment = findViewById(R.id.btn_submit_comment);

        btnDownload.setOnClickListener(v -> downloadFile());
    }
    
    private void setupReactionButtons() {
        btnLike.setOnClickListener(v -> {
            if (hasLiked) {
                // 이미 좋아요를 눌렀으면 취소 로직 (서버에서 지원하지 않으므로 주석 처리)
                // removeLike();
                Toast.makeText(PostDetailActivity.this, "좋아요는 취소할 수 없습니다.", Toast.LENGTH_SHORT).show();
            } else {
                likePost();
            }
        });
        
        btnDislike.setOnClickListener(v -> {
            if (hasDisliked) {
                // 이미 싫어요를 눌렀으면 취소 로직 (서버에서 지원하지 않으므로 주석 처리)
                // removeDislike();
                Toast.makeText(PostDetailActivity.this, "싫어요는 취소할 수 없습니다.", Toast.LENGTH_SHORT).show();
            } else {
                dislikePost();
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("게시글 상세");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black));
    }

    private void loadPostDetails(String postId) {
        showLoading(true);
        
        try {
            // 문자열 게시글 ID를 정수로 변환
            final int postIdInt = Integer.parseInt(postId);
            
            Call<ApiResponse> call = RetrofitClient.getApiService().getPost(postIdInt);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                        
                    // 서버 응답 로그 확인
                    Log.d(TAG, "API 응답: " + apiResponse.toString());
                    
                    // 게시글 데이터 추출
                    Map<String, Object> postData = null;
                    
                    // 1. getPost() 메서드로 post 필드 가져오기
                    if (apiResponse.getPost() != null) {
                        postData = apiResponse.getPost();
                        Log.d(TAG, "post 필드에서 데이터 추출: " + postData);
                    } 
                    // 2. getData()로 데이터 가져오기
                    else if (apiResponse.getData() != null) {
                        postData = (Map<String, Object>) apiResponse.getData();
                        Log.d(TAG, "data 필드에서 데이터 추출: " + postData);
                    }
                    
                    // 데이터가 있으면 표시
                    if (postData != null) {
                        displayPostData(postData);
                        
                        // 댓글 로드
                        loadComments(postIdInt);
                    } else {
                        Log.e(TAG, "게시글 데이터 추출 실패: " + apiResponse.toString());
                        Toast.makeText(PostDetailActivity.this, "게시글 로드 실패: 데이터 형식 오류", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    // 오류 응답 처리
                    int responseCode = response.code();
                    Log.e(TAG, "게시글 로드 실패 - HTTP 응답 코드: " + responseCode);
                    String errorMessage = "서버 응답 오류: " + responseCode;
                    
                    // 오류 바디 로깅
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "오류 응답 본문: " + errorBody);
                        } catch (IOException e) {
                            Log.e(TAG, "오류 응답 본문 읽기 실패", e);
                        }
                    }
                    
                    Toast.makeText(PostDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
                
                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    showLoading(false);
                    Log.e(TAG, "API 호출 실패: " + t.getMessage(), t);
                    Toast.makeText(PostDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } catch (NumberFormatException e) {
            showLoading(false);
            Log.e(TAG, "잘못된 게시글 ID 형식: " + postId, e);
            Toast.makeText(this, "잘못된 게시글 ID 형식입니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayPostData(Map<String, Object> postData) {
        post = new Post();
        post.setId(String.valueOf(postData.get("post_id")));
        post.setTitle((String) postData.get("title"));
        post.setContent((String) postData.get("content"));
        post.setAuthorId((String) postData.get("email"));
        post.setAuthorName((String) postData.get("name"));
        post.setCategory((String) postData.get("board_type"));
        
        // 날짜 설정
        try {
            if (postData.get("created_at") != null) {
                String createdAtStr = postData.get("created_at").toString();
                SimpleDateFormat serverFormat;
                
                // 날짜 형식 처리 (밀리초 포함 여부에 따라 다른 포맷 사용)
                if (createdAtStr.contains(".")) {
                    serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                } else {
                    serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                }
                
                Date createdAt = serverFormat.parse(createdAtStr);
                post.setCreatedAt(createdAt);
                
                // 화면에 표시
                SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                tvDate.setText(displayFormat.format(createdAt));
            }
        } catch (Exception e) {
            Log.e(TAG, "날짜 파싱 오류: " + e.getMessage(), e);
            tvDate.setText("날짜 정보 없음");
        }

        // 파일 정보 설정
        if (postData.get("file_name") != null) {
            String fileName = (String) postData.get("file_name");
            post.setFileName(fileName);
            tvFileName.setText(fileName);
            file_container.setVisibility(View.VISIBLE);
            btnDownload.setVisibility(View.VISIBLE);
        } else {
            file_container.setVisibility(View.GONE);
            btnDownload.setVisibility(View.GONE);
        }

        // 좋아요/싫어요 수 설정
        if (postData.get("like_count") != null) {
            try {
                likeCount = Integer.parseInt(postData.get("like_count").toString());
                post.setLikeCount(likeCount);
                tvLikeCount.setText(String.valueOf(likeCount));
            } catch (NumberFormatException e) {
                Log.e(TAG, "좋아요 수 파싱 오류: " + e.getMessage(), e);
                tvLikeCount.setText("0");
            }
        }

        if (postData.get("dislike_count") != null) {
            try {
                dislikeCount = Integer.parseInt(postData.get("dislike_count").toString());
                tvDislikeCount.setText(String.valueOf(dislikeCount));
            } catch (NumberFormatException e) {
                Log.e(TAG, "싫어요 수 파싱 오류: " + e.getMessage(), e);
                tvDislikeCount.setText("0");
            }
        }

        // UI 업데이트
        tvTitle.setText(post.getTitle());
        tvContent.setText(post.getContent());
        tvAuthor.setText(post.getAuthorName());
        tvCategory.setText(post.getCategory());
        
        // 좋아요/싫어요 상태 업데이트
        updateLikeButtonState();
        updateDislikeButtonState();
    }
    
    private void updateLikeButtonState() {
        if (hasLiked) {
            btnLike.setText("👍 좋아요 취소");
        } else {
            btnLike.setText("👍 좋아요");
        }
    }
    
    private void updateDislikeButtonState() {
        if (hasDisliked) {
            btnDislike.setText("👎 싫어요 취소");
        } else {
            btnDislike.setText("👎 싫어요");
        }
    }
    
    private void likePost() {
        showLoading(true);
        
        try {
            int postIdInt = Integer.parseInt(post.getId());
            Map<String, String> request = new HashMap<>();
            request.put("email", userEmail);
            
            Call<ApiResponse> call = RetrofitClient.getApiService().likePost(postIdInt, request);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    showLoading(false);
                    
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            hasLiked = true;
                            likeCount++;
                            tvLikeCount.setText(String.valueOf(likeCount));
                            updateLikeButtonState();
                            Toast.makeText(PostDetailActivity.this, "좋아요를 눌렀습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(PostDetailActivity.this, "좋아요 실패: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(PostDetailActivity.this, "서버 응답 오류", Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    showLoading(false);
                    Toast.makeText(PostDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "API 호출 실패: " + t.getMessage(), t);
                }
            });
        } catch (NumberFormatException e) {
            showLoading(false);
            Toast.makeText(this, "잘못된 게시글 ID 형식입니다.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void dislikePost() {
        showLoading(true);
        
        try {
            int postIdInt = Integer.parseInt(post.getId());
            Map<String, String> request = new HashMap<>();
            request.put("email", userEmail);
            
            Call<ApiResponse> call = RetrofitClient.getApiService().dislikePost(postIdInt, request);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    showLoading(false);
                    
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            hasDisliked = true;
                            dislikeCount++;
                            tvDislikeCount.setText(String.valueOf(dislikeCount));
                            updateDislikeButtonState();
                            Toast.makeText(PostDetailActivity.this, "싫어요를 눌렀습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(PostDetailActivity.this, "싫어요 실패: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(PostDetailActivity.this, "서버 응답 오류", Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    showLoading(false);
                    Toast.makeText(PostDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "API 호출 실패: " + t.getMessage(), t);
                }
            });
        } catch (NumberFormatException e) {
            showLoading(false);
            Toast.makeText(this, "잘못된 게시글 ID 형식입니다.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadComments(int postId) {
        showLoading(true);
        
        // 새로운 API 엔드포인트 사용
        Call<ApiResponse> call = RetrofitClient.getApiService().getCommentsByPostId(postId);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    
                    // 로그 추가
                    Log.d(TAG, "댓글 API 응답: " + apiResponse.toString());
                    
                    // 1. getComments() 메서드로 comments 필드 먼저 확인
                    if (apiResponse.getComments() != null) {
                        List<Comment> comments = new ArrayList<>();
                        List<Map<String, Object>> commentDataList = apiResponse.getComments();
                        
                        for (Map<String, Object> commentData : commentDataList) {
                            Comment comment = parseCommentData(commentData);
                            if (comment != null) {
                                comments.add(comment);
                            }
                        }
                        
                        // 데이터 설정
                        commentAdapter.setComments(comments);
                        
                        // 댓글 수 업데이트
                        if (post != null) {
                            post.setCommentCount(comments.size());
                        }
                        
                        // 댓글이 없을 경우 메시지 표시
                        if (comments.isEmpty()) {
                            Toast.makeText(PostDetailActivity.this, "댓글이 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    
                    // 2. 이전 방식으로 데이터 파싱 (하위 호환성 유지)
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        try {
                            // 서버 응답에서 댓글 목록 추출
                            List<Comment> comments = new ArrayList<>();
                            
                            // 데이터가 List 형태인 경우
                            if (apiResponse.getData() instanceof List) {
                                List<Map<String, Object>> commentDataList = (List<Map<String, Object>>) apiResponse.getData();
                                for (Map<String, Object> commentData : commentDataList) {
                                    Comment comment = parseCommentData(commentData);
                                    if (comment != null) {
                                        comments.add(comment);
                                    }
                                }
                            } else if (apiResponse.getData() instanceof Map) {
                                // 데이터가 Map 형태로 담겨있고, 그 안에 comments라는 키가 있는 경우
                                Map<String, Object> dataMap = (Map<String, Object>) apiResponse.getData();
                                if (dataMap.containsKey("comments") && dataMap.get("comments") instanceof List) {
                                    List<Map<String, Object>> commentDataList = (List<Map<String, Object>>) dataMap.get("comments");
                                    for (Map<String, Object> commentData : commentDataList) {
                                        Comment comment = parseCommentData(commentData);
                                        if (comment != null) {
                                            comments.add(comment);
                                        }
                                    }
                                }
                            }
                            
                            // 데이터 설정
                            commentAdapter.setComments(comments);
                            
                            // 댓글 수 업데이트
                            if (post != null) {
                                post.setCommentCount(comments.size());
                            }
                            
                            // 댓글이 없을 경우 메시지 표시
                            if (comments.isEmpty()) {
                                Toast.makeText(PostDetailActivity.this, "댓글이 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "댓글 데이터 파싱 오류: " + e.getMessage(), e);
                            Toast.makeText(PostDetailActivity.this, "댓글 데이터 로드 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(PostDetailActivity.this, "댓글 로드 실패: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 404 같은 경우 처리
                    int responseCode = response.code();
                    Log.e(TAG, "댓글 로드 실패 - HTTP 응답 코드: " + responseCode);
                    
                    if (responseCode == 404) {
                        // 404 오류일 경우 빈 댓글 목록으로 처리
                        commentAdapter.setComments(new ArrayList<>());
                        Toast.makeText(PostDetailActivity.this, "해당 게시글에 대한 댓글이 없습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(PostDetailActivity.this, "서버 응답 오류: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(PostDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "API 호출 실패: " + t.getMessage(), t);
            }
        });
    }
    
    private Comment parseCommentData(Map<String, Object> commentData) {
        try {
            Comment comment = new Comment();
            
            // 댓글 ID 설정
            if (commentData.get("comment_id") != null) {
                comment.setId(Integer.parseInt(commentData.get("comment_id").toString()));
            }
            
            // 내용 설정
            if (commentData.get("content") != null) {
                comment.setContent(commentData.get("content").toString());
            }
            
            // 작성자 이메일 설정
            if (commentData.get("email") != null) {
                comment.setAuthorEmail(commentData.get("email").toString());
            }
            
            // 작성자 이름 설정
            if (commentData.get("name") != null) {
                comment.setAuthorName(commentData.get("name").toString());
            }
            
            // 게시글 ID 설정
            if (commentData.get("post_id") != null) {
                comment.setPostId(Integer.parseInt(commentData.get("post_id").toString()));
            }
            
            // 생성 날짜 설정
            if (commentData.get("created_at") != null) {
                try {
                    String createdAtStr = commentData.get("created_at").toString();
                    SimpleDateFormat serverFormat;
                    
                    // yyyy-MM-dd'T'HH:mm:ss.SSS'Z' 또는 yyyy-MM-dd'T'HH:mm:ss 형식 처리
                    if (createdAtStr.contains(".")) {
                        serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                    } else {
                        serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    }
                    
                    Date createdAt = serverFormat.parse(createdAtStr);
                    comment.setCreatedAt(createdAt);
                } catch (Exception e) {
                    Log.e(TAG, "날짜 파싱 오류: " + e.getMessage(), e);
                }
            }
            
            return comment;
        } catch (Exception e) {
            Log.e(TAG, "댓글 파싱 오류: " + e.getMessage(), e);
            return null;
        }
    }
    
    private void submitComment() {
        String commentContent = etComment.getText().toString().trim();
        
        if (commentContent.isEmpty()) {
            Toast.makeText(this, "댓글 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading(true);
        
        try {
            int postIdInt = Integer.parseInt(post.getId());
            Map<String, String> request = new HashMap<>();
            request.put("email", userEmail);
            request.put("content", commentContent);
            request.put("name", userName);
            
            Call<ApiResponse> call = RetrofitClient.getApiService().addComment(postIdInt, request);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    showLoading(false);
                    
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            Toast.makeText(PostDetailActivity.this, "댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                            
                            // 입력 필드 초기화
                            etComment.setText("");
                            
                            // 댓글 새로고침
                            loadComments(postIdInt);
                        } else {
                            Toast.makeText(PostDetailActivity.this, "댓글 등록 실패: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(PostDetailActivity.this, "서버 응답 오류", Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    showLoading(false);
                    Toast.makeText(PostDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "API 호출 실패: " + t.getMessage(), t);
                }
            });
        } catch (NumberFormatException e) {
            showLoading(false);
            Toast.makeText(this, "잘못된 게시글 ID 형식입니다.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void deleteComment(int commentId) {
        showLoading(true);
        
        Map<String, String> request = new HashMap<>();
        request.put("email", userEmail);
        
        Call<ApiResponse> call = RetrofitClient.getApiService().deleteCommentById(commentId, request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(PostDetailActivity.this, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        
                        // UI에서 댓글 제거
                        commentAdapter.removeComment(commentId);
                    } else {
                        Toast.makeText(PostDetailActivity.this, "댓글 삭제 실패: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(PostDetailActivity.this, "서버 응답 오류", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(PostDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "API 호출 실패: " + t.getMessage(), t);
            }
        });
    }

    private void downloadFile() {
        if (post == null || !post.hasFile()) {
            Toast.makeText(this, "다운로드할 파일이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        Toast.makeText(this, "파일 다운로드 중...", Toast.LENGTH_SHORT).show();

        Call<ResponseBody> call = RetrofitClient.getApiService().downloadBoardFile(post.getFileName());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    boolean saved = saveFile(response.body(), post.getFileName());
                    if (saved) {
                        Toast.makeText(PostDetailActivity.this, "파일 다운로드 완료", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(PostDetailActivity.this, "파일 저장 실패", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(PostDetailActivity.this, "파일 다운로드 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showLoading(false);
                Toast.makeText(PostDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "파일 다운로드 실패: " + t.getMessage(), t);
            }
        });
    }

    private boolean saveFile(ResponseBody body, String fileName) {
        try {
            File downloadsDir = getExternalFilesDir(null);
            File file = new File(downloadsDir, fileName);

            InputStream inputStream = body.byteStream();
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            // 파일을 열기 위한 인텐트 생성
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(file);
            intent.setDataAndType(uri, "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "파일 열기 실패: " + e.getMessage(), e);
                Toast.makeText(this, "이 파일을 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show();
            }

            return true;
        } catch (IOException e) {
            Log.e(TAG, "파일 저장 오류: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 본인 글인 경우에만 수정/삭제 메뉴 표시
        if (post != null && post.getAuthorId().equals(userEmail)) {
            getMenuInflater().inflate(R.menu.menu_post_detail, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_edit) {
            editPost();
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void editPost() {
        // TODO: 게시글 수정 화면으로 이동
        Intent intent = new Intent(this, PostEditActivity.class);
        intent.putExtra("post", post);
        startActivityForResult(intent, REQUEST_EDIT_POST);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("게시글 삭제")
                .setMessage("정말로 이 게시글을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deletePost())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deletePost() {
        showLoading(true);

        try {
            int postIdInt = Integer.parseInt(post.getId());
            Call<ApiResponse> call = RetrofitClient.getApiService().deletePost(postIdInt);

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(PostDetailActivity.this, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(PostDetailActivity.this, "삭제 실패: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(PostDetailActivity.this, "서버 응답 오류", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(PostDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "API 호출 실패: " + t.getMessage(), t);
            }
        });
        } catch (NumberFormatException e) {
            showLoading(false);
            Toast.makeText(this, "잘못된 게시글 ID 형식입니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "잘못된 게시글 ID 형식: " + post.getId(), e);
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_POST && resultCode == RESULT_OK) {
            // 게시글이 수정되었으면 새로고침
            loadPostDetails(postId);
        }
    }
}