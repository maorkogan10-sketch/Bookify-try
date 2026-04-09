package com.example.bookify_try;

import java.util.ArrayList;
import java.util.List;

public class WorkingHours {
    private String dayOfWeek;
    private List<TimeSlot> timeSlots;

    public WorkingHours() {
        this.timeSlots = new ArrayList<>();
    }

    public WorkingHours(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
        this.timeSlots = new ArrayList<>();
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public List<TimeSlot> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(List<TimeSlot> timeSlots) {
        this.timeSlots = timeSlots;
    }
}