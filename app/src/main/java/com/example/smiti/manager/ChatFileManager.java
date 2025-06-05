package com.example.smiti.manager;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;

import com.example.smiti.WebSocketService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatFileManager {
    
    private static final String TAG = "ChatFileManager";
    
    private final Context context;
    private final ContentResolver contentResolver;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    
    public interface FileUploadCallback {
        void onFileReadSuccess(String fileName, byte[] fileBytes);
        void onFileReadFailed(String error);
        void onUploadProgress(String message);
        void onUploadSuccess(String message);
        void onUploadFailed(String error);
    }
      public ChatFileManager(Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
      /**
     * 파일 업로드 처리
     */
    public void uploadFile(Uri fileUri, FileUploadCallback callback) {
        // 파일 읽기 시작 알림
        callback.onUploadProgress("파일을 읽는 중...");
        
        try {
            String mimeType = contentResolver.getType(fileUri);
            String displayName = getFileDisplayName(fileUri);
            
            if (displayName == null || displayName.isEmpty()) {
                displayName = generateDefaultFileName(mimeType);
            }
            
            Log.d(TAG, "파일 업로드 시작: fileName=" + displayName + ", mimeType=" + mimeType);
            
            readFileToBytes(fileUri, displayName, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "파일 업로드 준비 오류: " + fileUri, e);
            callback.onFileReadFailed("파일 업로드 준비 중 오류 발생");
        }
    }    /**
     * 파일을 바이트 배열로 읽기 (백그라운드 스레드에서 실행)
     */
    private void readFileToBytes(Uri fileUri, String fileName, FileUploadCallback callback) {
        // ExecutorService를 사용하여 백그라운드에서 파일 읽기 작업 수행
        executorService.execute(() -> {
            try {
                Log.d(TAG, "백그라운드에서 파일 읽기 시작: " + fileName);
                
                InputStream inputStream = contentResolver.openInputStream(fileUri);
                if (inputStream == null) {
                    mainHandler.post(() -> callback.onFileReadFailed("파일을 읽을 수 없습니다"));
                    return;
                }
                
                byte[] fileBytes = readInputStreamToBytes(inputStream);
                inputStream.close();
                
                Log.d(TAG, "파일 읽기 완료: " + fileName + " (" + fileBytes.length + " bytes)");
                
                // UI 스레드에서 콜백 호출
                mainHandler.post(() -> callback.onFileReadSuccess(fileName, fileBytes));
                
            } catch (Exception e) {
                Log.e(TAG, "파일 읽기 오류", e);
                mainHandler.post(() -> callback.onFileReadFailed("파일 읽기 중 오류 발생: " + e.getMessage()));
            }
        });
    }
    
    /**
     * WebSocket을 통한 파일 전송
     */
    public void sendFileViaWebSocket(String fileName, byte[] fileBytes, 
                                   WebSocketService webSocketService, FileUploadCallback callback) {
        if (webSocketService == null || !webSocketService.isConnected()) {
            callback.onUploadFailed("서버 연결이 필요합니다");
            return;
        }
        
        try {
            JSONObject fileMessage = createFileMessage(fileName, fileBytes);
            
            Log.d(TAG, "WebSocket으로 파일 전송: " + fileName + " (" + fileBytes.length + " bytes)");
            
            webSocketService.sendMessage(fileMessage.toString());
            callback.onUploadProgress("파일 전송 중: " + fileName);
            
        } catch (JSONException e) {
            Log.e(TAG, "파일 전송 JSON 생성 오류", e);
            callback.onUploadFailed("파일 전송 준비 오류");
        } catch (Exception e) {
            Log.e(TAG, "파일 전송 오류", e);
            callback.onUploadFailed("파일 전송 실패");
        }
    }
    
    /**
     * 파일 메시지 JSON 생성
     */
    private JSONObject createFileMessage(String fileName, byte[] fileBytes) throws JSONException {
        JSONObject fileMessage = new JSONObject();
        JSONObject fileData = new JSONObject();
        
        fileData.put("filename", fileName);
        
        // 바이트 배열을 정수 배열로 변환 (JSON 호환)
        JSONArray contentArray = new JSONArray();
        for (byte b : fileBytes) {
            contentArray.put(b & 0xFF); // unsigned byte 변환
        }
        fileData.put("content", contentArray);
        
        fileMessage.put("file", fileData);
        
        return fileMessage;
    }
    
    /**
     * InputStream을 바이트 배열로 변환
     */
    private byte[] readInputStreamToBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        
        return buffer.toByteArray();
    }
    
    /**
     * 파일명 가져오기
     */
    private String getFileDisplayName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "파일명 가져오기 실패", e);
            }
        }
        return result;
    }
    
    /**
     * 기본 파일명 생성
     */
    private String generateDefaultFileName(String mimeType) {
        String fileName = "uploaded_file";
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                fileName = "uploaded_image." + getMimeExtension(mimeType);
            } else if (mimeType.equals("application/pdf")) {
                fileName = "uploaded_document.pdf";
            } else {
                fileName = "uploaded_file." + getMimeExtension(mimeType);
            }
        }
        return fileName;
    }
    
    /**
     * MIME 타입에서 확장자 추출
     */
    private String getMimeExtension(String mimeType) {
        switch (mimeType) {
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            case "application/pdf":
                return "pdf";
            case "text/plain":
                return "txt";
            default:
                return "dat";
        }
    }
    
    /**
     * 파일 URL에서 파일 타입 추론
     */
    public static String inferFileTypeFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "";
        }
        
        String lowerUrl = fileUrl.toLowerCase();
        
        // 이미지 파일 확장자 확인
        if (lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
            return "image";
        }
        // PDF 파일 확장자 확인
        else if (lowerUrl.endsWith(".pdf")) {
            return "pdf";
        }
        // 문서 파일 확장자 확인
        else if (lowerUrl.matches(".*\\.(doc|docx|ppt|pptx|xls|xlsx|txt)$")) {
            return "document";
        }
        // 비디오 파일 확장자 확인
        else if (lowerUrl.matches(".*\\.(mp4|avi|mov|wmv|flv|mkv)$")) {
            return "video";
        }
        // 오디오 파일 확장자 확인
        else if (lowerUrl.matches(".*\\.(mp3|wav|ogg|aac|flac)$")) {
            return "audio";
        }
        // 압축 파일 확장자 확인
        else if (lowerUrl.matches(".*\\.(zip|rar|7z|tar|gz)$")) {
            return "archive";
        }
        
        // 기본값: 일반 파일
        return "file";
    }
    
    /**
     * 파일명에서 확장자 추출
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex);
        }
        return "";
    }
    
    /**
     * 파일 크기를 사람이 읽기 쉬운 형태로 변환
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * 지원되는 파일 타입인지 확인
     */
    public static boolean isSupportedFileType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        
        return mimeType.startsWith("image/") ||
               mimeType.startsWith("text/") ||
               mimeType.equals("application/pdf") ||
               mimeType.startsWith("audio/") ||
               mimeType.startsWith("video/") ||
               mimeType.startsWith("application/");
    }
      /**
     * 안전한 InputStream 닫기
     */
    public static void closeStream(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(TAG, "InputStream 닫기 오류", e);
            }
        }
    }
    
    /**
     * 리소스 정리
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "ExecutorService 종료됨");
        }
    }
}
