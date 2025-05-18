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
import android.content.ActivityNotFoundException; // 추가
import android.os.Build; // 추가

import androidx.core.content.FileProvider; // 추가
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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

import de.hdodenhof.circleimageview.BuildConfig;
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
    private boolean isFirstLoad = true;
    private ApiResponse cachedResponse = null;

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

        // 댓글 제출 버튼 리스너 설정 (수정된 부분)
        btnSubmitComment.setOnClickListener(v -> {
            if (post != null && post.getId() != null) {
                submitComment();
            } else {
                Toast.makeText(PostDetailActivity.this, "게시글 정보를 로딩 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
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
            // 이미 싫어요를 누른 경우 좋아요 불가능
            if (hasDisliked) {
                Toast.makeText(PostDetailActivity.this, "이미 싫어요를 누른 게시글입니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 이미 좋아요를 누른 경우 중복 방지
            if (hasLiked) {
                Toast.makeText(PostDetailActivity.this, "이미 좋아요를 누른 게시글입니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            likePost();
        });
        
        btnDislike.setOnClickListener(v -> {
            // 이미 좋아요를 누른 경우 싫어요 불가능
            if (hasLiked) {
                Toast.makeText(PostDetailActivity.this, "이미 좋아요를 누른 게시글입니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 이미 싫어요를 누른 경우 중복 방지
            if (hasDisliked) {
                Toast.makeText(PostDetailActivity.this, "이미 싫어요를 누른 게시글입니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            dislikePost();
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
            final int postIdInt = Integer.parseInt(postId);
            // 현재 사용자의 이메일을 쿼리 파라미터로 전달하여 좋아요/싫어요 상태 확인
            Call<ApiResponse> call = RetrofitClient.getApiService().getPost(postIdInt, userEmail);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        Log.d(TAG, "loadPostDetails() - 전체 API 응답: " + apiResponse.toString()); // 전체 응답 로그 확인 (디버깅용)
                        cachedResponse = apiResponse; // 응답 캐시 저장
                        displayPostData(apiResponse); // ApiResponse 전체를 전달
                        btnSubmitComment.setEnabled(true);
                    } else {
                        Log.e(TAG, "loadPostDetails() - 서버 응답 오류: " + response.code());
                        Toast.makeText(PostDetailActivity.this, "서버 응답 오류", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    showLoading(false);
                    Log.e(TAG, "loadPostDetails() - API 호출 실패: " + t.getMessage());
                    Toast.makeText(PostDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } catch (NumberFormatException e) {
            showLoading(false);
            Log.e(TAG, "loadPostDetails() - 잘못된 게시글 ID 형식: " + postId);
            Toast.makeText(this, "잘못된 게시글 ID 형식입니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private void displayPostData(ApiResponse apiResponse) {
        Log.d(TAG, "displayPostData() 호출됨 - 전달받은 ApiResponse: " + (apiResponse != null ? apiResponse.toString() : "null"));

        if (apiResponse != null && apiResponse.getPost() != null) {
            Map<String, Object> postData = apiResponse.getPost();
            
            // Ensure post object is initialized
            if (this.post == null) {
                this.post = new Post();
            }

            // ID 및 기본 정보 설정
            try {
                Object idObj = postData.get("id");
                if (idObj != null) {
                    this.post.setId(String.valueOf(((Number) idObj).intValue()));
                    Log.d(TAG, "displayPostData() - post.getId(): " + this.post.getId());
                } else {
                    Log.e(TAG, "displayPostData() - Post ID is null from server.");
                    // Handle error: maybe finish activity or show error message
                    Toast.makeText(this, "게시글 ID를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "displayPostData() - Error parsing Post ID: " + postData.get("id"), e);
                Toast.makeText(this, "게시글 ID 파싱 오류.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            this.post.setTitle((String) postData.get("title"));
            this.post.setContent((String) postData.get("content"));
            this.post.setAuthorId((String) postData.get("email")); // Assuming 'email' is authorId
            this.post.setAuthorName((String) postData.get("name")); // Assuming 'name' is authorName
            this.post.setCategory((String) postData.get("board_type"));

            // 날짜 설정 (기존 로직 유지)
            try {
                if (postData.get("created_at") != null) {
                    String createdAtStr = postData.get("created_at").toString();
                    SimpleDateFormat serverFormat;
                    if (createdAtStr.contains(".")) {
                        serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                    } else {
                        serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    }
                    Date createdAt = serverFormat.parse(createdAtStr);
                    this.post.setCreatedAt(createdAt);
                    SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    tvDate.setText(displayFormat.format(createdAt));
                }
            } catch (Exception e) {
                Log.e(TAG, "날짜 파싱 오류: " + e.getMessage(), e);
                tvDate.setText("날짜 정보 없음");
            }

            // 파일 정보 설정 (수정됨: file_path에서 순수 파일 이름 추출)
            Object filePathObj = postData.get("file_path"); // 1. "file_path" 키로 객체를 가져옵니다.
            if (filePathObj != null && !filePathObj.toString().isEmpty()) { // 2. null이 아니고 빈 문자열이 아닌지 확인합니다.
                String fullFilePath = filePathObj.toString(); // 3. 객체를 문자열로 변환합니다 (예: "board_uploads/실제파일이름.pdf")
                String extractedFileName = fullFilePath; // 4. 추출된 파일 이름을 담을 변수, 기본값은 전체 경로

                // 5. 경로 구분자 '/'가 있는지 확인하고, 있다면 마지막 '/' 이후의 문자열을 추출합니다.
                int lastSeparatorIndex = fullFilePath.lastIndexOf('/');
                if (lastSeparatorIndex != -1 && lastSeparatorIndex < fullFilePath.length() - 1) {
                    // 마지막 '/' 다음 문자부터 끝까지 추출
                    extractedFileName = fullFilePath.substring(lastSeparatorIndex + 1);
                }
                // 만약 '/'가 없거나 맨 마지막 문자라면, fullFilePath 자체가 파일 이름으로 간주됩니다 (위 기본값 유지).

                // 6. Post 객체가 null이 아닌지 확인 후 파일 이름 설정
                if (this.post != null) {
                    this.post.setFileName(extractedFileName); // <<--- 핵심!!! 추출된 순수 파일 이름을 Post 객체에 저장
                }

                // 7. 화면의 TextView에는 추출된 순수 파일 이름을 표시
                tvFileName.setText(extractedFileName);
                file_container.setVisibility(View.VISIBLE);
                btnDownload.setVisibility(View.VISIBLE);
                Log.d(TAG, "파일 표시: " + extractedFileName + " (원본 경로: " + fullFilePath + ")");
            } else {
                // 8. 파일 정보가 없는 경우의 처리
                if (this.post != null) {
                    this.post.setFileName(null); // Post 객체의 파일 이름도 null로
                }
                file_container.setVisibility(View.GONE);
                btnDownload.setVisibility(View.GONE);
                Log.d(TAG, "파일 정보 없음. file_path 값: " + filePathObj);
            }
            // 상세 로깅: 서버로부터 받은 원시 데이터
            Log.d(TAG, "displayPostData - Raw from server: like_count=" + postData.get("like_count") +
                    ", dislike_count=" + postData.get("dislike_count") +
                    ", user_like_status=" + postData.get("user_like_status"));

            // 좋아요 수 설정
            Object likeCountObj = postData.get("likes");
            if (likeCountObj != null) {
                try {
                    this.likeCount = ((Number) likeCountObj).intValue();
                } catch (ClassCastException | NumberFormatException e1) {
                    Log.w(TAG, "Like count direct number parsing failed (" + likeCountObj + "), trying string parsing.", e1);
                    try {
                        this.likeCount = Integer.parseInt(likeCountObj.toString());
                    } catch (NumberFormatException e2) {
                        Log.e(TAG, "Error parsing like_count as string: " + likeCountObj, e2);
                        this.likeCount = 0; // 파싱 실패 시 0으로 설정
                    }
                }
            } else {
                Log.d(TAG, "like_count is null from server.");
                this.likeCount = 0;
            }
            if (this.post != null) this.post.setLikeCount(this.likeCount); // Post 객체에도 반영
            tvLikeCount.setText(String.valueOf(this.likeCount));

            // 싫어요 수 설정
            Object dislikeCountObj = postData.get("dislikes");
            if (dislikeCountObj != null) {
                try {
                    this.dislikeCount = ((Number) dislikeCountObj).intValue();
                } catch (ClassCastException | NumberFormatException e1) {
                    Log.w(TAG, "Dislike count direct number parsing failed (" + dislikeCountObj + "), trying string parsing.", e1);
                    try {
                        this.dislikeCount = Integer.parseInt(dislikeCountObj.toString());
                    } catch (NumberFormatException e2) {
                        Log.e(TAG, "Error parsing dislike_count as string: " + dislikeCountObj, e2);
                        this.dislikeCount = 0; // 파싱 실패 시 0으로 설정
                    }
                }
            } else {
                Log.d(TAG, "dislike_count is null from server.");
                this.dislikeCount = 0;
            }
            if (this.post != null) this.post.setDislikeCount(this.dislikeCount); // Post 객체에도 반영
            tvDislikeCount.setText(String.valueOf(this.dislikeCount));
            
            // 사용자의 좋아요/싫어요 상태 확인
            Object userLikeStatusObj = postData.get("user_like_status");
            if (userLikeStatusObj != null) {
                String likeStatus = userLikeStatusObj.toString();
                this.hasLiked = "liked".equals(likeStatus);
                this.hasDisliked = "disliked".equals(likeStatus);
            } else {
                Log.d(TAG, "user_like_status is null from server.");
                this.hasLiked = false;
                this.hasDisliked = false;
            }
            // Note: Assuming Post model does not need to store hasLiked/hasDisliked for now.

            // 최종 파싱된 값 로깅
            Log.d(TAG, "displayPostData - Parsed values: likeCount=" + this.likeCount +
                    ", dislikeCount=" + this.dislikeCount +
                    ", hasLiked=" + this.hasLiked + ", hasDisliked=" + this.hasDisliked);

            // 나머지 UI 업데이트
            tvTitle.setText(this.post.getTitle());
            tvContent.setText(this.post.getContent());
            tvAuthor.setText(this.post.getAuthorName());
            tvCategory.setText(this.post.getCategory());

            updateLikeButtonState(); // 버튼 텍스트 등 상태 업데이트 (내용은 변경하지 않음)
            updateDislikeButtonState();

        } else {
            Log.e(TAG, "displayPostData - ApiResponse or Post data is null.");
            Toast.makeText(this, "게시글 데이터를 표시할 수 없습니다.", Toast.LENGTH_SHORT).show();
            // Optionally finish activity or show a more specific error UI
        }

        // 댓글 목록 처리 (기존 로직 유지)
        if (apiResponse != null && apiResponse.getComments() != null && !apiResponse.getComments().isEmpty()) {
            List<Map<String, Object>> commentDataList = apiResponse.getComments();
            Log.d(TAG, "displayPostData() - commentDataList 크기: " + commentDataList.size());
            List<Comment> comments = new ArrayList<>();
            for (Map<String, Object> commentData : commentDataList) {
                Comment comment = parseCommentData(commentData);
                if (comment != null) {
                    comments.add(comment);
                }
            }
            commentAdapter.setComments(comments);
            commentAdapter.notifyDataSetChanged();
        } else {
            Log.d(TAG, "displayPostData - No comments in ApiResponse or comments list is empty.");
            if (commentAdapter != null) { // Ensure adapter is initialized
                 commentAdapter.setComments(new ArrayList<>());
                 commentAdapter.notifyDataSetChanged();
            }
        }
    }
    private void updateLikeButtonState() {
        // 취소 기능이 필요없으므로 항상 동일한 텍스트 표시
        btnLike.setText("👍 좋아요");
    }
    
    private void updateDislikeButtonState() {
        // 취소 기능이 필요없으므로 항상 동일한 텍스트 표시
        btnDislike.setText("👎 싫어요");
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
                    showLoading(false); // loadPostDetails가 자체적으로 로딩 관리를 하므로 여기서 false로 설정
                    
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            hasLiked = true; // 중복 클릭 방지를 위해 로컬 상태 우선 업데이트
                            // updateLikeButtonState(); // loadPostDetails 후 displayPostData에서 호출됨
                            Toast.makeText(PostDetailActivity.this, "좋아요를 눌렀습니다.", Toast.LENGTH_SHORT).show();
                            loadPostDetails(postId); // 서버로부터 최신 정보 로드하여 UI 전체 업데이트
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
                    showLoading(false); // loadPostDetails가 자체적으로 로딩 관리를 하므로 여기서 false로 설정
                    
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            hasDisliked = true; // 중복 클릭 방지를 위해 로컬 상태 우선 업데이트
                            // updateDislikeButtonState(); // loadPostDetails 후 displayPostData에서 호출됨
                            Toast.makeText(PostDetailActivity.this, "싫어요를 눌렀습니다.", Toast.LENGTH_SHORT).show();
                            loadPostDetails(postId); // 서버로부터 최신 정보 로드하여 UI 전체 업데이트
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

    private Comment parseCommentData(Map<String, Object> commentData) {
        Log.d(TAG, "parseCommentData() - commentData: " + commentData.toString()); // 이 로그 추가
        try {
            Comment comment = new Comment();

            // 댓글 ID 설정
            // 댓글 ID 설정
            if (commentData.get("id") != null) {
                try {
                    double idDouble = ((Number) commentData.get("id")).doubleValue();
                    comment.setId((int) idDouble);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "댓글 ID 파싱 오류 (Double -> Int): " + e.getMessage(), e);
                    // 파싱 실패 시 처리 (예: 기본값 설정 또는 댓글 객체 null 반환)
                    return null; // 파싱 실패한 댓글은 건너뛰도록 처리
                }
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

                    //yyyy-MM-dd'T'HH:mm:ss.SSS'Z' 또는 yyyy-MM-dd'T'HH:mm:ss 형식 처리
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
                    Log.d(TAG, "addComment() 응답 코드: " + response.code());

                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        Log.d(TAG, "addComment() 응답 성공: " + apiResponse.toString());
                        if (apiResponse.isSuccess()) {
                            Toast.makeText(PostDetailActivity.this, "댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                            etComment.setText("");

                            // 새로 작성된 댓글 정보가 응답에 포함되어 있다면 UI 업데이트
                            Map<String, Object> newCommentData = apiResponse.getNewComment();
                            if (newCommentData != null) {
                                Comment newComment = parseCommentData(newCommentData);
                                if (newComment != null) {
                                    commentAdapter.addComment(newComment);
                                    recyclerComments.scrollToPosition(commentAdapter.getItemCount() - 1); // 마지막 댓글로 스크롤
                                }
                            } else {
                                // 응답에 새 댓글 정보가 없으면 게시글 상세 정보 다시 로드 (기존 방식 유지)
                                loadPostDetails(postId);
                            }
                        } else {
                            Log.e(TAG, "댓글 등록 실패: " + apiResponse.getMessage());
                            Toast.makeText(PostDetailActivity.this, "댓글 등록 실패: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "addComment() 응답 실패: " + response.code() + ", ");
                        if (response.errorBody() != null) {
                            try {
                                Log.e(TAG, "오류 응답 본문: " + response.errorBody().string());
                            } catch (IOException e) {
                                Log.e(TAG, "오류 응답 본문 읽기 실패", e);
                            }
                        } else {
                            Log.e(TAG, "오류 응답 본문 없음");
                        }
                        Toast.makeText(PostDetailActivity.this, "서버 응답 오류", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    showLoading(false);
                    Log.e(TAG, "addComment() API 호출 실패: " + t.getMessage(), t);
                    Toast.makeText(PostDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            showLoading(false);
            Toast.makeText(this, "잘못된 게시글 ID 형식입니다.", Toast.LENGTH_SHORT).show();
        }
    }
    private void deleteComment(int commentId) {
        showLoading(true);
        Log.d(TAG, "deleteComment() 호출 - 댓글 ID: " + commentId + ", userEmail: " + userEmail); // 이 로그 추가

        Map<String, String> request = new HashMap<>();
        request.put("email", userEmail);

        Call<ApiResponse> call = RetrofitClient.getApiService().deleteCommentById(commentId, request); // 요청 Body에 email을 담아 보냄
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
        Log.d(TAG, "downloadFile() 메소드 호출됨");
        if (post == null || post.getFileName() == null || post.getFileName().isEmpty()) {
            Toast.makeText(this, "다운로드할 파일 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "다운로드 시도 실패: post 또는 fileName이 null이거나 비어있음. post: " + post + (post != null ? ", fileName: " + post.getFileName() : ""));
            return;
        }

        showLoading(true);
        String fileNameForDownload = post.getFileName();
        Log.i(TAG, "downloadFile - post.getFileName()으로 가져온 값: '" + fileNameForDownload + "'");
        Toast.makeText(this, "파일 다운로드 중: " + fileNameForDownload, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "다운로드 요청 시작 - 파일 이름: " + fileNameForDownload);

        Call<ResponseBody> call = RetrofitClient.getApiService().downloadBoardFile(post.getFileName());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                showLoading(false);
                Log.i(TAG, "downloadFile onResponse - HTTP Code: " + response.code() + ", Message: " + response.message()); // 이 로그 추가

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "다운로드 응답 성공. Content-Type: " + response.headers().get("Content-Type") + ", Content-Length: " + response.body().contentLength());
                    boolean saved = saveFile(response.body(), post.getFileName());
                    // ...
                } else {
                    Toast.makeText(PostDetailActivity.this, "파일 다운로드 실패 (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "파일 다운로드 서버 응답 실패 - Code: " + response.code() + ", Message: " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            String errorBodyString = response.errorBody().string();
                            Log.e(TAG, "Error Body: " + errorBodyString);
                        } catch (IOException e) {
                            Log.e(TAG, "Error body parsing failed", e);
                        }
                    } else {
                        Log.e(TAG, "Error Body is null.");
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showLoading(false);
                Toast.makeText(PostDetailActivity.this, "네트워크 오류 (파일 다운로드): " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "파일 다운로드 네트워크 onFailure: ", t); // Throwable 전체 로깅
            }
        });
    }

    private boolean saveFile(ResponseBody body, String fileName) {
        try {
            File downloadsDir = getExternalFilesDir(null);
            if (downloadsDir == null) {
                Log.e(TAG, "saveFile - External storage not available.");
                Toast.makeText(this, "외부 저장소를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return false;
            }
            File file = new File(downloadsDir, fileName);

            InputStream inputStream = null; // finally 블록에서 닫기 위해 try 블록 밖으로 이동
            OutputStream outputStream = null; // ""

            try {
                inputStream = body.byteStream();
                outputStream = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush(); // 쓰기 완료 후 버퍼 비우기
                Log.d(TAG, "파일 저장 완료: " + file.getAbsolutePath());

                // 파일을 열기 위한 인텐트 생성 (FileProvider 사용)
                Uri uri;
                // Android N (API 24) 이상에서는 FileProvider를 사용해야 함
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // authorities는 AndroidManifest.xml에 정의된 것과 일치해야 함
                    // BuildConfig.APPLICATION_ID는 현재 앱의 패키지 이름을 가져옴
                    try {
                        // !!!!! 여기가 핵심 수정 부분 !!!!!
                        uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "FileProvider.getUriForFile() 실패: authorities를 확인하세요. AndroidManifest.xml에 provider가 올바르게 선언되었는지, res/xml/file_paths.xml 파일이 올바른지 확인하세요.", e);
                        Toast.makeText(this, "파일을 열 수 없습니다 (Provider 구성 오류).", Toast.LENGTH_LONG).show();
                        return false; // FileProvider 오류 시 진행 중단
                    }
                } else {
                    // API 24 미만에서는 기존 방식 사용
                    uri = Uri.fromFile(file);
                }

                Intent intent = new Intent(Intent.ACTION_VIEW);
                String mimeType = getContentResolver().getType(uri); // URI로부터 MIME 타입 가져오기
                if (mimeType == null) {
                    // MIME 타입을 결정할 수 없는 경우, 일반적인 타입으로 설정
                    mimeType = "*/*";
                }
                Log.d(TAG, "파일 열기 시도 - URI: " + uri.toString() + ", MIME Type: " + mimeType);

                intent.setDataAndType(uri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 다른 앱에 URI 읽기 권한 부여
                // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 필요에 따라 추가 (Activity 외부에서 시작 시)

                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) { // 구체적인 예외 타입으로 변경
                    Log.e(TAG, "파일 열기 실패: 해당 파일을 열 수 있는 앱이 없습니다. URI: " + uri + ", MIME: " + mimeType, e);
                    Toast.makeText(this, "이 파일을 열 수 있는 앱이 설치되어 있지 않습니다.", Toast.LENGTH_LONG).show();
                }

                return true;
            } catch (IOException e) {
                Log.e(TAG, "파일 쓰기/읽기 오류: " + e.getMessage(), e);
                Toast.makeText(this, "파일 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                return false;
            } finally {
                // 스트림을 안전하게 닫습니다.
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "스트림 닫기 오류: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) { // 그 외 예외 처리 (예: FileOutputStream 생성 실패 등)
            Log.e(TAG, "saveFile에서 예기치 않은 오류 발생: " + e.getMessage(), e);
            Toast.makeText(this, "파일 처리 중 알 수 없는 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
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
            // 게시글 수정 후 상세 정보 다시 로드
            loadPostDetails(postId);
            setResult(RESULT_OK); // 게시글 목록을 새로고침하도록 결과 설정
            Toast.makeText(this, "게시글이 수정되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (postId != null) {
            if (isFirstLoad) {
                // 첫 번째 onResume 호출 (onCreate 직후). 
                // onCreate에서 loadPostDetails가 이미 호출되었으므로 여기서는 호출하지 않음.
                // isFirstLoad를 false로 설정하여 다음 onResume부터는 새로고침하도록 함.
                isFirstLoad = false;
            } else {
                // 첫 번째 onResume이 아닌 경우 (화면으로 돌아온 경우)
                // 서버로부터 최신 정보를 로드합니다.
                Log.d(TAG, "onResume: Subsequent call, refreshing post details for postId: " + postId);
                loadPostDetails(postId);
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        // 뒤로가기 버튼 누를 때 RESULT_OK로 설정해서 게시글 목록이 업데이트되도록 함
        setResult(RESULT_OK);
        super.onBackPressed();
    }
}
