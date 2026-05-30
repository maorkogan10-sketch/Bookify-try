package com.example.bookify_try;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class OwnerHomeActivity extends AppCompatActivity {

    private static final String TAG = "OwnerHomeActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    //קבוצת כפתורים שמאפשרת להעלים כפתור אחד ולשים במקומו אחר - אדיט במקום קרייט. מאפשר ליצור כמה קומבינציות של כפתורים ולהראות כל פעם אחת מהן
    private LinearLayout businessExistsGroup;
    private Button createBusinessButton;
    private Button viewBookingsButton;
    private Button editBusinessButton;
    private Button logOutButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //חיבור לאקסמל
        setContentView(R.layout.activity_owner_home);

        //חיבור לפיירבייס
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //חיבור הכפתורים
        businessExistsGroup = findViewById(R.id.businessExistsGroup);
        createBusinessButton = findViewById(R.id.createBusinessButton);
        viewBookingsButton = findViewById(R.id.viewBookingsButton);
        editBusinessButton = findViewById(R.id.editBusinessButton);
        logOutButton = findViewById(R.id.logOutButton);

        //כשלוחצים על כפתור הקרייט
        createBusinessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //מעביר למסך יצירת העסק
                Intent intent = new Intent(OwnerHomeActivity.this, CreateBusinessActivity.class);
                startActivity(intent);
            }
        });

        //כשלוחצים על כפתור מסך ההזמנות
        viewBookingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //מעביר למסך שבו רואים את ההזמנות
                Intent intent = new Intent(OwnerHomeActivity.this, ViewBookingsCalendarActivity.class);
                startActivity(intent);
            }
        });

        //כפתור עריכת העסק אחרי שהוא כבר קיים
         editBusinessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // שולח לאותו מסך (CreateBusinessActivity) פשוט במצב העריכה שלו
                Intent intent = new Intent(OwnerHomeActivity.this, CreateBusinessActivity.class);
                startActivity(intent);
            }
        });

         //כשלוחצים על כפתור התנתקות מהעסק
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //סוגר את הסרוויס שעובד ברקע
                stopService(new Intent(OwnerHomeActivity.this, BookingListenerService.class));
                //מנתק את המשתמש הנוכחי שמחובר מהAUTH
                mAuth.signOut();
                //מעביר למסך הHOME ACTIVIY
                Intent intent = new Intent(OwnerHomeActivity.this, HomeActivity.class);
               // מנקה את כל המסכים שהיו פתוחים עד כה
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                //מוריד את המסך מהזיכרון והופך את המסך הבא לראשון במחסנית
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //קורא לפונקציה שבודקת אם קיים עסק
        checkIfBusinessExists();
    }

    //הפונקציה לא מקבלת כלום ובודקת עם העסק קיים
    private void checkIfBusinessExists() {
        //המשתמש הנוכחי שמחובר בAUTH
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        //הID של המשתמש הנוכחי
        String userId = currentUser.getUid();

        //הולך לאוסף העסקים, ובודק אם קיים עסק על שם הID של המשתמש שמחובר
        db.collection("businesses").document(userId).get()
            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        //אם העסק קיים, תציג את הקומבניציה של ערוך עסק וצפה בהזמנות
                        if (document.exists()) {
                            businessExistsGroup.setVisibility(View.VISIBLE);
                            createBusinessButton.setVisibility(View.GONE);
                            //תתחיל את ההאזנה של הסרוויס
                            startBookingService();
                        } else {
                            //אם לא, תציג את הקומבינציה של יצירת עסק
                            businessExistsGroup.setVisibility(View.GONE);
                            createBusinessButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
    }

    //הפונקציה לא מקבלת כלום ומתחילה את הפעילות של הסרוויס
    private void startBookingService() {
        Intent serviceIntent = new Intent(this, BookingListenerService.class);
        startService(serviceIntent);
    }
}