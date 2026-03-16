package com.example.bookify_try;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "!!! ReminderReceiver received the broadcast !!!");
        
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");

        if (title == null) title = "תזכורת להזמנה";
        if (message == null) message = "יש לך הזמנה למחר!";

        Log.d(TAG, "Showing notification: " + title + " - " + message);

        // מקפית את ההתראה בעזרת HELPER שיצרתי לפני
        NotificationHelper.showNotification(context, title, message);
    }
}