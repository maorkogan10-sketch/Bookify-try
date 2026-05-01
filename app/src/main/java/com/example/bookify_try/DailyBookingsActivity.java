package com.example.bookify_try;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DailyBookingsActivity extends AppCompatActivity {

    private static final String TAG = "DailyBookingsActivity";

    //שמות האקסטרות שקיבלנו
    public static final String EXTRA_YEAR = "YEAR";
    public static final String EXTRA_MONTH = "MONTH";
    public static final String EXTRA_DAY = "DAY";

    //אובייקט מסוג של האדפטר שיצרנו
    private DailyBookingsAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_bookings);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        //הכותרת מהאקסמל
        TextView dateTitleTextView = findViewById(R.id.dateTitleTextView);
        //חיבור לריסייקל ויו
        RecyclerView recyclerView = findViewById(R.id.dailyBookingsRecyclerView);
        //שמים לייאאוט שישים שורות אחת מתחת לשנייה
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //יצירת אדפטר חדש וחיבור לריסייקל ויו
        adapter = new DailyBookingsAdapter();
        recyclerView.setAdapter(adapter);

        //חיבור האקסטרות למה שקיבלנו
        int year = getIntent().getIntExtra(EXTRA_YEAR, -1);
        int month = getIntent().getIntExtra(EXTRA_MONTH, -1);
        int day = getIntent().getIntExtra(EXTRA_DAY, -1);

        //אם לא התקבלה שנה
        if (year == -1) {
            finish();
            return;
        }

        //שינוי הכותרת לתאריך שהתקבל
        dateTitleTextView.setText(String.format(Locale.getDefault(), "הזמנות עבור %d/%d/%d", day, month + 1, year));

        //אם לוחצים על פריט - פונקציה מהאדפטר
        setupClickListener();
        //קריאה לפונקציה שמעלה את הפרטים והרשימה
        loadBookingsForDate(year, month, day);
    }

    //הפונקציה מקבלת את השנה היום והחודש של התאריך שעליו הבעל עסק לחץ ומעלה את רשימת ההזמנות על המסך
    private void loadBookingsForDate(int year, int month, int day) {
        //אם אין משתמש מחובר
        if (mAuth.getCurrentUser() == null) return;
//יצירת אובייקט של זמן התחלה
        Calendar startCal = Calendar.getInstance();
        //שמים באובייקט את התאריך התחלתי שהתקבל לשעה 00:00
        startCal.set(year, month, day, 0, 0, 0);
        //ממירים את השעה לשעה מסוג Timestamp
        Timestamp startOfDay = new Timestamp(startCal.getTime());

        //יוצרים אובייקט של זמן סיום
        Calendar endCal = Calendar.getInstance();
        //שמים בו את התאריך שהתקבל עם השעה 23:59
        endCal.set(year, month, day, 23, 59, 59);
        Timestamp endOfDay = new Timestamp(endCal.getTime());

        //לוקחים את הID של העסק/בעל העסק
        String businessId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Loading bookings for businessId: " + businessId + " between " + startOfDay.toDate() + " and " + endOfDay.toDate());

        //הולכים לאוסף של ההזמנות לבעל העסק הזה
        db.collection("bookings")
                .whereEqualTo("businessId", businessId)
                //שמקיימות בין הזמן התחלה לזמן סיום
                .whereGreaterThanOrEqualTo("startTime", startOfDay)
                .whereLessThanOrEqualTo("startTime", endOfDay)
                .get()
                .addOnSuccessListener(bookingSnapshots -> {
                    //אם אין עסקים שעונים על התנאים האלו נשלח טואוסט
                    if (bookingSnapshots.isEmpty()) {
                        Log.d(TAG, "Query successful, but no bookings found for this date.");
                        Toast.makeText(this, "אין הזמנות לתאריך זה", Toast.LENGTH_SHORT).show();
                        //ננקה את הרשימה של האדפטר
                        adapter.updateData(new ArrayList<>());
                        return;
                    }

                    Log.d(TAG, "Found " + bookingSnapshots.size() + " bookings for this date. Fetching user details...");
                    //רשימה של משימות
                    List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
                    //רשימה של הזמנות חדשות
                    List<Booking> bookings = new ArrayList<>();

                    //לולאה שעוברת על כל הצילומי מסך שחזרו
                    for (QueryDocumentSnapshot bookingDoc : bookingSnapshots) {
                        //המרת כל צילום מסך שחזר לאובייקט של הזמנה
                        Booking booking = bookingDoc.toObject(Booking.class);
                        //הוספה לרשימת ההזמנות
                        bookings.add(booking);
                        Log.d(TAG, "- Booking for resource '" + booking.getResourceName() + "', customerId: " + booking.getCustomerId());
                        //מוסיפים את ההזמנה לרשימת המשימות ונותנים משימה להביא את המשתמש שהID שלו בהזמנה
                        userTasks.add(db.collection("users").document(booking.getCustomerId()).get());
                    }

                    //כשכל המשימות הסתיימו
                    Tasks.whenAllSuccess(userTasks).addOnSuccessListener(userSnapshots -> {
                        Log.d(TAG, "Successfully fetched details for " + userSnapshots.size() + " users.");
                        //רשימה של אובייקט מסוג BookingWithUser - שמחזיק גם את ההזמנה וגם את המשתמש ביחד
                        List<BookingWithUser> combinedList = new ArrayList<>();
                        for (int i = 0; i < bookings.size(); i++) {
                            //המשתמש מהרשימה של המשימות שביקשנו להביא
                            DocumentSnapshot userDoc = (DocumentSnapshot) userSnapshots.get(i);
                            if(userDoc.exists()){
                                //המרת המשימה למשתמש
                                User user = userDoc.toObject(User.class);
                                //הוספה לרשימה - את ההזמנה ואת המשתמש
                                combinedList.add(new BookingWithUser(bookings.get(i), user));
                            } else {
                                Log.w(TAG, "User document not found for customerId: " + bookings.get(i).getCustomerId());
                            }
                        }
                        Log.d(TAG, "Submitting " + combinedList.size() + " combined items to adapter.");
                        //שליחת הרשימה לאדפטר
                        adapter.updateData(combinedList);
                    }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch user details", e));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "<<<<< FAILED to load daily bookings >>>>>", e);
                    Toast.makeText(this, "Error loading bookings: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    //אם לוחצים על אחד האיברים ברשימה הפונקציה קוראת
    private void setupClickListener() {
        //כאשר לוחצים על רכיב ברשימה
        adapter.setOnItemClickListener(item -> {
            //אובייקט של ההזמנה שלחצו עליה
            Booking booking = item.getBooking();
            // אובייקט של המשתמש שהזמין
            User user = item.getUser();
            //אם אחד מהם ריק
            if (booking == null || user == null) return;

            //יצירת פורמט פשוט ונוח להצגת הזמן בצורה של XX:YY
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            //בניית המחרוזת שתופיע בדיאלוג
            String bookingInfo = "משאב: " + booking.getResourceName() + "\n"
                    + "שעות: " + timeFormat.format(booking.getStartTime().toDate()) + " - " + timeFormat.format(booking.getEndTime().toDate()) + "\n"
                    + "מייל לקוח: " + user.getEmail();

            //שימוש בדיאלוג קיים והקפצה שלו + כפתור אישור
            new AlertDialog.Builder(this)
                    .setTitle("פרטי הזמנה של " + user.getFullName())
                    .setMessage(bookingInfo)
                    .setPositiveButton("אישור", null)
                    .show();
        });
    }
}