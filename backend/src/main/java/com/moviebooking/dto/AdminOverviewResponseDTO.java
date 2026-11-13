package com.moviebooking.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOverviewResponseDTO {

    private Long totalUsers;
    private Long totalTheatres;
    private Long totalShows;
    private Long totalMovies;
    private Long totalBookings;
    private BigDecimal totalPlatformRevenue;
}
