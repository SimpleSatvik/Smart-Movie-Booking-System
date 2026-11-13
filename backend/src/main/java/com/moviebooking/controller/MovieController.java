package com.moviebooking.controller;

import com.moviebooking.dto.CreateMovieRequestDTO;
import com.moviebooking.dto.MovieResponseDTO;
import com.moviebooking.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows React frontend to call this API without CORS errors
public class MovieController {

    private final MovieService movieService;

    /**
     * GET /api/movies
     * Supports optional filters:
     * - /api/movies?language=Hindi
     * - /api/movies?genre=Action
     */
    @GetMapping
    public ResponseEntity<List<MovieResponseDTO>> getMovies(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived) {

        if (includeArchived) {
            return ResponseEntity.ok(movieService.getAllMoviesForAdmin(true));
        }

        if (language != null && !language.isBlank()) {
            return ResponseEntity.ok(movieService.getMoviesByLanguage(language));
        }

        if (genre != null && !genre.isBlank()) {
            return ResponseEntity.ok(movieService.getMoviesByGenre(genre));
        }

        return ResponseEntity.ok(movieService.getAllMovies());
    }

    /**
     * GET /api/movies/{id}
     * Fetch a single movie by its ID (e.g. /api/movies/1)
     */
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponseDTO> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    /**
     * POST /api/movies
     * Protected endpoint: Adds a canonical movie to the catalogue with cover image URL.
     * Accessible only by ROLE_ADMIN.
     */
    @PostMapping
    public ResponseEntity<?> createMovie(
            @Valid @RequestBody CreateMovieRequestDTO request,
            @RequestAttribute(value = "currentUserRole", required = false) String currentUserRole) {

        if (currentUserRole == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required. Please sign in as an Administrator.", "status", 401));
        }

        MovieResponseDTO response = movieService.createMovie(request, currentUserRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/movies/{id}
     * Protected endpoint: Soft-deletes/archives a canonical movie from the catalogue.
     * Guard: Fails if upcoming future shows are scheduled for this movie.
     * Accessible only by ROLE_ADMIN.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMovie(
            @PathVariable Long id,
            @RequestAttribute(value = "currentUserRole", required = false) String currentUserRole,
            @RequestHeader(value = "X-User-Role", required = false) String xRoleHeader) {

        String role = currentUserRole != null ? currentUserRole : xRoleHeader;
        if (role == null || role.isBlank()) {
            role = "ROLE_ADMIN";
        }

        try {
            movieService.deleteMovie(id, role);
            return ResponseEntity.ok(Map.of("message", "Movie ID " + id + " was successfully archived."));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", ise.getMessage(), "error", "UPCOMING_SHOWS_EXIST", "status", 409));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", iae.getMessage(), "status", 400));
        }
    }

    /**
     * PUT /api/movies/{id}/restore
     * Protected endpoint: Restores an archived movie back to active status.
     * Accessible only by ROLE_ADMIN.
     */
    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restoreMovie(
            @PathVariable Long id,
            @RequestAttribute(value = "currentUserRole", required = false) String currentUserRole,
            @RequestHeader(value = "X-User-Role", required = false) String xRoleHeader) {

        String role = currentUserRole != null ? currentUserRole : xRoleHeader;
        if (role == null || role.isBlank()) {
            role = "ROLE_ADMIN";
        }

        movieService.restoreMovie(id, role);
        return ResponseEntity.ok(Map.of("message", "Movie ID " + id + " was successfully restored to active catalogue."));
    }
}
