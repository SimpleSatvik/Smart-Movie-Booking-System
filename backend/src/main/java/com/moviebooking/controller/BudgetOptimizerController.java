package com.moviebooking.controller;

import com.moviebooking.dto.BudgetOptimizationRequestDTO;
import com.moviebooking.dto.BudgetRecommendationResponseDTO;
import com.moviebooking.service.BudgetOptimizerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/optimizer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BudgetOptimizerController {

    private final BudgetOptimizerService budgetOptimizerService;

    /**
     * POST /api/optimizer/movie-night
     * Calculates the optimal movie show + seat tier combinations
     * for a given budget and party size.
     */
    @PostMapping("/movie-night")
    public ResponseEntity<List<BudgetRecommendationResponseDTO>> optimizeMovieNight(
            @Valid @RequestBody BudgetOptimizationRequestDTO request) {
        List<BudgetRecommendationResponseDTO> recommendations = budgetOptimizerService.findOptimalShows(request);
        return ResponseEntity.ok(recommendations);
    }
}
