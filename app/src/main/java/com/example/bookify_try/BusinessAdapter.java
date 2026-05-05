package com.example.bookify_try;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

//האדפטר שמנהל את רשימת העסקים
public class BusinessAdapter extends RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder> {

    //רשימת העסקים
    private final List<Business> businessList = new ArrayList<>();
    //מאזין של לחיצה על אחד העסקים ברשימה
    private OnItemClickListener listener;

    @NonNull
    // יוצר את הקופסא - אייטם של השורה באמצעות הקוד business_list_item.xml
    @Override
    public BusinessViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //יוצר את הנראות של כל שורה ומחבר אותה לITEM שיצרנו
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.business_list_item, parent, false);
        return new BusinessViewHolder(view);
    }

    //מקבל את ההולדר מהמסך הקודם ואת המיקום של העסק ועושה חיבור כל עסק לXML
    @Override
    public void onBindViewHolder(@NonNull BusinessViewHolder holder, int position) {
        //אובייקט של העסק הנוכחי
        Business business = businessList.get(position);
        //משנה את הטקסט לשם העסק
        holder.businessNameTextView.setText(business.getBusinessName());
    }

    //מספר השורות של הרשימה
    @Override
    public int getItemCount() {
        return businessList.size();
    }

    //מקבלת רשימה ומשנה את רשימת העסקים לפי הרשימה שקיבלה
    public void submitList(List<Business> newBusinessList) {
        businessList.clear();
        businessList.addAll(newBusinessList);
        //פקודה שאומרת שהרשימה השתנתה
        notifyDataSetChanged();
    }

    //ממשחק לחיצה שמי שמשתמש באפטר יורש אותו
    public interface OnItemClickListener {
        //פונקצייה של לחיצה על עסק ברשימה - מקבלת אובייקט של העסק
        void onItemClick(Business business);
    }

    //מאזין שמי שמשתמש באדפטר יכול להשתמש בו
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    //קלאס ששומר את מה שיש בשורה בודדת - מקשר לאייטם
    class BusinessViewHolder extends RecyclerView.ViewHolder {
        //שם העסק
        TextView businessNameTextView;

        public BusinessViewHolder(@NonNull View itemView) {
            super(itemView);
            //מקשר בין הגאבה לXML, משנה את הטקסט הזמני ששמנו בשורה לשם העסק
            businessNameTextView = itemView.findViewById(R.id.businessNameTextView);

            itemView.setOnClickListener(v -> {
                //מיקום השורה
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    //האדפטר נותן את העסק כדי שמי שמשתמש באדפטר יוכל להשתמש בפונקציה של לחיצה על השורה ולקבל את הפרטים של העסק
                    listener.onItemClick(businessList.get(position));
                }
            });
        }
    }
}