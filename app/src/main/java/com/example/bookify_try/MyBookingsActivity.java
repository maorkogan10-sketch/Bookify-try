package com.example.bookify_try;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
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

        loadMyBookings();
    }

    private void loadMyBookings() {
        if (mAuth.getCurrentUser() == null) return;

        String customerId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Attempting to load bookings for customer: " + customerId);

        db.collection("bookings")
                .whereEqualTo("customerId", customerId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Successfully retrieved " + queryDocumentSnapshots.size() + " booking documents.");
                    List<Booking> bookings = new ArrayList<>();
                    List<Task<DocumentSnapshot>> businessTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Booking booking = doc.toObject(Booking.class);
                        bookings.add(booking);
                        businessTasks.add(db.collection("businesses").document(booking.getBusinessId()).get());
                    }

                    if (bookings.isEmpty()) {
                        Log.d(TAG, "No bookings found for this customer.");
                        Toast.makeText(this, "לא נמצאו הזמנות.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Tasks.whenAllSuccess(businessTasks).addOnSuccessListener(objects -> {
                        List<BookingWithBusiness> combinedList = new ArrayList<>();
                        for (int i = 0; i < bookings.size(); i++) {
                            DocumentSnapshot businessDoc = (DocumentSnapshot) objects.get(i);
                            String businessName = businessDoc.exists() ? businessDoc.getString("businessName") : "עסק לא ידוע";
                            combinedList.add(new BookingWithBusiness(bookings.get(i), businessName));
                        }
                        Log.d(TAG, "Successfully combined bookings with business names. Updating adapter.");
                        adapter.updateData(combinedList);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching business details", e);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "******************* LOAD BOOKINGS FAILED *******************");
                    Log.e(TAG, "Reason: ", e);
                    Toast.makeText(this, "שגיאה בטעינת הזמנות: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}