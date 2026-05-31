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
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.widget.ArrayAdapter;
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
    //מספר מזהה שנשתמש בו לבקש רשות מהמשתמש לשלוח התראות
    private static final int NOTIFICATION_PERMISSION_CODE = 123;

    //משתנים של רמזים שיחזיקו את הרמזים מהמסך הקודם
    public static final String EXTRA_BUSINESS_ID = "EXTRA_BUSINESS_ID";
    public static final String EXTRA_YEAR = "EXTRA_YEAR";
    public static final String EXTRA_MONTH = "EXTRA_MONTH";
    public static final String EXTRA_DAY = "EXTRA_DAY";

    //אפשרות בחירה שבתוכה יש את כל המשאבים
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
        //חיבור לאקסמל
        setContentView(R.layout.activity_create_booking);

        //חיבור לפיירבייס
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        //חיבור לXML
        TextView selectedDateTextView = findViewById(R.id.selectedDateTextView);
        resourceSpinner = findViewById(R.id.resourceSpinner);
        startTimeButton = findViewById(R.id.startTimeButton);
        endTimeButton = findViewById(R.id.endTimeButton);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);

        // חיבור הרמזים מהמסך הקודם
        Intent intent = getIntent();
        businessId = intent.getStringExtra(EXTRA_BUSINESS_ID);
        year = intent.getIntExtra(EXTRA_YEAR, -1);
        month = intent.getIntExtra(EXTRA_MONTH, -1);
        day = intent.getIntExtra(EXTRA_DAY, -1);

        //אם לא הצליחו להתחבר לרמזים
        if (businessId == null || year == -1) {
            Toast.makeText(this, "Error loading booking details", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //משנה את הכותרת במסך כך שייראה את התאריך שאותו בחרו בלוח השנה להזמין בו
        selectedDateTextView.setText(String.format(Locale.getDefault(), "תאריך: %d/%d/%d", day, month + 1, year));

        //קריאה לפונקציה שמעלה את הנתונים לספינר (סוגי המשאבים)
        loadBusinessData();

        //כשלוחצים על הכפתור של בחירת השעה זה קורא לפונקציה שמראה את השעון המובנה של האנרואיד - TRUE שעת התחלה
        startTimeButton.setOnClickListener(v -> showTimePicker(true));
        endTimeButton.setOnClickListener(v -> showTimePicker(false));
        //כשלוחצים על הכפתור זה הולך לפונקציה שבודקת האם ניתן להזמין
        confirmBookingButton.setOnClickListener(v -> createBooking());

        //קריאה לקלאס NotificationHelper שיוצר מסלול שדרכו יעברו ההתראות
        NotificationHelper.createNotificationChannel(this);
        //קורא לפונקציה שמבקשת מהמשתמש לשלוח התראות
        checkNotificationPermission();
    }

    /// /מקור עזר מספר 1
    //הפונקציה לא מקבלת כלום ולא מחזירה כלום היא מבקשת מהמשתמש אישור לשלוח לו התראות
    private void checkNotificationPermission() {
        //בודק אם הגרסה הנוכחית מחייבת לבקש אישור מהמשתמש לשלוח לו התראות
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //אם אין הרשאה
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                //האנדרואיד מקפיץ למשתמש את השאלה על המסך
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }
    /// /

//פונקציה שנפתחת אחרי שהמשתמש לחץ על אשר או סרב
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //בודק אם המספר שהגיע שווה לקוד 123 שקבענו למעלה ולא קשור למשהו אחר
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            //אם ההשראה אושרה
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "הרשאת התראות אושרה!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "לא תקבל תזכורות ללא אישור התראות.", Toast.LENGTH_LONG).show();
            }
        }
    }

    //הפוקנציה לא מקבלת ולא מחזירה כלום ומעלה את הנתונים של המשאבים הקיימים לתוך הספינר (אפשרות הבחירה)
    private void loadBusinessData() {
        //הולך לקולקשן עסקים למסמך של העסק עם האיידי שקיבל ומוציא ממנו כמו צילום של המסמך
        db.collection("businesses").document(businessId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    //אם המסמך קיים
                    if (documentSnapshot.exists()) {
                        //ממיר את המסמך לאובייקט של עסק ושומר אותו באובייקט שיצרנו
                        business = documentSnapshot.toObject(Business.class);
                        //אם העסק לא ריק ורשימת המשאבים לא ריקה
                        if (business != null && business.getResources() != null) {
                            //יוצר רשימה של כל המשאבים שיש לעסק, הופך אותם לפס ייצור אחד אחרי השני (STREAM) לוקח רק את השם שלהם (MAP) ושם אותם בתוך הרשימה (COLLECT)
                            List<String> resourceNames = business.getResources().stream()
                                    .map(Resource::getName)
                                    .collect(Collectors.toList());
                            //יוצר אוביקט של אדפטר חדש שכבר מובנה בתוך האנרואיד ולא היה צריך ליצור אותו שמותאם להצגה בסיסית בספינר
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, resourceNames);
                            //אומר לספינר שכשהוא נפתח שישמתמש בתפריט נפתח שהמשאבים יהיו בו אחד מתחת לשני
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            //מחבר את האדפטר לספינר
                            resourceSpinner.setAdapter(adapter);
                        }
                    }
                });
    }

    //הפונקציה מקבלת האם השעון צריך להציג את שעת ההתחלה או שעת הסיום לפי הBOOL ולא מחזירה כלום ובעצם מציגה את השעון העגול של אנדוראיד ושומרת את השעה שהמשתמש בחר
    private void showTimePicker(boolean isStartTime) {
        //רכיב מוכן של דיאלוג שמובנה באנדרואיד שמקפיץ את החלון עם ונותן לו להזין שעות ודקות
        TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            //יוצר אובייקט לוח שנה חדש שמחזיק את הזמן של כרגע
            Calendar selectedTime = Calendar.getInstance();
            //מעדכן את האובייקט עם הפרטים שהמשתמש בחר
            selectedTime.set(year, month, day, hourOfDay, minute, 0);
            //אם השעון הוא לשעת התחלה
            if (isStartTime) {
                //שומר את הזמן שבחרנו במשתמש שהגדרנו בתחילת המסך
                startTime = selectedTime;
                //משנה את מה שמופיע על הכפתור לשעה שנבחרה
                startTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            } else {
                endTime = selectedTime;
                endTimeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }
        }, 9, 0, true); //ברירת מחדל של מה השעון יציג בתצוגה ראשונית
        //קורא לפונקציה TIMEPICKER שמראה את השעון עצמו
        timePicker.show();
    }

    //הפונקציה לא מקבלת ולא מחזירה כלום והיא קוראת כאשר המשתמש לוחץ על הכפתור של יצירת הזמנה ובודקת האם אפשר להזמין את ההזמנה
    private void createBooking() {
        //קורא לפונקציית עזר שבודקת אם המשתמש הזין את כל השדות והכל תקין ומחובר ואם לא עוצרת ישר הכל
        if (!isInputValid()) return;

        // בודק ששעת ההתחלה לא עברה כבר ונמצאת אחרי הזמן של ההווה
        if (startTime.before(Calendar.getInstance())) {
            Toast.makeText(this, "לא ניתן להזמין זמן שכבר עבר.", Toast.LENGTH_LONG).show();
            return;
        }

        //ממיר מאובייקט הקלנדר לאובייקט של Timestamp את שעת ההתחלה ושעת הסיום
        Timestamp startTimestamp = new Timestamp(startTime.getTime());
        Timestamp endTimestamp = new Timestamp(endTime.getTime());
        //משתנה שמחזיק את השם של המשאב שנבחר בספינר
        String selectedResourceName = (String) resourceSpinner.getSelectedItem();

        //קורא לפונקציית עזר getSelectedResource שעוזרת לקבל את האובייקט המלא של המשאב
        Resource selectedResource = getSelectedResource(selectedResourceName);
       // בודק שהאובייקט לא ריק
        if (selectedResource == null) {
            Toast.makeText(this, "Error: Resource details not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        //קורא לפונקציית עזר שבודקת האם השעות שהוזנו נמצאות בתוך שעות הפעילות של העסק
        if (!isBookingWithinWorkingHours(startTime, endTime)) {
            Toast.makeText(this, "ההזמנה מחוץ לשעות הפעילות של העסק", Toast.LENGTH_LONG).show();
            return;
        }

        //קורא לפונקציית עזר שבודקת בענן אם המשאב תפוס או לא
        checkCollisionsAndSave(startTimestamp, endTimestamp, selectedResourceName, selectedResource.getQuantity());
    }

    // הפונקציה מקבלת את זמן ההתחלה, הסיום, שם המשאב והכמות מאותו משאב ובודקת האם ניתן להשלים את ההזמנה ויש מקום פנוי
    private void checkCollisionsAndSave(Timestamp start, Timestamp end, String resourceName, int resourceQuantity) {
        //הולך לקולקשן bookings, לוקח רק את ההזמנות של העסק הזה, רק של המשאב הזה, ורק את ההזמנו שמתחילות לפני שההזמנה הזו מסתיימת
        //משתמש באינדקס השני כי התנאים משלבים כמה דברים שונים - זמן ושם
        db.collection("bookings")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("resourceName", resourceName)
                .whereLessThan("startTime", end)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    //משתנה שיחזיק כמה הזמנות חופפות להזמנה שלנו
                    int conflictingBookingsCount = 0;
                    //לולאה שעוברת על כל המסמכים שעמדו בקריטריונים
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        //המרה לאובייקט
                        Booking existing = document.toObject(Booking.class);
                        if (existing.getEndTime().compareTo(start) > 0) {
                            conflictingBookingsCount++;
                        }
                    }

                    //בודק אם מספר ההזמנות החופפות גדול או שווה לכמות המשאבים שיש לעסק, המשאב תפוס. אחרת, קרא לפונקציה saveBooking לשמירת ההזמנה
                    if (conflictingBookingsCount >= resourceQuantity) {
                        Toast.makeText(this, "המשאב תפוס לחלוטין בשעות אלו", Toast.LENGTH_LONG).show();
                    } else {
                        saveBooking(start, end, resourceName);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Booking check failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    //הפונקציה מקבלת את זמן ההתחלה והסוף של ההזמנה ואת שם המשאב, שומרת את ההזמנה בפיירבייס וקוראת לפונקציה ששולחת את ההתראות
    private void saveBooking(Timestamp start, Timestamp end, String resource) {
        //מוציא מהAUTH את הUID הייחודי של המשתמש
        String customerId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        //מבקש מפיירבייס לייצר מסמך על שם הID של הזמנה חדשה
        String bookingId = db.collection("bookings").document().getId();
        //יוצר אובייקט הזמנה חדש
        Booking booking = new Booking(bookingId, businessId, customerId, resource, start, end);

        //מוסיף את האובייקט של ההזמנה לקולקשן של ההזמנות תחת הID שיצרנו
        db.collection("bookings").document(bookingId).set(booking)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "ההזמנה בוצעה בהצלחה!", Toast.LENGTH_LONG).show();
                    //קורא לפונקציה ששולחת את ההתראה
                    scheduleReminder(booking);
                    //מעביר את המשתמש חזרה למסך חיפוש העסקים
                    Intent intent = new Intent(this, SearchBusinessActivity.class);
                    //מנקה את ההיסטוריה של המסכים במחסנית
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    //סוגר את המסך
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    //הפונקציה מקבלת את האובייקט של ההזמנה ואחראית לשלוח את ההתראת תזכורת
    private void scheduleReminder(Booking booking) {
        //קובע מתי התזכורת תקרה
        //שורה לצורך בדיקה - 10 שניות אחרי שההזמנה בוצעה
        long reminderTimeMillis = System.currentTimeMillis() + 10000;
        //ההתראה בפועל - שעה לפני ההזמנה
        // long reminderTimeMillis = booking.getStartTime().toDate().getTime() - (60 * 60 * 1000);
        //יוצר משתנה של alarmManager (פקודה עתידית)
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        //כאשר הזמן יגיע, תפעיל את הקלאס ReminderReceiver שנוצר מכך שפתחנו את ערוץ התקשורת בעזרת הNotificationHelper
        Intent intent = new Intent(this, ReminderReceiver.class);
        //מוסיף רמזים לקלאס של מה יהיה כתוב בהתראה
        intent.putExtra("title", "תזכורת להזמנה");
        intent.putExtra("message", "יש לך הזמנה ל-" + booking.getResourceName() + " בעוד שעה. ניפגש! ");

/// /מקור עזר מספר 1
        //אישור למערכת להשתמש באינטנט של הRECIVER אוטומטית
        //האינטנט הרגיל לRECIVER נעטף בPENDING  כדי שנוכל לקשר בינו לבין האלרם מנגר
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 
                booking.getBookingId().hashCode(), //מזהה יחודי לכל הזמנה, כדי שלא הזמנה חדשה תדרוס את הקודמת
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        /// /
        //בודק שהשירות לא ריק
        if (alarmManager != null) {
            try {
                //גם אם הטלפון במצב של חיסכון בסוללה שולחים את ההתראה
                //קורא לאלרם מנגר עם האלרם מנגר, הזמן, והאינטנט של הRECIVER שבו בנויה ההודעה עצמה
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
            } catch (SecurityException e) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
            }
        }
        /// /
    }

    //הפונקציה מקבלת את השעות שהמשתמש בחר ובודקת האם הן נמצאות בתוך תחום הפעילות של העסק
    private boolean isBookingWithinWorkingHours(Calendar bookingStart, Calendar bookingEnd) {
        //בודק שהנתונים לא ריקים
        if (business == null || business.getWorkingHours() == null) return false;
        //משתנה שמחזיק את היום בשבוע של ההזמנה
        int dayOfWeek = bookingStart.get(Calendar.DAY_OF_WEEK);
        //משתנה שמחזיק את שם היום בשבוע - נעזר בפונקציית getDayName
        String dayName = getDayName(dayOfWeek);
        //לולאה שעוברת על כל ימי הפעילות
        for (WorkingHours wh : business.getWorkingHours()) {
            //אם היום הנוכחי בלולאה שווה ליום שהמשתמש בחר
            if (dayName.equals(wh.getDayOfWeek())) {
                //עובר על כל הסלוטים של העסק באתו היום
                for (TimeSlot ts : wh.getTimeSlots()) {
                    //יוצר משתני קלנדר ששומרים את הפתיחה והסגירה של הסלוט הנוכחי
                    //CLONE בסוף כדי לשכפל ולא להרוס את השמתנה המקורי
                    Calendar slotStart = (Calendar) bookingStart.clone();
                    slotStart.set(Calendar.HOUR_OF_DAY, ts.getStartHour());
                    slotStart.set(Calendar.MINUTE, ts.getStartMinute());
                    Calendar slotEnd = (Calendar) bookingEnd.clone();
                    slotEnd.set(Calendar.HOUR_OF_DAY, ts.getEndHour());
                    slotEnd.set(Calendar.MINUTE, ts.getEndMinute());
                    //אם התור נמצא בתוך הסלוט מחזיר TRUE
                    if (!bookingStart.before(slotStart) && !bookingEnd.after(slotEnd)) {
                        return true; 
                    }
                }
            }
        }
        return false;
    }

    //פונקצייה שמקבלת את שם המשאב ומחזירה את האובייקט המלא שלו
    private Resource getSelectedResource(String resourceName) {
        if (business != null && business.getResources() != null) {
            for (Resource res : business.getResources()) {
                if (res.getName().equals(resourceName)) return res;
            }
        }
        return null;
    }

    //הפונקציה לא מקבלת כלום ומחזירה TRUE או FALSE לאם כל השדות הוזנו והכל מחובר
    private boolean isInputValid() {
        //אם אין משתמש מחובר
        if (mAuth.getCurrentUser() == null) return false;
        //אם שעת ההתחלה ושעת הסיום לא ריקות
        if (startTime == null || endTime == null) {
            Toast.makeText(this, "יש לבחור שעת התחלה וסיום", Toast.LENGTH_SHORT).show();
            return false;
        }
        //אם זמן הסיום לפני זמן ההתחלה
        if (!endTime.after(startTime)) {
            Toast.makeText(this, "שעת הסיום חייבת להיות אחרי שעת ההתחלה", Toast.LENGTH_SHORT).show();
            return false;
        }
        //אם לא נבחר משאב בספינר
        if (resourceSpinner.getSelectedItem() == null) {
            Toast.makeText(this, "יש לבחור משאב", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    //פונקציה שמקבלת את מספר היום בשבוע ומחזירה את השם שלו במילים - פונקציית תרגום
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