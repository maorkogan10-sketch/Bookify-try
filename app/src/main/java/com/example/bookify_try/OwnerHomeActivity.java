package com.example.bookify_try;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

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

    private LinearLayout businessExistsGroup;
    private Button createBusinessButton;
    private Button viewBookingsButton;
    private Button editBusinessButton;
    private Button logOutButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views
        businessExistsGroup = findViewById(R.id.businessExistsGroup);
        createBusinessButton = findViewById(R.id.createBusinessButton);
        viewBookingsButton = findViewById(R.id.viewBookingsButton);
        editBusinessButton = findViewById(R.id.editBusinessButton);
        logOutButton = findViewById(R.id.logOutButton);

        // Set listeners
        createBusinessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OwnerHomeActivity.this, CreateBusinessActivity.class);
                startActivity(intent);
            }
        });

        viewBookingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Navigate to ViewBookingsActivity
                Toast.makeText(OwnerHomeActivity.this, "Navigate to View Bookings", Toast.LENGTH_SHORT).show();
            }
        });

         editBusinessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Navigate to EditBusinessActivity
                Toast.makeText(OwnerHomeActivity.this, "Navigate to Edit Business", Toast.LENGTH_SHORT).show();
            }
        });

        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(OwnerHomeActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkIfBusinessExists();
    }

    private void checkIfBusinessExists() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Should not happen, but as a safeguard
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String userId = currentUser.getUid();

        // The business document ID is the same as the owner's UID.
        db.collection("businesses").document(userId).get()
            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Business exists
                            Log.d(TAG, "Business document found.");
                            businessExistsGroup.setVisibility(View.VISIBLE);
                            createBusinessButton.setVisibility(View.GONE);
                        } else {
                            // Business does not exist
                            Log.d(TAG, "No such document. User needs to create a business.");
                            businessExistsGroup.setVisibility(View.GONE);
                            createBusinessButton.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                        Toast.makeText(OwnerHomeActivity.this, "Error checking for business.", Toast.LENGTH_SHORT).show();
                        // Show create button as a fallback
                        createBusinessButton.setVisibility(View.VISIBLE);
                        businessExistsGroup.setVisibility(View.GONE);
                    }
                }
            });
    }
}