package com.example.smiti;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.CollapsingToolbarLayout; // 제목 설정용

import com.example.smiti.model.Group;


import java.util.Random; // 랜덤 이미지 생성을 위해 추가


public class GroupSearchResultActivity extends AppCompatActivity {


    public static final String EXTRA_GROUP = "extra_group";
    public static final String EXTRA_IS_AI_MODE = "extra_is_ai_mode";

    // UI 요소 멤버 변수 (새로운 ID에 맞게)
    private ImageView ivGroupHeaderImage;     // 상단 헤더 이미지 (ID: iv_group_detail_header_image)
    private ImageView ivGroupProfileIcon;     // 그룹 프로필 아이콘 (ID: iv_group_profile_icon)
    private TextView tvGroupNameDetail;       // 그룹 이름 (ID: tv_group_detail_name)
    private TextView tvGroupScoreDetail;      // 그룹 점수 (ID: tv_group_detail_score)
    private TextView tvGroupDescriptionDetail;// 그룹 설명 (ID: tv_group_detail_description)
    private TextView tvMemberInfoDetail;      // 멤버 정보 (ID: tv_member_count)
    private Button btnJoinGroupDetail;        // 그룹 참가 버튼 (ID: btn_group_detail_join)
    private ImageButton btnBack;              // 뒤로가기 버튼 (ID: btn_group_detail_back)
    private CollapsingToolbarLayout collapsingToolbarLayout; // 스크롤 시 제목 변경용 (ID: collapsing_toolbar_group_detail)

    private Group currentGroup;
    private boolean isAiMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 이 액티비티가 사용하는 레이아웃 파일 (GroupDetailActivity와 동일한 파일을 사용한다고 가정)
        setContentView(R.layout.activity_group_detail);

        // UI 요소 초기화 (새로운 ID로)
        ivGroupHeaderImage = findViewById(R.id.iv_group_detail_header_image);
        ivGroupProfileIcon = findViewById(R.id.iv_group_profile_icon);
        tvGroupNameDetail = findViewById(R.id.tv_group_detail_name);
        tvGroupScoreDetail = findViewById(R.id.tv_group_detail_score); // 레이아웃에 해당 ID가 있어야 함
        tvGroupDescriptionDetail = findViewById(R.id.tv_group_detail_description);
        tvMemberInfoDetail = findViewById(R.id.tv_member_count); // 레이아웃에 해당 ID가 있어야 함
        btnJoinGroupDetail = findViewById(R.id.btn_group_detail_join);
        btnBack = findViewById(R.id.btn_group_detail_back);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar_group_detail);

        // 상단 헤더 이미지 랜덤 설정
        int[] headerImages = {R.drawable.image1, R.drawable.image2, R.drawable.image3};
        if (headerImages.length > 0) { // 이미지 배열이 비어있지 않은 경우에만
            Random random = new Random();
            ivGroupHeaderImage.setImageResource(headerImages[random.nextInt(headerImages.length)]);
        } else {

        }

        // 뒤로가기 버튼 클릭 리스너 설정
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed(); // 안드로이드 시스템의 기본 뒤로가기 동작 호출
            }
        });


        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_GROUP)) {
            currentGroup = (Group) intent.getSerializableExtra(EXTRA_GROUP);
            isAiMode = intent.getBooleanExtra(EXTRA_IS_AI_MODE, false);

            if (currentGroup != null) {
                populateGroupDetails();
            } else {
                Toast.makeText(this, "그룹 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish(); // 오류 시 액티비티 종료
            }
        } else {
            Toast.makeText(this, "잘못된 접근입니다. 그룹 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish(); // 오류 시 액티비티 종료
        }

        // 그룹 참가 버튼 클릭 리스너 설정
        btnJoinGroupDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentGroup != null) {
                    // TODO: 실제 그룹 가입 로직 구현 (API 호출 등)
                    Toast.makeText(GroupSearchResultActivity.this, // 클래스명 변경
                            currentGroup.getName() + " 그룹에 가입 요청을 보냈습니다.",
                            Toast.LENGTH_SHORT).show();
                    // 예: callJoinApi(currentGroup.getId());
                }
            }
        });
    }

    private void populateGroupDetails() {
        if (currentGroup == null) return;

        // CollapsingToolbarLayout에 제목 설정 (스크롤 시 상단 바에 표시될 제목)
        if (collapsingToolbarLayout != null) {
            collapsingToolbarLayout.setTitle(currentGroup.getName());
        }
        // 또는 화면 내의 TextView에 그룹 이름 설정 (프로필 아이콘 옆)
        tvGroupNameDetail.setText(currentGroup.getName());


        // 그룹 프로필 아이콘 설정 (실제 그룹 프로필 이미지가 있다면 Glide 등으로 로드)
        // 여기서는 기본 플레이스홀더 이미지 설정 예시
        if (ivGroupProfileIcon != null) {

            ivGroupProfileIcon.setImageResource(R.drawable.ic_profile_placeholder); // 기본 이미지 리소스 필요
        }


        // 그룹 설명 설정
        if (currentGroup.hasDescription()) {
            tvGroupDescriptionDetail.setText(currentGroup.getDescription());
            tvGroupDescriptionDetail.setVisibility(View.VISIBLE);
        } else {
            tvGroupDescriptionDetail.setText("등록된 그룹 설명이 없습니다.");
            // tvGroupDescriptionDetail.setVisibility(View.GONE); // 또는 메시지 유지
        }

        // 그룹 점수 설정
        int roundedScore = currentGroup.getMbtiScoreAsInt();
        if (tvGroupScoreDetail != null) { // 레이아웃에 해당 TextView가 있는지 확인
            if (isAiMode) {
                tvGroupScoreDetail.setText("AI 추천: " + roundedScore + "점");
            } else {
                tvGroupScoreDetail.setText("궁합: " + roundedScore + "점");
            }

        }

        // 멤버 정보 설정
        if (tvMemberInfoDetail != null) { // 레이아웃에 해당 TextView가 있는지 확인
            if (currentGroup.getMax_members() > 0) {
                tvMemberInfoDetail.setText("참여 인원: " + currentGroup.getCurrent_members() + " / " + currentGroup.getMax_members() + "명");
                // tvMemberInfoDetail.setVisibility(View.VISIBLE); // 필요시 보이도록 설정
            } else if (currentGroup.getCurrent_members() > 0) {
                tvMemberInfoDetail.setText("현재 인원: " + currentGroup.getCurrent_members() + "명");
                // tvMemberInfoDetail.setVisibility(View.VISIBLE);
            } else { // 정보가 없으면 숨김
                tvMemberInfoDetail.setVisibility(View.GONE);
            }
        }
    }


}
