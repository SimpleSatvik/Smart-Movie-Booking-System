package com.moviebooking.service;

import com.moviebooking.dto.CreateTheatreRequestDTO;
import com.moviebooking.dto.TheatreAnalyticsResponseDTO;
import com.moviebooking.dto.TheatreResponseDTO;
import com.moviebooking.entity.*;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TheatreService {

    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;

    /**
     * Creates a new theatre with screens and pre-populates each screen with 30 physical seats
     * (Row A: 10 Silver, Row B: 10 Gold, Row C: 10 Platinum).
     * Clears cached theatre catalogue so the new cinema appears immediately.
     */
    @Transactional
    @CacheEvict(value = "theatres", allEntries = true)
    public TheatreResponseDTO createTheatre(CreateTheatreRequestDTO request, String currentUserEmail, String currentUserRole) {
        log.info("Request to create theatre '{}' by user: {} with role: {}", request.getName(), currentUserEmail, currentUserRole);

        // Security role check
        if (currentUserRole == null || (!currentUserRole.equals("ROLE_THEATRE_OWNER") && !currentUserRole.equals("ROLE_ADMIN"))) {
            throw new IllegalArgumentException("Access denied: Only Theatre Owners or Admins can create theatres.");
        }

        User owner = userRepository.findByGmailIgnoreCase(currentUserEmail)
                .orElseThrow(() -> new IllegalArgumentException("Owner account not found: " + currentUserEmail));

        int screensCount = (request.getTotalScreens() != null && request.getTotalScreens() > 0) ? request.getTotalScreens() : 1;

        Theatre theatre = Theatre.builder()
                .name(request.getName().trim())
                .location(request.getLocation().trim())
                .totalScreens(screensCount)
                .owner(owner)
                .build();

        theatre = theatreRepository.save(theatre);

        // Auto-initialize each screen with 30 physical seats
        List<Screen> screenList = new ArrayList<>();
        for (int s = 1; s <= screensCount; s++) {
            Screen screen = Screen.builder()
                    .name("Audi " + s)
                    .totalSeats(30)
                    .theatre(theatre)
                    .build();

            List<Seat> seats = new ArrayList<>();
            createSeatsForRow(screen, "A", 10, SeatCategory.SILVER, seats);
            createSeatsForRow(screen, "B", 10, SeatCategory.GOLD, seats);
            createSeatsForRow(screen, "C", 10, SeatCategory.PLATINUM, seats);
            screen.setSeats(seats);

            Screen savedScreen = screenRepository.save(screen);
            screenList.add(savedScreen);
        }

        theatre.setScreens(screenList);
        log.info("Successfully created Theatre '{}' (ID: {}) with {} screens for owner {}",
                theatre.getName(), theatre.getId(), screensCount, owner.getGmail());

        return TheatreResponseDTO.fromEntity(theatre);
    }

    /**
     * Returns all theatres owned by the currently authenticated theatre owner.
     */
    @Transactional(readOnly = true)
    public List<TheatreResponseDTO> getMyTheatres(String ownerEmail) {
        if (ownerEmail == null || ownerEmail.isBlank()) {
            return List.of();
        }

        User owner = userRepository.findByGmailIgnoreCase(ownerEmail).orElse(null);
        List<Theatre> theatres;
        if (owner != null) {
            theatres = theatreRepository.findTheatresByOwner(owner.getId(), ownerEmail);
        } else {
            theatres = theatreRepository.findByOwnerGmailIgnoreCase(ownerEmail);
        }

        return theatres.stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .map(TheatreResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TheatreResponseDTO.ScreenSummaryDTO> getScreensByTheatreId(Long theatreId) {
        return screenRepository.findByTheatreId(theatreId).stream()
                .map(s -> TheatreResponseDTO.ScreenSummaryDTO.builder()
                        .screenId(s.getId())
                        .screenName(s.getName())
                        .totalSeats(s.getTotalSeats())
                        .build())
                .toList();
    }

    /**
     * Public endpoint to browse all theatres.
     * Cached in RAM for instant 0ms responses across all customer requests.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "theatres")
    public List<TheatreResponseDTO> getAllTheatres() {
        return theatreRepository.findByIsDeletedFalse()
                .stream()
                .map(TheatreResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Calculates revenue, tickets sold, and occupancy analytics for a specific theatre.
     * Enforces ownership authorization: only the theatre's owner (or ADMIN) can view.
     */
    @Transactional(readOnly = true)
    public TheatreAnalyticsResponseDTO getTheatreAnalytics(Long theatreId, String currentUserEmail, String currentUserRole) {
        log.info("Fetching analytics for theatre ID: {} by user: {} ({})", theatreId, currentUserEmail, currentUserRole);

        if (currentUserRole == null || (!currentUserRole.equals("ROLE_THEATRE_OWNER") && !currentUserRole.equals("ROLE_ADMIN"))) {
            throw new IllegalArgumentException("Access denied: Only Theatre Owners or Admins can view cinema analytics.");
        }

        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() -> new IllegalArgumentException("Theatre not found with ID: " + theatreId));

        if (!"ROLE_ADMIN".equals(currentUserRole)) {
            if (theatre.getOwner() == null || !theatre.getOwner().getGmail().equalsIgnoreCase(currentUserEmail)) {
                throw new IllegalArgumentException("Access denied: You do not own this theatre.");
            }
        }

        long totalShows = showRepository.countByScreenTheatreId(theatreId);
        long totalCapacity = showSeatRepository.countTotalSeatsByTheatreId(theatreId);
        long totalTicketsSold = showSeatRepository.countSeatsByTheatreIdAndStatus(theatreId, SeatStatus.BOOKED);
        BigDecimal totalRevenue = showSeatRepository.sumRevenueByTheatreIdAndStatus(theatreId, SeatStatus.BOOKED);

        double occupancyRate = 0.0;
        if (totalCapacity > 0) {
            occupancyRate = BigDecimal.valueOf((double) totalTicketsSold * 100.0 / totalCapacity)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return TheatreAnalyticsResponseDTO.builder()
                .theatreId(theatre.getId())
                .theatreName(theatre.getName())
                .location(theatre.getLocation())
                .totalScreens(theatre.getTotalScreens())
                .totalShows(totalShows)
                .totalSeatsCapacity(totalCapacity)
                .totalTicketsSold(totalTicketsSold)
                .totalRevenue(totalRevenue)
                .occupancyRate(occupancyRate)
                .build();
    }

    private void createSeatsForRow(Screen screen, String row, int count, SeatCategory category, List<Seat> list) {
        for (int i = 1; i <= count; i++) {
            list.add(Seat.builder()
                    .screen(screen)
                    .row(row)
                    .seatNumber(i)
                    .category(category)
                    .build());
        }
    }
}
