package com.example.smiti.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.smiti.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 메시지 영속성을 위한 SQLite 데이터베이스
 */
public class MessageDatabase extends SQLiteOpenHelper {
    private static final String TAG = "MessageDatabase";
    private static final String DB_NAME = "smiti_messages.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_MESSAGES = "messages";
    
    // 컬럼 정의
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_GROUP_ID = "group_id";
    private static final String COLUMN_SENDER_ID = "sender_id";
    private static final String COLUMN_SENDER_NAME = "sender_name";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_FILE_URL = "file_url";
    private static final String COLUMN_FILE_TYPE = "file_type";
    
    private static volatile MessageDatabase INSTANCE;
    
    public static MessageDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MessageDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MessageDatabase(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }
    
    private MessageDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_MESSAGES + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_GROUP_ID + " TEXT NOT NULL, "
            + COLUMN_SENDER_ID + " TEXT NOT NULL, "
            + COLUMN_SENDER_NAME + " TEXT, "
            + COLUMN_MESSAGE + " TEXT, "
            + COLUMN_TIMESTAMP + " INTEGER NOT NULL, "
            + COLUMN_FILE_URL + " TEXT, "
            + COLUMN_FILE_TYPE + " TEXT"
            + ");";
            
        db.execSQL(createTable);
        
