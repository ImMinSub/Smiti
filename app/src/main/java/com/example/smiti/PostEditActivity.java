package com.example.smiti;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.RetrofitClient;
import com.example.smiti.model.Post;
import com.example.smiti.model.UploadedFile;
import com.google.gson.Gson; // 디버깅용

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private static final String KEY_USER_EMAIL = "email";
    private static final String KEY_USER_ADMIN_STATUS = "admin";

    private Spinner spinnerPostCategory;
    private EditText etPostTitle;
    private EditText etPostContent;
    private TextView tvFileNameDisplay;
    private Button btnAttachFile;
    private Button btnSubmit;
    private ProgressBar progressBar;

    private UploadedFile selectedUploadedFile;

    private Post postToEdit;
    private boolean isAdmin = false;
    private String currentUserEmail;

    // 화면 표시용 카테고리 이름 (BoardActivity와 일치시킴)
    private final String CATEGORY_DISPLAY_NOTICE = "공지사항";
    private final String CATEGORY_DISPLAY_FREE = "자유게시판";
    private final String CATEGORY_DISPLAY_INFO = "정보게시판";

    // API에서 사용하는 실제 카테고리 값 (서버와 일치해야 함)
    private final String BOARD_TYPE_API_NOTICE = "공지";
    private final String BOARD_TYPE_API_FREE = "자유";
    private final String BOARD_TYPE_API_INFO = "정보"; // "정보게시판"에 매핑되는 API 값

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleFilePickerResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_edit); // activity_post_edit.xml 레이아웃 사용 가정
        Log.d(TAG, "onCreate called");

        initViews();
        setupToolbar();
        loadAdminStatusAndEmail();
        setupCategorySpinner();
        setupButtons();
        checkEditMode();
    }

    private void initViews() {
        Log.d(TAG, "initViews called");
        spinnerPostCategory = findViewById(R.id.spinner_category);
        etPostTitle = findViewById(R.id.et_title);
        etPostContent = findViewById(R.id.et_content);
        tvFileNameDisplay = findViewById(R.id.tv_file_name);
        btnAttachFile = findViewById(R.id.btn_attach_file);
        btnSubmit = findViewById(R.id.btn_submit);
        progressBar = findViewById(R.id.progress_bar);

        // Null 체크 (레이아웃에 해당 ID가 없는 경우를 대비)
        if (spinnerPostCategory == null) Log.e(TAG, "Spinner (spinner_category_edit) not found!");
        if (etPostTitle == null) Log.e(TAG, "EditText (et_title_edit) not found!");
        if (etPostContent == null) Log.e(TAG, "EditText (et_content_edit) not found!");
        // ... 다른 뷰들도 필요시 Null 체크
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar); // XML의 Toolbar ID (예시)
        if (toolbar == null) {
            Log.e(TAG, "Toolbar (toolbar_edit) not found!");
            return;
        }
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            // 제목 설정은 checkEditMode에서 수행
        }
    }

    private void loadAdminStatusAndEmail() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isAdmin = (sharedPreferences.getInt(KEY_USER_ADMIN_STATUS, 0) == 1);
        currentUserEmail = sharedPreferences.getString(KEY_USER_EMAIL, "");
        Log.i(TAG, "loadAdminStatusAndEmail - isAdmin: " + isAdmin + ", currentUserEmail: " + currentUserEmail);
    }

    private void setupCategorySpinner() {
        Log.d(TAG, "setupCategorySpinner called. isAdmin: " + isAdmin);
        if (spinnerPostCategory == null) return; // Spinner가 없으면 진행 불가

        List<String> categoriesForSpinner = new ArrayList<>();

        if (isAdmin) {
            categoriesForSpinner.add(CATEGORY_DISPLAY_NOTICE); // 공지사항
        }
        categoriesForSpinner.add(CATEGORY_DISPLAY_FREE);   // 자유게시판
        categoriesForSpinner.add(CATEGORY_DISPLAY_INFO);   // 정보게시판

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categoriesForSpinner);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPostCategory.setAdapter(categoryAdapter);
        Log.d(TAG, "Spinner categories set: " + categoriesForSpinner);

        // 기본 선택값 설정 (옵션 - 첫 번째 항목 또는 특정 값)
        if (!categoriesForSpinner.isEmpty()) {
            spinnerPostCategory.setSelection(0);
        }
    }

    private void setupButtons() {
        if (btnAttachFile != null) {
            btnAttachFile.setOnClickListener(v -> openFilePicker());
        }
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                Log.d(TAG, "Submit button clicked.");
                if (validateInputs()) {
                    if (postToEdit != null) {
                        updatePost(); // 수정 모드
                    } else {
                        submitPost(); // 새 글 작성 모드
                    }
                }
            });
        } else {
            Log.e(TAG, "Submit button is null. Check R.id in layout.");
        }
    }

    private boolean validateInputs() {
        Log.d(TAG, "validateInputs called");
        String title = etPostTitle != null && etPostTitle.getText() != null ? etPostTitle.getText().toString().trim() : "";
        String content = etPostContent != null && etPostContent.getText() != null ? etPostContent.getText().toString().trim() : "";
        Object selectedItem = spinnerPostCategory != null ? spinnerPostCategory.getSelectedItem() : null;

        if (selectedItem == null || selectedItem.toString().isEmpty()) {
            Toast.makeText(this, "카테고리를 선택해주세요.", Toast.LENGTH_SHORT).show();
            if (spinnerPostCategory != null) spinnerPostCategory.performClick();
            Log.w(TAG, "Validation Failed: Category not selected.");
            return false;
        }
        if (title.isEmpty()) {
            if (etPostTitle != null) etPostTitle.setError("제목을 입력해주세요.");
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation Failed: Title is empty.");
            return false;
        }
        if (content.isEmpty()) {
            if (etPostContent != null) etPostContent.setError("내용을 입력해주세요.");
            Toast.makeText(this, "내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation Failed: Content is empty.");
            return false;
        }
        Log.d(TAG, "Validation Succeeded.");
        return true;
    }

    private void openFilePicker() {
        Log.d(TAG, "openFilePicker called");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // 모든 타입의 파일 선택 가능
        // 필요하다면 특정 MIME 타입만 허용하도록 수정 가능
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "image/*", "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
                "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
                "text/plain"
        });
        try {
            filePickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "파일 선택기를 열 수 없습니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Cannot open file picker", e);
        }
    }

    private void handleFilePickerResult(androidx.activity.result.ActivityResult resultData) {
        Log.d(TAG, "handleFilePickerResult: ResultCode = " + resultData.getResultCode());
        selectedUploadedFile = null; // 이전 선택 파일 정보 초기화
        if (tvFileNameDisplay != null) tvFileNameDisplay.setVisibility(View.GONE);

        if (resultData.getResultCode() == RESULT_OK && resultData.getData() != null) {
            Uri fileUri = resultData.getData().getData();
            if (fileUri != null) {
                Log.d(TAG, "File URI received: " + fileUri.toString());
                try {
                    String fileName = getFileNameFromUri(fileUri);
                    long fileSize = getFileSizeFromUri(fileUri);

                    if (fileSize > 100 * 1024 * 1024) { // 100MB 제한 예시
                        Toast.makeText(this, "파일 크기는 100MB 이하여야 합니다.", Toast.LENGTH_LONG).show();
                        Log.w(TAG, "File size exceeds limit: " + fileSize);
                        return;
                    }

                    String mimeType = getContentResolver().getType(fileUri);
                    if (mimeType == null) {
                        mimeType = "application/octet-stream"; // 기본 MIME 타입
                    }
                    selectedUploadedFile = new UploadedFile(fileUri, fileName, fileSize, mimeType);

                    if (tvFileNameDisplay != null) {
                        tvFileNameDisplay.setText("선택: " + fileName + " (" + getReadableFileSize(fileSize) + ")");
                        tvFileNameDisplay.setVisibility(View.VISIBLE);
                    }
                    Log.i(TAG, "File selected successfully: " + fileName + ", Size: " + fileSize + ", Type: " + mimeType);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing selected file: " + e.getMessage(), e);
                    Toast.makeText(this, "파일 정보 로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    selectedUploadedFile = null;
                    if (tvFileNameDisplay != null) tvFileNameDisplay.setVisibility(View.GONE);
                }
            } else {
                Log.w(TAG, "File URI is null after selection.");
            }
        } else {
            Log.w(TAG, "File selection cancelled or failed. ResultCode: " + resultData.getResultCode());
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri == null) return "unknown_file";
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) result = cursor.getString(nameIndex);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name from content URI: " + uri, e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "unknown_file";
    }

    private long getFileSizeFromUri(Uri uri) {
        if (uri == null) return 0;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        return cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file size from content URI: " + uri, e);
            }
        }
        return 0;
    }

    private String getReadableFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private void checkEditMode() {
        Log.d(TAG, "checkEditMode called");
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("post")) {
            postToEdit = (Post) intent.getSerializableExtra("post");
            if (postToEdit != null) {
                Log.i(TAG, "Editing post ID: " + postToEdit.getId() + ", Title: " + postToEdit.getTitle());
                if (etPostTitle != null) etPostTitle.setText(postToEdit.getTitle());
                if (etPostContent != null) etPostContent.setText(postToEdit.getContent());

                // 서버에서 받은 API용 board_type (예: "공지", "자유", "정보")
                String apiBoardTypeFromServer = postToEdit.getCategory(); // Post 모델의 getCategory()가 API용 값을 반환한다고 가정
                // Spinner에 표시할 화면용 카테고리 이름 (예: "공지사항", "자유게시판", "정보게시판")
                String displayCategory = mapApiBoardTypeToDisplay(apiBoardTypeFromServer);
                Log.d(TAG, "Edit mode - Category from server (API value): " + apiBoardTypeFromServer + ", Mapped to display: " + displayCategory);

                if (spinnerPostCategory != null) {
                    ArrayAdapter<String> categoryAdapter = (ArrayAdapter<String>) spinnerPostCategory.getAdapter();
                    if (categoryAdapter != null) {
                        int position = categoryAdapter.getPosition(displayCategory);

                        // 관리자가 아닌데 공지사항을 수정하려고 하는 경우 처리
                        if (displayCategory.equals(CATEGORY_DISPLAY_NOTICE) && !isAdmin) {
                            Log.w(TAG, "Non-admin attempting to edit a NOTICE post. Defaulting category.");
                            spinnerPostCategory.setSelection(getNonNoticeDefaultSpinnerPosition(categoryAdapter));
                            Toast.makeText(this, "공지사항은 관리자만 수정 가능합니다. 카테고리가 일반으로 변경됩니다.", Toast.LENGTH_LONG).show();
                        } else if (position >= 0) {
                            spinnerPostCategory.setSelection(position);
                            Log.d(TAG, "Edit mode: Spinner set to '" + displayCategory + "' at position " + position);
                        } else {
                            Log.w(TAG, "Edit mode: Category '" + displayCategory + "' NOT FOUND in spinner (API value was '" + apiBoardTypeFromServer + "'). Defaulting.");
                            if (categoryAdapter.getCount() > 0) spinnerPostCategory.setSelection(0);
                        }
                    }
                }

                if (postToEdit.getFileName() != null && !postToEdit.getFileName().isEmpty()) {
                    if (tvFileNameDisplay != null) {
                        tvFileNameDisplay.setText("기존 파일: " + postToEdit.getFileName() + (postToEdit.getFileSize() > 0 ? " (" + getReadableFileSize(postToEdit.getFileSize()) + ")" : ""));
                        tvFileNameDisplay.setVisibility(View.VISIBLE);
                    }
                }

                if (btnSubmit != null) btnSubmit.setText("수정하기");
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("게시글 수정");

            } else {
                Log.e(TAG, "postToEdit is null even though 'post' extra exists.");
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("게시글 작성"); // 오류 시 기본 제목
            }
        } else {
            Log.d(TAG, "Not in edit mode. Setting title to '게시글 작성'");
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("게시글 작성");
        }
    }

    private int getNonNoticeDefaultSpinnerPosition(ArrayAdapter<String> adapter) {
        if (adapter == null) return 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            String item = adapter.getItem(i);
            if (item != null && !item.equals(CATEGORY_DISPLAY_NOTICE)) {
                return i; // 공지사항이 아닌 첫 번째 카테고리 반환 (예: 자유게시판)
            }
        }
        return adapter.getCount() > 0 ? 0 : -1; // 모든 항목이 공지사항이거나 비어있을 경우
    }

    // API용 board_type 값을 화면 표시용 카테고리 이름으로 변환
    private String mapApiBoardTypeToDisplay(String apiBoardType) {
        if (apiBoardType == null) return CATEGORY_DISPLAY_FREE; // 기본값: 자유게시판
        if (apiBoardType.equals(BOARD_TYPE_API_NOTICE)) return CATEGORY_DISPLAY_NOTICE;
        if (apiBoardType.equals(BOARD_TYPE_API_FREE)) return CATEGORY_DISPLAY_FREE;
        if (apiBoardType.equals(BOARD_TYPE_API_INFO)) return CATEGORY_DISPLAY_INFO; // API "정보" -> 화면 "정보게시판"
        // 질문게시판은 현재 사용 안함
        Log.w(TAG, "mapApiBoardTypeToDisplay: Unknown API board_type '" + apiBoardType + "'. Defaulting to Free.");
        return CATEGORY_DISPLAY_FREE; // 매칭되는 것이 없으면 자유게시판으로
    }

    // 화면 표시용 카테고리 이름을 API용 board_type 값으로 변환
    private String mapDisplayCategoryToApiBoardType(String displayCategory) {
        if (displayCategory == null) return BOARD_TYPE_API_FREE; // 기본값
        if (displayCategory.equals(CATEGORY_DISPLAY_NOTICE)) return BOARD_TYPE_API_NOTICE;
        if (displayCategory.equals(CATEGORY_DISPLAY_FREE)) return BOARD_TYPE_API_FREE;
        if (displayCategory.equals(CATEGORY_DISPLAY_INFO)) return BOARD_TYPE_API_INFO; // 화면 "정보게시판" -> API "정보"
        // 질문게시판은 현재 사용 안함
        Log.w(TAG, "mapDisplayCategoryToApiBoardType: Unknown display category '" + displayCategory + "'. Defaulting to API Free.");
        return BOARD_TYPE_API_FREE; // 매칭되는 것이 없으면 API 자유로
    }

    private void submitPost() {
        showLoading(true);

        String title = etPostTitle.getText().toString().trim();
        String content = etPostContent.getText().toString().trim();
        String selectedCategoryDisplay = spinnerPostCategory.getSelectedItem() != null ? spinnerPostCategory.getSelectedItem().toString() : "";
        String boardTypeForApi = mapDisplayCategoryToApiBoardType(selectedCategoryDisplay);

        if (boardTypeForApi.equals(BOARD_TYPE_API_NOTICE) && !isAdmin) {
            Toast.makeText(this, "공지사항은 관리자만 작성할 수 있습니다.", Toast.LENGTH_LONG).show();
            showLoading(false);
            return;
        }

        Log.i(TAG, "Submitting new post - Email: " + currentUserEmail + ", BoardType: " + boardTypeForApi + ", Title: " + title);

        RequestBody emailPart = RequestBody.create(MediaType.parse("text/plain"), currentUserEmail != null ? currentUserEmail : "");
        RequestBody boardTypePart = RequestBody.create(MediaType.parse("text/plain"), boardTypeForApi);
        RequestBody titlePart = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody contentPart = RequestBody.create(MediaType.parse("text/plain"), content);
        MultipartBody.Part filePart = null;

        if (selectedUploadedFile != null && selectedUploadedFile.getFileUri() != null) {
            try (InputStream inputStream = getContentResolver().openInputStream(selectedUploadedFile.getFileUri())) {
                if (inputStream == null) throw new IOException("Failed to open input stream for file URI");
                byte[] fileBytes = getBytesFromInputStream(inputStream);
                String mimeType = selectedUploadedFile.getMimeType() != null ? selectedUploadedFile.getMimeType() : "application/octet-stream";
                RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), fileBytes);
                filePart = MultipartBody.Part.createFormData("file", selectedUploadedFile.getFileName(), requestFile);
                Log.d(TAG, "File part created for: " + selectedUploadedFile.getFileName() + " with MIME type: " + mimeType);
            } catch (IOException e) {
                Log.e(TAG, "Error creating file part: " + e.getMessage(), e);
                Toast.makeText(this, "파일 첨부 중 오류: " + e.getMessage(), Toast.LENGTH_LONG).show();
                showLoading(false);
                return;
            }
        }

        Call<ApiResponse> call;
        if (filePart != null) {
            Log.d(TAG, "Calling createPostWithFile API");
            call = RetrofitClient.getApiService().createPostWithFile(emailPart, boardTypePart, titlePart, contentPart, filePart);
        } else {
            Log.d(TAG, "Calling createPost API (no file)");
            call = RetrofitClient.getApiService().createPost(emailPart, boardTypePart, titlePart, contentPart);
        }

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                handlePostResponse(response, "게시글 등록");
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                handlePostFailure(t, "게시글 등록");
            }
        });
    }

    private void updatePost() {
        if (postToEdit == null || postToEdit.getId() == null) {
            Toast.makeText(this, "수정할 게시글 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading(true);

        String title = etPostTitle.getText().toString().trim();
        String content = etPostContent.getText().toString().trim();
        String selectedCategoryDisplay = spinnerPostCategory.getSelectedItem() != null ? spinnerPostCategory.getSelectedItem().toString() : CATEGORY_DISPLAY_FREE;
        String boardTypeForApi = mapDisplayCategoryToApiBoardType(selectedCategoryDisplay);

        if (boardTypeForApi.equals(BOARD_TYPE_API_NOTICE) && !isAdmin) {
            Toast.makeText(this, "공지사항은 관리자만 수정할 수 있습니다. 카테고리가 자유게시판으로 변경됩니다.", Toast.LENGTH_LONG).show();
            boardTypeForApi = BOARD_TYPE_API_FREE; // 관리자가 아니면 강제로 자유게시판으로
        }

        int postIdToUpdate;
        try {
            postIdToUpdate = Integer.parseInt(postToEdit.getId());
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid post ID for update: " + postToEdit.getId(), e);
            Toast.makeText(this, "잘못된 게시글 ID 형식입니다.", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        Log.i(TAG, "Updating post (text fields only) - ID: " + postIdToUpdate + ", Email: " + currentUserEmail + ", BoardType: " + boardTypeForApi + ", Title: " + title);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("post_id", postIdToUpdate);
        requestBodyMap.put("email", currentUserEmail != null ? currentUserEmail : "");
        requestBodyMap.put("title", title);
        requestBodyMap.put("content", content);
        requestBodyMap.put("board_type", boardTypeForApi);

        // 파일 수정 기능은 현재 코드에서 지원하지 않는 것으로 보임.
        // 만약 파일도 수정해야 한다면, createPostWithFile과 유사한 updatePostWithFile API 호출 로직 필요.
        // 여기서는 텍스트 정보만 업데이트하는 API를 호출한다고 가정.

        Call<ApiResponse> call = RetrofitClient.getApiService().updatePost(postIdToUpdate, requestBodyMap);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                handlePostResponse(response, "게시글 수정");
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                handlePostFailure(t, "게시글 수정");
            }
        });
    }

    private void handlePostResponse(Response<ApiResponse> response, String operationType) {
        showLoading(false);
        if (response.isSuccessful() && response.body() != null) {
            ApiResponse apiResponse = response.body();
            Log.d(TAG, operationType + " API Response: " + new Gson().toJson(apiResponse));

            boolean isOperationSuccessful = (response.code() >= 200 && response.code() < 300) &&
                    (apiResponse.isSuccess() ||
                            (apiResponse.getMessage() != null && (apiResponse.getMessage().toLowerCase().contains("success") || apiResponse.getMessage().toLowerCase().contains("성공"))) ||
                            apiResponse.getPostId() != null ||
                            (operationType.contains("수정") && response.code() == 200));

            if (isOperationSuccessful) {
                Toast.makeText(PostEditActivity.this, operationType + " 완료", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK); // BoardActivity에 변경 알림
                finish();
            } else {
                String errorMsg = apiResponse.getMessage() != null ? apiResponse.getMessage() : operationType + " 실패 (서버 메시지 없음)";
                Toast.makeText(PostEditActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                Log.e(TAG, operationType + " failed. Server message: " + errorMsg + ", Response code: " + response.code() + ", Full Body: " + new Gson().toJson(apiResponse));
            }
        } else {
            String errorDetail = "";
            if (response.errorBody() != null) {
                try {
                    errorDetail = response.errorBody().string();
                    Log.e(TAG, operationType + " server error. Code: " + response.code() + ", ErrorBody: " + errorDetail);
                } catch (IOException e) {
                    Log.e(TAG, "Error reading error body for " + operationType, e);
                }
            } else {
                Log.e(TAG, operationType + " server error. Code: " + response.code() + ", Message: " + response.message());
            }
            Toast.makeText(PostEditActivity.this, operationType + " 중 서버 오류 (Code: " + response.code() + ")" + (!errorDetail.isEmpty() ? "\n상세: " + errorDetail : ""), Toast.LENGTH_LONG).show();
        }
    }

    private void handlePostFailure(Throwable t, String operationType) {
        showLoading(false);
        Log.e(TAG, operationType + " API call failed", t);
        Toast.makeText(PostEditActivity.this, "네트워크 오류로 " + operationType + "에 실패했습니다: " + t.getMessage(), Toast.LENGTH_LONG).show();
    }

    private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) throw new IOException("Input stream is null");
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        try {
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
        } finally {
            // inputStream.close(); // try-with-resources를 사용하면 자동으로 닫힘. 여기서는 호출부에서 닫도록 함.
        }
        return byteBuffer.toByteArray();
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // 버튼 활성화/비활성화 (옵션)
        if (btnSubmit != null) btnSubmit.setEnabled(!isLoading);
        if (btnAttachFile != null) btnAttachFile.setEnabled(!isLoading);
        if (spinnerPostCategory != null) spinnerPostCategory.setEnabled(!isLoading);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 뒤로가기 버튼 처리
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
    }
}
