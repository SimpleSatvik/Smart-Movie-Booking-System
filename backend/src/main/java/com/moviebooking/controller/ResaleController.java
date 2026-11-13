package com.moviebooking.controller;

import com.moviebooking.dto.CreateResaleListingRequestDTO;
import com.moviebooking.dto.ResaleListingResponseDTO;
import com.moviebooking.service.ResaleMarketplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resale")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows React frontend to connect without CORS issues
public class ResaleController {

    private final ResaleMarketplaceService resaleMarketplaceService;

    /**
     * 1. POST /api/resale/listings
     * Put a booked seat up for sale on the public marketplace.
     */
    @PostMapping("/listings")
    public ResponseEntity<ResaleListingResponseDTO> listTicket(@Valid @RequestBody CreateResaleListingRequestDTO request) {
        ResaleListingResponseDTO response = resaleMarketplaceService.listTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. GET /api/resale/listings
     * Browse all active resale tickets. Optionally filter by showId: /api/resale/listings?showId=1
     */
    @GetMapping("/listings")
    public ResponseEntity<List<ResaleListingResponseDTO>> getActiveListings(@RequestParam(required = false) Long showId) {
        return ResponseEntity.ok(resaleMarketplaceService.getActiveListings(showId));
    }

    /**
     * 3. POST /api/resale/listings/{id}/buy?buyerUserId=2
     * Purchase a resale ticket (concurrency-safe via row-level locks).
     */
    @PostMapping("/listings/{id}/buy")
    public ResponseEntity<ResaleListingResponseDTO> buyResaleTicket(
            @PathVariable Long id,
            @RequestParam Long buyerUserId) {
        return ResponseEntity.ok(resaleMarketplaceService.buyResaleTicket(id, buyerUserId));
    }

    /**
     * 4. DELETE /api/resale/listings/{id}?sellerUserId=1
     * Cancel an active listing before anyone buys it.
     */
    @DeleteMapping("/listings/{id}")
    public ResponseEntity<ResaleListingResponseDTO> cancelListing(
            @PathVariable Long id,
            @RequestParam Long sellerUserId) {
        return ResponseEntity.ok(resaleMarketplaceService.cancelListing(id, sellerUserId));
    }

    /**
     * 5. GET /api/resale/my-listings?sellerUserId=1
     * View all tickets posted for resale by a specific seller.
     */
    @GetMapping("/my-listings")
    public ResponseEntity<List<ResaleListingResponseDTO>> getMyListings(@RequestParam Long sellerUserId) {
        return ResponseEntity.ok(resaleMarketplaceService.getMyListings(sellerUserId));
    }
}
