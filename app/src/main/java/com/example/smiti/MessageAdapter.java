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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone; // TimeZone import 추가

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "MessageAdapter";
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_MESSAGE_WITH_BUTTONS = 3; // 버튼이 있는 메시지 유형 추가

    private Context context;
    private List<Message> messageList;
    private String currentUserIdentifier; // 현재 사용자 식별자 (이메일)
    private OnQuestionButtonClickListener buttonClickListener; // 버튼 클릭 리스너 추가

    // 한국 시간대(KST)를 위한 TimeZone 객체
    private static final TimeZone KST = TimeZone.getTimeZone("Asia/Seoul");

    // 버튼 클릭 이벤트를 처리하기 위한 인터페이스
    public interface OnQuestionButtonClickListener {
        void onQuestionButtonClick(String question);
    }

    public MessageAdapter(Context context, List<Message> messageList, String currentUserIdentifier) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserIdentifier = currentUserIdentifier;
        Log.d(TAG, "어댑터 초기화: currentUserIdentifier=" + currentUserIdentifier);
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

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        String senderId = message.getSenderId(); // 메시지 발신자 ID (이메일)

        // 시스템 메시지이고 첫 메시지인 경우 버튼이 있는 메시지 타입으로 설정
        if ("system".equals(senderId) && position == 0) {
            return VIEW_TYPE_MESSAGE_WITH_BUTTONS;
        }
        // 메시지가 현재 사용자의 것인지 확인하여 뷰 타입 결정
        else if (currentUserIdentifier != null && senderId != null && senderId.equals(currentUserIdentifier)) {
            return VIEW_TYPE_MESSAGE_SENT; // 보낸 메시지
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED; // 받은 메시지
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        // 뷰 타입에 따라 다른 레이아웃 inflate
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
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
        Message message = messageList.get(position);
        // ViewHolder 타입에 따라 데이터 바인딩
        if (holder instanceof SentMessageHolder) {
            ((SentMessageHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageHolder) {
            ((ReceivedMessageHolder) holder).bind(message);
        } else if (holder instanceof ButtonMessageHolder) {
            ((ButtonMessageHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // 메시지 리스트에 새 메시지 추가 및 UI 갱신
    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1); // 효율적인 UI 갱신
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
            messageText.setText(message.getMessage());

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

            // 파일 표시 로직
            if (message.hasFile() && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                String fileType = message.getFileType();
                final String fileUrl = message.getFileUrl();
                
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
                        
                        // 이미지 클릭 이벤트 설정 - final 변수로 복사
                        final String finalFileUrl = fileUrl;
                        final String finalFileType = fileType;
                        fileImageView.setOnClickListener(v -> {
                            openFileUrl(finalFileUrl, finalFileType);
                        });
                        
                        // 이미지 다운로드 버튼 클릭 이벤트
                        if (imageDownloadBtn != null) {
                            final String finalFileUrl2 = fileUrl;
                            imageDownloadBtn.setOnClickListener(v -> {
                                downloadFile(finalFileUrl2, extractFilenameFromUrl(finalFileUrl2), "image/*");
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "이미지 로드 오류: " + e.getMessage());
                    }
                } 
                else if ("pdf".equals(fileType) && pdfContainer != null && pdfFileContainer != null) {
                    // PDF 파일 표시
                    pdfContainer.setVisibility(View.VISIBLE);
                    
                    // 파일명 추출 및 표시
                    String filename = extractFilenameFromUrl(fileUrl);
                    if (pdfFilename != null) {
                        pdfFilename.setText(filename);
                    }
                    
                    // PDF 컨테이너 클릭 이벤트 설정 - final 변수로 복사
                    final String finalFileUrl = fileUrl;
                    final String finalFileType = fileType;
                    pdfFileContainer.setOnClickListener(v -> {
                        openFileUrl(finalFileUrl, finalFileType);
                    });
                    
                    // PDF 다운로드 버튼 클릭 이벤트
                    if (pdfDownloadBtn != null) {
                        final String finalFileUrl2 = fileUrl;
                        final String finalFilename = filename;
                        pdfDownloadBtn.setOnClickListener(v -> {
                            downloadFile(finalFileUrl2, finalFilename, "application/pdf");
                        });
                    }
                }
                // 다른 타입의 파일도 처리할 수 있도록 추가
                else if (fileType != null && !fileType.isEmpty() && pdfContainer != null && pdfFileContainer != null) {
                    // 알 수 없는 파일 형식은 일반 파일로 표시
                    pdfContainer.setVisibility(View.VISIBLE);
                    
                    // 파일명 추출 및 표시
                    String filename = extractFilenameFromUrl(fileUrl);
                    if (pdfFilename != null) {
                        pdfFilename.setText(filename);
                    }
                    
                    // 파일 컨테이너 클릭 이벤트 설정 - final 변수로 복사
                    final String finalFileUrl = fileUrl;
                    final String finalFileType = fileType;
                    pdfFileContainer.setOnClickListener(v -> {
                        openFileUrl(finalFileUrl, finalFileType);
                    });
                    
                    // 파일 다운로드 버튼 클릭 이벤트
                    if (pdfDownloadBtn != null) {
                        final String finalFileUrl2 = fileUrl;
                        final String finalFilename = filename;
                        pdfDownloadBtn.setOnClickListener(v -> {
                            downloadFile(finalFileUrl2, finalFilename, "*/*");
                        });
                    }
                }
                // 파일 URL이 있지만 처리되지 않은 경우 (기본 처리)
                else if (pdfContainer != null && pdfFileContainer != null) {
                    pdfContainer.setVisibility(View.VISIBLE);
                    
                    // 파일명 추출 및 표시
                    String filename = extractFilenameFromUrl(fileUrl);
                    if (pdfFilename != null) {
                        pdfFilename.setText(filename);
                    }
                    
                    // 파일 컨테이너 클릭 이벤트 설정
                    final String finalFileUrl = fileUrl;
                    pdfFileContainer.setOnClickListener(v -> {
                        openFileUrl(finalFileUrl, "file");
                    });
                    
                    // 파일 다운로드 버튼 클릭 이벤트
                    if (pdfDownloadBtn != null) {
                        final String finalFileUrl2 = fileUrl;
                        final String finalFilename = filename;
                        pdfDownloadBtn.setOnClickListener(v -> {
                            downloadFile(finalFileUrl2, finalFilename, "*/*");
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
            // 발신자 이름 설정
            String senderName = message.getSenderName();
            if (senderName == null || senderName.isEmpty()) {
                nameText.setText("알 수 없음");
            } else {
                nameText.setText(senderName);
            }

            messageText.setText(message.getMessage());

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

            // 파일 표시 로직
            if (message.hasFile() && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                String fileType = message.getFileType();
                final String fileUrl = message.getFileUrl();
                
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
                        
                        // 이미지 클릭 이벤트 설정 - final 변수로 복사
                        final String finalFileUrl = fileUrl;
                        final String finalFileType = fileType;
                        fileImageView.setOnClickListener(v -> {
                            openFileUrl(finalFileUrl, finalFileType);
                        });
                        
                        // 이미지 다운로드 버튼 클릭 이벤트
                        if (imageDownloadBtn != null) {
                            final String finalFileUrl2 = fileUrl;
                            imageDownloadBtn.setOnClickListener(v -> {
                                downloadFile(finalFileUrl2, extractFilenameFromUrl(finalFileUrl2), "image/*");
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "이미지 로드 오류: " + e.getMessage(), e);
                    }
                } 
                else if ("pdf".equals(fileType) && pdfContainer != null && pdfFileContainer != null) {
                    // PDF 파일 표시
                    pdfContainer.setVisibility(View.VISIBLE);
                    
                    // 파일명 추출 및 표시
                    String filename = extractFilenameFromUrl(fileUrl);
                    if (pdfFilename != null) {
                        pdfFilename.setText(filename);
                    }
                    
                    // PDF 컨테이너 클릭 이벤트 설정 - final 변수로 복사
                    final String finalFileUrl = fileUrl;
                    final String finalFileType = fileType;
                    pdfFileContainer.setOnClickListener(v -> {
                        openFileUrl(finalFileUrl, finalFileType);
                    });
                    
                    // PDF 다운로드 버튼 클릭 이벤트
                    if (pdfDownloadBtn != null) {
                        final String finalFileUrl2 = fileUrl;
                        final String finalFilename = filename;
                        pdfDownloadBtn.setOnClickListener(v -> {
                            downloadFile(finalFileUrl2, finalFilename, "application/pdf");
                        });
                    }
                }
                // 다른 타입의 파일도 처리할 수 있도록 추가
                else if (fileType != null && !fileType.isEmpty() && pdfContainer != null && pdfFileContainer != null) {
                    // 알 수 없는 파일 형식은 일반 파일로 표시
                    pdfContainer.setVisibility(View.VISIBLE);
                    
                    // 파일명 추출 및 표시
                    String filename = extractFilenameFromUrl(fileUrl);
                    if (pdfFilename != null) {
                        pdfFilename.setText(filename);
                    }
                    
                    // 파일 컨테이너 클릭 이벤트 설정 - final 변수로 복사
                    final String finalFileUrl = fileUrl;
                    final String finalFileType = fileType;
                    pdfFileContainer.setOnClickListener(v -> {
                        openFileUrl(finalFileUrl, finalFileType);
                    });
                    
                    // 파일 다운로드 버튼 클릭 이벤트
                    if (pdfDownloadBtn != null) {
                        final String finalFileUrl2 = fileUrl;
                        final String finalFilename = filename;
                        pdfDownloadBtn.setOnClickListener(v -> {
                            downloadFile(finalFileUrl2, finalFilename, "*/*");
                        });
                    }
                }
                // 파일 URL이 있지만 처리되지 않은 경우 (기본 처리)
                else if (pdfContainer != null && pdfFileContainer != null) {
                    pdfContainer.setVisibility(View.VISIBLE);
                    
                    // 파일명 추출 및 표시
                    String filename = extractFilenameFromUrl(fileUrl);
                    if (pdfFilename != null) {
                        pdfFilename.setText(filename);
                    }
                    
                    // 파일 컨테이너 클릭 이벤트 설정
                    final String finalFileUrl = fileUrl;
                    pdfFileContainer.setOnClickListener(v -> {
                        openFileUrl(finalFileUrl, "file");
                    });
                    
                    // 파일 다운로드 버튼 클릭 이벤트
                    if (pdfDownloadBtn != null) {
                        final String finalFileUrl2 = fileUrl;
                        final String finalFilename = filename;
                        pdfDownloadBtn.setOnClickListener(v -> {
                            downloadFile(finalFileUrl2, finalFilename, "*/*");
                        });
                    }
                }
            }

            // 프로필 이미지 설정 (필요시 Glide 등 사용)
            // if (profileImage != null) {
            //     Glide.with(context).load(프로필_이미지_URL).circleCrop().into(profileImage);
            // }
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

            // 버튼 클릭 이벤트 설정
            buttonQuestion1.setOnClickListener(v -> {
                if (buttonClickListener != null) {
                    buttonClickListener.onQuestionButtonClick(buttonQuestion1.getText().toString());
                }
            });

            buttonQuestion2.setOnClickListener(v -> {
                if (buttonClickListener != null) {
                    buttonClickListener.onQuestionButtonClick(buttonQuestion2.getText().toString());
                }
            });

            buttonQuestion3.setOnClickListener(v -> {
                if (buttonClickListener != null) {
                    buttonClickListener.onQuestionButtonClick(buttonQuestion3.getText().toString());
                }
            });
        }

        void bind(Message message) {
            // 발신자 이름 설정
            String senderName = message.getSenderName();
            if (senderName == null || senderName.isEmpty()) {
                nameText.setText("챗봇");
            } else {
                nameText.setText(senderName);
            }

            messageText.setText(message.getMessage());

            long timeInMillis = message.getTimestamp(); // UTC 기준 밀리초
            if (timeInMillis > 0) { // 유효한 타임스탬프인지 확인
                // SimpleDateFormat에 한국 시간대(KST) 강제 설정
                SimpleDateFormat sdf = new SimpleDateFormat("a h:mm", Locale.KOREA);
                sdf.setTimeZone(KST); // 시간대 KST로 설정
                timeText.setText(sdf.format(new Date(timeInMillis))); // KST 기준으로 시간 포맷
            } else {
                timeText.setText(""); // 타임스탬프 파싱 실패 시 시간 비우기
            }
        }
    }

    // 파일 URL에서 파일명 추출
    private String extractFilenameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "unknown_file";
        }
        
        try {
            // URL에서 마지막 '/' 이후의 문자열을 파일명으로 처리
            int lastSlashIndex = url.lastIndexOf('/');
            if (lastSlashIndex != -1 && lastSlashIndex < url.length() - 1) {
                String filename = url.substring(lastSlashIndex + 1);
                
                // 쿼리 파라미터 제거
                int queryIndex = filename.indexOf('?');
                if (queryIndex != -1) {
                    filename = filename.substring(0, queryIndex);
                }
                
                // URL 디코딩
                try {
                    filename = java.net.URLDecoder.decode(filename, "UTF-8");
                } catch (Exception e) {
                    Log.e(TAG, "URL 디코딩 오류", e);
                }
                
                return filename;
            }
            
            return "unknown_file";
        } catch (Exception e) {
            Log.e(TAG, "파일명 추출 오류", e);
            return "unknown_file";
        }
    }
    
    // 파일 URL 열기
    private void openFileUrl(String fileUrl, String fileType) {
        try {
            // 상대 경로인 경우 절대 경로로 변환
            if (fileUrl.startsWith("/")) {
                fileUrl = "http://202.31.246.51:80" + fileUrl;
                Log.d(TAG, "URL 열기: 상대 경로를 절대 경로로 변환 - " + fileUrl);
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(fileUrl);
            
            if ("pdf".equals(fileType) || fileUrl.toLowerCase().endsWith(".pdf")) {
                intent.setDataAndType(uri, "application/pdf");
            } else if ("image".equals(fileType) || fileUrl.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                intent.setDataAndType(uri, "image/*");
            } else if ("video".equals(fileType) || fileUrl.toLowerCase().matches(".*\\.(mp4|avi|mov|wmv|flv|mkv)$")) {
                intent.setDataAndType(uri, "video/*");
            } else if ("audio".equals(fileType) || fileUrl.toLowerCase().matches(".*\\.(mp3|wav|ogg|m4a|aac)$")) {
                intent.setDataAndType(uri, "audio/*");
            } else if ("document".equals(fileType) || fileUrl.toLowerCase().matches(".*\\.(doc|docx|xls|xlsx|ppt|pptx|txt)$")) {
                intent.setDataAndType(uri, "application/*");
            } else {
                intent.setData(uri);
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // 인텐트를 처리할 앱이 있는지 확인
            if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            } else {
                // 처리할 수 있는 앱이 없는 경우 다운로드 제안
                Toast.makeText(context, "이 파일을 열 수 있는 앱이 없습니다. 다운로드를 시도해보세요.", Toast.LENGTH_SHORT).show();
                downloadFile(fileUrl, extractFilenameFromUrl(fileUrl), "*/*");
            }
        } catch (Exception e) {
            Log.e(TAG, "파일 열기 오류: " + e.getMessage(), e);
            Toast.makeText(context, "파일을 열 수 없습니다. 다운로드를 시도해보세요.", Toast.LENGTH_SHORT).show();
            
            // 오류 발생 시 자동으로 다운로드 시도
            downloadFile(fileUrl, extractFilenameFromUrl(fileUrl), "*/*");
        }
    }
    
    // 파일 다운로드 기능
    private void downloadFile(String fileUrl, String filename, String mimeType) {
        try {
            // 상대 경로인 경우 절대 경로로 변환
            if (fileUrl.startsWith("/")) {
                fileUrl = "http://202.31.246.51:80" + fileUrl;
                Log.d(TAG, "파일 다운로드: 상대 경로를 절대 경로로 변환 - " + fileUrl);
            }
            
            if (filename == null || filename.isEmpty()) {
                filename = "file_" + System.currentTimeMillis();
                
                // 확장자 추가
                if (mimeType.equals("application/pdf")) {
                    filename += ".pdf";
                } else if (mimeType.startsWith("image/")) {
                    filename += ".jpg";
                } else if (mimeType.startsWith("video/")) {
                    filename += ".mp4";
                } else if (mimeType.startsWith("audio/")) {
                    filename += ".mp3";
                }
            }
            
            // 파일명에 확장자가 없는 경우 MIME 타입에 따라 추가
            if (!filename.contains(".")) {
                if (mimeType.equals("application/pdf")) {
                    filename += ".pdf";
                } else if (mimeType.equals("image/*") || mimeType.startsWith("image/")) {
                    filename += ".jpg";
                } else if (mimeType.equals("video/*") || mimeType.startsWith("video/")) {
                    filename += ".mp4";
                } else if (mimeType.equals("audio/*") || mimeType.startsWith("audio/")) {
                    filename += ".mp3";
                } else {
                    filename += ".dat";
                }
            }
            
            // 특수문자 제거 또는 대체
            filename = filename.replaceAll("[\\\\/:*?\"<>|]", "_");
            
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
            request.setTitle(filename);
            request.setDescription("파일 다운로드 중...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            // 다운로드 위치 설정 (다운로드 폴더 내 Smiti 디렉토리)
            File destinationDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "Smiti");
            if (!destinationDir.exists()) {
                boolean dirCreated = destinationDir.mkdirs();
                if (!dirCreated) {
                    Log.w(TAG, "다운로드 디렉토리 생성 실패, 기본 다운로드 폴더 사용");
                }
            }
            
            // 경로 지정 방식 수정 - 첫 번째 인자로는 표준 디렉토리만 전달
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, "Smiti/" + filename);
            
            // MIME 타입 설정
            request.setMimeType(mimeType);
            
            // DownloadManager를 통해 다운로드 시작
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = downloadManager.enqueue(request);
            
            Toast.makeText(context, filename + " 다운로드를 시작합니다.", Toast.LENGTH_SHORT).show();
            
            // 다운로드 완료 시 파일 열기를 위한 BroadcastReceiver 등록 가능
            // 예시 코드 생략 (필요시 추가)
            
        } catch (Exception e) {
            Log.e(TAG, "파일 다운로드 오류: " + e.getMessage(), e);
            Toast.makeText(context, "다운로드 중 오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
