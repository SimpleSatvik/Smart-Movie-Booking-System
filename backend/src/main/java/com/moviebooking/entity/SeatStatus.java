package com.moviebooking.entity;

public enum SeatStatus {
    AVAILABLE,   // Open for booking
    LOCKED,      // Temporarily held while a user is in the checkout/payment window
    BOOKED       // Confirmed and ticket issued
}
