package com.example.bookify_try;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateBusinessActivity extends AppCompatActivity {

    private static final String TAG = "CreateBusinessActivity";

    private TextInputEditText businessNameEditText;
    private LinearLayout resourcesContainer, workingHoursContainer;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final List<Resource> resourceList = new ArrayList<>();
    private final List<WorkingHours> workingHoursList = new ArrayList<>();
    private final Map<String, TextView> dayHoursTextViews = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_business);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        businessNameEditText = findViewById(R.id.businessNameEditText);
        findViewById(R.id.addResourceButton).setOnClickListener(v -> showAddResourceDialog());
        resourcesContainer = findViewById(R.id.resourcesContainer);
        workingHoursContainer = findViewById(R.id.workingHoursContainer);
        findViewById(R.id.saveBusinessButton).setOnClickListener(v -> saveBusiness());

        setupWorkingHoursViews();
    }

    //check
    private void setupWorkingHoursViews() {
        String[] days = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String day : days) {
            WorkingHours wh = new WorkingHours(day);
            workingHoursList.add(wh);

            View dayView = inflater.inflate(R.layout.day_working_hours_item, workingHoursContainer, false);
            TextView dayNameTextView = dayView.findViewById(R.id.dayNameTextView);
            TextView hoursTextView = dayView.findViewById(R.id.hoursTextView);
            dayNameTextView.setText("יום " + day);
            hoursTextView.setText("לא הוגדר");
            dayHoursTextViews.put(day, hoursTextView);

            dayView.findViewById(R.id.editHoursButton).setOnClickListener(v -> showEditHoursDialog(wh));
            workingHoursContainer.addView(dayView);
        }
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

    private void updateDayHoursTextView(WorkingHours workingHours) {
        TextView hoursTextView = dayHoursTextViews.get(workingHours.getDayOfWeek());
        if (hoursTextView != null) {
            if (workingHours.getTimeSlots().isEmpty()) {
                hoursTextView.setText("לא הוגדר");
            } else {
                Collections.sort(workingHours.getTimeSlots(), Comparator.comparingInt(TimeSlot::getStartHour));
                String slotsText = workingHours.getTimeSlots().stream().map(TimeSlot::toString).collect(Collectors.joining(", "));
                hoursTextView.setText(slotsText);
            }
        }
    }

    private void saveBusiness() {
        String businessName = businessNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(businessName)) {
            businessNameEditText.setError("יש למלא את שם העסק.");
            return;
        }
        if (resourceList.isEmpty()) {
            Toast.makeText(this, "יש להוסיף לפחות משאב אחד.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "שגיאה: לא נמצא משתמש מחובר.", Toast.LENGTH_SHORT).show();
            return;
        }

        String ownerId = currentUser.getUid();
        Business business = new Business(ownerId, businessName, resourceList, workingHoursList);

        db.collection("businesses").document(ownerId)
                .set(business)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Business successfully written!"); // Log on success
                    Toast.makeText(CreateBusinessActivity.this, "העסק נוצר בהצלחה!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error writing document", e);
                    Toast.makeText(CreateBusinessActivity.this, "שגיאה ביצירת העסק: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showAddResourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_resource, null);
        final TextInputEditText resourceNameEditText = dialogView.findViewById(R.id.resourceNameEditText);
        final TextInputEditText resourceCapacityEditText = dialogView.findViewById(R.id.resourceCapacityEditText);
        final TextInputEditText resourceQuantityEditText = dialogView.findViewById(R.id.resourceQuantityEditText);

        builder.setView(dialogView)
                .setPositiveButton("הוסף", (dialog, id) -> {
                    String name = resourceNameEditText.getText().toString().trim();
                    String capacityStr = resourceCapacityEditText.getText().toString().trim();
                    String quantityStr = resourceQuantityEditText.getText().toString().trim();

                    if (!name.isEmpty() && !capacityStr.isEmpty() && !quantityStr.isEmpty()) {
                        try {
                           int capacity = Integer.parseInt(capacityStr);
                           int quantity = Integer.parseInt(quantityStr);
                           Resource resource = new Resource(name, capacity, quantity);
                           addResourceToList(resource);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "קיבולת וכמות חייבים להיות מספרים.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "יש למלא את כל השדות.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("ביטול", null);
        builder.create().show();
    }

    private void addResourceToList(Resource resource) {
        resourceList.add(resource);
        View resourceItemView = getLayoutInflater().inflate(R.layout.resource_item_view, resourcesContainer, false);
        ((TextView) resourceItemView.findViewById(R.id.resourceDetailsTextView)).setText(String.format(Locale.getDefault(), "%s (קיבולת: %d, כמות: %d)", resource.getName(), resource.getCapacity(), resource.getQuantity()));
        resourceItemView.findViewById(R.id.removeResourceButton).setOnClickListener(v -> {
            resourceList.remove(resource);
            resourcesContainer.removeView(resourceItemView);
        });
        resourcesContainer.addView(resourceItemView);
    }
}