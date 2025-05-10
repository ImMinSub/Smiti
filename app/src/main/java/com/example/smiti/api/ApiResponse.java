package com.example.smiti.api;
//서버로부터 받는 응답을 표현하는 모델델
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import com.example.smiti.model.Group;
import com.example.smiti.model.Comment;

public class ApiResponse {
    @SerializedName("status")
    private String status;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("data")
    private Object data;
    
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
    
    public String getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Object getData() {
        // post_id가 있으면 가장 먼저 반환
        if (postId != null) {
            return postId;
        }
        // post 필드가 있으면 두 번째로 반환
        if (post != null) {
            return post;
        }
        // posts 필드가 있으면 세 번째로 반환
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
    
    // 원본 데이터(JSON 구조 전체)에 직접 접근하기 위한 메서드
    public Object getRawData() {
        return this;
    }
    
    // success 여부 확인 메서드 추가
    public boolean isSuccess() {
        return "success".equals(status) || 
               posts != null || 
               data != null || 
               groups != null || 
               post != null || 
               postId != null ||
               comments != null;  // comments가 있으면 성공으로 간주
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
                ", posts=" + (posts != null ? posts.size() + " items" : "null") +
                ", groups=" + (groups != null ? groups.size() + " items" : "null") +
                ", post=" + (post != null ? "not null" : "null") +
                ", post_id=" + (postId != null ? postId : "null") +
                ", comments=" + (comments != null ? comments.size() + " items" : "null") +
                '}';
    }
} 