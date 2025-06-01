package com.example.smiti.manager;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import com.example.smiti.R;
import com.example.smiti.MainActivity;
import com.example.smiti.GroupSearchActivity;
import com.example.smiti.BoardActivity;
import com.example.smiti.ProfileActivity;
import com.example.smiti.MessageAdapter;
import com.example.smiti.adapter.MemberAdapter;
import com.example.smiti.model.User;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatActivity의 UI 관리를 담당하는 매니저 클래스
 * 메모리 누수 방지와 안전한 UI 조작을 위한 방어적 프로그래밍 적용
 */
public class ChatUIManager {
    
    private static final String TAG = "ChatUIManager";
    
    // Activity 약한 참조로 메모리 누수 방지
    private final WeakReference<Activity> activityRef;    
    // UI 컴포넌트들 - WeakReference로 메모리 누수 방지
    private WeakReference<RecyclerView> recyclerViewRef;
    private WeakReference<EditText> messageEditTextRef;
    private WeakReference<ImageButton> sendButtonRef;
    private WeakReference<ImageButton> attachButtonRef;
    private WeakReference<ImageButton> summaryButtonRef;
    private WeakReference<ImageButton> menuButtonRef;
    private WeakReference<BottomNavigationView> bottomNavigationViewRef;
    private WeakReference<View> rootViewRef;
    
    // 사이드바 관련
    private WeakReference<DrawerLayout> drawerLayoutRef;
    private WeakReference<NavigationView> navigationViewRef;
    private WeakReference<RecyclerView> membersRecyclerViewRef;
    private MemberAdapter memberAdapter;
    
    // Dialog 참조 관리 (메모리 누수 방지)
    private AlertDialog currentDialog;
    
    // 초기화 상태 추적
    private volatile boolean isInitialized = false;
    private volatile boolean isDestroyed = false;
    
