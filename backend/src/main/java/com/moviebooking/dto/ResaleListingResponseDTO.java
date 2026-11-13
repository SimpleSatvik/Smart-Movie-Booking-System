package com.moviebooking.dto;

import com.moviebooking.entity.ResaleListingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResaleListingResponseDTO {

    private Long listingId;
    private Long showSeatId;

    // Rich movie and show details so frontend doesn't need extra API calls
    private String movieTitle;
    private String theatreName;
    private String screenName;
    private LocalDateTime showStartTime;

    // Physical seat details
    private String rowName;
    private Integer seatNumber;
    private String seatCode;
    private String bookingReference;
    private String seatCategory;

    // Pricing
    private BigDecimal originalPrice;
    private BigDecimal askingPrice;

    // User details
    private Long sellerId;
    private String sellerName;
    private Long buyerId;
    private String buyerName;

    // Status and timestamps
    private ResaleListingStatus status;
    private LocalDateTime listedAt;
    private LocalDateTime soldAt;
}
