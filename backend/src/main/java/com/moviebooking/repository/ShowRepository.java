package com.moviebooking.repository;

import com.moviebooking.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    // Find all scheduled shows for a movie
    List<Show> findByMovieId(Long movieId);

    // Find shows for a movie in a specific time window (e.g. today between 00:00 and 23:59)
    List<Show> findByMovieIdAndStartTimeBetween(Long movieId, LocalDateTime start, LocalDateTime end);

    // Find shows for a movie in a specific time window ordered by start time
    List<Show> findByMovieIdAndStartTimeBetweenOrderByStartTimeAsc(Long movieId, LocalDateTime start, LocalDateTime end);

    // Find all upcoming shows scheduled after a given time
    List<Show> findByStartTimeAfter(LocalDateTime time);

    // Find all shows scheduled for a particular screen
    List<Show> findByScreenId(Long screenId);

    // Count all shows scheduled across all screens of a theatre
    long countByScreenTheatreId(Long theatreId);

    // Find all scheduled shows for a specific theatre multiplex
    List<Show> findByScreenTheatreIdOrderByStartTimeAsc(Long theatreId);

    // Count upcoming future shows for a movie
    long countByMovieIdAndStartTimeGreaterThanEqual(Long movieId, LocalDateTime time);

    // Find upcoming future shows for a movie
    List<Show> findByMovieIdAndStartTimeGreaterThanEqual(Long movieId, LocalDateTime time);

    // Find upcoming future shows for a movie ordered by start time
    List<Show> findByMovieIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(Long movieId, LocalDateTime time);

    // Count upcoming or active shows across specific theatres
    @Query("SELECT COUNT(s) FROM Show s WHERE s.screen.theatre.id IN :theatreIds AND (s.endTime >= :now OR s.startTime >= :now)")
    long countUpcomingShowsByTheatreIds(@Param("theatreIds") List<Long> theatreIds, @Param("now") LocalDateTime now);

    // Count all shows scheduled for a list of theatre IDs
    @Query("SELECT COUNT(s) FROM Show s WHERE s.screen.theatre.id IN :theatreIds")
    long countTotalShowsByTheatreIds(@Param("theatreIds") List<Long> theatreIds);

    // Count upcoming shows across all screens belonging to a specific theatre owner
    @Query("SELECT COUNT(s) FROM Show s WHERE (s.screen.theatre.owner.id = :ownerId OR LOWER(s.screen.theatre.owner.gmail) = LOWER(:ownerEmail)) AND (s.endTime >= :now OR s.startTime >= :now)")
    long countUpcomingShowsByTheatreOwner(@Param("ownerId") Long ownerId, @Param("ownerEmail") String ownerEmail, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM Show s WHERE s.screen.theatre.owner.id = :ownerId AND (s.endTime >= :now OR s.startTime >= :now)")
    long countUpcomingShowsByTheatreOwner(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);
}
