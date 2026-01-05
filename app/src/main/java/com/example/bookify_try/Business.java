package com.example.bookify_try;

import java.util.List;

public class Business {
    private String ownerId;
    private String businessName;
    private List<Resource> resources;
    private List<WorkingHours> workingHours; // Changed from Map to List

    // Required empty constructor for Firestore
    public Business() {
    }

    public Business(String ownerId, String businessName, List<Resource> resources, List<WorkingHours> workingHours) {
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

    public List<WorkingHours> getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(List<WorkingHours> workingHours) {
        this.workingHours = workingHours;
    }
}