package com.moviebooking.repository;

import com.moviebooking.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {

    // Spring Data JPA generates: "SELECT * FROM screens WHERE t_id = ?"
    List<Screen> findByTheatreId(Long theatreId);
}
