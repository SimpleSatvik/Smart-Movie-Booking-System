package com.moviebooking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moviebooking.entity.Show;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowResponseDTO {

    private Long showId;
    private Long movieId;
    private String movieTitle;
    private String theatreName;
    private String theatreLocation;
    private String screenName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    // Helper method to convert Entity -> DTO
    public static ShowResponseDTO fromEntity(Show show) {
        return ShowResponseDTO.builder()
                .showId(show.getId())
                .movieId(show.getMovie().getId())
                .movieTitle(show.getMovie().getTitle())
                .theatreName(show.getScreen().getTheatre().getName())
                .theatreLocation(show.getScreen().getTheatre().getLocation())
                .screenName(show.getScreen().getName())
                .startTime(show.getStartTime())
                .endTime(show.getEndTime())
                .build();
    }
}
