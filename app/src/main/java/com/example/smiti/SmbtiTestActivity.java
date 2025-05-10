package com.example.smiti;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.example.smiti.api.RetrofitClient;
import com.example.smiti.api.UpdateSmbtiRequest;
import com.example.smiti.api.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SmbtiTestActivity extends AppCompatActivity {
    private static final String TAG = "SmbtiTestActivity";
    private static final String PREF_NAME = "LoginPrefs";
    
    private TextView questionText;
    private TextView optionAText;
    private TextView optionBText;
    private CardView optionACard;
    private CardView optionBCard;
    private RadioGroup radioGroup;
    private RadioButton radioButtonA;
    private RadioButton radioButtonB;
    private Button nextButton;
    private Button prevButton;
    private TextView progressText;
    private ProgressBar progressBar;
    
    private List<SmbtiQuestion> questions;
    private int currentQuestion = 0;
    private int[] answers = new int[12]; // 0: A 선택, 1: B 선택, -1: 미선택
    private boolean optionASelected = false;
    private boolean optionBSelected = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smbti_test);
        
        // 뷰 초기화
        questionText = findViewById(R.id.question_text);
        optionAText = findViewById(R.id.option_a_text);
        optionBText = findViewById(R.id.option_b_text);
        optionACard = findViewById(R.id.option_a_card);
        optionBCard = findViewById(R.id.option_b_card);
        radioGroup = findViewById(R.id.radio_group);
        radioButtonA = findViewById(R.id.radio_button_a);
        radioButtonB = findViewById(R.id.radio_button_b);
        nextButton = findViewById(R.id.next_button);
        prevButton = findViewById(R.id.prev_button);
        progressText = findViewById(R.id.progress_text);
        progressBar = findViewById(R.id.progress_bar);
        
        // 질문 초기화
        initQuestions();
        
        // 답변 배열 초기화
        for (int i = 0; i < answers.length; i++) {
            answers[i] = -1; // 모든 질문을 미응답 상태로 초기화
        }
        
        // 첫 번째 질문 표시
        displayQuestion(currentQuestion);
        
        // 카드 클릭 이벤트 설정
        optionACard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectOptionA();
            }
        });
        
        optionBCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectOptionB();
            }
        });
        
        // 이전 버튼 클릭 이벤트
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentQuestion > 0) {
                    currentQuestion--;
                    displayQuestion(currentQuestion);
                }
            }
        });
        
        // 다음 버튼 클릭 이벤트
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 다음 질문으로 이동 또는 결과 처리
                if (currentQuestion < questions.size() - 1) {
                    currentQuestion++;
                    displayQuestion(currentQuestion);
                } else {
                    // 모든 질문 완료, 결과 계산
                    calculateResult();
                }
            }
        });
    }
    
    private void selectOptionA() {
        // 스타일 변경
        optionACard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        optionBCard.setCardBackgroundColor(getResources().getColor(R.color.cardLightBlue));
        
        // 라디오 버튼 체크 (로직 유지를 위해)
        radioButtonA.setChecked(true);
        
        // 상태 저장
        optionASelected = true;
        optionBSelected = false;
        
        // 답변 저장
        answers[currentQuestion] = 0;
        
        // 다음 버튼 활성화
        nextButton.setEnabled(true);
        nextButton.setAlpha(1.0f);
    }
    
    private void selectOptionB() {
        // 스타일 변경
        optionBCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        optionACard.setCardBackgroundColor(getResources().getColor(R.color.cardLightBlue));
        
        // 라디오 버튼 체크 (로직 유지를 위해)
        radioButtonB.setChecked(true);
        
        // 상태 저장
        optionBSelected = true;
        optionASelected = false;
        
        // 답변 저장
        answers[currentQuestion] = 1;
        
        // 다음 버튼 활성화
        nextButton.setEnabled(true);
        nextButton.setAlpha(1.0f);
    }
    
    private void initQuestions() {
        questions = new ArrayList<>();
        
        // 1-3: 학습 접근 방식 (이론형 vs 실행형)
        questions.add(new SmbtiQuestion(
                "Q1. 새로운 걸 배울 때, 어떤 것이 효과적이라 생각하시나요?",
                "새로운 내용을 배울 때,\n먼저 개념적인 설명이나\n이론을 이해하는 것이\n중요하다고 생각한다.",
                "새로운 내용을 배울 때,\n직접 해보거나 실습을 통해\n익히는 것이 더 효과적이라고\n생각한다."
        ));
        questions.add(new SmbtiQuestion(
                "Q2. 공부할 때, 어떤 방식을 선호하시나요?",
                "설명을 듣고 머릿속으로\n논리적으로 정리하는 과정을\n중요하게 생각한다.",
                "직접 문제를 풀거나\n자료를 다루면서\n감각적으로 이해하는 것을\n선호한다."
        ));
        questions.add(new SmbtiQuestion(
                "Q3. 학습 방법으로, 어떤 것을 선호하시나요?",
                "전체적인 구조나 순서를\n먼저 파악하고 나서\n세부적인 내용을 공부하는 것을\n선호한다.",
                "전체적인 구조보다는\n당장 눈앞의 과제나 문제부터\n해결하면서 학습하는 것을\n선호한다."
        ));
        
        // 4-6: 협업 스타일 (개별학습형 vs 협력형)
        questions.add(new SmbtiQuestion(
                "Q4. 학습 환경으로, 어떤 것이 더 효율적인가요?",
                "혼자 조용한 환경에서\n집중하여 공부하는 것이\n더 효율적이라고 느낀다.",
                "여러 사람들과 함께\n아이디어를 공유하고 토론하면서\n공부하는 것이 더 즐겁고\n효과적이라고 느낀다."
        ));
        questions.add(new SmbtiQuestion(
                "Q5. 학습 계획에서, 어떤 것이 편한가요?",
                "그룹 스터디보다는 혼자\n계획을 세우고 자신의 계획대로\n공부하는 것이 편하다.",
                "다른 사람들과 함께 목표를\n설정하고 서로에게 동기 부여를\n받으며 공부하는 것을 선호한다."
        ));
        questions.add(new SmbtiQuestion(
                "Q6. 협업에 대해, 어떻게 생각하시나요?",
                "다른 사람들의 의견이 많거나\n협업 과정이 복잡하면\n오히려 집중하기 어렵다고 느낀다.",
                "다양한 사람들의 관점을 접하고\n함께 문제를 해결해 나가는\n과정에서 새로운 아이디어를\n얻는 것을 좋아한다."
        ));
        
        // 7-9: 학습 동기 (목표지향형 vs 유연지향형)
        questions.add(new SmbtiQuestion(
                "Q7. 학습 계획에 대해, 어떤 것이 중요한가요?",
                "학습 계획을 꼼꼼하게 세우고\n그 계획에 따라 꾸준히 실천하는\n것을 중요하게 생각한다.",
                "미리 정해진 계획보다는\n자신의 컨디션이나 상황에 맞춰\n학습 내용을 유연하게\n조절하는 것을 선호한다."
        ));
        questions.add(new SmbtiQuestion(
                "Q8. 학습 목표에 대해, 어떤 것이 만족스러운가요?",
                "정해진 학습 시간과 목표량을\n채우는 것에 만족감을 느낀다.",
                "학습 진도나 시간에 얽매이기보다는\n흥미를 느끼는 분야를\n깊이 있게 탐구하는 것을 좋아한다."
        ));
        questions.add(new SmbtiQuestion(
                "Q9. 계획 변경에 대해, 어떻게 생각하시나요?",
                "예상치 못한 상황이나\n변경 사항이 발생하는 것을\n별로 좋아하지 않는다.",
                "새로운 아이디어가 떠오르거나\n흥미로운 주제가 생기면\n원래 계획을 수정해서라도\n시도해보고 싶어 한다."
        ));
        
        // 10-12: 정보 수용 방식 (세부중심형 vs 아이디어중심형)
        questions.add(new SmbtiQuestion(
                "Q10. 학습 내용 이해에서, 어떤 것이 더 쉬운가요?",
                "구체적인 예시나 사례를 통해\n이해하는 것이 더 쉽다.",
                "핵심적인 원리나 개념을\n먼저 파악하는 것이 중요하다."
        ));
        questions.add(new SmbtiQuestion(
                "Q11. 정보 선호도에서, 어떤 것을 선호하시나요?",
                "폭넓은 아이디어나 추상적인\n설명보다는 명확하고\n자세한 정보를 선호한다.",
                "세부적인 내용보다는\n전체적인 그림이나 맥락을\n이해하는 데 더 집중한다."
        ));
        questions.add(new SmbtiQuestion(
                "Q12. 문제 해결 접근법으로, 어떤 것을 선호하시나요?",
                "복잡한 문제나 과제를 해결할 때,\n단계별로 차근차근 접근하는 것을\n선호한다.",
                "복잡한 문제나 과제를 해결할 때,\n다양한 가능성을 열어두고\n창의적인 해결책을 모색하는 것을\n즐긴다."
        ));
    }
    
    private void displayQuestion(int questionIndex) {
        SmbtiQuestion question = questions.get(questionIndex);
        questionText.setText(question.getQuestion());
        optionAText.setText(question.getOptionA());
        optionBText.setText(question.getOptionB());
        progressText.setText((questionIndex + 1) + " / " + questions.size());
        
        // 프로그레스 바 업데이트
        int progressPercentage = (int) (((float) (questionIndex + 1) / questions.size()) * 100);
        progressBar.setProgress(progressPercentage);
        
        // 이전 버튼 표시 여부 설정
        if (questionIndex == 0) {
            prevButton.setVisibility(View.INVISIBLE);
        } else {
            prevButton.setVisibility(View.VISIBLE);
        }
        
        // 이전에 응답한 답변이 있으면 상태 복원
        optionASelected = false;
        optionBSelected = false;
        optionACard.setCardBackgroundColor(getResources().getColor(R.color.cardLightBlue));
        optionBCard.setCardBackgroundColor(getResources().getColor(R.color.cardLightBlue));
        
        radioGroup.clearCheck();
        if (answers[questionIndex] == 0) {
            selectOptionA();
        } else if (answers[questionIndex] == 1) {
            selectOptionB();
        } else {
            // 응답하지 않은 질문은 다음 버튼 비활성화
            nextButton.setEnabled(false);
            nextButton.setAlpha(0.5f);
        }
    }
    
    private void calculateResult() {
        // 결과 계산
        // 학습 접근 방식 (T/E)
        String firstLetter = calculateFirstLetter();
        
        // 협업 스타일 (I/C)
        String secondLetter = calculateSecondLetter();
        
        // 학습 동기 (P/F)
        String thirdLetter = calculateThirdLetter();
        
        // 정보 수용 방식 (D/M)
        String fourthLetter = calculateFourthLetter();
        
        // 최종 결과
        String smbtiResult = firstLetter + secondLetter + thirdLetter + fourthLetter;
        
        // 결과 저장 및 서버에 전송
        saveSmbtiResult(smbtiResult);
    }
    
    private String calculateFirstLetter() {
        // 첫 3개 질문 (0, 1, 2)
        int theoryCount = 0;
        int experienceCount = 0;
        
        for (int i = 0; i <= 2; i++) {
            if (answers[i] == 0) {
                theoryCount++;
            } else {
                experienceCount++;
            }
        }
        
        return theoryCount > experienceCount ? "T" : "E";
    }
    
    private String calculateSecondLetter() {
        // 다음 3개 질문 (3, 4, 5)
        int individualCount = 0;
        int collaborativeCount = 0;
        
        for (int i = 3; i <= 5; i++) {
            if (answers[i] == 0) {
                individualCount++;
            } else {
                collaborativeCount++;
            }
        }
        
        return individualCount > collaborativeCount ? "I" : "C";
    }
    
    private String calculateThirdLetter() {
        // 다음 3개 질문 (6, 7, 8)
        int plannedCount = 0;
        int flexibleCount = 0;
        
        for (int i = 6; i <= 8; i++) {
            if (answers[i] == 0) {
                plannedCount++;
            } else {
                flexibleCount++;
            }
        }
        
        return plannedCount > flexibleCount ? "P" : "F";
    }
    
    private String calculateFourthLetter() {
        // 마지막 3개 질문 (9, 10, 11)
        int detailCount = 0;
        int conceptCount = 0;
        
        for (int i = 9; i <= 11; i++) {
            if (answers[i] == 0) {
                detailCount++;
            } else {
                conceptCount++;
            }
        }
        
        return detailCount > conceptCount ? "D" : "M";
    }
    
    private void saveSmbtiResult(final String smbtiResult) {
        Log.d(TAG, "SMBTI 결과 저장 시작: " + smbtiResult);
        
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        final String email = sharedPreferences.getString("email", "");
        
        Log.d(TAG, "사용자 이메일: " + email);
        
        if (email.isEmpty()) {
            showResultDialog(smbtiResult);
            return;
        }
        
        // SMBTI 결과를 로컬에 저장
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("mbti", smbtiResult);
        editor.apply();
        Log.d(TAG, "SMBTI 결과 로컬 저장 완료");
        
        // 서버에 SMBTI 결과 업데이트 요청
        UpdateSmbtiRequest request = new UpdateSmbtiRequest(email, smbtiResult);
        Log.d(TAG, "서버 요청 데이터 - 이메일: " + email + ", SMBTI: " + smbtiResult);
        
        // Retrofit 호출
        Call<ApiResponse> call = RetrofitClient.getApiService().updateSmbti(request);
        Log.d(TAG, "서버 URL: " + call.request().url().toString());
        
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                Log.d(TAG, "서버 응답 코드: " + response.code());
                
                if (response.isSuccessful()) {
                    // Toast 대신 AlertDialog로 결과 표시
                    showResultDialog(smbtiResult);
                    
                    if (response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        Log.d(TAG, "서버 응답 성공: " + apiResponse.getStatus() + ", 메시지: " + apiResponse.getMessage());
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "알 수 없는 오류";
                        Log.e(TAG, "서버 응답 오류: " + response.code() + ", 내용: " + errorBody);
                        // 서버 오류가 발생해도 결과 표시
                        showResultDialog(smbtiResult);
                    } catch (Exception e) {
                        Log.e(TAG, "에러 바디 읽기 실패", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "서버 연결 오류: " + t.getMessage(), t);
                // 서버 연결 오류가 발생해도 결과 표시
                showResultDialog(smbtiResult);
            }
        });
    }
    
    // AlertDialog로 SMBTI 결과를 표시하는 메소드
    private void showResultDialog(final String smbtiResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("당신의 SMBTI는?")
               .setMessage(smbtiResult)
               .setPositiveButton("닫기", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finishWithResult(smbtiResult);
                    }
                })
               .setCancelable(false)
               .show();
    }
    
    // 에러가 발생해도 결과를 메인 화면으로 돌려주는 메소드
    private void finishWithResult(String smbtiResult) {
        Intent intent = new Intent();
        intent.putExtra("smbti_result", smbtiResult);
        setResult(RESULT_OK, intent);
        finish();
    }
    
    // SMBTI 질문 클래스
    private static class SmbtiQuestion {
        private String question;
        private String optionA;
        private String optionB;
        
        public SmbtiQuestion(String question, String optionA, String optionB) {
            this.question = question;
            this.optionA = optionA;
            this.optionB = optionB;
        }
        
        public String getQuestion() {
            return question;
        }
        
        public String getOptionA() {
            return optionA;
        }
        
        public String getOptionB() {
            return optionB;
        }
    }
} 