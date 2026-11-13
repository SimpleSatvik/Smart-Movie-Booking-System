package com.moviebooking.dto;

import com.moviebooking.entity.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Show ID is required")
    private Long showId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<Long> showSeatIds; // e.g. [1, 2] for seats A1, A2

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod; // MOCK_UPI, MOCK_CARD, MOCK_NET_BANKING
}
