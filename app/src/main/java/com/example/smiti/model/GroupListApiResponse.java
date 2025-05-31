package com.example.smiti.model;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class GroupListApiResponse {
    // API 응답에서 그룹 목록이 'groups'라는 필드로 온다고 가정하고 작성합니다.
    @SerializedName("groups")
    private List<GroupAlternate> groups;

    // 기타 응답 필드 (예: 상태 코드, 메시지 등)가 있다면 추가합니다.
    // @SerializedName("success")
    // private boolean success;
    // @SerializedName("message")
    // private String message;

    // 그룹 목록을 가져오는 Getter 메소드
    public List<GroupAlternate> getGroups() {
        return groups;
    }

    // 필요에 따라 다른 필드들의 Getter 메소드를 추가합니다.

    // Gson이 객체를 생성하기 위한 기본 생성자
    public GroupListApiResponse() {
    }
} 