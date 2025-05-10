package com.example.smiti;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class FindPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button findButton;
    private ImageButton backButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_password);
        
        // 뷰 초기화
        emailEditText = findViewById(R.id.email);
        findButton = findViewById(R.id.find_button);
        backButton = findViewById(R.id.back_button);
        
        // 뒤로가기 버튼 클릭 이벤트
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        
        // 비밀번호 찾기 버튼 클릭 이벤트
        findButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 입력값 가져오기
                String email = emailEditText.getText().toString();
                
                // 간단한 유효성 검사
                if (email.isEmpty()) {
                    Toast.makeText(FindPasswordActivity.this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 이메일 형식 검사
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(FindPasswordActivity.this, "올바른 이메일 형식이 아닙니다", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 비밀번호 찾기 로직 구현 (현재는 더미 메시지 표시)
                Toast.makeText(FindPasswordActivity.this, "입력하신 이메일로 비밀번호 재설정 링크를 발송했습니다", Toast.LENGTH_LONG).show();
                
                // TODO: 서버 통신을 통한 비밀번호 재설정 이메일 발송 구현
                // resetPassword(email);
            }
        });
    }
    
    // 서버 통신을 통한 비밀번호 재설정 메소드
    private void resetPassword(String email) {
        // TODO: API 호출을 통한 비밀번호 재설정 구현
    }
} 