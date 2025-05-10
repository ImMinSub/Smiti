package com.example.smiti.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smiti.R;
import com.example.smiti.model.Group;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private List<Group> groupList;
    private Context context;
    private OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onJoinClick(Group group);
        void onGroupClick(Group group);
    }

    public GroupAdapter(Context context, List<Group> groupList, OnGroupClickListener listener) {
        this.context = context;
        this.groupList = groupList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groupList.get(position);
        
        holder.nameTextView.setText(group.getName());
        holder.descriptionTextView.setText(group.getDescription());
        holder.memberCountTextView.setText(context.getString(R.string.member_count, group.getMemberCount()));
        holder.categoryTextView.setText(group.getCategory());
        
        // SMBTI 점수 설정 (double → int 변환 수정)
        double mbtiScoreDouble = group.getMbtiScore();
        int mbtiScore = (int) Math.round(mbtiScoreDouble);
        
        holder.mbtiScoreTextView.setText(context.getString(R.string.mbti_compatibility));
        holder.mbtiScoreValueTextView.setText(mbtiScore + "점");
        holder.mbtiScoreProgressBar.setProgress(mbtiScore);
        
        // SMBTI 점수에 따라 색상 설정
        int color;
        if (mbtiScore >= 80) {
            color = context.getResources().getColor(android.R.color.holo_green_dark);
        } else if (mbtiScore >= 60) {
            color = context.getResources().getColor(android.R.color.holo_blue_dark);
        } else if (mbtiScore >= 40) {
            color = context.getResources().getColor(android.R.color.holo_orange_dark);
        } else {
            color = context.getResources().getColor(android.R.color.holo_red_dark);
        }
        holder.mbtiScoreProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
        holder.mbtiScoreValueTextView.setTextColor(color);
        
        holder.joinButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onJoinClick(group);
            }
        });
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGroupClick(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupList != null ? groupList.size() : 0;
    }

    public void updateData(List<Group> newGroupList) {
        this.groupList = newGroupList;
        notifyDataSetChanged();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView descriptionTextView;
        TextView memberCountTextView;
        TextView categoryTextView;
        TextView mbtiScoreTextView;
        TextView mbtiScoreValueTextView;
        ProgressBar mbtiScoreProgressBar;
        Button joinButton;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tv_group_name);
            descriptionTextView = itemView.findViewById(R.id.tv_group_description);
            memberCountTextView = itemView.findViewById(R.id.tv_member_count);
            categoryTextView = itemView.findViewById(R.id.tv_category);
            mbtiScoreTextView = itemView.findViewById(R.id.tv_mbti_score);
            mbtiScoreValueTextView = itemView.findViewById(R.id.tv_mbti_score_value);
            mbtiScoreProgressBar = itemView.findViewById(R.id.progress_mbti_score);
            joinButton = itemView.findViewById(R.id.btn_join);
        }
    }
} 