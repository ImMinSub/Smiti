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
    private int currentMemberCount = 1; // 이 값을 CardItem의 maxMembers로 전달
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

        int selectedImageResource = 0;
        if (randomCardImages.length > 0) {
            int randomIndex = randomGenerator.nextInt(randomCardImages.length);
            selectedImageResource = randomCardImages[randomIndex];
        } else {
            Toast.makeText(this, "경고: 카드에 할당할 랜덤 이미지가 없습니다.", Toast.LENGTH_SHORT).show();
        }

        // CardItem 생성자 호출: (int imageResource, String title, String subtitle, Class<?> activityToOpen, String category, Calendar studyDate, int maxMembers)
        // 에 맞게 currentMemberCount를 마지막 인자로 전달
        CardItem newCard = new CardItem(
                selectedImageResource,
                title,
                subtitle,
                null, // Class<?> activityToOpen: 새로 만드는 인기 그룹이므로 null
                selectedCategory,
                selectedDateCalendar,
                currentMemberCount      // currentMemberCount (최대 인원) 전달
        );

        CardDataHolder.addPopularItem(newCard);
        Toast.makeText(this, "'" + newCard.getTitle() + "' 그룹이 추가되었습니다.", Toast.LENGTH_SHORT).show();

        finish(); // 현재 액티비티 종료
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
