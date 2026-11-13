package com.moviebooking.repository;

import com.moviebooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Fetch booking by digital ticket code (e.g. "TKT-A92F1X")
    Optional<Booking> findByBookingReference(String bookingReference);

    // Fetch all bookings made by a specific user (booking history)
    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Aggregate platform revenue across all confirmed bookings
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(b.totalAmountPaid), 0) FROM Booking b WHERE b.status = com.moviebooking.entity.BookingStatus.CONFIRMED")
    java.math.BigDecimal calculateTotalPlatformRevenue();
}
