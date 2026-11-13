package com.moviebooking.repository;

import com.moviebooking.entity.ResaleListing;
import com.moviebooking.entity.ResaleListingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResaleListingRepository extends JpaRepository<ResaleListing, Long> {

    // 1. Fetch all listings with a specific status (e.g. all ACTIVE tickets on the marketplace)
    List<ResaleListing> findByStatus(ResaleListingStatus status);

    @Query("SELECT r FROM ResaleListing r WHERE r.status = :status AND r.showSeat.show.startTime >= :now ORDER BY r.showSeat.show.startTime ASC")
    List<ResaleListing> findUpcomingByStatus(@Param("status") ResaleListingStatus status, @Param("now") java.time.LocalDateTime now);

    // 2. Fetch all active resale tickets for a specific show (useful when a user is browsing seats for a particular show)
    List<ResaleListing> findByShowSeatShowIdAndStatus(Long showId, ResaleListingStatus status);

    @Query("SELECT r FROM ResaleListing r WHERE r.showSeat.show.id = :showId AND r.status = :status AND r.showSeat.show.startTime >= :now ORDER BY r.showSeat.show.startTime ASC")
    List<ResaleListing> findUpcomingByShowIdAndStatus(@Param("showId") Long showId, @Param("status") ResaleListingStatus status, @Param("now") java.time.LocalDateTime now);

    // 3. Fetch all listings posted by a specific seller (for the "My Listings" page)
    List<ResaleListing> findBySellerId(Long sellerId);

    // 4. Check if a seat is ALREADY actively listed (prevents a user from listing the same seat twice)
    boolean existsByShowSeatIdAndStatus(Long showSeatId, ResaleListingStatus status);

    // Fetch active listings for a list of show seats
    List<ResaleListing> findByShowSeatIdInAndStatus(List<Long> showSeatIds, ResaleListingStatus status);

    /**
     * Pessimistic lock for safe updates/cancellations.
     * Prevents cancellation from racing with an in-progress purchase.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ResaleListing r WHERE r.id = :id")
    Optional<ResaleListing> findByIdWithLock(@Param("id") Long id);

    /**
     * 5. Concurrency Safe Purchase Query (Pessimistic Locking):
     * Native PostgreSQL query with 'FOR UPDATE SKIP LOCKED'.
     *
     * How it works:
     * - The first transaction to call this query locks the resale row exclusively.
     * - If a second concurrent transaction tries to call this at the same millisecond,
     *   PostgreSQL skips the row because it is locked, returning Optional.empty().
     * - This ensures zero double-selling and zero waiting/deadlocks.
     */
    @Query(value = "SELECT * FROM resale_listings WHERE listing_id = :listingId AND status = 'ACTIVE' FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<ResaleListing> findActiveListingForUpdateSkipLocked(@Param("listingId") Long listingId);
}
