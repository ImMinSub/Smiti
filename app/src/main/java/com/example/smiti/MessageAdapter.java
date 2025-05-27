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
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    // 표시 리스트 업데이트 (날짜 구분선 및 로딩 인디케이터 포함)
    private void updateDisplayList() {
        displayList.clear();
        
        // 로딩 인디케이터가 표시되어야 하면 맨 위에 추가
        if (showLoadingIndicator) {
            displayList.add("LOADING_INDICATOR");
        }
        
        if (originalMessageList.isEmpty()) {
            return;
        }
        
        String lastDateString = "";
        
        for (int i = 0; i < originalMessageList.size(); i++) {
            Message message = originalMessageList.get(i);
            long timestamp = message.getTimestamp();
            
            // 메시지의 날짜 문자열 생성
            String currentDateString = formatDateForSeparator(timestamp);
            
            // 날짜가 바뀌었으면 날짜 구분선 추가
            if (!currentDateString.equals(lastDateString)) {
                DateSeparatorItem dateSeparator = new DateSeparatorItem(currentDateString, timestamp);
                displayList.add(dateSeparator);
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

    @Override
    public int getItemViewType(int position) {
        Object item = displayList.get(position);
        
        // 로딩 인디케이터인 경우
        if (item instanceof String && "LOADING_INDICATOR".equals(item)) {
            return VIEW_TYPE_LOADING_INDICATOR;
        }
        
        // 날짜 구분선인 경우
        if (item instanceof DateSeparatorItem) {
            return VIEW_TYPE_DATE_SEPARATOR;
        }
        
        // 메시지인 경우
        Message message = (Message) item;
        String senderId = message.getSenderId(); // 메시지 발신자 ID (이메일)

        // 시스템 메시지이고 첫 메시지인 경우 버튼이 있는 메시지 타입으로 설정
        if ("system".equals(senderId) && isFirstMessage(message)) {
            return VIEW_TYPE_MESSAGE_WITH_BUTTONS;
        }
        // 메시지가 현재 사용자의 것인지 확인하여 뷰 타입 결정
        else if (currentUserIdentifier != null && senderId != null && senderId.equals(currentUserIdentifier)) {
            return VIEW_TYPE_MESSAGE_SENT; // 보낸 메시지
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED; // 받은 메시지
        }
    }
    
    // 첫 번째 메시지인지 확인하는 헬퍼 메서드
    private boolean isFirstMessage(Message targetMessage) {
        for (Message message : originalMessageList) {
            if (message == targetMessage) {
                return originalMessageList.indexOf(message) == 0;
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
        Object item = displayList.get(position);
        
        // ViewHolder 타입에 따라 데이터 바인딩
        if (holder instanceof LoadingIndicatorHolder) {
            ((LoadingIndicatorHolder) holder).bind(loadingMessage);
        } else if (holder instanceof DateSeparatorHolder) {
            ((DateSeparatorHolder) holder).bind((DateSeparatorItem) item);
        } else if (holder instanceof SentMessageHolder) {
            ((SentMessageHolder) holder).bind((Message) item);
        } else if (holder instanceof ReceivedMessageHolder) {
            ((ReceivedMessageHolder) holder).bind((Message) item);
        } else if (holder instanceof ButtonMessageHolder) {
            ((ButtonMessageHolder) holder).bind((Message) item);
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    // 메시지 리스트에 새 메시지 추가 및 UI 갱신
    public void addMessage(Message message) {
        originalMessageList.add(message);
        updateDisplayList(); // 표시 리스트 업데이트
        notifyDataSetChanged(); // 전체 갱신 (날짜 구분선 때문에)
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
            // 파일이 있는 경우 적절한 메시지 표시
            if (message.hasFile() && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                String fileType = message.getFileType();
                if ("image".equals(fileType)) {
                    messageText.setText("이미지를 전송했습니다.");
                } else if ("pdf".equals(fileType)) {
                    messageText.setText("PDF 문서를 전송했습니다.");
                } else {
                    messageText.setText("파일을 전송했습니다.");
                }
            } else {
                // 일반 텍스트 메시지
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

            // 파일 처리 로직
            if (message.hasFile() && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                final String fileUrl = message.getFileUrl();
                String fileType = message.getFileType();
                
                // fileType이 null인 경우 파일 URL에서 타입 추측
                if (fileType == null || fileType.isEmpty()) {
                    if (fileUrl.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                        fileType = "image";
                    } else if (fileUrl.toLowerCase().endsWith(".pdf")) {
                        fileType = "pdf";
                    } else {
                        fileType = "file"; // 기본값
                    }
                }
                
                final String finalFileType = fileType; // final 변수로 만들어 람다에서 사용
                
                if ("image".equals(fileType) && fileImageView != null && imageContainer != null) {
                    // 이미지 파일 표시
                    imageContainer.setVisibility(View.VISIBLE);
                    
                    try {
                        // 상대 경로 처리
                        String imageUrl = fileUrl;
                        if (imageUrl.startsWith("/")) {
                            imageUrl = "http://202.31.246.51:80" + imageUrl;
                        }
                        
                        // Glide로 이미지 로드
                        Glide.with(context)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_image_loading)
                            .error(R.drawable.ic_image_error)
                            .into(fileImageView);
                        
                        // 이미지 클릭 이벤트 설정
                        fileImageView.setOnClickListener(v -> {
                            openFileUrl(fileUrl, finalFileType);
                        });
                        
                        // 다운로드 버튼 설정
                        if (imageDownloadBtn != null) {
                            imageDownloadBtn.setOnClickListener(v -> {
                                String filename = extractFilenameFromUrl(fileUrl);
                                downloadFile(fileUrl, filename, "image/*");
                            });
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "이미지 로드 오류: " + fileUrl, e);
                        imageContainer.setVisibility(View.GONE);
                    }
                    
                } else if ("pdf".equals(fileType) && pdfContainer != null) {
                    // PDF 파일 표시
                    pdfContainer.setVisibility(View.VISIBLE);
                    
                    if (pdfFilename != null) {
                        String filename = extractFilenameFromUrl(fileUrl);
                        pdfFilename.setText(filename);
                    }
                    
                    // PDF 다운로드 버튼 설정
                    if (pdfDownloadBtn != null) {
                        pdfDownloadBtn.setOnClickListener(v -> {
                            String filename = extractFilenameFromUrl(fileUrl);
                            downloadFile(fileUrl, filename, "application/pdf");
                        });
                    }
                    
                    // PDF 컨테이너 클릭 이벤트 설정
                    if (pdfFileContainer != null) {
                        pdfFileContainer.setOnClickListener(v -> {
                            openFileUrl(fileUrl, finalFileType);
                        });
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
            
            // 파일이 있는 경우 적절한 메시지 표시
            if (message.hasFile() && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                String fileType = message.getFileType();
                if ("image".equals(fileType)) {
                    messageText.setText("이미지를 전송했습니다.");
                } else if ("pdf".equals(fileType)) {
                    messageText.setText("PDF 문서를 전송했습니다.");
                } else {
                    messageText.setText("파일을 전송했습니다.");
                }
            } else {
                // 일반 텍스트 메시지
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

            // 파일 처리 로직
            if (message.hasFile() && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                final String fileUrl = message.getFileUrl();
                String fileType = message.getFileType();
                
                // fileType이 null인 경우 파일 URL에서 타입 추측
                if (fileType == null || fileType.isEmpty()) {
                    if (fileUrl.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                        fileType = "image";
                    } else if (fileUrl.toLowerCase().endsWith(".pdf")) {
                        fileType = "pdf";
                    } else {
                        fileType = "file"; // 기본값
                    }
                }
                
                final String finalFileType = fileType; // final 변수로 만들어 람다에서 사용
                
                if ("image".equals(fileType) && fileImageView != null && imageContainer != null) {
                    // 이미지 파일 표시
                    imageContainer.setVisibility(View.VISIBLE);
                    
                    try {
                        // 상대 경로 처리
                        String imageUrl = fileUrl;
                        if (imageUrl.startsWith("/")) {
                            imageUrl = "http://202.31.246.51:80" + imageUrl;
                        }
                        
                        // Glide로 이미지 로드
                        Glide.with(context)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_image_loading)
                            .error(R.drawable.ic_image_error)
                            .into(fileImageView);
                        
                        // 이미지 클릭 이벤트 설정
                        fileImageView.setOnClickListener(v -> {
                            openFileUrl(fileUrl, finalFileType);
                        });
                        
                        // 다운로드 버튼 설정
                        if (imageDownloadBtn != null) {
                            imageDownloadBtn.setOnClickListener(v -> {
                                String filename = extractFilenameFromUrl(fileUrl);
                                downloadFile(fileUrl, filename, "image/*");
                            });
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "이미지 로드 오류: " + fileUrl, e);
                        imageContainer.setVisibility(View.GONE);
                    }
                    
                } else if ("pdf".equals(fileType) && pdfContainer != null) {
                    // PDF 파일 표시
                    pdfContainer.setVisibility(View.VISIBLE);
                    
                    if (pdfFilename != null) {
                        String filename = extractFilenameFromUrl(fileUrl);
                        pdfFilename.setText(filename);
                    }
                    
                    // PDF 다운로드 버튼 설정
                    if (pdfDownloadBtn != null) {
                        pdfDownloadBtn.setOnClickListener(v -> {
                            String filename = extractFilenameFromUrl(fileUrl);
                            downloadFile(fileUrl, filename, "application/pdf");
                        });
                    }
                    
                    // PDF 컨테이너 클릭 이벤트 설정
                    if (pdfFileContainer != null) {
                        pdfFileContainer.setOnClickListener(v -> {
                            openFileUrl(fileUrl, finalFileType);
                        });
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
                        buttonClickListener.onQuestionButtonClick("SMBTI 테스트를 해보고 싶어요");
                    }
                });
            }
            if (buttonQuestion2 != null) {
                buttonQuestion2.setOnClickListener(v -> {
                    if (buttonClickListener != null) {
                        buttonClickListener.onQuestionButtonClick("스터디 그룹을 찾고 있어요");
                    }
                });
            }
            if (buttonQuestion3 != null) {
                buttonQuestion3.setOnClickListener(v -> {
                    if (buttonClickListener != null) {
                        buttonClickListener.onQuestionButtonClick("공부 시간을 관리하고 싶어요");
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
}
