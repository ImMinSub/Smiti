package com.example.smiti;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText; // XML에서 사용했으므로
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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


    private Calendar selectedStudyDate;
    private TextView tvSelectedDateDialogView;
    private TextView tvSelectedCategoryDialogView;
    private String currentSelectedCategory;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homedash);

        // CardDataHolder를 통해 '인기 그룹' 초기 데이터 로드 (필요한 경우 한 번만)
        CardDataHolder.initializePopularItemsIfNeeded();
        // CardDataHolder.initializeSmbtiItemsIfNeeded(); // SMBTI도 CardDataHolder 사용 시

        toolbar = findViewById(R.id.toolbar); // activity_home_dashboard.xml 에 toolbar ID가 있다고 가정
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

        // 인기 그룹 설정
        recyclerViewPopularGroups.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        popularGroupsAdapter = new CardAdapter(this, CardDataHolder.getPopularItems(), this, CardAdapter.AdapterType.POPULAR);
        recyclerViewPopularGroups.setAdapter(popularGroupsAdapter);

        // SMBTI 그룹 설정 (현재는 Activity에서 매번 초기화)
        smbtiItems = new ArrayList<>();
        initializeSmbtiItems(); // SMBTI 아이템 초기화 메소드
        smbtiGroupsAdapter = new CardAdapter(this, smbtiItems, new CardAdapter.OnItemInteractionListener() {
            @Override
            public void onItemClick(CardItem item) {
                Toast.makeText(HomeDashboardActivity.this, "SMBTI 그룹: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                 if (item.getActivityToOpen() != null) {
                     Intent intent = new Intent(HomeDashboardActivity.this, item.getActivityToOpen());
                     startActivity(intent);
                 }
            }

            @Override
            public void onDeleteClick(int position) {
                if (position >= 0 && position < smbtiItems.size()) {
                    smbtiItems.remove(position);
                    smbtiGroupsAdapter.notifyItemWasRemoved(position); // 어댑터에 알림
                    Toast.makeText(HomeDashboardActivity.this, "SMBTI 그룹 삭제됨", Toast.LENGTH_SHORT).show();
                }
            }
        }, CardAdapter.AdapterType.SMBTI);
        recyclerViewSmbtiGroups.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewSmbtiGroups.setAdapter(smbtiGroupsAdapter);


        fabAddCard.setOnClickListener(v -> showAddCardDialog());
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
        imageFlipper.setFlipInterval(3000); // 3초 간격
        imageFlipper.setAutoStart(true);
        imageFlipper.setInAnimation(this, android.R.anim.slide_in_left);
        imageFlipper.setOutAnimation(this, android.R.anim.slide_out_right);
    }


    private void initializeSmbtiItems() {

        smbtiItems.add(new CardItem(R.drawable.image4, "INFP 모여라", "감성 토론방", null, "INFP", CardDataHolder.createCalendar(2025, Calendar.AUGUST, 5)));
        smbtiItems.add(new CardItem(R.drawable.image5, "ESTJ 스터디", "계획적인 스터디", null, "ESTJ", CardDataHolder.createCalendar(2025, Calendar.SEPTEMBER, 15)));

    }

    private void showAddCardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_card, null); // dialog_add_card.xml 사용
        builder.setView(dialogView);
        builder.setTitle("새 스터디 그룹 추가");

        final TextInputEditText etCardTitle = dialogView.findViewById(R.id.et_card_title);
        final TextInputEditText etCardSubtitle = dialogView.findViewById(R.id.et_card_subtitle);
        final LinearLayout layoutCategorySelect = dialogView.findViewById(R.id.layout_category_select_dialog);
        tvSelectedCategoryDialogView = dialogView.findViewById(R.id.tv_selected_category_dialog); // 멤버 변수에 할당
        final LinearLayout layoutDateSelect = dialogView.findViewById(R.id.layout_date_select_dialog);
        tvSelectedDateDialogView = dialogView.findViewById(R.id.tv_selected_date_dialog); // 멤버 변수에 할당

        // 최대 인원 설정 부분 (dialog_add_card.xml에 정의된 ID 사용)
        final ImageButton btnDecreaseMembers = dialogView.findViewById(R.id.btn_decrease_members_dialog);
        final ImageButton btnIncreaseMembers = dialogView.findViewById(R.id.btn_increase_members_dialog);
        final TextView tvMemberCount = dialogView.findViewById(R.id.tv_member_count_dialog);
        // 초기 인원 설정 (예: 1)
        final int[] currentMaxMembers = {1}; // 배열로 해야 내부 클래스에서 접근 가능
        tvMemberCount.setText(String.valueOf(currentMaxMembers[0]));

        btnDecreaseMembers.setOnClickListener(v -> {
            if (currentMaxMembers[0] > 1) {
                currentMaxMembers[0]--;
                tvMemberCount.setText(String.valueOf(currentMaxMembers[0]));
            }
        });
        btnIncreaseMembers.setOnClickListener(v -> {
            // 최대 인원 제한 (예: 100명)
            if (currentMaxMembers[0] < 100) {
                currentMaxMembers[0]++;
                tvMemberCount.setText(String.valueOf(currentMaxMembers[0]));
            }
        });


        // 카테고리 선택 레이아웃 클릭 리스너
        layoutCategorySelect.setOnClickListener(v -> showCategorySelectionDialog());

        // 날짜 선택 레이아웃 클릭 리스너
        layoutDateSelect.setOnClickListener(v -> showDatePickerDialog());

        // 초기화
        selectedStudyDate = null;
        currentSelectedCategory = null;
        tvSelectedDateDialogView.setText("날짜를 선택하세요");
        tvSelectedCategoryDialogView.setText("선택하세요");


        builder.setPositiveButton("추가", null); // 클릭 리스너를 아래에서 오버라이드
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = builder.create();

        // Positive 버튼 클릭 리스너를 오버라이드하여 유효성 검사 후 dismiss 제어
        alertDialog.setOnShowListener(dialogInterface -> {
            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String title = etCardTitle.getText().toString().trim();
                String subtitle = etCardSubtitle.getText().toString().trim();

                if (title.isEmpty()) {
                    etCardTitle.setError("그룹 이름을 입력해주세요.");
                    return;
                }
                if (subtitle.isEmpty()) {
                    etCardSubtitle.setError("그룹 설명을 입력해주세요.");
                    return;
                }
                if (currentSelectedCategory == null || currentSelectedCategory.equals("선택하세요")) {
                    Toast.makeText(HomeDashboardActivity.this, "스터디 카테고리를 선택해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedStudyDate == null) {
                    Toast.makeText(HomeDashboardActivity.this, "스터디 기간(시작일)을 선택해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 모든 유효성 검사 통과
                addNewPopularCard(title, subtitle, currentSelectedCategory, selectedStudyDate, currentMaxMembers[0]);
                alertDialog.dismiss();
            });
        });

        alertDialog.show();
    }

    private void showCategorySelectionDialog() {
        // 실제 카테고리 목록 (values/strings.xml 또는 직접 정의)
        final String[] categories = {"프로그래밍", "어학", "디자인", "취업", "자격증", "기타"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("스터디 카테고리 선택")
                .setItems(categories, (dialog, which) -> {
                    currentSelectedCategory = categories[which];
                    if (tvSelectedCategoryDialogView != null) {
                        tvSelectedCategoryDialogView.setText(currentSelectedCategory);
                        tvSelectedCategoryDialogView.setTextColor(getResources().getColor(android.R.color.black)); // 선택 시 색상 변경
                    }
                });
        builder.create().show();
    }

    private void showDatePickerDialog() {
        final Calendar currentDate = Calendar.getInstance();
        int year = currentDate.get(Calendar.YEAR);
        int month = currentDate.get(Calendar.MONTH);
        int day = currentDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedStudyDate = Calendar.getInstance();
                    selectedStudyDate.set(year1, monthOfYear, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
                    if (tvSelectedDateDialogView != null) {
                        tvSelectedDateDialogView.setText(sdf.format(selectedStudyDate.getTime()));
                        tvSelectedDateDialogView.setTextColor(getResources().getColor(android.R.color.black)); // 선택 시 색상 변경
                    }
                }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); // 오늘 이전 날짜 선택 불가
        datePickerDialog.show();
    }

    private void addNewPopularCard(String title, String subtitle, String category, Calendar studyDate, int maxMembers) {
        // 실제 이미지 리소스는 여기서 결정하거나, CardItem 생성자에서 기본값을 갖도록 수정
        // 예시: 카테고리별 기본 이미지 또는 랜덤 이미지
        int imageResource = R.drawable.image1; // 예시 기본 이미지 (프로젝트에 추가 필요)
        // subtitle에 최대 인원 정보를 추가할 수 있음 (예: "최대 인원: " + maxMembers + "명\n" + subtitle)
        String newSubtitle = "최대 " + maxMembers + "명 | " + subtitle;


        CardItem newCard = new CardItem(imageResource, title, newSubtitle, GroupSearchActivity.class, category, studyDate);

        CardDataHolder.addPopularItem(newCard);
        popularGroupsAdapter.notifyNewItemAdded(0); // 맨 앞에 추가되었으므로 0번 인덱스에 알림
        recyclerViewPopularGroups.scrollToPosition(0); // 새로 추가된 아이템으로 스크롤
        Toast.makeText(this, title + " 그룹이 추가되었습니다.", Toast.LENGTH_SHORT).show();
    }

    // CardAdapter.OnItemInteractionListener 구현 (인기 그룹용)
    @Override
    public void onItemClick(CardItem item) {
        Toast.makeText(this, "클릭: " + item.getTitle() + "\n카테고리: " + item.getCategory() + "\n시작일: " + item.getStudyDateFormatted(), Toast.LENGTH_LONG).show();
        if (item.getActivityToOpen() != null) {
             Intent intent = new Intent(this, item.getActivityToOpen());
             startActivity(intent);
        }
    }

    @Override
    public void onDeleteClick(int position) {
        // 삭제 확인 다이얼로그 (선택 사항)
        new AlertDialog.Builder(this)
                .setTitle("삭제 확인")
                .setMessage("'" + CardDataHolder.getPopularItems().get(position).getTitle() + "' 그룹을 정말 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    CardDataHolder.removePopularItem(position);
                    popularGroupsAdapter.notifyItemWasRemoved(position);
                    Toast.makeText(this, "삭제됨", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 다른 액티비티에서 돌아왔을 때, CardDataHolder의 데이터가 변경되었을 수 있으므로 어댑터를 갱신합니다.
        if (popularGroupsAdapter != null) {
            popularGroupsAdapter.notifyDataSetChanged(); // 전체 데이터셋 변경 알림
        }
        // SMBTI 어댑터도 필요하다면 갱신 (만약 SMBTI 데이터도 CardDataHolder 등을 통해 관리된다면)
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
        if (item.getItemId() == R.id.action_search) { // toolbar_menu.xml에 정의된 ID
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
            if (itemId == R.id.navigation_home) { // bottom_navigation_menu.xml 에 정의된 ID
                // 현재 화면이므로 아무것도 안 함
                return true;
            } else if (itemId == R.id.navigation_home) {
                Toast.makeText(HomeDashboardActivity.this, "홈 탭 클릭", Toast.LENGTH_SHORT).show();
                 Intent intent = new Intent(this, MainActivity.class);
                 startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_chat) {
                Toast.makeText(HomeDashboardActivity.this, "채팅 탭 클릭", Toast.LENGTH_SHORT).show();
                 Intent intent = new Intent(this, ChatActivity.class);
                 startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_board) {
                Toast.makeText(HomeDashboardActivity.this, "채팅 게시판 클릭", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, BoardActivity.class);
                startActivity(intent);
                return true;

            } else if (itemId == R.id.navigation_profile) {
                Toast.makeText(HomeDashboardActivity.this, "프로필 탭 클릭", Toast.LENGTH_SHORT).show();
                 Intent intent = new Intent(this, ProfileActivity.class);
                 startActivity(intent);
                return true;
            }
            return false;
        });
        // 시작 시 홈 선택
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    // 이 메소드는 CardAdapter에서 삭제 버튼 가시성 로직에 잠재적으로 사용될 수 있으나,
    // AdapterType으로 구분하는 것이 더 권장.
    public CardAdapter getPopularGroupsAdapterInstance() {
        return popularGroupsAdapter;
    }
}