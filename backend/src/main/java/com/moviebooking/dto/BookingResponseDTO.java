package com.moviebooking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moviebooking.entity.Booking;
import com.moviebooking.entity.ShowSeat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {

    private Long bookingId;
    private String bookingReference;
    private String movieTitle;
    private String theatreName;
    private String screenName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime showTime;
    private List<String> seatCodes; // e.g. ["A1", "A2"]
    private List<Long> showSeatIds; // e.g. [1, 2]
    private BigDecimal totalAmountPaid;
    private String status;
    private String transactionId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime bookedAt;

    // Helper method to convert Booking Entity -> Digital Ticket DTO
    public static BookingResponseDTO fromEntity(Booking booking) {
        List<ShowSeat> currentSeats = booking.getBookedSeats() != null
                ? booking.getBookedSeats().stream()
                    .filter(ss -> ss.getBooking() != null && ss.getBooking().getId().equals(booking.getId()))
                    .toList()
                : List.of();

        List<String> seats = !currentSeats.isEmpty()
                ? currentSeats.stream()
                    .map(ss -> ss.getSeat().getRow() + ss.getSeat().getSeatNumber())
                    .toList()
                : (booking.getSeatSummary() != null && !booking.getSeatSummary().isBlank() && booking.getStatus() != com.moviebooking.entity.BookingStatus.RESOLD
                    ? List.of(booking.getSeatSummary().split(",\\s*"))
                    : List.of());

        List<Long> seatIds = !currentSeats.isEmpty()
                ? currentSeats.stream()
                    .map(ShowSeat::getId)
                    .toList()
                : List.of();

        String txnId = booking.getPayment() != null ? booking.getPayment().getTransactionId() : "N/A";

        return BookingResponseDTO.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .movieTitle(booking.getShow() != null && booking.getShow().getMovie() != null 
                        ? booking.getShow().getMovie().getTitle() : "Movie")
                .theatreName(booking.getShow() != null && booking.getShow().getScreen() != null 
                        ? booking.getShow().getScreen().getTheatre().getName() : "Theatre")
                .screenName(booking.getShow() != null && booking.getShow().getScreen() != null 
                        ? booking.getShow().getScreen().getName() : "Screen 1")
                .showTime(booking.getShow() != null ? booking.getShow().getStartTime() : LocalDateTime.now())
                .seatCodes(seats)
                .showSeatIds(seatIds)
                .totalAmountPaid(booking.getTotalAmountPaid())
                .status(booking.getStatus() != null ? booking.getStatus().name() : "CONFIRMED")
                .transactionId(txnId)
                .bookedAt(booking.getCreatedAt())
                .build();
    }
}
