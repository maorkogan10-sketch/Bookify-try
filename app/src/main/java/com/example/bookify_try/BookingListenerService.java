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

// הסרוויס שאחראי על ההקפצת התראות לבעל העסק כשהלקוח מזמין בזמן אמת
public class BookingListenerService extends Service {
    private static final String TAG = "BookingService";
    //חיבור לפיירבייס
    private FirebaseFirestore db;
    //משתנה שישמור את הרישום למאזין כדי שיהיה אפשר לבטל
    private ListenerRegistration listenerRegistration;
    
    // משתנה שעוזר לדעת אם זו הטעינה הראשונה מהענן כדי לא להקפיץ התראות ישנות
    private boolean isInitialLoad = true;

    @Override
    public void onCreate() {
        super.onCreate();
        //חיבור לפירבייס
        db = FirebaseFirestore.getInstance();
        //פתיחת ערוץ התראות בעזרת הNotificationHelper שבנינו
        NotificationHelper.createNotificationChannel(this);
        Log.d(TAG, "Service Created");
    }

    @Override
    //הפונקציה שמתחילה את הסרוויס. מקבלת את האינטנט הID והדגלים. פונקציית חובה למימוש בסרוויס
    public int onStartCommand(Intent intent, int flags, int startId) {
        //בדיקה האם יש מאזין קיים פעיל כדי שלא יווצרו כפילויות של התראות
        if (listenerRegistration == null) {
            Log.d(TAG, "Starting fresh listener for bookings");
            //קריאה לפונקציה שמאזינה להזמנות
            startListeningForBookings();
        } else {
            Log.d(TAG, "Service already listening, skipping re-initialization");
        }
        // START_STICKY פעולה שאומרת למערכת להפעיל את הסרוויס מחדש אם הוא נסגר בטעות
        return START_STICKY;
    }

    // הפונקציה המרכזית שמאזינה לשינויים בהזמנות בפיירסטור
    private void startListeningForBookings() {
        //משתנה שמחזיק את הID של בעל העסק
        String ownerId = FirebaseAuth.getInstance().getUid();
        if (ownerId == null) {
            Log.e(TAG, "Error: ownerId is null!");
            //סיום הסרוויס
            stopSelf();
            return;
        }
//מאפסים את המשתנה של ההעלאה ההתחלתית לפני החיבור לענן
        isInitialLoad = true;

        // מגדירים מאזין של SnapshotListener על אוסף ההזמנות של העסק
        listenerRegistration = db.collection("bookings")
                //הולך לאוסף לפי האיידי של בעל העסק
                .whereEqualTo("businessId", ownerId)
                //מוסיף מאזין חדש - עכשיו הוא ער לכל שינוי
                .addSnapshotListener((value, error) -> {
                    //אם הייתה שגיאה
                    if (error != null) {
                        Log.e(TAG, "Firestore Listen failed: ", error);
                        return;
                    }

                    // אם המאזין התחבר ויש נתונים מהענן
                    if (value != null) {
                        Log.d(TAG, "Snapshot received. Changes size: " + value.getDocumentChanges().size());

                        // אם זו הפעם הראשונה שהמאזין מתחבר הוא מקבל את כל ההיסטוריה
                        if (isInitialLoad) {
                            //שינוי המשתנה של ההעלאה הראשונה לשלילי
                            isInitialLoad = false;
                            Log.d(TAG, "Initial load processed. Now listening for NEW changes.");
                            return;
                        }

                        /// /
                        // לולאה שעוברת על כל שינוי שקרה בנתונים מאז שהמאזין היה מחובר בפעם האחרונה
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            //לוקחים כל הזמנה שעוברים עליה בלולאה והופכים אותה ממסמך לאובייקט
                            Booking booking = dc.getDocument().toObject(Booking.class);
                            
                            // בודקים את סוג השינוי של ההזמנה. אם סוג השינוי הוא סוג ADDED - נוסף, זה אומר שההזמנה היא הזמנה חדשה
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Log.d(TAG, "New booking added in real-time!");
                                //מקפיץ את ערוץ ההתראות
                                NotificationHelper.showNotification(
                                        this,
                                        "הזמנה חדשה!",
                                        "לקוח הזמין תור ל-" + booking.getResourceName()
                                );
                            } 
                            // אם סוג השינוי הוא REMOVED - ביטול, זה אומר שהלקוח ביטל הזמנה קיימת
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

    //פונקציית ניקיון שחובה להשתמש בה ולממש אותה
    @Override
    public void onDestroy() {
        super.onDestroy();
        // כשסוגרים את הסרוויס בהתנתקות, אני מפסיק את ההאזנה לפיירבייס
        if (listenerRegistration != null) {
            //מנתקים את קו ההאזנה לפיירבייס כדי לא לבזבז סוללה
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        Log.d(TAG, "Service Destroyed");
    }

    //פונקציה שחובה לממש שמשמשת לחיבור בין האקטיביטי לסרוויס
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // אני לא משתמש ב-Binding בסרוויס הזה אבל אני חייב לממש את זה כי אני יורש מABSTRACT
        return null;
    }
}