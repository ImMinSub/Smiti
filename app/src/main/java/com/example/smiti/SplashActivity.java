package com.example.smiti;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    
    private static final String TAG = "SplashActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_AUTO_LOGIN = "auto_login";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    
    private ImageView logoImageView;
    private TextView taglineTextView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        
        // UI 요소 초기화
        logoImageView = findViewById(R.id.splash_logo);
        taglineTextView = findViewById(R.id.splash_text);
        
        // 애니메이션 시작
        startAnimations();
        
        // 메인 액티비티로 전환 (3.5초 후)
        new Handler().postDelayed(this::checkLoginStatusAndNavigate, 3500);
    }
    
    private void startAnimations() {
        // 로고 초기 상태 설정 (투명도 0)
        logoImageView.setAlpha(0f);
        
        // 1. 로고 애니메이션: 화면 왼쪽 바깥에서 중앙으로 서서히 이동
        ObjectAnimator logoSlide = ObjectAnimator.ofFloat(
                logoImageView, View.TRANSLATION_X, -2000f, 0f);
        logoSlide.setDuration(1500); // 더 긴 시간 동안 이동
        logoSlide.setInterpolator(new DecelerateInterpolator(1.5f)); // 부드러운 감속
        
        // 로고 페이드인 애니메이션 (좀 더 천천히)
        ObjectAnimator logoFadeIn = ObjectAnimator.ofFloat(
                logoImageView, View.ALPHA, 0f, 1f);
        logoFadeIn.setDuration(1200);
        
        // 2. 텍스트 애니메이션: 로고 이동이 완료된 후 천천히 페이드인
        ObjectAnimator textFadeIn = ObjectAnimator.ofFloat(
                taglineTextView, View.ALPHA, 0f, 1f);
        textFadeIn.setDuration(1000);
        textFadeIn.setStartDelay(500); // 로고 이동이 거의 완료된 후 시작
        
        // 텍스트 위치 애니메이션 (약간 위에서 아래로 서서히 이동)
        ObjectAnimator textSlideDown = ObjectAnimator.ofFloat(
                taglineTextView, View.TRANSLATION_Y, -15f, 0f);
        textSlideDown.setDuration(1000);
        textSlideDown.setStartDelay(500);
        textSlideDown.setInterpolator(new DecelerateInterpolator());
        
        // 로고 애니메이션 세트
        AnimatorSet logoAnimSet = new AnimatorSet();
        logoAnimSet.playTogether(logoSlide, logoFadeIn);
        
        // 텍스트 애니메이션 세트
        AnimatorSet textAnimSet = new AnimatorSet();
        textAnimSet.playTogether(textFadeIn, textSlideDown);
        
        // 전체 애니메이션 시퀀스 구성
        AnimatorSet fullAnimSet = new AnimatorSet();
        fullAnimSet.play(logoAnimSet);
        fullAnimSet.play(textAnimSet).after(700); // 로고 애니메이션이 어느 정도 진행된 후 텍스트 애니메이션 시작
        fullAnimSet.start();
    }
    
    private void checkLoginStatusAndNavigate() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        String email = sharedPreferences.getString(KEY_EMAIL, "");
        
        Log.d(TAG, "로그인 상태 확인: " + isLoggedIn);
        Log.d(TAG, "저장된 이메일: " + (email.isEmpty() ? "없음" : email));
        
        Intent intent;
        
        // 로그인 상태면 MainActivity로 이동
        if (isLoggedIn && !email.isEmpty()) {
            Log.d(TAG, "로그인 상태 확인됨: MainActivity로 이동");
            intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.putExtra("email", email); // MainActivity에서 필요할 수 있음
        } else {
            // 로그인 상태가 아니면 LoginActivity로 이동
            Log.d(TAG, "로그인 상태 아님: LoginActivity로 이동");
            // 이전 로그인 정보가 남아있을 수 있으므로 초기화 (선택적)
            // SharedPreferences.Editor editor = sharedPreferences.edit();
            // editor.clear(); // 로그아웃되지 않은 상태에서 정보가 남아있을 수 있으므로 명시적 로그아웃 전까지는 클리어하지 않음
            // editor.apply();
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }
        
        startActivity(intent);
        
        // 페이드인 트랜지션 효과 적용 (커스텀 애니메이션 리소스 사용)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        
        // 스플래시 액티비티 종료
        finish();
    }
} 