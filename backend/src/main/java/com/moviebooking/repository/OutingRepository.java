package com.moviebooking.repository;

import com.moviebooking.entity.Outing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OutingRepository extends JpaRepository<Outing, Long> {

    Optional<Outing> findByBookingId(Long bookingId);
}
