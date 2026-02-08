package com.example.bookify_try;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    private EditText fullNameEditText, emailEditText, passwordEditText;
    private RadioGroup userTypeRadioGroup;
    private Button createAccountButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views by ID
        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        userTypeRadioGroup = findViewById(R.id.userTypeRadioGroup);
        createAccountButton = findViewById(R.id.createAccountButton);

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAccount();
            }
        });
    }

    private void createAccount() {
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // --- Input Validation ---
        if (TextUtils.isEmpty(fullName)) {
            fullNameEditText.setError("יש למלא שם מלא.");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("יש למלא כתובת אימייל.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("יש למלא סיסמה.");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("הסיסמה חייבת להכיל לפחות 6 תווים.");
            return;
        }

        // כאן יוצרים AUTH חדש
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, now save user data to Firestore
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                saveUserDataToFirestore(firebaseUser, fullName);
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUpActivity.this, "הרשמה נכשלה: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserDataToFirestore(FirebaseUser firebaseUser, String fullName) {
        String email = firebaseUser.getEmail();
        String uid = firebaseUser.getUid();

        int selectedId = userTypeRadioGroup.getCheckedRadioButtonId();
        String userType = (selectedId == R.id.customerRadioButton) ? "Customer" : "Owner";

        User user = new User(fullName, email, userType);

        // יוצרים כאן DOCUMENT חדש עם האיי די
        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                        Toast.makeText(SignUpActivity.this, "ההרשמה הושלמה בהצלחה!", Toast.LENGTH_LONG).show();

                        // סוגר את כל המסכים שהיו פתוחים עד עכשיו ומחזיר למסך הבית
                        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish(); // סוגר לגמרי
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                        Toast.makeText(SignUpActivity.this, "שגיאה בשמירת נתונים.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}