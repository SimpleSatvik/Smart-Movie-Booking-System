package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seat_for_a_show", uniqueConstraints = {
    // One physical seat can only have one status record per show
    @UniqueConstraint(columnNames = {"show_id", "seat_id"})
}, indexes = {
    // Accelerates live seat map loading & concurrent lock lookups
    @Index(name = "idx_show_seat_show_status", columnList = "show_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sfas_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // e.g. 250.00

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil; // e.g. current_time + 10 mins

    // Helper method to check if a seat is currently open for booking
    public boolean isAvailableForBooking() {
        if (this.status == SeatStatus.AVAILABLE) {
            return true;
        }
        // If it was temporarily locked, but the lock has expired, treat it as available!
        if (this.status == SeatStatus.LOCKED && this.lockedUntil != null && this.lockedUntil.isBefore(LocalDateTime.now())) {
            return true;
        }
        return false;
    }
}
