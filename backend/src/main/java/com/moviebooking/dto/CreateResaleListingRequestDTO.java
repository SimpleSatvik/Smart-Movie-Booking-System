package com.moviebooking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateResaleListingRequestDTO {

    @NotNull(message = "Seller user ID is required")
    private Long sellerUserId;

    @NotNull(message = "Show seat ID is required")
    private Long showSeatId;

    @NotNull(message = "Asking price is required")
    @DecimalMin(value = "1.0", message = "Asking price must be at least ₹1.00")
    private BigDecimal askingPrice;
}
