package com.example.bookify_try;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class cheking extends AppCompatActivity {

    private Button myButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cheking);

        myButton = findViewById(R.id.myButton);
        myButton.setOnClickListener(v -> {

            Toast.makeText(this, "הודעה למסך", Toast.LENGTH_SHORT).show();

        });

    }
}