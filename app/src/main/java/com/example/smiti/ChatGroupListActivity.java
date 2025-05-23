package com.example.smiti;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChatGroupListActivity extends AppCompatActivity {
    private static final String TAG = "ChatGroupListActivity";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String BASE_URL = "http://202.31.246.51:80";

    private RecyclerView recyclerView;
    private ChatGroupAdapter adapter;
    private List<ChatGroup> groupList;
    private BottomNavigationView bottomNavigationView;
    private String currentUserEmail;
    private TextView emptyStateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_group_list);

        // 사용자 정보 로드
        loadUserData();

        // 뷰 초기화
        recyclerView = findViewById(R.id.recyclerViewGroups);
        emptyStateTextView = findViewById(R.id.text_empty_state);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        groupList = new ArrayList<>();
        adapter = new ChatGroupAdapter(groupList);
        recyclerView.setAdapter(adapter);
        
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();

        // 그룹 목록 가져오기
        fetchGroupList();
    }

    private void loadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        currentUserEmail = sharedPreferences.getString(KEY_EMAIL, "user@example.com");
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_chat);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(ChatGroupListActivity.this, MainActivity.class));
                finish(); 
                return true;
            } else if (id == R.id.navigation_search) {
                startActivity(new Intent(ChatGroupListActivity.this, GroupSearchActivity.class));
                finish(); 
                return true;
            } else if (id == R.id.navigation_chat) {
                return true; // 현재 화면이므로 아무 작업 안함
            } else if (id == R.id.navigation_board) {
                startActivity(new Intent(ChatGroupListActivity.this, BoardActivity.class));
                finish(); 
                return true;
            } else if (id == R.id.navigation_profile) {
                startActivity(new Intent(ChatGroupListActivity.this, ProfileActivity.class));
                finish(); 
                return true;
            }
            return false;
        });
    }
    
    // 문자열 배열을 그룹으로 파싱하는 메소드 (my_groups 형식의 응답용)
    private void parseStringArrayAsGroups(JSONArray groupNamesArray, List<ChatGroup> groups) throws JSONException {
        for (int i = 0; i < groupNamesArray.length(); i++) {
            String groupName = groupNamesArray.getString(i);
            // 이름만 알고 나머지 정보는 없기 때문에 기본값으로 설정
            String id = String.valueOf(i); // 임시 ID
            String description = ""; // 설명 없음
            int memberCount = 0; // 멤버 수 정보 없음
            
            ChatGroup group = new ChatGroup(id, groupName, description, memberCount);
            groups.add(group);
        }
    }
    
    // 그룹 객체 배열 파싱 헬퍼 메소드
    private void parseGroupsArray(JSONArray groupsArray, List<ChatGroup> groups) throws JSONException {
        for (int i = 0; i < groupsArray.length(); i++) {
            JSONObject groupObject = groupsArray.getJSONObject(i);
            String id = groupObject.optString("id", "");
            String name = groupObject.optString("name", "무제 그룹");
            String description = groupObject.optString("description", "");
            int memberCount = groupObject.optInt("member_count", 0);
            
            // 필드 이름이 다를 경우를 대비한 대체 키도 확인
            if (id.isEmpty() && groupObject.has("group_id")) {
                id = groupObject.optString("group_id", "");
            }
            
            if (name.equals("무제 그룹") && groupObject.has("group_name")) {
                name = groupObject.optString("group_name", "무제 그룹");
            }
            
            if (memberCount == 0 && groupObject.has("members")) {
                Object membersObj = groupObject.get("members");
                if (membersObj instanceof JSONArray) {
                    memberCount = ((JSONArray) membersObj).length();
                } else if (membersObj instanceof Integer) {
                    memberCount = (Integer) membersObj;
                }
            }
            
            ChatGroup group = new ChatGroup(id, name, description, memberCount);
            groups.add(group);
        }
    }

    private void fetchGroupList() {
        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "/users/me/groups?email=" + currentUserEmail;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "그룹 목록 가져오기 실패", e);
                runOnUiThread(() -> {
                    Toast.makeText(ChatGroupListActivity.this, 
                            "그룹 목록을 가져오는데 실패했습니다", Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        Log.d(TAG, "서버 응답: " + responseData); // 응답 로깅
                        
                        List<ChatGroup> groups = new ArrayList<>();
                        
                        // 응답 형식 확인
                        if (responseData.trim().startsWith("[")) {
                            // 응답이 배열로 시작하는 경우
                            JSONArray groupsArray = new JSONArray(responseData);
                            parseStringArrayAsGroups(groupsArray, groups);
                        } else {
                            // 응답이 객체로 시작하는 경우
                            JSONObject jsonObject = new JSONObject(responseData);
                            
                            // my_groups 키 확인 (API 응답에서 확인된 형식)
                            if (jsonObject.has("my_groups")) {
                                JSONArray groupNamesArray = jsonObject.getJSONArray("my_groups");
                                parseStringArrayAsGroups(groupNamesArray, groups);
                            }
                            // groups 키가 있는지 확인
                            else if (jsonObject.has("groups")) {
                                JSONArray groupsArray = jsonObject.getJSONArray("groups");
                                parseGroupsArray(groupsArray, groups);
                            } 
                            // data 키가 있는지 확인
                            else if (jsonObject.has("data")) {
                                Object dataObj = jsonObject.get("data");
                                if (dataObj instanceof JSONArray) {
                                    parseGroupsArray((JSONArray) dataObj, groups);
                                } else if (dataObj instanceof JSONObject) {
                                    JSONObject dataObject = (JSONObject) dataObj;
                                    if (dataObject.has("groups")) {
                                        parseGroupsArray(dataObject.getJSONArray("groups"), groups);
                                    }
                                }
                            }
                            // 다른 키를 찾아서 처리
                            else {
                                // 최상위 객체의 모든 키를 순회하며 배열 찾기
                                Iterator<String> keys = jsonObject.keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    try {
                                        Object value = jsonObject.get(key);
                                        if (value instanceof JSONArray) {
                                            // 문자열 배열이면 parseStringArrayAsGroups 사용
                                            JSONArray array = (JSONArray) value;
                                            if (array.length() > 0 && array.opt(0) instanceof String) {
                                                parseStringArrayAsGroups(array, groups);
                                            } else {
                                                parseGroupsArray(array, groups);
                                            }
                                            break;
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "키 '" + key + "' 처리 중 오류", e);
                                    }
                                }
                            }
                        }

                        runOnUiThread(() -> {
                            groupList.clear();
                            groupList.addAll(groups);
                            adapter.notifyDataSetChanged();
                            
                            if (groups.isEmpty()) {
                                // 그룹이 없는 경우 메시지 표시
                                showEmptyState(true);
                            } else {
                                showEmptyState(false);
                            }
                        });

                    } catch (JSONException e) {
                        Log.e(TAG, "그룹 목록 파싱 오류", e);
                        runOnUiThread(() -> {
                            Toast.makeText(ChatGroupListActivity.this, 
                                    "데이터 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                            showEmptyState(true);
                        });
                    }
                } else {
                    Log.e(TAG, "그룹 목록 서버 오류: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(ChatGroupListActivity.this, 
                                "서버 오류: " + response.code(), Toast.LENGTH_SHORT).show();
                        showEmptyState(true);
                    });
                }
            }
        });
    }
    
    private void showEmptyState(boolean show) {
        if (show) {
            recyclerView.setVisibility(View.GONE);
            emptyStateTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateTextView.setVisibility(View.GONE);
        }
    }

    // 채팅 그룹 데이터 클래스
    private static class ChatGroup {
        private String id;
        private String name;
        private String description;
        private int memberCount;

        public ChatGroup(String id, String name, String description, int memberCount) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.memberCount = memberCount;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public int getMemberCount() {
            return memberCount;
        }
    }

    // 그룹 목록 어댑터
    private class ChatGroupAdapter extends RecyclerView.Adapter<ChatGroupAdapter.GroupViewHolder> {
        private List<ChatGroup> groups;

        public ChatGroupAdapter(List<ChatGroup> groups) {
            this.groups = groups;
        }

        @NonNull
        @Override
        public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_group, parent, false);
            return new GroupViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
            ChatGroup group = groups.get(position);
            holder.bind(group);
        }

        @Override
        public int getItemCount() {
            return groups.size();
        }

        class GroupViewHolder extends RecyclerView.ViewHolder {
            private TextView groupName;
            private TextView groupDescription;
            private TextView memberCount;
            private ImageView groupIcon;

            public GroupViewHolder(@NonNull View itemView) {
                super(itemView);
                groupName = itemView.findViewById(R.id.text_group_name);
                groupDescription = itemView.findViewById(R.id.text_group_description);
                memberCount = itemView.findViewById(R.id.text_member_count);
                groupIcon = itemView.findViewById(R.id.image_group);

                // 아이템 클릭 시 해당 그룹의 채팅방으로 이동
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ChatGroup selectedGroup = groups.get(position);
                        Intent intent = new Intent(ChatGroupListActivity.this, ChatActivity.class);
                        intent.putExtra("group_id", selectedGroup.getId());
                        intent.putExtra("group_name", selectedGroup.getName());
                        startActivity(intent);
                    }
                });
            }

            public void bind(ChatGroup group) {
                groupName.setText(group.getName());
                groupDescription.setText(group.getDescription());
                memberCount.setText(group.getMemberCount() + "명");
                // TODO: 그룹 아이콘 설정 (필요시)
            }
        }
    }
} 
