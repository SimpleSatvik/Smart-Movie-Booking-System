package com.moviebooking;

import com.moviebooking.dto.BookingRequestDTO;
import com.moviebooking.dto.BookingResponseDTO;
import com.moviebooking.dto.CreateResaleListingRequestDTO;
import com.moviebooking.dto.ResaleListingResponseDTO;
import com.moviebooking.entity.PaymentMethod;
import com.moviebooking.entity.ResaleListingStatus;
import com.moviebooking.entity.SeatStatus;
import com.moviebooking.entity.User;
import com.moviebooking.repository.UserRepository;
import com.moviebooking.service.BookingService;
import com.moviebooking.service.ResaleMarketplaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ResaleMarketplaceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ResaleMarketplaceService resaleMarketplaceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.moviebooking.repository.ShowSeatRepository showSeatRepository;

    @Autowired
    private com.moviebooking.repository.ShowRepository showRepository;

    private User seller;
    private User buyer;

    @BeforeEach
    void setupUsers() {
        // Ensure we have at least 2 users for our tests
        seller = userRepository.findById(1L).orElseGet(() ->
                userRepository.save(User.builder()
                        .name("Satvik Seller")
                        .gmail("seller@example.com")
                        .password("pass123")
                        .role("ROLE_USER")
                        .isDeleted(false)
                        .build())
        );

        buyer = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(seller.getId()))
                .findFirst()
                .orElseGet(() ->
                        userRepository.save(User.builder()
                                .name("Priya Buyer")
                                .gmail("priya_buyer@example.com")
                                .password("pass123")
                                .role("ROLE_USER")
                                .isDeleted(false)
                                .build())
                );

        // Ensure Show 1 is scheduled for the future so resale validation passes
        showRepository.findById(1L).ifPresent(show -> {
            if (show.getStartTime().isBefore(java.time.LocalDateTime.now())) {
                show.setStartTime(java.time.LocalDateTime.now().plusDays(2));
                show.setEndTime(java.time.LocalDateTime.now().plusDays(2).plusHours(3));
                showRepository.save(show);
            }
        });
    }

    @Test
    @DisplayName("Feature 4: Full Resale Lifecycle (Book -> List -> Browse -> Buy -> Verify Ownership)")
    void testResaleLifecycle() {
        // Step 1: Find an available seat dynamically
        List<com.moviebooking.entity.ShowSeat> availableSeats = showSeatRepository.findByShowIdAndStatus(1L, SeatStatus.AVAILABLE);
        assertFalse(availableSeats.isEmpty(), "Must have available seats for testing");
        com.moviebooking.entity.ShowSeat targetSeat = availableSeats.get(0);
        Long seatId = targetSeat.getId();
        Long showId = targetSeat.getShow().getId();

        BookingRequestDTO bookingRequest = BookingRequestDTO.builder()
                .userId(seller.getId())
                .showId(showId)
                .showSeatIds(List.of(seatId))
                .paymentMethod(PaymentMethod.MOCK_UPI)
                .build();

        BookingResponseDTO initialBooking = bookingService.createBooking(bookingRequest);
        assertNotNull(initialBooking.getBookingReference());

        // Step 2: Seller lists the booked seat on the Resale Marketplace for ₹199.00
        CreateResaleListingRequestDTO listRequest = CreateResaleListingRequestDTO.builder()
                .sellerUserId(seller.getId())
                .showSeatId(seatId)
                .askingPrice(new BigDecimal("199.00"))
                .build();

        ResaleListingResponseDTO listing = resaleMarketplaceService.listTicket(listRequest);
        assertNotNull(listing.getListingId());
        assertEquals(ResaleListingStatus.ACTIVE, listing.getStatus());
        assertEquals(new BigDecimal("199.00"), listing.getAskingPrice());

        // Step 3: Browse active listings and verify our ticket appears
        List<ResaleListingResponseDTO> activeListings = resaleMarketplaceService.getActiveListings(showId);
        boolean foundInMarketplace = activeListings.stream()
                .anyMatch(l -> l.getListingId().equals(listing.getListingId()));
        assertTrue(foundInMarketplace, "Listed ticket must appear in active marketplace browse results");

        // Step 4: Buyer purchases the ticket
        ResaleListingResponseDTO purchased = resaleMarketplaceService.buyResaleTicket(listing.getListingId(), buyer.getId());
        assertEquals(ResaleListingStatus.SOLD, purchased.getStatus());
        assertEquals(buyer.getId(), purchased.getBuyerId());
        assertNotNull(purchased.getSoldAt());

        // Step 5: Verify that trying to buy it again fails
        assertThrows(IllegalStateException.class, () ->
                resaleMarketplaceService.buyResaleTicket(listing.getListingId(), buyer.getId())
        );
    }

    @Test
    @DisplayName("Feature 4: Concurrency Test - 10 buyers simultaneously attempt to purchase the same resale ticket")
    void testConcurrentResalePurchase() throws InterruptedException {
        // Step 1: Find an available seat dynamically
        List<com.moviebooking.entity.ShowSeat> availableSeats = showSeatRepository.findByShowIdAndStatus(1L, SeatStatus.AVAILABLE);
        assertFalse(availableSeats.isEmpty(), "Must have available seats for testing");
        com.moviebooking.entity.ShowSeat targetSeat = availableSeats.get(0);
        Long seatId = targetSeat.getId();
        Long showId = targetSeat.getShow().getId();

        BookingRequestDTO bookingRequest = BookingRequestDTO.builder()
                .userId(seller.getId())
                .showId(showId)
                .showSeatIds(List.of(seatId))
                .paymentMethod(PaymentMethod.MOCK_CARD)
                .build();

        bookingService.createBooking(bookingRequest);

        // Step 2: List the seat on Resale Marketplace
        CreateResaleListingRequestDTO listRequest = CreateResaleListingRequestDTO.builder()
                .sellerUserId(seller.getId())
                .showSeatId(seatId)
                .askingPrice(new BigDecimal("180.00"))
                .build();

        ResaleListingResponseDTO listing = resaleMarketplaceService.listTicket(listRequest);
        Long listingId = listing.getListingId();

        // Step 3: Launch 10 simultaneous threads competing for this exact resale listing
        int numberOfConcurrentBuyers = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentBuyers);

        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(numberOfConcurrentBuyers);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfConcurrentBuyers; i++) {
            executor.submit(() -> {
                try {
                    // Wait for starting gun so all threads hit database simultaneously
                    startGun.await();
                    resaleMarketplaceService.buyResaleTicket(listingId, buyer.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        // Fire the gun!
        startGun.countDown();

        // Wait for all 10 threads to cross the finish line
        finishLine.await();
        executor.shutdown();

        // Verify: EXACTLY 1 buyer succeeds, EXACTLY 9 buyers fail
        assertEquals(1, successCount.get(), "Exactly ONE buyer must successfully purchase the resale ticket");
        assertEquals(9, failureCount.get(), "Exactly NINE buyers must be rejected due to row-level locks");
    }
}
