package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "m_id")
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String genre; // e.g. "Action, Sci-Fi"

    @Column(nullable = false, length = 50)
    private String language; // e.g. "Hindi", "English"

    @Column(nullable = false)
    private Integer duration; // in minutes (e.g. 150)

    @Column(length = 100)
    private String mood; // e.g. "feel-good", "intense", "romantic" (for Vibe Search)

    @Column(name = "poster_url", length = 500)
    private String posterUrl; // Image URL for frontend cover display

    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Show> shows = new ArrayList<>();
}
