package com.moviebooking.controller;

import com.moviebooking.dto.BookingRequestDTO;
import com.moviebooking.dto.BookingResponseDTO;
import com.moviebooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows React frontend to call without CORS blocks
public class BookingController {

    private final BookingService bookingService;

    /**
     * POST /api/bookings
     * Create a new booking, process payment, and issue digital ticket
     */
    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@Valid @RequestBody BookingRequestDTO request) {
        BookingResponseDTO response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/bookings/{reference}
     * Look up digital ticket details by reference code (e.g. /api/bookings/TKT-9F8A12BC)
     */
    @GetMapping("/{reference}")
    public ResponseEntity<BookingResponseDTO> getBookingByReference(@PathVariable String reference) {
        return ResponseEntity.ok(bookingService.getBookingByReference(reference));
    }

    /**
     * GET /api/bookings/user/{userId}
     * Look up all digital tickets/passes for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    /**
     * POST /api/bookings/{bookingIdOrRef}/cancel
     * Moviegoer Booking Cancellation:
     * Permits cancellation only if >= 2 hours remain before showtime.
     * Releases seats back to AVAILABLE, cancels active resale listings, and marks booking CANCELLED.
     */
    @PostMapping("/{bookingIdOrRef}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(
            @PathVariable String bookingIdOrRef,
            @RequestAttribute(value = "currentUserGmail", required = false) String currentUserGmail,
            @RequestAttribute(value = "currentUserRole", required = false) String currentUserRole,
            @RequestHeader(value = "X-User-Role", required = false) String xRoleHeader,
            @RequestHeader(value = "X-User-Email", required = false) String xEmailHeader,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserIdHeader) {

        String role = currentUserRole != null ? currentUserRole : xRoleHeader;
        String email = (xEmailHeader != null && !xEmailHeader.isBlank()) ? xEmailHeader : currentUserGmail;
        BookingResponseDTO response = bookingService.cancelBooking(bookingIdOrRef, email, role, xUserIdHeader);
        return ResponseEntity.ok(response);
    }
}
