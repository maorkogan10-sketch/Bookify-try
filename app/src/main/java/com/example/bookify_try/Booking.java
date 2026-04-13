package com.example.bookify_try;

import com.google.firebase.Timestamp;

public class Booking {
    private String bookingId; // הID של המסמך בפיירסטור
    private String businessId;
    private String customerId;
    private String resourceName;
    private Timestamp startTime;
    private Timestamp endTime;

    public Booking() {
    }

    public Booking(String bookingId, String businessId, String customerId, String resourceName, Timestamp startTime, Timestamp endTime) {
        this.bookingId = bookingId;
        this.businessId = businessId;
        this.customerId = customerId;
        this.resourceName = resourceName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
}