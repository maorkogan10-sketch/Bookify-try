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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in, redirect to the correct home screen
            redirectUser(currentUser.getUid());
        } else {
            // No user is signed in, show the welcome screen with login/signup buttons
            showWelcomeScreen();
        }
    }

    private void redirectUser(String userId) {
        db.collection("users").document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String userType = document.getString("userType");
                                if ("Owner".equals(userType)) {
                                    startActivity(new Intent(MainActivity.this, OwnerHomeActivity.class));
                                } else {
                                    startActivity(new Intent(MainActivity.this, CustomerHomeActivity.class));
                                }
                                finish(); // Close this activity
                            } else {
                                // Document doesn't exist, something is wrong. Log out and show welcome.
                                Log.d(TAG, "No such document");
                                Toast.makeText(MainActivity.this, "שגיאה בטעינת נתוני משתמש.", Toast.LENGTH_SHORT).show();
                                FirebaseAuth.getInstance().signOut();
                                showWelcomeScreen();
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                            Toast.makeText(MainActivity.this, "שגיאה בטעינת נתונים.", Toast.LENGTH_SHORT).show();
                            FirebaseAuth.getInstance().signOut();
                            showWelcomeScreen();
                        }
                    }
                });
    }

    private void showWelcomeScreen() {
        setContentView(R.layout.activity_main);

        Button signUpButton = findViewById(R.id.signUpButton);
        Button logInButton = findViewById(R.id.logInButton);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SignUpActivity.class));
            }
        });

        logInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LogInActivity.class));
            }
        });
    }
}