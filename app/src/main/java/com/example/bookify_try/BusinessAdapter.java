package com.example.bookify_try;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BusinessAdapter extends RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder> {

    private final List<Business> businessList = new ArrayList<>();
    private OnItemClickListener listener;

    @NonNull
    @Override
    public BusinessViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.business_list_item, parent, false);
        return new BusinessViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BusinessViewHolder holder, int position) {
        Business business = businessList.get(position);
        holder.businessNameTextView.setText(business.getBusinessName());
    }

    @Override
    public int getItemCount() {
        return businessList.size();
    }

    public void submitList(List<Business> newBusinessList) {
        businessList.clear();
        businessList.addAll(newBusinessList);
        notifyDataSetChanged();
    }

    // Click Listener Interface
    public interface OnItemClickListener {
        void onItemClick(Business business);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    class BusinessViewHolder extends RecyclerView.ViewHolder {
        TextView businessNameTextView;

        public BusinessViewHolder(@NonNull View itemView) {
            super(itemView);
            businessNameTextView = itemView.findViewById(R.id.businessNameTextView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(businessList.get(position));
                }
            });
        }
    }
}