package com.example.smiti.model;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class ApiResponse {
    // API 응답에 포함될 수 있는 필드들을 정의합니다.
    // 그룹 목록이 'groups'라는 필드로 온다고 가정하고 작성합니다.
    @SerializedName("groups")
    private List<Group> groups;

    // 기타 응답 필드 (예: 상태 코드, 메시지 등)가 있다면 추가합니다.
    // @SerializedName("success")
    // private boolean success;
    // @SerializedName("message")
    // private String message;

    // 그룹 목록을 가져오는 Getter 메소드
    public List<Group> getGroups() {
        return groups;
    }

    // 필요에 따라 다른 필드들의 Getter 메소드를 추가합니다.
    // public boolean isSuccess() {
    //     return success;
    // }
    // public String getMessage() {
    //     return message;
    // }

    // Gson이 객체를 생성하기 위한 기본 생성자 (필요할 수 있습니다)
    public ApiResponse() {
    }
} 