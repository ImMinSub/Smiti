package com.example.smiti.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import com.example.smiti.model.Group;
// import com.example.smiti.model.Comment; // 현재 이 클래스는 사용되지 않으므로 주석 처리 또는 제거 가능

// LoginResponse 클래스를 임포트해야 합니다.
// LoginResponse.java 파일이 com.example.smiti.network 패키지에 있다고 가정합니다.
// 실제 위치에 맞게 수정해주세요.
import com.example.smiti.network.LoginResponse;


public class ApiResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Object data; // 다른 API 응답에서 사용될 수 있는 일반적인 data 필드

    @SerializedName("posts")
    private List<Map<String, Object>> posts;

    @SerializedName("groups")
    private List<Group> groups;

    @SerializedName("post")
    private Map<String, Object> post;

    @SerializedName("post_id")
    private Integer postId;

    @SerializedName("available_times")
    private Map<String, List<String>> availableTimes;

    @SerializedName("comments")
    private List<Map<String, Object>> comments;

    @SerializedName("comment")
    private Map<String, Object> newComment;

    // 로그인 응답의 "user" 객체를 담을 필드 추가
    @SerializedName("user")
    private LoginResponse user; // LoginResponse 타입으로 선언

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    // getUser() 메소드 추가
    public LoginResponse getUser() {
        return user;
    }

    // setUser() 메소드 추가 (선택 사항)
    public void setUser(LoginResponse user) {
        this.user = user;
    }

    public Object getData() {
        // 만약 다른 API에서 'data' 필드에 사용자 정보가 오는 경우가 있다면,
        // 여기서도 LoginResponse 타입인지 확인하고 반환하는 로직을 추가할 수 있습니다.
        // 하지만 로그인 API에서는 'user' 필드를 직접 사용하는 것이 더 명확합니다.

        // 기존 getData() 로직 유지 (다른 API 호환성)
        if (postId != null) {
            return postId;
        }
        if (post != null) {
            return post;
        }
        if (posts != null) {
            return posts;
        }
        return data;
    }

    public List<Map<String, Object>> getPosts() {
        return posts;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public Map<String, Object> getPost() {
        return post;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public List<Map<String, Object>> getComments() {
        return comments;
    }

    public Map<String, Object> getNewComment() {
        return newComment;
    }

    public Object getRawData() {
        return this;
    }

    public boolean isSuccess() {
        // isSuccess 로직은 현재 상태로도 동작할 수 있지만,
        // 로그인 성공 여부는 주로 status나 message, 또는 user 객체의 존재 여부로 판단하는 것이 더 명확할 수 있습니다.
        // 예를 들어, 로그인 API에서는 user 객체가 null이 아니면 성공으로 간주할 수 있습니다.
        return "success".equalsIgnoreCase(status) || // status 값을 대소문자 구분 없이 비교
                (message != null && message.toLowerCase().contains("successful")) || // message에 "successful" 포함 여부
                user != null || // 로그인 응답의 경우 user 객체가 있으면 성공
                posts != null ||
                data != null ||
                groups != null ||
                post != null ||
                postId != null ||
                comments != null ||
                newComment != null;
    }

    public Map<String, List<String>> getAvailableTimes() {
        return availableTimes;
    }

    public void setAvailableTimes(Map<String, List<String>> availableTimes) {
        this.availableTimes = availableTimes;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", data=" + (data != null ? "not null" : "null") +
                ", user=" + (user != null ? user.toString() : "null") + // user 정보 추가
                ", posts=" + (posts != null ? posts.size() + " items" : "null") +
                ", groups=" + (groups != null ? groups.size() + " items" : "null") +
                ", post=" + (post != null ? "not null" : "null") +
                ", post_id=" + (postId != null ? postId : "null") +
                ", comments=" + (comments != null ? comments.size() + " items" : "null") +
                ", newComment=" + (newComment != null ? newComment.toString() : "null") +
                '}';
    }
}
