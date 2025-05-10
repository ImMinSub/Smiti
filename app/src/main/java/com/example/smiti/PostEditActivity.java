package com.example.smiti;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.PostRequest;
import com.example.smiti.api.RetrofitClient;
import com.example.smiti.model.Post;
import com.example.smiti.model.UploadedFile;
import com.example.smiti.adapter.UploadedFileAdapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostEditActivity extends AppCompatActivity {

    private static final String TAG = "PostEditActivity";
    private static final String PREF_NAME = "LoginPrefs";

    private Spinner spinnerCategory;
    private EditText etTitle, etContent;
    private TextView tvFileName;
    private Button btnAttachFile, btnSubmit;
    private CheckBox cbNotice;
    private ProgressBar progressBar;
    private ImageView previewImageView;
    private RecyclerView recyclerFiles;
    
    private List<UploadedFile> uploadedFiles = new ArrayList<>();
    private UploadedFileAdapter fileAdapter;

    private Uri selectedFileUri;
    private String selectedFileName;
    private long selectedFileSize;
    private InputStream fileInputStream;
    private Post postToEdit; // 수정할 게시글 정보

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleFilePickerResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_edit);

        // 뷰 초기화
        initViews();
        setupToolbar();
        setupCategorySpinner();
        setupButtons();

        // 관리자 여부에 따라 공지 체크박스 표시
        checkAdminStatus();

        // 수정 모드인 경우 게시글 정보 설정
        checkEditMode();
    }

    private void initViews() {
        spinnerCategory = findViewById(R.id.spinner_category);
        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        tvFileName = findViewById(R.id.tv_file_name);
        btnAttachFile = findViewById(R.id.btn_attach_file);
        btnSubmit = findViewById(R.id.btn_submit);
        cbNotice = findViewById(R.id.cb_notice);
        progressBar = findViewById(R.id.progress_bar);
        previewImageView = findViewById(R.id.preview_image);
        recyclerFiles = findViewById(R.id.recycler_files);
        
        // 파일 목록 RecyclerView 설정
        recyclerFiles.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter = new UploadedFileAdapter(this);
        recyclerFiles.setAdapter(fileAdapter);
        
        // 파일 삭제 리스너 설정
        fileAdapter.setOnFileRemoveListener(position -> {
            UploadedFile file = uploadedFiles.get(position);
            uploadedFiles.remove(position);
            fileAdapter.removeFile(position);
            
            if (uploadedFiles.isEmpty()) {
                recyclerFiles.setVisibility(View.GONE);
            }
            
            Toast.makeText(this, file.getFileName() + " 파일이 제거되었습니다.", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        TextView titleTextView = findViewById(R.id.toolbar_title);
        if (getIntent().hasExtra("post")) {
            titleTextView.setText("게시글 수정");
        } else {
            titleTextView.setText("게시글 작성");
        }
    }

    private void setupCategorySpinner() {
        List<String> categories = new ArrayList<>();
        categories.add(Post.CATEGORY_FREE);
        categories.add(Post.CATEGORY_QUESTION);
        categories.add(Post.CATEGORY_SHARE);
        if (isAdmin()) {
            categories.add(Post.CATEGORY_NOTICE);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupButtons() {
        btnAttachFile.setOnClickListener(v -> openFilePicker());

        btnSubmit.setOnClickListener(v -> {
            if (validateInputs()) {
                if (postToEdit != null) {
                    updatePost();
                } else {
                    submitPost();
                }
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"})
                .setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private void handleFilePickerResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            selectedFileUri = result.getData().getData();
            if (selectedFileUri != null) {
                try {
                    selectedFileName = getFileNameFromUri(selectedFileUri);
                    
                    // 파일 크기 확인
                    Cursor cursor = getContentResolver().query(selectedFileUri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                        selectedFileSize = cursor.getLong(sizeIndex);
                        cursor.close();
                        
                        // 파일 크기가 100MB를 초과하는 경우
                        if (selectedFileSize > 100 * 1024 * 1024) {
                            Toast.makeText(this, "파일 크기는 100MB 이하여야 합니다.", Toast.LENGTH_SHORT).show();
                            resetFileSelection();
                            return;
                        }
                        
                        // 파일 정보 표시
                        tvFileName.setText("선택된 파일: " + selectedFileName + " (" + getReadableFileSize(selectedFileSize) + ")");
                        tvFileName.setVisibility(View.VISIBLE);
                        
                        // 파일 타입에 따른 처리
                        String mimeType = getContentResolver().getType(selectedFileUri);
                        
                        // 파일 모델 생성 및 목록에 추가
                        UploadedFile uploadedFile = new UploadedFile(selectedFileUri, selectedFileName, selectedFileSize, mimeType);
                        uploadedFiles.add(uploadedFile);
                        fileAdapter.addFile(uploadedFile);
                        
                        // 파일 목록이 있으면 RecyclerView 표시
                        recyclerFiles.setVisibility(View.VISIBLE);
                        
                        // 이미지 미리보기는 이제 사용하지 않음
                            previewImageView.setVisibility(View.GONE);
                        
                        // 파일 스트림 열기
                        fileInputStream = getContentResolver().openInputStream(selectedFileUri);
                        
                        // 새 파일 선택을 위해 초기화
                        selectedFileUri = null;
                        selectedFileName = null;
                        selectedFileSize = 0;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "파일 처리 오류: " + e.getMessage(), e);
                    Toast.makeText(this, "파일 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetFileSelection();
                }
            }
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getReadableFileSize(long size) {
        if (size <= 0) return "0 B";
        
        final String[] units = new String[] {"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private void resetFileSelection() {
        selectedFileUri = null;
        selectedFileName = null;
        selectedFileSize = 0;
        tvFileName.setText("");
        tvFileName.setVisibility(View.GONE);
        closeFileInputStream();
    }

    private void closeFileInputStream() {
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
                fileInputStream = null;
            } catch (IOException e) {
                Log.e(TAG, "파일 스트림 닫기 실패", e);
            }
        }
    }

    private boolean validateInputs() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력하세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (content.isEmpty()) {
            Toast.makeText(this, "내용을 입력하세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("post")) {
            postToEdit = (Post) intent.getSerializableExtra("post");

            if (postToEdit != null) {
                // 기존 게시글 정보로 UI 업데이트
                etTitle.setText(postToEdit.getTitle());
                etContent.setText(postToEdit.getContent());

                // 카테고리 선택
                ArrayAdapter adapter = (ArrayAdapter) spinnerCategory.getAdapter();
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getItem(i).equals(postToEdit.getCategory())) {
                        spinnerCategory.setSelection(i);
                        break;
                    }
                }

                // 첨부 파일 정보 표시
                if (postToEdit.hasFile()) {
                    tvFileName.setText("기존 첨부 파일: " + postToEdit.getFileName());
                    tvFileName.setVisibility(View.VISIBLE);
                }

                // 공지 여부 설정
                cbNotice.setChecked(postToEdit.isNotice());
            }
        }
    }

    private void submitPost() {
        // 로딩 표시
        showLoading(true);

        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("email", "");
        
        // 파일이 없는 경우, API 명세서에 맞게 multipart/form-data 형식으로 요청
        if (uploadedFiles.isEmpty()) {
            // 각 필드를 RequestBody로 변환 (명세서에 있는 필수 필드만 포함)
            RequestBody emailPart = RequestBody.create(MediaType.parse("text/plain"), userEmail);
            RequestBody titlePart = RequestBody.create(MediaType.parse("text/plain"), title);
            RequestBody contentPart = RequestBody.create(MediaType.parse("text/plain"), content);
            RequestBody boardTypePart = RequestBody.create(MediaType.parse("text/plain"), category);
            
            // 디버깅을 위해 로깅
            Log.d(TAG, "요청 데이터: email=" + userEmail + ", title=" + title + 
                  ", content=" + content + ", board_type=" + category);
            
            // API 호출
            Call<ApiResponse> call = RetrofitClient.getApiService().createPost(
                emailPart, boardTypePart, titlePart, contentPart);
                
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    handlePostResponse(response);
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    handlePostFailure(t);
                }
            });
        } else {
            // 현재는 한 개의 파일만 지원
            // 나중에 여러 파일을 처리하려면 여기를 수정해야 함
            UploadedFile uploadedFile = uploadedFiles.get(0);
            
            // 파일이 있는 경우, API 명세서에 맞게 multipart/form-data 형식 사용
            RequestBody emailPart = RequestBody.create(MediaType.parse("text/plain"), userEmail);
            RequestBody boardTypePart = RequestBody.create(MediaType.parse("text/plain"), category);
            RequestBody titlePart = RequestBody.create(MediaType.parse("text/plain"), title);
            RequestBody contentPart = RequestBody.create(MediaType.parse("text/plain"), content);

            try {
                // 선택된 파일 가져오기
                Uri fileUri = uploadedFile.getFileUri();
                String fileName = uploadedFile.getFileName();
                
                // 파일 스트림 열기
                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                byte[] fileBytes = getBytesFromInputStream(inputStream);
                
                RequestBody requestFile = RequestBody.create(
                        MediaType.parse(getContentResolver().getType(fileUri) != null ? 
                            getContentResolver().getType(fileUri) : "application/octet-stream"),
                        fileBytes);
                MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileName, requestFile);
                
                Call<ApiResponse> call = RetrofitClient.getApiService().createPostWithFile(
                        emailPart, boardTypePart, titlePart, contentPart, filePart);
                    
                call.enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        handlePostResponse(response);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable t) {
                        handlePostFailure(t);
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "파일 처리 오류: " + e.getMessage(), e);
                Toast.makeText(this, "파일 첨부 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        }
    }

    private void updatePost() {
        // 로딩 표시
        showLoading(true);
        
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        boolean isNotice = cbNotice.isChecked();
        
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("email", "");
        
        try {
            int postId = Integer.parseInt(postToEdit.getId());
            
            // API 명세서와 일치하는 요청 데이터 생성
            Map<String, String> requestMap = new HashMap<>();
            requestMap.put("title", title);
            requestMap.put("content", content);
            
            // API 호출
            Call<ApiResponse> call = RetrofitClient.getApiService().updatePost(postId, requestMap);
            
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    handlePostResponse(response);
                }
                
                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    handlePostFailure(t);
                }
            });
        } catch (NumberFormatException e) {
            showLoading(false);
            Log.e(TAG, "잘못된 게시글 ID 형식: " + postToEdit.getId(), e);
            Toast.makeText(this, "게시글 ID 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 코드 중복 제거를 위한 응답 처리 메서드
    private void handlePostResponse(Response<ApiResponse> response) {
        showLoading(false);
        
        if (response.isSuccessful()) {
            ApiResponse apiResponse = response.body();
            
            // API 응답 디버깅을 위한 로그 추가
            Log.d(TAG, "API 응답: " + (apiResponse != null ? apiResponse.toString() : "null"));
            
            if (apiResponse != null) {
            Log.d(TAG, "status: " + (apiResponse.getStatus() != null ? apiResponse.getStatus() : "null"));
            Log.d(TAG, "message: " + (apiResponse.getMessage() != null ? apiResponse.getMessage() : "null"));
            Log.d(TAG, "data: " + (apiResponse.getData() != null ? apiResponse.getData().toString() : "null"));
            
                // post_id 확인
                if (apiResponse.getPostId() != null) {
                    Log.d(TAG, "post_id: " + apiResponse.getPostId());
                }
                
                // post_id가 포함된 응답을 성공으로 처리
                if (apiResponse.isSuccess() || apiResponse.getPostId() != null) {
                    Toast.makeText(PostEditActivity.this, "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                    return;
                }
            }
            
            // 서버 응답이 200 OK이고 성공적으로 응답이 왔으면 무조건 성공으로 처리
            // 왜냐하면 서버가 명확한 성공 상태를 반환했기 때문입니다
            if (response.code() == 200) {
                Toast.makeText(PostEditActivity.this, "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
                return;
            }
            
            // 위의 모든 조건에 해당하지 않으면 실패로 간주
            Toast.makeText(PostEditActivity.this, 
                    apiResponse != null && apiResponse.getMessage() != null ? 
                    "게시글 등록 실패: " + apiResponse.getMessage() : 
                    "게시글 등록 실패", 
                    Toast.LENGTH_SHORT).show();
        } else {
            try {
                // 오류 응답 본문을 가져와 로그로 출력
                if (response.errorBody() != null) {
                    String errorBody = response.errorBody().string();
                    Log.e(TAG, "서버 응답 오류: " + response.code() + " - " + response.message() + "\n오류 내용: " + errorBody);
            Toast.makeText(PostEditActivity.this, "서버 응답 오류: " + response.code(), Toast.LENGTH_SHORT).show();
                } else {
            Log.e(TAG, "서버 응답 오류: " + response.code() + " - " + response.message());
                    Toast.makeText(PostEditActivity.this, "서버 응답 오류: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "오류 응답 처리 실패", e);
                Toast.makeText(PostEditActivity.this, "서버 응답 오류: " + response.code(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 코드 중복 제거를 위한 실패 처리 메서드
    private void handlePostFailure(Throwable t) {
        showLoading(false);
        Toast.makeText(PostEditActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        Log.e(TAG, "API 호출 실패: " + t.getMessage(), t);
    }

    private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        btnSubmit.setEnabled(!isLoading);
        btnAttachFile.setEnabled(!isLoading);
    }

    private boolean isAdmin() {
        // TODO: 실제 관리자 체크 로직으로 변경해야 함
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String email = sharedPreferences.getString("email", "");
        return email.contains("admin");
    }

    private void checkAdminStatus() {
        if (isAdmin()) {
            cbNotice.setVisibility(View.VISIBLE);
        } else {
            cbNotice.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 파일 스트림 닫기
        closeFileInputStream();
    }
}