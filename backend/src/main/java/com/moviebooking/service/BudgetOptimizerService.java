package com.moviebooking.service;

import com.moviebooking.dto.BudgetOptimizationRequestDTO;
import com.moviebooking.dto.BudgetRecommendationResponseDTO;
import com.moviebooking.entity.SeatCategory;
import com.moviebooking.entity.SeatStatus;
import com.moviebooking.entity.Show;
import com.moviebooking.entity.ShowSeat;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetOptimizerService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;

    /**
     * Finds and ranks the optimal movie show + seat tier combinations
     * that fit strictly within the user's budget and seat quantity.
     */
    @Transactional(readOnly = true)
    public List<BudgetRecommendationResponseDTO> findOptimalShows(BudgetOptimizationRequestDTO request) {
        int seatsRequested = (request.getNumberOfSeats() != null && request.getNumberOfSeats() > 0)
                ? request.getNumberOfSeats() : 1;

        log.info("Optimizing movie night for budget: ₹{}, seats: {}, date: {}, genre: {}, lang: {}",
                request.getTotalBudget(), seatsRequested, request.getDate(), request.getGenre(), request.getLanguage());

        // 1. Fetch upcoming shows
        List<Show> upcomingShows = showRepository.findByStartTimeAfter(LocalDateTime.now());

        // 2. Filter by user's optional criteria
        List<Show> filteredShows = upcomingShows.stream()
                .filter(show -> {
                    // Filter by Date if specified
                    if (request.getDate() != null && !show.getStartTime().toLocalDate().equals(request.getDate())) {
                        return false;
                    }
                    // Filter by Genre if specified
                    if (request.getGenre() != null && !request.getGenre().isBlank()) {
                        if (!show.getMovie().getGenre().equalsIgnoreCase(request.getGenre().trim())) {
                            return false;
                        }
                    }
                    // Filter by Language if specified
                    if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
                        if (!show.getMovie().getLanguage().equalsIgnoreCase(request.getLanguage().trim())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 3. For each show, evaluate available seat categories against the budget
        List<CandidateOption> candidates = new ArrayList<>();

        for (Show show : filteredShows) {
            List<ShowSeat> availableSeats = showSeatRepository.findByShowIdAndStatus(show.getId(), SeatStatus.AVAILABLE);
            if (availableSeats.isEmpty()) {
                continue;
            }

            // Group available seats by category (SILVER, GOLD, PLATINUM)
            Map<SeatCategory, List<ShowSeat>> seatsByCategory = availableSeats.stream()
                    .collect(Collectors.groupingBy(ss -> ss.getSeat().getCategory()));

            for (Map.Entry<SeatCategory, List<ShowSeat>> entry : seatsByCategory.entrySet()) {
                SeatCategory category = entry.getKey();
                List<ShowSeat> seatsInTier = entry.getValue();
                int availableCount = seatsInTier.size();

                // Check Constraint 1: Capacity (Are there enough seats in this tier?)
                if (availableCount < seatsRequested) {
                    continue;
                }

                BigDecimal pricePerSeat = seatsInTier.get(0).getPrice();
                BigDecimal totalCost = pricePerSeat.multiply(BigDecimal.valueOf(seatsRequested));

                // Check Constraint 2: Budget (Does total cost fit within user budget?)
                if (totalCost.compareTo(request.getTotalBudget()) <= 0) {
                    BigDecimal remainingBudget = request.getTotalBudget().subtract(totalCost);

                    candidates.add(new CandidateOption(
                            show,
                            category,
                            pricePerSeat,
                            seatsRequested,
                            totalCost,
                            remainingBudget,
                            availableCount
                    ));
                }
            }
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Rank and classify the candidates:
        // Sort by quality/tier descending (PLATINUM > GOLD > SILVER) and price descending for BEST_EXPERIENCE
        candidates.sort((c1, c2) -> {
            int tierComparison = c2.category.compareTo(c1.category);
            if (tierComparison != 0) return tierComparison;
            return c2.pricePerSeat.compareTo(c1.pricePerSeat);
        });
        CandidateOption bestExperience = candidates.get(0);

        // Sort by total cost ascending for MAX_SAVINGS (cheapest option)
        candidates.sort(Comparator.comparing(c -> c.totalCost));
        CandidateOption maxSavings = candidates.get(0);

        // 5. Construct the final recommendations list
        List<BudgetRecommendationResponseDTO> recommendations = new ArrayList<>();

        // Add "BEST_EXPERIENCE"
        recommendations.add(buildDTO("BEST_EXPERIENCE", bestExperience));

        // Add "MAX_SAVINGS" if it is distinct from BEST_EXPERIENCE
        if (!isSameOption(bestExperience, maxSavings)) {
            recommendations.add(buildDTO("MAX_SAVINGS", maxSavings));
        }

        // Add up to 3 remaining candidates as "ALTERNATIVE"
        for (CandidateOption option : candidates) {
            if (!isSameOption(option, bestExperience) && !isSameOption(option, maxSavings)) {
                recommendations.add(buildDTO("ALTERNATIVE", option));
                if (recommendations.size() >= 5) {
                    break;
                }
            }
        }

        return recommendations;
    }

    private boolean isSameOption(CandidateOption o1, CandidateOption o2) {
        return o1.show.getId().equals(o2.show.getId()) && o1.category == o2.category;
    }

    private BudgetRecommendationResponseDTO buildDTO(String type, CandidateOption opt) {
        return BudgetRecommendationResponseDTO.builder()
                .recommendationType(type)
                .movieId(opt.show.getMovie().getId())
                .movieTitle(opt.show.getMovie().getTitle())
                .genre(opt.show.getMovie().getGenre())
                .language(opt.show.getMovie().getLanguage())
                .theatreName(opt.show.getScreen().getTheatre().getName())
                .screenName(opt.show.getScreen().getName())
                .showId(opt.show.getId())
                .showStartTime(opt.show.getStartTime())
                .seatCategory(opt.category.name())
                .pricePerSeat(opt.pricePerSeat)
                .numberOfSeats(opt.numberOfSeats)
                .totalCost(opt.totalCost)
                .remainingBudget(opt.remainingBudget)
                .availableSeats(opt.availableSeats)
                .build();
    }

    // Lightweight internal container for evaluating options
    private record CandidateOption(
            Show show,
            SeatCategory category,
            BigDecimal pricePerSeat,
            Integer numberOfSeats,
            BigDecimal totalCost,
            BigDecimal remainingBudget,
            Integer availableSeats
    ) {}
}
