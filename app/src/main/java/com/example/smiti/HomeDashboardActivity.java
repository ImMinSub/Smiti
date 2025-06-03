package com.example.smiti;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.util.Log;
import com.example.smiti.api.ApiService;
import com.example.smiti.api.RetrofitClient;
import com.example.smiti.model.Group;
import com.example.smiti.api.ApiResponse;
import com.example.smiti.model.GroupAlternate;
import com.example.smiti.model.GroupListApiResponse;

import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.widget.ProgressBar;
import android.view.View;
import androidx.annotation.NonNull;
import android.content.SharedPreferences;

public class HomeDashboardActivity extends AppCompatActivity implements CardAdapter.OnItemInteractionListener {

    private RecyclerView recyclerViewPopularGroups;
    private RecyclerView recyclerViewSmbtiGroups;
    private CardAdapter popularGroupsAdapter;
    private CardAdapter smbtiGroupsAdapter;

    private List<CardItem> smbtiItems;

    private ViewFlipper imageFlipper;
    private FloatingActionButton fabAddCard;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private ProgressBar progressBar;

    // 이미지 리소스 배열 정의
    private final int[] sampleCardImages = {
            R.drawable.image1, // 1번 이미지
            R.drawable.image2, // 2번 이미지
            R.drawable.image3, // 3번 이미지
            R.drawable.image4, // 4번 이미지
            R.drawable.image5  // 5번 이미지
            // 더 많은 이미지들을 여기에 추가
    };

