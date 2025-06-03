package com.example.smiti;

import android.app.DatePickerDialog;
// import android.content.Intent; // 현재 직접적인 사용처 없음
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random; // 랜덤 기능 사용을 위해 추가

import android.content.SharedPreferences;
import android.util.Log;
import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.ApiService;
import com.example.smiti.api.RetrofitClient;
import com.example.smiti.api.CreateGroupRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddCardActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextInputEditText etCardTitlePage;
    private TextInputEditText etCardSubtitlePage;
    private ImageButton btnDecreaseMembersPage;
    private ImageButton btnIncreaseMembersPage;
    private TextView tvMemberCountPage;
    private LinearLayout layoutCategorySelectPage;
    private TextView tvSelectedCategoryPage;
    private LinearLayout layoutDateSelectPage;
    private TextView tvSelectedDatePage;
    private Button btnCreateGroup;

    private final String[] categories = {"프로그래밍", "어학", "취업", "자격증", "취미", "기타"};
    private String selectedCategory = "";
    private Calendar selectedDateCalendar = Calendar.getInstance();
    private int currentMemberCount = 10; // 이 값을 CardItem의 maxMembers로 전달
    private final int MAX_MEMBER_LIMIT = 20;
    private boolean isDateSelected = false;

    // 랜덤으로 선택될 이미지 리소스 ID 배열
    private final int[] randomCardImages = {
            R.drawable.image1,
            R.drawable.image2,
            R.drawable.image3,
            R.drawable.image4,
            R.drawable.image5,
            // 필요에 따라 더 많은 이미지 리소스 ID 추가
    };
    private Random randomGenerator = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card); // activity_add_card.xml 참조

        toolbar = findViewById(R.id.toolbar_add_card);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        etCardTitlePage = findViewById(R.id.et_card_title_page);
        etCardSubtitlePage = findViewById(R.id.et_card_subtitle_page);
        btnDecreaseMembersPage = findViewById(R.id.btn_decrease_members_page);
        tvMemberCountPage = findViewById(R.id.tv_member_count_page);
        btnIncreaseMembersPage = findViewById(R.id.btn_increase_members_page);
        layoutCategorySelectPage = findViewById(R.id.layout_category_select_page);
        tvSelectedCategoryPage = findViewById(R.id.tv_selected_category_page);
        layoutDateSelectPage = findViewById(R.id.layout_date_select_page);
        tvSelectedDatePage = findViewById(R.id.tv_selected_date_page);
        btnCreateGroup = findViewById(R.id.btn_create_group);

        tvMemberCountPage.setText(String.valueOf(currentMemberCount));
        updateDateLabel();
        tvSelectedCategoryPage.setText("선택");

        // Intent에서 검색어 가져오기
        String initialKeyword = getIntent().getStringExtra("SEARCH_KEYWORD");
        if (initialKeyword != null && !initialKeyword.isEmpty()) {
            etCardTitlePage.setText(initialKeyword + " 그룹"); // 그룹 이름에 검색어 + " 그룹" 설정
            etCardSubtitlePage.setText(initialKeyword + "에 관련된 그룹입니다."); // 그룹 설명에 검색어 관련 설명 설정
        }

        setupListeners();
    }

    private void setupListeners() {
        btnDecreaseMembersPage.setOnClickListener(v -> {
            if (currentMemberCount > 1) {
                currentMemberCount--;
                tvMemberCountPage.setText(String.valueOf(currentMemberCount));
            }
        });

        btnIncreaseMembersPage.setOnClickListener(v -> {
            if (currentMemberCount < MAX_MEMBER_LIMIT) {
                currentMemberCount++;
                tvMemberCountPage.setText(String.valueOf(currentMemberCount));
            } else {
                Toast.makeText(this, "최대 " + MAX_MEMBER_LIMIT + "명까지 설정 가능합니다.", Toast.LENGTH_SHORT).show();
            }
        });

        layoutCategorySelectPage.setOnClickListener(v -> showCategorySelectionDialog());
        layoutDateSelectPage.setOnClickListener(v -> showDatePickerDialog());
        btnCreateGroup.setOnClickListener(v -> createGroupAndFinish());
    }

    private void showCategorySelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("카테고리 선택")
                .setItems(categories, (dialog, which) -> {
                    selectedCategory = categories[which];
                    tvSelectedCategoryPage.setText(selectedCategory);
                    tvSelectedCategoryPage.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
                });
        builder.create().show();
    }

    private void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            selectedDateCalendar.set(Calendar.YEAR, year);
            selectedDateCalendar.set(Calendar.MONTH, monthOfYear);
            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            isDateSelected = true;
            updateDateLabel();
        };

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, dateSetListener,
                selectedDateCalendar.get(Calendar.YEAR),
                selectedDateCalendar.get(Calendar.MONTH),
                selectedDateCalendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        if (isDateSelected) {
            String displayFormat = "yyyy-MM-dd";
            SimpleDateFormat sdf = new SimpleDateFormat(displayFormat, Locale.getDefault());
            tvSelectedDatePage.setText(sdf.format(selectedDateCalendar.getTime()));
            tvSelectedDatePage.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
        } else {
            tvSelectedDatePage.setText("선택");
            // R.color.medium_gray 가 colors.xml에 정의되어 있어야 함.
            tvSelectedDatePage.setTextColor(getResources().getColor(R.color.medium_gray, getTheme()));
        }
    }

    private void createGroupAndFinish() {
        String title = etCardTitlePage.getText().toString().trim();
        String subtitle = etCardSubtitlePage.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "그룹 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            etCardTitlePage.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(subtitle)) {
            Toast.makeText(this, "그룹 설명을 입력해주세요.", Toast.LENGTH_SHORT).show();
            etCardSubtitlePage.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(selectedCategory) || selectedCategory.equals("선택")) {
            Toast.makeText(this, "스터디 카테고리를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isDateSelected) {
            Toast.makeText(this, "스터디 기간을 설정해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // API를 사용하여 그룹 생성 요청
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("email", "");

        if (userEmail.isEmpty()) {
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();

        CreateGroupRequest request = new CreateGroupRequest();
        request.setGroup_name(title);
        request.setDescription(subtitle);
        request.setEmail(userEmail);
        request.setTopics(selectedCategory);
        request.setUseAi(false);
        request.setMax_members(currentMemberCount);

        Call<ApiResponse> call = apiService.createGroup(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 성공적으로 그룹 생성
                    Toast.makeText(AddCardActivity.this, "'" + title + "' 그룹이 성공적으로 생성되었습니다!", Toast.LENGTH_LONG).show();

                    // 성공 시 홈 화면에 카드를 추가
                    int selectedImageResource = 0;
                    if (randomCardImages.length > 0) {
                        int randomIndex = randomGenerator.nextInt(randomCardImages.length);
                        selectedImageResource = randomCardImages[randomIndex];
                    } else {
                        Log.w("AddCardActivity", "경고: 카드에 할당할 랜덤 이미지가 없습니다.");
                        // 기본 이미지 또는 오류 처리 필요
                    }

                    // CardItem 생성자 호출: (int imageResource, String title, String subtitle, Class<?> activityToOpen, String category, Calendar studyDate, int maxMembers, int currentMembers)
                    CardItem newCard = new CardItem(
                            selectedImageResource,
                            title,
                            subtitle,
                            null, // Class<?> activityToOpen: 새로 만드는 그룹이므로 null
                            selectedCategory,
                            selectedDateCalendar,
                            currentMemberCount,      // maxMembers (최대 인원) 전달
                            1, // currentMembers (현재 인원)로 1 전달 - 추가
                            "" // groupId: 새로 생성하는 그룹이므로 빈 문자열 전달
                    );

                    CardDataHolder.addPopularItem(newCard); // 인기 그룹 목록에 추가

                    finish(); // 현재 액티비티 종료
                } else {
                    // 그룹 생성 실패
                    String errorBody = "";
                    try {
                         if (response.errorBody() != null) {
                             errorBody = response.errorBody().string();
                         }
                    } catch (Exception e) {
                         Log.e("AddCardActivity", "Error reading error body", e);
                    }
                    Log.e("AddCardActivity", "Group creation failed: " + response.code() + ", " + errorBody);
                    Toast.makeText(AddCardActivity.this, "그룹 생성 실패: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // 네트워크 오류 등으로 인한 실패
                Log.e("AddCardActivity", "Group creation network error: " + t.getMessage(), t);
                Toast.makeText(AddCardActivity.this, "그룹 생성 네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
