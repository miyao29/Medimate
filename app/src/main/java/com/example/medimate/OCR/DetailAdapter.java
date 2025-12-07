package com.example.medimate.OCR;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView; // TextView import 추가

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medimate.R;

import java.util.List;

public class DetailAdapter extends RecyclerView.Adapter<DetailAdapter.ViewHolder> {

    // 1. 듣기 기능을 위한 인터페이스 정의
    public interface OnSpeakListener {
        void onSpeak(String text);
    }

    private List<DetailItem> items;
    private OnSpeakListener speakListener;
    private TextView infoDisplayTextView; // 새로 추가된 TextView 변수

    // (DetailItem 클래스는 DetailItem.java 파일에 별도로 존재)

    // 2. 생성자 수정
    // 리스트, 리스너, 그리고 TextView를 받습니다.
    public DetailAdapter(List<DetailItem> items, OnSpeakListener listener, TextView infoDisplayTextView) {
        this.items = items;
        this.speakListener = listener;
        this.infoDisplayTextView = infoDisplayTextView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_detail_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetailItem item = items.get(position);

        // 버튼 제목 설정
        holder.btnTitle.setText(item.title);

        // 버튼 클릭 리스너
        holder.btnTitle.setOnClickListener(v -> {
            if (speakListener != null) {
                speakListener.onSpeak(item.ttsContent);
            }

            if (infoDisplayTextView != null && item.displayContent != null) {
                infoDisplayTextView.setText(item.displayContent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView btnTitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            btnTitle = itemView.findViewById(R.id.tvTitle);
        }
    }
}