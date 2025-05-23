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
import android.content.ActivityNotFoundException;
import android.os.Build;

import androidx.core.content.FileProvider;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smiti.adapter.CommentAdapter;
import com.example.smiti.api.ApiResponse;
// import com.example.smiti.api.PostRequest; // 현재 코드에서 직접 사용되지 않음
import com.example.smiti.api.RetrofitClient;
import com.example.smiti.model.Comment;
import com.example.smiti.model.Post;

// import com.google.gson.Gson; // 디버깅에 필요하면 주석 해제
// import com.google.gson.GsonBuilder; // 디버깅에 필요하면 주석 해제

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
import java.util.TimeZone; // 추가

// import de.hdodenhof.circleimageview.BuildConfig; // 사용하지 않는다면 제거 가능
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailActivity extends AppCompatActivity {

    private static final String TAG = "PostDetailActivity";
    private static final String PREF_NAME = "LoginPrefs"; // BoardActivity와 동일한 이름 사용
    private static final String KEY_USER_EMAIL = "email"; // SharedPreferences 키 (일관성 유지)
    private static final String KEY_USER_NAME = "name";   // SharedPreferences 키 (일관성 유지)
    private static final String KEY_USER_ADMIN_STATUS = "admin"; // SharedPreferences 키 (일관성 유지)
    private static final int REQUEST_EDIT_POST = 1001;

    private TextView tvTitle, tvContent, tvAuthor, tvDate, tvCategory, tvFileName;
    private TextView btnDownload; // XML에서 Button일 수 있으므로, 실제 타입에 맞게 캐스팅 필요
    private ProgressBar progressBar;
    private View file_container;

    private Button btnLike, btnDislike;
    private TextView tvLikeCount, tvDislikeCount;

    private RecyclerView recyclerComments;
    private EditText etComment;
    private Button btnSubmitComment;
    private CommentAdapter commentAdapter;

    private String postId;
    private Post post; // 현재 보고 있는 게시글 객체
    private String userEmail; // 현재 로그인한 사용자의 이메일
    private String userName;  // 현재 로그인한 사용자의 이름
    private boolean isAdmin = false; // 현재 로그인한 사용자가 관리자인지 여부

    private int likeCount = 0;
    private int dislikeCount = 0;
    private boolean hasLiked = false;
    private boolean hasDisliked = false;
    private boolean isFirstLoad = true;
    // private ApiResponse cachedResponse = null; // 현재 직접 사용되지 않음

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);
        Log.d(TAG, "onCreate called");

        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userEmail = sharedPreferences.getString(KEY_USER_EMAIL, "");
        userName = sharedPreferences.getString(KEY_USER_NAME, "");
        isAdmin = (sharedPreferences.getInt(KEY_USER_ADMIN_STATUS, 0) == 1);
        Log.i(TAG, "Current User - Email: " + userEmail + ", Name: " + userName + ", IsAdmin: " + isAdmin);

        initViews();
        setupToolbar();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("post_id")) {
            postId = intent.getStringExtra("post_id");
            Log.d(TAG, "Received post_id: " + postId);
            if (postId != null && !postId.isEmpty()) {
                loadPostDetails(postId);
            } else {
                Toast.makeText(this, "유효하지 않은 게시글 ID입니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "게시글 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No post_id found in intent.");
            finish();
            return;
        }

        commentAdapter = new CommentAdapter(this, userEmail);
        recyclerComments.setAdapter(commentAdapter);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter.setOnCommentDeleteListener(this::deleteComment);

        setupReactionButtons();

        if (btnSubmitComment != null) { // Null 체크
            btnSubmitComment.setOnClickListener(v -> {
                if (post != null && post.getId() != null) {
                    submitComment();
                } else {
                    Toast.makeText(PostDetailActivity.this, "게시글 정보를 로딩 중입니다.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initViews() {
        Log.d(TAG, "initViews called");
        tvTitle = findViewById(R.id.tv_title);
        tvContent = findViewById(R.id.tv_content);
        tvAuthor = findViewById(R.id.tv_author);
        tvDate = findViewById(R.id.tv_date);
        tvCategory = findViewById(R.id.tv_category);
        btnDownload = findViewById(R.id.btn_download);
        progressBar = findViewById(R.id.progress_bar);
        file_container = findViewById(R.id.file_container);
        tvFileName = findViewById(R.id.tv_file_name);

        btnLike = findViewById(R.id.btn_like);
        btnDislike = findViewById(R.id.btn_dislike);
        tvLikeCount = findViewById(R.id.tv_like_count);
        tvDislikeCount = findViewById(R.id.tv_dislike_count);

        recyclerComments = findViewById(R.id.recycler_comments);
        etComment = findViewById(R.id.et_comment);
        btnSubmitComment = findViewById(R.id.btn_submit_comment);

        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> downloadFile());
        } else {
            Log.w(TAG, "btnDownload (R.id.btn_download) is null.");
        }
    }

    private void setupReactionButtons() {
        if (btnLike != null) {
            btnLike.setOnClickListener(v -> {
                if (hasDisliked) { Toast.makeText(PostDetailActivity.this, "이미 싫어요를 누른 게시글입니다.", Toast.LENGTH_SHORT).show(); return; }
                if (hasLiked) { Toast.makeText(PostDetailActivity.this, "이미 좋아요를 누른 게시글입니다.", Toast.LENGTH_SHORT).show(); return; }
                likePost();
            });
        }
        if (btnDislike != null) {
            btnDislike.setOnClickListener(v -> {
                if (hasLiked) { Toast.makeText(PostDetailActivity.this, "이미 좋아요를 누른 게시글입니다.", Toast.LENGTH_SHORT).show(); return; }
                if (hasDisliked) { Toast.makeText(PostDetailActivity.this, "이미 싫어요를 누른 게시글입니다.", Toast.LENGTH_SHORT).show(); return; }
                dislikePost();
            });
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            Log.e(TAG, "Toolbar (R.id.toolbar) not found in layout.");
            return;
        }
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("게시글 상세");
        }
    }

    private void loadPostDetails(String currentPostId) {
        if (currentPostId == null || currentPostId.isEmpty()) {
            Log.e(TAG, "loadPostDetails: currentPostId is null or empty.");
            Toast.makeText(this, "게시글 ID가 유효하지 않습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        showLoading(true);
        try {
            final int postIdInt = Integer.parseInt(currentPostId);
            Log.d(TAG, "Loading post details for ID: " + postIdInt + ", by user: " + userEmail);
            Call<ApiResponse> call = RetrofitClient.getApiService().getPost(postIdInt, userEmail);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        displayPostData(apiResponse);
                        if (btnSubmitComment != null) btnSubmitComment.setEnabled(true);
                    } else {
                        Log.e(TAG, "loadPostDetails - Server error: " + response.code() + " for postId: " + postIdInt);
                        Toast.makeText(PostDetailActivity.this, "게시글 로드 실패 (서버: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    showLoading(false);
                    Log.e(TAG, "loadPostDetails - API call failure for postId: " + postIdInt, t);
                    Toast.makeText(PostDetailActivity.this, "네트워크 오류로 게시글을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            showLoading(false);
            Log.e(TAG, "loadPostDetails - Invalid post ID format: " + currentPostId, e);
            Toast.makeText(this, "잘못된 게시글 ID 형식입니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayPostData(ApiResponse apiResponse) {
        Log.d(TAG, "displayPostData called.");
        if (apiResponse == null || apiResponse.getPost() == null) {
            Log.e(TAG, "displayPostData - ApiResponse or Post data is null.");
            Toast.makeText(this, "게시글 데이터를 표시할 수 없습니다.", Toast.LENGTH_SHORT).show();
            invalidateOptionsMenu();
            return;
        }

        Map<String, Object> postData = apiResponse.getPost();
        this.post = new Post(); // 새 객체 또는 기존 객체 재사용 결정

        try {
            Object idObj = postData.get("id");
            if (idObj instanceof Number) this.post.setId(String.valueOf(((Number) idObj).intValue()));
            else if (idObj != null) this.post.setId(String.valueOf(idObj));
            else { Log.e(TAG, "Post ID is null from server."); finish(); return; }
        } catch (Exception e) { Log.e(TAG, "Error parsing Post ID", e); finish(); return; }

        this.post.setTitle(getStringFromMap(postData, "title"));
        this.post.setContent(getStringFromMap(postData, "content"));
        this.post.setAuthorId(getStringFromMap(postData, "email"));
        this.post.setAuthorName(getStringFromMap(postData, "name"));
        this.post.setCategory(getStringFromMap(postData, "board_type")); // API 응답의 board_type 저장

        try {
            String createdAtStr = getStringFromMap(postData, "created_at");
            if (createdAtStr != null) {
                // UTC 시간 파싱 및 로컬 시간 변환을 위한 포매터 설정
                SimpleDateFormat serverFormat = new SimpleDateFormat(createdAtStr.contains(".") ? "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" : "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
                serverFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date createdAt = serverFormat.parse(createdAtStr);
                this.post.setCreatedAt(createdAt);

                SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                // displayFormat.setTimeZone(TimeZone.getDefault()); // 시스템 기본 타임존으로 표시
                tvDate.setText(displayFormat.format(createdAt));
            } else { tvDate.setText("날짜 정보 없음"); }
        } catch (Exception e) { Log.e(TAG, "Date parsing error", e); tvDate.setText("날짜 형식 오류"); }

        Object filePathObj = postData.get("file_path");
        if (filePathObj != null && !filePathObj.toString().isEmpty()) {
            String fullFilePath = filePathObj.toString();
            String extractedFileName = fullFilePath.substring(fullFilePath.lastIndexOf('/') + 1);
            this.post.setFileName(extractedFileName);
            if (tvFileName != null) tvFileName.setText(extractedFileName);
            if (file_container != null) file_container.setVisibility(View.VISIBLE);
            if (btnDownload != null) btnDownload.setVisibility(View.VISIBLE);
        } else {
            this.post.setFileName(null);
            if (file_container != null) file_container.setVisibility(View.GONE);
            if (btnDownload != null) btnDownload.setVisibility(View.GONE);
        }

        this.likeCount = getIntFromMap(postData, "likes", 0);
        this.post.setLikeCount(this.likeCount);
        tvLikeCount.setText(String.valueOf(this.likeCount));

        this.dislikeCount = getIntFromMap(postData, "dislikes", 0);
        this.post.setDislikeCount(this.dislikeCount);
        tvDislikeCount.setText(String.valueOf(this.dislikeCount));

        Object userLikeStatusObj = postData.get("user_like_status");
        if (userLikeStatusObj != null) {
            String likeStatus = userLikeStatusObj.toString();
            this.hasLiked = "liked".equals(likeStatus);
            this.hasDisliked = "disliked".equals(likeStatus);
        } else { this.hasLiked = false; this.hasDisliked = false; }

        tvTitle.setText(this.post.getTitle());
        tvContent.setText(this.post.getContent());
        tvAuthor.setText(this.post.getAuthorName());
        tvCategory.setText(mapApiBoardTypeToDisplay(this.post.getCategory())); // 화면 표시용으로 변환

        updateLikeButtonState();
        updateDislikeButtonState();
        invalidateOptionsMenu();

        if (apiResponse.getComments() != null && !apiResponse.getComments().isEmpty()) {
            List<Map<String, Object>> commentDataList = apiResponse.getComments();
            List<Comment> comments = new ArrayList<>();
            for (Map<String, Object> commentData : commentDataList) {
                Comment comment = parseCommentData(commentData);
                if (comment != null) comments.add(comment);
            }
            commentAdapter.setComments(comments);
        } else {
            if (commentAdapter != null) commentAdapter.setComments(new ArrayList<>());
        }
    }

    private String mapApiBoardTypeToDisplay(String apiBoardType) {
        if (apiBoardType == null) return "기타";
        switch (apiBoardType) {
            case "공지": return "공지사항";
            case "자유": return "자유게시판";
            case "정보": return "정보게시판";
            default: return apiBoardType;
        }
    }

    private void updateLikeButtonState() {
        if (btnLike != null) btnLike.setText("👍 좋아요");
    }

    private void updateDislikeButtonState() {
        if (btnDislike != null) btnDislike.setText("👎 싫어요");
    }

    private void likePost() {
        if (post == null || post.getId() == null) return;
        showLoading(true);
        try {
            int postIdInt = Integer.parseInt(post.getId());
            Map<String, String> request = new HashMap<>();
            request.put("email", userEmail);
            RetrofitClient.getApiService().likePost(postIdInt, request).enqueue(new Callback<ApiResponse>() {
                @Override public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(PostDetailActivity.this, "좋아요를 눌렀습니다.", Toast.LENGTH_SHORT).show();
                        loadPostDetails(postId);
                    } else { Toast.makeText(PostDetailActivity.this, "좋아요 실패", Toast.LENGTH_SHORT).show(); showLoading(false); }
                }
                @Override public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    showLoading(false); Toast.makeText(PostDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) { showLoading(false); Toast.makeText(this, "ID 오류", Toast.LENGTH_SHORT).show(); }
    }

    private void dislikePost() {
        if (post == null || post.getId() == null) return;
        showLoading(true);
        try {
            int postIdInt = Integer.parseInt(post.getId());
            Map<String, String> request = new HashMap<>();
            request.put("email", userEmail);
            RetrofitClient.getApiService().dislikePost(postIdInt, request).enqueue(new Callback<ApiResponse>() {
                @Override public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(PostDetailActivity.this, "싫어요를 눌렀습니다.", Toast.LENGTH_SHORT).show();
                        loadPostDetails(postId);
                    } else { Toast.makeText(PostDetailActivity.this, "싫어요 실패", Toast.LENGTH_SHORT).show(); showLoading(false); }
                }
                @Override public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    showLoading(false); Toast.makeText(PostDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) { showLoading(false); Toast.makeText(this, "ID 오류", Toast.LENGTH_SHORT).show(); }
    }

    private Comment parseCommentData(Map<String, Object> commentData) {
        if (commentData == null) return null;
        Comment comment = new Comment();
        try {
            if (commentData.get("id") != null) comment.setId(getIntFromMap(commentData, "id", 0));
            if (commentData.get("content") != null) comment.setContent(getStringFromMap(commentData, "content"));
            if (commentData.get("email") != null) comment.setAuthorEmail(getStringFromMap(commentData, "email"));
            if (commentData.get("name") != null) comment.setAuthorName(getStringFromMap(commentData, "name"));
            if (commentData.get("post_id") != null) comment.setPostId(getIntFromMap(commentData, "post_id", 0));
            if (commentData.get("created_at") != null) {
                String createdAtStr = getStringFromMap(commentData, "created_at");
                if (createdAtStr != null) {
                    SimpleDateFormat serverFormat = new SimpleDateFormat(createdAtStr.contains(".") ? "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" : "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
                    serverFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    comment.setCreatedAt(serverFormat.parse(createdAtStr));
                }
            }
            return comment;
        } catch (Exception e) { Log.e(TAG, "Error parsing comment data", e); return null; }
    }

    private void submitComment() {
        if (post == null || post.getId() == null) return;
        String commentContent = etComment.getText().toString().trim();
        if (commentContent.isEmpty()) { Toast.makeText(this, "댓글 내용을 입력해주세요.", Toast.LENGTH_SHORT).show(); return; }
        showLoading(true);
        try {
            int postIdInt = Integer.parseInt(post.getId());
            Map<String, String> request = new HashMap<>();
            request.put("email", userEmail);
            request.put("content", commentContent);
            request.put("name", userName);
            RetrofitClient.getApiService().createComment(postIdInt, request).enqueue(new Callback<ApiResponse>() {
                @Override public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(PostDetailActivity.this, "댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                        etComment.setText("");
                        Map<String, Object> newCommentData = response.body().getNewComment();
                        if (newCommentData != null) {
                            Comment newComment = parseCommentData(newCommentData);
                            if (newComment != null && commentAdapter != null) commentAdapter.addComment(newComment);
                        } else {
                            loadPostDetails(postId);
                        }
                    } else { Toast.makeText(PostDetailActivity.this, "댓글 등록 실패", Toast.LENGTH_SHORT).show(); }
                }
                @Override public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    showLoading(false); Toast.makeText(PostDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) { showLoading(false); Toast.makeText(this, "ID 오류", Toast.LENGTH_SHORT).show(); }
    }

    private void deleteComment(int commentId) {
        showLoading(true);
        Map<String, String> request = new HashMap<>();
        request.put("email", userEmail);
        RetrofitClient.getApiService().deleteCommentById(commentId, request).enqueue(new Callback<ApiResponse>() {
            @Override public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(PostDetailActivity.this, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    if (commentAdapter != null) commentAdapter.removeComment(commentId);
                } else { Toast.makeText(PostDetailActivity.this, "댓글 삭제 실패", Toast.LENGTH_SHORT).show(); }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                showLoading(false); Toast.makeText(PostDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadFile() {
        if (post == null || post.getFileName() == null || post.getFileName().isEmpty()) {
            Toast.makeText(this, "다운로드할 파일 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading(true);
        Toast.makeText(this, "파일 다운로드 중: " + post.getFileName(), Toast.LENGTH_SHORT).show();
        RetrofitClient.getApiService().downloadBoardFile(post.getFileName()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "File download successful. Saving file...");
                    saveFile(response.body(), post.getFileName());
                } else {
                    Toast.makeText(PostDetailActivity.this, "파일 다운로드 실패 (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "File download server error - Code: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(PostDetailActivity.this, "네트워크 오류 (파일 다운로드)", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "File download network failure", t);
            }
        });
    }

    private boolean saveFile(ResponseBody body, String fileName) {
        try {
            File downloadsDir = getExternalFilesDir(null); // 앱별 외부 저장소
            if (downloadsDir == null) {
                Toast.makeText(this, "외부 저장소를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            File file = new File(downloadsDir, fileName);

            try (InputStream inputStream = body.byteStream();
                 OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                Log.d(TAG, "File saved successfully: " + file.getAbsolutePath());

                Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                String mimeType = getContentResolver().getType(uri);
                intent.setDataAndType(uri, mimeType != null ? mimeType : "*/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "이 파일을 열 수 있는 앱이 설치되어 있지 않습니다.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "No app found to open file: " + e);
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, "파일을 열 수 없습니다 (FileProvider 구성 오류).", Toast.LENGTH_LONG).show();
                Log.e(TAG, "FileProvider error: " + e);
            } catch (IOException e) {
                Toast.makeText(this, "파일 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error writing/reading file", e);
            }
        } catch (Exception e) { // 그 외 모든 예외
            Toast.makeText(this, "파일 처리 중 알 수 없는 오류.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unexpected error in saveFile", e);
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu called.");
        if (post != null && post.getAuthorId() != null) {
            boolean isAuthor = userEmail != null && !userEmail.isEmpty() && userEmail.equals(post.getAuthorId());
            Log.d(TAG, "Menu check: isAdmin=" + isAdmin + ", isAuthor=" + isAuthor +
                    " (PostAuthor: " + post.getAuthorId() + ", CurrentUser: " + userEmail + ")");
            if (isAdmin || isAuthor) {
                getMenuInflater().inflate(R.menu.menu_post_detail, menu);
                Log.i(TAG, "Displaying post detail menu.");
                return true;
            } else {
                Log.i(TAG, "Not displaying menu (Not admin and not author).");
            }
        } else {
            Log.w(TAG, "Not displaying menu (Post object or Post AuthorId is null).");
            if (post == null) Log.w(TAG, "Post object is null in onCreateOptionsMenu.");
            else if (post.getAuthorId() == null) Log.w(TAG, "Post AuthorId is null in onCreateOptionsMenu.");
        }
        return super.onCreateOptionsMenu(menu); // 기본 메뉴 처리 (아무것도 표시 안 함)
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            setResult(RESULT_OK); // 변경사항이 있을 수 있다는 가정하에 목록 새로고침 유도
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
        if (post == null) {
            Toast.makeText(this, "수정할 게시글 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, PostEditActivity.class);
        intent.putExtra("post", post); // Post 객체가 Serializable 인터페이스를 구현해야 함
        startActivityForResult(intent, REQUEST_EDIT_POST);
    }

    private void confirmDelete() {
        if (post == null) {
            Toast.makeText(this, "삭제할 게시글 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("게시글 삭제")
                .setMessage("정말로 이 게시글을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deletePost())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deletePost() {
        if (post == null || post.getId() == null) {
            Toast.makeText(this, "삭제할 게시글 ID가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading(true);
        try {
            final int postIdInt = Integer.parseInt(post.getId()); // final은 콜백 내부에서 직접 사용 안 하므로 제거해도 무방

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", userEmail); // 삭제를 요청하는 사용자의 이메일 (서버에서 권한 확인에 사용)

            // !!!!! 여기가 수정된 부분: 주석 해제 !!!!!
            requestBody.put("post_id", postIdInt); // 서버 API 명세에 따라 post_id를 본문에 포함

            Log.d(TAG, "Attempting to delete post ID: " + postIdInt + " by user: " + userEmail +
                    (isAdmin ? " (Admin)" : "") + ". Request Body: " + requestBody.toString());

            Call<ApiResponse> call = RetrofitClient.getApiService().deletePost(postIdInt, requestBody);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(PostDetailActivity.this, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK); // BoardActivity에 변경사항 알림
                        finish();
                    } else {
                        String errorMsg = "삭제 실패";
                        if (response.body() != null && response.body().getMessage() != null) {
                            errorMsg += ": " + response.body().getMessage();
                        } else if (response.errorBody() != null) {
                            try {
                                String errorBodyString = response.errorBody().string(); // 오류 본문을 한 번만 읽어야 함
                                Log.e(TAG, "Delete post error body: " + errorBodyString);
                                // 서버가 보낸 구체적인 오류 메시지를 포함 (예: 필드 누락)
                                errorMsg += " (서버: " + response.code() + " - " + errorBodyString + ")";
                            } catch (IOException e) {
                                errorMsg += " (서버 오류: " + response.code() + " - 오류 본문 읽기 실패)";
                            }
                        } else {
                            errorMsg += " (서버 응답 오류: " + response.code() + ")";
                        }
                        Toast.makeText(PostDetailActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed to delete post: " + errorMsg + " | Original Response Code: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    showLoading(false);
                    Toast.makeText(PostDetailActivity.this, "네트워크 오류로 삭제에 실패했습니다: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Delete post API call failed.", t);
                }
            });
        } catch (NumberFormatException e) {
            showLoading(false);
            Toast.makeText(this, "잘못된 게시글 ID 형식입니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "deletePost - Invalid ID format: " + post.getId(), e);
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
            Log.d(TAG, "Post edit successful (resultCode OK), reloading details.");
            if (postId != null) {
                loadPostDetails(postId); // 수정 후 상세 정보 다시 로드
            }
            setResult(RESULT_OK); // BoardActivity에도 변경사항 알림
            Toast.makeText(this, "게시글이 수정되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // `isFirstLoad` 플래그를 사용하여 onCreate에서 이미 로드된 경우 중복 로드 방지
        if (postId != null) {
            if (isFirstLoad) {
                isFirstLoad = false; // onCreate에서 한 번 로드했으므로 플래그 변경
            } else {
                // 다른 화면에서 돌아왔을 때 (예: 수정 후) 데이터 새로고침
                Log.d(TAG, "onResume: Not first load, refreshing post details for postId: " + postId);
                loadPostDetails(postId);
            }
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK); // BoardActivity가 onActivityResult에서 받을 수 있도록
        super.onBackPressed();
    }

    // Helper method from previous context, ensure consistency
    private String getStringFromMap(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            return String.valueOf(map.get(key));
        }
        return null;
    }

    private int getIntFromMap(Map<String, Object> map, String key, int defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            Object value = map.get(key);
            try {
                if (value instanceof Number) return ((Number) value).intValue();
                if (value instanceof String && !((String) value).isEmpty())
                    return Double.valueOf((String) value).intValue();
            } catch (NumberFormatException e) { Log.w(TAG, "getIntFromMap NFE for " + key + ": " + value); }
        }
        return defaultValue;
    }
}
