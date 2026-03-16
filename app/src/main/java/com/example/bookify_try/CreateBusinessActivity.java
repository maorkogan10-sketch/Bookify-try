package com.example.bookify_try;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CreateBusinessActivity extends AppCompatActivity {

    private static final String TAG = "CreateBusinessActivity";

    public static final String EXTRA_BUSINESS_ID = "EXTRA_BUSINESS_ID";
    public static final String EXTRA_YEAR = "EXTRA_YEAR";
    public static final String EXTRA_MONTH = "EXTRA_MONTH";
    public static final String EXTRA_DAY = "EXTRA_DAY";

    private TextInputEditText businessNameEditText, businessAddressEditText, businessDescriptionEditText;
    private LinearLayout resourcesContainer, workingHoursContainer;
    private TextView createBusinessTitle;
    private Button saveBusinessButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final List<Resource> resourceList = new ArrayList<>();
    private final List<WorkingHours> workingHoursList = new ArrayList<>();
    private final Map<String, TextView> dayHoursTextViews = new LinkedHashMap<>();

    private Spinner resourceSpinner;
    private Button startTimeButton, endTimeButton, confirmBookingButton;
    private int year, month, day;
    private Calendar startTime, endTime;
    private Business business;
    private String businessId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (getIntent().hasExtra(EXTRA_YEAR)) {
            setupForBooking();
        } else {
            setupForBusinessManagement();
        }
    }

    private void setupForBusinessManagement() {
        setContentView(R.layout.activity_create_business);

        createBusinessTitle = findViewById(R.id.createBusinessTitle);
        businessNameEditText = findViewById(R.id.businessNameEditText);
        businessAddressEditText = findViewById(R.id.businessAddressEditText);
        businessDescriptionEditText = findViewById(R.id.businessDescriptionEditText);
        resourcesContainer = findViewById(R.id.resourcesContainer);
        workingHoursContainer = findViewById(R.id.workingHoursContainer);
        saveBusinessButton = findViewById(R.id.saveBusinessButton);

        findViewById(R.id.addResourceButton).setOnClickListener(v -> showAddResourceDialog());
        saveBusinessButton.setOnClickListener(v -> saveBusiness());

        setupWorkingHoursViews();
        loadExistingBusinessData();
    }

    private void loadExistingBusinessData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("businesses").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Business existingBusiness = documentSnapshot.toObject(Business.class);
                        if (existingBusiness != null) {
                            createBusinessTitle.setText("עריכת עסק");
                            saveBusinessButton.setText("עדכן עסק");
                            businessNameEditText.setText(existingBusiness.getBusinessName());
                            businessAddressEditText.setText(existingBusiness.getAddress());
                            businessDescriptionEditText.setText(existingBusiness.getDescription());
                            
                            if (existingBusiness.getResources() != null) {
                                resourcesContainer.removeAllViews();
                                resourceList.clear();
                                for (Resource res : existingBusiness.getResources()) {
                                    addResourceToList(res);
                                }
                            }
                            if (existingBusiness.getWorkingHours() != null) {
                                for (WorkingHours wh : existingBusiness.getWorkingHours()) {
                                    for (WorkingHours localWh : workingHoursList) {
                                        if (localWh.getDayOfWeek().equals(wh.getDayOfWeek())) {
                                            localWh.setTimeSlots(wh.getTimeSlots());
                                            updateDayHoursTextView(localWh);
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private void saveBusiness() {
        String businessName = businessNameEditText.getText().toString().trim();
        String address = businessAddressEditText.getText().toString().trim();
        String description = businessDescriptionEditText.getText().toString().trim();

        if (TextUtils.isEmpty(businessName)) {
            businessNameEditText.setError("יש למלא את שם העסק.");
            return;
        }
        if (resourceList.isEmpty()) {
            Toast.makeText(this, "יש להוסיף לפחות משאב אחד.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        Business newBusiness = new Business(currentUser.getUid(), businessName, address, description, resourceList, workingHoursList);

        db.collection("businesses").document(currentUser.getUid())
                .set(newBusiness)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CreateBusinessActivity.this, "העסק נשמר בהצלחה!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(CreateBusinessActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void setupForBooking() {
        setContentView(R.layout.activity_create_booking);
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

        selectedDateTextView.setText(String.format(Locale.getDefault(), "תאריך: %d/%d/%d", day, month + 1, year));
        loadBusinessDataForBooking();

        startTimeButton.setOnClickListener(v -> showTimePicker(true));
        endTimeButton.setOnClickListener(v -> showTimePicker(false));
        confirmBookingButton.setOnClickListener(v -> createBooking());

        NotificationHelper.createNotificationChannel(this);
    }

    private void loadBusinessDataForBooking() {
        db.collection("businesses").document(businessId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        business = documentSnapshot.toObject(Business.class);
                        if (business != null && business.getResources() != null) {
                            List<String> resourceNames = business.getResources().stream().map(Resource::getName).collect(Collectors.toList());
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
        if (startTime.before(Calendar.getInstance())) {
            Toast.makeText(this, "לא ניתן להזמין זמן שכבר עבר.", Toast.LENGTH_LONG).show();
            return;
        }
        Timestamp startTimestamp = new Timestamp(startTime.getTime());
        Timestamp endTimestamp = new Timestamp(endTime.getTime());
        String selectedResourceName = (String) resourceSpinner.getSelectedItem();
        Resource selectedResource = getSelectedResource(selectedResourceName);
        if (selectedResource == null) return;

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
                        if (existing.getEndTime().compareTo(start) > 0) conflictingBookingsCount++;
                    }
                    if (conflictingBookingsCount >= resourceQuantity) {
                        Toast.makeText(this, "המשאב תפוס לחלוטין בשעות אלו", Toast.LENGTH_LONG).show();
                    } else {
                        saveBookingFinal(start, end, resourceName);
                    }
                });
    }

    private void saveBookingFinal(Timestamp start, Timestamp end, String resource) {
        String customerId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        String bookingId = db.collection("bookings").document().getId();
        Booking booking = new Booking(bookingId, businessId, customerId, resource, start, end);
        db.collection("bookings").document(bookingId).set(booking)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "ההזמנה בוצעה בהצלחה!", Toast.LENGTH_LONG).show();
                    scheduleReminder(booking);
                    finish();
                });
    }

    private void scheduleReminder(Booking booking) {
        long reminderTimeMillis = System.currentTimeMillis() + 10000;
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, booking.getBookingId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
    }

    private boolean isInputValid() {
        if (mAuth.getCurrentUser() == null) return false;
        if (startTime == null || endTime == null) {
            Toast.makeText(this, "יש לבחור שעת התחלה וסיום", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!endTime.after(startTime)) {
            Toast.makeText(this, "שעת הסיום חייבת להיות אחרי שעת ההתחלה", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (resourceSpinner != null && resourceSpinner.getSelectedItem() == null) {
            Toast.makeText(this, "יש לבחור משאב", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private Resource getSelectedResource(String resourceName) {
        if (business != null && business.getResources() != null) {
            for (Resource res : business.getResources()) {
                if (res.getName().equals(resourceName)) return res;
            }
        }
        return null;
    }

    private boolean isBookingWithinWorkingHours(Calendar bookingStart, Calendar bookingEnd) {
        if (business == null || business.getWorkingHours() == null) return false;
        int dayOfWeek = bookingStart.get(Calendar.DAY_OF_WEEK);
        String dayName = getDayName(dayOfWeek);
        for (WorkingHours wh : business.getWorkingHours()) {
            if (dayName.equals(wh.getDayOfWeek())) {
                for (TimeSlot ts : wh.getTimeSlots()) {
                    Calendar slotStart = (Calendar) bookingStart.clone();
                    slotStart.set(Calendar.HOUR_OF_DAY, ts.getStartHour());
                    slotStart.set(Calendar.MINUTE, ts.getStartMinute());
                    Calendar slotEnd = (Calendar) bookingEnd.clone();
                    slotEnd.set(Calendar.HOUR_OF_DAY, ts.getEndHour());
                    slotEnd.set(Calendar.MINUTE, ts.getEndMinute());
                    if (!bookingStart.before(slotStart) && !bookingEnd.after(slotEnd)) return true; 
                }
            }
        }
        return false;
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

    private void setupWorkingHoursViews() {
        String[] days = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String day : days) {
            WorkingHours wh = new WorkingHours(day);
            workingHoursList.add(wh);
            View dayView = inflater.inflate(R.layout.day_working_hours_item, workingHoursContainer, false);
            ((TextView) dayView.findViewById(R.id.dayNameTextView)).setText("יום " + day);
            TextView hoursTV = dayView.findViewById(R.id.hoursTextView);
            hoursTV.setText("לא הוגדר");
            dayHoursTextViews.put(day, hoursTV);
            dayView.findViewById(R.id.editHoursButton).setOnClickListener(v -> showEditHoursDialog(wh));
            workingHoursContainer.addView(dayView);
        }
    }

    private void updateDayHoursTextView(WorkingHours workingHours) {
        TextView hoursTextView = dayHoursTextViews.get(workingHours.getDayOfWeek());
        if (hoursTextView != null) {
            if (workingHours.getTimeSlots().isEmpty()) hoursTextView.setText("לא הוגדר");
            else {
                Collections.sort(workingHours.getTimeSlots(), Comparator.comparingInt(TimeSlot::getStartHour));
                hoursTextView.setText(workingHours.getTimeSlots().stream().map(TimeSlot::toString).collect(Collectors.joining(", ")));
            }
        }
    }

    private void showAddResourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_resource, null);
        final TextInputEditText resNameET = dialogView.findViewById(R.id.resourceNameEditText);
        final TextInputEditText resCapET = dialogView.findViewById(R.id.resourceCapacityEditText);
        final TextInputEditText resQtyET = dialogView.findViewById(R.id.resourceQuantityEditText);
        builder.setView(dialogView).setPositiveButton("הוסף", (dialog, id) -> {
            String name = resNameET.getText().toString().trim();
            if (!name.isEmpty() && !resCapET.getText().toString().isEmpty() && !resQtyET.getText().toString().isEmpty()) {
                addResourceToList(new Resource(name, Integer.parseInt(resCapET.getText().toString()), Integer.parseInt(resQtyET.getText().toString())));
            }
        }).setNegativeButton("ביטול", null).show();
    }

    private void addResourceToList(Resource resource) {
        resourceList.add(resource);
        View view = getLayoutInflater().inflate(R.layout.resource_item_view, resourcesContainer, false);
        ((TextView) view.findViewById(R.id.resourceDetailsTextView)).setText(String.format(Locale.getDefault(), "%s (קיבולת: %d, כמות: %d)", resource.getName(), resource.getCapacity(), resource.getQuantity()));
        view.findViewById(R.id.removeResourceButton).setOnClickListener(v -> { resourceList.remove(resource); resourcesContainer.removeView(view); });
        resourcesContainer.addView(view);
    }

    private void showEditHoursDialog(@NonNull final WorkingHours workingHours) {
        final List<TimeSlot> dialogTimeSlots = new ArrayList<>();
        workingHours.getTimeSlots().forEach(ts -> dialogTimeSlots.add(new TimeSlot(ts.getStartHour(), ts.getStartMinute(), ts.getEndHour(), ts.getEndMinute())));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_working_hours, null);
        ((TextView) dialogView.findViewById(R.id.dialogTitle)).setText("עריכת שעות עבור יום " + workingHours.getDayOfWeek());
        LinearLayout timeSlotsContainer = dialogView.findViewById(R.id.timeSlotsContainer);

        for (TimeSlot ts : dialogTimeSlots) {
            addTimeSlotViewToDialog(getLayoutInflater(), timeSlotsContainer, dialogTimeSlots, ts);
        }

        dialogView.findViewById(R.id.addTimeSlotButton).setOnClickListener(v -> {
            TimePickerDialog startTimePicker = new TimePickerDialog(this, (view, startHour, startMinute) -> {
                TimePickerDialog endTimePicker = new TimePickerDialog(this, (view2, endHour, endMinute) -> {
                    if (endHour < startHour || (endHour == startHour && endMinute <= startMinute)) {
                        Toast.makeText(this, "שעת הסיום חייבת להיות אחרי שעת ההתחלה", Toast.LENGTH_LONG).show();
                        return;
                    }
                    TimeSlot newTimeSlot = new TimeSlot(startHour, startMinute, endHour, endMinute);
                    dialogTimeSlots.add(newTimeSlot);
                    addTimeSlotViewToDialog(getLayoutInflater(), timeSlotsContainer, dialogTimeSlots, newTimeSlot);
                }, 9, 0, true);
                endTimePicker.show();
            }, 9, 0, true);
            startTimePicker.show();
        });

        builder.setView(dialogView)
                .setPositiveButton("שמור", (dialog, id) -> {
                    workingHours.setTimeSlots(dialogTimeSlots);
                    updateDayHoursTextView(workingHours);
                })
                .setNegativeButton("ביטול", null);
        builder.create().show();
    }

    private void addTimeSlotViewToDialog(LayoutInflater inflater, LinearLayout container, List<TimeSlot> list, TimeSlot timeSlot) {
        View timeSlotView = inflater.inflate(R.layout.time_slot_item, container, false);
        ((TextView) timeSlotView.findViewById(R.id.timeSlotTextView)).setText(timeSlot.toString());
        timeSlotView.findViewById(R.id.removeTimeSlotButton).setOnClickListener(v -> {
            list.remove(timeSlot);
            container.removeView(timeSlotView);
        });
        container.addView(timeSlotView);
    }
}