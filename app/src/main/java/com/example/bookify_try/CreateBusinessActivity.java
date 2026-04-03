package com.example.bookify_try;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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

    private void saveBusiness() {
        String name = businessNameEditText.getText().toString().trim();
        String addr = businessAddressEditText.getText().toString().trim();
        String desc = businessDescriptionEditText.getText().toString().trim();
        if (TextUtils.isEmpty(name)) { businessNameEditText.setError("חובה"); return; }
        if (resourceList.isEmpty()) { Toast.makeText(this, "הוסף משאב", Toast.LENGTH_SHORT).show(); return; }
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        Business newBus = new Business(user.getUid(), name, addr, desc, resourceList, workingHoursList);
        db.collection("businesses").document(user.getUid()).set(newBus)
                .addOnSuccessListener(aVoid -> { Toast.makeText(this, "נשמר!", Toast.LENGTH_SHORT).show(); finish(); })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה", Toast.LENGTH_SHORT).show());
    }

    private void loadExistingBusinessData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.collection("businesses").document(user.getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Business b = doc.toObject(Business.class);
                if (b != null) {
                    createBusinessTitle.setText("עריכת עסק");
                    saveBusinessButton.setText("עדכן");
                    businessNameEditText.setText(b.getBusinessName());
                    businessAddressEditText.setText(b.getAddress());
                    businessDescriptionEditText.setText(b.getDescription());
                    if (b.getResources() != null) {
                        resourcesContainer.removeAllViews();
                        resourceList.clear();
                        for (Resource r : b.getResources()) addResourceToList(r);
                    }
                    if (b.getWorkingHours() != null) {
                        for (WorkingHours wh : b.getWorkingHours()) {
                            for (WorkingHours lWh : workingHoursList) {
                                if (lWh.getDayOfWeek().equals(wh.getDayOfWeek())) {
                                    lWh.setTimeSlots(wh.getTimeSlots());
                                    updateDayHoursTextView(lWh);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private void setupForBooking() {
        setContentView(R.layout.activity_create_booking);
        TextView dateTV = findViewById(R.id.selectedDateTextView);
        resourceSpinner = findViewById(R.id.resourceSpinner);
        startTimeButton = findViewById(R.id.startTimeButton);
        endTimeButton = findViewById(R.id.endTimeButton);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);
        Intent intent = getIntent();
        businessId = intent.getStringExtra(EXTRA_BUSINESS_ID);
        year = intent.getIntExtra(EXTRA_YEAR, -1);
        month = intent.getIntExtra(EXTRA_MONTH, -1);
        day = intent.getIntExtra(EXTRA_DAY, -1);
        dateTV.setText(String.format(Locale.getDefault(), "תאריך: %d/%d/%d", day, month + 1, year));
        loadBusinessDataForBooking();
        startTimeButton.setOnClickListener(v -> showTimePicker(true));
        endTimeButton.setOnClickListener(v -> showTimePicker(false));
        confirmBookingButton.setOnClickListener(v -> createBooking());
        NotificationHelper.createNotificationChannel(this);
    }

    private void loadBusinessDataForBooking() {
        db.collection("businesses").document(businessId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                business = doc.toObject(Business.class);
                if (business != null && business.getResources() != null) {
                    List<String> names = business.getResources().stream().map(Resource::getName).collect(Collectors.toList());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    resourceSpinner.setAdapter(adapter);
                }
            }
        });
    }

    private void showTimePicker(boolean isStart) {
        new TimePickerDialog(this, (view, h, m) -> {
            Calendar cal = Calendar.getInstance(); cal.set(year, month, day, h, m, 0);
            if (isStart) { startTime = cal; startTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)); }
            else { endTime = cal; endTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)); }
        }, 9, 0, true).show();
    }

    private void createBooking() {
        if (!isInputValid()) return;
        if (startTime.before(Calendar.getInstance())) { Toast.makeText(this, "זמן עבר", Toast.LENGTH_SHORT).show(); return; }
        String resName = (String) resourceSpinner.getSelectedItem();
        Resource res = getSelectedResource(resName);
        if (res == null) return;
        if (!isBookingWithinWorkingHours(startTime, endTime)) { Toast.makeText(this, "מחוץ לשעות", Toast.LENGTH_SHORT).show(); return; }
        checkCollisionsAndSave(new Timestamp(startTime.getTime()), new Timestamp(endTime.getTime()), resName, res.getQuantity());
    }

    private void checkCollisionsAndSave(Timestamp s, Timestamp e, String name, int qty) {
        db.collection("bookings").whereEqualTo("businessId", businessId).whereEqualTo("resourceName", name).whereLessThan("startTime", e).get()
                .addOnSuccessListener(snaps -> {
                    int count = 0;
                    for (QueryDocumentSnapshot d : snaps) {
                        Booking ex = d.toObject(Booking.class);
                        if (ex.getEndTime().compareTo(s) > 0) count++;
                    }
                    if (count >= qty) Toast.makeText(this, "תפוס", Toast.LENGTH_SHORT).show();
                    else saveBookingFinal(s, e, name);
                });
    }

    private void saveBookingFinal(Timestamp s, Timestamp e, String res) {
        String bId = db.collection("bookings").document().getId();
        Booking b = new Booking(bId, businessId, Objects.requireNonNull(mAuth.getCurrentUser()).getUid(), res, s, e);
        db.collection("bookings").document(bId).set(b).addOnSuccessListener(v -> {
            Toast.makeText(this, "בוצע!", Toast.LENGTH_SHORT).show();
            scheduleReminder(b);
            finish();
        });
    }

    private void scheduleReminder(Booking b) {
        long time = System.currentTimeMillis() + 10000;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(this, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, b.getBookingId().hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.set(AlarmManager.RTC_WAKEUP, time, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
        }
    }

    private boolean isInputValid() {
        if (mAuth.getCurrentUser() == null || startTime == null || endTime == null || resourceSpinner.getSelectedItem() == null) return false;
        if (!endTime.after(startTime)) { Toast.makeText(this, "סיום לפני התחלה", Toast.LENGTH_SHORT).show(); return false; }
        return true;
    }

    private Resource getSelectedResource(String n) {
        if (business != null && business.getResources() != null) {
            for (Resource r : business.getResources()) if (r.getName().equals(n)) return r;
        }
        return null;
    }

    private boolean isBookingWithinWorkingHours(Calendar s, Calendar e) {
        if (business == null || business.getWorkingHours() == null) return false;
        String dayN = getDayName(s.get(Calendar.DAY_OF_WEEK));
        for (WorkingHours wh : business.getWorkingHours()) {
            if (dayN.equals(wh.getDayOfWeek())) {
                for (TimeSlot ts : wh.getTimeSlots()) {
                    Calendar start = (Calendar) s.clone(); start.set(Calendar.HOUR_OF_DAY, ts.getStartHour()); start.set(Calendar.MINUTE, ts.getStartMinute());
                    Calendar end = (Calendar) e.clone(); end.set(Calendar.HOUR_OF_DAY, ts.getEndHour()); end.set(Calendar.MINUTE, ts.getEndMinute());
                    if (!s.before(start) && !e.after(end)) return true;
                }
            }
        }
        return false;
    }

    private String getDayName(int d) {
        switch (d) {
            case Calendar.SUNDAY: return "ראשון"; case Calendar.MONDAY: return "שני"; case Calendar.TUESDAY: return "שלישי";
            case Calendar.WEDNESDAY: return "רביעי"; case Calendar.THURSDAY: return "חמישי"; case Calendar.FRIDAY: return "שישי";
            case Calendar.SATURDAY: return "שבת"; default: return "";
        }
    }

    private void setupWorkingHoursViews() {
        String[] days = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};
        LayoutInflater inf = LayoutInflater.from(this);
        for (String day : days) {
            WorkingHours wh = new WorkingHours(day); workingHoursList.add(wh);
            View v = inf.inflate(R.layout.day_working_hours_item, workingHoursContainer, false);
            ((TextView) v.findViewById(R.id.dayNameTextView)).setText("יום " + day);
            TextView tv = v.findViewById(R.id.hoursTextView); tv.setText("לא הוגדר");
            dayHoursTextViews.put(day, tv);
            v.findViewById(R.id.editHoursButton).setOnClickListener(view -> showEditHoursDialog(wh));
            workingHoursContainer.addView(v);
        }
    }

    private void updateDayHoursTextView(WorkingHours wh) {
        TextView tv = dayHoursTextViews.get(wh.getDayOfWeek());
        if (tv != null) {
            if (wh.getTimeSlots().isEmpty()) tv.setText("לא הוגדר");
            else {
                wh.getTimeSlots().sort(Comparator.comparingInt(TimeSlot::getStartHour));
                tv.setText(wh.getTimeSlots().stream().map(TimeSlot::toString).collect(Collectors.joining(", ")));
            }
        }
    }

    private void showAddResourceDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_add_resource, null);
        final TextInputEditText nET = v.findViewById(R.id.resourceNameEditText), cET = v.findViewById(R.id.resourceCapacityEditText), qET = v.findViewById(R.id.resourceQuantityEditText);
        b.setView(v).setPositiveButton("הוסף", (d, id) -> {
            String name = nET.getText().toString();
            if (!name.isEmpty()) addResourceToList(new Resource(name, Integer.parseInt(cET.getText().toString()), Integer.parseInt(qET.getText().toString())));
        }).setNegativeButton("בטל", null).show();
    }

    private void addResourceToList(Resource r) {
        resourceList.add(r);
        View v = getLayoutInflater().inflate(R.layout.resource_item_view, resourcesContainer, false);
        ((TextView) v.findViewById(R.id.resourceDetailsTextView)).setText(r.getName());
        v.findViewById(R.id.removeResourceButton).setOnClickListener(view -> { resourceList.remove(r); resourcesContainer.removeView(v); });
        resourcesContainer.addView(v);
    }

    private void showEditHoursDialog(@NonNull final WorkingHours wh) {
        final List<TimeSlot> slots = new ArrayList<>();
        wh.getTimeSlots().forEach(ts -> slots.add(new TimeSlot(ts.getStartHour(), ts.getStartMinute(), ts.getEndHour(), ts.getEndMinute())));
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_edit_working_hours, null);
        LinearLayout container = v.findViewById(R.id.timeSlotsContainer);
        for (TimeSlot ts : slots) addTimeSlotViewToDialog(getLayoutInflater(), container, slots, ts);
        v.findViewById(R.id.addTimeSlotButton).setOnClickListener(view -> {
            new TimePickerDialog(this, (v1, h1, m1) -> {
                new TimePickerDialog(this, (v2, h2, m2) -> {
                    TimeSlot ts = new TimeSlot(h1, m1, h2, m2); slots.add(ts); addTimeSlotViewToDialog(getLayoutInflater(), container, slots, ts);
                }, 9, 0, true).show();
            }, 9, 0, true).show();
        });
        b.setView(v).setPositiveButton("שמור", (d, id) -> { wh.setTimeSlots(slots); updateDayHoursTextView(wh); }).setNegativeButton("בטל", null).show();
    }

    private void addTimeSlotViewToDialog(LayoutInflater inf, LinearLayout con, List<TimeSlot> list, TimeSlot ts) {
        View v = inf.inflate(R.layout.time_slot_item, con, false);
        ((TextView) v.findViewById(R.id.timeSlotTextView)).setText(ts.toString());
        v.findViewById(R.id.removeTimeSlotButton).setOnClickListener(view -> { list.remove(ts); con.removeView(v); });
        con.addView(v);
    }
}