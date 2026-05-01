package com.example.bookify_try;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CalendarView;

import androidx.appcompat.app.AppCompatActivity;

public class ViewBookingsCalendarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_bookings_calendar);

        //התחברות לקלנדר בXML
        CalendarView calendarView = findViewById(R.id.calendarView);
        
        // הגבלת לוח השנה בשביל שיהיה אפשר לבחור רק מהתאריך הנוכחי והלאה
        calendarView.setMinDate(System.currentTimeMillis() - 1000);

        //כאשר לחצו על תאריך מסוים
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            //תעבור למסך של ההזמנות היומיות
            Intent intent = new Intent(ViewBookingsCalendarActivity.this, DailyBookingsActivity.class);
            //תשלח עם האינטנט את היום, החודש, והשנה 
            intent.putExtra(DailyBookingsActivity.EXTRA_YEAR, year);
            intent.putExtra(DailyBookingsActivity.EXTRA_MONTH, month);
            intent.putExtra(DailyBookingsActivity.EXTRA_DAY, dayOfMonth);
            startActivity(intent);
        });
    }
}