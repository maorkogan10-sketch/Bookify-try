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

    private MyBookingsAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        RecyclerView recyclerView = findViewById(R.id.myBookingsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyBookingsAdapter();
        recyclerView.setAdapter(adapter);

        // הגדרת מאזין למחיקה מהאדפטר
        adapter.setOnDeleteClickListener(booking -> showDeleteConfirmationDialog(booking));

        loadMyBookings();
    }

    /**
     * הצגת דיאלוג אישור לפני מחיקת הזמנה
     */
    private void showDeleteConfirmationDialog(Booking booking) {
        new AlertDialog.Builder(this)
                .setTitle("ביטול הזמנה")
                .setMessage("האם אתה בטוח שברצונך לבטל הזמנה זו?")
                .setPositiveButton("כן, בטל", (dialog, which) -> deleteBooking(booking))
                .setNegativeButton("לא, חזור", null)
                .show();
    }

    /**
     * מחיקת ההזמנה מ-Firestore ועדכון הרשימה
     */
    private void deleteBooking(Booking booking) {
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

    private void loadMyBookings() {
        if (mAuth.getCurrentUser() == null) return;

        String customerId = mAuth.getCurrentUser().getUid();
        Timestamp now = new Timestamp(new Date());

        db.collection("bookings")
                .whereEqualTo("customerId", customerId)
                .whereGreaterThan("startTime", now)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Booking> bookings = new ArrayList<>();
                    List<Task<DocumentSnapshot>> businessTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Booking booking = doc.toObject(Booking.class);
                        bookings.add(booking);
                        businessTasks.add(db.collection("businesses").document(booking.getBusinessId()).get());
                    }

                    if (bookings.isEmpty()) {
                        adapter.updateData(new ArrayList<>());
                        Toast.makeText(this, "לא נמצאו הזמנות עתידיות.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Tasks.whenAllSuccess(businessTasks).addOnSuccessListener(objects -> {
                        List<BookingWithBusiness> combinedList = new ArrayList<>();
                        for (int i = 0; i < bookings.size(); i++) {
                            DocumentSnapshot businessDoc = (DocumentSnapshot) objects.get(i);
                            String businessName = businessDoc.exists() ? businessDoc.getString("businessName") : "עסק לא ידוע";
                            combinedList.add(new BookingWithBusiness(bookings.get(i), businessName));
                        }
                        adapter.updateData(combinedList);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading bookings", e);
                    Toast.makeText(this, "שגיאה בטעינת הזמנות: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}