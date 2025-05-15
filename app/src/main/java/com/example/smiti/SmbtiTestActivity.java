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
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
    private List<SmbtiQuestion> selectedQuestions; // 각 카테고리에서 선택된 질문들
    private int currentQuestion = 0;
    private int[] answers; // 0: A 선택, 1: B 선택, -1: 미선택
    private boolean optionASelected = false;
    private boolean optionBSelected = false;
    
    // 각 카테고리별 인덱스 범위 상수
    private static final int CATEGORY_1_START = 0;  // 학습 접근 방식 시작 인덱스
    private static final int CATEGORY_1_END = 2;    // 학습 접근 방식 끝 인덱스
    private static final int CATEGORY_2_START = 3;  // 협업 스타일 시작 인덱스
    private static final int CATEGORY_2_END = 5;    // 협업 스타일 끝 인덱스
    private static final int CATEGORY_3_START = 6;  // 학습 동기 시작 인덱스
    private static final int CATEGORY_3_END = 8;    // 학습 동기 끝 인덱스
    private static final int CATEGORY_4_START = 9;  // 정보 수용 방식 시작 인덱스
    private static final int CATEGORY_4_END = 11;   // 정보 수용 방식 끝 인덱스
    
    // 각 카테고리별 선택된 질문 인덱스를 저장하는 배열
    private int[][] selectedQuestionIndices;
    
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
        
        // 질문 초기화 (이미 내부에서 카테고리별 질문을 3개씩 랜덤하게 선택함)
        initQuestions();
        
        // 선택된 질문을 저장
        selectedQuestions = new ArrayList<>(questions);
        
        // 답변 배열 초기화
        answers = new int[questions.size()]; // 질문 개수에 맞게 배열 크기 조정
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
        List<SmbtiQuestion> category1 = new ArrayList<>(); // 학습 접근 방식 (이론형 vs 실행형)
        List<SmbtiQuestion> category2 = new ArrayList<>(); // 협업 스타일 (개별학습형 vs 협력형)
        List<SmbtiQuestion> category3 = new ArrayList<>(); // 학습 동기 (목표지향형 vs 유연지향형)
        List<SmbtiQuestion> category4 = new ArrayList<>(); // 정보 수용 방식 (세부중심형 vs 아이디어중심형)
        
        // 1. 학습 접근 방식: 이론형 vs 실행형 (5개 질문)
        category1.add(new SmbtiQuestion(
                "1-1. 새로운 것을 배울 때",
                "개념, 원리 등 이론적 설명부터 깊이 이해해야 한다.",
                "일단 해보면서 직접 경험하고 익히는 것이 편하다."
        ));
        category1.add(new SmbtiQuestion(
                "1-2. 공부나 작업에 들어가기 전에",
                "관련 이론이나 배경 지식을 충분히 학습하고 시작하는 편이다.",
                "바로 실전 문제나 과제에 부딪히며 배워나가는 편이다."
        ));
        category1.add(new SmbtiQuestion(
                "1-3. 설명 자료를 볼 때",
                "실생활 적용 예시보다 개념 설명이 더 와닿고 중요하다.",
                "개념 설명보다는 직접 해볼 수 있는 실습 과정에 집중하게 된다."
        ));
        category1.add(new SmbtiQuestion(
                "1-4. 문제를 푸는 방식",
                "개념을 먼저 확실히 이해한 후에 문제를 푸는 편이다.",
                "문제를 먼저 풀어보면서 개념을 역으로 파악하기도 한다."
        ));
        category1.add(new SmbtiQuestion(
                "1-5. 학습 효율을 높이는 방법",
                "설명을 듣고 머릿속으로 체계적으로 정리하는 것이 중요하다.",
                "설명을 듣는 것보다 직접 손으로 만지거나 조작하며 해보는 것이 더 효과적이다."
        ));
        
        // 2. 협업 스타일: 개별학습형 vs 협력형 (5개 질문)
        category2.add(new SmbtiQuestion(
                "2-1. 선호하는 학습 환경",
                "혼자 조용히 집중하며 공부할 때 가장 효율이 높다.",
                "여러 사람과 함께 아이디어를 나누고 소통하며 공부할 때 더 잘 된다."
        ));
        category2.add(new SmbtiQuestion(
                "2-2. 그룹 스터디 참여",
                "그룹 스터디보다는 혼자 계획을 세우고 실행하는 것이 더 익숙하고 편하다.",
                "스터디에 참여하면 책임감이 생기고 진도가 잘 나가는 편이다."
        ));
        category2.add(new SmbtiQuestion(
                "2-3. 다른 사람의 의견",
                "타인의 의견이 많으면 오히려 집중력이 흐트러지고 방해가 되기도 한다.",
                "공부하다 막히는 부분이 있으면 누군가와 대화하며 해결하는 것이 더 빠르고 효과적이다."
        ));
        category2.add(new SmbtiQuestion(
                "2-4. 혼자 있는 시간 vs 함께 하는 시간",
                "혼자만의 시간이 공부 효율을 높이는 데 중요하다고 느낀다.",
                "같은 목표를 가진 사람들과 함께 할 때 동기 부여가 되고 공부에 도움이 된다."
        ));
        category2.add(new SmbtiQuestion(
                "2-5. 이해를 돕는 방식",
                "혼자 차분히 정리하며 스스로 이해하는 것을 선호한다.",
                "다른 사람에게 설명해주거나 설명을 들을 때 이해가 더 잘 되는 편이다."
        ));
        
        // 3. 학습 동기: 목표 지향형 vs 유연지향형 (5개 질문)
        category3.add(new SmbtiQuestion(
                "3-1. 학습 계획의 중요성",
                "학습 전에 명확한 목표와 구체적인 계획을 세우는 것이 매우 중요하다.",
                "계획을 세우는 것보다 일단 시작하고 그때그때 상황에 맞추는 것이 더 자연스럽다."
        ));
        category3.add(new SmbtiQuestion(
                "3-2. 계획 준수 vs 유연성",
                "계획표를 만들어두면 그에 따라 실천하려고 노력하며 마음이 편안하다.",
                "기분이나 컨디션에 따라 학습 시간을 유동적으로 조절하는 것을 선호한다."
        ));
        category3.add(new SmbtiQuestion(
                "3-3. 마감일의 영향",
                "마감일이나 일정이 명확해야 집중력이 높아지고 실행하게 된다.",
                "마감일에 쫓기기보다 가능한 만큼 꾸준히 해내는 걸 선호하며, 갑작스러운 일정 변경에도 유연하게 대처한다."
        ));
        category3.add(new SmbtiQuestion(
                "3-4. 동기 부여 요인",
                "학습 목표가 분명하고 구체적일수록 더 강하게 동기 부여가 된다.",
                "목표를 정해두지 않아도 흥미나 필요에 따라 자율적으로 학습하는 편이다."
        ));
        category3.add(new SmbtiQuestion(
                "3-5. 학습 흐름 조절",
                "정해진 계획과 루틴에 따라 학습 흐름을 유지하는 것이 중요하다.",
                "스스로 학습 흐름을 조절하며 자율적으로 공부하는 것을 더 편하게 느낀다."
        ));
        
        // 4. 정보 수용 방식: 세부중심형 vs 아이디어 중심형 (5개 질문)
        category4.add(new SmbtiQuestion(
                "4-1. 정보 습득 시 초점",
                "구체적인 예시, 정확한 정의, 세부 원리 등 디테일한 정보에 먼저 집중한다.",
                "전체적인 맥락, 핵심 아이디어, 큰 그림을 먼저 파악하려고 한다."
        ));
        category4.add(new SmbtiQuestion(
                "4-2. 설명이나 자료를 볼 때",
                "작은 부분이나 디테일한 표현 하나하나까지 놓치지 않으려고 노력한다.",
                "핵심 개념이나 몇 가지 중요한 내용만으로도 충분히 이해가 된다고 느낀다."
        ));
        category4.add(new SmbtiQuestion(
                "4-3. 중요한 정보의 기준",
                "전체 흐름이나 구조보다는 정확한 사실이나 세부 사항이 더 중요하다고 생각한다.",
                "세부 내용보다는 논리 구조나 전체적인 방향성을 이해하는 것이 우선이라고 생각한다."
        ));
        category4.add(new SmbtiQuestion(
                "4-4. 학습 자료 검토 시",
                "학습 자료에서 빠지거나 잘못된 세부 사항이 보이면 찝찝하고 신경 쓰인다.",
                "먼저 전체 맥락을 파악하고 나면 세부 내용은 자연스럽게 따라오거나 나중에 보충해도 된다고 생각한다."
        ));
        category4.add(new SmbtiQuestion(
                "4-5. 개념 이해 방식",
                "구체적인 사례나 문제를 통해 개념을 배우는 것을 좋아한다.",
                "개념적 틀이나 구조를 먼저 알고 나면 세부 내용은 더 쉽게 이해된다."
        ));
        
        // 각 카테고리에서 3개씩 랜덤 선택
        Collections.shuffle(category1);
        Collections.shuffle(category2);
        Collections.shuffle(category3);
        Collections.shuffle(category4);
        
        // 선택된 질문 추가 (순서대로 카테고리1→카테고리2→카테고리3→카테고리4)
        for (int i = 0; i < 3; i++) questions.add(category1.get(i));
        for (int i = 0; i < 3; i++) questions.add(category2.get(i));
        for (int i = 0; i < 3; i++) questions.add(category3.get(i));
        for (int i = 0; i < 3; i++) questions.add(category4.get(i));
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
        // 첫 번째 카테고리 (0, 1, 2번 인덱스)
        int theoryCount = 0;
        int experienceCount = 0;
        
        for (int i = 0; i <= 2; i++) {
            if (answers[i] == 0) {
                theoryCount++;
            } else if (answers[i] == 1) {
                experienceCount++;
            }
        }
        
        return theoryCount > experienceCount ? "T" : "E";
    }
    
    private String calculateSecondLetter() {
        // 두 번째 카테고리 (3, 4, 5번 인덱스)
        int individualCount = 0;
        int collaborativeCount = 0;
        
        for (int i = 3; i <= 5; i++) {
            if (answers[i] == 0) {
                individualCount++;
            } else if (answers[i] == 1) {
                collaborativeCount++;
            }
        }
        
        return individualCount > collaborativeCount ? "I" : "C";
    }
    
    private String calculateThirdLetter() {
        // 세 번째 카테고리 (6, 7, 8번 인덱스)
        int plannedCount = 0;
        int flexibleCount = 0;
        
        for (int i = 6; i <= 8; i++) {
            if (answers[i] == 0) {
                plannedCount++;
            } else if (answers[i] == 1) {
                flexibleCount++;
            }
        }
        
        return plannedCount > flexibleCount ? "P" : "F";
    }
    
    private String calculateFourthLetter() {
        // 네 번째 카테고리 (9, 10, 11번 인덱스)
        int detailCount = 0;
        int conceptCount = 0;
        
        for (int i = 9; i <= 11; i++) {
            if (answers[i] == 0) {
                detailCount++;
            } else if (answers[i] == 1) {
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
