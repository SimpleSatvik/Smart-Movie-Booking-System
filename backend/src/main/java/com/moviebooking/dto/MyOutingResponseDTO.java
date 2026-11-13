package com.moviebooking.dto;

import com.moviebooking.entity.Outing;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyOutingResponseDTO {

    private Long outingId;
    private String outingName;
    private Long bookingId;
    private String bookingReference;
    private String movieTitle;
    private String posterUrl;
    private String theatreName;
    private LocalDateTime showTime;
    private Long createdByUserId;
    private String createdByName;
    private int totalMembers;
    private BigDecimal totalGroupSpend;
    private LocalDateTime createdAt;

    public static MyOutingResponseDTO fromEntity(Outing outing) {
        String poster = null;
        String theatre = null;
        LocalDateTime st = null;
        String bRef = null;
        Long bId = null;

        if (outing.getBooking() != null) {
            bId = outing.getBooking().getId();
            bRef = outing.getBooking().getBookingReference();
            if (outing.getBooking().getShow() != null) {
                st = outing.getBooking().getShow().getStartTime();
                if (outing.getBooking().getShow().getMovie() != null) {
                    poster = outing.getBooking().getShow().getMovie().getPosterUrl();
                }
                if (outing.getBooking().getShow().getScreen() != null && outing.getBooking().getShow().getScreen().getTheatre() != null) {
                    theatre = outing.getBooking().getShow().getScreen().getTheatre().getName();
                }
            }
        }

        BigDecimal totalSpend = outing.getExpenses() != null
                ? outing.getExpenses().stream().map(e -> e.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        return MyOutingResponseDTO.builder()
                .outingId(outing.getId())
                .outingName(outing.getName())
                .bookingId(bId)
                .bookingReference(bRef)
                .movieTitle(outing.getBooking() != null && outing.getBooking().getShow() != null && outing.getBooking().getShow().getMovie() != null 
                        ? outing.getBooking().getShow().getMovie().getTitle() : "Movie Outing")
                .posterUrl(poster)
                .theatreName(theatre)
                .showTime(st)
                .createdByUserId(outing.getCreatedBy() != null ? outing.getCreatedBy().getId() : null)
                .createdByName(outing.getCreatedBy() != null ? outing.getCreatedBy().getName() : "Organizer")
                .totalMembers(outing.getMembers() != null ? outing.getMembers().size() : 1)
                .totalGroupSpend(totalSpend)
                .createdAt(outing.getCreatedAt())
                .build();
    }
}
