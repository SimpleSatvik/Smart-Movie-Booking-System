package com.moviebooking;

import com.moviebooking.dto.CreateShowRequestDTO;
import com.moviebooking.dto.CreateTheatreRequestDTO;
import com.moviebooking.dto.ShowResponseDTO;
import com.moviebooking.dto.TheatreAnalyticsResponseDTO;
import com.moviebooking.dto.TheatreResponseDTO;
import com.moviebooking.entity.Movie;
import com.moviebooking.entity.SeatStatus;
import com.moviebooking.entity.ShowSeat;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ShowSeatRepository;
import com.moviebooking.service.ShowService;
import com.moviebooking.service.TheatreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TheatreOwnerOperationsTest {

    @Autowired
    private TheatreService theatreService;

    @Autowired
    private ShowService showService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Test
    @DisplayName("Theatre Owner Lifecycle: Create Theatre -> Schedule Show -> Auto-Generate 30 Seats -> View Analytics")
    void testFullTheatreOwnerLifecycle() {
        String ownerEmail = "owner@pvr.com";
        String ownerRole = "ROLE_THEATRE_OWNER";

        // 1. Owner creates a new theatre
        CreateTheatreRequestDTO theatreRequest = CreateTheatreRequestDTO.builder()
                .name("PVR Cyberhub " + System.currentTimeMillis())
                .location("Gurgaon")
                .totalScreens(1)
                .build();

        TheatreResponseDTO theatre = theatreService.createTheatre(theatreRequest, ownerEmail, ownerRole);

        assertNotNull(theatre.getId(), "Theatre ID must be generated");
        assertEquals("Gurgaon", theatre.getLocation());
        assertEquals(1, theatre.getScreens().size());
        assertEquals(30, theatre.getScreens().get(0).getTotalSeats(), "Screen must have 30 pre-populated seats");

        Long screenId = theatre.getScreens().get(0).getScreenId();

        // 2. Owner schedules a show using a canonical movie
        Movie movie = movieRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Movie catalog should contain seeded movies"));

        LocalDateTime startTime = LocalDateTime.now().plusDays(2).withHour(20).withMinute(0);

        CreateShowRequestDTO showRequest = CreateShowRequestDTO.builder()
                .movieId(movie.getId())
                .screenId(screenId)
                .startTime(startTime)
                .silverPrice(BigDecimal.valueOf(220.00))
                .goldPrice(BigDecimal.valueOf(380.00))
                .platinumPrice(BigDecimal.valueOf(550.00))
                .build();

        ShowResponseDTO show = showService.createShow(showRequest, ownerEmail, ownerRole);

        assertNotNull(show.getShowId(), "Show ID must be generated");
        assertEquals(movie.getTitle(), show.getMovieTitle());

        // 3. Verify that 30 ShowSeat inventory records were automatically generated & AVAILABLE
        List<ShowSeat> availableSeats = showSeatRepository.findByShowIdWithSeatDetails(show.getShowId());
        assertEquals(30, availableSeats.size(), "All 30 physical seats must be generated as AVAILABLE for this show");

        // Verify tier pricing
        ShowSeat silverSeat = availableSeats.stream()
                .filter(s -> s.getSeat().getCategory().name().equals("SILVER"))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("220.00"), silverSeat.getPrice());

        // 4. Fetch Owner Analytics Dashboard
        TheatreAnalyticsResponseDTO analytics = theatreService.getTheatreAnalytics(theatre.getId(), ownerEmail, ownerRole);

        assertNotNull(analytics);
        assertEquals(theatre.getId(), analytics.getTheatreId());
        assertEquals(1L, analytics.getTotalShows(), "Should reflect 1 scheduled show");
        assertEquals(30L, analytics.getTotalSeatsCapacity(), "Capacity should reflect 30 seats");
        assertEquals(0L, analytics.getTotalTicketsSold(), "Initially 0 tickets sold");
        assertEquals(0.0, analytics.getOccupancyRate(), "Initially 0.0% occupancy");

        // 5. Security Check: An unauthorized user cannot view this owner's analytics
        assertThrows(IllegalArgumentException.class, () ->
                theatreService.getTheatreAnalytics(theatre.getId(), "unauthorized@gmail.com", "ROLE_USER")
        );
    }
}
