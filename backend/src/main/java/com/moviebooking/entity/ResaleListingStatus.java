package com.moviebooking.entity;

public enum ResaleListingStatus {
    ACTIVE,     // Ticket is currently listed on the marketplace and available for anyone to buy
    SOLD,       // Another user purchased this ticket; ownership has been transferred
    CANCELLED   // The original seller decided not to sell and pulled the listing down
}
