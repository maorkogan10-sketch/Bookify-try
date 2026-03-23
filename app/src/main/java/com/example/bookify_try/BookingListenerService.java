package com.example.bookify_try;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

// זה הסרוויס שלי שאחראי על סנכרון הנתונים בזמן אמת וקפיצת התראות לבעל העסק
public class BookingListenerService extends Service {
    private static final String TAG = "BookingService";
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;
    
    // משתנה שעוזר לי לדעת אם זו הטעינה הראשונה מהענן כדי לא להקפיץ התראות ישנות
    private boolean isInitialLoad = true;

    @Override
    public void onCreate() {
        super.onCreate();
        // כאן אני מאתחל את הגישה למסד הנתונים ואת ערוץ ההתראות בטלפון
        db = FirebaseFirestore.getInstance();
        NotificationHelper.createNotificationChannel(this);
        Log.d(TAG, "Service Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // הפונקציה הזו רצה כשהסרוויס מתחיל. אני בודק אם כבר יש מאזין פעיל
        // כדי שלא ייוצרו כפילויות של התראות בכל פעם שנכנסים למסך
        if (listenerRegistration == null) {
            Log.d(TAG, "Starting fresh listener for bookings");
            startListeningForBookings();
        } else {
            Log.d(TAG, "Service already listening, skipping re-initialization");
        }
        // START_STICKY אומר למערכת להפעיל את הסרוויס מחדש אם הוא נסגר בטעות
        return START_STICKY;
    }

    // הפונקציה המרכזית שמאזינה לשינויים בהזמנות ב-Firestore
    private void startListeningForBookings() {
        String ownerId = FirebaseAuth.getInstance().getUid();
        if (ownerId == null) {
            Log.e(TAG, "Error: ownerId is null!");
            stopSelf();
            return;
        }

        isInitialLoad = true;

        // כאן אני מגדיר מאזין בזמן אמת (SnapshotListener) על קולקציית ההזמנות ששייכות לעסק שלי
        listenerRegistration = db.collection("bookings")
                .whereEqualTo("businessId", ownerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Firestore Listen failed: ", error);
                        return;
                    }

                    if (value != null) {
                        Log.d(TAG, "Snapshot received. Changes size: " + value.getDocumentChanges().size());

                        // אם זו הפעם הראשונה שהמאזין מתחבר, הוא מקבל את כל ההיסטוריה.
                        // אני רק מסמן שהטעינה הסתיימה ולא שולח התראות על העבר.
                        if (isInitialLoad) {
                            isInitialLoad = false;
                            Log.d(TAG, "Initial load processed. Now listening for NEW changes.");
                            return;
                        }

                        // כאן אני עובר על כל שינוי שקרה במסד הנתונים מאז שהמאזין התחבר
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            Booking booking = dc.getDocument().toObject(Booking.class);
                            
                            // אם סוג השינוי הוא ADDED - זה אומר שלקוח ביצע הזמנה חדשה עכשיו
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Log.d(TAG, "New booking added in real-time!");
                                NotificationHelper.showNotification(
                                        this,
                                        "הזמנה חדשה!",
                                        "לקוח הזמין תור ל-" + booking.getResourceName()
                                );
                            } 
                            // אם סוג השינוי הוא REMOVED - זה אומר שהלקוח ביטל הזמנה קיימת
                            else if (dc.getType() == DocumentChange.Type.REMOVED) {
                                Log.d(TAG, "Booking removed in real-time!");
                                NotificationHelper.showNotification(
                                        this,
                                        "הזמנה בוטלה",
                                        "התור ל-" + booking.getResourceName() + " בוטל על ידי הלקוח."
                                );
                            }
                        }
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // כשסוגרים את הסרוויס (למשל בהתנתקות), אני מפסיק את ההאזנה ל-Firestore
        // כדי לא לבזבז סוללה ומשאבי מערכת
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        Log.d(TAG, "Service Destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // אנחנו לא משתמשים ב-Binding בסרוויס הזה אבל אני חייב לממש את זה כי אני יורש מABSTRACT
        return null;
    }
}