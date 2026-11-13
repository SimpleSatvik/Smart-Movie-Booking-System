package com.moviebooking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetOptimizationRequestDTO {

    @NotNull(message = "Total budget is required")
    @DecimalMin(value = "50.0", message = "Budget must be at least ₹50.00")
    private BigDecimal totalBudget; // e.g. 500.00

    @Min(value = 1, message = "Must request at least 1 seat")
    @Builder.Default
    private Integer numberOfSeats = 1; // e.g. 2 seats

    // Optional filters to narrow down choices
    private LocalDate date;
    private String genre;       // e.g. "Action", "Sci-Fi"
    private String language;    // e.g. "Hindi", "English"
}
