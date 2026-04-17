package com.example.bookify_try;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyBookingsAdapter extends RecyclerView.Adapter<MyBookingsAdapter.ViewHolder> {

    //יוצר רשימה של אובייקטים מסוג BookingWithBusiness
    private final List<BookingWithBusiness> bookings = new ArrayList<>();
    //יוצר משתנה של מאזין שמאזין לכפתור המחיקה
    private OnDeleteClickListener deleteClickListener;

    //ממשק של פונקציית המחיקה אותו האקטיביטי תירש ותממש
    public interface OnDeleteClickListener {
        void onDeleteClick(Booking booking);
    }
//הפונקציה מקבלת את המאזין ומנהלת את המחיקה
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
       //משתנה של המאזין מהאקטיביטי
        this.deleteClickListener = listener;
    }

    //הפוקנציה יוצרת את השלד של כל שורה ברשימה (ViewHolder שהוא קלאס פנימי שיצרנו) - היא מקבלת את ה RecyclerView עצמו
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //לוקחת את הITEM שיצרנו לשורה בודדת והופכת אותו לאובייקט תצוגה VIEW
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_booking_item, parent, false);
       //מחזיר אובייקט ViewHolder חדש שמחזיק את השורה שיצרנו
        return new ViewHolder(view);
    }

    //הפונקציה מקבל את השלד של האיייטם (הVIEW שיצרנו) ואת המיקום של השורה ומכניסה את הנתונים של ההזמנה בשורה הזאת אל השלד
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //ניגשים לרשימת ההזמנות ומוציאים את האובייקט שנמצא במיקום הנוכחי
        BookingWithBusiness item = bookings.get(position);
        //לוקח את שם העסק ושם אותו בשלד
        holder.businessNameTextView.setText(item.getBusinessName());
        //לוקח את שם המשאב וגם אותו בשלד
        holder.resourceNameTextView.setText("משאב: " + item.getBooking().getResourceName());

        //יוצר 2 תאריכים שאחד מראה תאריך ושעה והשני שעה
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        //ממלא את התאריך של ההתחלה - תאריך ושעה והסיום - שעה
        String startStr = dateFormat.format(item.getBooking().getStartTime().toDate());
        String endStr = timeFormat.format(item.getBooking().getEndTime().toDate());

        holder.dateTimeTextView.setText("תאריך ושעה: " + startStr + " - " + endStr);

        //כאשר המשתמש לוחץ על כפתור הפח
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                //מפעיל את פונקציית המחיקה שתקרה באקטיביטי
                deleteClickListener.onDeleteClick(item.getBooking());
            }
        });
    }

    //פונקציה שמחזירה את מספר האיברים ברשימה
    @Override
    public int getItemCount() {
        return bookings.size();
    }

    //פונקצייה שמעדכנת את הרשימה ומזינה בה את הפרטים - נקראת מהאקטיבטי
    public void updateData(List<BookingWithBusiness> newBookings) {
        //מנקה את הרשימה
        bookings.clear();
        //מכניס את כל המידע החדש שנכנס
        bookings.addAll(newBookings);
        //פקודה שאומרת לצייר את הרשימה על המסך
        notifyDataSetChanged();
    }

    //קלאס פנימי ששומר על הנתונים של כל ITEM ברשימה
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView businessNameTextView, resourceNameTextView, dateTimeTextView;
        ImageButton deleteButton;
//מחבר את הכפתורים והטקסטים
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            businessNameTextView = itemView.findViewById(R.id.businessNameTextView);
            resourceNameTextView = itemView.findViewById(R.id.resourceNameTextView);
            dateTimeTextView = itemView.findViewById(R.id.dateTimeTextView);
            deleteButton = itemView.findViewById(R.id.deleteBookingButton);
        }
    }
}