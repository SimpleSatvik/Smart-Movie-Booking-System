package com.moviebooking.dto;

import com.moviebooking.entity.SeatCategory;
import com.moviebooking.entity.SeatStatus;
import com.moviebooking.entity.ShowSeat;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatLayoutResponseDTO {

    private Long showSeatId;
    private Long seatId;
    private String row;
    private Integer seatNumber;
    private SeatCategory category;
    private BigDecimal price;
    private SeatStatus status;

    // Helper method to convert Entity -> DTO
    public static SeatLayoutResponseDTO fromEntity(ShowSeat showSeat) {
        // If the lock has expired, present it as AVAILABLE to the frontend
        SeatStatus effectiveStatus = showSeat.isAvailableForBooking() 
                ? SeatStatus.AVAILABLE 
                : showSeat.getStatus();

        return SeatLayoutResponseDTO.builder()
                .showSeatId(showSeat.getId())
                .seatId(showSeat.getSeat().getId())
                .row(showSeat.getSeat().getRow())
                .seatNumber(showSeat.getSeat().getSeatNumber())
                .category(showSeat.getSeat().getCategory())
                .price(showSeat.getPrice())
                .status(effectiveStatus)
                .build();
    }
}
