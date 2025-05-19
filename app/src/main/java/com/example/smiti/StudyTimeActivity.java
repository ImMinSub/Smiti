package com.example.smiti;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smiti.adapter.TimeSlotAdapter;
import com.example.smiti.api.ApiResponse;
import com.example.smiti.api.AvailableTimesRequest;
import com.example.smiti.api.RetrofitClient;
import com.example.smiti.model.TimeSlot;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudyTimeActivity extends AppCompatActivity implements TimeSlotAdapter.OnTimeSlotClickListener {
    private static final String TAG = "StudyTimeActivity";
    private static final String PREF_NAME = "LoginPrefs";

    // 요일 정보
    private final String[] DAYS = {"월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"};
    // 영어로 된 요일 (API 요청용)
    private final String[] DAY_KEYS = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

    // UI 요소
    private TabLayout tabDays;
    private RecyclerView recyclerTimeSlots;
    private View emptyView;
    private FloatingActionButton fabAddTime;
    private ImageButton btnBack;
    private Button btnSave;

    // 데이터
    private Map<String, List<TimeSlot>> availableTimeMap; // 요일별 시간 슬롯 맵
    private TimeSlotAdapter adapter;
    private String currentDay; // 현재 선택된 요일
    private String currentDayKey; // 현재 선택된 요일 키 (API용)
    private String userEmail; // 사용자 이메일

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_time);

        // 사용자 이메일 불러오기
        loadUserEmail();

        // UI 초기화
        initViews();
        initListeners();

        // 데이터 초기화
        availableTimeMap = new HashMap<>();
        for (String dayKey : DAY_KEYS) {
            availableTimeMap.put(dayKey, new ArrayList<>());
        }

        // 첫 번째 요일 설정
        currentDay = DAYS[0];
        currentDayKey = DAY_KEYS[0];

        // 어댑터 설정 (먼저 설정)
        setupAdapter();
        
        // 탭 설정 (어댑터 설정 후에 호출)
        setupTabs();

        // 서버에서 스터디 가능 시간 불러오기
        fetchAvailableTimes();
    }

    private void loadUserEmail() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userEmail = prefs.getString("email", "");
        if (userEmail.isEmpty()) {
            finish();
        }
    }

    private void initViews() {
        tabDays = findViewById(R.id.tab_days);
        recyclerTimeSlots = findViewById(R.id.recycler_time_slots);
        emptyView = findViewById(R.id.empty_view);
        fabAddTime = findViewById(R.id.fab_add_time);
        btnBack = findViewById(R.id.btn_back);
        btnSave = findViewById(R.id.btn_save);
    }

    private void initListeners() {
        // 뒤로가기 버튼
        btnBack.setOnClickListener(v -> finish());
        
        // 저장 버튼
        btnSave.setOnClickListener(v -> {
            saveAvailableTimes();
        });

        // FAB 클릭 - 시간 추가
        fabAddTime.setOnClickListener(v -> showTimePickerDialog());

        // 탭 변경 리스너
        tabDays.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                currentDay = DAYS[position];
                currentDayKey = DAY_KEYS[position];
                updateUI();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupTabs() {
        for (String day : DAYS) {
            tabDays.addTab(tabDays.newTab().setText(day));
        }
    }

    private void setupAdapter() {
        adapter = new TimeSlotAdapter(this, new ArrayList<>(), this);
        recyclerTimeSlots.setLayoutManager(new LinearLayoutManager(this));
        recyclerTimeSlots.setAdapter(adapter);
    }

    /**
     * 서버에서 가능한 시간 정보 가져오기
     */
    private void fetchAvailableTimes() {
        if (userEmail.isEmpty()) return;

        RetrofitClient.getApiService().getAvailableTimes(userEmail).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    
                    try {
                        // 응답 본문에서 JSON으로 직접 파싱
                        String responseJson = response.body().toString();
                        
                        // 응답에 available_times가 포함되어 있는지 확인
                        if (responseJson.contains("available_times")) {
                            try {
                                // JSON에서 직접 available_times 필드 추출
                                Object availableTimesData = null;
                                
                                if (apiResponse.getData() != null) {
                                    // data 객체 내에 available_times가 있는 경우
                                    Map<String, Object> data = (Map<String, Object>) apiResponse.getData();
                                    if (data.containsKey("available_times")) {
                                        availableTimesData = data.get("available_times");
                                    }
                                } else {
                                    // 최상위에 available_times가 있는 경우 (리플렉션으로 접근)
                                    try {
                                        java.lang.reflect.Field field = apiResponse.getClass().getDeclaredField("available_times");
                                        field.setAccessible(true);
                                        availableTimesData = field.get(apiResponse);
                                    } catch (Exception ignored) {
                                        // 리플렉션 실패 시 무시
                                    }
                                }
                                
                                // available_times 데이터가 있으면 파싱
                                if (availableTimesData instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, List<String>> serverTimes = (Map<String, List<String>>) availableTimesData;
                                    parseServerTimes(serverTimes);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "시간 데이터 파싱 오류: " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "응답 처리 오류: " + e.getMessage());
                    }
                    
                    // UI 업데이트
                    updateUI();
                } else {
                    Log.e(TAG, "서버 오류: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
            }
        });
    }

    /**
     * 서버에서 받은 시간 데이터를 파싱하여 내부 모델로 변환
     */
    private void parseServerTimes(Map<String, List<String>> serverTimes) {
        for (String dayKey : DAY_KEYS) {
            List<TimeSlot> timeSlots = new ArrayList<>();
            
            if (serverTimes.containsKey(dayKey)) {
                List<String> timeStrings = serverTimes.get(dayKey);
                
                for (String timeString : timeStrings) {
                    String[] parts = timeString.split("~");
                    if (parts.length == 2) {
                        timeSlots.add(new TimeSlot(parts[0].trim(), parts[1].trim()));
                    }
                }
            }
            
            availableTimeMap.put(dayKey, timeSlots);
        }
    }

    /**
     * UI 업데이트 - 현재 선택된 요일의 시간 슬롯을 표시
     */
    private void updateUI() {
        List<TimeSlot> currentTimeSlots = availableTimeMap.get(currentDayKey);
        adapter.updateData(currentTimeSlots);
        
        // 빈 상태 표시
        if (currentTimeSlots.isEmpty()) {
            recyclerTimeSlots.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerTimeSlots.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    /**
     * 시간 선택 다이얼로그 표시
     */
    private void showTimePickerDialog() {
        // 커스텀 다이얼로그 뷰 생성
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_range_picker, null);
        
        // 시작 및 종료 타임피커 참조
        TimePicker startTimePicker = dialogView.findViewById(R.id.picker_start_time);
        TimePicker endTimePicker = dialogView.findViewById(R.id.picker_end_time);
        
        // 24시간 형식 설정
        startTimePicker.setIs24HourView(true);
        endTimePicker.setIs24HourView(true);
        
        // 기본 시간 설정 (현재 시간 + 1시간)
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        
        startTimePicker.setHour(currentHour);
        startTimePicker.setMinute(currentMinute);
        
        // 종료 시간은 시작 시간 + 1시간으로 설정
        endTimePicker.setHour(Math.min(currentHour + 1, 23));
        endTimePicker.setMinute(currentMinute);
        
        // 다이얼로그 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // 취소 버튼 클릭 리스너
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        // 저장 버튼 클릭 리스너
        Button saveButton = dialogView.findViewById(R.id.btn_save);
        saveButton.setOnClickListener(v -> {
            // 시간 가져오기
            int startHour = startTimePicker.getHour();
            int startMinute = startTimePicker.getMinute();
            int endHour = endTimePicker.getHour();
            int endMinute = endTimePicker.getMinute();
            
            // 시간 문자열 생성
            String startTime = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute);
            String endTime = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute);
            
            // 시간 검증
            if (isValidTimeRange(startHour, startMinute, endHour, endMinute)) {
                // 시간 추가
                addTimeSlot(startTime, endTime);
                // 자동 저장
                saveAvailableTimes();
                // 다이얼로그 닫기
                dialog.dismiss();
            } else {
                // 오류 메시지 표시
                Toast.makeText(this, "종료 시간은 시작 시간보다 나중이어야 합니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 시간 범위 유효성 검사
     */
    private boolean isValidTimeRange(int startHour, int startMinute, int endHour, int endMinute) {
        if (startHour < endHour) {
            return true;
        } else return startHour == endHour && startMinute < endMinute;
    }

    /**
     * 시간 슬롯 추가
     */
    private void addTimeSlot(String startTime, String endTime) {
        List<TimeSlot> timeSlots = availableTimeMap.get(currentDayKey);
        timeSlots.add(new TimeSlot(startTime, endTime));
        updateUI();
    }

    /**
     * 시간 슬롯 삭제
     */
    @Override
    public void onTimeSlotRemove(int position) {
        List<TimeSlot> timeSlots = availableTimeMap.get(currentDayKey);
        if (position >= 0 && position < timeSlots.size()) {
            timeSlots.remove(position);
            updateUI();
        }
    }

    /**
     * 스터디 가능 시간 저장
     */
    private void saveAvailableTimes() {
        // API 요청 형식으로 변환
        Map<String, List<String>> requestTimes = new HashMap<>();
        
        for (Map.Entry<String, List<TimeSlot>> entry : availableTimeMap.entrySet()) {
            String dayKey = entry.getKey();
            List<TimeSlot> timeSlots = entry.getValue();
            
            List<String> timeStrings = new ArrayList<>();
            for (TimeSlot slot : timeSlots) {
                timeStrings.add(slot.toString());
            }
            
            requestTimes.put(dayKey, timeStrings);
        }
        
        // 저장 중 표시
        Toast.makeText(StudyTimeActivity.this, "저장 중...", Toast.LENGTH_SHORT).show();
        
        // API 요청 생성
        AvailableTimesRequest request = new AvailableTimesRequest(userEmail, requestTimes);
        
        // 서버에 저장 요청
        RetrofitClient.getApiService().updateAvailableTimes(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    
                    if (apiResponse.getStatus().equals("success")) {
                        // 스터디 시간 정보를 SharedPreferences에 저장
                        saveStudyTimesToLocal(requestTimes);
                        Toast.makeText(StudyTimeActivity.this, "스터디 가능 시간이 저장되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(StudyTimeActivity.this, 
                                "저장 실패: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(StudyTimeActivity.this, 
                            "서버 오류 발생: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                Toast.makeText(StudyTimeActivity.this, "서버 연결에 실패했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // 스터디 시간 정보를 로컬에 저장
    private void saveStudyTimesToLocal(Map<String, List<String>> studyTimes) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // 요일별 스터디 시간 정보 저장
        for (String dayKey : DAY_KEYS) {
            List<String> times = studyTimes.get(dayKey);
            if (times != null && !times.isEmpty()) {
                // 쉼표로 구분된 문자열로 변환 (예: "09:00~10:00,14:00~16:00")
                editor.putString("study_time_" + dayKey, String.join(",", times));
            } else {
                editor.putString("study_time_" + dayKey, "");
            }
        }
        
        // 스터디 시간 설정 여부 플래그 저장
        editor.putBoolean("has_study_times", true);
        editor.apply();
    }
} 
