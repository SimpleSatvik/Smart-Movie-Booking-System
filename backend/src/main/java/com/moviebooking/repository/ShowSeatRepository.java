package com.moviebooking.repository;

import com.moviebooking.entity.SeatStatus;
import com.moviebooking.entity.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    // Fetch all seats for a given show (for rendering the frontend seat map)
    @Query("SELECT ss FROM ShowSeat ss JOIN FETCH ss.seat WHERE ss.show.id = :showId ORDER BY ss.seat.row, ss.seat.seatNumber")
    List<ShowSeat> findByShowIdWithSeatDetails(@Param("showId") Long showId);

    // Fetch seats for a show filtered by status
    List<ShowSeat> findByShowIdAndStatus(Long showId, SeatStatus status);

    // Find a specific seat for a show
    Optional<ShowSeat> findByShowIdAndSeatId(Long showId, Long seatId);

    // Find multiple seats by their IDs for booking
    @Query("SELECT ss FROM ShowSeat ss JOIN FETCH ss.seat WHERE ss.id IN :ids")
    List<ShowSeat> findAllByIdWithSeatDetails(@Param("ids") List<Long> ids);

    // Fetch seats associated with a booking
    List<ShowSeat> findByBookingId(Long bookingId);

    /**
     * Feature 2: Core Concurrency Locking Query
     * Acquires a row-level pessimistic write lock on the specified available seats.
     * If any seat is currently being locked by another concurrent transaction,
     * PostgreSQL immediately skips it instead of waiting or causing a deadlock.
     */
    @Query(value = "SELECT * FROM seat_for_a_show ss " +
                   "WHERE ss.sfas_id IN (:seatIds) " +
                   "AND ss.status = 'AVAILABLE' " +
                   "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<ShowSeat> findAvailableSeatsForUpdateSkipLocked(@Param("seatIds") List<Long> seatIds);

    /**
     * Pessimistic row-level write lock on a single ShowSeat record.
     * Generates 'SELECT ... FOR UPDATE' in PostgreSQL via Hibernate.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ShowSeat s WHERE s.id = :seatId")
    Optional<ShowSeat> findByIdWithLock(@Param("seatId") Long seatId);

    // Aggregate queries for Theatre Owner Analytics
    @Query("SELECT COUNT(ss) FROM ShowSeat ss WHERE ss.show.screen.theatre.id = :theatreId")
    long countTotalSeatsByTheatreId(@Param("theatreId") Long theatreId);

    @Query("SELECT COUNT(ss) FROM ShowSeat ss WHERE ss.show.screen.theatre.id = :theatreId AND ss.status = :status")
    long countSeatsByTheatreIdAndStatus(@Param("theatreId") Long theatreId, @Param("status") SeatStatus status);

    @Query("SELECT COALESCE(SUM(ss.price), 0) FROM ShowSeat ss WHERE ss.show.screen.theatre.id = :theatreId AND ss.status = :status")
    java.math.BigDecimal sumRevenueByTheatreIdAndStatus(@Param("theatreId") Long theatreId, @Param("status") SeatStatus status);
}