    public interface UICallback {
        void onSendButtonClick();
        void onAttachButtonClick();
        void onSummaryButtonClick();
        void onMenuButtonClick();
        void onFileTypeSelected(String mimeType);
    }
      public ChatUIManager(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity cannot be null");
        }
        this.activityRef = new WeakReference<>(activity);
        this.isDestroyed = false;
    }
    
    /**
     * Activity 참조 안전하게 가져오기
     */
    private Activity getActivity() {
        if (isDestroyed) {
            Log.w(TAG, "ChatUIManager has been destroyed");
            return null;
        }
        
        Activity activity = activityRef.get();
        if (activity == null) {
            Log.w(TAG, "Activity reference has been garbage collected");
        } else if (activity.isDestroyed() || activity.isFinishing()) {
            Log.w(TAG, "Activity is destroyed or finishing");
            return null;
        }
        
        return activity;
    }
    
    /**
     * 안전한 UI 작업 실행
     */
    private void runSafely(Runnable action) {
        Activity activity = getActivity();
        if (activity != null && !isDestroyed) {
            try {
                activity.runOnUiThread(() -> {
                    if (!isDestroyed && getActivity() != null) {
                        action.run();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error executing UI action: " + e.getMessage(), e);
            }
        }
    }
      /**
     * 모든 뷰 초기화 - 방어적 프로그래밍 적용
     */
    public void initViews() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "Cannot initialize views: Activity is null");
            return;
        }
        
        try {
            // 기본 뷰들 초기화
            View rootView = activity.findViewById(android.R.id.content);
            RecyclerView recyclerView = activity.findViewById(R.id.recyclerView);
            EditText messageEditText = activity.findViewById(R.id.edit_message);
            ImageButton sendButton = activity.findViewById(R.id.send_button);
            ImageButton attachButton = activity.findViewById(R.id.attach_button);
            BottomNavigationView bottomNavigationView = activity.findViewById(R.id.bottom_navigation);
            ImageButton summaryButton = activity.findViewById(R.id.summary_button);
            ImageButton menuButton = activity.findViewById(R.id.menu_button);
            
            // 사이드바 관련 뷰 초기화
            DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
            NavigationView navigationView = activity.findViewById(R.id.nav_view);
            RecyclerView membersRecyclerView = activity.findViewById(R.id.members_recycler_view);
            
            // WeakReference로 저장
            this.rootViewRef = new WeakReference<>(rootView);
            this.recyclerViewRef = new WeakReference<>(recyclerView);
            this.messageEditTextRef = new WeakReference<>(messageEditText);
            this.sendButtonRef = new WeakReference<>(sendButton);
            this.attachButtonRef = new WeakReference<>(attachButton);
            this.bottomNavigationViewRef = new WeakReference<>(bottomNavigationView);
            this.summaryButtonRef = new WeakReference<>(summaryButton);
            this.menuButtonRef = new WeakReference<>(menuButton);
            this.drawerLayoutRef = new WeakReference<>(drawerLayout);
            this.navigationViewRef = new WeakReference<>(navigationView);
            this.membersRecyclerViewRef = new WeakReference<>(membersRecyclerView);
            
            // 필수 뷰 존재 여부 검증
            validateEssentialViews();
            
            isInitialized = true;
            Log.d(TAG, "Views initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            isInitialized = false;
        }
    }
    
    /**
     * 필수 뷰들의 존재 여부 검증
     */
    private void validateEssentialViews() {
        List<String> missingViews = new ArrayList<>();
        
        if (getRecyclerView() == null) missingViews.add("RecyclerView");
        if (getMessageEditText() == null) missingViews.add("MessageEditText");
        if (getSendButton() == null) missingViews.add("SendButton");
        if (getBottomNavigationView() == null) missingViews.add("BottomNavigationView");
        
        if (!missingViews.isEmpty()) {
            Log.w(TAG, "Missing essential views: " + missingViews);
        }
    }
      /**
     * 리사이클러뷰 설정 - 안전성 개선
     */
    public void setupRecyclerView(MessageAdapter messageAdapter) {
        if (!isInitialized) {
            Log.e(TAG, "Views not initialized yet. Call initViews() first.");
            return;
        }
        
        if (messageAdapter == null) {
            Log.e(TAG, "MessageAdapter cannot be null");
            return;
        }
        
        RecyclerView recyclerView = getRecyclerView();
        if (recyclerView == null) {
            Log.e(TAG, "RecyclerView is null, cannot setup");
            return;
        }
        
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "Activity is null, cannot setup RecyclerView");
            return;
        }
        
        try {
            LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(messageAdapter);
            Log.d(TAG, "RecyclerView setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }
    
    /**
     * 멤버 리사이클러뷰 설정 - 안전성 개선
     */
    public void setupMembersRecyclerView(List<User> memberList) {
        if (!isInitialized) {
            Log.e(TAG, "Views not initialized yet. Call initViews() first.");
            return;
        }
        
        RecyclerView membersRecyclerView = getMembersRecyclerView();
        if (membersRecyclerView == null) {
            Log.w(TAG, "Members RecyclerView is null, skipping setup");
            return;
        }
        
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "Activity is null, cannot setup Members RecyclerView");
            return;
        }
        
        try {
            // memberList가 null이면 빈 리스트 사용
            List<User> safeList = memberList != null ? memberList : new ArrayList<>();
            
            memberAdapter = new MemberAdapter(safeList);
            LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
            membersRecyclerView.setLayoutManager(layoutManager);
            membersRecyclerView.setAdapter(memberAdapter);
            
            Log.d(TAG, "Members RecyclerView setup completed with " + safeList.size() + " members");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Members RecyclerView", e);
        }
    }
      /**
     * 버튼 리스너 설정 - 안전성 개선
     */
    public void setupListeners(UICallback callback) {
        if (!isInitialized) {
            Log.e(TAG, "Views not initialized yet. Call initViews() first.");
            return;
        }
        
        if (callback == null) {
            Log.e(TAG, "UICallback cannot be null");
            return;
        }
        
        try {
            ImageButton sendButton = getSendButton();
            if (sendButton != null) {
                sendButton.setOnClickListener(v -> {
                    if (getActivity() != null && !isDestroyed) {
                        callback.onSendButtonClick();
                    }
                });
            }
            
            ImageButton attachButton = getAttachButton();
            if (attachButton != null) {
                attachButton.setOnClickListener(v -> {
                    if (getActivity() != null && !isDestroyed) {
                        callback.onAttachButtonClick();
                    }
                });
            }
            
            ImageButton summaryButton = getSummaryButton();
            if (summaryButton != null) {
                summaryButton.setOnClickListener(v -> {
                    if (getActivity() != null && !isDestroyed) {
                        callback.onSummaryButtonClick();
                    }
                });
            }
            
            ImageButton menuButton = getMenuButton();
            if (menuButton != null) {
                menuButton.setOnClickListener(v -> {
                    if (getActivity() != null && !isDestroyed) {
                        callback.onMenuButtonClick();
                    }
                });
            }
            
            Log.d(TAG, "Button listeners setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up button listeners", e);
        }
    }
    
    /**
     * 하단 네비게이션 설정 - 안전성 개선
     */
    public void setupBottomNavigation() {
        if (!isInitialized) {
            Log.e(TAG, "Views not initialized yet. Call initViews() first.");
            return;
        }
        
        BottomNavigationView bottomNavigationView = getBottomNavigationView();
        if (bottomNavigationView == null) {
            Log.e(TAG, "BottomNavigationView is null, cannot setup");
            return;
        }
        
        try {
            bottomNavigationView.setSelectedItemId(R.id.navigation_chat);
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                Activity activity = getActivity();
                if (activity == null || isDestroyed) {
                    Log.w(TAG, "Activity is null or destroyed, ignoring navigation");
                    return false;
                }
                
                int id = item.getItemId();
                Intent intent = null;
                
                if (id == R.id.navigation_home) {
                    intent = new Intent(activity, MainActivity.class);
                } else if (id == R.id.navigation_search) {
                    intent = new Intent(activity, GroupSearchActivity.class);
                } else if (id == R.id.navigation_chat) {
                    return true; // 현재 화면이므로 아무 작업 안함
                } else if (id == R.id.navigation_board) {
                    intent = new Intent(activity, BoardActivity.class);
                } else if (id == R.id.navigation_profile) {
                    intent = new Intent(activity, ProfileActivity.class);
                }
                
                if (intent != null) {
                    try {
                        activity.startActivity(intent);
                        activity.finish();
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting activity", e);
                        showToast("화면 전환 중 오류가 발생했습니다");
                    }
                }
                
                return false;
            });
            
            Log.d(TAG, "Bottom navigation setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom navigation", e);
        }
    }
      /**
     * 파일 타입 선택 다이얼로그 표시 - 안전성 개선
     */
    public void showFileTypeSelectionDialog(UICallback callback) {
        Activity activity = getActivity();
        if (activity == null || callback == null) {
            Log.e(TAG, "Cannot show file type dialog: Activity or callback is null");
            return;
        }
        
        // 기존 다이얼로그 정리
        dismissCurrentDialog();
        
        try {
            final CharSequence[] items = {"이미지", "PDF 문서", "모든 파일"};
            
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("업로드할 파일 유형 선택");
            builder.setItems(items, (dialog, which) -> {
                if (getActivity() != null && !isDestroyed) {
                    switch(which) {
                        case 0: // 이미지
                            callback.onFileTypeSelected("image/*");
                            break;
                        case 1: // PDF
                            callback.onFileTypeSelected("application/pdf");
                            break;
                        case 2: // 모든 파일
                            callback.onFileTypeSelected("*/*");
                            break;
                    }
                }
            });
            
            builder.setOnDismissListener(dialog -> currentDialog = null);
            
            currentDialog = builder.create();
            currentDialog.show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing file type dialog", e);
            showToast("파일 선택 창을 열 수 없습니다");
        }
    }
    
    /**
     * 요약 결과 다이얼로그 표시 - 안전성 개선
     */
    public void showSummaryDialog(String summary) {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "Cannot show summary dialog: Activity is null");
            return;
        }
        
        if (summary == null || summary.trim().isEmpty()) {
            Log.w(TAG, "Summary is empty");
            showToast("요약 내용이 없습니다");
            return;
        }
        
        // 기존 다이얼로그 정리
        dismissCurrentDialog();
        
        runSafely(() -> {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("대화 요약");
                builder.setMessage(summary.trim());
                builder.setPositiveButton("확인", null);
                builder.setOnDismissListener(dialog -> currentDialog = null);
                
                currentDialog = builder.create();
                currentDialog.show();
                
            } catch (Exception e) {
                Log.e(TAG, "Error showing summary dialog", e);
                showToast("요약 창을 열 수 없습니다");
            }
        });
    }
      /**
     * 간단한 메시지 표시 - 안전성 개선
     */
    public void showToast(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        runSafely(() -> {
            try {
                Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, message.trim(), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error showing toast: " + e.getMessage());
            }
        });
    }
    
    /**
     * 긴 메시지 표시 - 안전성 개선
     */
    public void showLongToast(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        runSafely(() -> {
            try {
                Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, message.trim(), Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error showing long toast: " + e.getMessage());
            }
        });
    }
    
    /**
     * 드로어 열기/닫기 - 안전성 개선
     */
    public void toggleDrawer() {
        DrawerLayout drawerLayout = getDrawerLayout();
        NavigationView navigationView = getNavigationView();
        
        if (drawerLayout == null || navigationView == null) {
            Log.w(TAG, "Drawer components are null, cannot toggle");
            return;
        }
        
        try {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling drawer", e);
        }
    }
    
    /**
     * 드로어가 열려있는지 확인 - 안전성 개선
     */
    public boolean isDrawerOpen() {
        DrawerLayout drawerLayout = getDrawerLayout();
        NavigationView navigationView = getNavigationView();
        
        if (drawerLayout == null || navigationView == null) {
            return false;
        }
        
        try {
            return drawerLayout.isDrawerOpen(navigationView);
        } catch (Exception e) {
            Log.e(TAG, "Error checking drawer state", e);
            return false;
        }
    }
    
    /**
     * 드로어 닫기 - 안전성 개선
     */
    public void closeDrawer() {
        DrawerLayout drawerLayout = getDrawerLayout();
        NavigationView navigationView = getNavigationView();
        
        if (drawerLayout == null || navigationView == null) {
            Log.w(TAG, "Drawer components are null, cannot close");
            return;
        }
        
        try {
            drawerLayout.closeDrawer(navigationView);
        } catch (Exception e) {
            Log.e(TAG, "Error closing drawer", e);
        }
    }
      /**
     * 메시지 입력란 텍스트 가져오기 - 안전성 개선
     */
    public String getMessageText() {
        EditText messageEditText = getMessageEditText();
        if (messageEditText == null) {
            Log.w(TAG, "MessageEditText is null");
            return "";
        }
        
        try {
            String text = messageEditText.getText().toString();
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            Log.e(TAG, "Error getting message text", e);
            return "";
        }
    }
    
    /**
     * 메시지 입력란 비우기 - 안전성 개선
     */
    public void clearMessageText() {
        EditText messageEditText = getMessageEditText();
        if (messageEditText == null) {
            Log.w(TAG, "MessageEditText is null, cannot clear");
            return;
        }
        
        runSafely(() -> {
            try {
                messageEditText.setText("");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing message text", e);
            }
        });
    }
    
    /**
     * 메시지 입력란에 포커스 설정 - 안전성 개선
     */
    public void focusMessageEditText() {
        EditText messageEditText = getMessageEditText();
        if (messageEditText == null) {
            Log.w(TAG, "MessageEditText is null, cannot focus");
            return;
        }
        
        runSafely(() -> {
            try {
                messageEditText.requestFocus();
            } catch (Exception e) {
                Log.e(TAG, "Error focusing message text", e);
            }
        });
    }      /**
     * 리사이클러뷰 맨 아래로 스크롤 - 안전성 개선
     */
    public void scrollToBottom(int messageCount) {
        if (messageCount <= 0) {
            return;
        }
        
        RecyclerView recyclerView = getRecyclerView();
        if (recyclerView == null) {
            Log.w(TAG, "RecyclerView is null, cannot scroll");
            return;
        }
        
        runSafely(() -> {
            try {
                // 즉시 스크롤을 시도하고, 필요시 지연 후 재시도
                recyclerView.smoothScrollToPosition(messageCount - 1);
                
                // 레이아웃이 완료되지 않았을 경우를 대비하여 지연 후 재시도
                recyclerView.post(() -> {
                    if (getRecyclerView() != null && messageCount > 0 && !isDestroyed) {
                        recyclerView.smoothScrollToPosition(messageCount - 1);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error scrolling to bottom", e);
            }
        });
    }
    
    /**
     * 강제로 맨 아래로 스크롤 (지연 처리 포함) - 안전성 개선
     */
    public void forceScrollToBottom(int messageCount) {
        if (messageCount <= 0) {
            return;
        }
        
        RecyclerView recyclerView = getRecyclerView();
        if (recyclerView == null) {
            Log.w(TAG, "RecyclerView is null, cannot force scroll");
            return;
        }
        
        runSafely(() -> {
            try {
                // 즉시 스크롤
                recyclerView.scrollToPosition(messageCount - 1);
                
                // 100ms 후 부드러운 스크롤로 재시도
                recyclerView.postDelayed(() -> {
                    if (getRecyclerView() != null && messageCount > 0 && !isDestroyed) {
                        recyclerView.smoothScrollToPosition(messageCount - 1);
                    }
                }, 100);
            } catch (Exception e) {
                Log.e(TAG, "Error force scrolling to bottom", e);
            }
        });
    }
    
    /**
     * 리사이클러뷰 특정 위치로 스크롤 - 안전성 개선
     */
    public void scrollToPosition(int position) {
        if (position < 0) {
            return;
        }
        
        RecyclerView recyclerView = getRecyclerView();
        if (recyclerView == null) {
            Log.w(TAG, "RecyclerView is null, cannot scroll to position");
            return;
        }
        
        runSafely(() -> {
            try {
                recyclerView.scrollToPosition(position);
            } catch (Exception e) {
                Log.e(TAG, "Error scrolling to position: " + position, e);
            }
        });
    }
      /**
     * 사용자가 현재 최신 메시지 근처에 있는지 확인 - 안전성 개선
     * @param messageCount 총 메시지 개수
     * @return 최신 메시지 근처에 있으면 true, 그렇지 않으면 false
     */
    public boolean isNearBottom(int messageCount) {
        if (messageCount <= 0) {
            return false;
        }
        
        RecyclerView recyclerView = getRecyclerView();
        if (recyclerView == null) {
            return false;
        }
        
        try {
            androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
                (androidx.recyclerview.widget.LinearLayoutManager) recyclerView.getLayoutManager();
            
            if (layoutManager == null) {
                return false;
            }
            
            int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
            int totalItems = layoutManager.getItemCount();
            
            // 마지막 5개 메시지 범위 내에 있으면 최신 메시지 근처로 판단
            return lastVisiblePosition >= totalItems - 5;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if near bottom", e);
            return false;
        }
    }
    
    /**
     * 조건부 스크롤 - 사용자가 최신 메시지 근처에 있을 때만 스크롤
     * @param messageCount 총 메시지 개수
     */
    public void scrollToBottomIfNearBottom(int messageCount) {
        try {
            if (isNearBottom(messageCount)) {
                forceScrollToBottom(messageCount);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in conditional scroll", e);
        }
    }
    
    /**
     * 멤버 어댑터 업데이트 - 안전성 개선
     */
    public void updateMemberAdapter(List<User> memberList) {
        if (memberAdapter == null) {
            Log.w(TAG, "Member adapter is null, cannot update");
            return;
        }
        
        try {
            // memberList가 null이면 빈 리스트 사용
            List<User> safeList = memberList != null ? memberList : new ArrayList<>();
            memberAdapter.updateMembers(safeList);
            Log.d(TAG, "Member adapter updated with " + safeList.size() + " members");
        } catch (Exception e) {
            Log.e(TAG, "Error updating member adapter", e);
        }
    }
      /**
     * 오류 다이얼로그 표시 - 안전성 개선
     */
    public void showErrorDialog(String title, String message) {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "Cannot show error dialog: Activity is null");
            return;
        }
        
        if (title == null) title = "오류";
        if (message == null || message.trim().isEmpty()) {
            message = "알 수 없는 오류가 발생했습니다";
        }
        
        // 기존 다이얼로그 정리
        dismissCurrentDialog();
        
        final String finalTitle = title;
        final String finalMessage = message.trim();
        
        runSafely(() -> {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(finalTitle);
                builder.setMessage(finalMessage);
                builder.setPositiveButton("확인", null);
                builder.setOnDismissListener(dialog -> currentDialog = null);
                
                currentDialog = builder.create();
                currentDialog.show();
                
            } catch (Exception e) {
                Log.e(TAG, "Error showing error dialog", e);
            }
        });
    }
    
    /**
     * 확인 다이얼로그 표시 - 안전성 개선
     */
    public void showConfirmDialog(String title, String message, 
                                Runnable onConfirm, Runnable onCancel) {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "Cannot show confirm dialog: Activity is null");
            return;
        }
        
        if (title == null) title = "확인";
        if (message == null || message.trim().isEmpty()) {
            Log.w(TAG, "Confirm dialog message is empty");
            return;
        }
        
        // 기존 다이얼로그 정리
        dismissCurrentDialog();
        
        final String finalTitle = title;
        final String finalMessage = message.trim();
        
        runSafely(() -> {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(finalTitle);
                builder.setMessage(finalMessage);
                builder.setPositiveButton("확인", (dialog, which) -> {
                    if (onConfirm != null && !isDestroyed) {
                        try {
                            onConfirm.run();
                        } catch (Exception e) {
                            Log.e(TAG, "Error in confirm callback", e);
                        }
                    }
                });
                builder.setNegativeButton("취소", (dialog, which) -> {
                    if (onCancel != null && !isDestroyed) {
                        try {
                            onCancel.run();
                        } catch (Exception e) {
                            Log.e(TAG, "Error in cancel callback", e);
                        }
                    }
                });
                builder.setOnDismissListener(dialog -> currentDialog = null);
                
                currentDialog = builder.create();
                currentDialog.show();
                
            } catch (Exception e) {
                Log.e(TAG, "Error showing confirm dialog", e);
            }
        });
    }
    
    /**
     * 로딩 다이얼로그 표시 - 안전성 개선
     */
    public AlertDialog showLoadingDialog(String message) {
        Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "Cannot show loading dialog: Activity is null");
            return null;
        }
        
        if (message == null || message.trim().isEmpty()) {
            message = "처리 중...";
        }
        
        // 기존 다이얼로그 정리
        dismissCurrentDialog();
        
        final String finalMessage = message.trim();
        
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(finalMessage);
            builder.setCancelable(false);
            builder.setOnDismissListener(dialog -> currentDialog = null);
            
            currentDialog = builder.create();
            currentDialog.show();
            
            return currentDialog;
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing loading dialog", e);
            return null;
        }
    }
    
    /**
     * 현재 다이얼로그 닫기
     */
    private void dismissCurrentDialog() {
        if (currentDialog != null && currentDialog.isShowing()) {
            try {
                currentDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing current dialog", e);
            } finally {
                currentDialog = null;
            }
        }
    }      /**
     * View 접근자들 - 안전성 개선
     */    
    public RecyclerView getRecyclerView() {
        if (recyclerViewRef == null) {
            Log.e(TAG, "RecyclerView reference is null! initViews() must be called first");
            return null;
        }
        
        RecyclerView recyclerView = recyclerViewRef.get();
        if (recyclerView == null) {
            Log.e(TAG, "RecyclerView has been garbage collected!");
            Log.e(TAG, "Call stack: ", new Exception("RecyclerView access trace"));
        }
        return recyclerView;
    }
    
    public EditText getMessageEditText() {
        return messageEditTextRef != null ? messageEditTextRef.get() : null;
    }
    
    public ImageButton getSendButton() {
        return sendButtonRef != null ? sendButtonRef.get() : null;
    }
    
    public ImageButton getAttachButton() {
        return attachButtonRef != null ? attachButtonRef.get() : null;
    }
    
    public ImageButton getSummaryButton() {
        return summaryButtonRef != null ? summaryButtonRef.get() : null;
    }
    
    public ImageButton getMenuButton() {
        return menuButtonRef != null ? menuButtonRef.get() : null;
    }
    
    public BottomNavigationView getBottomNavigationView() {
        return bottomNavigationViewRef != null ? bottomNavigationViewRef.get() : null;
    }
    
    public DrawerLayout getDrawerLayout() {
        return drawerLayoutRef != null ? drawerLayoutRef.get() : null;
    }
    
    public NavigationView getNavigationView() {
        return navigationViewRef != null ? navigationViewRef.get() : null;
    }
    
    public RecyclerView getMembersRecyclerView() {
        return membersRecyclerViewRef != null ? membersRecyclerViewRef.get() : null;
    }
    
    public View getRootView() {
        return rootViewRef != null ? rootViewRef.get() : null;
    }
    
    /**
     * 초기화 상태 확인
     */
    public boolean isInitialized() {
        return isInitialized && !isDestroyed;
    }
    
    /**
     * 리소스 정리 및 메모리 누수 방지
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up ChatUIManager resources");
        
        isDestroyed = true;
        isInitialized = false;
        
        // 다이얼로그 정리
        dismissCurrentDialog();
        
        // WeakReference들 정리
        if (recyclerViewRef != null) {
            recyclerViewRef.clear();
            recyclerViewRef = null;
        }
        
        if (messageEditTextRef != null) {
            messageEditTextRef.clear();
            messageEditTextRef = null;
        }
        
        if (sendButtonRef != null) {
            sendButtonRef.clear();
            sendButtonRef = null;
        }
        
        if (attachButtonRef != null) {
            attachButtonRef.clear();
            attachButtonRef = null;
        }
        
        if (summaryButtonRef != null) {
            summaryButtonRef.clear();
            summaryButtonRef = null;
        }
        
        if (menuButtonRef != null) {
            menuButtonRef.clear();
            menuButtonRef = null;
        }
        
        if (bottomNavigationViewRef != null) {
            bottomNavigationViewRef.clear();
            bottomNavigationViewRef = null;
        }
        
        if (rootViewRef != null) {
            rootViewRef.clear();
            rootViewRef = null;
        }
        
        if (drawerLayoutRef != null) {
            drawerLayoutRef.clear();
            drawerLayoutRef = null;
        }
        
        if (navigationViewRef != null) {
            navigationViewRef.clear();
            navigationViewRef = null;
        }
        
        if (membersRecyclerViewRef != null) {
            membersRecyclerViewRef.clear();
            membersRecyclerViewRef = null;
        }
        
        // 어댑터 정리
        memberAdapter = null;
        
        Log.d(TAG, "ChatUIManager cleanup completed");
    }
}
