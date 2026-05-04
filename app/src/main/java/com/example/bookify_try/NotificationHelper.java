package com.example.bookify_try;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

//המסך לפתיחת ערוץ ההתראות
public class NotificationHelper {
    public static final String CHANNEL_ID = "booking_reminders";
    private static final String CHANNEL_NAME = "Booking Reminders";
    private static final String CHANNEL_DESC = "Notifications for upcoming bookings";

    /// /
    // הפונקציה מקבלת את המסך שבו נמצאים ויוצרת ערוץ שבו יהיה ניתן לשלוח את ההתראות

    public static void createNotificationChannel(Context context) {
        //בודק אם הגרסה הנוכחית של הטלפון מחייבת לפתוח ערוץ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //יוצר אובייקט של ערוץ חדש עם המשתנים שהגדרנו בהתחלה
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESC);

            //מבקש ממערכת ההפעלה של הטלפון את המנהל שאחראי על המשימות
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                //המנהל רושם את הערוץ בתוך מערכת ההפעלה של הטלפון
                manager.createNotificationChannel(channel);
            }
        }
    }
    /// /

    // הפונקציה מקבלת את המסך והטקסטים של ההודעה שרוצים להקפיץ ומראה את ההודעה בפועל
    public static void showNotification(Context context, String title, String message) {
        //מעצב את ההתראה - אייקון, כתובת מודגשת, טקסט...
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        //מנהל התראות שיודע לעבוד עם כל הגרסאות
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        //ונ בדיקת הרשאה בטלפון אם האפליקציה יכולה לשלוח לי התראות ונותנים למערכת מספר ייחודי
        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}