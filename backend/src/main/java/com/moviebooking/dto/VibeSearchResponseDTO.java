package com.moviebooking.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VibeSearchResponseDTO {

    private Long movieId;
    private String movieTitle;
    private String genre;
    private String language;
    private String mood;

    // AI-generated reasoning tailored to the user's input
    private String matchReason;

    // Confidence score between 0.0 and 1.0 (e.g. 0.95 = 95% match)
    private Double confidenceScore;

    // List of actual future shows available for booking
    @Builder.Default
    private List<UpcomingShowInfoDTO> upcomingShows = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpcomingShowInfoDTO {
        private Long showId;
        private String theatreName;
        private String screenName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
