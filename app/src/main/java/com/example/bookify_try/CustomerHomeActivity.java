package com.example.bookify_try;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class CustomerHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //חיבור לXML
        setContentView(R.layout.activity_customer_home);

        //חיבור לכפתורים
        Button searchBusinessButton = findViewById(R.id.searchBusinessButton);
        Button myBookingsButton = findViewById(R.id.myBookingsButton);
        Button logOutButton = findViewById(R.id.logOutButton);

        //כשלחצו על כפתור חיפוש העסקים
        searchBusinessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //מעביר למסך חיפוש ההזמנות
                Intent intent = new Intent(CustomerHomeActivity.this, SearchBusinessActivity.class);
                startActivity(intent);
            }
        });

        //כשלוחצים על כפתור צפייה בהזמנות
        myBookingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //מעביר למסך ההזמנות שלי
                Intent intent = new Intent(CustomerHomeActivity.this, MyBookingsActivity.class);
                startActivity(intent);
            }
        });


        //כשלוחצים על כפתור התנתקות
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //משתמש בפעולה מספריית AUTH שבעצם אומרת התנתקות המשתמש הנוכחי שמחובר
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerHomeActivity.this, HomeActivity.class);
                //סוגר את כל מה שמעל המסך - מוודא שהמשתמש לא יוכל לחזור אחורה למסך הזה אחרי שהוא התנתק
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                //סוגר את המסך הזה שלא יהיה ניתן לחזור אליו
                finish();
            }
        });
    }
}