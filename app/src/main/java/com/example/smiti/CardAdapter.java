package com.example.smiti; // 실제 패키지 이름으로 변경해주세요.


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private Context context;
    private List<CardItem> cardItems; // 이 리스트는 CardDataHolder의 리스트를 참조하게 됩니다.
    private OnItemInteractionListener listener;
    private AdapterType adapterType;

    // 어댑터 타입을 구분하기 위한 Enum
    public enum AdapterType {
        POPULAR, SMBTI, OTHER // 필요에 따라 추가
    }

    public interface OnItemInteractionListener {
        void onItemClick(CardItem item);
        void onDeleteClick(int position);
    }

    // 생성자 수정
    public CardAdapter(Context context, List<CardItem> cardItems, OnItemInteractionListener listener, AdapterType adapterType) {
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

        holder.imageView.setImageResource(currentItem.getImageResource());
        holder.titleView.setText(currentItem.getTitle());
        holder.subtitleView.setText(currentItem.getSubtitle());

        // 삭제 버튼 가시성 로직
        // HomeDashboardActivity 내의 '인기 그룹' RecyclerView에 사용될 때만 삭제 버튼 표시
        if (context instanceof HomeDashboardActivity && adapterType == AdapterType.POPULAR) {
            // HomeDashboardActivity의 getPopularGroupsAdapterInstance()를 통해
            // 현재 어댑터가 '인기 그룹' 어댑터인지 확인하는 로직도 추가할 수 있으나,
            // AdapterType으로 구분하는 것이 더 명확할 수 있습니다.
            // 여기서는 AdapterType.POPULAR이면 삭제 버튼을 보여줍니다.
            holder.deleteButton.setVisibility(View.VISIBLE);
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentItem);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(currentPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return cardItems == null ? 0 : cardItems.size();
    }

    // Activity/Fragment에서 CardDataHolder의 데이터가 변경된 후 호출될 메소드들
    public void notifyNewItemAdded(int position) {
        notifyItemInserted(position);
    }

    public void notifyItemWasRemoved(int position) {
        notifyItemRemoved(position);
        // 삭제 후 나머지 아이템들의 인덱스가 변경될 수 있으므로,
        // 필요에 따라 notifyItemRangeChanged를 호출하여 뷰를 갱신합니다.
        notifyItemRangeChanged(position, cardItems.size());
    }

    // 데이터셋 전체가 변경되었을 때 (예: 필터링, 정렬)
    public void updateData(List<CardItem> newItems) {
        this.cardItems = newItems; // 새 데이터 리스트로 교체
        notifyDataSetChanged();    // 전체 새로고침
    }


    static class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleView;
        TextView subtitleView;
        ImageButton deleteButton; // item_card.xml의 ID와 일치해야 함 (btn_delete_card)

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.item_image);
            titleView = itemView.findViewById(R.id.item_title);
            subtitleView = itemView.findViewById(R.id.item_subtitle);
            deleteButton = itemView.findViewById(R.id.btn_delete_card);
        }
    }
}
