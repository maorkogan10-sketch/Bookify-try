package com.example.bookify_try;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    //און קרייט שקורה כשהמסך נוצר
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //חיבור לפיירבייס
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }
//און סטרט כל פעם שהאקטיביטי הופך להיות גלוי למשתמש
    @Override
    protected void onStart() {
        super.onStart();
        //המשתמש הנוכחי
        FirebaseUser currentUser = mAuth.getCurrentUser();
        //אם הוא מחובר תפנה אותו לפונקציה
        if (currentUser != null) {
            redirectUser(currentUser.getUid());
        } else {
            //אם הוא לא מחובר תראה לו את מסך הפתיחה
            showWelcomeScreen();
        }
    }

    //הפונקציה מקבלת את הID של הלקוח שמחובר ומפנה אותו למסך הבית המתאים לו - בעל עסק/לקוח
    private void redirectUser(String userId) {
        //הולכים לאוסף המשתמשים לID של הלקוח ומביאים את צילום המסך של המסמך
        db.collection("users").document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        //אם המשימה הצליחה
                        if (task.isSuccessful()) {
                            //משתנה שיחזיק את התוצאה
                            DocumentSnapshot document = task.getResult();
                            //אם לא ריק
                            if (document != null && document.exists()) {
                                //סוג המשתמש
                                String userType = document.getString("userType");
                                //אם זה בעל עסק להעביר למסך הבית של בעל עסק
                                if ("Owner".equals(userType)) {
                                    startActivity(new Intent(MainActivity.this, OwnerHomeActivity.class));
                                } else {
                                    //אחרת להעביר למסך בית של הלקוח
                                    startActivity(new Intent(MainActivity.this, CustomerHomeActivity.class));
                                }
                                finish(); // סוגר את המסך ועובר
                            } else {
                                // אם הייתה בעיה והמסמך ריק או לא קיים
                                Log.d(TAG, "No such document");
                                Toast.makeText(MainActivity.this, "שגיאה בטעינת נתוני משתמש.", Toast.LENGTH_SHORT).show();
                                FirebaseAuth.getInstance().signOut();
                                //להעביר למסך הבית של האפליקציה
                                showWelcomeScreen();
                            }
                        }
                            //אם היה כישלון בהבאת המסמך
                            else {
                            Log.d(TAG, "get failed with ", task.getException());
                            Toast.makeText(MainActivity.this, "שגיאה בטעינת נתונים.", Toast.LENGTH_SHORT).show();
                            FirebaseAuth.getInstance().signOut();
                            //להעביר למסך הבית של האפליקציה
                            showWelcomeScreen();
                        }
                    }
                });
    }

    //הפונקציה לא עושה כלום ומעלה את מסך הבית של האפליקציה
    private void showWelcomeScreen() {
        //חיבור לXML
        setContentView(R.layout.activity_main);

        //חיבור לכפתורים
        Button signUpButton = findViewById(R.id.signUpButton);
        Button logInButton = findViewById(R.id.logInButton);

        //לחיצה על כפתור הרשמה
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SignUpActivity.class));
            }
        });

        //לחיצה על כפתור התחברות
        logInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LogInActivity.class));
            }
        });
    }
}