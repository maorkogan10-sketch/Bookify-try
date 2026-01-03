package com.example.bookify_try;

import java.util.List;
import java.util.Map;

public class Business {
    private String ownerId;
    private String businessName;
    private List<Resource> resources;
    private Map<String, WorkingHours> workingHours;

    // Required empty constructor for Firestore
    public Business() {
    }

    public Business(String ownerId, String businessName, List<Resource> resources, Map<String, WorkingHours> workingHours) {
        this.ownerId = ownerId;
        this.businessName = businessName;
        this.resources = resources;
        this.workingHours = workingHours;
    }

    // Getters and Setters
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public Map<String, WorkingHours> getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(Map<String, WorkingHours> workingHours) {
        this.workingHours = workingHours;
    }
}