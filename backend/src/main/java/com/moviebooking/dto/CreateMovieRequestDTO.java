package com.moviebooking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMovieRequestDTO {

    @NotBlank(message = "Movie title is required")
    private String title;

    @NotBlank(message = "Genre is required (e.g. Action, Sci-Fi)")
    private String genre;

    @NotBlank(message = "Language is required (e.g. Hindi, English)")
    private String language;

    @NotNull(message = "Duration in minutes is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer duration;

    // Optional mood tag for AI Vibe Search (e.g. mind-bending, heartwarming)
    private String mood;

    // Optional cover picture / poster image URL
    private String posterUrl;
}
