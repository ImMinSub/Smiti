package com.example.smiti;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // 타입 일치를 위해 ImageButton import
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    public enum AdapterType {
        POPULAR,
        SMBTI
    }

    private Context context;
    private ArrayList<CardItem> cardItems;
    private OnItemInteractionListener listener;
    private AdapterType adapterType;

    public interface OnItemInteractionListener {
        void onItemClick(CardItem item);
        void onDeleteClick(int position);
    }

    public CardAdapter(Context context, ArrayList<CardItem> cardItems, OnItemInteractionListener listener, AdapterType adapterType) {
        this.context = context;
        this.cardItems = cardItems;
        this.listener = listener;
        this.adapterType = adapterType;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardItem currentItem = cardItems.get(position);

        if (currentItem.getImageResource() != 0) {
            holder.ivCardImage.setImageResource(currentItem.getImageResource());
            holder.ivCardImage.setVisibility(View.VISIBLE);
        } else {
            holder.ivCardImage.setVisibility(View.GONE);
        }

        holder.tvCardTitle.setText(currentItem.getTitle());
        holder.tvCardSubtitle.setText(currentItem.getDescription());
        holder.tvCardCategory.setText(currentItem.getCategory());
        holder.tvCardStudyDate.setText(currentItem.getStudyDateFormatted());
        holder.tvCardMemberCount.setText(currentItem.getCurrentMembers() + "/" + currentItem.getMaxMembers() + " 명");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentItem);
            }
        });

        if (holder.btnDeleteItem != null && listener != null) {
            if (adapterType == AdapterType.POPULAR) {
                holder.btnDeleteItem.setVisibility(View.VISIBLE);
                holder.btnDeleteItem.setOnClickListener(v -> {
                    int currentPosition = holder.getBindingAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        listener.onDeleteClick(currentPosition);
                    }
                });
            } else {
                holder.btnDeleteItem.setVisibility(View.GONE);
            }
        } else if (holder.btnDeleteItem != null) {
            holder.btnDeleteItem.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return cardItems == null ? 0 : cardItems.size();
    }

    public void updateData(List<CardItem> newCardItems) {
        this.cardItems.clear();
        if (newCardItems != null) {
            this.cardItems.addAll(newCardItems);
        }
        notifyDataSetChanged();
    }


    static class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCardImage;
        TextView tvCardTitle;
        TextView tvCardSubtitle;
        TextView tvCardCategory;
        TextView tvCardStudyDate;
        TextView tvCardMemberCount;
        ImageButton btnDeleteItem;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCardImage = itemView.findViewById(R.id.iv_card_image);
            tvCardTitle = itemView.findViewById(R.id.tv_card_title);
            tvCardSubtitle = itemView.findViewById(R.id.tv_card_subtitle);
            tvCardCategory = itemView.findViewById(R.id.tv_card_category);
            tvCardStudyDate = itemView.findViewById(R.id.tv_card_study_date);
            tvCardMemberCount = itemView.findViewById(R.id.tv_card_member_count);
            btnDeleteItem = itemView.findViewById(R.id.btn_delete_item);
        }
    }
}
