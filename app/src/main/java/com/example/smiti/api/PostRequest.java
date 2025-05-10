package com.example.smiti.api;

import com.google.gson.annotations.SerializedName;

public class PostRequest {
    // 필드 이름을 서버가 기대하는 이름과 정확히 일치하도록 수정
    @SerializedName("id")
    private Integer id;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("board_type")
    private String board_type;
    
    @SerializedName("notice")
    private Boolean notice;

    // 게시글 생성 시 사용되는 생성자
    public PostRequest(String email, String title, String content, String board_type, boolean notice) {
        this.email = email;
        this.title = title;
        this.content = content;
        this.board_type = board_type;
        this.notice = notice;
    }

    // 게시글 수정 시 사용되는 생성자
    public PostRequest(Integer id, String email, String title, String content) {
        this.id = id;
        this.email = email;
        this.title = title;
        this.content = content;
    }

    // 특정 게시글 조회 시 사용되는 생성자
    public PostRequest(Integer id) {
        this.id = id;
    }

    // 게시글 삭제 시 사용되는 생성자
    public PostRequest(Integer id, String email) {
        this.id = id;
        this.email = email;
    }

    // 게시판 유형별 조회 시 사용되는 생성자
    public PostRequest(String board_type) {
        this.board_type = board_type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getBoard_type() {
        return board_type;
    }

    public void setBoard_type(String board_type) {
        this.board_type = board_type;
    }

    public Boolean getNotice() {
        return notice;
    }

    public void setNotice(Boolean notice) {
        this.notice = notice;
    }
    
    @Override
    public String toString() {
        return "PostRequest{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", board_type='" + board_type + '\'' +
                ", notice=" + notice +
                '}';
    }
} 