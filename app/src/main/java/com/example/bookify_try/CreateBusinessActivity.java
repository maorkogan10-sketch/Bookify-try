package com.example.bookify_try;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

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
    private Button saveBusinessButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<Resource> resourceList = new ArrayList<>();
    private Map<String, WorkingHours> workingHoursMap = new LinkedHashMap<>();
    private Map<String, TextView> dayHoursTextViews = new LinkedHashMap<>();

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
        saveBusinessButton = findViewById(R.id.saveBusinessButton);

        saveBusinessButton.setOnClickListener(v -> saveBusiness());
        setupWorkingHoursViews();
    }

    private void setupWorkingHoursViews() {
        String[] days = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String day : days) {
            workingHoursMap.put(day, new WorkingHours(day));
            View dayView = inflater.inflate(R.layout.day_working_hours_item, workingHoursContainer, false);
            TextView dayNameTextView = dayView.findViewById(R.id.dayNameTextView);
            TextView hoursTextView = dayView.findViewById(R.id.hoursTextView);
            Button editHoursButton = dayView.findViewById(R.id.editHoursButton);

            dayNameTextView.setText("יום " + day);
            hoursTextView.setText("לא הוגדר");
            dayHoursTextViews.put(day, hoursTextView);

            editHoursButton.setOnClickListener(v -> showEditHoursDialog(day));
            workingHoursContainer.addView(dayView);
        }
    }

    private void showEditHoursDialog(final String day) {
        WorkingHours currentDayHours = workingHoursMap.get(day);
        // Create a deep copy to modify in the dialog
        final List<TimeSlot> dialogTimeSlots = new ArrayList<>();
        if (currentDayHours != null) {
            for(TimeSlot ts : currentDayHours.getTimeSlots()){
                dialogTimeSlots.add(new TimeSlot(ts.getStartHour(), ts.getStartMinute(), ts.getEndHour(), ts.getEndMinute()));
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_working_hours, null);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        LinearLayout timeSlotsContainer = dialogView.findViewById(R.id.timeSlotsContainer);
        Button addTimeSlotButton = dialogView.findViewById(R.id.addTimeSlotButton);

        dialogTitle.setText("עריכת שעות עבור יום " + day);

        // Populate dialog with existing time slots
        for (TimeSlot ts : dialogTimeSlots) {
            addTimeSlotViewToDialog(inflater, timeSlotsContainer, dialogTimeSlots, ts);
        }

        addTimeSlotButton.setOnClickListener(v -> {
            // Show Time Picker for start time
            TimePickerDialog startTimePicker = new TimePickerDialog(this, (view, startHour, startMinute) -> {
                // Show Time Picker for end time
                TimePickerDialog endTimePicker = new TimePickerDialog(this, (view2, endHour, endMinute) -> {
                    if(endHour < startHour || (endHour == startHour && endMinute <= startMinute)){
                        Toast.makeText(this, "שעת הסיום חייבת להיות אחרי שעת ההתחלה", Toast.LENGTH_LONG).show();
                        return;
                    }
                    TimeSlot newTimeSlot = new TimeSlot(startHour, startMinute, endHour, endMinute);
                    dialogTimeSlots.add(newTimeSlot);
                    addTimeSlotViewToDialog(inflater, timeSlotsContainer, dialogTimeSlots, newTimeSlot);

                }, 0, 0, true);
                endTimePicker.setTitle("בחר שעת סיום");
                endTimePicker.show();
            }, 0, 0, true);
            startTimePicker.setTitle("בחר שעת התחלה");
            startTimePicker.show();
        });

        builder.setView(dialogView)
                .setPositiveButton("שמור", (dialog, id) -> {
                    WorkingHours dayHours = workingHoursMap.get(day);
                    if (dayHours != null) {
                        dayHours.setTimeSlots(dialogTimeSlots);
                        updateDayHoursTextView(day);
                    }
                })
                .setNegativeButton("ביטול", (dialog, id) -> dialog.cancel());

        builder.create().show();
    }

    private void addTimeSlotViewToDialog(LayoutInflater inflater, LinearLayout container, List<TimeSlot> list, TimeSlot timeSlot) {
        View timeSlotView = inflater.inflate(R.layout.time_slot_item, container, false);
        TextView timeSlotTextView = timeSlotView.findViewById(R.id.timeSlotTextView);
        ImageButton removeButton = timeSlotView.findViewById(R.id.removeTimeSlotButton);

        timeSlotTextView.setText(timeSlot.toString());
        removeButton.setOnClickListener(v -> {
            list.remove(timeSlot);
            container.removeView(timeSlotView);
        });
        container.addView(timeSlotView);
    }

    private void updateDayHoursTextView(String day) {
        WorkingHours dayHours = workingHoursMap.get(day);
        TextView hoursTextView = dayHoursTextViews.get(day);
        if (dayHours != null && hoursTextView != null) {
            if (dayHours.getTimeSlots().isEmpty()) {
                hoursTextView.setText("לא הוגדר");
            } else {
                 Collections.sort(dayHours.getTimeSlots(), Comparator.comparingInt(TimeSlot::getStartHour));
                 String slotsText = dayHours.getTimeSlots().stream()
                         .map(TimeSlot::toString)
                         .collect(Collectors.joining(", "));
                hoursTextView.setText(slotsText);
            }
        }
    }

    private void saveBusiness() {
        String businessName = businessNameEditText.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (TextUtils.isEmpty(businessName)) {
            businessNameEditText.setError("יש למלא את שם העסק.");
            return;
        }
        if (resourceList.isEmpty()) {
            Toast.makeText(this, "יש להוסיף לפחות משאב אחד.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) {
            Toast.makeText(this, "שגיאה: לא נמצא משתמש מחובר.", Toast.LENGTH_SHORT).show();
            return;
        }

        String ownerId = currentUser.getUid();
        Business business = new Business(ownerId, businessName, resourceList, workingHoursMap);

        db.collection("businesses").document(ownerId)
                .set(business)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CreateBusinessActivity.this, "העסק נוצר בהצלחה!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateBusinessActivity.this, "שגיאה ביצירת העסק.", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showAddResourceDialog() {
        // Unchanged from before
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
                        Resource resource = new Resource(name, Integer.parseInt(capacityStr), Integer.parseInt(quantityStr));
                        addResourceToList(resource);
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
        TextView resourceDetailsTextView = resourceItemView.findViewById(R.id.resourceDetailsTextView);
        resourceDetailsTextView.setText(String.format(Locale.getDefault(), "%s (קיבולת: %d, כמות: %d)", resource.getName(), resource.getCapacity(), resource.getQuantity()));
        resourceItemView.findViewById(R.id.removeResourceButton).setOnClickListener(v -> {
            resourceList.remove(resource);
            resourcesContainer.removeView(resourceItemView);
        });
        resourcesContainer.addView(resourceItemView);
    }
}