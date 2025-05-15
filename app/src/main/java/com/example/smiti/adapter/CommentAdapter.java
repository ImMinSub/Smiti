package com.example.smiti.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smiti.R;
import com.example.smiti.model.Comment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private static final String TAG = "CommentAdapter"; // TAG 추가
    private List<Comment> commentList;
    private Context context;
    private String currentUserEmail;
    private OnCommentDeleteListener onCommentDeleteListener;

    public CommentAdapter(Context context, String currentUserEmail) {
        this.context = context;
        this.currentUserEmail = currentUserEmail;
        this.commentList = new ArrayList<>();
    }

    public interface OnCommentDeleteListener {
        void onCommentDelete(int commentId);
    }

    public void setOnCommentDeleteListener(OnCommentDeleteListener listener) {
        this.onCommentDeleteListener = listener;
    }

    public void setComments(List<Comment> comments) {
        this.commentList = comments;
        notifyDataSetChanged();
    }

    public void addComment(Comment comment) {
        this.commentList.add(comment);
        notifyItemInserted(commentList.size() - 1);
    }

    public void removeComment(int commentId) {
        for (int i = 0; i < commentList.size(); i++) {
            if (commentList.get(i).getId() == commentId) {
                commentList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvAuthor, tvDate, tvContent;
        private Button btnDelete;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tv_comment_author);
            tvDate = itemView.findViewById(R.id.tv_comment_date);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
            btnDelete = itemView.findViewById(R.id.btn_delete_comment);
        }

        public void bind(Comment comment) {
            tvAuthor.setText(comment.getAuthorName());
            tvContent.setText(comment.getContent());

            if (comment.getCreatedAt() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                tvDate.setText(dateFormat.format(comment.getCreatedAt()));
            } else {
                tvDate.setText("");
            }

            // 자신이 작성한 댓글만 삭제 버튼 표시
            if (currentUserEmail != null && currentUserEmail.equals(comment.getAuthorEmail())) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> {
                    Log.d(TAG, "삭제 버튼 클릭 - 댓글 ID: " + comment.getId()); // 이 로그 추가
                    if (onCommentDeleteListener != null) {
                        onCommentDeleteListener.onCommentDelete(comment.getId());
                    }
                });
            } else {
                btnDelete.setVisibility(View.GONE);
            }
        }
    }
}
