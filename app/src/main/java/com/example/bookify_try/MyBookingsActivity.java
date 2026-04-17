package com.example.bookify_try;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyBookingsActivity extends AppCompatActivity {

    private static final String TAG = "MyBookingsActivity";

    //משתנה מסוג האדפטר שבנינו
    private MyBookingsAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        //חיבור לפיירבייס
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        //מחבר את הריסייקל ויו לXML - רכיב שיוצר בכל פעם מספר מסוים של רכיבים ברשימה ואז מאפשר לגלול למעלה ולמטה
        RecyclerView recyclerView = findViewById(R.id.myBookingsRecyclerView);
        //מסדר את רכיבים אחד מתחת לשני
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
       //יוצר אובייקט חדש של האדפטר
        adapter = new MyBookingsAdapter();
        //מחבר את הריסייקל ויו לאדפטר
        recyclerView.setAdapter(adapter);
/// /
        // הפונקציה הפנימית בקלאס של האדפטר שמאזינה עד שמישהו לוחץ על כפתור המחיקה - קורא גם לפונקציה שמקפיצה את הדיאלוג
        adapter.setOnDeleteClickListener(booking -> showDeleteConfirmationDialog(booking));
/// /
        //קריאה לפונקציה של העלאת ההזמנות
        loadMyBookings();
    }

    //הפונקציה מקבלת את האובייקט של ההזמנה ומקפיצה דיאלוג ששואל את הבן אדם אם הוא בטוח רוצה למחוק
    private void showDeleteConfirmationDialog(Booking booking) {
        //יוצר דיאלוג חדש מסוג מובנה שיש במערכת ולא אנחנו יצרנו ומעדכן את הפרטים שלו ובסוף מקפיץ אותו
        new AlertDialog.Builder(this)
                .setTitle("ביטול הזמנה")
                .setMessage("האם אתה בטוח שברצונך לבטל הזמנה זו?")
                /// /
                //אם המשתמש לוחץ על כפתור הכן זה מקפיץ את הפונקציה deleteBooking
                .setPositiveButton("כן, בטל", (dialog, which) -> deleteBooking(booking))
                /// /
                .setNegativeButton("לא, חזור", null)
                .show();
    }

    //הפונקציה מקבלת את האובייקט של ההזמנה לא מחזירה כלום ומוחקת את ההזמנה מפיירבייס ומעדכנת את הרשימה
    private void deleteBooking(Booking booking) {
        //הולך לפיירסטור לתיקיית הזמנות ומוחק את ההזמנה עם הID של ההזמנה של האובייקט שקיבל
        db.collection("bookings").document(booking.getBookingId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "ההזמנה בוטלה בהצלחה.", Toast.LENGTH_SHORT).show();
                    // רענון הרשימה לאחר המחיקה
                    loadMyBookings();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting booking", e);
                    Toast.makeText(this, "שגיאה בביטול ההזמנה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    //הפונקציה לא מקבלת ולא מחזירה כלום, יוצרת את הרשימה של ההזמנות של הלקוח ומראה אותה על המסך
    private void loadMyBookings() {
        //אם המשתמש הנוכחי לא מחובר
        if (mAuth.getCurrentUser() == null) return;

        //משתנה ששומר את הID של הלקוח שמחובר
        String customerId = mAuth.getCurrentUser().getUid();
        //יוצר אובייקט של זמן עכשיו כדי לסנן הזמנות שכבר קרו
        Timestamp now = new Timestamp(new Date());

        //הולך לתיקיית הזמנות, רק להזמנות של הלקוח עם הID ששמרנו, שמתחילות אחריי הזמן הנוכחי
        db.collection("bookings")
                .whereEqualTo("customerId", customerId)
                //יצרנו אינדקס בתור הפיירבייס ששואל את השאלה הזאת
                .whereGreaterThan("startTime", now)
                //ממיין את ההזמנות מההכי קרובה להכי רחוקה
                .orderBy("startTime", Query.Direction.ASCENDING)
                //שולח למאזין
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    //יוצר רשימה חדשה של אובייקטים של הזמנות
                    List<Booking> bookings = new ArrayList<>();
                    //יורצ רשימה של ''משימות'' - כל משימה היא להביא את שם העסק של ההזמנה. הרשימה מאגדת את כל המשימות
                    List<Task<DocumentSnapshot>> businessTasks = new ArrayList<>();

                    //לולאה שעוברת על כל המסמכים - הזמנות שעמדו בקריטריונים
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                       //ממיר את המסמך לאובייקט
                        Booking booking = doc.toObject(Booking.class);
                        //מוסיף לרשימה
                        bookings.add(booking);
                        //מוסיף את הID של העסק לרשימת המשימות
                        businessTasks.add(db.collection("businesses").document(booking.getBusinessId()).get());
                    }
                    //אם הרשימה ריקה
                    if (bookings.isEmpty()) {
                        //מנקה את הרשימה על המסך
                        adapter.updateData(new ArrayList<>());
                        Toast.makeText(this, "לא נמצאו הזמנות עתידיות.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //מאזין שמחכה שהמחשב יסיים את כל המשימות - כלומר להביא את כל השמות
                   /// /
                    Tasks.whenAllSuccess(businessTasks).addOnSuccessListener(objects -> {
                        //יוצר רשימה של האובייקט BookingWithBusiness
                        List<BookingWithBusiness> combinedList = new ArrayList<>();
                        for (int i = 0; i < bookings.size(); i++) {
                            //מוציא את המסמך של העסק שמתאים להזמנה
                            DocumentSnapshot businessDoc = (DocumentSnapshot) objects.get(i);
                            //בודק עם העסק קיים - אם כן לוקח את השם שלו
                            String businessName = businessDoc.exists() ? businessDoc.getString("businessName") : "עסק לא ידוע";
                            //מוסיף את האובייקט לרשימת האובייקטים
                            combinedList.add(new BookingWithBusiness(bookings.get(i), businessName));
                        }
                        /// /
                        //שולח לפונקציה של האדפטר את הרשימה כדי שיעלה אותה על המסך
                        adapter.updateData(combinedList);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading bookings", e);
                    Toast.makeText(this, "שגיאה בטעינת הזמנות: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}