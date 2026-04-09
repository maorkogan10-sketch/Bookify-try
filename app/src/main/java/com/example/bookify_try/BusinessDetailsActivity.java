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

    //הid של העסק מהמסך הקודם
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

        // הגבלת לוח השנה לתאריך הנוכחי והלאה. לא יוכל לבחור תאריך מהעבר
        calendarView.setMinDate(System.currentTimeMillis() - 1000);

        //השמת הID של העסק במשתנה
        businessId = getIntent().getStringExtra(EXTRA_BUSINESS_ID);

        //בדיקה שהוא לא ריק
        if (businessId == null) {
            Toast.makeText(this, "Error: Business ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadBusinessDetails();

        //מאזין ללוח שנה, קורה כאשר הלקוח לוחץ על תאריך
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            //עובר למסך הבא
            Intent intent = new Intent(BusinessDetailsActivity.this, CreateBookingActivity.class);
            //שולח כאקסטרה את הID של העסק, השנה החודש והיום של התאריך שנבחר
            intent.putExtra(CreateBookingActivity.EXTRA_BUSINESS_ID, businessId);
            intent.putExtra(CreateBookingActivity.EXTRA_YEAR, year);
            intent.putExtra(CreateBookingActivity.EXTRA_MONTH, month);
            intent.putExtra(CreateBookingActivity.EXTRA_DAY, dayOfMonth);
            startActivity(intent);
        });
    }

    //הפונקציה לא מקבלת כלום ולא מחזירה כלום היא רק מעלה את המסמך של העסק מהפיירבייס ושולחת אותו כאובייקט לפונקציה הבאה
    private void loadBusinessDetails() {
        db.collection("businesses").document(businessId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        //המרת המסמך של העסק מהפיירבייס לאובייקט
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
        //משנה את הטקסט בXML לשם העסק לפי האובייקט שקיבל
        businessNameTextView.setText(business.getBusinessName());

        //משנה את כל הפרטים של העסק בXML של האובייקט שקיבל
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

        //הופך את רשימת המשאבים לטקסט אחד ארוך שקוראים לו resourcesString
        if (business.getResources() != null) {
            String resourcesString = business.getResources().stream()
                    .map(r -> "- " + r.getName() + " (קיבולת: " + r.getCapacity() + ")")
                    .collect(Collectors.joining("\n"));
            resourcesTextView.setText(resourcesString);
        }
/// ///
        //בודק אם השעות לא ריקות ויוצר סוג של מילון שמכיל את כל שעות הפעילות של העסק
        if (business.getWorkingHours() != null && !business.getWorkingHours().isEmpty()) {
            Map<String, WorkingHours> workingHoursMap = business.getWorkingHours().stream()
                    .collect(Collectors.toMap(WorkingHours::getDayOfWeek, wh -> wh));

            //מערך של כל הימים
            String[] daysOrder = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};
            StringBuilder hoursBuilder = new StringBuilder(); //טקסט ארוך שיכיל את הטקסט של כל שעות העבודה

            for (String day : daysOrder) {
                //לוקח מהמילון את שעות העבודה ביום ספציפי
                WorkingHours wh = workingHoursMap.get(day);

                //בודק שהשעות עבודה ביום הזה לא ריקות
                if (wh != null && wh.getTimeSlots() != null && !wh.getTimeSlots().isEmpty()) {
                    //ממין את הסלוטים ככה שיהיה בסדר עולה של שעות
                    wh.getTimeSlots().sort(Comparator.comparingInt(TimeSlot::getStartHour));
                    //שם את כל הסלוטים של יום עבודה ברצף עם פסיק ביניהם ומוסיף את המילה יום והיום ויורד שורה - ככה נראה כל יום ברשימת שעות הפעילות
                    String slots = wh.getTimeSlots().stream()
                            .map(TimeSlot::toString)
                            .collect(Collectors.joining(", "));
                    hoursBuilder.append("יום ").append(day).append(": ").append(slots).append("\n");
                }
            }
            //מוסיף את כל הטקסט הארוך כמקשה אחת
            workingHoursTextView.setText(hoursBuilder.toString().trim());
        } else {
            workingHoursTextView.setText("שעות פעילות לא הוגדרו.");
        }
    }
}