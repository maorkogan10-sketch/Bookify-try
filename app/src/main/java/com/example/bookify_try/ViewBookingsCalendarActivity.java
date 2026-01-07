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

        CalendarView calendarView = findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Intent intent = new Intent(ViewBookingsCalendarActivity.this, DailyBookingsActivity.class);
            intent.putExtra(DailyBookingsActivity.EXTRA_YEAR, year);
            intent.putExtra(DailyBookingsActivity.EXTRA_MONTH, month);
            intent.putExtra(DailyBookingsActivity.EXTRA_DAY, dayOfMonth);
            startActivity(intent);
        });
    }
}