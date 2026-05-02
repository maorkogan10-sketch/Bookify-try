package com.example.bookify_try;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
/// /
public class DailyBookingsAdapter extends RecyclerView.Adapter<DailyBookingsAdapter.ViewHolder> {

    //יוצר רשימה של אובייקטים מסוג BookingWithUser (מכיל הזמנה ויוזר)
    private final List<BookingWithUser> bookings = new ArrayList<>();
    //מאזין כשלוחצים על איבר ברשימה
    private OnItemClickListener listener;

    //פונקצייה שחייב לרשת
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.booking_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingWithUser item = bookings.get(position);
        if (item.getUser() != null) {
            holder.customerNameTextView.setText(item.getUser().getFullName());
        }
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public void updateData(List<BookingWithUser> newBookings) {
        bookings.clear();
        bookings.addAll(newBookings);
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(BookingWithUser item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView customerNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            customerNameTextView = itemView.findViewById(R.id.customerNameTextView);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(bookings.get(position));
                }
            });
        }
    }
}
////