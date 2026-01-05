package com.example.bookify_try;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class CustomerHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);

        Button searchBusinessButton = findViewById(R.id.searchBusinessButton);
        Button myBookingsButton = findViewById(R.id.myBookingsButton);
        Button logOutButton = findViewById(R.id.logOutButton);

        searchBusinessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerHomeActivity.this, SearchBusinessActivity.class);
                startActivity(intent);
            }
        });

        myBookingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Navigate to MyBookingsActivity
                Toast.makeText(CustomerHomeActivity.this, "My Bookings clicked", Toast.LENGTH_SHORT).show();
            }
        });

        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerHomeActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}