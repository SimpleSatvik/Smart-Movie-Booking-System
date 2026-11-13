package com.moviebooking.controller;

import com.moviebooking.dto.CreateTheatreRequestDTO;
import com.moviebooking.dto.TheatreAnalyticsResponseDTO;
import com.moviebooking.dto.TheatreResponseDTO;
import com.moviebooking.service.TheatreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/theatres")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows React frontend to connect without CORS issues
public class TheatreController {

    private final TheatreService theatreService;

    /**
     * POST /api/theatres
     * Protected endpoint: Creates a new theatre with screens and 30 physical seats per screen.
     * Accessible by ROLE_THEATRE_OWNER and ROLE_ADMIN.
     */
    @PostMapping
    public ResponseEntity<TheatreResponseDTO> createTheatre(
            @Valid @RequestBody CreateTheatreRequestDTO request,
            @RequestAttribute(value = "currentUserGmail", required = false) String currentUserGmail,
            @RequestAttribute(value = "currentUserRole", required = false) String currentUserRole) {

        if (currentUserGmail == null || currentUserRole == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TheatreResponseDTO response = theatreService.createTheatre(request, currentUserGmail, currentUserRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/theatres/my-theatres
     * Protected endpoint: Returns all theatres owned by the currently authenticated theatre owner.
     */
    @GetMapping("/my-theatres")
    public ResponseEntity<List<TheatreResponseDTO>> getMyTheatres(
            @RequestAttribute(value = "currentUserGmail", required = false) String currentUserGmail) {

        if (currentUserGmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<TheatreResponseDTO> theatres = theatreService.getMyTheatres(currentUserGmail);
        return ResponseEntity.ok(theatres);
    }

    /**
     * GET /api/theatres/{theatreId}/screens
     * Returns all auditorium screens with screenId, screenName, and totalSeats for the selected theatre.
     */
    @GetMapping("/{theatreId}/screens")
    public ResponseEntity<List<TheatreResponseDTO.ScreenSummaryDTO>> getScreensByTheatre(
            @PathVariable Long theatreId) {
        return ResponseEntity.ok(theatreService.getScreensByTheatreId(theatreId));
    }

    /**
     * GET /api/theatres
     * Public endpoint: Lists all active theatres in the catalogue.
     */
    @GetMapping
    public ResponseEntity<List<TheatreResponseDTO>> getAllTheatres() {
        return ResponseEntity.ok(theatreService.getAllTheatres());
    }

    /**
     * GET /api/theatres/{theatreId}/analytics
     * Protected endpoint: Returns revenue, tickets sold, and seat occupancy % for this cinema.
     * Accessible by ROLE_THEATRE_OWNER (of this theatre) and ROLE_ADMIN.
     */
    @GetMapping("/{theatreId}/analytics")
    public ResponseEntity<TheatreAnalyticsResponseDTO> getTheatreAnalytics(
            @PathVariable Long theatreId,
            @RequestAttribute(value = "currentUserGmail", required = false) String currentUserGmail,
            @RequestAttribute(value = "currentUserRole", required = false) String currentUserRole) {

        if (currentUserGmail == null || currentUserRole == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TheatreAnalyticsResponseDTO analytics =
                theatreService.getTheatreAnalytics(theatreId, currentUserGmail, currentUserRole);
        return ResponseEntity.ok(analytics);
    }
}
