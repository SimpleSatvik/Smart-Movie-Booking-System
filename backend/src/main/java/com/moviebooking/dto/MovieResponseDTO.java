package com.moviebooking.dto;

import com.moviebooking.entity.Movie;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieResponseDTO {

    private Long id;
    private String title;
    private String genre;
    private String language;
    private Integer duration;
    private String mood;
    private String posterUrl;
    private Boolean isDeleted;

    // Helper method to convert an Entity -> DTO
    public static MovieResponseDTO fromEntity(Movie movie) {
        return MovieResponseDTO.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .genre(movie.getGenre())
                .language(movie.getLanguage())
                .duration(movie.getDuration())
                .mood(movie.getMood())
                .posterUrl(movie.getPosterUrl())
                .isDeleted(movie.getIsDeleted() != null ? movie.getIsDeleted() : false)
                .build();
    }
}
