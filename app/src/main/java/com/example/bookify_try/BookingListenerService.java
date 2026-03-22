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
    private boolean isInitialLoad = true;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        NotificationHelper.createNotificationChannel(this);
        Log.d(TAG, "Service Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // חשוב: אנחנו מפעילים את המאזין רק אם הוא לא קיים כבר
        // זה מונע איפוס של השירות בכל פעם שבעל העסק חוזר למסך הבית
        if (listenerRegistration == null) {
            Log.d(TAG, "Starting fresh listener for bookings");
            startListeningForBookings();
        } else {
            Log.d(TAG, "Service already listening, skipping re-initialization");
        }
        return START_STICKY;
    }

    private void startListeningForBookings() {
        String ownerId = FirebaseAuth.getInstance().getUid();
        if (ownerId == null) {
            Log.e(TAG, "Error: ownerId is null!");
            stopSelf();
            return;
        }

        isInitialLoad = true;

        // האזנה לכל ההזמנות ששייכות לבעל העסק הזה
        listenerRegistration = db.collection("bookings")
                .whereEqualTo("businessId", ownerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Firestore Listen failed: ", error);
                        return;
                    }

                    if (value != null) {
                        Log.d(TAG, "Snapshot received. Changes size: " + value.getDocumentChanges().size());

                        // Snapshot הראשון תמיד נחשב כטעינה ראשונית של המידע הקיים
                        if (isInitialLoad) {
                            isInitialLoad = false;
                            Log.d(TAG, "Initial load processed. Now listening for NEW changes.");
                            return;
                        }

                        // רק שינויים שקורים מעכשיו והלאה יטופלו כאן
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            Booking booking = dc.getDocument().toObject(Booking.class);
                            
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Log.d(TAG, "New booking added in real-time!");
                                NotificationHelper.showNotification(
                                        this,
                                        "הזמנה חדשה!",
                                        "לקוח הזמין תור ל-" + booking.getResourceName()
                                );
                            } else if (dc.getType() == DocumentChange.Type.REMOVED) {
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
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        Log.d(TAG, "Service Destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}