package com.example.bookify_try;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

    private static final String TAG = "CreateBookingActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 123;

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

        // 1. יצירת ערוץ התראות
        NotificationHelper.createNotificationChannel(this);
        
        // 2. בקשת הרשאה מהמשתמש (עבור אנדרואיד 13 ומעלה)
        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "הרשאת התראות אושרה!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "לא תקבל תזכורות ללא אישור התראות.", Toast.LENGTH_LONG).show();
            }
        }
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
            Calendar selectedTime = Calendar.getInstance();
            selectedTime.set(year, month, day, hourOfDay, minute, 0);
            if (isStartTime) {
                startTime = selectedTime;
                startTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            } else {
                endTime = selectedTime;
                endTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }
        }, 9, 0, true);
        timePicker.show();
    }

    private void createBooking() {
        if (!isInputValid()) return;

        Timestamp startTimestamp = new Timestamp(startTime.getTime());
        Timestamp endTimestamp = new Timestamp(endTime.getTime());
        String selectedResourceName = (String) resourceSpinner.getSelectedItem();

        Resource selectedResource = getSelectedResource(selectedResourceName);
        if (selectedResource == null) {
            Toast.makeText(this, "Error: Resource details not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isBookingWithinWorkingHours(startTime, endTime)) {
            Toast.makeText(this, "ההזמנה מחוץ לשעות הפעילות של העסק", Toast.LENGTH_LONG).show();
            return;
        }

        checkCollisionsAndSave(startTimestamp, endTimestamp, selectedResourceName, selectedResource.getQuantity());
    }

    private void checkCollisionsAndSave(Timestamp start, Timestamp end, String resourceName, int resourceQuantity) {
        db.collection("bookings")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("resourceName", resourceName)
                .whereLessThan("startTime", end)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int conflictingBookingsCount = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking existing = document.toObject(Booking.class);
                        if (existing.getEndTime().compareTo(start) > 0) {
                            conflictingBookingsCount++;
                        }
                    }

                    if (conflictingBookingsCount >= resourceQuantity) {
                        Toast.makeText(this, "המשאב תפוס לחלוטין בשעות אלו", Toast.LENGTH_LONG).show();
                    } else {
                        saveBooking(start, end, resourceName);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Booking check failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void saveBooking(Timestamp start, Timestamp end, String resource) {
        String customerId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        String bookingId = db.collection("bookings").document().getId();
        Booking booking = new Booking(bookingId, businessId, customerId, resource, start, end);

        db.collection("bookings").document(bookingId).set(booking)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "ההזמנה בוצעה בהצלחה!", Toast.LENGTH_LONG).show();
                    
                    // תזמון התזכורת
                    scheduleReminder(booking);
                    
                    Intent intent = new Intent(this, SearchBusinessActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void scheduleReminder(Booking booking) {
        // לצורך הבדיקה שלך: התראה בעוד 10 שניות מהרגע
        long reminderTimeMillis = System.currentTimeMillis() + 10000;

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", "תזכורת להזמנה");
        intent.putExtra("message", "יש לך הזמנה ל-" + booking.getResourceName() + " בעוד זמן קצר.");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 
                booking.getBookingId().hashCode(), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
            } catch (SecurityException e) {
                Log.e(TAG, "Exact alarm permission denied", e);
                // נפילה חזרה להתראה לא מדויקת אם אין הרשאת שעון מדויק
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
            }
        }
    }

    private boolean isBookingWithinWorkingHours(Calendar bookingStart, Calendar bookingEnd) {
        if (business == null || business.getWorkingHours() == null) return false;
        int dayOfWeek = bookingStart.get(Calendar.DAY_OF_WEEK);
        String dayName = getDayName(dayOfWeek);
        for (WorkingHours wh : business.getWorkingHours()) {
            if (dayName.equals(wh.getDayOfWeek())) {
                for (TimeSlot ts : wh.getTimeSlots()) {
                    Calendar slotStart = Calendar.getInstance();
                    slotStart.set(year, month, day, ts.getStartHour(), ts.getStartMinute());
                    Calendar slotEnd = Calendar.getInstance();
                    slotEnd.set(year, month, day, ts.getEndHour(), ts.getEndMinute());
                    if (!bookingStart.before(slotStart) && !bookingEnd.after(slotEnd)) {
                        return true; 
                    }
                }
            }
        }
        return false;
    }
    
    private Resource getSelectedResource(String resourceName) {
        if (business != null && business.getResources() != null) {
            for (Resource res : business.getResources()) {
                if (res.getName().equals(resourceName)) return res;
            }
        }
        return null;
    }

    private boolean isInputValid() {
        if (mAuth.getCurrentUser() == null) return false;
        if (startTime == null || endTime == null) return false;
        if (!endTime.after(startTime)) return false;
        if (resourceSpinner.getSelectedItem() == null) return false;
        return true;
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