package com.example.smiti.model;

import java.util.Date;

public class Comment {
    private int id;
    private String content;
    private String authorEmail;
    private String authorName;
    private Date createdAt;
    private int postId;

    public Comment() {
    }

    public Comment(int id, String content, String authorEmail, String authorName, Date createdAt, int postId) {
        this.id = id;
        this.content = content;
        this.authorEmail = authorEmail;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.postId = postId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }
} 