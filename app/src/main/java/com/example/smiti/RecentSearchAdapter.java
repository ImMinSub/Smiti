package com.example.smiti;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RecentSearchAdapter extends RecyclerView.Adapter<RecentSearchAdapter.ViewHolder> {

    private List<String> recentSearches;
    private OnRecentSearchInteractionListener listener;

    public interface OnRecentSearchInteractionListener {
        void onRecentSearchClicked(String query);
        void onRecentSearchDeleteClicked(String query);
    }

    public RecentSearchAdapter(List<String> recentSearches, OnRecentSearchInteractionListener listener) {
        this.recentSearches = recentSearches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String query = recentSearches.get(position);
        holder.tvQuery.setText(query);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecentSearchClicked(query);
            }
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecentSearchDeleteClicked(query);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recentSearches != null ? recentSearches.size() : 0;
    }

    public void updateData(List<String> newSearches) {
        this.recentSearches.clear();
        this.recentSearches.addAll(newSearches);
        notifyDataSetChanged();
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuery;
        ImageButton btnDelete;
        // ImageView ivIcon; // item_recent_search.xml 에 따라

        ViewHolder(View itemView) {
            super(itemView);
            tvQuery = itemView.findViewById(R.id.tv_recent_search_query);
            btnDelete = itemView.findViewById(R.id.btn_delete_recent_search_item);
            // ivIcon = itemView.findViewById(R.id.iv_recent_search_icon);
        }
    }
}
