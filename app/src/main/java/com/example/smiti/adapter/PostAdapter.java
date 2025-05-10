package com.example.smiti.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smiti.R;
import com.example.smiti.model.Post;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;
    private OnItemClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Post> newList) {
        this.postList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        
        // 프로필 이미지 설정 (기본 이미지 사용)
        // holder.profileImage.setImageResource(R.drawable.default_profile);
        
        // 작성자 이름 표시
        holder.authorText.setText(post.getAuthorName());
        
        // 작성 시간 표시 (현재 시간으로부터 얼마나 지났는지)
        holder.dateText.setText(getTimeAgo(post.getCreatedAt()));
        
        // 제목 표시
        String titleText = post.getTitle();
        if (post.isNotice()) {
            titleText = "[공지] " + titleText;
        }
        holder.titleText.setText(titleText);
        
        // 내용 표시
        holder.contentText.setText(post.getContent());
        
        // 카테고리 표시 (선택적)
        if (post.getCategory() != null && !post.getCategory().equals("전체")) {
            holder.categoryText.setVisibility(View.VISIBLE);
            holder.categoryText.setText(post.getCategory());
        } else {
            holder.categoryText.setVisibility(View.GONE);
        }
        
        // 좋아요, 댓글, 조회수 표시
        holder.likeCountText.setText(String.valueOf(post.getLikeCount()));
        holder.commentCountText.setText(String.valueOf(post.getCommentCount()));
        holder.viewCountText.setText(String.valueOf(post.getViewCount()));
        
        // 파일 첨부 아이콘 표시
        if (post.hasFile()) {
            holder.fileAttachmentIcon.setVisibility(View.VISIBLE);
        } else {
            holder.fileAttachmentIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return postList == null ? 0 : postList.size();
    }
    
    /**
     * 날짜를 "몇 분 전", "몇 시간 전", "몇 일 전" 형식으로 변환
     */
    private String getTimeAgo(Date date) {
        if (date == null) return "";
        
        long currentTime = System.currentTimeMillis();
        long dateTime = date.getTime();
        long diff = currentTime - dateTime;
        
        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "방금 전";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + "분 전";
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + "시간 전";
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + "일 전";
        } else {
            return dateFormat.format(date);
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView authorText, dateText, titleText, contentText, categoryText;
        TextView likeCountText, commentCountText, viewCountText;
        ImageView fileAttachmentIcon;

        public PostViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            
            profileImage = itemView.findViewById(R.id.iv_profile);
            authorText = itemView.findViewById(R.id.tv_author);
            dateText = itemView.findViewById(R.id.tv_date);
            titleText = itemView.findViewById(R.id.tv_title);
            contentText = itemView.findViewById(R.id.tv_content);
            categoryText = itemView.findViewById(R.id.tv_category);
            likeCountText = itemView.findViewById(R.id.tv_like_count);
            commentCountText = itemView.findViewById(R.id.tv_comment_count);
            viewCountText = itemView.findViewById(R.id.tv_view_count);
            fileAttachmentIcon = itemView.findViewById(R.id.iv_file_attachment);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }
} 