    private Random randomGenerator = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homedash); // activity_homedash.xml 참조

        // CardDataHolder의 인기 그룹 데이터 초기화 (앱 시작 시 한 번)
        CardDataHolder.initializePopularItemsIfNeeded();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("찾기");
        }

        imageFlipper = findViewById(R.id.image_flipper);
        if (imageFlipper != null) {
            setupImageFlipper();
        }

        recyclerViewPopularGroups = findViewById(R.id.recycler_view_popular_groups);
        recyclerViewSmbtiGroups = findViewById(R.id.recycler_view_smbti_groups);
        fabAddCard = findViewById(R.id.fab_add_card);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        progressBar = findViewById(R.id.progress_bar);

        // 인기 그룹 설정
        recyclerViewPopularGroups.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        popularGroupsAdapter = new CardAdapter(this, (ArrayList<CardItem>) CardDataHolder.getPopularItems(), this, CardAdapter.AdapterType.POPULAR);
        recyclerViewPopularGroups.setAdapter(popularGroupsAdapter);

        // 인기 그룹 데이터를 서버에서 가져오도록 변경
        popularGroupsAdapter = new CardAdapter(this, new ArrayList<>(), this, CardAdapter.AdapterType.POPULAR); // 초기에는 빈 리스트로 설정
        recyclerViewPopularGroups.setAdapter(popularGroupsAdapter);

        // 서버에서 최신 그룹 데이터 로드
        fetchLatestGroups();

        // SMBTI 그룹 설정
        smbtiItems = new ArrayList<>();


        smbtiGroupsAdapter = new CardAdapter(this, (ArrayList<CardItem>) smbtiItems, new CardAdapter.OnItemInteractionListener() {
            @Override
            public void onItemClick(CardItem item) {
                // Toast.makeText(HomeDashboardActivity.this, "SMBTI 그룹: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                // GroupDetailActivity로 이동하도록 수정 및 그룹 ID 전달
                if (item.getGroupId() != null) {
                    Intent intent = new Intent(HomeDashboardActivity.this, GroupDetailActivity.class);
                    intent.putExtra("groupId", item.getGroupId()); // 그룹 ID 전달
                    intent.putExtra("groupName", item.getTitle()); // 그룹 이름 전달
                    intent.putExtra("groupDescription", item.getDescription()); // 그룹 설명 전달
                    intent.putExtra("maxMembers", item.getMaxMembers()); // 최대 인원 전달
                    intent.putExtra("currentMembers", item.getCurrentMembers()); // 현재 인원 전달
                    startActivity(intent);
                } else {
                    Toast.makeText(HomeDashboardActivity.this, "그룹 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDeleteClick(int position) {
                if (position >= 0 && position < smbtiItems.size()) {
                    CardItem removedItem = smbtiItems.remove(position);
                    smbtiGroupsAdapter.notifyItemRemoved(position);
                    smbtiGroupsAdapter.notifyItemRangeChanged(position, smbtiItems.size() - position);
                    Toast.makeText(HomeDashboardActivity.this, "'" + removedItem.getTitle() + "' SMBTI 그룹 삭제됨", Toast.LENGTH_SHORT).show();
                }
            }
        }, CardAdapter.AdapterType.SMBTI);
        recyclerViewSmbtiGroups.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewSmbtiGroups.setAdapter(smbtiGroupsAdapter);

        fabAddCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeDashboardActivity.this, AddCardActivity.class);
            startActivity(intent);
        });

        setupBottomNavigation();
    }

    private void setupImageFlipper() {
        int[] sampleImages = {R.drawable.image1, R.drawable.image2, R.drawable.image3};
        for (int image : sampleImages) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(image);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageFlipper.addView(imageView);
        }
        imageFlipper.setFlipInterval(3000);
        imageFlipper.setAutoStart(true);
        imageFlipper.setInAnimation(this, android.R.anim.slide_in_left);
        imageFlipper.setOutAnimation(this, android.R.anim.slide_out_right);
    }

    @Override
    public void onItemClick(CardItem item) {
        // Toast.makeText(this, "클릭: " + item.getTitle() + "\n카테고리: " + item.getCategory() + "\n시작일: " + item.getStudyDateFormatted(), Toast.LENGTH_LONG).show();
        // GroupDetailActivity로 이동하도록 수정 및 그룹 ID 전달
        if (item.getGroupId() != null) {
            Intent intent = new Intent(this, GroupDetailActivity.class);
            intent.putExtra("groupId", item.getGroupId()); // 그룹 ID 전달
            intent.putExtra("groupName", item.getTitle()); // 그룹 이름 전달
            intent.putExtra("groupDescription", item.getDescription()); // 그룹 설명 전달
            intent.putExtra("maxMembers", item.getMaxMembers()); // 최대 인원 전달
            intent.putExtra("currentMembers", item.getCurrentMembers()); // 현재 인원 전달
            startActivity(intent);
        } else {
            Toast.makeText(this, "그룹 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(int position) {
        if (position >= 0 && position < CardDataHolder.getPopularItems().size()) { // 인기 그룹 아이템 개수와 비교
            CardItem itemToDelete = CardDataHolder.getPopularItems().get(position);
            new AlertDialog.Builder(this)
                    .setTitle("삭제 확인")
                    .setMessage("'" + itemToDelete.getTitle() + "' 그룹을 정말 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        CardDataHolder.removePopularItem(position);
                        popularGroupsAdapter.notifyItemRemoved(position);
                        popularGroupsAdapter.notifyItemRangeChanged(position, CardDataHolder.getPopularItems().size() - position);
                        Toast.makeText(this, "'" + itemToDelete.getTitle() + "' 삭제됨", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 액티비티가 다시 활성화될 때 최신 그룹 목록 갱신
        fetchLatestGroups();
        // SMBTI 그룹은 정적인 데이터이므로 갱신 불필요 또는 필요시 별도 로직 추가
        if (smbtiGroupsAdapter != null) {
             smbtiGroupsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_search) {
            Toast.makeText(this, "검색 아이콘 클릭됨", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, GroupSearchActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                // 현재 화면이므로 아무것도 안 함
                return true;
            } else if (itemId == R.id.navigation_chat) {
                Intent intent = new Intent(this, ChatActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_board) {
                Intent intent = new Intent(this, BoardActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    // 이 메소드는 현재 코드에서 직접적인 사용처가 명확하지 않으나, 유지합니다.
    public CardAdapter getPopularGroupsAdapterInstance() {
        return popularGroupsAdapter;
    }

    private void fetchLatestGroups() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<GroupListApiResponse> call = apiService.getGroupsForHomeDashboard();

        showLoading(true);

        call.enqueue(new Callback<GroupListApiResponse>() {
            @Override
            public void onResponse(Call<GroupListApiResponse> call, Response<GroupListApiResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    GroupListApiResponse apiResponse = response.body();
                    List<GroupAlternate> allGroupsAlternate = apiResponse.getGroups();

                    if (allGroupsAlternate != null && !allGroupsAlternate.isEmpty()) {
                        // GroupAlternate 목록을 ID를 기준으로 내림차순 정렬하여 최신 그룹을 위로
                        Collections.sort(allGroupsAlternate, (g1, g2) -> {
                            try {
                                // ID가 숫자라고 가정하고 비교
                                Integer id1 = Integer.parseInt(g1.getId());
                                Integer id2 = Integer.parseInt(g2.getId());
                                return id2.compareTo(id1); // 내림차순
                            } catch (NumberFormatException e) {
                                // ID가 숫자가 아니면 문자열로 비교
                                return g2.getId().compareTo(g1.getId());
                            }
                        });

                        // 최신 5개 그룹 추출 (목록 크기 확인)
                        int count = Math.min(allGroupsAlternate.size(), 5);
                        List<GroupAlternate> latestGroupsAlternate = allGroupsAlternate.subList(0, count);


                        ArrayList<CardItem> latestGroupCardItems = new ArrayList<>();


                        for (int i = 0; i < latestGroupsAlternate.size(); i++) {
                            GroupAlternate groupAlt = latestGroupsAlternate.get(i);

                            // 클래스 멤버인 sampleCardImages에서 순환하며 이미지 할당
                            int imageResource = sampleCardImages[i % sampleCardImages.length]; // 순환하며 이미지 할당

                            CardItem cardItem = new CardItem(
                                imageResource,
                                groupAlt.getName(),
                                "", // GroupAlternate에 설명 필드가 없으므로 빈 문자열 전달
                                GroupDetailActivity.class, // 클릭 시 GroupDetailActivity 열기
                                "", // 카테고리 필드가 없으므로 빈 문자열
                                Calendar.getInstance(), // 기본 날짜 설정 (HomeDashboard에서는 날짜 정보 사용 안 함)
                                groupAlt.getMax_members(), // GroupAlternate에서 max_members 값 가져옴
                                groupAlt.getCurrent_members(), // GroupAlternate에서 current_members 값 가져옴
                                String.valueOf(groupAlt.getId()) // 그룹 ID를 String으로 변환하여 전달
                            );
                            latestGroupCardItems.add(cardItem);
                        }

                        // Adapter 데이터 갱신
                        popularGroupsAdapter.updateData(latestGroupCardItems);
                        // 인기 그룹 로딩 완료 후 SMBTI 추천 그룹 로드 시작
                        fetchSmbtiRecommendedGroups();

                    } else {
                        Log.d("HomeDashboard", "No groups found from API");
                        // 결과가 없을 경우 UI 처리 (예: 메시지 표시)
                    }
                } else {
                    Log.e("HomeDashboard", "Failed to fetch groups: " + response.code());
                    // API 호출 실패 시 UI 처리
                    Toast.makeText(HomeDashboardActivity.this, "그룹 목록을 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GroupListApiResponse> call, Throwable t) {
                showLoading(false);
                Log.e("HomeDashboard", "Group fetch network error", t);
                // 네트워크 오류 시 UI 처리
                Toast.makeText(HomeDashboardActivity.this, "네트워크 오류 발생", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 사용자 SMBTI 점수 기반 추천 그룹 목록을 가져오는 메소드
     */
    private void fetchSmbtiRecommendedGroups() {
        showLoading(true);
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("email", "");

        if (userEmail.isEmpty()) {
            showLoading(false);
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        Call<ApiResponse> call = apiService.getGroupsWithSmbtiScore(userEmail);

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);
                smbtiItems.clear(); // 기존 목록 비우기

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    List<Group> groupsWithScore = apiResponse.getGroups(); // ApiResponse에 getGroupsWithScore()가 있다고 가정

                    if (groupsWithScore != null && !groupsWithScore.isEmpty()) {
                        // SMBTI 점수 기준으로 내림차순 정렬
                        Collections.sort(groupsWithScore, (g1, g2) -> Double.compare(g2.getMbtiScore(), g1.getMbtiScore()));

                        // 상위 5개만 선택하여 CardItem으로 변환
                        int count = 0;
                        // 기존 이미지 할당 관련 코드 삭제
                        // int[] smbtiSampleImages = {R.drawable.image1, R.drawable.image2, R.drawable.image3, R.drawable.image4, R.drawable.image5};

                        for (int i = 0; i < groupsWithScore.size() && count < 5; i++) { // 상위 5개까지만
                            Group group = groupsWithScore.get(i);

                            // TODO: 실제 이미지 리소스 또는 처리 로직 필요
                            // 클래스 멤버인 sampleCardImages에서 랜덤으로 가져옴
                            int selectedImageResource = 0; // 기본값 0으로 초기화
                            if (sampleCardImages.length > 0) {
                                int randomIndex = randomGenerator.nextInt(sampleCardImages.length);
                                selectedImageResource = sampleCardImages[randomIndex];
                            } else {
                                Log.w("HomeDashboardActivity", "경고: 카드에 할당할 랜덤 이미지가 없습니다.");
                                // 대체 이미지 또는 오류 처리 필요
                            }

                            CardItem cardItem = new CardItem(
                                selectedImageResource,
                                group.getName() != null ? group.getName() : "무제 그룹",
                                group.getDescription() != null ? group.getDescription() : "", // 그룹 설명 전달
                                GroupDetailActivity.class, // 클릭 시 GroupDetailActivity 열기
                                "", // 카테고리 또는 SMBTI 유형 - 빈 문자열로 변경
                                Calendar.getInstance(), // 기본 날짜 설정 (HomeDashboard에서는 날짜 정보 사용 안 함)
                                group.getMax_members(), // Group에서 max_members 값 가져옴
                                group.getCurrent_members(), // Group에서 current_members 값 가져옴
                                String.valueOf(group.getId()) // 그룹 ID를 String으로 변환하여 전달
                            );
                            smbtiItems.add(cardItem);
                            count++;
                        }
                    }
                } else {
                    Log.e("HomeDashboardActivity", "Failed to fetch SMBTI recommended groups: " + response.code());
                    Toast.makeText(HomeDashboardActivity.this, "SMBTI 추천 그룹을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
                smbtiGroupsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showLoading(false);
                Log.e("HomeDashboardActivity", "Network error fetching SMBTI recommended groups", t);
                Toast.makeText(HomeDashboardActivity.this, "SMBTI 추천 그룹 네트워크 오류", Toast.LENGTH_SHORT).show();
                smbtiGroupsAdapter.notifyDataSetChanged(); // 실패 시에도 목록 갱신 (빈 목록이 표시될 것)
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // 로딩 중 다른 UI 요소 비활성화 등을 추가할 수 있습니다.
    }
}
