package com.moviebooking;

import com.moviebooking.dto.BudgetOptimizationRequestDTO;
import com.moviebooking.dto.BudgetRecommendationResponseDTO;
import com.moviebooking.entity.Show;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.service.BudgetOptimizerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BudgetOptimizerTest {

    @Autowired
    private BudgetOptimizerService budgetOptimizerService;

    @Autowired
    private ShowRepository showRepository;

    @BeforeEach
    void ensureFutureShowsExist() {
        // Ensure at least Show 1 and Show 2 are scheduled for the future
        List<Show> shows = showRepository.findAll();
        for (Show show : shows) {
            if (show.getStartTime().isBefore(LocalDateTime.now())) {
                show.setStartTime(LocalDateTime.now().plusDays(1).withHour(19).withMinute(0));
                show.setEndTime(LocalDateTime.now().plusDays(1).withHour(21).withMinute(30));
                showRepository.save(show);
            }
        }
    }

    @Test
    @DisplayName("Feature 5: Strict Budget Optimization - ₹800 for 2 seats (Gold fits, Silver saves, Platinum excluded)")
    void testBudgetOptimizationForTwoSeats() {
        BudgetOptimizationRequestDTO request = BudgetOptimizationRequestDTO.builder()
                .totalBudget(new BigDecimal("800.00"))
                .numberOfSeats(2)
                .build();

        List<BudgetRecommendationResponseDTO> recommendations = budgetOptimizerService.findOptimalShows(request);

        assertNotNull(recommendations, "Recommendations list should not be null");
        assertFalse(recommendations.isEmpty(), "Should find matching options for ₹800 budget");

        // Verify every single recommendation strictly respects the total budget
        for (BudgetRecommendationResponseDTO rec : recommendations) {
            assertTrue(rec.getTotalCost().compareTo(new BigDecimal("800.00")) <= 0,
                    "Total cost must never exceed the total budget of ₹800");
            assertEquals(2, rec.getNumberOfSeats(), "Must be for 2 seats");
            assertTrue(rec.getAvailableSeats() >= 2, "Must have at least 2 available seats in tier");
            assertNotEquals("PLATINUM", rec.getSeatCategory(),
                    "Platinum (₹500 * 2 = ₹1000) must be excluded as it exceeds budget");
        }

        // Verify BEST_EXPERIENCE recommendation exists
        boolean hasBestExperience = recommendations.stream()
                .anyMatch(r -> "BEST_EXPERIENCE".equals(r.getRecommendationType()));
        assertTrue(hasBestExperience, "Must provide a BEST_EXPERIENCE recommendation");

        // Verify MAX_SAVINGS recommendation exists
        boolean hasMaxSavings = recommendations.stream()
                .anyMatch(r -> "MAX_SAVINGS".equals(r.getRecommendationType()));
        assertTrue(hasMaxSavings, "Must provide a MAX_SAVINGS recommendation");
    }

    @Test
    @DisplayName("Feature 5: Insufficient Budget - ₹100 for 1 seat (cheapest is ₹150, should return empty)")
    void testInsufficientBudgetReturnsEmpty() {
        BudgetOptimizationRequestDTO request = BudgetOptimizationRequestDTO.builder()
                .totalBudget(new BigDecimal("100.00"))
                .numberOfSeats(1)
                .build();

        List<BudgetRecommendationResponseDTO> recommendations = budgetOptimizerService.findOptimalShows(request);

        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty(), "Should return empty list when budget is less than cheapest seat");
    }

    @Test
    @DisplayName("Feature 5: Genre Filtering - Only return shows matching the requested genre")
    void testGenreFilter() {
        BudgetOptimizationRequestDTO request = BudgetOptimizationRequestDTO.builder()
                .totalBudget(new BigDecimal("1000.00"))
                .numberOfSeats(1)
                .genre("Action")
                .build();

        List<BudgetRecommendationResponseDTO> recommendations = budgetOptimizerService.findOptimalShows(request);

        assertNotNull(recommendations);
        for (BudgetRecommendationResponseDTO rec : recommendations) {
            assertEquals("Action", rec.getGenre(), "All recommended shows must belong to the Action genre");
        }
    }
}
