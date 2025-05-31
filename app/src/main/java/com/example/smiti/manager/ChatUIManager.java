package com.example.smiti.manager;

import android.app.Activity;
import android.content.Intent;
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

import java.util.List;

public class ChatUIManager {
    
    private static final String TAG = "ChatUIManager";
    
    private final Activity activity;
    
    // UI 컴포넌트들
    private RecyclerView recyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton attachButton;
    private ImageButton summaryButton;
    private ImageButton menuButton;
    private BottomNavigationView bottomNavigationView;
    private View rootView;
    
    // 사이드바 관련
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView membersRecyclerView;
    private MemberAdapter memberAdapter;
    
    public interface UICallback {
        void onSendButtonClick();
        void onAttachButtonClick();
        void onSummaryButtonClick();
        void onMenuButtonClick();
        void onFileTypeSelected(String mimeType);
    }
    
    public ChatUIManager(Activity activity) {
        this.activity = activity;
    }
    
    /**
     * 모든 뷰 초기화
     */
    public void initViews() {
        rootView = activity.findViewById(android.R.id.content);
        recyclerView = activity.findViewById(R.id.recyclerView);
        messageEditText = activity.findViewById(R.id.edit_message);
        sendButton = activity.findViewById(R.id.send_button);
        attachButton = activity.findViewById(R.id.attach_button);
        bottomNavigationView = activity.findViewById(R.id.bottom_navigation);
        summaryButton = activity.findViewById(R.id.summary_button);
        menuButton = activity.findViewById(R.id.menu_button);
        
        // 사이드바 관련 뷰 초기화
        drawerLayout = activity.findViewById(R.id.drawer_layout);
        navigationView = activity.findViewById(R.id.nav_view);
        membersRecyclerView = activity.findViewById(R.id.members_recycler_view);
    }
    
    /**
     * 리사이클러뷰 설정
     */
    public void setupRecyclerView(MessageAdapter messageAdapter) {
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(messageAdapter);
    }
    
    /**
     * 멤버 리사이클러뷰 설정
     */
    public void setupMembersRecyclerView(List<User> memberList) {
        memberAdapter = new MemberAdapter(memberList);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        membersRecyclerView.setAdapter(memberAdapter);
    }
    
    /**
     * 버튼 리스너 설정
     */
    public void setupListeners(UICallback callback) {
        if (sendButton != null) {
            sendButton.setOnClickListener(v -> callback.onSendButtonClick());
        }
        
        if (attachButton != null) {
            attachButton.setOnClickListener(v -> callback.onAttachButtonClick());
        }
        
        if (summaryButton != null) {
            summaryButton.setOnClickListener(v -> callback.onSummaryButtonClick());
        }
        
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> callback.onMenuButtonClick());
        }
    }
    
    /**
     * 하단 네비게이션 설정
     */
    public void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_chat);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                activity.startActivity(new Intent(activity, MainActivity.class));
                activity.finish();
                return true;
            } else if (id == R.id.navigation_search) {
                activity.startActivity(new Intent(activity, GroupSearchActivity.class));
                activity.finish();
                return true;
            } else if (id == R.id.navigation_chat) {
                return true; // 현재 화면이므로 아무 작업 안함
            } else if (id == R.id.navigation_board) {
                activity.startActivity(new Intent(activity, BoardActivity.class));
                activity.finish();
                return true;
            } else if (id == R.id.navigation_profile) {
                activity.startActivity(new Intent(activity, ProfileActivity.class));
                activity.finish();
                return true;
            }
            return false;
        });
    }
    
    /**
     * 파일 타입 선택 다이얼로그 표시
     */
    public void showFileTypeSelectionDialog(UICallback callback) {
        final CharSequence[] items = {"이미지", "PDF 문서", "모든 파일"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("업로드할 파일 유형 선택");
        builder.setItems(items, (dialog, which) -> {
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
        });
        builder.show();
    }
    
    /**
     * 요약 결과 다이얼로그 표시
     */
    public void showSummaryDialog(String summary) {
        new AlertDialog.Builder(activity)
                .setTitle("대화 요약")
                .setMessage(summary)
                .setPositiveButton("확인", null)
                .show();
    }
    
    /**
     * 간단한 메시지 표시
     */
    public void showToast(String message) {
        try {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // 오류 시 무시
        }
    }
    
    /**
     * 긴 메시지 표시
     */
    public void showLongToast(String message) {
        try {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // 오류 시 무시
        }
    }
    
    /**
     * 드로어 열기/닫기
     */
    public void toggleDrawer() {
        if (drawerLayout != null && navigationView != null) {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
        }
    }
    
    /**
     * 드로어가 열려있는지 확인
     */
    public boolean isDrawerOpen() {
        return drawerLayout != null && navigationView != null && 
               drawerLayout.isDrawerOpen(navigationView);
    }
    
    /**
     * 드로어 닫기
     */
    public void closeDrawer() {
        if (drawerLayout != null && navigationView != null) {
            drawerLayout.closeDrawer(navigationView);
        }
    }
    
    /**
     * 메시지 입력란 텍스트 가져오기
     */
    public String getMessageText() {
        if (messageEditText != null) {
            return messageEditText.getText().toString().trim();
        }
        return "";
    }
    
    /**
     * 메시지 입력란 비우기
     */
    public void clearMessageText() {
        if (messageEditText != null) {
            messageEditText.setText("");
        }
    }
    
    /**
     * 메시지 입력란에 포커스 설정
     */
    public void focusMessageEditText() {
        if (messageEditText != null) {
            messageEditText.requestFocus();
        }
    }
    
    /**
     * 리사이클러뷰 맨 아래로 스크롤
     */
    public void scrollToBottom(int messageCount) {
        if (recyclerView != null && messageCount > 0) {
            recyclerView.smoothScrollToPosition(messageCount - 1);
        }
    }
    
    /**
     * 리사이클러뷰 특정 위치로 스크롤
     */
    public void scrollToPosition(int position) {
        if (recyclerView != null) {
            recyclerView.scrollToPosition(position);
        }
    }
    
    /**
     * 멤버 어댑터 업데이트
     */
    public void updateMemberAdapter(List<User> memberList) {
        if (memberAdapter != null) {
            memberAdapter.updateMembers(memberList);
        }
    }
    
    /**
     * 오류 다이얼로그 표시
     */
    public void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();
    }
    
    /**
     * 확인 다이얼로그 표시
     */
    public void showConfirmDialog(String title, String message, 
                                Runnable onConfirm, Runnable onCancel) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", (dialog, which) -> {
                    if (onConfirm != null) onConfirm.run();
                })
                .setNegativeButton("취소", (dialog, which) -> {
                    if (onCancel != null) onCancel.run();
                })
                .show();
    }
    
    /**
     * 로딩 다이얼로그 표시
     */
    public AlertDialog showLoadingDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }
    
    /**
     * View 접근자들
     */
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }
    
    public EditText getMessageEditText() {
        return messageEditText;
    }
    
    public ImageButton getSendButton() {
        return sendButton;
    }
    
    public ImageButton getAttachButton() {
        return attachButton;
    }
    
    public ImageButton getSummaryButton() {
        return summaryButton;
    }
    
    public ImageButton getMenuButton() {
        return menuButton;
    }
    
    public BottomNavigationView getBottomNavigationView() {
        return bottomNavigationView;
    }
    
    public DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }
    
    public NavigationView getNavigationView() {
        return navigationView;
    }
    
    public RecyclerView getMembersRecyclerView() {
        return membersRecyclerView;
    }
    
    public View getRootView() {
        return rootView;
    }
} 