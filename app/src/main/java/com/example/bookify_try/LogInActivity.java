package com.example.bookify_try;

import
        android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LogInActivity extends AppCompatActivity {

    private static final String TAG = "LogInActivity";

    private EditText emailEditText, passwordEditText;
    private Button logInButton;
    private TextView forgotPasswordTextView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        //מתחבר לפיירבייס
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        logInButton = findViewById(R.id.logInButton);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);

        logInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logInUser();
            }
        });

        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });
    }

    //הפונקציה לא מקבלת כלום וזאת הפונקציה שבעצם מבצעת את סיום תהליך ההרשמה - מתרחשת כשלוחצים על כפתור ההרשמה
    private void logInUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        //בודק שלא הזינו טקסט ריק
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("יש למלא כתובת אימייל.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("יש למלא סיסמה.");
            return;
        }

        //משתמש בפונקציה של AUTH של התחברות למערכת באמצעות המייל והסיסמא
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                redirectUser(user.getUid());
                            }
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LogInActivity.this, "התחברות נכשלה. בדוק אימייל וסיסמה.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    //הפונקציה מקבלת את הUID של המשתמש בפיירבייס ומנתבת אותו לאן שהוא צריך להגיע - לקוח/בעל עסק
    private void redirectUser(String userId) {
        //הולך לאוסף הUSERS, ומביא את המסך עם הUID שקיבל
        db.collection("users").document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            //משתנה שמחזיק את המסמך שהוציא
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                //סוג הלקוח
                                String userType = document.getString("userType");
                                Toast.makeText(LogInActivity.this, "התחברות מוצלחת!", Toast.LENGTH_SHORT).show();
                                //בעל עסק, מנתב אותו למסך הבית של בעל העסק. אם לא אז מנתב למסך הבית של הלקוח
                                if ("Owner".equals(userType)) {
                                    startActivity(new Intent(LogInActivity.this, OwnerHomeActivity.class));
                                } else {
                                    startActivity(new Intent(LogInActivity.this, CustomerHomeActivity.class));
                                }
                                finishAffinity(); // סוגר את כל הACTIVITY שפתוחות כדי שהמחסנית תתרוקן והמסך הבא יהיה הראשון שקיים במחסנית
                            } else {
                                Log.d(TAG, "No such document");
                                Toast.makeText(LogInActivity.this, "שגיאה בטעינת נתוני משתמש.", Toast.LENGTH_SHORT).show();
                                FirebaseAuth.getInstance().signOut();
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                            Toast.makeText(LogInActivity.this, "שגיאה בטעינת נתונים.", Toast.LENGTH_SHORT).show();
                            FirebaseAuth.getInstance().signOut();
                        }
                    }
                });
    }

    //הפונקציה לא מקבלת כלום ושולחת את הלקוח לאיפוס סיסמא
    private void resetPassword() {
        String email = emailEditText.getText().toString().trim();

        //בודק אם האימייל ריק
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("יש למלא כתובת אימייל כדי לאפס סיסמה.");
            return;
        }

        //משתמש בפעולה שAUTH עושה שנותן לשחזר סיסמא באמצעות מייל
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email sent.");
                            Toast.makeText(LogInActivity.this, "מייל לאיפוס סיסמה נשלח לכתובת " + email, Toast.LENGTH_LONG).show();
                        } else {
                            Log.w(TAG, "sendPasswordResetEmail:failure", task.getException());
                            Toast.makeText(LogInActivity.this, "שליחת מייל נכשלה.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}