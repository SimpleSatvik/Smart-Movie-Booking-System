package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "outings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outing_id")
    private Long id;

    @Column(nullable = false, length = 150)
    private String name; // e.g. "Jawan Night with Friends"

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "outing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OutingMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "outing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Expense> expenses = new ArrayList<>();

    @OneToMany(mappedBy = "outing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Settlement> settlements = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
