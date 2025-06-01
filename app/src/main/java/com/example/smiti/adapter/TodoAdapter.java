package com.example.smiti.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smiti.R;
import com.example.smiti.model.Todo;

import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    private List<Todo> todoList;
    private OnTodoInteractionListener listener;

    public interface OnTodoInteractionListener {
        void onTodoCompletedChanged(Todo todo, boolean isCompleted);
        void onTodoEdit(Todo todo);
        void onTodoDelete(Todo todo);
    }

    public TodoAdapter(List<Todo> todoList, OnTodoInteractionListener listener) {
        this.todoList = todoList;
        this.listener = listener;
    }

    public void updateTodos(List<Todo> newTodos) {
        this.todoList = newTodos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        Todo todo = todoList.get(position);
        holder.taskTextView.setText(todo.getTask());
        holder.dueDateTextView.setText(todo.getDueDate());

        holder.ignoreCheckChange = true;
        holder.completedCheckBox.setChecked(todo.isCompleted());
        holder.ignoreCheckChange = false;

        // 완료 상태에 따라 텍스트에 취소선 적용
        if (todo.isCompleted()) {
            holder.taskTextView.setPaintFlags(holder.taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.taskTextView.setPaintFlags(holder.taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.completedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (holder.ignoreCheckChange) {
                return;
            }
            if (listener != null) {
                listener.onTodoCompletedChanged(todo, isChecked);
            }
        });

        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTodoEdit(todo);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTodoDelete(todo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        CheckBox completedCheckBox;
        TextView taskTextView;
        TextView dueDateTextView;
        ImageButton editButton;
        ImageButton deleteButton;
        boolean ignoreCheckChange = false;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            completedCheckBox = itemView.findViewById(R.id.checkbox_todo_completed);
            taskTextView = itemView.findViewById(R.id.text_view_todo_task);
            dueDateTextView = itemView.findViewById(R.id.text_view_todo_due_date);
            editButton = itemView.findViewById(R.id.button_edit_todo);
            deleteButton = itemView.findViewById(R.id.button_delete_todo);
        }
    }
} 