        // 인덱스 생성 (성능 향상)
        db.execSQL("CREATE INDEX idx_messages_group_id ON " + TABLE_MESSAGES 
            + "(" + COLUMN_GROUP_ID + ");");
        db.execSQL("CREATE INDEX idx_messages_timestamp ON " + TABLE_MESSAGES 
            + "(" + COLUMN_TIMESTAMP + ");");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "데이터베이스 업그레이드: " + oldVersion + " -> " + newVersion);
        
        // 버전별 마이그레이션 처리
        if (oldVersion < 2) {
            // 버전 2로 업그레이드 시 필요한 작업
            // 예: 새 컬럼 추가, 인덱스 추가 등
        }
        
        // 현재는 단순히 테이블 재생성 (개발 단계)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }
    
    /**
     * 데이터베이스 무결성 검사
     */
    public boolean checkDatabaseIntegrity() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("PRAGMA integrity_check", null);
        
        boolean isIntact = false;
        if (cursor.moveToFirst()) {
            String result = cursor.getString(0);
            isIntact = "ok".equals(result);
        }
        cursor.close();
        
        Log.d(TAG, "데이터베이스 무결성 검사: " + (isIntact ? "정상" : "손상"));
        return isIntact;
    }
    
    /**
     * 특정 그룹의 모든 메시지를 시간순으로 가져옵니다.
     */
    public List<Message> getMessagesForGroup(String groupId) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = COLUMN_GROUP_ID + " = ?";
        String[] selectionArgs = {groupId};
        String orderBy = COLUMN_TIMESTAMP + " ASC";
        
        Cursor cursor = db.query(
            TABLE_MESSAGES,
            null,
            selection,
            selectionArgs,
            null,
            null,
            orderBy
        );
        
        try {
            while (cursor.moveToNext()) {
                Message message = cursorToMessage(cursor);
                messages.add(message);
            }
            Log.d(TAG, "그룹 " + groupId + "의 메시지 " + messages.size() + "개 로드됨");
        } finally {
            cursor.close();
        }
        
        return messages;
    }
    
    /**
     * 새 메시지를 데이터베이스에 저장합니다.
     */
    public long insertMessage(String groupId, Message message) {
        // 중복 메시지 체크 (같은 그룹, 발신자, 내용, 시간대가 비슷한 메시지)
        if (isDuplicateMessage(groupId, message)) {
            Log.d(TAG, "중복 메시지 감지, 저장 건너뜀: " + message.getMessage());
            return -1;
        }
        
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = messageToContentValues(groupId, message);
        
        long result = db.insert(TABLE_MESSAGES, null, values);
        if (result != -1) {
            Log.d(TAG, "메시지 저장됨: groupId=" + groupId + ", senderId=" + message.getSenderId());
        } else {
            Log.e(TAG, "메시지 저장 실패");
        }
        
        return result;
    }
    
    /**
     * 중복 메시지인지 확인합니다. - 완화된 조건
     */
    private boolean isDuplicateMessage(String groupId, Message message) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        // 파일 메시지의 경우 더 엄격한 기준으로 체크
        if (message.hasFile() && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
            // 같은 파일 URL을 가진 메시지가 이미 있는지 확인
            String selection = COLUMN_GROUP_ID + " = ? AND " +
                              COLUMN_SENDER_ID + " = ? AND " +
                              COLUMN_FILE_URL + " = ?";
            
            String[] selectionArgs = {
                groupId,
                message.getSenderId(),
                message.getFileUrl()
            };
            
            Cursor cursor = db.query(
                TABLE_MESSAGES,
                new String[]{COLUMN_ID},
                selection,
                selectionArgs,
                null,
                null,
                null,
                "1"
            );
            
            boolean isDuplicate = cursor.getCount() > 0;
            cursor.close();
            
            if (isDuplicate) {
                Log.d(TAG, "중복 파일 메시지 감지: " + message.getFileUrl());
            }
            
            return isDuplicate;
        }
        
        // 텍스트 메시지의 경우 시간 기준을 더 짧게 (3초)
        long timeThreshold = message.getTimestamp() - 3000; // 3초 전
        
        String selection = COLUMN_GROUP_ID + " = ? AND " +
                          COLUMN_SENDER_ID + " = ? AND " +
                          COLUMN_MESSAGE + " = ? AND " +
                          COLUMN_TIMESTAMP + " > ? AND " +
                          COLUMN_FILE_URL + " IS NULL"; // 파일이 없는 텍스트 메시지만
        
        String[] selectionArgs = {
            groupId,
            message.getSenderId(),
            message.getMessage(),
            String.valueOf(timeThreshold)
        };
        
        Cursor cursor = db.query(
            TABLE_MESSAGES,
            new String[]{COLUMN_ID},
            selection,
            selectionArgs,
            null,
            null,
            null,
            "1"
        );
        
        boolean isDuplicate = cursor.getCount() > 0;
        cursor.close();
        
        if (isDuplicate) {
            Log.d(TAG, "중복 텍스트 메시지 감지: " + message.getMessage());
        }
        
        return isDuplicate;
    }
    
    /**
     * 특정 그룹의 메시지 개수를 반환합니다.
     */
    public int getMessageCountForGroup(String groupId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_GROUP_ID + " = ?";
        String[] selectionArgs = {groupId};
        
        Cursor cursor = db.query(
            TABLE_MESSAGES,
            new String[]{"COUNT(*) as count"},
            selection,
            selectionArgs,
            null,
            null,
            null
        );
        
        int count = 0;
        try {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            cursor.close();
        }
        
        return count;
    }
    
    /**
     * 오래된 메시지를 삭제합니다 (성능 최적화용)
     */
    public void deleteOldMessages(String groupId, int keepCount) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // 최신 keepCount개를 제외한 나머지 삭제
        String deleteQuery = "DELETE FROM " + TABLE_MESSAGES + 
            " WHERE " + COLUMN_GROUP_ID + " = ? " +
            " AND " + COLUMN_ID + " NOT IN (" +
            "   SELECT " + COLUMN_ID + " FROM " + TABLE_MESSAGES +
            "   WHERE " + COLUMN_GROUP_ID + " = ? " +
            "   ORDER BY " + COLUMN_TIMESTAMP + " DESC " +
            "   LIMIT ?" +
            ")";
        
        db.execSQL(deleteQuery, new Object[]{groupId, groupId, keepCount});
    }
    
    /**
     * Cursor에서 Message 객체로 변환합니다.
     */
    private Message cursorToMessage(Cursor cursor) {
        String senderId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_ID));
        String senderName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_NAME));
        String messageText = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE));
        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
        
        String fileUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_URL));
        String fileType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_TYPE));
        
        // 파일이 있는 경우와 없는 경우 구분
        if (fileUrl != null && !fileUrl.isEmpty()) {
            return new Message(senderId, senderName, messageText, timestamp, fileUrl, fileType);
        } else {
            return new Message(senderId, senderName, messageText, timestamp);
        }
    }
    
    /**
     * Message 객체에서 ContentValues로 변환합니다.
     */
    private ContentValues messageToContentValues(String groupId, Message message) {
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_GROUP_ID, groupId);
        values.put(COLUMN_SENDER_ID, message.getSenderId());
        values.put(COLUMN_SENDER_NAME, message.getSenderName());
        values.put(COLUMN_MESSAGE, message.getMessage());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        
        // 파일 정보가 있는 경우 추가
        if (message.hasFile()) {
            values.put(COLUMN_FILE_URL, message.getFileUrl());
            values.put(COLUMN_FILE_TYPE, message.getFileType());
        }
        
        return values;
    }
}
