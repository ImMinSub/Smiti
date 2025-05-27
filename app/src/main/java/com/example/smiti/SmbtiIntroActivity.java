package com.example.smiti;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SmbtiIntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smbti_intro);

        Button startTestButton = findViewById(R.id.start_test_button);
        
        startTestButton.setOnClickListener(v -> {
            Intent intent = new Intent(SmbtiIntroActivity.this, SmbtiTestActivity.class);
            startActivity(intent);
            finish(); // 소개 화면 종료
        });
    }
} 
