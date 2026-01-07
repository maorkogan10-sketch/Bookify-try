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

    public static final String EXTRA_YEAR = "YEAR";
    public static final String EXTRA_MONTH = "MONTH";
    public static final String EXTRA_DAY = "DAY";

    private DailyBookingsAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_bookings);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        TextView dateTitleTextView = findViewById(R.id.dateTitleTextView);
        RecyclerView recyclerView = findViewById(R.id.dailyBookingsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DailyBookingsAdapter();
        recyclerView.setAdapter(adapter);

        int year = getIntent().getIntExtra(EXTRA_YEAR, -1);
        int month = getIntent().getIntExtra(EXTRA_MONTH, -1);
        int day = getIntent().getIntExtra(EXTRA_DAY, -1);

        if (year == -1) {
            finish();
            return;
        }

        dateTitleTextView.setText(String.format(Locale.getDefault(), "הזמנות עבור %d/%d/%d", day, month + 1, year));

        setupClickListener();
        loadBookingsForDate(year, month, day);
    }

    private void loadBookingsForDate(int year, int month, int day) {
        if (mAuth.getCurrentUser() == null) return;

        Calendar startCal = Calendar.getInstance();
        startCal.set(year, month, day, 0, 0, 0);
        Timestamp startOfDay = new Timestamp(startCal.getTime());

        Calendar endCal = Calendar.getInstance();
        endCal.set(year, month, day, 23, 59, 59);
        Timestamp endOfDay = new Timestamp(endCal.getTime());

        String businessId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Loading bookings for businessId: " + businessId + " between " + startOfDay.toDate() + " and " + endOfDay.toDate());

        db.collection("bookings")
                .whereEqualTo("businessId", businessId)
                .whereGreaterThanOrEqualTo("startTime", startOfDay)
                .whereLessThanOrEqualTo("startTime", endOfDay)
                .get()
                .addOnSuccessListener(bookingSnapshots -> {
                    if (bookingSnapshots.isEmpty()) {
                        Log.d(TAG, "Query successful, but no bookings found for this date.");
                        Toast.makeText(this, "אין הזמנות לתאריך זה", Toast.LENGTH_SHORT).show();
                        adapter.updateData(new ArrayList<>()); // Clear the list
                        return;
                    }

                    Log.d(TAG, "Found " + bookingSnapshots.size() + " bookings for this date. Fetching user details...");
                    List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
                    List<Booking> bookings = new ArrayList<>();

                    for (QueryDocumentSnapshot bookingDoc : bookingSnapshots) {
                        Booking booking = bookingDoc.toObject(Booking.class);
                        bookings.add(booking);
                        Log.d(TAG, "- Booking for resource '" + booking.getResourceName() + "', customerId: " + booking.getCustomerId());
                        userTasks.add(db.collection("users").document(booking.getCustomerId()).get());
                    }

                    Tasks.whenAllSuccess(userTasks).addOnSuccessListener(userSnapshots -> {
                        Log.d(TAG, "Successfully fetched details for " + userSnapshots.size() + " users.");
                        List<BookingWithUser> combinedList = new ArrayList<>();
                        for (int i = 0; i < bookings.size(); i++) {
                            DocumentSnapshot userDoc = (DocumentSnapshot) userSnapshots.get(i);
                            if(userDoc.exists()){
                                User user = userDoc.toObject(User.class);
                                combinedList.add(new BookingWithUser(bookings.get(i), user));
                            } else {
                                Log.w(TAG, "User document not found for customerId: " + bookings.get(i).getCustomerId());
                            }
                        }
                        Log.d(TAG, "Submitting " + combinedList.size() + " combined items to adapter.");
                        adapter.updateData(combinedList);
                    }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch user details", e));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "<<<<< FAILED to load daily bookings >>>>>", e);
                    Toast.makeText(this, "Error loading bookings: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupClickListener() {
        adapter.setOnItemClickListener(item -> {
            Booking booking = item.getBooking();
            User user = item.getUser();
            if (booking == null || user == null) return;

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String bookingInfo = "משאב: " + booking.getResourceName() + "\n"
                    + "שעות: " + timeFormat.format(booking.getStartTime().toDate()) + " - " + timeFormat.format(booking.getEndTime().toDate()) + "\n"
                    + "מייל לקוח: " + user.getEmail();

            new AlertDialog.Builder(this)
                    .setTitle("פרטי הזמנה של " + user.getFullName())
                    .setMessage(bookingInfo)
                    .setPositiveButton("אישור", null)
                    .show();
        });
    }
}