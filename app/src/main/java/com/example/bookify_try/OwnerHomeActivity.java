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
                Intent intent = new Intent(OwnerHomeActivity.this, ViewBookingsCalendarActivity.class);
                startActivity(intent);
            }
        });

         editBusinessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // פתיחת אותו מסך (CreateBusinessActivity) במצב עריכה
                Intent intent = new Intent(OwnerHomeActivity.this, CreateBusinessActivity.class);
                startActivity(intent);
            }
        });

        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(OwnerHomeActivity.this, BookingListenerService.class));
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
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();

        db.collection("businesses").document(userId).get()
            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            businessExistsGroup.setVisibility(View.VISIBLE);
                            createBusinessButton.setVisibility(View.GONE);
                            startBookingService();
                        } else {
                            businessExistsGroup.setVisibility(View.GONE);
                            createBusinessButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
    }

    private void startBookingService() {
        Intent serviceIntent = new Intent(this, BookingListenerService.class);
        startService(serviceIntent);
    }
}