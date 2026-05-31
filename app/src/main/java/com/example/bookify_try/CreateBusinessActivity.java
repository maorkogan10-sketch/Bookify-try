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
    //לא אמורים לקבל את האקסטרות האלה הלקוח לא אמור להגיע למסך הזה השארתי את זה אופציונלית ליתר ביטחון
    public static final String EXTRA_YEAR = "EXTRA_YEAR";
    //public static final String EXTRA_MONTH = "EXTRA_MONTH";
    //public static final String EXTRA_DAY = "EXTRA_DAY";

    private TextInputEditText businessNameEditText, businessAddressEditText, businessDescriptionEditText;
    //מכולות שאליהן אפשר לשים קוד בזמן אמת - המשאבים ולוחות הזמנים
    private LinearLayout resourcesContainer, workingHoursContainer;
    private TextView createBusinessTitle;
    private Button saveBusinessButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

//רשימה של משאבים
    private final List<Resource> resourceList = new ArrayList<>();
    //רשימה של שעות עבודה
    private final List<WorkingHours> workingHoursList = new ArrayList<>();
    //שמירה על סדר הימים וקישור בין שם היום לבין מה שמציג את השם שלו בVALUE - XML
    private final Map<String, TextView> dayHoursTextViews = new LinkedHashMap<>();
