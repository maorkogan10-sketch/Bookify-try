package com.example.bookify_try;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchBusinessActivity extends AppCompatActivity {

    private static final String TAG = "SearchBusinessActivity";

    //הרשימה הממוחזרת שבכל פעם ניתן להציג בה מספר מסוים של רכיבים
    private RecyclerView businessesRecyclerView;

    //משתנה של אדפטר שמציג את רשימת העסקים בחיפוש
    private BusinessAdapter businessAdapter;
    //רשימת העסקים
    private final List<Business> fullBusinessList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //חיבור לXML
        setContentView(R.layout.activity_search_business);

        //חיבור לפיירסטור
        db = FirebaseFirestore.getInstance();

        //חיבור לריסייקל ויו
        businessesRecyclerView = findViewById(R.id.businessesRecyclerView);
        //גודל הרשימה לא משתנה
        businessesRecyclerView.setHasFixedSize(true);
        //קובע איך האובייקטים ברשימה יסתדרו - אחד מתחת לשני
        businessesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        //יוצר אדפטר חדש
        businessAdapter = new BusinessAdapter();
        //חיבור האדפטר לרשימה
        businessesRecyclerView.setAdapter(businessAdapter);

        //יצירת חיפוש וחיבור לXML
        SearchView searchView = findViewById(R.id.searchView);
        setupSearchView(searchView); //הפונקציה שמנהלת את החיפוש. מאזינה לשינויים בו
        setupItemClickListener(); //אם איבר ברשימה נלחץ

        loadBusinesses(); //העלאת העסקים לרשימה והשמה באמצעות האדפטר בתוך הריסייקל ויו
    }

    //הפונקציה לא מקבלת כלום ומעלה את רשימת העסקים באמצעות האדפטר והריסייקל
    private void loadBusinesses() {
        Log.d(TAG, "Attempting to load businesses from Firestore...");
        //הולך לאוסף העסקים
        db.collection("businesses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullBusinessList.clear(); //מנקה את הרשימה מדברים קודמים
                    /// / מקור מספר 1
                    //לולאה שמוסיפה את העסקים לרשימה מתוך האוסף של businesses
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            //המרת המסמך מהדאטה בייס לאובייקט
                            Business business = document.toObject(Business.class);
                            //הוספת האובייקט לרשימה
                            fullBusinessList.add(business);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting document to Business object", e);
                        }
                    }
                    /// /
                    Log.d(TAG, "Successfully loaded and parsed " + fullBusinessList.size() + " businesses.");
                    //קריאה לאדפטר - הרשימה הושלמה, צייר אותה על המסך
                    businessAdapter.submitList(new ArrayList<>(fullBusinessList));
                })
                //במקרה שלא הצליח להביא צילומי מסך מהפיירסטור
                .addOnFailureListener(e -> {
                    Log.e(TAG, "******************* FIREBASE LOAD FAILED *******************");
                    Log.e(TAG, "Error loading businesses from Firestore", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    //הפונקציה מקבלת את התיבת חיפוש של הXML ומנהלת אותה - מאזינה לשינויים של ומשתמשת בפונקציות אחרות כדי לסנן
    private void setupSearchView(SearchView searchView) {
        // מאזין לתיבת החיפוש
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            //בלי שימוש
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            //פונקצייה מובנית שקורית בכל פעם שמוסיפים או מוחקים אות
            @Override
            public boolean onQueryTextChange(String newText) {
                //קריאה לפילטר
                filter(newText);
                return true;
            }
        });
    }

    // הפונקציה מקבלת את טקסט החיפוש שהוזן באותו רגע ומסננת את מה שלא תואם ברשימה שמוצגת
    private void filter(String text) {
        //רשימה זמנית של כל העסקים שמתאימים למה שכתוב בתיבת החיפוש
        List<Business> filteredList = new ArrayList<>();
        //אם התיבה ריקה, הוספת כל העסקים שברשימה
        if (text.isEmpty()) {
            filteredList.addAll(fullBusinessList);
        } else {
            for (Business item : fullBusinessList) {
                //בדיקה אם העסק לא ריק וגם העסק מכיל את הטקסט שהוזן
                if (item.getBusinessName() != null && item.getBusinessName().toLowerCase().contains(text.toLowerCase())) {
                    //הוספה לרשימה הזמנית
                    filteredList.add(item);
                }
            }
        }
        // עדכון התצוגה רק לרשימה הזמנית באמצעו הפונקציה של האדפטר
        businessAdapter.submitList(filteredList);
    }

    //הפונקציה לא מקבלת דבר ושולחת את המשתמש לדף של העסק שעליו הוא לחץ - פונקציה שיורשים מהאדפטר
    private void setupItemClickListener() {
        //אם המשתמש לחץ על אחד העסקים שברשימה
        businessAdapter.setOnItemClickListener(business -> {
            //עובר למסך פרטי עסק של העסק שנלחץ
            Intent intent = new Intent(SearchBusinessActivity.this, BusinessDetailsActivity.class);
            if (business.getOwnerId() == null) {
                Toast.makeText(this, "Error: Business has no owner ID.", Toast.LENGTH_SHORT).show();
                return;
            }
            //שומרים את הID כדי שהאנדרואיד ידע לאיזה מסך צריך ללכת
            intent.putExtra(BusinessDetailsActivity.EXTRA_BUSINESS_ID, business.getOwnerId());
            startActivity(intent);
        });
    }
}