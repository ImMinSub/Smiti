package com.example.smiti.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smiti.R;
import com.example.smiti.model.UploadedFile;

import java.util.ArrayList;
import java.util.List;

public class UploadedFileAdapter extends RecyclerView.Adapter<UploadedFileAdapter.FileViewHolder> {

    private final List<UploadedFile> files;
    private final Context context;
    private OnFileRemoveListener onFileRemoveListener;

    public interface OnFileRemoveListener {
        void onFileRemove(int position);
    }

    public UploadedFileAdapter(Context context) {
        this.context = context;
        this.files = new ArrayList<>();
    }

    public void setOnFileRemoveListener(OnFileRemoveListener listener) {
        this.onFileRemoveListener = listener;
    }

    public void addFile(UploadedFile file) {
        files.add(file);
        notifyItemInserted(files.size() - 1);
    }

    public void removeFile(int position) {
        if (position >= 0 && position < files.size()) {
            files.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clearFiles() {
        int size = files.size();
        files.clear();
        notifyItemRangeRemoved(0, size);
    }

    public List<UploadedFile> getFiles() {
        return files;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_uploaded_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        UploadedFile file = files.get(position);
        holder.bind(file, position);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivFileIcon;
        private final TextView tvFileName;
        private final TextView tvFileSize;
        private final ImageButton btnRemoveFile;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileIcon = itemView.findViewById(R.id.iv_file_icon);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileSize = itemView.findViewById(R.id.tv_file_size);
            btnRemoveFile = itemView.findViewById(R.id.btn_remove_file);
        }

        public void bind(UploadedFile file, int position) {
            // 파일 이름 설정
            tvFileName.setText(file.getFileName());
            
            // 파일 크기 설정
            tvFileSize.setText(getReadableFileSize(file.getFileSize()));
            
            // 파일 타입에 따른 아이콘 설정
            if (file.isImage()) {
                ivFileIcon.setImageResource(R.drawable.ic_image);
            } else {
                ivFileIcon.setImageResource(R.drawable.ic_file_document);
            }
            
            // 파일 삭제 버튼 클릭 리스너
            btnRemoveFile.setOnClickListener(v -> {
                if (onFileRemoveListener != null) {
                    onFileRemoveListener.onFileRemove(position);
                }
            });
        }
        
        private String getReadableFileSize(long size) {
            if (size <= 0) return "0 B";
            
            final String[] units = new String[] {"B", "KB", "MB", "GB"};
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
            
            return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
        }
    }
} 