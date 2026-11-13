package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats", uniqueConstraints = {
    // A screen cannot have duplicate seats with same row and seat number
    @UniqueConstraint(columnNames = {"screen_id", "row_identifier", "seat_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long id;

    @Column(name = "row_identifier", nullable = false, length = 5)
    private String row; // e.g. "A", "B", "C"

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber; // e.g. 1, 2, 3

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatCategory category; // SILVER, GOLD, PLATINUM, VIP

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;
}
