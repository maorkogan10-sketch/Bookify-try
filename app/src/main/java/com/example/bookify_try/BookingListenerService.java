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

public class BookingListenerService extends Service {
    private static final String TAG = "BookingService";
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;
    private boolean isFirstRun = true;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Service Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started");
        startListeningForBookings();
        return START_STICKY;
    }

    private void startListeningForBookings() {
        String ownerId = FirebaseAuth.getInstance().getUid();
        if (ownerId == null) {
            stopSelf();
            return;
        }

        // האזנה בזמן אמת לשינויים בהזמנות של העסק הזה
        listenerRegistration = db.collection("bookings")
                .whereEqualTo("businessId", ownerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        // התעלמות מהריצה הראשונה שטוענת את הנתונים הקיימים
                        if (isFirstRun) {
                            isFirstRun = false;
                            return;
                        }

                        for (DocumentChange dc : value.getDocumentChanges()) {
                            Booking booking = dc.getDocument().toObject(Booking.class);
                            
                            switch (dc.getType()) {
                                case ADDED:
                                    Log.d(TAG, "New booking added: " + booking.getBookingId());
                                    NotificationHelper.showNotification(
                                            this,
                                            "הזמנה חדשה!",
                                            "התקבלה הזמנה חדשה ל-" + booking.getResourceName()
                                    );
                                    break;
                                    
                                case REMOVED:
                                    Log.d(TAG, "Booking canceled: " + booking.getBookingId());
                                    NotificationHelper.showNotification(
                                            this,
                                            "הזמנה בוטלה",
                                            "הזמנה ל-" + booking.getResourceName() + " בוטלה על ידי הלקוח."
                                    );
                                    break;
                                    
                                case MODIFIED:
                                    // אופציונלי: אפשר להוסיף כאן התראה גם על עדכון הזמנה אם תרצה בעתיד
                                    break;
                            }
                        }
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
        Log.d(TAG, "Service Destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}