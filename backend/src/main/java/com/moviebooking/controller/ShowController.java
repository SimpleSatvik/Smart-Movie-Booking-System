package com.moviebooking.controller;

import com.moviebooking.dto.CreateShowRequestDTO;
import com.moviebooking.dto.SeatLayoutResponseDTO;
import com.moviebooking.dto.ShowResponseDTO;
import com.moviebooking.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows React frontend to call without CORS blocks
public class ShowController {

    private final ShowService showService;

    /**
     * POST /api/shows
     * Protected endpoint: Schedules a show on a screen and automatically populates all 30
     * physical ShowSeat inventory records with their tier prices.
     * Accessible by ROLE_THEATRE_OWNER and ROLE_ADMIN.
     */
    @PostMapping
    public ResponseEntity<ShowResponseDTO> createShow(
            @Valid @RequestBody CreateShowRequestDTO request,
            @RequestAttribute(value = "currentUserGmail", required = false) String currentUserGmail,
            @RequestAttribute(value = "currentUserRole", required = false) String currentUserRole) {

        if (currentUserGmail == null || currentUserRole == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ShowResponseDTO response = showService.createShow(request, currentUserGmail, currentUserRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/shows/movie/{movieId}
     * Fetches shows for a movie. Optional date filter:
     * - /api/shows/movie/1
     * - /api/shows/movie/1?date=2026-11-09
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowResponseDTO>> getShowsByMovie(
            @PathVariable Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (date != null) {
            return ResponseEntity.ok(showService.getShowsByMovieAndDate(movieId, date));
        }

        return ResponseEntity.ok(showService.getShowsByMovie(movieId));
    }

    /**
     * GET /api/shows/theatre/{theatreId}
     * Fetches all scheduled shows for a specific theatre multiplex.
     */
    @GetMapping("/theatre/{theatreId}")
    public ResponseEntity<List<ShowResponseDTO>> getShowsByTheatre(@PathVariable Long theatreId) {
        return ResponseEntity.ok(showService.getShowsByTheatreId(theatreId));
    }

    /**
     * GET /api/shows/{showId}/seats
     * Fetches interactive seat layout and availability for a show (e.g. /api/shows/1/seats)
     */
    @GetMapping("/{showId}/seats")
    public ResponseEntity<List<SeatLayoutResponseDTO>> getSeatLayout(@PathVariable Long showId) {
        return ResponseEntity.ok(showService.getSeatLayoutForShow(showId));
    }
}
