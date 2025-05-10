package com.example.smiti;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.ApiService;
import com.example.smiti.api.CreateGroupRequest;
import com.example.smiti.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateGroupActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etGroupName;
    private EditText etGroupDescription;
    private EditText etGroupTopic;
    private CheckBox cbUseAi;
    private Button btnCreateGroup;
    private ApiService apiService;
    private String searchQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        // 인텐트에서 검색어 가져오기
        if (getIntent() != null && getIntent().hasExtra("SEARCH_QUERY")) {
            searchQuery = getIntent().getStringExtra("SEARCH_QUERY");
        }

        // 뷰 초기화
        btnBack = findViewById(R.id.btn_back);
        etGroupName = findViewById(R.id.et_group_name);
        etGroupDescription = findViewById(R.id.et_group_description);
        etGroupTopic = findViewById(R.id.et_group_topic);
        cbUseAi = findViewById(R.id.cb_use_ai);
        btnCreateGroup = findViewById(R.id.btn_create_group);

        // 검색어가 있으면 그룹명에 자동 입력
        if (searchQuery != null && !searchQuery.isEmpty()) {
            etGroupName.setText(searchQuery);
        }

        // API 서비스 초기화
        apiService = RetrofitClient.getApiService();

        // 뒤로가기 버튼 리스너
        btnBack.setOnClickListener(v -> finish());

        // 그룹 생성 버튼 리스너
        btnCreateGroup.setOnClickListener(v -> createGroup());
    }

    private void createGroup() {
        // 입력값 가져오기
        String groupName = etGroupName.getText().toString().trim();
        String groupDescription = etGroupDescription.getText().toString().trim();
        String groupTopic = etGroupTopic.getText().toString().trim();
        boolean useAi = cbUseAi.isChecked();

        // 입력값 검증
        if (groupName.isEmpty()) {
            Toast.makeText(this, "그룹명을 입력해주세요", Toast.LENGTH_SHORT).show();
            etGroupName.requestFocus();
            return;
        }

        if (groupDescription.isEmpty()) {
            Toast.makeText(this, "그룹 설명을 입력해주세요", Toast.LENGTH_SHORT).show();
            etGroupDescription.requestFocus();
            return;
        }

        // 사용자 이메일 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("email", "");
        
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 로딩 표시
        btnCreateGroup.setEnabled(false);
        btnCreateGroup.setText("생성 중...");

        // 그룹 생성 요청 생성
        CreateGroupRequest request = new CreateGroupRequest();
        request.setGroup_name(groupName);
        request.setDescription(groupDescription);
        request.setEmail(userEmail);
        request.setTopics(groupTopic);
        request.setUseAi(useAi);

        // API 호출
        apiService.createGroup(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                btnCreateGroup.setEnabled(true);
                btnCreateGroup.setText("그룹 생성하기");

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(CreateGroupActivity.this, "그룹이 성공적으로 생성되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(CreateGroupActivity.this, "그룹 생성 실패: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    int errorCode = response.code();
                    Toast.makeText(CreateGroupActivity.this, "서버 오류: " + errorCode, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                btnCreateGroup.setEnabled(true);
                btnCreateGroup.setText("그룹 생성하기");
                Toast.makeText(CreateGroupActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
} 