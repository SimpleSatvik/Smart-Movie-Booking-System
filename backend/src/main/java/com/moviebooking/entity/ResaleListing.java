package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resale_listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResaleListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id")
    private Long id;

    // The specific seat for a show that is being put up for sale
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sfas_id", nullable = false)
    private ShowSeat showSeat;

    // The user who originally owned the ticket and wants to sell it
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    // The new buyer who purchases this resale ticket (empty until someone buys it)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    // The price the seller is asking for the ticket
    @Column(name = "asking_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal askingPrice;

    // Current state of this listing: ACTIVE, SOLD, or CANCELLED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ResaleListingStatus status = ResaleListingStatus.ACTIVE;

    // When the seller put this ticket on the marketplace
    @CreationTimestamp
    @Column(name = "listed_at", updatable = false)
    private LocalDateTime listedAt;

    // When the buyer completed the purchase
    @Column(name = "sold_at")
    private LocalDateTime soldAt;
}
