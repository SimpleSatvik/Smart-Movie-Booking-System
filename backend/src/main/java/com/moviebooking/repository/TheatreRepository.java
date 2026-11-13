package com.moviebooking.repository;

import com.moviebooking.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    // Spring Data JPA generates: "SELECT * FROM theatres WHERE LOWER(location) LIKE LOWER(?)"
    List<Theatre> findByLocationContainingIgnoreCase(String location);

    // Find all theatres owned by a specific owner's email (case-insensitive)
    List<Theatre> findByOwnerGmailIgnoreCase(String ownerGmail);

    List<Theatre> findByOwnerGmail(String ownerGmail);

    List<Theatre> findByOwnerId(Long ownerId);

    List<Theatre> findByIsDeletedFalse();

    long countByIsDeletedFalse();

    @Query("SELECT t FROM Theatre t WHERE (t.owner.id = :ownerId OR LOWER(t.owner.gmail) = LOWER(:ownerEmail)) AND (t.isDeleted = false OR t.isDeleted IS NULL)")
    List<Theatre> findTheatresByOwner(@Param("ownerId") Long ownerId, @Param("ownerEmail") String ownerEmail);
}
