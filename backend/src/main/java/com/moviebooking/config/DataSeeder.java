package com.moviebooking.config;

import com.moviebooking.entity.*;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        // 1. Ensure partial unique index on resale_listings exists in PostgreSQL
        try {
            jdbcTemplate.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_resale_seat " +
                "ON resale_listings (sfas_id) WHERE status = 'ACTIVE'"
            );
            log.info("Verified partial unique index on resale_listings (sfas_id) WHERE status = 'ACTIVE'.");
        } catch (Exception e) {
            log.warn("Could not create partial unique index: {}", e.getMessage());
        }

        // 2. Ensure email_otps table exists if Hibernate hasn't auto-created it yet
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS email_otps (" +
                "otp_id BIGSERIAL PRIMARY KEY, " +
                "email VARCHAR(150) NOT NULL, " +
                "otp_code VARCHAR(10) NOT NULL, " +
                "purpose VARCHAR(30) NOT NULL, " +
                "expiry_time TIMESTAMP NOT NULL, " +
                "is_used BOOLEAN NOT NULL DEFAULT FALSE, " +
                "attempts INT NOT NULL DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            log.info("Verified email_otps table exists in PostgreSQL.");
        } catch (Exception e) {
            log.warn("Note on email_otps table check: {}", e.getMessage());
        }

        // 2b. Ensure is_deleted column on movies table exists with default false
        try {
            jdbcTemplate.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("UPDATE movies SET is_deleted = FALSE WHERE is_deleted IS NULL");
            log.info("Verified is_deleted column on movies table.");
        } catch (Exception e) {
            log.warn("Note on movies is_deleted column check: {}", e.getMessage());
        }

        // 2c. Ensure is_deleted column on theatres table exists with default false
        try {
            jdbcTemplate.execute("ALTER TABLE theatres ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("UPDATE theatres SET is_deleted = FALSE WHERE is_deleted IS NULL");
            log.info("Verified is_deleted column on theatres table.");
        } catch (Exception e) {
            log.warn("Note on theatres is_deleted column check: {}", e.getMessage());
        }

        // 2d. Ensure seat_summary column on bookings table exists
        try {
            jdbcTemplate.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS seat_summary VARCHAR(100)");
            log.info("Verified seat_summary column on bookings table.");
        } catch (Exception e) {
            log.warn("Note on bookings seat_summary column check: {}", e.getMessage());
        }

        // 2e. Update bookings_status_check to permit 'RESOLD' in PostgreSQL
        try {
            jdbcTemplate.execute("ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_status_check");
            jdbcTemplate.execute("ALTER TABLE bookings ADD CONSTRAINT bookings_status_check CHECK (status IN ('CONFIRMED', 'CANCELLED', 'RESOLD'))");
            log.info("Verified bookings_status_check constraint permits CONFIRMED, CANCELLED, RESOLD.");
        } catch (Exception e) {
            log.warn("Note on bookings_status_check constraint update: {}", e.getMessage());
        }

        // 2f. Ensure legacy full unique constraint on resale_listings.sfas_id is dropped in favor of partial index
        try {
            jdbcTemplate.execute("ALTER TABLE resale_listings DROP CONSTRAINT IF EXISTS uk_resale_listings_sfas_id");
            jdbcTemplate.execute("ALTER TABLE resale_listings DROP CONSTRAINT IF EXISTS resale_listings_sfas_id_key");
            log.info("Verified legacy unique constraint on resale_listings (sfas_id) dropped in favor of partial index.");
        } catch (Exception e) {
            log.debug("Note dropping legacy unique constraint on resale_listings.sfas_id: {}", e.getMessage());
        }

        // 3. Ensure base accounts exist and are verified for immediate login
        seedUsersIfMissing();

        // 4. Seed initial motion pictures ONLY if movie table is completely empty
        seedInitialMoviesIfEmpty();

        // 5. Seed default authentic Theatres, Screens, and Physical Seats if missing
        seedTheatresAndScreensIfMissing();

        // 6. Seed real future Shows & ShowSeats in PostgreSQL for any active movie lacking shows
        seedShowsIfMissing();
    }

    private void seedInitialMoviesIfEmpty() {
        try {
            if (movieRepository.count() == 0) {
                log.info("Catalog is empty. Seeding initial movie titles...");
                movieRepository.save(Movie.builder()
                        .title("Dune: Part Two")
                        .genre("Sci-Fi, Action")
                        .language("English")
                        .duration(166)
                        .mood("Epic & Grand")
                        .posterUrl("https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=800&q=80")
                        .build());

                movieRepository.save(Movie.builder()
                        .title("Interstellar")
                        .genre("Sci-Fi, Drama")
                        .language("English")
                        .duration(169)
                        .mood("Mind-Bending")
                        .posterUrl("https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80")
                        .build());

                movieRepository.save(Movie.builder()
                        .title("Jawan")
                        .genre("Action, Thriller")
                        .language("Hindi")
                        .duration(169)
                        .mood("Mass Action & Adrenaline")
                        .posterUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRdnT7OTN5iPqcTD9hozOIuIVG8IWbeFLcPMWDFybXLyw&s=10")
                        .build());

                movieRepository.save(Movie.builder()
                        .title("Gladiator II")
                        .genre("Action, Drama")
                        .language("English")
                        .duration(148)
                        .mood("Intense")
                        .posterUrl("https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?auto=format&fit=crop&w=800&q=80")
                        .build());

                movieRepository.save(Movie.builder()
                        .title("Spider-Man: Across the Spider-Verse")
                        .genre("Animation, Action")
                        .language("English")
                        .duration(140)
                        .mood("Adrenaline High")
                        .posterUrl("https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=800&q=80")
                        .build());
                log.info("Initial movie titles seeded successfully.");
            }

            // Synchronize and backfill accurate poster URLs for any existing movies (e.g. Jawan)
            List<Movie> existingMovies = movieRepository.findAll();
            for (Movie m : existingMovies) {
                String title = m.getTitle() != null ? m.getTitle().toLowerCase() : "";
                if (title.contains("jawan") && (m.getPosterUrl() == null || !m.getPosterUrl().contains("ANd9GcRdnT7OTN5iPqcTD9hozOIuIVG8IWbeFLcPMWDFybXLyw"))) {
                    m.setPosterUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRdnT7OTN5iPqcTD9hozOIuIVG8IWbeFLcPMWDFybXLyw&s=10");
                    movieRepository.save(m);
                    log.info("Synchronized poster URL for movie: Jawan");
                } else if (m.getPosterUrl() == null || m.getPosterUrl().isBlank() || m.getPosterUrl().contains("placeholder")) {
                    if (title.contains("dune")) {
                        m.setPosterUrl("https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=800&q=80");
                    } else if (title.contains("interstellar")) {
                        m.setPosterUrl("https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80");
                    } else if (title.contains("gladiator")) {
                        m.setPosterUrl("https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?auto=format&fit=crop&w=800&q=80");
                    } else if (title.contains("spider")) {
                        m.setPosterUrl("https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=800&q=80");
                    } else {
                        m.setPosterUrl("https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=800&q=80");
                    }
                    movieRepository.save(m);
                    log.info("Backfilled poster URL for movie: {}", m.getTitle());
                }
            }
        } catch (Exception e) {
            log.warn("Note during initial movie seeding: {}", e.getMessage());
        }
    }

    /**
     * Purges legacy seeded movies, shows, screens, and theatres to give the user
     * an unpolluted database where they can add their own movies.
     */
    public void purgeSeederCatalogData() {
        try {
            log.info("Purging legacy seeder catalog data (movies, theatres, shows, seats)...");

            // PostgreSQL cascading truncate if supported
            try {
                jdbcTemplate.execute(
                    "TRUNCATE TABLE payments, settlements, resale_listings, outing_expense_splits, " +
                    "outing_expenses, outing_members, outings, booking_seats, bookings, show_seats, " +
                    "shows, seats, screens, theatres, movies CASCADE"
                );
                log.info("Successfully truncated legacy catalog tables via CASCADE.");
                return;
            } catch (Exception truncateEx) {
                log.debug("Truncate cascade note: {}. Falling back to sequential deletes.", truncateEx.getMessage());
            }

            // Sequential fallback with foreign-key safe dependency order
            String[] tables = {
                "payments", "settlements", "booking_seats", "resale_listings",
                "outing_expense_splits", "outing_expenses", "outing_members", "outings",
                "bookings", "show_seats", "shows", "seats", "screens", "theatres", "movies"
            };
            for (String table : tables) {
                try {
                    jdbcTemplate.execute("DELETE FROM " + table + " WHERE 1=1");
                } catch (Exception te) {
                    log.debug("Note deleting from {}: {}", table, te.getMessage());
                }
            }
            log.info("Seeder catalog data cleared successfully! Database is ready for user-created movies and theatres.");
        } catch (Exception e) {
            log.warn("Note during catalog purge: {}", e.getMessage());
        }
    }

    private void seedUsersIfMissing() {
        upsertUser("satvik@gmail.com", "Satvik Kapoor", "password123", "ROLE_USER");
        upsertUser("varun@gmail.com", "Varun Sharma", "password123", "ROLE_USER");
        upsertUser("akshit@gmail.com", "Akshit Gupta", "password123", "ROLE_USER");
        upsertUser("owner@pvr.com", "Rajesh Sharma (PVR Owner)", "password123", "ROLE_THEATRE_OWNER");
        upsertUser("admin@cineplex.com", "Platform Admin", "admin123", "ROLE_ADMIN");
    }

    private void upsertUser(String email, String name, String rawPassword, String role) {
        try {
            userRepository.findByGmailIgnoreCase(email).ifPresentOrElse(
                user -> {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    user.setRole(role);
                    user.setIsDeleted(false);
                    userRepository.save(user);
                    log.info("Synchronized base account: {} with role {}", email, role);
                },
                () -> {
                    userRepository.save(User.builder()
                            .name(name)
                            .gmail(email)
                            .password(passwordEncoder.encode(rawPassword))
                            .role(role)
                            .isDeleted(false)
                            .build());
                    log.info("Created base account: {} with role {}", email, role);
                }
            );
        } catch (Exception e) {
            log.warn("Could not upsert user {}: {}", email, e.getMessage());
        }
    }

    @Transactional
    public void seedTheatresAndScreensIfMissing() {
        try {
            User owner = userRepository.findByGmailIgnoreCase("owner@pvr.com")
                    .orElseGet(() -> userRepository.save(User.builder()
                            .name("Rajesh Sharma (PVR Owner)")
                            .gmail("owner@pvr.com")
                            .password(passwordEncoder.encode("password123"))
                            .role("ROLE_THEATRE_OWNER")
                            .isDeleted(false)
                            .build()));

            List<Theatre> existingTheatres = theatreRepository.findByIsDeletedFalse();
            Theatre theatre;
            if (existingTheatres.isEmpty()) {
                theatre = theatreRepository.save(Theatre.builder()
                        .name("PVR Director's Cut, Delhi NCR")
                        .location("Select Citywalk, Saket")
                        .totalScreens(2)
                        .owner(owner)
                        .isDeleted(false)
                        .build());
                log.info("Seeded primary theatre: {}", theatre.getName());
            } else {
                theatre = existingTheatres.get(0);
            }

            List<Screen> screens = screenRepository.findByTheatreId(theatre.getId());
            if (screens.isEmpty()) {
                Screen screen1 = screenRepository.save(Screen.builder()
                        .name("Audi 1 (IMAX Laser)")
                        .totalSeats(30)
                        .theatre(theatre)
                        .build());

                Screen screen2 = screenRepository.save(Screen.builder()
                        .name("Audi 2 (Dolby Atmos)")
                        .totalSeats(30)
                        .theatre(theatre)
                        .build());

                screens = List.of(screen1, screen2);
                log.info("Seeded 2 screens for theatre {}", theatre.getName());
            }

            // Ensure physical seats exist for each screen (10 SILVER, 10 GOLD, 10 PLATINUM)
            for (Screen screen : screens) {
                List<Seat> seats = seatRepository.findByScreenId(screen.getId());
                if (seats.isEmpty()) {
                    List<Seat> newSeats = new ArrayList<>();
                    for (int i = 1; i <= 10; i++) {
                        newSeats.add(Seat.builder().screen(screen).row("A").seatNumber(i).category(SeatCategory.SILVER).build());
                    }
                    for (int i = 1; i <= 10; i++) {
                        newSeats.add(Seat.builder().screen(screen).row("B").seatNumber(i).category(SeatCategory.GOLD).build());
                    }
                    for (int i = 1; i <= 10; i++) {
                        newSeats.add(Seat.builder().screen(screen).row("C").seatNumber(i).category(SeatCategory.PLATINUM).build());
                    }
                    seatRepository.saveAll(newSeats);
                    log.info("Seeded 30 physical seats for Screen {}", screen.getName());
                }
            }
        } catch (Exception e) {
            log.warn("Note during theatre/screen seeding: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void seedShowsIfMissing() {
        try {
            List<Movie> activeMovies = movieRepository.findByIsDeletedFalse();
            if (activeMovies.isEmpty()) {
                return;
            }

            List<Theatre> theatres = theatreRepository.findByIsDeletedFalse();
            if (theatres.isEmpty()) {
                return;
            }

            List<Screen> screens = screenRepository.findByTheatreId(theatres.get(0).getId());
            if (screens.isEmpty()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            for (Movie movie : activeMovies) {
                long futureShowsCount = showRepository.countByMovieIdAndStartTimeGreaterThanEqual(movie.getId(), now);
                if (futureShowsCount == 0) {
                    log.info("Movie '{}' (ID: {}) has no scheduled future shows. Seeding authentic PostgreSQL shows...", movie.getTitle(), movie.getId());

                    LocalDateTime baseToday = now.isBefore(now.withHour(18).withMinute(0))
                            ? now.withHour(18).withMinute(30).withSecond(0).withNano(0)
                            : now.plusHours(2).withMinute(0).withSecond(0).withNano(0);

                    List<LocalDateTime> showTimes = List.of(
                            baseToday,
                            baseToday.plusHours(3).plusMinutes(30),
                            now.plusDays(1).withHour(14).withMinute(30).withSecond(0).withNano(0),
                            now.plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0),
                            now.plusDays(1).withHour(21).withMinute(30).withSecond(0).withNano(0)
                    );

                    int duration = movie.getDuration() != null && movie.getDuration() > 0 ? movie.getDuration() : 150;

                    for (int idx = 0; idx < showTimes.size(); idx++) {
                        Screen screen = screens.get(idx % screens.size());
                        LocalDateTime start = showTimes.get(idx);
                        LocalDateTime end = start.plusMinutes(duration + 15);

                        Show show = showRepository.save(Show.builder()
                                .movie(movie)
                                .screen(screen)
                                .startTime(start)
                                .endTime(end)
                                .build());

                        List<Seat> physicalSeats = seatRepository.findByScreenId(screen.getId());
                        List<ShowSeat> showSeats = new ArrayList<>();
                        for (Seat s : physicalSeats) {
                            BigDecimal price = switch (s.getCategory()) {
                                case SILVER -> BigDecimal.valueOf(200.00);
                                case GOLD -> BigDecimal.valueOf(350.00);
                                case PLATINUM -> BigDecimal.valueOf(500.00);
                                case VIP -> BigDecimal.valueOf(800.00);
                            };

                            showSeats.add(ShowSeat.builder()
                                    .show(show)
                                    .seat(s)
                                    .price(price)
                                    .status(SeatStatus.AVAILABLE)
                                    .build());
                        }
                        showSeatRepository.saveAll(showSeats);
                        log.info("Scheduled Show ID: {} for movie '{}' on screen '{}' at {} with {} bookable seats",
                                show.getId(), movie.getTitle(), screen.getName(), start, showSeats.size());
                    }
                }
            }

            // Ensure any existing future shows without seats get their ShowSeat records populated
            List<Show> upcomingShows = showRepository.findByStartTimeAfter(now);
            for (Show s : upcomingShows) {
                long seatCount = showSeatRepository.findByShowIdWithSeatDetails(s.getId()).size();
                if (seatCount == 0 && s.getScreen() != null) {
                    List<Seat> physicalSeats = seatRepository.findByScreenId(s.getScreen().getId());
                    if (!physicalSeats.isEmpty()) {
                        List<ShowSeat> showSeats = new ArrayList<>();
                        for (Seat seat : physicalSeats) {
                            BigDecimal price = switch (seat.getCategory()) {
                                case SILVER -> BigDecimal.valueOf(200.00);
                                case GOLD -> BigDecimal.valueOf(350.00);
                                case PLATINUM -> BigDecimal.valueOf(500.00);
                                case VIP -> BigDecimal.valueOf(800.00);
                            };
                            showSeats.add(ShowSeat.builder()
                                    .show(s)
                                    .seat(seat)
                                    .price(price)
                                    .status(SeatStatus.AVAILABLE)
                                    .build());
                        }
                        showSeatRepository.saveAll(showSeats);
                        log.info("Backfilled {} missing ShowSeat records for existing Show ID: {}", showSeats.size(), s.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Note during show seeding: {}", e.getMessage(), e);
        }
    }
}
