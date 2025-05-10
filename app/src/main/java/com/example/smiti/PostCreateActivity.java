package com.example.smiti;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.ApiService;
import com.example.smiti.api.RetrofitClient;

import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * 파일 없이 게시글을 생성하는 예제 액티비티
 * 새로운 createPost API를 사용하는 방법을 보여줍니다.
 */
public class PostCreateActivity extends AppCompatActivity {

    private EditText etTitle, etContent, etEmail;
    private Spinner spinnerCategory;
    private Button btnSubmit;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_create);
        
        // 뷰 초기화
        initViews();
        
        // 카테고리 스피너 설정
        setupCategorySpinner();
        
        // 버튼 클릭 리스너 설정
        btnSubmit.setOnClickListener(v -> submitPost());
    }
    
    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        etEmail = findViewById(R.id.etEmail);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSubmit = findViewById(R.id.btnSubmit);
    }
    
    private void setupCategorySpinner() {
        // 카테고리 배열 (board_type)
        String[] categories = {"자유", "공지", "질문", "정보"};
        
        // 스피너 어댑터 설정
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }
    
    private void submitPost() {
        // 입력값 검증
        if (!validateInputs()) {
            return;
        }
        
        // 입력 값 가져오기
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        
        // API 명세서에 맞게 각 필드를 RequestBody로 변환
        RequestBody emailPart = RequestBody.create(MediaType.parse("text/plain"), email);
        RequestBody titlePart = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody contentPart = RequestBody.create(MediaType.parse("text/plain"), content);
        RequestBody boardTypePart = RequestBody.create(MediaType.parse("text/plain"), category);
        
        // API 호출
        ApiService apiService = RetrofitClient.getApiService();
        Call<ApiResponse> call = apiService.createPost(emailPart, boardTypePart, titlePart, contentPart);
        
        // 비동기 API 호출 실행
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(PostCreateActivity.this, 
                                "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(PostCreateActivity.this, 
                                "게시글 등록 실패: " + apiResponse.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(PostCreateActivity.this, 
                            "서버 응답 오류: " + response.code(), 
                            Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toast.makeText(PostCreateActivity.this, 
                        "네트워크 오류: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private boolean validateInputs() {
        // 제목 검증
        if (etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            etTitle.requestFocus();
            return false;
        }
        
        // 내용 검증
        if (etContent.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            etContent.requestFocus();
            return false;
        }
        
        // 이메일 검증
        if (etEmail.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        }
        
        return true;
    }
} 