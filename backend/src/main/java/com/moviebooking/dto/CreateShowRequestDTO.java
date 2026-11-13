package com.moviebooking.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShowRequestDTO {

    @NotNull(message = "Movie ID is required")
    private Long movieId;

    @JsonAlias({"screenId", "screen_id"})
    @JsonProperty("screenId")
    private Long screenId;

    // Optional convenience fields allowing the service to resolve the screen
    private Long theatreId;
    private String screenName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    @NotNull(message = "Show start time is required")
    private LocalDateTime startTime;

    // Optional: If omitted, auto-calculated as startTime + movie.duration + 15 mins buffer
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime endTime;

    // Optional custom seat tier prices (defaults: Silver=200, Gold=350, Platinum=500)
    private BigDecimal silverPrice;
    private BigDecimal goldPrice;
    private BigDecimal platinumPrice;
}
