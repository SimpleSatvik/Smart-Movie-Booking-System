package com.moviebooking.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetRecommendationResponseDTO {

    // e.g. "BEST_EXPERIENCE" (highest tier within budget) or "MAX_SAVINGS" (cheapest option)
    private String recommendationType;

    // Movie and Show details
    private Long movieId;
    private String movieTitle;
    private String genre;
    private String language;
    private String theatreName;
    private String screenName;
    private Long showId;
    private LocalDateTime showStartTime;

    // Seat and Pricing details
    private String seatCategory;      // SILVER, GOLD, PLATINUM
    private BigDecimal pricePerSeat;  // e.g. 200.00
    private Integer numberOfSeats;    // e.g. 2
    private BigDecimal totalCost;     // e.g. 400.00
    private BigDecimal remainingBudget; // e.g. 100.00 saved from their budget
    private Integer availableSeats;   // Count of free seats in this category
}
