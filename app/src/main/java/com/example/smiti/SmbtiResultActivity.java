package com.example.smiti;

// 현재 나와 잘 맞는 스터디 그룹 찾기 기능 미구현 상태

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SmbtiResultActivity extends AppCompatActivity {
    private TextView tvSmbtiType;
    private TextView tvSmbtiDescription;
    private TextView tvPersonalityDescription;
    private TextView tvLearningStyle1;
    private TextView tvLearningStyle2;
    private TextView tvLearningStyle3;
    private TextView tvStudyMethod1;
    private TextView tvStudyMethod2;
    private TextView tvStudyMethod3;
    private Button btnFindStudyGroup;
    private Button btnRetakeTest;
    private ImageView btnBack;
    private ImageView ivProfile;
    private LinearLayout layoutPersonalityTagsRow1;
    private LinearLayout layoutPersonalityTagsRow2;

    private String smbtiResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smbti_result);

        // Intent에서 SMBTI 결과 받기
        smbtiResult = getIntent().getStringExtra("smbti_result");
        if (smbtiResult == null) {
            smbtiResult = "default"; // 기본값
        }

        initViews();
        setupSmbtiResult();
        setupClickListeners();
        setupButtonStyles();

        // 뒤로가기 버튼 클릭 시 메인 화면으로 이동
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SmbtiResultActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    private void initViews() {
        tvSmbtiType = findViewById(R.id.tv_smbti_type);
        tvSmbtiDescription = findViewById(R.id.tv_smbti_description);
        tvPersonalityDescription = findViewById(R.id.tv_personality_description);
        tvLearningStyle1 = findViewById(R.id.tv_learning_style_1);
        tvLearningStyle2 = findViewById(R.id.tv_learning_style_2);
        tvLearningStyle3 = findViewById(R.id.tv_learning_style_3);
        tvStudyMethod1 = findViewById(R.id.tv_study_method_1);
        tvStudyMethod2 = findViewById(R.id.tv_study_method_2);
        tvStudyMethod3 = findViewById(R.id.tv_study_method_3);
        btnFindStudyGroup = findViewById(R.id.btn_find_study_group);
        btnRetakeTest = findViewById(R.id.btn_retake_test);
        btnBack = findViewById(R.id.btn_back);
        ivProfile = findViewById(R.id.iv_profile);
        layoutPersonalityTagsRow1 = findViewById(R.id.layout_personality_tags_row1);
        layoutPersonalityTagsRow2 = findViewById(R.id.layout_personality_tags_row2);
    }

    private void setupSmbtiResult() {
        tvSmbtiType.setText(smbtiResult);

        // SMBTI 유형별 데이터 설정
        SmbtiData data = getSmbtiData(smbtiResult);
        tvSmbtiDescription.setText(data.description);
        tvPersonalityDescription.setText(data.personalityDescription);
        tvLearningStyle1.setText(data.learningStyle1);
        tvLearningStyle2.setText(data.learningStyle2);
        tvLearningStyle3.setText(data.learningStyle3);
        tvStudyMethod1.setText(data.studyMethod1);
        tvStudyMethod2.setText(data.studyMethod2);
        tvStudyMethod3.setText(data.studyMethod3);

        // 색상 및 이미지 설정
        setupSmbtiColors();
        setupProfileImage();

        // 성격 특징 태그 설정
        setupPersonalityTags(data.personalityTags);
    }

    private void setupSmbtiColors() {
        String resultColor = getSmbtiResultColor(smbtiResult);
        
        // SMBTI 결과 배경색 설정
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor(resultColor));
        drawable.setCornerRadius(24f);
        tvSmbtiType.setBackground(drawable);
    }

    private void setupProfileImage() {
        int imageResId = getSmbtiImageResource(smbtiResult);
        if (imageResId != 0) {
            ivProfile.setImageResource(imageResId);
        }
    }

    private void setupButtonStyles() {
        // "나와 잘 맞는 스터디 그룹 찾기" 버튼 - 불투명
        GradientDrawable findGroupDrawable = new GradientDrawable();
        findGroupDrawable.setShape(GradientDrawable.RECTANGLE);
        findGroupDrawable.setColor(Color.parseColor("#1F4059"));
        findGroupDrawable.setCornerRadius(25f);
        btnFindStudyGroup.setBackground(findGroupDrawable);

        // "다시 테스트하기" 버튼 - 30% 투명
        GradientDrawable retakeDrawable = new GradientDrawable();
        retakeDrawable.setShape(GradientDrawable.RECTANGLE);
        retakeDrawable.setColor(Color.parseColor("#4D1F4059")); // 30% alpha
        retakeDrawable.setCornerRadius(25f);
        btnRetakeTest.setBackground(retakeDrawable);
    }

    // 결과 색상 설정
    private String getSmbtiResultColor(String smbtiType) {
        switch (smbtiType) {
            case "TIPD": return "#57ABF9";
            case "TIPM": return "#7893A8";
            case "TIFD": return "#7C8E72";
            case "TIFM": return "#C19BCA";
            case "TCPD": return "#7421A4";
            case "TCPM": return "#61A9E0";
            case "TCFD": return "#8E6458";
            case "TCFM": return "#F3998A";
            case "EIPD": return "#FAA125";
            case "EIPM": return "#F4E150";
            case "EIFD": return "#BCA596";
            case "EIFM": return "#FFD4B9";
            case "ECPD": return "#E34545";
            case "ECPM": return "#42948E";
            case "ECFD": return "#76D9C8";
            case "ECFM": return "#DDBCED";
            default: return "#8B5CF6"; // 기본 색상
        }
    }

    // 태그 색상 설정
    private String getSmbtiTagColor(String smbtiType) {
        String baseColor = getSmbtiResultColor(smbtiType);
        // 50% 투명도 적용 - 모든 케이스에서 "#"만 제거하고 "80" 추가
        return "#80" + baseColor.substring(1);
    }

    private int getSmbtiImageResource(String smbtiType) {
        switch (smbtiType) {
            case "TIPD": return R.drawable.tipd;
            case "TIPM": return R.drawable.tipm;
            case "TIFD": return R.drawable.tifd;
            case "TIFM": return R.drawable.tifm;
            case "TCPD": return R.drawable.tcpd;
            case "TCPM": return R.drawable.tcpm;
            case "TCFD": return R.drawable.tcfd;
            case "TCFM": return R.drawable.tcfm;
            case "EIPD": return R.drawable.eipd;
            case "EIPM": return R.drawable.eipm;
            case "EIFD": return R.drawable.eifd;
            case "EIFM": return R.drawable.eifm;
            case "ECPD": return R.drawable.ecpd;
            case "ECPM": return R.drawable.ecpm;
            case "ECFD": return R.drawable.ecfd;
            case "ECFM": return R.drawable.ecfm;
            default: return R.drawable.ic_profile_placeholder;
        }
    }

    private void setupPersonalityTags(String[] tags) {
        if (tags == null || tags.length == 0) return;

        // 기존 태그들 제거
        layoutPersonalityTagsRow1.removeAllViews();
        layoutPersonalityTagsRow2.removeAllViews();

        String tagColor = getSmbtiTagColor(smbtiResult);

        // 태그 추가 (첫 번째 행에 3개, 두 번째 행에 3개)
        for (int i = 0; i < tags.length; i++) {
            TextView tagView = createTagTextView(tags[i], tagColor);

            if (i < 3) {
                // 첫 번째 행
                tagView.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                if (i < 2) {
                    ((LinearLayout.LayoutParams) tagView.getLayoutParams()).setMarginEnd(
                            (int) getResources().getDimension(R.dimen.tag_margin));
                }

                layoutPersonalityTagsRow1.addView(tagView);
            } else {
                // 두 번째 행
                tagView.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                if (i < 5) {
                    ((LinearLayout.LayoutParams) tagView.getLayoutParams()).setMarginEnd(
                            (int) getResources().getDimension(R.dimen.tag_margin));
                }

                layoutPersonalityTagsRow2.addView(tagView);
            }
        }

        // 남은 공간을 채우기 위한 빈 뷰 추가
        int emptyViewsNeeded = 6 - tags.length;
        for (int i = 0; i < emptyViewsNeeded; i++) {
            View emptyView = new View(this);
            emptyView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            if (tags.length + i < 3) {
                layoutPersonalityTagsRow1.addView(emptyView);
            } else {
                layoutPersonalityTagsRow2.addView(emptyView);
            }
        }
    }

    private TextView createTagTextView(String text, String backgroundColor) {
        TextView tagView = new TextView(this);
        tagView.setText(text);
        tagView.setTextSize(12);
        tagView.setTextColor(getResources().getColor(android.R.color.white));
        
        // 동적으로 배경색 설정
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor(backgroundColor));
        drawable.setCornerRadius(16f);
        tagView.setBackground(drawable);
        
        tagView.setPadding(
                (int) getResources().getDimension(R.dimen.tag_padding_horizontal),
                (int) getResources().getDimension(R.dimen.tag_padding_vertical),
                (int) getResources().getDimension(R.dimen.tag_padding_horizontal),
                (int) getResources().getDimension(R.dimen.tag_padding_vertical));
        tagView.setGravity(android.view.Gravity.CENTER);
        return tagView;
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SmbtiResultActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        btnRetakeTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SmbtiResultActivity.this, SmbtiTestActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnFindStudyGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 스터디 그룹 찾기 기능 구현
                /*
                 * 현재 미구현 상태
                 */
            }
        });
    }

    // SMBTI 유형별 데이터 클래스
    private SmbtiData getSmbtiData(String smbtiType) {
        switch (smbtiType) {
            case "TIPD":
                return new SmbtiData(
                        "계획형 혼공러",
                        "TIPD 유형은 체계적이고 계획적인 학습자로, 개념과 구조를 먼저 파악한 후 세밀한 계획을 세워 자기주도적으로 학습합니다. 완벽을 추구하며 꼼꼼한 정리와 반복 점검을 통해 깊이 있는 이해를 추구하는 것을 선호합니다.",
                        "개념적 이해를 중요시하며 깊이 있는 탐구를 선호합니다.",
                        "독립적인 학습 환경에서 자신만의 방식으로 공부하는 것을 선호합니다.",
                        "세부 일정과 계획표를 만들어 실천하며 학습합니다.",
                        "학습 전 개념을 구조화해 정리하고, 주간·일간 계획표와 체크리스트로 계획적으로 학습을 진행한다.",
                        "실수한 문제는 따로 모아 오답 리스트를 만들고, 자가 퀴즈와 복습 루틴으로 반복 학습을 강화한다.",
                        "조용한 공간과 방해받지 않는 고정 시간대를 설정해 몰입할 수 있는 학습 환경을 스스로 조성한다.",
                        new String[]{"혼공러", "계획형", "정리중독", "완벽주의", "오답분석러", "계획형"}
                );

            case "TIPM":
                return new SmbtiData(
                        "전략 설계형 혼공러",
                        "TIPM 유형은 구조적이고 통찰력 있는 학습자로, 전체 흐름과 개념의 연결을 중시하며 큰 그림을 먼저 그리는 학습자입니다. 혼자 조용한 환경에서 주도적으로 학습하고, 유연한 계획 속에서 논리적으로 내용을 정리하는 것을 선호합니다.",
                        "공부를 시작하기 전 전체 흐름과 구조를 파악하고, 왜 배우는지를 이해한 뒤 학습에 들어갑니다.",
                        "혼자 조용한 환경에서 주도적으로 학습하며, 챕터별 흐름에 따라 구조화된 노트를 작성합니다.",
                        "계획은 목표 단위로 유연하게 설정하고, 큰 그림을 그린 후 그에 맞게 내용을 채워나가는 방식을 선호합니다.",
                        "책을 펴면 먼저 목차와 흐름을 파악하고, 전체 구조를 이해한 뒤 세부 내용을 채워가는 학습을 한다.",
                        "반복보다는 개념 간 연결과 흐름 중심의 정리를 중시하며, 단원별 목표 단위로 유연하게 계획을 세운다.",
                        "자유롭게 자료를 구성할 수 있는 환경에서 도식과 로드맵 등을 활용해 몰입도 높은 학습을 실천한다.",
                        new String[]{"혼공러", "전략설계자", "개념구조러", "로드맵중심", "큰그림중시", "계획적자유형"}
                );

            case "TIFD":
                return new SmbtiData(
                        "유연한 정리자",
                        "TIFD 유형은 개념과 논리 흐름을 중시하며 조용한 환경에서 혼자 몰입해 학습합니다. 학습 순서는 흥미와 컨디션에 따라 유연하게 조정하고, 예시나 세부 정보까지 꼼꼼히 챙기는 편입니다.",
                        "이론과 개념의 논리적 흐름을 중시하며, 세부 내용과 예시까지 꼼꼼히 기록합니다.",
                        "정해진 계획보다는 그날의 집중도나 흥미에 따라 학습 루트를 유연하게 조정합니다.",
                        "일정한 학습 시간은 없지만, 주변 간섭이 없을 때 깊게 몰입하며 높은 집중력을 발휘합니다.",
                        "유연한 계획 속에서 집중 가능한 시간대를 찾아 깊이 몰입하며 학습한다.",
                        "개념 정리를 우선으로 하고, 흐름 중심의 자유로운 노트에 핵심과 디테일을 함께 기록한다.",
                        "정해진 루틴보다는 주간 목표만 설정하고, 자신만의 요약과 예시 중심 복습으로 학습 내용을 다진다.",
                        new String[]{"자기주도학습", "디테일집착", "혼공자율형", "이론파자유러", "몰입러", "꼼꼼탐구자"}
                );

            case "TIFM":
                return new SmbtiData(
                        "창의적 설계자",
                        "TIFM 유형은 개념과 흐름을 중시하며, 조용한 환경에서 혼자 몰입해 유연하게 학습하는 스타일입니다. 전체 구조를 먼저 파악하고 논리적으로 정리하는 것을 선호합니다.",
                        "학습 전에 전체 구조와 개념의 흐름을 먼저 파악하며, 방향성과 맥락 중심의 이해를 선호합니다.",
                        "계획은 느슨하지만 학습 흐름은 전략적으로 잡고, 디테일보다는 큰 틀과 개념 연결을 중시합니다.",
                        "공부 순서나 챕터 이동에도 유연하게 대응하며, 깊이보다는 넓고 연결된 이해를 추구합니다.",
                        "유연한 계획 속에서도 개념 정리와 흐름 중심의 노트로 학습 내용을 체계화한다.",
                        "간섭 없는 환경에서 집중 가능한 시간대를 찾아 몰입하며, 사례 중심 복습과 키워드 정리로 이해를 강화한다.",
                        "하루하루는 자유롭게 운영하되, 주간 목표를 통해 학습의 흐름을 유지한다.",
                        new String[]{"구조중심", "혼공전략가", "자유계획러", "큰그림학습", "큰그림중시", "계획형자유형"}
                );

            case "TCPD":
                return new SmbtiData(
                        "조용한 전략형 실무자",
                        "TCPD 유형은 구조와 개념을 논리적으로 정리하며, 협력을 통해 동기를 얻고 계획적으로 실천하는 학습자입니다. 세부 내용과 실수까지 꼼꼼히 챙기는 성향이 강합니다.",
                        "역할을 분담해 협력하고, 목표 기반으로 팀 학습을 이끄는 유형입니다.",
                        "전체 개념을 구조화한 뒤 과제 전에 실행 계획과 작업 순서를 명확히 정리합니다.",
                        "사소한 개념도 놓치지 않고 실수를 복기하며, 팀의 진도와 완성도를 꼼꼼히 관리합니다.",
                        "팀원들과 목표를 설정하고 역할을 나눈 뒤, 계획에 따라 체계적으로 학습한다.",
                        "개념을 정리하고 실습 예시와 오답을 시각화해 복습 루틴을 구성하며, 실수와 개선점을 노트에 정리한다.",
                        "타인의 자료에 피드백을 주고 학습 보고서를 공유하며, 협업 속에서 실행력과 책임감을 발휘한다.",
                        new String[]{"이론실행러", "계획정리왕", "조용한리더", "실수제로", "팀내실무자", "개념+실행형"}
                );

            case "TCPM":
                return new SmbtiData(
                        "전략적 협업 리더",
                        "TCPM 유형은 개념과 구조를 바탕으로 학습하며, 협업을 통해 효율을 높이고 목표 중심의 계획을 세우는 학습자입니다. 전체 흐름과 핵심 연결을 중시하는 거시적 사고를 갖고 있습니다.",
                        "팀 프로젝트나 학습에서 전체 계획과 흐름을 설계하는 것을 즐깁니다.",
                        "학습 전 개념 분류와 구조를 도식화하며, 설계와 개념의 연계성 파악에 강합니다.",
                        "팀의 방향을 조율하며, 학습의 목적을 먼저 이해하려고 합니다.",
                        "학습을 시작하기 전에 전체 흐름과 개념 구조를 도식화해 큰 틀을 잡는다.",
                        "팀원과 역할을 나눈 뒤 학습 계획을 공유하고, 개념 분류와 연계 구조 중심으로 노트를 정리한다.",
                        "유사 개념을 연결하고 키워드로 복습하며, 중간 점검과 피드백 체크리스트로 학습 방향을 조율한다.",
                        new String[]{"협업설계자", "전체그림러", "계획전략가", "이론구조형", "로직마스터", "조용한리더"}
                );

            case "TCFD":
                return new SmbtiData(
                        "섬세한 실행형 조력가",
                        "TCFD 유형은 개념을 먼저 이해하고 논리적으로 접근하며, 사람들과 함께할 때 더 잘 배우는 협력형입니다. 일정에 유연하게 대응하고, 예시나 세부사항도 꼼꼼히 챙기는 성향을 가집니다.",
                        "편안한 분위기에서 사람들과 함께할 때 집중력이 높고, 일정 변화에 유연합니다.",
                        "개념을 먼저 파악한 뒤 예시를 꼼꼼히 챙기는 학습 스타일입니다.",
                        "팀플에서는 문서 정리와 세부 조율에 강합니다.",
                        "느슨한 계획 속에서도 마감 전엔 반드시 점검 루틴을 지키며 마무리를 탄탄히 한다.",
                        "팀에서는 자료 정리, 요약, 맞춤법 확인 등 세부 서포트 역할을 잘 수행한다.",
                        "사례 위주로 복습하고, 질문과 피드백을 통해 정확도를 높이며 자기 관리 루틴도 중요하게 여긴다.",
                        new String[]{"협업실행러", "유연계획성", "분위기메이커", "틀린거못참음", "정리의달인", "디테일조력자"}
                );

            case "TCFM":
                return new SmbtiData(
                        "자유로운 설계자",
                        "TCFM 유형은 개념과 흐름을 중시하며 논리적으로 사고하고, 협력을 통해 아이디어를 확장하며, 유연하게 계획을 조정하는 학습자입니다.",
                        "큰 그림을 중심으로 융통성 있게 학습을 진행하며, 계획보다는 흐름을 중시합니다.",
                        "팀 대화를 통해 개념을 확장하고, 구조를 도식화하거나 흐름을 정리하는 데 강합니다.",
                        "세부보다 핵심을 먼저 파악하고 설명에 능하며, 조율자 역할도 자연스럽게 수행합니다.",
                        "전체 개념과 흐름을 도식화하며 핵심 연결 구조 중심으로 학습한다.",
                        "틀에 얽매이지 않고 유연한 협업과 계획 조정을 지향하며, 자연스러운 팀워크 속에서 학습을 진행한다.",
                        "구조 정리 노트를 바탕으로 팀원과 개념을 나누고, 리뷰와 점검을 통해 완성도를 높인다.",
                        new String[]{"유연전략가", "팀플브레인", "전체흐름러", "구조설계자", "논리유도형", "분위기조율러"}
                );

            case "EIPD":
                return new SmbtiData(
                        "조용한 실행 전문가",
                        "EIPD 유형은 직접 해보며 배우는 경험 중심의 학습을 선호하며, 혼자서 집중하고 자기 페이스를 지킬 때 가장 편안함을 느낍니다. 계획을 세우고 루틴을 따를 때 학습 효율이 높으며, 작은 실수도 놓치지 않고 완성도를 중요하게 여깁니다.",
                        "실습과 문제풀이를 중심으로 학습하며, 조용한 환경에서 혼자 몰입하는 편입니다.",
                        "계획표를 학습의 틀로 여기고 성실하게 루틴을 지킵니다.",
                        "오답노트와 점검표를 활용해 실수를 줄이고, 실행–피드백–보완의 과정을 반복합니다.",
                        "주간 계획과 일일 체크리스트를 통해 루틴을 유지하며, 실습 중심의 반복 학습으로 개념을 다진다.",
                        "틀린 문제는 원인을 분석해 유형별로 정리하고, 퀴즈나 셀프 테스트를 통해 피드백 루틴을 지속한다.",
                        "정돈된 학습 환경을 유지하고, 디테일 체크리스트를 활용해 주제별 성취도를 점검한다.",
                        new String[]{"몰입장인", "실전혼공러", "오답분석러", "루틴성애자", "디테일실행러","홀로학습형"}
                );

            case "EIPM":
                return new SmbtiData(
                        "설계하는 실천형 전략가",
                        "EIPM 유형은 이론보다는 직접 해보며 익히는 것을 선호하고, 조용한 환경에서 혼자 몰입하는 자기 주도적 학습자입니다. 학습 전 큰 틀의 목표와 계획을 세우고 이를 실천하며, 전체 구조와 개념 간의 흐름과 연결성을 중시합니다.",
                        "직접 부딪혀보며 감을 익힌 뒤 개념을 정리하고, 실수를 통해 흐름을 이해하며 보완합니다.",
                        "학습 전 전체 구조를 계획하고 단계별로 실행하는 루틴을 따릅니다.",
                        "노트 정리는 개별 개념보다 전체 흐름과 연결성에 중점을 둡니다.",
                        "주제별 실습 계획을 세우고 전체 흐름을 도식화한 뒤, 실행 결과를 기반으로 복습하고 보완한다.",
                        "실습 후 복기와 재설계를 반복하고, 오답보다 느낀 점을 중심으로 복습한다.",
                        "학습 내용을 주간 단위로 점검하고, 노트 정리는 개념 흐름과 전체 구조에 맞춰 정리한다.",
                        new String[]{"혼공전략가", "실전계획러", "자기주도러", "몸으로익히기", "흐름중심학습", "실행+설계형"}
                );

            case "EIFD":
                return new SmbtiData(
                        "감각적 디테일러",
                        "EIFD 유형은 혼자 조용히 몰입하며 직접 해보며 배우는 실전형 학습자입니다. 계획보다 흐름에 따라 유연하게 움직이고, 세부적인 오류나 디테일에 민감합니다.",
                        "이론 설명보다 직접 문제를 풀고 실습할 때 가장 효과적으로 학습합니다.",
                        "혼자 있는 공간에서 몰입도가 높으며, 계획은 유연하지만 실행 과정의 디테일을 중시합니다.",
                        "헷갈린 부분은 따로 메모해 복기하고, 한 번 시작한 학습은 끝까지 완성하려는 의지가 강합니다.",
                        "학습은 자율적으로 시작하되 끝까지 완성하는 루틴을 습관화한다.",
                        "실습 후 헷갈린 부분을 정리 노트에 피드백하며, 손으로 필기해 감각적으로 익힌 내용을 정리한다.",
                        "방해 없는 환경을 조성하고, 짧은 목표 단위로 몰입하며 디테일한 복습을 반복한다.",
                        new String[]{"디테일파", "자율몰입러", "손으로배우는타입", "즉흥집중형", "오답메모장인", "혼공실습러"}
                );

            case "EIFM":
                return new SmbtiData(
                        "탐구하는 창의적 자유인",
                        "EIFM 유형은 실습을 통해 배우고, 혼자 조용히 몰입하며 학습합니다. 계획보다는 흐름에 따라 유연하게 움직이고, 세부보다는 전체 흐름과 핵심 개념에 집중합니다.",
                        "이론보다 직접 문제를 풀거나 실습하며 이해하는 것을 선호합니다.",
                        "혼자만의 시간에 몰입해 사고를 확장하며, 호기심이 가는 부분부터 탐색하는 경우가 많습니다.",
                        "전체 흐름과 개념 연결을 중시하며, 맥락 중심의 요약 정리를 선호합니다.",
                        "흥미 있는 파트부터 자유롭게 탐색하며 개념 간 연결 구조를 파악한다.",
                        "실습과 함께 흐름을 정리하고, 떠오른 아이디어는 즉시 기록해 노트로 정리한다.",
                        "목표 리스트 기반으로 자율적으로 학습을 계획하고, 복습은 반복보다 구조와 맥락 이해에 집중한다.",
                        new String[]{"자기주도탐색형", "개념확장러", "실전감각자", "혼자파고드는", "호기심기반", "마인드맵장인"}
                );

            case "ECPD":
                return new SmbtiData(
                        "팀 속의 실행 담당자",
                        "ECPD 유형은 직접 해보며 배우고, 팀 내 협력과 역할 분담을 통해 학습 효율을 높입니다. 계획에 따라 루틴을 성실히 지키며, 작은 실수도 놓치지 않고 디테일을 꼼꼼하게 관리합니다.",
                        "말보다 행동이 빠르고, 직접 해보며 배우는 것을 선호합니다.",
                        "팀 내에서 책임감 있게 역할을 수행하고, 계획에 맞춰 일정을 성실히 소화합니다.",
                        "실수와 오류를 꼼꼼히 기록하고 복습하며, 디테일을 중시해 완성도 높은 결과물을 만듭니다.",
                        "팀 내에서 실습, 문서, 디테일 담당 역할을 맡아 체크리스트로 일정을 관리한다.",
                        "실습 후 오류를 메모하고 정리하여 재실습하며, 강의 내용을 표나 도식으로 시각화한다.",
                        "과제 제출 전 리뷰표를 활용해 완성도를 점검하고, 팀원들과 계획과 진행 상황을 주기적으로 공유한다.",
                        new String[]{"실전계획러", "팀워크실행자", "체크리스트장인", "디테일담당", "실습리더", "완성도집착러"}
                );

            case "ECPM":
                return new SmbtiData(
                        "실천형 설계자",
                        "ECPM 유형은 직접 해보며 배우고, 팀워크와 소통을 통해 몰입도가 높아집니다. 전체 일정과 목표를 세운 뒤 실행하며, 세부보다는 흐름과 구조 같은 큰 맥락에 집중합니다.",
                        "실습과 행동을 통해 배우며, 경험을 바탕으로 전체 흐름을 파악하는 유형입니다.",
                        "팀 내에서 전략과 계획 수립에 강하고, 개념 연결과 핵심 구조에 집중합니다.",
                        "의견 조율과 진도 체크에 책임감을 가지고, 정해진 계획을 바탕으로 유연하게 팀을 이끕니다.",
                        "실습 전 로드맵을 그려 전체 흐름을 설계하고, 실행 후 구조를 재정비한다.",
                        "팀원과 역할을 나누고 흐름을 점검하며, 방향을 유연하게 조정한다.",
                        "개념 간 관계를 구조화하고, 흐름 중심의 요약 복습과 실행 후 점검 루틴을 병행한다.",
                        new String[]{"팀워크설계자", "흐름조율자", "전략형실습러", "전체구조파악러", "협력실행브레인", "말보다행동"}
                );

            case "ECFD":
                return new SmbtiData(
                        "섬세한 협업형 실천가",
                        "ECFD 유형은 직접 해보며 배우고, 협력 속에서 집중력과 동기부여가 높아집니다. 자연스러운 흐름을 따르며 학습하고, 작은 오류도 놓치지 않는 꼼꼼한 성향입니다.",
                        "팀 내에서 자유로운 분위기 속에서 함께 실습하며 배우는 방식에 익숙합니다.",
                        "계획보다는 유연하게 역할을 나누고 흐름에 따라 실행하며, 작은 실수나 누락에도 민감하게 반응합니다.",
                        "팀원에게 친절하게 협력하되, 완성도와 정확도를 놓치지 않고 실습 오류를 꼼꼼히 복습합니다.",
                        "유연하게 역할을 분담해 실습하며 진행하고, 중간 점검은 디테일 중심으로 수행한다.",
                        "틀리거나 애매한 부분은 따로 정리해 재확인하고, 체크리스트를 활용해 결과물을 복습한다.",
                        "오탈자 수정과 문장 정리에 강하며, 실습 중 떠오른 포인트를 바로 기록하고 팀원과 피드백을 주고받는다.",
                        new String[]{"협업실행러", "디테일마스터", "무계획획완벽주의자", "분위기조율러", "친절한피드백러", "실습수정러"}
                );

            case "ECFM":
                return new SmbtiData(
                        "여유로운 구조 설계자",
                        "ECFM 유형은 직접 부딪히며 실습을 통해 이해를 넓히고, 협업을 통해 흥미와 몰입도를 높이는 학습자입니다. 정해진 루틴보다는 상황에 따라 유연하게 학습을 조정하며, 전체 흐름과 개념 간의 연결, 맥락 파악에 집중합니다.",
                        "실습을 통해 익히고, 팀원과의 상호작용을 통해 자연스럽게 이해를 확장합니다.",
                        "팀 프로젝트에서 흐름을 조율하며, 개념의 연결을 통해 전체 구조를 완성합니다.",
                        "감각적인 흐름을 따라 학습하며, 디테일보다는 전략과 구조 중심의 정리를 선호합니다.",
                        "실습을 통해 개념을 연결하고 구조를 도식화하며 흐름 중심으로 학습한다.",
                        "팀에서는 과도한 계획보다 느슨한 타임라인과 유연한 역할 조정을 통해 흐름을 조율한다.",
                        "시각적 정리와 자유로운 피드백 대화를 활용하고, 마감 전 체크리스트로 핵심을 점검한다.",
                        new String[]{"유연조율자", "감각적실습러", "팀속브레인", "계획보다흐름", "개념연결러", "내추럴학습리더"}
                );

            default:
                return new SmbtiData(
                        "고유한 학습 스타일",
                        "각자만의 고유한 학습 방식과 특성을 가지고 있습니다.",
                        "학습 스타일 1",
                        "학습 스타일 2",
                        "학습 스타일 3",
                        "유형별 추천 학습법 1",
                        "유형별 추천 학습법 2",
                        "유형별 추천 학습법 3",
                        new String[]{"태그1", "태그2", "태그3", "태그4", "태그5", "태그6"}
                );

        }
    }

    // SMBTI 데이터를 담는 내부 클래스
    private static class SmbtiData {
        String description;
        String personalityDescription;
        String learningStyle1;
        String learningStyle2;
        String learningStyle3;
        String studyMethod1;
        String studyMethod2;
        String studyMethod3;
        String[] personalityTags;

        SmbtiData(String description, String personalityDescription,
                  String learningStyle1, String learningStyle2, String learningStyle3,
                  String studyMethod1, String studyMethod2, String studyMethod3,
                  String[] personalityTags) {
            this.description = description;
            this.personalityDescription = personalityDescription;
            this.learningStyle1 = learningStyle1;
            this.learningStyle2 = learningStyle2;
            this.learningStyle3 = learningStyle3;
            this.studyMethod1 = studyMethod1;
            this.studyMethod2 = studyMethod2;
            this.studyMethod3 = studyMethod3;
            this.personalityTags = personalityTags;
        }
    }
}
