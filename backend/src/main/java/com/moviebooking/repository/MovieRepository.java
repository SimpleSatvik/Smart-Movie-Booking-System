package com.moviebooking.repository;

import com.moviebooking.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByIsDeletedFalse();

    Optional<Movie> findByIdAndIsDeletedFalse(Long id);

    List<Movie> findByLanguageIgnoreCaseAndIsDeletedFalse(String language);

    List<Movie> findByGenreContainingIgnoreCaseAndIsDeletedFalse(String genre);

    long countByIsDeletedFalse();

    List<Movie> findAllByOrderByIdDesc();
}
