package com.example.smiti.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smiti.R;
import com.example.smiti.model.User;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {
    
    private List<User> memberList;
    
    public MemberAdapter(List<User> memberList) {
        this.memberList = memberList;
    }
    
    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User member = memberList.get(position);
        holder.bind(member);
    }
    
    @Override
    public int getItemCount() {
        return memberList.size();
    }
    
    public void updateMembers(List<User> newMembers) {
        this.memberList = newMembers;
        notifyDataSetChanged();
    }
    
    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        
        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tv_member_name);
        }
        
        public void bind(User member) {
            nameTextView.setText(member.getName());
        }
    }
} 
