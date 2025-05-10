package com.example.smiti;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class FindIdActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText phoneNumberEditText;
    private Button findButton;
    private ImageButton backButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id);
        
        // 뷰 초기화
        nameEditText = findViewById(R.id.name);
        phoneNumberEditText = findViewById(R.id.phone_number);
        findButton = findViewById(R.id.find_button);
        backButton = findViewById(R.id.back_button);
        
        // 뒤로가기 버튼 클릭 이벤트
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        
        // 아이디 찾기 버튼 클릭 이벤트
        findButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 입력값 가져오기
                String name = nameEditText.getText().toString();
                String phoneNumber = phoneNumberEditText.getText().toString();
                
                // 간단한 유효성 검사
                if (name.isEmpty() || phoneNumber.isEmpty()) {
                    Toast.makeText(FindIdActivity.this, "모든 항목을 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 아이디 찾기 로직 구현 (현재는 더미 메시지 표시)
                Toast.makeText(FindIdActivity.this, "입력하신 정보로 아이디를 찾는 중입니다...", Toast.LENGTH_LONG).show();
                
                // TODO: 서버 통신을 통한 아이디 찾기 구현
                // findId(name, phoneNumber);
            }
        });
    }
    
    // 서버 통신을 통한 아이디 찾기 메소드
    private void findId(String name, String phoneNumber) {
        // TODO: API 호출을 통한 아이디 찾기 구현
    }
} 