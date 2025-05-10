package com.example.smiti.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smiti.R;
import com.example.smiti.model.TimeSlot;

import java.util.List;

/**
 * 스터디 가능 시간 슬롯을 보여주는 RecyclerView 어댑터
 */
public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {

    private List<TimeSlot> timeSlots;
    private Context context;
    private OnTimeSlotClickListener listener;

    /**
     * 시간 슬롯 클릭 이벤트 처리를 위한 인터페이스
     */
    public interface OnTimeSlotClickListener {
        void onTimeSlotRemove(int position);
    }

    public TimeSlotAdapter(Context context, List<TimeSlot> timeSlots, OnTimeSlotClickListener listener) {
        this.context = context;
        this.timeSlots = timeSlots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_time_slot, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        TimeSlot timeSlot = timeSlots.get(position);
        holder.timeRangeText.setText(timeSlot.getTimeRangeString());

        // 삭제 버튼 클릭 이벤트
        holder.removeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTimeSlotRemove(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return timeSlots != null ? timeSlots.size() : 0;
    }

    /**
     * 데이터 업데이트
     */
    public void updateData(List<TimeSlot> newTimeSlots) {
        this.timeSlots = newTimeSlots;
        notifyDataSetChanged();
    }

    /**
     * 시간 슬롯 ViewHolder 클래스
     */
    static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        TextView timeRangeText;
        ImageButton removeButton;

        public TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            timeRangeText = itemView.findViewById(R.id.tv_time_range);
            removeButton = itemView.findViewById(R.id.btn_remove);
        }
    }
} 