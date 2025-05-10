package com.example.smiti.model;

import android.net.Uri;

/**
 * 업로드된 파일 정보를 담는 모델 클래스
 */
public class UploadedFile {
    private Uri fileUri;
    private String fileName;
    private long fileSize;
    private String mimeType;

    public UploadedFile(Uri fileUri, String fileName, long fileSize, String mimeType) {
        this.fileUri = fileUri;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    public Uri getFileUri() {
        return fileUri;
    }

    public void setFileUri(Uri fileUri) {
        this.fileUri = fileUri;
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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }
} 