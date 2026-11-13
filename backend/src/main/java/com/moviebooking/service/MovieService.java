package com.moviebooking.service;

import com.moviebooking.dto.CreateMovieRequestDTO;
import com.moviebooking.dto.MovieResponseDTO;
import com.moviebooking.entity.Movie;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;

    // 1. Fetch active movies (Cached in RAM - 0ms response time on repeat visits)
    @Cacheable(value = "movies")
    public List<MovieResponseDTO> getAllMovies() {
        return movieRepository.findByIsDeletedFalse()
                .stream()
                .map(MovieResponseDTO::fromEntity)
                .toList();
    }

    // 1b. Fetch all movies for Admin (can include archived/soft-deleted titles)
    public List<MovieResponseDTO> getAllMoviesForAdmin(boolean includeArchived) {
        List<Movie> list = includeArchived ? movieRepository.findAllByOrderByIdDesc() : movieRepository.findByIsDeletedFalse();
        return list.stream()
                .map(MovieResponseDTO::fromEntity)
                .toList();
    }

    // 2. Filter active movies by language (e.g. "Hindi")
    public List<MovieResponseDTO> getMoviesByLanguage(String language) {
        return movieRepository.findByLanguageIgnoreCaseAndIsDeletedFalse(language)
                .stream()
                .map(MovieResponseDTO::fromEntity)
                .toList();
    }

    // 3. Filter active movies by genre (e.g. "Action")
    public List<MovieResponseDTO> getMoviesByGenre(String genre) {
        return movieRepository.findByGenreContainingIgnoreCaseAndIsDeletedFalse(genre)
                .stream()
                .map(MovieResponseDTO::fromEntity)
                .toList();
    }

    // 4. Fetch a single movie by its ID (Cached per movie ID)
    @Cacheable(value = "movie_by_id", key = "#id")
    public MovieResponseDTO getMovieById(Long id) {
        Movie movie = movieRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found with ID: " + id));
        return MovieResponseDTO.fromEntity(movie);
    }

    /**
     * Admin Operation: Adds a canonical movie to the master catalogue with cover picture URL.
     * Automatically invalidates cached movie catalogues so visitors instantly see the new movie.
     */
    @Transactional
    @CacheEvict(value = {"movies", "movie_by_id"}, allEntries = true)
    public MovieResponseDTO createMovie(CreateMovieRequestDTO request, String currentUserRole) {
        if (currentUserRole != null && !currentUserRole.toUpperCase().contains("ADMIN")) {
            throw new IllegalArgumentException("Access denied: Only Administrators can add movies to the catalogue.");
        }

        Movie movie = Movie.builder()
                .title(request.getTitle().trim())
                .genre(request.getGenre().trim())
                .language(request.getLanguage().trim())
                .duration(request.getDuration())
                .mood(request.getMood() != null ? request.getMood().trim() : null)
                .posterUrl(request.getPosterUrl() != null ? request.getPosterUrl().trim() : null)
                .isDeleted(false)
                .build();

        Movie saved = movieRepository.save(movie);
        return MovieResponseDTO.fromEntity(saved);
    }

    /**
     * Admin Operation: Soft-deletes a movie from the catalogue.
     * Guard: First checks if any active or upcoming shows exist for this movie.
     * If future screenings exist, deletion is blocked to protect booked customers and multiplex schedules.
     * If all future screenings have concluded, soft-deletes the movie (isDeleted = true),
     * preserving past booking history, receipts, and financial settlements while removing it
     * from customer discovery and owner scheduling.
     */
    @Transactional
    @CacheEvict(value = {"movies", "movie_by_id"}, allEntries = true)
    public void deleteMovie(Long movieId, String currentUserRole) {
        if (currentUserRole != null && !currentUserRole.toUpperCase().contains("ADMIN")) {
            throw new IllegalArgumentException("Access denied: Only Administrators can delete movies.");
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found with ID: " + movieId));

        if (Boolean.TRUE.equals(movie.getIsDeleted())) {
            throw new IllegalArgumentException("Movie '" + movie.getTitle() + "' is already archived/deleted.");
        }

        // 1. Guard check: Are there any upcoming screenings scheduled for this movie?
        LocalDateTime now = LocalDateTime.now();
        long upcomingCount = showRepository.countByMovieIdAndStartTimeGreaterThanEqual(movieId, now);

        if (upcomingCount > 0) {
            throw new IllegalStateException("Cannot delete '" + movie.getTitle() + "': It has " + upcomingCount +
                    " upcoming show(s) scheduled. Wait for all future screenings to finish or cancel them first.");
        }

        // 2. Soft-delete the movie
        movie.setIsDeleted(true);
        movieRepository.save(movie);

        log.info("Movie ID {} ('{}') soft-deleted/archived successfully by Administrator.", movieId, movie.getTitle());
    }

    /**
     * Admin Operation: Restores an archived/soft-deleted movie back to active status.
     */
    @Transactional
    @CacheEvict(value = {"movies", "movie_by_id"}, allEntries = true)
    public void restoreMovie(Long movieId, String currentUserRole) {
        if (currentUserRole != null && !currentUserRole.toUpperCase().contains("ADMIN")) {
            throw new IllegalArgumentException("Access denied: Only Administrators can restore movies.");
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found with ID: " + movieId));

        movie.setIsDeleted(false);
        movieRepository.save(movie);

        log.info("Movie ID {} ('{}') restored/reactivated successfully by Administrator.", movieId, movie.getTitle());
    }
}
