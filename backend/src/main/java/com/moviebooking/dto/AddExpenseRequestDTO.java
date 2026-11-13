package com.moviebooking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddExpenseRequestDTO {

    @NotNull(message = "Payer User ID is required")
    private Long paidByUserId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least ₹1.00")
    private BigDecimal amount;

    @NotBlank(message = "Reason is required")
    private String reason; // e.g. "Popcorn Combo"
}
