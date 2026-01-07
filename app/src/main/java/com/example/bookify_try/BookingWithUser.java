package com.example.bookify_try;

public class BookingWithUser {
    private Booking booking;
    private User user;

    public BookingWithUser(Booking booking, User user) {
        this.booking = booking;
        this.user = user;
    }

    public Booking getBooking() {
        return booking;
    }

    public User getUser() {
        return user;
    }
}