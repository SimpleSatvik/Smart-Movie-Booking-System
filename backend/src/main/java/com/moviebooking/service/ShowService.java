package com.moviebooking.service;

import com.moviebooking.dto.CreateShowRequestDTO;
import com.moviebooking.dto.SeatLayoutResponseDTO;
import com.moviebooking.dto.ShowResponseDTO;
import com.moviebooking.entity.*;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ScreenRepository screenRepository;
    private final MovieRepository movieRepository;
    private final SeatRepository seatRepository;

    /**
     * Schedules a new show on a screen and automatically populates all 30 ShowSeat records
     * as AVAILABLE so the show is immediately bookable.
     * Enforces ownership authorization: only the screen's theatre owner (or ADMIN) can schedule.
     */
    @Transactional
    public ShowResponseDTO createShow(CreateShowRequestDTO request, String currentUserEmail, String currentUserRole) {
        log.info("Scheduling show for movie ID: {} on screen ID: {} by user: {} ({})",
                request.getMovieId(), request.getScreenId(), currentUserEmail, currentUserRole);

        // Security check
        if (currentUserRole == null || (!currentUserRole.equals("ROLE_THEATRE_OWNER") && !currentUserRole.equals("ROLE_ADMIN"))) {
            throw new IllegalArgumentException("Access denied: Only Theatre Owners or Admins can schedule shows.");
        }

        Screen screen = null;
        if (request.getScreenId() != null) {
            screen = screenRepository.findById(request.getScreenId())
                    .orElseThrow(() -> new IllegalArgumentException("Screen not found with ID: " + request.getScreenId()));
        } else if (request.getTheatreId() != null) {
            List<Screen> screens = screenRepository.findByTheatreId(request.getTheatreId());
            if (screens.isEmpty()) {
                throw new IllegalArgumentException("No auditorium screens configured for theatre ID: " + request.getTheatreId());
            }
            if (request.getScreenName() != null && !request.getScreenName().isBlank()) {
                String targetName = request.getScreenName().trim();
                screen = screens.stream()
                        .filter(s -> s.getName().equalsIgnoreCase(targetName))
                        .findFirst()
                        .orElse(screens.get(0));
            } else {
                screen = screens.get(0);
            }
        } else {
            throw new IllegalArgumentException("Screen ID or Theatre ID is required to schedule a show.");
        }

        // Ownership verification (Admins can schedule anywhere, Owners only on their own screens)
        if (!"ROLE_ADMIN".equals(currentUserRole)) {
            if (screen.getTheatre().getOwner() == null ||
                    !screen.getTheatre().getOwner().getGmail().equalsIgnoreCase(currentUserEmail)) {
                throw new IllegalArgumentException("Access denied: You do not own the theatre containing Screen " + screen.getName());
            }
        }

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Movie not found with ID: " + request.getMovieId()));

        if (Boolean.TRUE.equals(movie.getIsDeleted())) {
            throw new IllegalArgumentException("Cannot schedule show: Movie '" + movie.getTitle() + "' has been archived by Administrator.");
        }

        // Auto-calculate end time if not explicitly provided
        LocalDateTime endTime = request.getEndTime() != null ?
                request.getEndTime() :
                request.getStartTime().plusMinutes(movie.getDuration() + 15); // +15 mins buffer

        Show show = Show.builder()
                .movie(movie)
                .screen(screen)
                .startTime(request.getStartTime())
                .endTime(endTime)
                .build();

        show = showRepository.save(show);

        // Fetch the 30 physical seats for this screen
        List<Seat> seats = seatRepository.findByScreenId(screen.getId());
        if (seats.isEmpty()) {
            throw new IllegalStateException("Screen " + screen.getName() + " has no physical seats configured.");
        }

        BigDecimal silverPrice = request.getSilverPrice() != null ? request.getSilverPrice() : BigDecimal.valueOf(200.00);
        BigDecimal goldPrice = request.getGoldPrice() != null ? request.getGoldPrice() : BigDecimal.valueOf(350.00);
        BigDecimal platinumPrice = request.getPlatinumPrice() != null ? request.getPlatinumPrice() : BigDecimal.valueOf(500.00);

        List<ShowSeat> showSeats = new ArrayList<>();
        for (Seat seat : seats) {
            BigDecimal price = switch (seat.getCategory()) {
                case SILVER -> silverPrice;
                case GOLD -> goldPrice;
                case PLATINUM -> platinumPrice;
                case VIP -> BigDecimal.valueOf(800.00);
            };

            showSeats.add(ShowSeat.builder()
                    .show(show)
                    .seat(seat)
                    .price(price)
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }

        showSeatRepository.saveAll(showSeats);
        show.setShowSeats(showSeats);

        log.info("Successfully scheduled Show ID: {} for movie '{}' on screen '{}' with {} bookable seats",
                show.getId(), movie.getTitle(), screen.getName(), showSeats.size());

        return ShowResponseDTO.fromEntity(show);
    }

    // 1. Get all scheduled future shows for a movie (completed past shows excluded)
    @Transactional(readOnly = true)
    public List<ShowResponseDTO> getShowsByMovie(Long movieId) {
        LocalDateTime now = LocalDateTime.now();
        return showRepository.findByMovieIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(movieId, now)
                .stream()
                .map(ShowResponseDTO::fromEntity)
                .toList();
    }

    // 2. Get future shows for a movie on a specific date (e.g. 2026-11-09)
    @Transactional(readOnly = true)
    public List<ShowResponseDTO> getShowsByMovieAndDate(Long movieId, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        if (date.isBefore(today)) {
            // Requested date is in the past: all shows on that date have concluded
            return List.of();
        }

        LocalDateTime startOfDay = date.isEqual(today) ? now : date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return showRepository.findByMovieIdAndStartTimeBetweenOrderByStartTimeAsc(movieId, startOfDay, endOfDay)
                .stream()
                .map(ShowResponseDTO::fromEntity)
                .toList();
    }

    // Get all scheduled shows for a specific theatre multiplex
    @Transactional(readOnly = true)
    public List<ShowResponseDTO> getShowsByTheatreId(Long theatreId) {
        return showRepository.findByScreenTheatreIdOrderByStartTimeAsc(theatreId)
                .stream()
                .map(ShowResponseDTO::fromEntity)
                .toList();
    }

    // 3. Get the interactive seat layout for a specific show
    @Transactional(readOnly = true)
    public List<SeatLayoutResponseDTO> getSeatLayoutForShow(Long showId) {
        // Verify show exists
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("Show not found with ID: " + showId));

        // Use our optimized JOIN FETCH query to pull all seats and their availability in 1 trip to DB
        List<ShowSeat> showSeats = showSeatRepository.findByShowIdWithSeatDetails(show.getId());

        return showSeats.stream()
                .map(SeatLayoutResponseDTO::fromEntity)
                .toList();
    }
}
