package com.example.smiti;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.cardview.widget.CardView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone; // TimeZone import 추가

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "MessageAdapter";
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_MESSAGE_WITH_BUTTONS = 3; // 버튼이 있는 메시지 유형 추가
    private static final int VIEW_TYPE_DATE_SEPARATOR = 4; // 날짜 구분선 유형 추가
    private static final int VIEW_TYPE_LOADING_INDICATOR = 5; // 로딩 인디케이터 유형 추가

    private Context context;
    private List<Message> originalMessageList; // 원본 메시지 리스트
    private List<Object> displayList; // 메시지와 날짜 구분선을 포함한 표시 리스트
    private String currentUserIdentifier; // 현재 사용자 식별자 (이메일)
    private OnQuestionButtonClickListener buttonClickListener; // 버튼 클릭 리스너 추가
    
    // 로딩 상태 관리
    private boolean showLoadingIndicator = false;
    private String loadingMessage = "이전 메시지를 불러오는 중...";

    // 한국 시간대(KST)를 위한 TimeZone 객체
    private static final TimeZone KST = TimeZone.getTimeZone("Asia/Seoul");

    // 버튼 클릭 이벤트를 처리하기 위한 인터페이스
    public interface OnQuestionButtonClickListener {
        void onQuestionButtonClick(String question);
    }

    public MessageAdapter(Context context, List<Message> messageList, String currentUserIdentifier) {
        this.context = context;
        this.originalMessageList = messageList;
        this.displayList = new ArrayList<>();
        this.currentUserIdentifier = currentUserIdentifier;
        Log.d(TAG, "어댑터 초기화: currentUserIdentifier=" + currentUserIdentifier);
        
        // 초기 표시 리스트 생성
        updateDisplayList();
    }

    // 버튼 클릭 리스너 설정 메서드
    public void setOnQuestionButtonClickListener(OnQuestionButtonClickListener listener) {
        this.buttonClickListener = listener;
    }

    // 현재 사용자 식별자 업데이트 메서드
    public void updateCurrentUserIdentifier(String identifier) {
        Log.d(TAG, "현재 사용자 식별자 업데이트: " + identifier);
        this.currentUserIdentifier = identifier;
        notifyDataSetChanged(); // 데이터 변경 알림
    }
    
    // 로딩 인디케이터 표시/숨김
    public void showLoadingIndicator(String message) {
        if (!showLoadingIndicator) {
            showLoadingIndicator = true;
            loadingMessage = message != null ? message : "이전 메시지를 불러오는 중...";
            updateDisplayList();
            notifyItemInserted(0); // 상단에 로딩 인디케이터 추가
        }
    }
    
    public void hideLoadingIndicator() {
        if (showLoadingIndicator) {
            showLoadingIndicator = false;
            updateDisplayList();
            notifyItemRemoved(0); // 상단의 로딩 인디케이터 제거
        }
    }

    // 표시 리스트 업데이트 (날짜 구분선 및 로딩 인디케이터 포함) - 개선된 버전
    private void updateDisplayList() {
        displayList.clear();
        
        // 로딩 인디케이터가 표시되어야 하면 맨 위에 추가
        if (showLoadingIndicator) {
            displayList.add("LOADING_INDICATOR");
        }
        
        if (originalMessageList.isEmpty()) {
            return;
        }
        
        // 메시지를 시간순으로 정렬 (안전장치)
        Collections.sort(originalMessageList, (m1, m2) -> 
            Long.compare(m1.getTimestamp(), m2.getTimestamp()));
        
        String lastDateString = "";
        
        for (int i = 0; i < originalMessageList.size(); i++) {
            Message message = originalMessageList.get(i);
            long timestamp = message.getTimestamp();
            
            // 메시지의 날짜 문자열 생성
            String currentDateString = formatDateForSeparator(timestamp);
            
            // 날짜가 바뀌었으면 날짜 구분선 추가
            if (!currentDateString.equals(lastDateString)) {
                displayList.add(new DateSeparatorItem(currentDateString, timestamp));
                lastDateString = currentDateString;
            }
            
            // 메시지 추가
            displayList.add(message);
        }
    }
    
    // 날짜 구분선용 날짜 포맷팅
    private String formatDateForSeparator(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        
        Calendar messageCalendar = Calendar.getInstance(KST);
        messageCalendar.setTimeInMillis(timestamp);
        
        Calendar today = Calendar.getInstance(KST);
        Calendar yesterday = Calendar.getInstance(KST);
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        
        // 오늘인지 확인
        if (isSameDay(messageCalendar, today)) {
            return "오늘";
        }
        // 어제인지 확인
        else if (isSameDay(messageCalendar, yesterday)) {
            return "어제";
        }
        // 그 외의 경우 날짜 표시
        else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA);
            sdf.setTimeZone(KST);
            return sdf.format(new Date(timestamp));
        }
    }
    
    // 두 Calendar가 같은 날인지 확인
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    // 첫 번째 메시지인지 확인하는 헬퍼 메서드 - 수정된 버전
    private boolean isFirstMessage(Message targetMessage) {
        if (originalMessageList == null || originalMessageList.isEmpty()) {
            return false;
        }
        
        // 시간순으로 정렬된 첫 번째 메시지인지 확인
        Message firstMessage = originalMessageList.get(0);
        
        // 동일한 메시지 객체이거나 같은 내용과 타임스탬프를 가진 메시지인지 확인
        if (targetMessage == firstMessage) {
            return true;
        }
        
        // 메시지 내용과 타임스탬프가 같은지 확인 (동일한 메시지의 다른 인스턴스일 가능성)
        if (targetMessage != null && firstMessage != null) {
            return targetMessage.getTimestamp() == firstMessage.getTimestamp() &&
                   java.util.Objects.equals(targetMessage.getMessage(), firstMessage.getMessage()) &&
                   java.util.Objects.equals(targetMessage.getSenderId(), firstMessage.getSenderId());
        }
        
        return false;
    }

    @Override
    public int getItemViewType(int position) {
        // 안전장치: position이 유효한지 확인
        if (position < 0 || position >= displayList.size()) {
            Log.w(TAG, "잘못된 position: " + position + ", displayList.size(): " + displayList.size());
            return VIEW_TYPE_MESSAGE_RECEIVED; // 기본값 반환
        }
        
        Object item = displayList.get(position);
        
        // 스크롤 시 과도한 로그 출력 방지 - 디버깅 필요시에만 활성화
        // Log.d(TAG, "=== getItemViewType 상세 로그 ===");
        // Log.d(TAG, "position: " + position);
        // Log.d(TAG, "item type: " + item.getClass().getSimpleName());
        
        // 로딩 인디케이터인 경우
        if (item instanceof String && "LOADING_INDICATOR".equals(item)) {
            return VIEW_TYPE_LOADING_INDICATOR;
        }
        
        // 날짜 구분선인 경우
        if (item instanceof DateSeparatorItem) {
            return VIEW_TYPE_DATE_SEPARATOR;
        }
        
        // 메시지인 경우
        if (!(item instanceof Message)) {
            Log.w(TAG, "예상치 못한 아이템 타입: " + item.getClass().getSimpleName());
            return VIEW_TYPE_MESSAGE_RECEIVED; // 기본값 반환
        }
        
        Message message = (Message) item;
        String senderId = message.getSenderId(); // 메시지 발신자 ID (이메일)
        String senderName = message.getSenderName(); // 메시지 발신자 이름

        // 스크롤 시 과도한 로그 출력 방지 - 디버깅 필요시에만 활성화
        // Log.d(TAG, "메시지 정보:");
        // Log.d(TAG, "  senderId: [" + senderId + "]");
        // Log.d(TAG, "  senderName: [" + senderName + "]");
        // Log.d(TAG, "  message: [" + message.getMessage() + "]");

        // 시스템 또는 봇 메시지인 경우 버튼 표시
        if (("system".equals(senderId) || "bot".equals(senderId))) {
            return VIEW_TYPE_MESSAGE_WITH_BUTTONS;
        }
        
        // 현재 사용자 메시지인지 정확히 판단 (강화된 로직)
        boolean isCurrentUser = isCurrentUserMessage(senderId, senderName);
        
        if (isCurrentUser) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }
    
    // 현재 사용자 메시지인지 정확히 판단하는 메서드 (강화된 버전)
    private boolean isCurrentUserMessage(String senderId, String senderName) {
        if (currentUserIdentifier == null || currentUserIdentifier.isEmpty()) {
            return false;
        }
        
        // 1. 정확한 이메일 매칭 (대소문자 무시)
        if (senderId != null && currentUserIdentifier.equalsIgnoreCase(senderId.trim())) {
            return true;
        }
        
        // 2. 이메일 앞부분 매칭 (@ 앞부분)
        if (senderId != null && currentUserIdentifier.contains("@")) {
            String emailPrefix = currentUserIdentifier.split("@")[0];
            if (emailPrefix.equalsIgnoreCase(senderId.trim())) {
                return true;
            }
        }
        
        // 3. 발신자명으로 판단 - 강화된 로직
        if (senderName != null) {
            String normalizedName = senderName.trim().toLowerCase();
            
            // "나" 또는 "me" 키워드 확인
            if (normalizedName.equals("나") || normalizedName.equals("me") || normalizedName.equals("myself")) {
                return true;
            }
            
            // 현재 사용자 이메일의 앞부분과 비교
            if (currentUserIdentifier.contains("@")) {
                String emailPrefix = currentUserIdentifier.split("@")[0];
                if (emailPrefix.equalsIgnoreCase(senderName.trim())) {
                    return true;
                }
            }
        }
        
        // 4. 부분 문자열 매칭 (더 관대한 방식)
        if (senderId != null && currentUserIdentifier != null) {
            String cleanSenderId = senderId.replaceAll("\\s+", "").toLowerCase();
            String cleanCurrentId = currentUserIdentifier.replaceAll("\\s+", "").toLowerCase();
            
            if (cleanCurrentId.contains(cleanSenderId) || cleanSenderId.contains(cleanCurrentId)) {
                return true;
            }
        }
        
        return false;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        // 뷰 타입에 따라 다른 레이아웃 inflate
        if (viewType == VIEW_TYPE_LOADING_INDICATOR) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading_indicator, parent, false);
            return new LoadingIndicatorHolder(view);
        } else if (viewType == VIEW_TYPE_DATE_SEPARATOR) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_separator, parent, false);
            return new DateSeparatorHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_WITH_BUTTONS) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_with_buttons, parent, false);
            return new ButtonMessageHolder(view);
        } else { // VIEW_TYPE_MESSAGE_RECEIVED
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // 안전장치: position이 유효한지 확인
        if (position < 0 || position >= displayList.size()) {
            Log.w(TAG, "onBindViewHolder - 잘못된 position: " + position + ", displayList.size(): " + displayList.size());
            return;
        }
        
        Object item = displayList.get(position);
        
        // ViewHolder 타입에 따라 데이터 바인딩
        if (holder instanceof LoadingIndicatorHolder) {
            ((LoadingIndicatorHolder) holder).bind(loadingMessage);
        } else if (holder instanceof DateSeparatorHolder) {
            if (item instanceof DateSeparatorItem) {
                ((DateSeparatorHolder) holder).bind((DateSeparatorItem) item);
            }
        } else if (holder instanceof SentMessageHolder) {
            if (item instanceof Message) {
                ((SentMessageHolder) holder).bind((Message) item);
            }
        } else if (holder instanceof ReceivedMessageHolder) {
            if (item instanceof Message) {
                ((ReceivedMessageHolder) holder).bind((Message) item);
            }
        } else if (holder instanceof ButtonMessageHolder) {
            if (item instanceof Message) {
                ((ButtonMessageHolder) holder).bind((Message) item);
            }
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    // 메시지 리스트에 새 메시지 추가 및 UI 갱신 - 스레드 안전 버전
    public void addMessage(Message message) {
        if (message == null) {
            return;
        }
        
        // 중복 메시지인지 확인
        if (isDuplicateMessageEnhanced(message)) {
            // 중복 메시지 감지 로그 제거 - 스크롤 시 과도한 출력 방지
            return;
        }
        
        originalMessageList.add(message);
        
        updateDisplayList();
        notifyItemInserted(displayList.size() - 1);
    }
    
    // 메시지 내용이 동일한지 확인하는 메서드
    private boolean isSameMessageContent(Message msg1, Message msg2) {
        if (msg1 == null || msg2 == null) {
            return false;
        }
        
        // 메시지 내용 비교
        String content1 = msg1.getMessage();
        String content2 = msg2.getMessage();
        if (content1 != null) content1 = content1.trim();
        if (content2 != null) content2 = content2.trim();
        
        if (!java.util.Objects.equals(content1, content2)) {
            return false;
        }
        
        // 파일 URL 비교 (있는 경우)
        String fileUrl1 = msg1.getFileUrl();
        String fileUrl2 = msg2.getFileUrl();
        if (fileUrl1 != null) fileUrl1 = fileUrl1.trim();
        if (fileUrl2 != null) fileUrl2 = fileUrl2.trim();
        
        return java.util.Objects.equals(fileUrl1, fileUrl2);
    }

    // 강화된 중복 메시지 체크
    private boolean isDuplicateMessageEnhanced(Message newMessage) {
        String newMessageHash = generateMessageHashEnhanced(newMessage);
        
        for (Message existingMessage : originalMessageList) {
            String existingHash = generateMessageHashEnhanced(existingMessage);
            if (newMessageHash.equals(existingHash)) {
                // 해시 기반 중복 감지 로그 제거 - 스크롤 시 과도한 출력 방지
                return true;
            }
            
            // 정확한 메시지 비교
            if (isSameMessageExact(newMessage, existingMessage)) {
                // 정확한 비교 중복 감지 로그 제거
                return true;
            }
            
            // 유사 메시지 체크 (내용과 시간이 비슷한 경우)
            if (isSimilarMessage(newMessage, existingMessage)) {
                // 유사 메시지 중복 감지 로그 제거
                return true;
            }
        }
        return false;
    }
    
    // 향상된 메시지 고유 해시 생성
    private String generateMessageHashEnhanced(Message message) {
        StringBuilder hashBuilder = new StringBuilder();
        
        // 타임스탬프 (밀리초 단위)
        hashBuilder.append(message.getTimestamp());
        hashBuilder.append("_");
        
        // 발신자 ID (정규화) - 현재 사용자 메시지는 통일
        String senderId = message.getSenderId();
        if (senderId != null) {
            senderId = senderId.trim().toLowerCase();
            // 현재 사용자 메시지인 경우 이메일로 통일
            if (isCurrentUserMessage(senderId, message.getSenderName())) {
                senderId = currentUserIdentifier != null ? currentUserIdentifier.toLowerCase() : senderId;
            }
        }
        hashBuilder.append(senderId != null ? senderId : "unknown");
        hashBuilder.append("_");
        
        // 메시지 내용 (정규화)
        String content = message.getMessage();
        if (content != null) {
            content = content.trim();
        }
        hashBuilder.append(content != null ? content.hashCode() : 0);
        
        // 파일 정보 처리 개선 - 파일 메시지는 URL을 주요 식별자로 사용
        if (message.hasFile() && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
            hashBuilder.append("_FILE_");
            hashBuilder.append(message.getFileUrl().trim()); // URL 전체 사용
            if (message.getFileType() != null && !message.getFileType().isEmpty()) {
                hashBuilder.append("_");
                hashBuilder.append(message.getFileType().trim());
            }
        } else {
            hashBuilder.append("_TEXT");
        }
        
        return hashBuilder.toString();
    }
    
    // 두 메시지가 정확히 동일한지 확인
    private boolean isSameMessageExact(Message msg1, Message msg2) {
        if (msg1 == null || msg2 == null) {
            return false;
        }
        
        // 1. 타임스탬프 비교 (정확히 같아야 함)
        if (msg1.getTimestamp() != msg2.getTimestamp()) {
            return false;
        }
        
        // 2. 발신자 ID 비교 (정규화 후 비교)
        String senderId1 = msg1.getSenderId();
        String senderId2 = msg2.getSenderId();
        if (senderId1 != null) senderId1 = senderId1.trim().toLowerCase();
        if (senderId2 != null) senderId2 = senderId2.trim().toLowerCase();
        
        if (!java.util.Objects.equals(senderId1, senderId2)) {
            return false;
        }
        
        // 3. 메시지 내용 비교 (정규화 후 비교)
        String content1 = msg1.getMessage();
        String content2 = msg2.getMessage();
        if (content1 != null) content1 = content1.trim();
        if (content2 != null) content2 = content2.trim();
        
        if (!java.util.Objects.equals(content1, content2)) {
            return false;
        }
        
        // 4. 파일 URL 비교 (있는 경우)
        String fileUrl1 = msg1.getFileUrl();
        String fileUrl2 = msg2.getFileUrl();
        if (fileUrl1 != null) fileUrl1 = fileUrl1.trim();
        if (fileUrl2 != null) fileUrl2 = fileUrl2.trim();
        
        return java.util.Objects.equals(fileUrl1, fileUrl2);
    }
    
    // 유사 메시지 감지 (내용과 시간이 비슷한 경우)
    private boolean isSimilarMessage(Message msg1, Message msg2) {
        if (msg1 == null || msg2 == null) {
            return false;
        }
        
        // 메시지 내용이 동일한지 확인
        String content1 = msg1.getMessage();
        String content2 = msg2.getMessage();
        if (content1 != null) content1 = content1.trim();
        if (content2 != null) content2 = content2.trim();
        
        if (!java.util.Objects.equals(content1, content2)) {
            return false;
        }
        
        // 시간 차이가 5초 이내인지 확인
        long timeDiff = Math.abs(msg1.getTimestamp() - msg2.getTimestamp());
        if (timeDiff > 5000) { // 5초 초과하면 다른 메시지로 판단
            return false;
        }
        
        // 발신자가 모두 현재 사용자인 경우
        boolean isMsg1Mine = isCurrentUserMessage(msg1.getSenderId(), msg1.getSenderName());
        boolean isMsg2Mine = isCurrentUserMessage(msg2.getSenderId(), msg2.getSenderName());
        
        if (isMsg1Mine && isMsg2Mine) {
            Log.d(TAG, "내가 보낸 메시지 중복 감지: 시간차=" + timeDiff + "ms");
            return true;
        }
        
        return false;
    }

    // 로딩 인디케이터 ViewHolder 클래스
    private class LoadingIndicatorHolder extends RecyclerView.ViewHolder {
        TextView loadingText;

        LoadingIndicatorHolder(View itemView) {
            super(itemView);
            loadingText = itemView.findViewById(R.id.text_loading_message);
        }

        void bind(String message) {
            loadingText.setText(message);
        }
    }

    // 날짜 구분선 ViewHolder 클래스
    private class DateSeparatorHolder extends RecyclerView.ViewHolder {
        TextView dateText;

        DateSeparatorHolder(View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.text_date);
        }

        void bind(DateSeparatorItem dateSeparator) {
            dateText.setText(dateSeparator.getDateText());
        }
    }

    // 보낸 메시지 ViewHolder 클래스
    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        ImageView fileImageView;
        CardView imageContainer, pdfContainer;
        LinearLayout pdfFileContainer;
        TextView pdfFilename;
        Button imageDownloadBtn, pdfDownloadBtn;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            
            // 이미지 관련 뷰
            imageContainer = itemView.findViewById(R.id.image_container);
            fileImageView = itemView.findViewById(R.id.image_file);
            imageDownloadBtn = itemView.findViewById(R.id.image_download_btn);
            
            // PDF 관련 뷰
            pdfContainer = itemView.findViewById(R.id.pdf_container);
            pdfFileContainer = itemView.findViewById(R.id.pdf_file_container);
            pdfFilename = itemView.findViewById(R.id.pdf_filename);
            pdfDownloadBtn = itemView.findViewById(R.id.pdf_download_btn);
        }

        void bind(Message message) {
            // 파일이 있는 경우 텍스트 숨기기, 파일만 표시
            if (message.hasFile() && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                // 파일 메시지인 경우 텍스트 숨기기
                messageText.setVisibility(View.GONE);
            } else {
                // 일반 텍스트 메시지
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(message.getMessage());
            }

            long timeInMillis = message.getTimestamp(); // UTC 기준 밀리초
            if (timeInMillis > 0) { // 유효한 타임스탬프인지 확인
                // SimpleDateFormat에 한국 시간대(KST) 강제 설정
                SimpleDateFormat sdf = new SimpleDateFormat("a h:mm", Locale.KOREA);
                sdf.setTimeZone(KST); // 시간대 KST로 설정
                timeText.setText(sdf.format(new Date(timeInMillis))); // KST 기준으로 시간 포맷
            } else {
                timeText.setText(""); // 타임스탬프 파싱 실패 시 시간 비우기
            }

            // 안전하게 컨테이너 nullcheck 수행
            if (imageContainer != null) {
                imageContainer.setVisibility(View.GONE);
            }
            if (pdfContainer != null) {
                pdfContainer.setVisibility(View.GONE);
            }

            // 파일 처리 로직 - 실제 파일 URL이 있는 경우만 처리
            if (message.hasFile() && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                final String fileUrl = message.getFileUrl();
                String fileType = message.getFileType();
                
                // 이미지 파일인 경우
                if ("image".equals(fileType) || (fileType != null && fileType.startsWith("image"))) {
                    if (imageContainer != null) {
                        imageContainer.setVisibility(View.VISIBLE);
                        if (fileImageView != null) {
                            // Glide로 이미지 로드
                            String fullImageUrl = fileUrl.startsWith("http") ? fileUrl : 
                                                  "http://202.31.246.51:80" + fileUrl;
                            
                            Glide.with(context)
                                .load(fullImageUrl)
                                .error(R.drawable.ic_image_error)
                                .into(fileImageView);
                        }
                        
                        if (imageDownloadBtn != null) {
                            imageDownloadBtn.setOnClickListener(v -> {
                                String filename = extractFilenameFromUrl(fileUrl);
                                downloadFile(fileUrl, filename, "image/*");
                            });
                        }
                    }
                    
                    if (pdfContainer != null) {
                        pdfContainer.setVisibility(View.GONE);
                    }
                } 
                // PDF 파일인 경우
                else if ("pdf".equals(fileType) || (fileUrl != null && fileUrl.toLowerCase().endsWith(".pdf"))) {
                    if (pdfContainer != null) {
                        pdfContainer.setVisibility(View.VISIBLE);
                        if (pdfFilename != null) {
                            String filename = extractFilenameFromUrl(fileUrl);
                            pdfFilename.setText(filename.isEmpty() ? "document.pdf" : filename);
                        }
                        
                        if (pdfDownloadBtn != null) {
                            pdfDownloadBtn.setOnClickListener(v -> {
                                String filename = extractFilenameFromUrl(fileUrl);
                                downloadFile(fileUrl, filename.isEmpty() ? "document.pdf" : filename, "application/pdf");
                            });
                        }
                    }
                    
                    if (imageContainer != null) {
                        imageContainer.setVisibility(View.GONE);
                    }
                } 
                // 기타 파일인 경우 (PDF 컨테이너 사용)
                else {
                    if (pdfContainer != null) {
                        pdfContainer.setVisibility(View.VISIBLE);
                        if (pdfFilename != null) {
                            String filename = extractFilenameFromUrl(fileUrl);
                            pdfFilename.setText(filename.isEmpty() ? "파일" : filename);
                        }
                        
                        if (pdfDownloadBtn != null) {
                            pdfDownloadBtn.setOnClickListener(v -> {
                                String filename = extractFilenameFromUrl(fileUrl);
                                downloadFile(fileUrl, filename.isEmpty() ? "file" : filename, "*/*");
                            });
                        }
                    }
                    
                    if (imageContainer != null) {
                        imageContainer.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    // 받은 메시지 ViewHolder 클래스
    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView nameText, messageText, timeText; // 이름, 메시지, 시간
        ImageView profileImage, fileImageView; // 프로필 이미지, 파일 이미지
        CardView imageContainer, pdfContainer; // 이미지 컨테이너, PDF 컨테이너
        LinearLayout pdfFileContainer; // PDF 파일 컨테이너
        TextView pdfFilename; // PDF 파일 이름
        Button imageDownloadBtn, pdfDownloadBtn; // 이미지 다운로드 버튼, PDF 다운로드 버튼

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_name);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            profileImage = itemView.findViewById(R.id.image_profile);
            
            // 이미지 관련 뷰
            imageContainer = itemView.findViewById(R.id.image_container);
            fileImageView = itemView.findViewById(R.id.image_file);
            imageDownloadBtn = itemView.findViewById(R.id.image_download_btn);
            
            // PDF 관련 뷰
            pdfContainer = itemView.findViewById(R.id.pdf_container);
            pdfFileContainer = itemView.findViewById(R.id.pdf_file_container);
            pdfFilename = itemView.findViewById(R.id.pdf_filename);
            pdfDownloadBtn = itemView.findViewById(R.id.pdf_download_btn);
        }

        void bind(Message message) {
            nameText.setText(message.getSenderName());
            
            // 파일이 있는 경우 텍스트 숨기기, 파일만 표시
            if (message.hasFile() && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                // 파일 메시지인 경우 텍스트 숨기기
                messageText.setVisibility(View.GONE);
            } else {
                // 일반 텍스트 메시지
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(message.getMessage());
            }

            long timeInMillis = message.getTimestamp(); // UTC 기준 밀리초
            if (timeInMillis > 0) { // 유효한 타임스탬프인지 확인
                // SimpleDateFormat에 한국 시간대(KST) 강제 설정
                SimpleDateFormat sdf = new SimpleDateFormat("a h:mm", Locale.KOREA);
                sdf.setTimeZone(KST); // 시간대 KST로 설정
                timeText.setText(sdf.format(new Date(timeInMillis))); // KST 기준으로 시간 포맷
            } else {
                timeText.setText(""); // 타임스탬프 파싱 실패 시 시간 비우기
            }

            // 프로필 이미지 설정 (기본 이미지 사용)
            if (profileImage != null) {
                profileImage.setImageResource(R.drawable.default_profile);
            }

            // 안전하게 컨테이너 nullcheck 수행
            if (imageContainer != null) {
                imageContainer.setVisibility(View.GONE);
            }
            if (pdfContainer != null) {
                pdfContainer.setVisibility(View.GONE);
            }

            // 파일 처리 로직 - 실제 파일 URL이 있는 경우만 처리
            if (message.hasFile() && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                final String fileUrl = message.getFileUrl();
                String fileType = message.getFileType();
                
                // 이미지 파일인 경우
                if ("image".equals(fileType) || (fileType != null && fileType.startsWith("image"))) {
                    if (imageContainer != null) {
                        imageContainer.setVisibility(View.VISIBLE);
                        if (fileImageView != null) {
                            // Glide로 이미지 로드
                            String fullImageUrl = fileUrl.startsWith("http") ? fileUrl : 
                                                  "http://202.31.246.51:80" + fileUrl;
                            
                            Glide.with(context)
                                .load(fullImageUrl)
                                .error(R.drawable.ic_image_error)
                                .into(fileImageView);
                        }
                        
                        if (imageDownloadBtn != null) {
                            imageDownloadBtn.setOnClickListener(v -> {
                                String filename = extractFilenameFromUrl(fileUrl);
                                downloadFile(fileUrl, filename, "image/*");
                            });
                        }
                    }
                    
                    if (pdfContainer != null) {
                        pdfContainer.setVisibility(View.GONE);
                    }
                } 
                // PDF 파일인 경우
                else if ("pdf".equals(fileType) || (fileUrl != null && fileUrl.toLowerCase().endsWith(".pdf"))) {
                    if (pdfContainer != null) {
                        pdfContainer.setVisibility(View.VISIBLE);
                        if (pdfFilename != null) {
                            String filename = extractFilenameFromUrl(fileUrl);
                            pdfFilename.setText(filename.isEmpty() ? "document.pdf" : filename);
                        }
                        
                        if (pdfDownloadBtn != null) {
                            pdfDownloadBtn.setOnClickListener(v -> {
                                String filename = extractFilenameFromUrl(fileUrl);
                                downloadFile(fileUrl, filename.isEmpty() ? "document.pdf" : filename, "application/pdf");
                            });
                        }
                    }
                    
                    if (imageContainer != null) {
                        imageContainer.setVisibility(View.GONE);
                    }
                } 
                // 기타 파일인 경우 (PDF 컨테이너 사용)
                else {
                    if (pdfContainer != null) {
                        pdfContainer.setVisibility(View.VISIBLE);
                        if (pdfFilename != null) {
                            String filename = extractFilenameFromUrl(fileUrl);
                            pdfFilename.setText(filename.isEmpty() ? "파일" : filename);
                        }
                        
                        if (pdfDownloadBtn != null) {
                            pdfDownloadBtn.setOnClickListener(v -> {
                                String filename = extractFilenameFromUrl(fileUrl);
                                downloadFile(fileUrl, filename.isEmpty() ? "file" : filename, "*/*");
                            });
                        }
                    }
                    
                    if (imageContainer != null) {
                        imageContainer.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    // 버튼이 있는 메시지 ViewHolder 클래스
    private class ButtonMessageHolder extends RecyclerView.ViewHolder {
        TextView nameText, messageText, timeText;
        ImageView profileImage;
        LinearLayout buttonContainer;
        Button buttonQuestion1, buttonQuestion2, buttonQuestion3;

        ButtonMessageHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_name);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            profileImage = itemView.findViewById(R.id.image_profile);
            buttonContainer = itemView.findViewById(R.id.button_container);
            buttonQuestion1 = itemView.findViewById(R.id.button_question_1);
            buttonQuestion2 = itemView.findViewById(R.id.button_question_2);
            buttonQuestion3 = itemView.findViewById(R.id.button_question_3);

            // 버튼 클릭 리스너 설정
            if (buttonQuestion1 != null) {
                buttonQuestion1.setOnClickListener(v -> {
                    if (buttonClickListener != null) {
                        buttonClickListener.onQuestionButtonClick("SMBTI가 뭐야?");
                    }
                });
            }
            if (buttonQuestion2 != null) {
                buttonQuestion2.setOnClickListener(v -> {
                    if (buttonClickListener != null) {
                        buttonClickListener.onQuestionButtonClick("SMBTI의 종류를 알려줘.");
                    }
                });
            }
            if (buttonQuestion3 != null) {
                buttonQuestion3.setOnClickListener(v -> {
                    if (buttonClickListener != null) {
                        buttonClickListener.onQuestionButtonClick("나의 SMBTI에 대해 알려줘");
                    }
                });
            }
        }

        void bind(Message message) {
            nameText.setText(message.getSenderName());
            messageText.setText(message.getMessage());

            long timeInMillis = message.getTimestamp();
            if (timeInMillis > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("a h:mm", Locale.KOREA);
                sdf.setTimeZone(KST);
                timeText.setText(sdf.format(new Date(timeInMillis)));
            } else {
                timeText.setText("");
            }

            // 프로필 이미지 설정
            if (profileImage != null) {
                profileImage.setImageResource(R.drawable.default_profile);
            }
        }
    }

    // URL에서 파일명 추출
    private String extractFilenameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "파일";
        }
        
        try {
            // URL에서 마지막 '/' 이후의 문자열을 파일명으로 사용
            int lastSlashIndex = url.lastIndexOf('/');
            if (lastSlashIndex != -1 && lastSlashIndex < url.length() - 1) {
                String filename = url.substring(lastSlashIndex + 1);
                
                // URL 디코딩 (한글 파일명 처리)
                try {
                    filename = java.net.URLDecoder.decode(filename, "UTF-8");
                } catch (Exception e) {
                    // 디코딩 실패 시 원본 사용
                }
                
                // 쿼리 파라미터 제거
                int queryIndex = filename.indexOf('?');
                if (queryIndex != -1) {
                    filename = filename.substring(0, queryIndex);
                }
                
                return filename.isEmpty() ? "파일" : filename;
            }
        } catch (Exception e) {
            Log.e(TAG, "파일명 추출 오류: " + url, e);
        }
        
        return "파일";
    }

    // 파일 URL 열기
    private void openFileUrl(String fileUrl, String fileType) {
        try {
            // 상대 경로를 절대 경로로 변환
            String fullUrl = fileUrl;
            if (fullUrl.startsWith("/")) {
                fullUrl = "http://202.31.246.51:80" + fullUrl;
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(fullUrl);
            
            if ("image".equals(fileType)) {
                intent.setDataAndType(uri, "image/*");
            } else if ("pdf".equals(fileType)) {
                intent.setDataAndType(uri, "application/pdf");
            } else {
                intent.setData(uri);
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // 인텐트를 처리할 수 있는 앱이 있는지 확인
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                // 처리할 수 있는 앱이 없으면 브라우저로 열기
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "파일 열기 오류: " + fileUrl, e);
            Toast.makeText(context, "파일을 열 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // 파일 다운로드
    private void downloadFile(String fileUrl, String filename, String mimeType) {
        try {
            // 상대 경로를 절대 경로로 변환
            String fullUrl = fileUrl;
            if (fullUrl.startsWith("/")) {
                fullUrl = "http://202.31.246.51:80" + fullUrl;
            }
            
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                Toast.makeText(context, "다운로드 매니저를 사용할 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Uri uri = Uri.parse(fullUrl);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            
            // 다운로드 설정
            request.setTitle(filename);
            request.setDescription("파일 다운로드 중...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            request.setMimeType(mimeType);
            
            // 다운로드 시작
            long downloadId = downloadManager.enqueue(request);
            Toast.makeText(context, "다운로드를 시작합니다: " + filename, Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "파일 다운로드 오류: " + fileUrl, e);
            Toast.makeText(context, "다운로드 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // 페이지네이션을 위한 메시지 상단 추가 메서드 (날짜 구분선 포함)
    public void addMessagesAtTop(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "상단에 " + messages.size() + "개 메시지 추가 시작");
        
        List<Message> validMessages = new ArrayList<>(); // 메서드 레벨에서 선언
        
        synchronized (originalMessageList) {
            // 중복 메시지 필터링
            for (Message message : messages) {
                if (message != null && !isDuplicateMessageEnhanced(message)) {
                    validMessages.add(message);
                }
            }
            
            if (validMessages.isEmpty()) {
                Log.d(TAG, "추가할 새 메시지가 없음 (모두 중복)");
                return;
            }
            
            // 원본 메시지 리스트에 추가
            originalMessageList.addAll(0, validMessages);
            
            // 표시 리스트 업데이트
            updateDisplayList();
            notifyDataSetChanged();
        }
        
        Log.d(TAG, "상단에 " + validMessages.size() + "개 메시지 추가 완료");
    }
    
    // 전체 메시지 교체 메서드 (날짜 구분선 포함)
    public void replaceAllMessages(List<Message> messages) {
        if (messages == null) {
            return;
        }
        
        Log.d(TAG, "전체 메시지 교체 시작: " + messages.size() + "개");
        
        List<Message> validMessages = new ArrayList<>(); // 메서드 레벨에서 선언
        
        synchronized (originalMessageList) {
            // 중복 메시지 필터링
            for (Message message : messages) {
                if (message != null && !isDuplicateMessageEnhanced(message)) {
                    validMessages.add(message);
                }
            }
            
            // 원본 메시지 리스트 교체
            originalMessageList.clear();
            originalMessageList.addAll(validMessages);
            
            // 표시 리스트 업데이트
            updateDisplayList();
            notifyDataSetChanged();
        }
        
        Log.d(TAG, "전체 메시지 교체 완료: " + validMessages.size() + "개");
    }
}
