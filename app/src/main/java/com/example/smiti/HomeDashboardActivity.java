package com.example.smiti;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

        // 인기 그룹 설정
        recyclerViewPopularGroups.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        // CardDataHolder.getPopularItems()는 List<CardItem>을 반환. 어댑터 생성자에 맞게 ArrayList로 캐스팅하거나 어댑터 생성자 타입을 List로 변경.
        // 여기서는 CardAdapter 생성자가 ArrayList<CardItem>을 받는다고 가정하고 캐스팅.
        popularGroupsAdapter = new CardAdapter(this, (ArrayList<CardItem>) CardDataHolder.getPopularItems(), this, CardAdapter.AdapterType.POPULAR);
        recyclerViewPopularGroups.setAdapter(popularGroupsAdapter);

        // SMBTI 그룹 설정
        smbtiItems = new ArrayList<>(); // ArrayList로 초기화
        initializeSmbtiItems(); // SMBTI 아이템 초기화 (이 부분 수정)

        smbtiGroupsAdapter = new CardAdapter(this, (ArrayList<CardItem>) smbtiItems, new CardAdapter.OnItemInteractionListener() {
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

    // 이 메소드에서 CardItem 생성자 호출 시 7개의 인자를 전달하도록 수정
    private void initializeSmbtiItems() {
        // CardItem 생성자 시그니처 (7개 인자):
        // (int imageResource, String title, String subtitle, Class<?> activityToOpen, String category, Calendar studyDate, int maxMembers)
        // 에 맞게 마지막 인자로 maxMembers 값을 추가합니다.

        smbtiItems.add(new CardItem(
                R.drawable.image4,             // imageResource
                "INFP 모여라",                 // title
                "감성 토론방",                 // subtitle
                null,                          // activityToOpen (SMBTI 그룹은 특정 액티비티 연결 X)
                "INFP",                        // category (또는 SMBTI 유형)
                CardDataHolder.createCalendar(2025, Calendar.AUGUST, 5), // studyDate
                5                              // maxMembers (예시 값)
        ));

        smbtiItems.add(new CardItem(
                R.drawable.image5,
                "ESTJ 스터디",
                "계획적인 스터디",
                null,
                "ESTJ",
                CardDataHolder.createCalendar(2025, Calendar.SEPTEMBER, 15),
                7 // maxMembers (예시 값)
        ));
        // 필요하다면 더 많은 SMBTI 그룹 아이템들을 위와 같은 형식으로 추가
    }

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
        if (popularGroupsAdapter != null) {
            // CardAdapter의 updateData 메소드가 List<CardItem>을 받는다면 캐스팅 불필요
            // 여기서는 getPopularItems()가 List<CardItem>을 반환한다고 가정하고,
            // 어댑터의 updateData 메소드가 이를 처리할 수 있도록 되어 있다고 가정합니다.
            // 또는 notifyDataSetChanged()만 호출해도 됩니다.
            popularGroupsAdapter.updateData(new ArrayList<>(CardDataHolder.getPopularItems())); // 새 리스트로 전달
            // popularGroupsAdapter.notifyDataSetChanged(); // 이 방법도 가능
        }
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
}
