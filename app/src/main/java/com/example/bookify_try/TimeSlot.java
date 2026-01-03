package com.example.bookify_try;

public class TimeSlot {
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;

    // Required empty constructor for Firestore
    public TimeSlot() {
    }

    public TimeSlot(int startHour, int startMinute, int endHour, int endMinute) {
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
    }

    // Getters and Setters
    public int getStartHour() {
        return startHour;
    }

    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getEndHour() {
        return endHour;
    }

    public void setEndHour(int endHour) {
        this.endHour = endHour;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(int endMinute) {
        this.endMinute = endMinute;
    }

    @Override
    public String toString() {
        return String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute);
    }
}