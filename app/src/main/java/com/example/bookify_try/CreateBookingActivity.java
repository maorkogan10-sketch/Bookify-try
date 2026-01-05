package com.example.bookify_try;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class CreateBookingActivity extends AppCompatActivity {

    public static final String EXTRA_BUSINESS_ID = "EXTRA_BUSINESS_ID";
    public static final String EXTRA_YEAR = "EXTRA_YEAR";
    public static final String EXTRA_MONTH = "EXTRA_MONTH";
    public static final String EXTRA_DAY = "EXTRA_DAY";

    private Spinner resourceSpinner;
    private Button startTimeButton, endTimeButton, confirmBookingButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String businessId;
    private Business business;
    private int year, month, day;
    private Calendar startTime, endTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_booking);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        TextView selectedDateTextView = findViewById(R.id.selectedDateTextView);
        resourceSpinner = findViewById(R.id.resourceSpinner);
        startTimeButton = findViewById(R.id.startTimeButton);
        endTimeButton = findViewById(R.id.endTimeButton);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);

        Intent intent = getIntent();
        businessId = intent.getStringExtra(EXTRA_BUSINESS_ID);
        year = intent.getIntExtra(EXTRA_YEAR, -1);
        month = intent.getIntExtra(EXTRA_MONTH, -1);
        day = intent.getIntExtra(EXTRA_DAY, -1);

        if (businessId == null || year == -1) {
            Toast.makeText(this, "Error loading booking details", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        selectedDateTextView.setText(String.format(Locale.getDefault(), "תאריך: %d/%d/%d", day, month + 1, year));

        loadBusinessData();

        startTimeButton.setOnClickListener(v -> showTimePicker(true));
        endTimeButton.setOnClickListener(v -> showTimePicker(false));
        confirmBookingButton.setOnClickListener(v -> createBooking());
    }

    private void loadBusinessData() {
        db.collection("businesses").document(businessId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        business = documentSnapshot.toObject(Business.class);
                        if (business != null && business.getResources() != null) {
                            List<String> resourceNames = business.getResources().stream()
                                    .map(Resource::getName)
                                    .collect(Collectors.toList());

                            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, resourceNames);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            resourceSpinner.setAdapter(adapter);
                        }
                    }
                });
    }

    private void showTimePicker(boolean isStartTime) {
        TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            if (isStartTime) {
                startTime = Calendar.getInstance();
                startTime.set(year, month, day, hourOfDay, minute, 0);
                startTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            } else {
                endTime = Calendar.getInstance();
                endTime.set(year, month, day, hourOfDay, minute, 0);
                endTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }
        }, 9, 0, true);
        timePicker.show();
    }

    private void createBooking() {
        if (!isInputValid()) return;

        Timestamp startTimestamp = new Timestamp(startTime.getTime());
        Timestamp endTimestamp = new Timestamp(endTime.getTime());
        String selectedResource = (String) resourceSpinner.getSelectedItem();

        // 1. Check if booking is within working hours
        if (!isBookingWithinWorkingHours(startTimestamp)) {
            Toast.makeText(this, "Booking time is outside of business working hours.", Toast.LENGTH_LONG).show();
            return;
        }

        // 2. Check for conflicting bookings
        db.collection("bookings")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("resourceName", selectedResource)
                .whereGreaterThanOrEqualTo("endTime", startTimestamp) // Check for overlaps
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Booking existingBooking = doc.toObject(Booking.class);
                        if (existingBooking.getStartTime().toDate().before(endTimestamp.toDate())) {
                             Toast.makeText(this, "The selected time slot is already booked.", Toast.LENGTH_LONG).show();
                             return; // Conflict found
                        }
                    }

                    // No conflicts, proceed to save
                    saveBooking(startTimestamp, endTimestamp, selectedResource);
                });
    }
    
    private boolean isInputValid(){
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to book.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (startTime == null || endTime == null) {
            Toast.makeText(this, "Please select start and end times.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!endTime.after(startTime)) {
            Toast.makeText(this, "End time must be after start time.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (resourceSpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a resource.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean isBookingWithinWorkingHours(Timestamp bookingStartTime) {
        if (business == null || business.getWorkingHours() == null) return false;

        Calendar bookingCal = Calendar.getInstance();
        bookingCal.setTime(bookingStartTime.toDate());
        int dayOfWeek = bookingCal.get(Calendar.DAY_OF_WEEK); // Sunday = 1, Saturday = 7
        String dayName = getDayName(dayOfWeek);

        for (WorkingHours wh : business.getWorkingHours()) {
            if (Objects.equals(wh.getDayOfWeek(), dayName)) {
                for (TimeSlot ts : wh.getTimeSlots()) {
                    Calendar slotStart = (Calendar) startTime.clone();
                    slotStart.set(Calendar.HOUR_OF_DAY, ts.getStartHour());
                    slotStart.set(Calendar.MINUTE, ts.getStartMinute());

                    Calendar slotEnd = (Calendar) endTime.clone();
                    slotEnd.set(Calendar.HOUR_OF_DAY, ts.getEndHour());
                    slotEnd.set(Calendar.MINUTE, ts.getEndMinute());

                    if (!startTime.before(slotStart) && !endTime.after(slotEnd)) {
                        return true; // Booking is within this time slot
                    }
                }
            }
        }
        return false;
    }
    
    private void saveBooking(Timestamp startTimestamp, Timestamp endTimestamp, String resourceName) {
        String customerId = mAuth.getCurrentUser().getUid();
        String bookingId = db.collection("bookings").document().getId();
        Booking booking = new Booking(bookingId, businessId, customerId, resourceName, startTimestamp, endTimestamp);

        db.collection("bookings").document(bookingId).set(booking)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking successful!", Toast.LENGTH_LONG).show();
                    Intent homeIntent = new Intent(CreateBookingActivity.this, CustomerHomeActivity.class);
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY: return "ראשון";
            case Calendar.MONDAY: return "שני";
            case Calendar.TUESDAY: return "שלישי";
            case Calendar.WEDNESDAY: return "רביעי";
            case Calendar.THURSDAY: return "חמישי";
            case Calendar.FRIDAY: return "שישי";
            case Calendar.SATURDAY: return "שבת";
            default: return "";
        }
    }
}