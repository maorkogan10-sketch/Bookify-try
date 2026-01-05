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

    private RecyclerView businessesRecyclerView;
    private BusinessAdapter businessAdapter;
    private final List<Business> fullBusinessList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_business);

        db = FirebaseFirestore.getInstance();

        businessesRecyclerView = findViewById(R.id.businessesRecyclerView);
        businessesRecyclerView.setHasFixedSize(true);
        businessesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        businessAdapter = new BusinessAdapter();
        businessesRecyclerView.setAdapter(businessAdapter);

        SearchView searchView = findViewById(R.id.searchView);
        setupSearchView(searchView);
        setupItemClickListener();

        loadBusinesses();
    }

    private void loadBusinesses() {
        Log.d(TAG, "Attempting to load businesses from Firestore...");
        db.collection("businesses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullBusinessList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Business business = document.toObject(Business.class);
                            fullBusinessList.add(business);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting document to Business object", e);
                        }
                    }
                    Log.d(TAG, "Successfully loaded and parsed " + fullBusinessList.size() + " businesses.");
                    businessAdapter.submitList(new ArrayList<>(fullBusinessList));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "******************* FIREBASE LOAD FAILED *******************");
                    Log.e(TAG, "Error loading businesses from Firestore", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupSearchView(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    private void filter(String text) {
        List<Business> filteredList = new ArrayList<>();
        if (text.isEmpty()) {
            filteredList.addAll(fullBusinessList);
        } else {
            for (Business item : fullBusinessList) {
                if (item.getBusinessName() != null && item.getBusinessName().toLowerCase().contains(text.toLowerCase())) {
                    filteredList.add(item);
                }
            }
        }
        businessAdapter.submitList(filteredList);
    }

    private void setupItemClickListener() {
        businessAdapter.setOnItemClickListener(business -> {
            Intent intent = new Intent(SearchBusinessActivity.this, BusinessDetailsActivity.class);
            if (business.getOwnerId() == null) {
                Toast.makeText(this, "Error: Business has no owner ID.", Toast.LENGTH_SHORT).show();
                return;
            }
            intent.putExtra(BusinessDetailsActivity.EXTRA_BUSINESS_ID, business.getOwnerId());
            startActivity(intent);
        });
    }
}