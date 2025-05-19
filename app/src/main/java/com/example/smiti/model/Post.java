package com.example.smiti.model;

import java.io.Serializable;
import java.util.Date;

public class Post implements Serializable {
    private String id;
    private String title;
    private String content;
    private String authorId;
    private String authorName;
    private String category;
    private Date createdAt;
    private Date updatedAt;
    private String fileUrl;
    private String fileName;
    private long fileSize;
    private boolean isNotice;
    private int likeCount; // 좋아요 수
    private int dislikeCount; // 싫어요 수
    private int commentCount; // 댓글 수
    private int viewCount; // 조회수

    // 카테고리 상수
    public static final String CATEGORY_FREE = "자유";
    public static final String CATEGORY_QUESTION = "질문";
    public static final String CATEGORY_SHARE = "공유";
    public static final String CATEGORY_NOTICE = "공지";

    // 기본 생성자
    public Post() {
        // 서버에서 받은 시간 사용을 위해 로컬 시간 초기화 제거
        this.likeCount = 0;
        this.dislikeCount = 0;
        this.commentCount = 0;
        this.viewCount = 0;
    }

    // 파일 없는 게시글 생성 생성자
    public Post(String title, String content, String authorId, String authorName, String category, boolean isNotice) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.category = category;
        // 서버에서 받은 시간 사용을 위해 로컬 시간 초기화 제거
        this.isNotice = isNotice;
        this.likeCount = 0;
        this.dislikeCount = 0;
        this.commentCount = 0;
        this.viewCount = 0;
    }

    // 파일 있는 게시글 생성 생성자
    public Post(String title, String content, String authorId, String authorName, String category,
                String fileUrl, String fileName, long fileSize, boolean isNotice) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.category = category;
        // 서버에서 받은 시간 사용을 위해 로컬 시간 초기화 제거
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.isNotice = isNotice;
        this.likeCount = 0;
        this.dislikeCount = 0;
        this.commentCount = 0;
        this.viewCount = 0;
    }

    // 게터와 세터
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public boolean isNotice() {
        return isNotice;
    }

    public void setNotice(boolean notice) {
        isNotice = notice;
    }

    public boolean hasFile() {
        return fileUrl != null && !fileUrl.isEmpty();
    }

    // 파일 크기를 읽기 쉽게 반환하는 메서드
    public String getReadableFileSize() {
        if (fileSize <= 0) return "0 B";

        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(fileSize) / Math.log10(1024));

        return String.format("%.1f %s", fileSize / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(int dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    @Override
    public String toString() {
        return "Post{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", authorId='" + authorId + '\'' +
                ", authorName='" + authorName + '\'' +
                ", category='" + category + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", fileUrl='" + fileUrl + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", isNotice=" + isNotice +
                ", likeCount=" + likeCount +
                ", dislikeCount=" + dislikeCount +
                ", commentCount=" + commentCount +
                ", viewCount=" + viewCount +
                '}';
    }
}
