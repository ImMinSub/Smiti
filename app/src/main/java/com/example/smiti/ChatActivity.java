package com.example.smiti;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

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
        LinearLayout pdfContainer;
        TextView pdfFilename;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            fileImageView = itemView.findViewById(R.id.image_file);
            pdfContainer = itemView.findViewById(R.id.pdf_file_container);
            pdfFilename = itemView.findViewById(R.id.pdf_filename);
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

            // 파일 표시 로직
            if (message.hasFile()) {
                String fileType = message.getFileType();
                String fileUrl = message.getFileUrl();
                
                if ("image".equals(fileType) && fileImageView != null) {
                    // 이미지 파일 표시
                fileImageView.setVisibility(View.VISIBLE);
                    if (pdfContainer != null) pdfContainer.setVisibility(View.GONE);
                    
                    // Glide로 이미지 로드
                    Glide.with(context).load(fileUrl).into(fileImageView);
                    
                    // 이미지 클릭 이벤트 설정
                    fileImageView.setOnClickListener(v -> {
                        openFileUrl(fileUrl, fileType);
                    });
                } 
                else if ("pdf".equals(fileType) && pdfContainer != null) {
                    // PDF 파일 표시
                    fileImageView.setVisibility(View.GONE);
                    pdfContainer.setVisibility(View.VISIBLE);
                    
                    // 파일명 추출 및 표시
                    String filename = extractFilenameFromUrl(fileUrl);
                    pdfFilename.setText(filename);
                    
                    // PDF 컨테이너 클릭 이벤트 설정
                    pdfContainer.setOnClickListener(v -> {
                        openFileUrl(fileUrl, fileType);
                    });
                }
                else {
                    // 지원하지 않는 파일 타입
                    fileImageView.setVisibility(View.GONE);
                    if (pdfContainer != null) pdfContainer.setVisibility(View.GONE);
                }
            } else {
                // 파일 없음
                fileImageView.setVisibility(View.GONE);
                if (pdfContainer != null) pdfContainer.setVisibility(View.GONE);
            }
        }
    }

    // 받은 메시지 ViewHolder 클래스
    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView nameText, messageText, timeText;
        ImageView profileImage, fileImageView;
        LinearLayout pdfContainer;
        TextView pdfFilename;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_name);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            profileImage = itemView.findViewById(R.id.image_profile);
            fileImageView = itemView.findViewById(R.id.image_file);
            pdfContainer = itemView.findViewById(R.id.pdf_file_container);
            pdfFilename = itemView.findViewById(R.id.pdf_filename);
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

            // 파일 표시 로직
            if (message.hasFile()) {
                String fileType = message.getFileType();
                String fileUrl = message.getFileUrl();
                
                if ("image".equals(fileType) && fileImageView != null) {
                    // 이미지 파일 표시
                fileImageView.setVisibility(View.VISIBLE);
                    if (pdfContainer != null) pdfContainer.setVisibility(View.GONE);
                    
                    // Glide로 이미지 로드
                    Glide.with(context).load(fileUrl).into(fileImageView);
                    
                    // 이미지 클릭 이벤트 설정
                    fileImageView.setOnClickListener(v -> {
                        openFileUrl(fileUrl, fileType);
                    });
                } 
                else if ("pdf".equals(fileType) && pdfContainer != null) {
                    // PDF 파일 표시
                    fileImageView.setVisibility(View.GONE);
                    pdfContainer.setVisibility(View.VISIBLE);
                    
                    // 파일명 추출 및 표시
                    String filename = extractFilenameFromUrl(fileUrl);
                    pdfFilename.setText(filename);
                    
                    // PDF 컨테이너 클릭 이벤트 설정
                    pdfContainer.setOnClickListener(v -> {
                        openFileUrl(fileUrl, fileType);
                    });
                }
                else {
                    // 지원하지 않는 파일 타입
                    fileImageView.setVisibility(View.GONE);
                    if (pdfContainer != null) pdfContainer.setVisibility(View.GONE);
                }
            } else {
                // 파일 없음
                fileImageView.setVisibility(View.GONE);
                if (pdfContainer != null) pdfContainer.setVisibility(View.GONE);
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
            return "file.pdf";
        }
        
        try {
            // URL에서 마지막 '/' 이후의 문자열을 파일명으로 처리
            int lastSlashIndex = url.lastIndexOf('/');
            if (lastSlashIndex != -1 && lastSlashIndex < url.length() - 1) {
                return url.substring(lastSlashIndex + 1);
            }
            
            // 쿼리 파라미터 제거
            int queryIndex = url.indexOf('?');
            if (queryIndex != -1) {
                return url.substring(lastSlashIndex + 1, queryIndex);
            }
            
            return url;
        } catch (Exception e) {
            Log.e(TAG, "파일명 추출 오류", e);
            return "file.pdf";
        }
    }
    
    // 파일 URL 열기
    private void openFileUrl(String fileUrl, String fileType) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(fileUrl);
            
            if ("pdf".equals(fileType)) {
                intent.setDataAndType(uri, "application/pdf");
            } else if ("image".equals(fileType)) {
                intent.setDataAndType(uri, "image/*");
            } else {
                intent.setData(uri);
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "파일 열기 오류", e);
            Toast.makeText(context, "파일을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
