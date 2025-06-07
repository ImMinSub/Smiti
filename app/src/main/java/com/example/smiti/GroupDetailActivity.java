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
import com.example.smiti.model.JoinGroupRequest; // JoinGroupRequest 추가

import java.util.Random; // 랜덤 이미지 생성을 위해 추가
import retrofit2.Call; // Retrofit 추가
import retrofit2.Callback; // Retrofit 추가
import retrofit2.Response; // Retrofit 추가
import com.example.smiti.api.ApiService; // ApiService 추가
import com.example.smiti.api.RetrofitClient; // RetrofitClient 추가
import android.content.SharedPreferences; // SharedPreferences 추가
import android.content.Context; // Context 추가
import android.util.Log; // Log 추가
import com.example.smiti.api.ApiResponse; // ApiResponse 추가
import java.util.List; // List 추가
import java.util.Map;
import java.util.HashMap;

public class GroupDetailActivity extends AppCompatActivity {

    private static final String TAG = "GroupDetailActivity";

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
    private Button btnLeaveGroupDetail;       // 그룹 탈퇴 버튼 (ID: btn_group_detail_leave)
    private ImageButton btnBack;              // 뒤로가기 버튼 (ID: btn_group_detail_back)
    private CollapsingToolbarLayout collapsingToolbarLayout; // 스크롤 시 제목 변경용 (ID: collapsing_toolbar_group_detail)

    private Group currentGroup;
    private boolean isAiMode;
    private String groupNameFromIntent; // Intent에서 받은 그룹 이름을 저장할 변수 추가
    private String currentUserEmail; // 현재 사용자 이메일 저장 변수 추가
    