//התפריט הנפתח שנותן אפשרויות בחירה ללקוח
    //private Spinner resourceSpinner;
    //private Button startTimeButton, endTimeButton, confirmBookingButton;
    //private int year, month, day;
    //private Calendar startTime, endTime;
    //private Business business;
    //private String businessId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //חיבור לפירבייס
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //האיף לא אמור לקרות השארתי רק ליתר ביטחון הלקוח לא אמור להגיע למסך הזה
        if (getIntent().hasExtra(EXTRA_YEAR)) {
            //setupForBooking();
        } else {
            //קורא לפונקציה של ניהול המסך
            setupForBusinessManagement();
        }
    }

    //הפונקציה לא מקבלת כלום ובעצם מחברת את המסך לגאבה ויוצרת אותו
    private void setupForBusinessManagement() {
        //חיבור לXML
        setContentView(R.layout.activity_create_business);
        createBusinessTitle = findViewById(R.id.createBusinessTitle);
        businessNameEditText = findViewById(R.id.businessNameEditText);
        businessAddressEditText = findViewById(R.id.businessAddressEditText);
        businessDescriptionEditText = findViewById(R.id.businessDescriptionEditText);
        resourcesContainer = findViewById(R.id.resourcesContainer);
        workingHoursContainer = findViewById(R.id.workingHoursContainer);
        saveBusinessButton = findViewById(R.id.saveBusinessButton);

        //לחיצה על כפתור הוספת משאב
        findViewById(R.id.addResourceButton).setOnClickListener(v -> showAddResourceDialog());
        //לחיצה על כפתור שמירת נתונים
        saveBusinessButton.setOnClickListener(v -> saveBusiness());

        //פונקציה שיוצרת את הרשימה של ימי השבוע
        setupWorkingHoursViews();
        // פונקציה שבודקת האם העסק כבר קיים וזהו מסך עריכה או שצריך ליצור את העסק מחדש - מנתבת את המסך לשתי האופציות שהוא יכול להיות
        loadExistingBusinessData();
    }

    //הפונקציה לא מקבלת כלום ושומרת את פרטי העסק שהוזנו
    private void saveBusiness() {
        //שומר את מה שהוזן באדיט טקסט בתוך משתנים
        String name = businessNameEditText.getText().toString().trim();
        String addr = businessAddressEditText.getText().toString().trim();
        String desc = businessDescriptionEditText.getText().toString().trim();
        //אם שם העסק ריק
        if (TextUtils.isEmpty(name)) { businessNameEditText.setError("חובה"); return; }
        //אם רשימת המשאבים ריקה
        if (resourceList.isEmpty()) { Toast.makeText(this, "הוסף משאב", Toast.LENGTH_SHORT).show(); return; }
        //המשתמש הנוכחי שמחובר בAUTH
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        //יוצר אובייקט של עסק חדש עם כל הפרטים שהוזנו
        Business newBus = new Business(user.getUid(), name, addr, desc, resourceList, workingHoursList);
        //הולך לאוסף העסקים עם הID של בעל העסק ומשנה את העסק לאובייקט שיצרנו. - בגלל שיש SET, זה לא משנה אם זה מסך יצירה או עריכה זה דורס הכל
        db.collection("businesses").document(user.getUid()).set(newBus)
                .addOnSuccessListener(aVoid -> { Toast.makeText(this, "נשמר!", Toast.LENGTH_SHORT).show(); finish(); })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה", Toast.LENGTH_SHORT).show());
    }

    //הפונקציה בודקת האם המסך הוא מסך עריכה ואם כן אז מעלה את הפרטים ומעדכנת אותם. יצירת העסק זה DEFAULT.
    private void loadExistingBusinessData() {
        //המשתמש הנוכחי של בעל העסק שמחובר
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        //הולך לאוסף העסקים עם הID של בעל העסק ובודק אם יש עסק קיים
        db.collection("businesses").document(user.getUid()).get().addOnSuccessListener(doc -> {
            //אם מצא מסמך
            if (doc.exists()) {
                //הופך את המסמך לאובייקט
                Business b = doc.toObject(Business.class);
                //אם האובייקט של העסק לא ריק
                if (b != null) {
                    //משנה את המסך למסך עריכת עסק ולא יצירת עסק
                    createBusinessTitle.setText("עריכת עסק");
                    //שם בתוך האדיט טקסטים את המידע שכבר קיים וכבר הוזן כשהעסק הוזן כדי שהבעל עסק ייראה את המצב הנוכחי
                    saveBusinessButton.setText("עדכן");
                    businessNameEditText.setText(b.getBusinessName());
                    businessAddressEditText.setText(b.getAddress());
                    businessDescriptionEditText.setText(b.getDescription());
                    //אם קיימים משאבים
                    if (b.getResources() != null) {
                        // מנקים מהלייאאוט את כל מה שיש בו כדי שלא יהיה גם משאבים וגם רשימה ריקה
                        resourcesContainer.removeAllViews();
                        resourceList.clear();
                        //עוברים בלולאה על כל משאב שכבר קיים ושמים אותו על המסך באמצעות הפונקציה addResourceToList
                        for (Resource r : b.getResources()) addResourceToList(r);
                    }
                    // אם קיימות שעות פעילות שמורות
                    if (b.getWorkingHours() != null) {
                        //עובר על הימים מהענן
                        for (WorkingHours wh : b.getWorkingHours()) {
                            //עובר על 7 הימים שיש על המסך
                            for (WorkingHours lWh : workingHoursList) {
                                //אם היום מהענן שווה לשם של היום על המסך
                                if (lWh.getDayOfWeek().equals(wh.getDayOfWeek())) {
                                    //היום מהענן מוכנס לתוך האובייקט שעל המסך
                                    lWh.setTimeSlots(wh.getTimeSlots());
                                    //קוראים לפונקציה שתעדכן פיזית את השעות על המסך
                                    updateDayHoursTextView(lWh);
                                }
                            }
                        }
                    }
                }
            }
        });
    }


//    private void setupForBooking() {
//        setContentView(R.layout.activity_create_booking);
//        TextView dateTV = findViewById(R.id.selectedDateTextView);
//        resourceSpinner = findViewById(R.id.resourceSpinner);
//        startTimeButton = findViewById(R.id.startTimeButton);
//        endTimeButton = findViewById(R.id.endTimeButton);
//        confirmBookingButton = findViewById(R.id.confirmBookingButton);
//        Intent intent = getIntent();
//        businessId = intent.getStringExtra(EXTRA_BUSINESS_ID);
//        year = intent.getIntExtra(EXTRA_YEAR, -1);
//        month = intent.getIntExtra(EXTRA_MONTH, -1);
//        day = intent.getIntExtra(EXTRA_DAY, -1);
//        dateTV.setText(String.format(Locale.getDefault(), "תאריך: %d/%d/%d", day, month + 1, year));
//        loadBusinessDataForBooking();
//        startTimeButton.setOnClickListener(v -> showTimePicker(true));
//        endTimeButton.setOnClickListener(v -> showTimePicker(false));
//        confirmBookingButton.setOnClickListener(v -> createBooking());
//        NotificationHelper.createNotificationChannel(this);
    //   }


