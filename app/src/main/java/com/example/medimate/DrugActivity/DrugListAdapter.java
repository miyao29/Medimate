package com.example.medimate.DrugActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medimate.GPT.models.Drug;
import com.example.medimate.R;

import java.util.List;

public class DrugListAdapter extends RecyclerView.Adapter<DrugListAdapter.ViewHolder> {

    public interface OnItemClick {
        void onClick(Drug drug);
    }

    private List<Drug> list;
    private OnItemClick listener;

    public DrugListAdapter(List<Drug> list, OnItemClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drug_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Drug drug = list.get(position);
        holder.title.setText(drug.getName());

        holder.itemView.setOnClickListener(v -> listener.onClick(drug));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.drug_card_title);
        }
    }
}