    // 데이터 로딩 상태 관리
    private boolean isGroupDetailsLoaded = false;
    private boolean isMembershipChecked = false;
    private boolean isMemberOfGroup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail); // 수정된 레이아웃 파일 사용

        // UI 요소 초기화 (새로운 ID로)
        ivGroupHeaderImage = findViewById(R.id.iv_group_detail_header_image);
        ivGroupProfileIcon = findViewById(R.id.iv_group_profile_icon);
        tvGroupNameDetail = findViewById(R.id.tv_group_detail_name);
        tvGroupScoreDetail = findViewById(R.id.tv_group_detail_score); // 레이아웃에 해당 ID가 있어야 함
        tvGroupDescriptionDetail = findViewById(R.id.tv_group_detail_description);
        tvMemberInfoDetail = findViewById(R.id.tv_member_count); // 레이아웃에 해당 ID가 있어야 함
        btnJoinGroupDetail = findViewById(R.id.btn_group_detail_join);
        btnLeaveGroupDetail = findViewById(R.id.btn_group_detail_leave);
        btnBack = findViewById(R.id.btn_group_detail_back);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar_group_detail);

        // 상단 헤더 이미지 랜덤 설정
        int[] headerImages = {R.drawable.image1, R.drawable.image2, R.drawable.image3};
        if (headerImages.length > 0) { // 이미지 배열이 비어있지 않은 경우에만
            Random random = new Random();
            ivGroupHeaderImage.setImageResource(headerImages[random.nextInt(headerImages.length)]);
        } else {
            // 기본 이미지 설정 또는 오류 처리
            // ivGroupHeaderImage.setImageResource(R.drawable.default_group_image_placeholder);
        }

        // 뒤로가기 버튼 클릭 리스너 설정
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed(); // 안드로이드 시스템의 기본 뒤로가기 동작 호출
            }
        });

        // Intent로부터 데이터 수신
        Intent intent = getIntent();
        if (intent != null) {
            // 두 가지 방식 지원: 1) EXTRA_GROUP으로 Group 객체 전달, 2) 개별 필드들 전달
            currentGroup = (Group) intent.getSerializableExtra(EXTRA_GROUP);
            isAiMode = intent.getBooleanExtra(EXTRA_IS_AI_MODE, false);

            String groupId = null;
            String groupName = null;
            String groupDescription = null;
            int maxMembers = 0;
            int currentMembers = 0;

            if (currentGroup != null) {
                // Group 객체가 전달된 경우
                groupId = currentGroup.getId();
                groupName = currentGroup.getName();
                groupDescription = currentGroup.getDescription();
                maxMembers = currentGroup.getMax_members();
                currentMembers = currentGroup.getCurrent_members();
            } else {
                // 개별 필드들이 전달된 경우 (HomeDashboardActivity에서 오는 경우)
                groupId = intent.getStringExtra("groupId");
                groupName = intent.getStringExtra("groupName");
                groupDescription = intent.getStringExtra("groupDescription");
                maxMembers = intent.getIntExtra("maxMembers", 0);
                currentMembers = intent.getIntExtra("currentMembers", 0);

                // 임시 Group 객체 생성 (API 호출 전까지 사용)
                if (groupId != null && groupName != null) {
                    currentGroup = new Group(groupId, groupName, groupDescription != null ? groupDescription : "", 
                                           currentMembers, "", 0.0, maxMembers, currentMembers);
                }
            }

            if (groupId == null || groupId.isEmpty()) {
                Toast.makeText(this, "그룹 ID 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // SharedPreferences에서 사용자 이메일 가져오기
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
            currentUserEmail = sharedPreferences.getString("email", null); // 멤버 변수에 저장

            if (currentUserEmail == null || currentUserEmail.isEmpty()) {
                 Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
                 // 로그인 액티비티로 이동하거나 다른 처리 필요
                 finish();
                 return;
            }

            // 로딩 화면 표시 (모든 UI 컴포넌트 숨김)
            showLoadingState();
            
            // 두 API를 동시에 호출하여 모든 데이터 로드 후 완성된 화면 표시
            fetchGroupDetailsAndMembership(groupId, isAiMode, currentUserEmail);

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
                    // 1. SharedPreferences에서 사용자 이메일 가져오기
                    SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                    String userEmail = sharedPreferences.getString("email", null);

                    if (userEmail == null || userEmail.isEmpty()) {
                        Toast.makeText(GroupDetailActivity.this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
                        // 로그인 액티비티로 이동하거나 다른 처리 필요
                        return;
                    }

                    // 2. 그룹 ID 가져오기
                    String groupIdString = currentGroup.getId(); // Group model에 getId() 메소드가 있다고 가정

                    if (groupIdString == null || groupIdString.isEmpty()) {
                         Toast.makeText(GroupDetailActivity.this, "그룹 ID 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                         return;
                    }

                    int groupId;
                    try {
                        groupId = Integer.parseInt(groupIdString);
                    } catch (NumberFormatException e) {
                        Log.e("GroupDetailActivity", "그룹 ID 형변환 오류: " + groupIdString, e);
                        Toast.makeText(GroupDetailActivity.this, "잘못된 그룹 정보입니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 3. Retrofit을 사용하여 서버 API 호출
                    ApiService apiService = RetrofitClient.getApiService();
                    JoinGroupRequest joinRequest = new JoinGroupRequest(userEmail, groupId, currentGroup.getName()); // currentGroup에서 그룹 이름 가져오기
                    Call<Void> call = apiService.joinGroup(joinRequest); // 메소드 이름 및 인자 수정

                    call.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                // 가입 성공 처리
                                Toast.makeText(GroupDetailActivity.this,
                                        currentGroup.getName() + " 그룹에 성공적으로 가입되었습니다.",
                                        Toast.LENGTH_SHORT).show();
                                // 가입 성공 후 버튼 상태 변경
                                runOnUiThread(() -> {
                                    btnJoinGroupDetail.setVisibility(View.GONE);
                                    btnLeaveGroupDetail.setVisibility(View.VISIBLE);
                                    isMemberOfGroup = true; // 상태 업데이트
                                });
                            } else {
                                // 가입 실패 처리 (HTTP 오류 코드 확인)
                                Log.e("GroupDetailActivity", "그룹 가입 실패: " + response.code());
                                Toast.makeText(GroupDetailActivity.this,
                                        "이미 그룹에 가입되어 있습니다. 오류 코드: " + response.code(),
                                        Toast.LENGTH_SHORT).show();
                                // 예: 이미 가입된 경우, 그룹이 꽉 찬 경우 등 추가 오류 처리
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            // 네트워크 오류 등
                            Log.e("GroupDetailActivity", "그룹 가입 네트워크 오류", t);
                            Toast.makeText(GroupDetailActivity.this, "그룹 가입 중 네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    Toast.makeText(GroupDetailActivity.this, "그룹 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // TODO: 그룹 탈퇴 버튼 클릭 리스너 설정
        btnLeaveGroupDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentGroup != null) {
                    leaveGroup(); // 그룹 탈퇴 로직 호출
                } else {
                    Toast.makeText(GroupDetailActivity.this, "그룹 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 그룹 탈퇴 로직 구현
    private void leaveGroup() {
        if (currentGroup == null || currentGroup.getId() == null) {
            showToast("그룹 정보를 찾을 수 없습니다.");
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("email", null);

        if (userEmail == null || userEmail.isEmpty()) {
            showToast("로그인 정보가 없습니다. 다시 로그인해주세요.");
            // 로그인 액티비티로 이동하거나 다른 처리 필요
            return;
        }

        // 그룹 ID를 int로 변환 (API 명세에 따라)
        int groupId;
        try {
            groupId = Integer.parseInt(currentGroup.getId());
        } catch (NumberFormatException e) {
            Log.e(TAG, "그룹 ID 형변환 오류 (탈퇴): " + currentGroup.getId(), e);
            showToast("잘못된 그룹 정보입니다.");
            return;
        }

        // 요청 본문 생성
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", userEmail);

        ApiService apiService = RetrofitClient.getApiService();
        Call<ApiResponse> call = apiService.deleteGroupUser(groupId, requestBody);

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) { // Check for HTTP 200 range status
                    // Consider success if response body is null (for DELETE) or message contains "success"
                    boolean isSuccess = false;
                    if (response.body() == null) {
                        isSuccess = true; // Null body for DELETE can indicate success
                    } else {
                        ApiResponse apiResponse = response.body();
                        if (apiResponse.getMessage() != null && apiResponse.getMessage().toLowerCase().contains("success")) {
                            isSuccess = true;
                        } else if ("success".equalsIgnoreCase(apiResponse.getStatus())) { // Also check the status field for robustness
                            isSuccess = true;
                        }
                    }

                    if (isSuccess) {
                        // 탈퇴 성공
                        showToast(currentGroup.getName() + " 그룹에서 탈퇴했습니다.");
                        // TODO: UI 갱신 또는 다른 화면으로 이동
                        // 예: 현재 액티비티 종료하고 이전 화면으로 돌아가기
                        setResult(RESULT_OK); // Set result for the calling activity
                        finish();
                    } else {
                        // 탈퇴 실패 (서버 메시지 확인)
                        // If isSuccess is false, but response is successful, use the message if available
                        String errorMessage = (response.body() != null && response.body().getMessage() != null)
                                ? response.body().getMessage()
                                : "알 수 없는 오류가 발생했습니다.";
                        Log.e(TAG, "그룹 탈퇴 실패: " + errorMessage);
                        showToast("그룹 탈퇴 실패: " + errorMessage);
                    }
                } else {
                    // 응답 실패 (HTTP 오류 코드)
                    Log.e(TAG, "그룹 탈퇴 응답 오류: " + response.code());
                    showToast("그룹 탈퇴 실패. 오류 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // 네트워크 오류
                Log.e(TAG, "그룹 탈퇴 API 호출 실패", t);
                showToast("그룹 탈퇴 중 네트워크 오류가 발생했습니다.");
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // 로딩 상태 표시 (모든 UI 요소 숨김, 시스템 기본 로딩만 표시)
    private void showLoadingState() {
        // 모든 주요 UI 컴포넌트 숨김
        tvGroupNameDetail.setVisibility(View.GONE);
        tvGroupDescriptionDetail.setVisibility(View.GONE);
        tvMemberInfoDetail.setVisibility(View.GONE);
        if (tvGroupScoreDetail != null) {
            tvGroupScoreDetail.setVisibility(View.GONE);
        }
        btnJoinGroupDetail.setVisibility(View.GONE);
        btnLeaveGroupDetail.setVisibility(View.GONE);
        ivGroupProfileIcon.setVisibility(View.GONE);
        
        // 시스템의 기본 로딩 애니메이션이 자동으로 표시됨
        // (안드로이드에서 네트워크 작업 중 자동으로 표시되는 로딩 스피너)
    }

    // 완성된 그룹 정보를 표시하는 메서드 (모든 데이터가 준비된 후 호출)
    private void populateCompleteGroupInfo(String groupName, String groupDescription, int maxMembers, int currentMembers) {
        if (collapsingToolbarLayout != null) {
            collapsingToolbarLayout.setTitle(groupName);
        }

        tvGroupNameDetail.setText(groupName);

        // 그룹 프로필 아이콘 설정
        if (ivGroupProfileIcon != null) {
            ivGroupProfileIcon.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // 그룹 설명 설정
        if (groupDescription != null && !groupDescription.isEmpty()) {
            tvGroupDescriptionDetail.setText(groupDescription);
        } else {
            tvGroupDescriptionDetail.setText("등록된 그룹 설명이 없습니다.");
        }

        // 멤버 정보 설정
        if (tvMemberInfoDetail != null) {
            if (maxMembers > 0) {
                tvMemberInfoDetail.setText("참여 인원: " + currentMembers + " / " + maxMembers + "명");
            } else if (currentMembers > 0) {
                tvMemberInfoDetail.setText("현재 인원: " + currentMembers + "명");
            } else {
                tvMemberInfoDetail.setText("인원 정보 없음");
            }
        }
    }

    // 두 API를 동시에 호출하는 메서드
    private void fetchGroupDetailsAndMembership(String groupId, boolean isAiMode, String userEmail) {
        // 두 API를 동시에 호출
        fetchGroupDetailsOnly(groupId, isAiMode);
        checkUserMembershipOnly(groupId, userEmail);
    }

    // 그룹 상세 정보만 로드하는 메서드
    private void fetchGroupDetailsOnly(String groupId, boolean isAiMode) {
        ApiService apiService = RetrofitClient.getApiService();
        int id;
        try {
            id = Integer.parseInt(groupId);
        } catch (NumberFormatException e) {
            Log.e(TAG, "그룹 ID 형변환 오류: " + groupId, e);
            showErrorState("잘못된 그룹 정보입니다.");
            return;
        }

        apiService.getGroupDetail(id, currentUserEmail).enqueue(new Callback<Group>() {
            @Override
            public void onResponse(Call<Group> call, Response<Group> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentGroup = response.body();
                    isGroupDetailsLoaded = true;
                    
                    // 모든 데이터가 로드되었는지 확인
                    if (isMembershipChecked) {
                        updateUIWithAllData();
                    }
                } else {
                    Log.e(TAG, "그룹 상세 정보 로드 실패: " + response.code());
                    showErrorState("그룹 정보를 불러오는데 실패했습니다.");
                }
            }

            @Override
            public void onFailure(Call<Group> call, Throwable t) {
                Log.e(TAG, "그룹 상세 정보 API 호출 실패", t);
                showErrorState("네트워크 오류가 발생했습니다.");
            }
        });
    }

    // 멤버십만 확인하는 메서드
    private void checkUserMembershipOnly(String groupId, String userEmail) {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getMyGroups(userEmail).enqueue(new Callback<List<Group>>() {
            @Override
            public void onResponse(Call<List<Group>> call, Response<List<Group>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Group> userGroups = response.body();
                    isMemberOfGroup = false;
                    for (Group group : userGroups) {
                        if (group.getId().equals(groupId)) {
                            isMemberOfGroup = true;
                            break;
                        }
                    }
                    isMembershipChecked = true;
                    
                    // 모든 데이터가 로드되었는지 확인
                    if (isGroupDetailsLoaded) {
                        updateUIWithAllData();
                    }
                } else {
                    Log.e(TAG, "사용자 그룹 목록 조회 실패: " + response.code());
                    isMembershipChecked = true;
                    isMemberOfGroup = false; // 실패 시 기본값으로 비회원 처리
                    
                    if (isGroupDetailsLoaded) {
                        updateUIWithAllData();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Group>> call, Throwable t) {
                Log.e(TAG, "사용자 그룹 목록 API 호출 실패", t);
                isMembershipChecked = true;
                isMemberOfGroup = false; // 실패 시 기본값으로 비회원 처리
                
                if (isGroupDetailsLoaded) {
                    updateUIWithAllData();
                }
            }
        });
    }

    // 모든 데이터 로드 후 완성된 화면을 한 번에 표시하는 메서드
    private void updateUIWithAllData() {
        runOnUiThread(() -> {
            if (currentGroup != null) {
                // 1. 모든 UI 컴포넌트를 보이도록 설정
                tvGroupNameDetail.setVisibility(View.VISIBLE);
                tvGroupDescriptionDetail.setVisibility(View.VISIBLE);
                tvMemberInfoDetail.setVisibility(View.VISIBLE);
                ivGroupProfileIcon.setVisibility(View.VISIBLE);
                
                // 2. 완성된 그룹 정보 표시 (API에서 받은 최신 정보로)
                populateCompleteGroupInfo(
                    currentGroup.getName(),
                    currentGroup.getDescription(),
                    currentGroup.getMax_members(),
                    currentGroup.getCurrent_members()
                );
                
                // 3. 궁합 점수 표시
                if (tvGroupScoreDetail != null) {
                    int roundedScore = (int) Math.round(currentGroup.getMbtiScore());
                    if (isAiMode) {
                        tvGroupScoreDetail.setText("AI 추천: " + roundedScore + "점");
                    } else {
                        tvGroupScoreDetail.setText("궁합: " + roundedScore + "점");
                    }
                    
                    if (roundedScore >= 0) {
                        tvGroupScoreDetail.setVisibility(View.VISIBLE);
                    } else {
                        tvGroupScoreDetail.setVisibility(View.GONE);
                    }
                }

                // 4. 올바른 버튼 상태 설정
                if (isMemberOfGroup) {
                    btnJoinGroupDetail.setVisibility(View.GONE);
                    btnLeaveGroupDetail.setVisibility(View.VISIBLE);
                } else {
                    btnJoinGroupDetail.setVisibility(View.VISIBLE);
                    btnLeaveGroupDetail.setVisibility(View.GONE);
                    btnJoinGroupDetail.setEnabled(true);
                }
                
                // 5. 완성된 화면이 표시됨 - 사용자는 모든 정보를 한 번에 볼 수 있음
            }
        });
    }

    // 오류 상태 표시 메서드
    private void showErrorState(String errorMessage) {
        runOnUiThread(() -> {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            
            // 오류 발생 시 화면을 닫고 이전 화면으로 돌아감
            finish();
        });
    }
}
