package com.moviebooking.service;

import com.moviebooking.dto.CreateResaleListingRequestDTO;
import com.moviebooking.dto.ResaleListingResponseDTO;
import com.moviebooking.entity.*;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResaleMarketplaceService {

    private final ResaleListingRepository resaleListingRepository;
    private final ShowSeatRepository showSeatRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    /**
     * 1. List a booked ticket for resale on the public marketplace.
     */
    @Transactional
    public ResaleListingResponseDTO listTicket(CreateResaleListingRequestDTO request) {
        log.info("User {} is attempting to list seat {} for resale at ₹{}", 
                request.getSellerUserId(), request.getShowSeatId(), request.getAskingPrice());

        // Validate seller exists
        User seller = userRepository.findById(request.getSellerUserId())
                .orElseThrow(() -> new IllegalArgumentException("Seller not found with ID: " + request.getSellerUserId()));

        // Validate seat exists
        ShowSeat showSeat = showSeatRepository.findById(request.getShowSeatId())
                .orElseThrow(() -> new IllegalArgumentException("Seat not found with ID: " + request.getShowSeatId()));

        // Rule 1: Seat must be currently BOOKED
        if (showSeat.getStatus() != SeatStatus.BOOKED || showSeat.getBooking() == null) {
            throw new IllegalStateException("Only confirmed, booked seats can be put up for resale.");
        }

        // Rule 2: The seller must be the actual owner of the booking
        if (!showSeat.getBooking().getUser().getId().equals(seller.getId())) {
            throw new IllegalArgumentException("You cannot resell a ticket that does not belong to you.");
        }

        // Rule 3: The movie show must not have already started
        if (showSeat.getShow().getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot resell a ticket for a show that has already started or completed.");
        }

        // Rule 4: Seat cannot already have an ACTIVE listing
        boolean alreadyListed = resaleListingRepository.existsByShowSeatIdAndStatus(showSeat.getId(), ResaleListingStatus.ACTIVE);
        if (alreadyListed) {
            throw new IllegalStateException("This seat is already actively listed on the marketplace.");
        }

        // Save the listing (protected by PostgreSQL partial unique index against race conditions)
        ResaleListing listing = ResaleListing.builder()
                .showSeat(showSeat)
                .seller(seller)
                .askingPrice(request.getAskingPrice())
                .status(ResaleListingStatus.ACTIVE)
                .build();

        try {
            ResaleListing saved = resaleListingRepository.saveAndFlush(listing);
            log.info("Resale listing #{} successfully created for seat {}", saved.getId(), showSeat.getId());
            return mapToResponseDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent listing race condition prevented by database index for seat {}", showSeat.getId());
            throw new IllegalStateException("This seat is already actively listed on the marketplace.");
        }
    }

    /**
     * 2. Browse all active resale listings (optionally filtered by show).
     */
    @Transactional(readOnly = true)
    public List<ResaleListingResponseDTO> getActiveListings(Long showId) {
        LocalDateTime now = LocalDateTime.now();
        List<ResaleListing> listings;
        if (showId != null) {
            listings = resaleListingRepository.findUpcomingByShowIdAndStatus(showId, ResaleListingStatus.ACTIVE, now);
        } else {
            listings = resaleListingRepository.findUpcomingByStatus(ResaleListingStatus.ACTIVE, now);
        }

        return listings.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * 3. Purchase a resale ticket with concurrency locking.
     */
    @Transactional
    public ResaleListingResponseDTO buyResaleTicket(Long listingId, Long buyerUserId) {
        log.info("Buyer {} is attempting to purchase resale listing #{}", buyerUserId, listingId);

        // Validate buyer exists
        User buyer = userRepository.findById(buyerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found with ID: " + buyerUserId));

        // Concurrency lock #1 (Listing):
        // If another buyer is processing this listing at this exact millisecond, this returns empty.
        ResaleListing listing = resaleListingRepository.findActiveListingForUpdateSkipLocked(listingId)
                .orElseThrow(() -> new IllegalStateException("This resale ticket is no longer available. Another user may have just purchased it!"));

        // Rule: A seller cannot buy their own ticket
        if (listing.getSeller().getId().equals(buyer.getId())) {
            throw new IllegalArgumentException("You cannot buy a ticket that you listed for resale.");
        }

        // Concurrency lock #2 (Physical Seat):
        // Lock the ShowSeat row via @Lock(LockModeType.PESSIMISTIC_WRITE)
        ShowSeat showSeat = showSeatRepository.findByIdWithLock(listing.getShowSeat().getId())
                .orElseThrow(() -> new IllegalStateException("Seat record not found for ID: " + listing.getShowSeat().getId()));

        Booking sellerBooking = showSeat.getBooking();

        // Transfer Ownership:
        // 1. Create a brand-new canonical booking record for the buyer
        String newBookingRef = "TKT-RESALE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String seatCode = (showSeat.getSeat() != null && showSeat.getSeat().getRow() != null && showSeat.getSeat().getSeatNumber() != null)
                ? showSeat.getSeat().getRow() + showSeat.getSeat().getSeatNumber()
                : (showSeat.getSeat() != null ? String.valueOf(showSeat.getSeat().getSeatNumber()) : "Seat");

        Booking buyerBooking = Booking.builder()
                .bookingReference(newBookingRef)
                .user(buyer)
                .show(showSeat.getShow())
                .totalAmountPaid(listing.getAskingPrice())
                .status(BookingStatus.CONFIRMED)
                .seatSummary(seatCode)
                .build();
        Booking savedBooking = bookingRepository.save(buyerBooking);

        // 2. Re-assign the physical seat to the buyer's booking
        showSeat.setBooking(savedBooking);
        showSeatRepository.saveAndFlush(showSeat);

        // 3. Atomically update the seller's original booking status
        if (sellerBooking != null) {
            List<ShowSeat> remainingSellerSeats = showSeatRepository.findByBookingId(sellerBooking.getId())
                    .stream()
                    .filter(ss -> !ss.getId().equals(showSeat.getId()))
                    .toList();

            if (remainingSellerSeats.isEmpty()) {
                sellerBooking.setStatus(BookingStatus.RESOLD);
                sellerBooking.setSeatSummary("");
                bookingRepository.saveAndFlush(sellerBooking);
                log.info("All seats on seller booking #{} were resold. Updated booking status to RESOLD.", sellerBooking.getId());
            } else {
                String remainingSummary = remainingSellerSeats.stream()
                        .map(ss -> ss.getSeat().getRow() + ss.getSeat().getSeatNumber())
                        .collect(Collectors.joining(", "));
                sellerBooking.setSeatSummary(remainingSummary);
                bookingRepository.saveAndFlush(sellerBooking);
                log.info("Seller booking #{} still has {} active seat(s): {}", sellerBooking.getId(), remainingSellerSeats.size(), remainingSummary);
            }
        }

        // 4. Mark the resale listing as SOLD
        listing.setBuyer(buyer);
        listing.setStatus(ResaleListingStatus.SOLD);
        listing.setSoldAt(LocalDateTime.now());
        ResaleListing updatedListing = resaleListingRepository.saveAndFlush(listing);

        log.info("Resale listing #{} purchased by User {}. New booking reference: {}", 
                listingId, buyer.getId(), newBookingRef);

        return mapToResponseDTO(updatedListing);
    }

    /**
     * 4. Cancel an active resale listing (only the seller can do this).
     * Uses pessimistic write lock to prevent racing against an in-progress purchase.
     */
    @Transactional
    public ResaleListingResponseDTO cancelListing(Long listingId, Long sellerUserId) {
        // Concurrency lock: acquires an exclusive write lock on the listing
        ResaleListing listing = resaleListingRepository.findByIdWithLock(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with ID: " + listingId));

        if (!listing.getSeller().getId().equals(sellerUserId)) {
            throw new IllegalArgumentException("You are not authorized to cancel this listing.");
        }

        if (listing.getStatus() != ResaleListingStatus.ACTIVE) {
            throw new IllegalStateException("Only active listings can be cancelled. Current status: " + listing.getStatus());
        }

        listing.setStatus(ResaleListingStatus.CANCELLED);
        ResaleListing saved = resaleListingRepository.save(listing);
        log.info("Resale listing #{} cancelled by seller {}", listingId, sellerUserId);
        return mapToResponseDTO(saved);
    }

    /**
     * 5. View all listings posted by a specific seller.
     */
    @Transactional(readOnly = true)
    public List<ResaleListingResponseDTO> getMyListings(Long sellerUserId) {
        return resaleListingRepository.findBySellerId(sellerUserId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Helper method: converts a ResaleListing entity into a rich ResaleListingResponseDTO.
     */
    private ResaleListingResponseDTO mapToResponseDTO(ResaleListing listing) {
        ShowSeat seat = listing.getShowSeat();
        String seatCode = (seat.getSeat() != null && seat.getSeat().getRow() != null && seat.getSeat().getSeatNumber() != null)
                ? seat.getSeat().getRow() + seat.getSeat().getSeatNumber()
                : (seat.getSeat() != null && seat.getSeat().getSeatNumber() != null ? String.valueOf(seat.getSeat().getSeatNumber()) : "A1");
        String bookingRef = (seat.getBooking() != null) ? seat.getBooking().getBookingReference() : null;

        return ResaleListingResponseDTO.builder()
                .listingId(listing.getId())
                .showSeatId(seat.getId())
                .movieTitle(seat.getShow().getMovie().getTitle())
                .theatreName(seat.getShow().getScreen().getTheatre().getName())
                .screenName(seat.getShow().getScreen().getName())
                .showStartTime(seat.getShow().getStartTime())
                .rowName(seat.getSeat() != null ? seat.getSeat().getRow() : null)
                .seatNumber(seat.getSeat() != null ? seat.getSeat().getSeatNumber() : null)
                .seatCode(seatCode)
                .bookingReference(bookingRef)
                .seatCategory(seat.getSeat() != null && seat.getSeat().getCategory() != null ? seat.getSeat().getCategory().name() : null)
                .originalPrice(seat.getPrice())
                .askingPrice(listing.getAskingPrice())
                .sellerId(listing.getSeller().getId())
                .sellerName(listing.getSeller().getName())
                .buyerId(listing.getBuyer() != null ? listing.getBuyer().getId() : null)
                .buyerName(listing.getBuyer() != null ? listing.getBuyer().getName() : null)
                .status(listing.getStatus())
                .listedAt(listing.getListedAt())
                .soldAt(listing.getSoldAt())
                .build();
    }
}
