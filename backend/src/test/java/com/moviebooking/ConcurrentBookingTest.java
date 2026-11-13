package com.moviebooking;

import com.moviebooking.dto.BookingRequestDTO;
import com.moviebooking.dto.BookingResponseDTO;
import com.moviebooking.entity.PaymentMethod;
import com.moviebooking.entity.SeatStatus;
import com.moviebooking.entity.ShowSeat;
import com.moviebooking.repository.ShowSeatRepository;
import com.moviebooking.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ConcurrentBookingTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Test
    @DisplayName("Feature 2: 10 concurrent threads try to book the exact same seat simultaneously")
    void testConcurrentBookingForSameSeat() throws InterruptedException {
        // 1. Pick an AVAILABLE seat dynamically (or reset Seat 3 if all are booked)
        ShowSeat seat = showSeatRepository.findByShowIdAndStatus(1L, SeatStatus.AVAILABLE).stream()
                .findFirst()
                .orElseGet(() -> {
                    ShowSeat s = showSeatRepository.findById(3L).orElseThrow();
                    s.setStatus(SeatStatus.AVAILABLE);
                    s.setBooking(null);
                    return showSeatRepository.save(s);
                });
        Long targetSeatId = seat.getId();

        int numberOfConcurrentUsers = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentUsers);

        // CountDownLatch acting as a starting pistol so all 10 threads fire at the exact same millisecond
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(numberOfConcurrentUsers);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 2. Launch 10 simultaneous threads
        for (int i = 0; i < numberOfConcurrentUsers; i++) {
            executor.submit(() -> {
                try {
                    // Wait for the starting gun so all threads hit the database together
                    startGun.await();

                    BookingRequestDTO request = BookingRequestDTO.builder()
                            .userId(1L)
                            .showId(1L)
                            .showSeatIds(List.of(targetSeatId))
                            .paymentMethod(PaymentMethod.MOCK_UPI)
                            .build();

                    BookingResponseDTO response = bookingService.createBooking(request);
                    if (response != null && response.getBookingReference() != null) {
                        successCount.incrementAndGet();
                        System.out.println("✅ [SUCCESS] Thread booked seat with ref: " + response.getBookingReference());
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("❌ [BLOCKED] Thread caught expected concurrency rejection: " + e.getMessage());
                } finally {
                    finishLine.countDown();
                }
            });
        }

        // 3. Fire the starting gun!
        System.out.println("🔫 FIRING STARTING GUN: 10 threads competing for Seat A3...");
        startGun.countDown();

        // Wait for all 10 threads to finish
        finishLine.await();
        executor.shutdown();

        // 4. Verify Concurrency Guarantees:
        System.out.println("\n========== CONCURRENCY TEST SUMMARY ==========");
        System.out.println("Total simultaneous attempts: " + numberOfConcurrentUsers);
        System.out.println("Successful bookings:        " + successCount.get());
        System.out.println("Blocked / Rejected attempts: " + failureCount.get());
        System.out.println("==============================================\n");

        // EXACTLY 1 thread must succeed
        assertEquals(1, successCount.get(), "Only 1 user should successfully book the seat!");

        // EXACTLY 9 threads must be rejected
        assertEquals(numberOfConcurrentUsers - 1, failureCount.get(), "All other 9 users must be blocked!");

        // Verify database state: Seat A3 must be marked BOOKED
        ShowSeat finalSeatState = showSeatRepository.findById(targetSeatId).orElseThrow();
        assertEquals(SeatStatus.BOOKED, finalSeatState.getStatus(), "Seat A3 in database must be marked BOOKED");
    }
}