//    private void loadBusinessDataForBooking() {
//        db.collection("businesses").document(businessId).get().addOnSuccessListener(doc -> {
//            if (doc.exists()) {
//                business = doc.toObject(Business.class);
//                if (business != null && business.getResources() != null) {
//                    List<String> names = business.getResources().stream().map(Resource::getName).collect(Collectors.toList());
//                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
//                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//                    resourceSpinner.setAdapter(adapter);
//                }
//            }
//        });
//    }

//    private void showTimePicker(boolean isStart) {
//        new TimePickerDialog(this, (view, h, m) -> {
//            Calendar cal = Calendar.getInstance(); cal.set(year, month, day, h, m, 0);
//            if (isStart) { startTime = cal; startTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)); }
//            else { endTime = cal; endTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)); }
//        }, 9, 0, true).show();
//    }

//    private void createBooking() {
//        if (!isInputValid()) return;
//        if (startTime.before(Calendar.getInstance())) { Toast.makeText(this, "זמן עבר", Toast.LENGTH_SHORT).show(); return; }
//        String resName = (String) resourceSpinner.getSelectedItem();
//        Resource res = getSelectedResource(resName);
//        if (res == null) return;
//        if (!isBookingWithinWorkingHours(startTime, endTime)) { Toast.makeText(this, "מחוץ לשעות", Toast.LENGTH_SHORT).show(); return; }
//        checkCollisionsAndSave(new Timestamp(startTime.getTime()), new Timestamp(endTime.getTime()), resName, res.getQuantity());
//    }

//    private void checkCollisionsAndSave(Timestamp s, Timestamp e, String name, int qty) {
//        db.collection("bookings").whereEqualTo("businessId", businessId).whereEqualTo("resourceName", name).whereLessThan("startTime", e).get()
//                .addOnSuccessListener(snaps -> {
//                    int count = 0;
//                    for (QueryDocumentSnapshot d : snaps) {
//                        Booking ex = d.toObject(Booking.class);
//                        if (ex.getEndTime().compareTo(s) > 0) count++;
//                    }
//                    if (count >= qty) Toast.makeText(this, "תפוס", Toast.LENGTH_SHORT).show();
//                    else saveBookingFinal(s, e, name);
//                });
//    }

//    private void saveBookingFinal(Timestamp s, Timestamp e, String res) {
//        String bId = db.collection("bookings").document().getId();
//        Booking b = new Booking(bId, businessId, Objects.requireNonNull(mAuth.getCurrentUser()).getUid(), res, s, e);
//        db.collection("bookings").document(bId).set(b).addOnSuccessListener(v -> {
//            Toast.makeText(this, "בוצע!", Toast.LENGTH_SHORT).show();
//            scheduleReminder(b);
//            finish();
//        });
//    }

//    private void scheduleReminder(Booking b) {
//        long time = System.currentTimeMillis() + 10000;
//        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//        if (am == null) return;
//        Intent i = new Intent(this, ReminderReceiver.class);
//        PendingIntent pi = PendingIntent.getBroadcast(this, b.getBookingId().hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
//            am.set(AlarmManager.RTC_WAKEUP, time, pi);
//        } else {
//            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
//        }
//    }

//    private boolean isInputValid() {
//        if (mAuth.getCurrentUser() == null || startTime == null || endTime == null || resourceSpinner.getSelectedItem() == null) return false;
//        if (!endTime.after(startTime)) { Toast.makeText(this, "סיום לפני התחלה", Toast.LENGTH_SHORT).show(); return false; }
//        return true;
//    }

//    private Resource getSelectedResource(String n) {
//        if (business != null && business.getResources() != null) {
//            for (Resource r : business.getResources()) if (r.getName().equals(n)) return r;
//        }
//        return null;
//    }

