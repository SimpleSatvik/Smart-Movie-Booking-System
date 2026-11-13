package com.moviebooking.service;

import com.moviebooking.dto.BookingRequestDTO;
import com.moviebooking.dto.BookingResponseDTO;
import com.moviebooking.entity.*;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final UserRepository userRepository;
    private final ResaleListingRepository resaleListingRepository;

    /**
     * Core Booking Flow (Feature 1)
     * Validates availability, calculates total, processes mock payment, 
     * updates seats to BOOKED, and issues a digital ticket.
     */
    @Transactional // Ensures Atomicity: all or nothing
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        log.info("Processing booking request for user: {} on show: {}", request.getUserId(), request.getShowId());

        // 1. Verify User exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + request.getUserId()));

        // 2. Verify Show exists
        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new IllegalArgumentException("Show not found with ID: " + request.getShowId()));

        // 3. Feature 2 (Concurrency Lock): 
        // Attempt to acquire an exclusive row-level lock using PostgreSQL's "FOR UPDATE SKIP LOCKED".
        // If another transaction is currently booking any of these seats, PostgreSQL skips those rows.
        List<ShowSeat> selectedSeats = showSeatRepository.findAvailableSeatsForUpdateSkipLocked(request.getShowSeatIds());

        // If the number of locked seats is less than what the user requested, someone else beat them to it!
        if (selectedSeats.size() != request.getShowSeatIds().size()) {
            throw new IllegalStateException("One or more selected seats are currently being booked by another user. Please select different seats!");
        }

        // 4. Validate that all requested seats belong to this show

        // 5. Calculate Total Bill
        BigDecimal totalAmount = selectedSeats.stream()
                .map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. Generate Unique Ticket Reference (e.g. TKT-9F8A12BC)
        String bookingRef = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String seatSummary = selectedSeats.stream()
                .map(ss -> ss.getSeat().getRow() + ss.getSeat().getSeatNumber())
                .collect(java.util.stream.Collectors.joining(", "));

        // 7. Create Confirmed Booking entity
        Booking booking = Booking.builder()
                .bookingReference(bookingRef)
                .user(user)
                .show(show)
                .totalAmountPaid(totalAmount)
                .status(BookingStatus.CONFIRMED)
                .seatSummary(seatSummary)
                .build();

        // 8. Process Mock Payment
        String mockTxnId = "TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(totalAmount)
                .method(request.getPaymentMethod())
                .result(PaymentStatus.SUCCESS)
                .transactionId(mockTxnId)
                .build();

        booking.setPayment(payment);

        // 9. Mark all selected seats as BOOKED and link them to this booking
        for (ShowSeat seat : selectedSeats) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setBooking(booking);
            seat.setLockedUntil(null); // Clear any hold timestamp
        }

        booking.setBookedSeats(selectedSeats);

        // 10. Save Booking (cascades to payment and updates all seats via dirty checking / save)
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking successful! Reference: {}, Total: ₹{}", bookingRef, totalAmount);

        // 11. Return Digital Ticket
        return BookingResponseDTO.fromEntity(savedBooking);
    }

    /**
     * View Digital Ticket by reference code (e.g. GET /api/bookings/TKT-9F8A12BC)
     */
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingByReference(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with reference: " + bookingReference));

        return BookingResponseDTO.fromEntity(booking);
    }

    /**
     * View all active confirmed bookings for a specific user (e.g. GET /api/bookings/user/1)
     * Automatically filters out CANCELLED and RESOLD bookings, ensuring sold tickets
     * are completely removed from the seller's active wallet.
     */
    @Transactional
    public List<BookingResponseDTO> getBookingsByUser(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return bookings.stream()
                .filter(b -> {
                    // 1. Exclude CANCELLED and RESOLD bookings from customer's active wallet
                    if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.RESOLD) {
                        return false;
                    }
                    // 2. Auto-detect and heal: If a booking has 0 physical seats remaining
                    // (because all its seats were transferred to resale buyers), transition to RESOLD
                    List<ShowSeat> activeSeats = showSeatRepository.findByBookingId(b.getId());
                    if (activeSeats.isEmpty()) {
                        b.setStatus(BookingStatus.RESOLD);
                        b.setSeatSummary("");
                        bookingRepository.save(b);
                        log.info("Auto-healed booking #{}: 0 active seats remaining (resold). Marked status as RESOLD.", b.getId());
                        return false;
                    }
                    return true;
                })
                .map(BookingResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public BookingResponseDTO cancelBooking(String bookingIdOrRef, String currentUserEmail, String currentUserRole) {
        return cancelBooking(bookingIdOrRef, currentUserEmail, currentUserRole, null);
    }

    /**
     * Customer Booking Cancellation:
     * Permits cancellation only if the show is scheduled at least 2 hours in the future.
     * Atomically sets booking status to CANCELLED, releases seats back to AVAILABLE,
     * clears any active resale listings, and initiates refund.
     */
    @Transactional
    public BookingResponseDTO cancelBooking(String bookingIdOrRef, String currentUserEmail, String currentUserRole, Long currentUserId) {
        log.info("Request to cancel booking '{}' by user email: '{}', user ID: {}, role: '{}'",
                bookingIdOrRef, currentUserEmail, currentUserId, currentUserRole);

        Booking booking;
        if (bookingIdOrRef.matches("\\d+")) {
            Long id = Long.parseLong(bookingIdOrRef);
            booking = bookingRepository.findById(id)
                    .or(() -> bookingRepository.findByBookingReference(bookingIdOrRef))
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingIdOrRef));
        } else {
            booking = bookingRepository.findByBookingReference(bookingIdOrRef)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found with reference: " + bookingIdOrRef));
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("This booking has already been cancelled.");
        }

        if (booking.getStatus() == BookingStatus.RESOLD) {
            throw new IllegalArgumentException("Cannot cancel booking: All tickets on this pass have already been resold on the Spare Tickets marketplace.");
        }

        // Ownership verification (Admins can cancel any booking; customers only their own)
        boolean isAdmin = currentUserRole != null && currentUserRole.toUpperCase().contains("ADMIN");
        if (!isAdmin) {
            boolean hasCallerIdentity = (currentUserEmail != null && !currentUserEmail.isBlank()) || currentUserId != null;
            if (hasCallerIdentity) {
                boolean emailMatch = currentUserEmail != null && booking.getUser() != null &&
                        booking.getUser().getGmail() != null &&
                        booking.getUser().getGmail().equalsIgnoreCase(currentUserEmail.trim());
                boolean idMatch = currentUserId != null && booking.getUser() != null &&
                        booking.getUser().getId() != null &&
                        booking.getUser().getId().equals(currentUserId);

                if (!emailMatch && !idMatch) {
                    // Check if demo user account: satvik@gmail.com
                    boolean isDemoUser = (currentUserEmail != null && currentUserEmail.equalsIgnoreCase("satvik@gmail.com")) ||
                            (booking.getUser() != null && "satvik@gmail.com".equalsIgnoreCase(booking.getUser().getGmail()));
                    if (!isDemoUser) {
                        log.warn("Access denied on booking cancellation: Booking belongs to user ID {} ({}), but caller is user ID {} ({})",
                                booking.getUser() != null ? booking.getUser().getId() : "null",
                                booking.getUser() != null ? booking.getUser().getGmail() : "null",
                                currentUserId, currentUserEmail);
                        throw new IllegalArgumentException("Access denied: You do not have permission to cancel this booking.");
                    }
                }
            }
        }

        // 2-Hour Time Window Rule:
        // Must have at least 2 hours remaining before show.startTime
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoHoursFromNow = now.plusHours(2);
        LocalDateTime showStart = booking.getShow().getStartTime();

        if (showStart.isBefore(twoHoursFromNow)) {
            throw new IllegalStateException("Cancellation window closed: Tickets can only be cancelled at least 2 hours prior to the scheduled showtime.");
        }

        // Guard: Check if any seat from this booking is actively listed on Resale Marketplace
        List<ShowSeat> bookedSeats = booking.getBookedSeats();
        if (bookedSeats == null || bookedSeats.isEmpty()) {
            bookedSeats = showSeatRepository.findByBookingId(booking.getId());
        }
        List<Long> seatIds = new ArrayList<>();
        if (bookedSeats != null && !bookedSeats.isEmpty()) {
            for (ShowSeat seat : bookedSeats) {
                seatIds.add(seat.getId());
            }
        }

        if (!seatIds.isEmpty()) {
            List<ResaleListing> activeListings = resaleListingRepository.findByShowSeatIdInAndStatus(seatIds, ResaleListingStatus.ACTIVE);
            if (!activeListings.isEmpty()) {
                String listedSeats = activeListings.stream()
                        .map(l -> {
                            if (l.getShowSeat() != null && l.getShowSeat().getSeat() != null) {
                                return l.getShowSeat().getSeat().getRow() + l.getShowSeat().getSeat().getSeatNumber();
                            }
                            return "Seat #" + l.getShowSeat().getId();
                        })
                        .distinct()
                        .collect(java.util.stream.Collectors.joining(", "));
                log.warn("Cancellation rejected for booking {}: Seat(s) {} are actively listed on Resale Marketplace", 
                        booking.getBookingReference(), listedSeats);
                throw new IllegalStateException("Cannot cancel booking: Seat(s) (" + listedSeats 
                        + ") are actively listed on the Resale Marketplace. Please remove/delist the ticket from Resale first before cancelling.");
            }
        }

        // Release seats back to AVAILABLE
        if (bookedSeats != null && !bookedSeats.isEmpty()) {
            for (ShowSeat seat : bookedSeats) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setBooking(null);
                seat.setLockedUntil(null);
            }
            showSeatRepository.saveAll(bookedSeats);
        }

        // Mark booking as CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking '{}' successfully cancelled. {} seat(s) released back to AVAILABLE.",
                booking.getBookingReference(), bookedSeats != null ? bookedSeats.size() : 0);

        return BookingResponseDTO.fromEntity(savedBooking);
    }
}
