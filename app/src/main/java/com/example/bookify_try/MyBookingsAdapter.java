package com.example.bookify_try;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyBookingsAdapter extends RecyclerView.Adapter<MyBookingsAdapter.ViewHolder> {

    private final List<BookingWithBusiness> bookings = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_booking_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingWithBusiness item = bookings.get(position);
        holder.businessNameTextView.setText(item.getBusinessName());
        holder.resourceNameTextView.setText("משאב: " + item.getBooking().getResourceName());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        String startStr = dateFormat.format(item.getBooking().getStartTime().toDate());
        String endStr = timeFormat.format(item.getBooking().getEndTime().toDate());
        
        holder.dateTimeTextView.setText("תאריך ושעה: " + startStr + " - " + endStr);
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public void updateData(List<BookingWithBusiness> newBookings) {
        bookings.clear();
        bookings.addAll(newBookings);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView businessNameTextView, resourceNameTextView, dateTimeTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            businessNameTextView = itemView.findViewById(R.id.businessNameTextView);
            resourceNameTextView = itemView.findViewById(R.id.resourceNameTextView);
            dateTimeTextView = itemView.findViewById(R.id.dateTimeTextView);
        }
    }
}