//    private boolean isBookingWithinWorkingHours(Calendar s, Calendar e) {
//        if (business == null || business.getWorkingHours() == null) return false;
//        String dayN = getDayName(s.get(Calendar.DAY_OF_WEEK));
//        for (WorkingHours wh : business.getWorkingHours()) {
//            if (dayN.equals(wh.getDayOfWeek())) {
//                for (TimeSlot ts : wh.getTimeSlots()) {
//                    Calendar start = (Calendar) s.clone(); start.set(Calendar.HOUR_OF_DAY, ts.getStartHour()); start.set(Calendar.MINUTE, ts.getStartMinute());
//                    Calendar end = (Calendar) e.clone(); end.set(Calendar.HOUR_OF_DAY, ts.getEndHour()); end.set(Calendar.MINUTE, ts.getEndMinute());
//                    if (!s.before(start) && !e.after(end)) return true;
//                }
//            }
//        }
//        return false;
//    }

//    private String getDayName(int d) {
//        switch (d) {
//            case Calendar.SUNDAY: return "ראשון"; case Calendar.MONDAY: return "שני"; case Calendar.TUESDAY: return "שלישי";
//            case Calendar.WEDNESDAY: return "רביעי"; case Calendar.THURSDAY: return "חמישי"; case Calendar.FRIDAY: return "שישי";
//            case Calendar.SATURDAY: return "שבת"; default: return "";
//        }
//    }

    //הפונקציה לא מקבלת ומחזירה כלום ומראה את רשימת השעות הלא מלאה על המסך
    private void setupWorkingHoursViews() {
        //רשימת שמות של ימים
        String[] days = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};
        /// /לוקח את הXML ונותן לנו להשתמש בו כאובייקט חי בקוד
        LayoutInflater inf = LayoutInflater.from(this);
        /// /
        //לולאה שעוברת על הימים
        for (String day : days) {
            //יוצר אובייקט של יום חדש
            WorkingHours wh = new WorkingHours(day);
            //מוסיף את האובייקט לרשימה של הימים
            workingHoursList.add(wh);
            // יוצר ITEM של day_working_hours_item שיצרנו
            View v = inf.inflate(R.layout.day_working_hours_item, workingHoursContainer, false);
            //הולכים לשורה של האייטם שנוצר וכותבים את היום הנוכחי בלולאה
            ((TextView) v.findViewById(R.id.dayNameTextView)).setText("יום " + day);
            //כותבים לא הוגדר על השעות פעילות כברירת מחדל
            TextView tv = v.findViewById(R.id.hoursTextView); tv.setText("לא הוגדר");
            // שם את כל הטקסט שהכנו בשורה
            dayHoursTextViews.put(day, tv);
            //כשלוחצים על כפתור העריכה של השעות - לפתוח דיאלוג לבחירת שעות
            v.findViewById(R.id.editHoursButton).setOnClickListener(view -> showEditHoursDialog(wh));
            //לוקח את השורה שהכנו - האייטם , ומוסיף אותה פיזית למסך
            workingHoursContainer.addView(v);
        }
    }

    //הפונקציה מעדכנת את השינויים בשעון על המסך - מקבלת אובייקט של WH של יום מסוים
    private void updateDayHoursTextView(WorkingHours wh) {
        //לוקח את השורה שעשינו בפונקציה setupWorkingHoursViews
        TextView tv = dayHoursTextViews.get(wh.getDayOfWeek());
        //אם השורה לא ריקה
        if (tv != null) {
            //אם שעות הפעילות ריקות
            if (wh.getTimeSlots().isEmpty()) tv.setText("לא הוגדר");
            //אם לא
            else {
                //מיון הסלוטים
                wh.getTimeSlots().sort(Comparator.comparingInt(TimeSlot::getStartHour));
                //משנה את הטקסט ומחברת את כל השעות למחרוזת אחת ארוכה
                tv.setText(wh.getTimeSlots().stream().map(TimeSlot::toString).collect(Collectors.joining(", ")));
            }
        }
    }

    //הקפצת הדיאלוג של הוספת משאב כאשר המשתמש לוחץ על הכפתור
    private void showAddResourceDialog() {
        //יוצר אובייקט של דיאלוג חדש
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        //יוצר את האובייקט עצמו - מה שבתוך הB - משתמש בdialog_add_resource שיצרנו
        View v = getLayoutInflater().inflate(R.layout.dialog_add_resource, null);
        // לוקחים את כל הפרטים שהוזנו בדיאלוג ושם כל אחד מהם במשתנה שלו
        final TextInputEditText nET = v.findViewById(R.id.resourceNameEditText), cET = v.findViewById(R.id.resourceCapacityEditText), qET = v.findViewById(R.id.resourceQuantityEditText);
        //יוצרים כפתור בתוך הדיאלוג של הוסף. אם ילחצו עליו מה שקורה אחרי החץ יקרה
        b.setView(v).setPositiveButton("הוסף", (d, id) -> {
           //ממיר את האדיט טקסט של השם לסטרינג
            String name = nET.getText().toString();
            //אם השם לא ריק, תשתמש בפונקציה addResourceToList ותנסה להוסיף את המשאב לרשימה
            if (!name.isEmpty()) addResourceToList(new Resource(name, Integer.parseInt(cET.getText().toString()), Integer.parseInt(qET.getText().toString())));
        }).setNegativeButton("בטל", null).show();//כפתור של בטל
    }

    //הפונקציה מקבלת אובייקט של משאב ומנסה להוסיף אותו לרשימת המשאבים
    private void addResourceToList(Resource r) {
        //מוסיף את האובייקט לרשימה
        resourceList.add(r);
        //יוצר אובייקט של האייטם resource_item_view
        View v = getLayoutInflater().inflate(R.layout.resource_item_view, resourcesContainer, false);
        //מוסיפים את שם המשאב
        ((TextView) v.findViewById(R.id.resourceDetailsTextView)).setText(r.getName());
        //הוספת כפתור מחיקת משאב
        v.findViewById(R.id.removeResourceButton).setOnClickListener(view -> { resourceList.remove(r); resourcesContainer.removeView(v); });
        //מכניסים את השורה למסך
        resourcesContainer.addView(v);
    }
    /// /מקור עזר מספר 1
    //הפונקציה מקבלת יום ונותנת לבעל העסק להזין את הסלוטים בתוך הדיאלוג
    private void showEditHoursDialog(@NonNull final WorkingHours wh) {
        //יוצר רשימה של סלוטים
        final List<TimeSlot> slots = new ArrayList<>();
        //מעתיקים את כל השעות הקיימות לתוך הרשימה שיצרנו בשורה הקודמת
        wh.getTimeSlots().forEach(ts -> slots.add(new TimeSlot(ts.getStartHour(), ts.getStartMinute(), ts.getEndHour(), ts.getEndMinute())));
       //יוצר אובייקט של הדיאלוג
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        //משתמש בדיאלוג שיצרנו
        View v = getLayoutInflater().inflate(R.layout.dialog_edit_working_hours, null);
        //יוצר מכולה שבה נשים את הכל
        LinearLayout container = v.findViewById(R.id.timeSlotsContainer);
        //עוברים על לולאה של הסלוטים שכבר קיימים ושמים אותם בתוך הדיאלוג
        for (TimeSlot ts : slots) addTimeSlotViewToDialog(getLayoutInflater(), container, slots, ts);
        //כשלוחצים על כפתור הוסף חלון זמן
        v.findViewById(R.id.addTimeSlotButton).setOnClickListener(view -> {
            //מעלה את השעון התחלה
            new TimePickerDialog(this, (v1, h1, m1) -> {
                //מעלה את השעון סוף
                new TimePickerDialog(this, (v2, h2, m2) -> {
                    TimeSlot ts = new TimeSlot(h1, m1, h2, m2);
                    slots.add(ts);
                    addTimeSlotViewToDialog(getLayoutInflater(), container, slots, ts);
                }, 9, 0, true).show();//ברירת מחדל
            }, 9, 0, true).show();//ברירת מחדל
        });
        //כפתור שמירה
        b.setView(v).setPositiveButton("שמור", (d, id) -> { wh.setTimeSlots(slots); updateDayHoursTextView(wh); }).setNegativeButton("בטל", null).show();
    }

//הפוקנציה שמה פיזית את השעות בדיאלוג
    private void addTimeSlotViewToDialog(LayoutInflater inf, LinearLayout con, List<TimeSlot> list, TimeSlot ts) {
        View v = inf.inflate(R.layout.time_slot_item, con, false);
        ((TextView) v.findViewById(R.id.timeSlotTextView)).setText(ts.toString());
        v.findViewById(R.id.removeTimeSlotButton).setOnClickListener(view -> { list.remove(ts); con.removeView(v); });
        con.addView(v);
    }
}
/// /