package com.example.bookify_try;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

public class BusinessDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_BUSINESS_ID = "com.example.bookify_try.EXTRA_BUSINESS_ID";

    private TextView businessNameTextView, businessAddressTextView, businessDescriptionTextView, workingHoursTextView, resourcesTextView;
    private CalendarView calendarView;

    private FirebaseFirestore db;
    private String businessId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_details);

        db = FirebaseFirestore.getInstance();

        businessNameTextView = findViewById(R.id.businessNameTextView);
        businessAddressTextView = findViewById(R.id.businessAddressTextView);
        businessDescriptionTextView = findViewById(R.id.businessDescriptionTextView);
        workingHoursTextView = findViewById(R.id.workingHoursTextView);
        resourcesTextView = findViewById(R.id.resourcesTextView);
        calendarView = findViewById(R.id.calendarView);

        // הגבלת לוח השנה לתאריך הנוכחי והלאה
        calendarView.setMinDate(System.currentTimeMillis() - 1000);

        businessId = getIntent().getStringExtra(EXTRA_BUSINESS_ID);

        if (businessId == null) {
            Toast.makeText(this, "Error: Business ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadBusinessDetails();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Intent intent = new Intent(BusinessDetailsActivity.this, CreateBookingActivity.class);
            intent.putExtra(CreateBookingActivity.EXTRA_BUSINESS_ID, businessId);
            intent.putExtra(CreateBookingActivity.EXTRA_YEAR, year);
            intent.putExtra(CreateBookingActivity.EXTRA_MONTH, month);
            intent.putExtra(CreateBookingActivity.EXTRA_DAY, dayOfMonth);
            startActivity(intent);
        });
    }

    private void loadBusinessDetails() {
        db.collection("businesses").document(businessId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Business business = documentSnapshot.toObject(Business.class);
                        if (business != null) {
                            displayBusinessDetails(business);
                        }
                    } else {
                        Toast.makeText(this, "Business not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load business details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void displayBusinessDetails(Business business) {
        businessNameTextView.setText(business.getBusinessName());
        
        if (business.getAddress() != null && !business.getAddress().isEmpty()) {
            businessAddressTextView.setText("כתובת: " + business.getAddress());
        } else {
            businessAddressTextView.setText("כתובת לא צוינה");
        }

        if (business.getDescription() != null && !business.getDescription().isEmpty()) {
            businessDescriptionTextView.setText(business.getDescription());
        } else {
            businessDescriptionTextView.setText("אין תיאור זמין לעסק זה.");
        }

        if (business.getResources() != null) {
            String resourcesString = business.getResources().stream()
                    .map(r -> "- " + r.getName() + " (קיבולת: " + r.getCapacity() + ")")
                    .collect(Collectors.joining("\n"));
            resourcesTextView.setText(resourcesString);
        }

        if (business.getWorkingHours() != null && !business.getWorkingHours().isEmpty()) {
            Map<String, WorkingHours> workingHoursMap = business.getWorkingHours().stream()
                    .collect(Collectors.toMap(WorkingHours::getDayOfWeek, wh -> wh));

            String[] daysOrder = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};
            StringBuilder hoursBuilder = new StringBuilder();

            for (String day : daysOrder) {
                WorkingHours wh = workingHoursMap.get(day);
                if (wh != null && wh.getTimeSlots() != null && !wh.getTimeSlots().isEmpty()) {
                    wh.getTimeSlots().sort(Comparator.comparingInt(TimeSlot::getStartHour));
                    String slots = wh.getTimeSlots().stream()
                            .map(TimeSlot::toString)
                            .collect(Collectors.joining(", "));
                    hoursBuilder.append("יום ").append(day).append(": ").append(slots).append("\n");
                }
            }
            workingHoursTextView.setText(hoursBuilder.toString().trim());
        } else {
            workingHoursTextView.setText("שעות פעילות לא הוגדרו.");
        }
    }
}