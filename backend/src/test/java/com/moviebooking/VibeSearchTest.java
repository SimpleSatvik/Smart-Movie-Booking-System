package com.moviebooking;

import com.moviebooking.dto.VibeSearchRequestDTO;
import com.moviebooking.dto.VibeSearchResponseDTO;
import com.moviebooking.entity.Show;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.service.VibeSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VibeSearchTest {

    @Autowired
    private VibeSearchService vibeSearchService;

    @Autowired
    private ShowRepository showRepository;

    @BeforeEach
    void ensureShowsAreInFuture() {
        // Ensure shows have future timestamps so they appear in Vibe Search
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
    @DisplayName("Feature 6: Vibe Search - 'Mind-bending puzzle' matches Inception with AI reasoning and shows")
    void testVibeSearchForInception() {
        VibeSearchRequestDTO request = VibeSearchRequestDTO.builder()
                .vibe("I want a mind-bending sci-fi puzzle that makes me question reality")
                .build();

        List<VibeSearchResponseDTO> results = vibeSearchService.searchByVibe(request);

        assertNotNull(results, "Results list should not be null");
        assertFalse(results.isEmpty(), "Should find at least 1 matching movie for this vibe");

        // The top recommendation should be Inception
        VibeSearchResponseDTO topMatch = results.get(0);
        assertEquals("Inception", topMatch.getMovieTitle(), "Top match for mind-bending sci-fi must be Inception");
        assertNotNull(topMatch.getMatchReason(), "AI matchReason must be populated");
        assertFalse(topMatch.getMatchReason().isBlank(), "AI matchReason cannot be empty");
        assertTrue(topMatch.getConfidenceScore() > 0.0, "Confidence score must be positive");

        // Verify future showtimes are attached
        assertFalse(topMatch.getUpcomingShows().isEmpty(), "Upcoming shows must be included for the recommended movie");
    }

    @Test
    @DisplayName("Feature 6: Vibe Search - 'Massive adrenaline action' matches Jawan with AI reasoning and shows")
    void testVibeSearchForJawan() {
        VibeSearchRequestDTO request = VibeSearchRequestDTO.builder()
                .vibe("I want an adrenaline-pumping, mass action movie with high energy and thrills")
                .build();

        List<VibeSearchResponseDTO> results = vibeSearchService.searchByVibe(request);

        assertNotNull(results, "Results list should not be null");
        assertFalse(results.isEmpty(), "Should find at least 1 matching movie for this vibe");

        // The top recommendation should be Jawan
        VibeSearchResponseDTO topMatch = results.get(0);
        assertEquals("Jawan", topMatch.getMovieTitle(), "Top match for mass action must be Jawan");
        assertNotNull(topMatch.getMatchReason(), "AI matchReason must be populated");
        assertFalse(topMatch.getMatchReason().isBlank(), "AI matchReason cannot be empty");
        assertTrue(topMatch.getConfidenceScore() > 0.0, "Confidence score must be positive");

        // Verify future showtimes are attached
        assertFalse(topMatch.getUpcomingShows().isEmpty(), "Upcoming shows must be included for the recommended movie");
    }
}
