package com.moviebooking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheatreAnalyticsResponseDTO {

    private Long theatreId;
    private String theatreName;
    private String location;
    private Integer totalScreens;

    // Operational metrics
    private Long totalShows;
    private Long totalSeatsCapacity;
    private Long totalTicketsSold;
    private BigDecimal totalRevenue;

    // e.g. 45.5%
    private Double occupancyRate;

    @JsonProperty("totalShowsScheduled")
    public Long getTotalShowsScheduled() {
        return totalShows != null ? totalShows : 0L;
    }

    @JsonProperty("totalSeatsBooked")
    public Long getTotalSeatsBooked() {
        return totalTicketsSold != null ? totalTicketsSold : 0L;
    }
}
