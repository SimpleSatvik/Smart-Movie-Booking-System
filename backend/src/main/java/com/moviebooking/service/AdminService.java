package com.moviebooking.service;

import com.moviebooking.dto.AdminOverviewResponseDTO;
import com.moviebooking.dto.UserResponseDTO;
import com.moviebooking.entity.Theatre;
import com.moviebooking.entity.User;
import com.moviebooking.repository.BookingRepository;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.repository.TheatreRepository;
import com.moviebooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;

    /**
     * Admin Operation: Soft-deletes a user and unlinks their email address
     * (Tombstone pattern). Frees up their original email for re-registration
     * while preserving relational booking integrity.
     */
    @Transactional
    public void deleteUser(Long userId, String currentAdminGmail, String currentUserRole) {
        log.info("Admin {} attempting to soft-delete user ID: {}", currentAdminGmail, userId);

        if (currentUserRole != null && !currentUserRole.toUpperCase().contains("ADMIN")) {
            throw new IllegalArgumentException("Access denied: Only Administrators can delete user accounts.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new IllegalArgumentException("User account is already deleted.");
        }

        if (user.getGmail().equalsIgnoreCase(currentAdminGmail)) {
            throw new IllegalArgumentException("Safety restriction: Administrators cannot delete their own active account.");
        }

        // Operational Guard: Check if the user owns any theatres or has scheduled shows
        List<Theatre> ownedTheatres = theatreRepository.findTheatresByOwner(userId, user.getGmail());
        boolean isOwner = (user.getRole() != null && user.getRole().toUpperCase().contains("OWNER")) || !ownedTheatres.isEmpty();

        if (isOwner) {
            LocalDateTime now = LocalDateTime.now();
            long upcomingShows = 0;
            if (!ownedTheatres.isEmpty()) {
                List<Long> theatreIds = ownedTheatres.stream().map(Theatre::getId).toList();
                upcomingShows = showRepository.countUpcomingShowsByTheatreIds(theatreIds, now);
            }
            if (upcomingShows == 0) {
                upcomingShows = showRepository.countUpcomingShowsByTheatreOwner(userId, user.getGmail(), now);
            }

            log.info("Operational Guard: Found {} theatre(s) and {} upcoming/active show(s) for user {} (ID: {})",
                    ownedTheatres.size(), upcomingShows, user.getGmail(), userId);

            if (upcomingShows > 0) {
                String venueNames = ownedTheatres.isEmpty() 
                        ? "associated cinema venues" 
                        : ownedTheatres.stream().map(Theatre::getName).collect(java.util.stream.Collectors.joining(", "));
                throw new IllegalStateException("Cannot delete Cinema Owner '" + user.getName() + "': Their venue(s) (" 
                        + venueNames + ") currently have " + upcomingShows 
                        + " upcoming or active show(s) scheduled. Screening schedules must finish or be cancelled before this account can be deactivated.");
            }
        }

        // Soft-delete pattern: mark deleted & free up email
        user.setIsDeleted(true);
        user.setGmail(user.getGmail() + "_deleted_" + System.currentTimeMillis());
        userRepository.save(user);

        // Soft-delete orphaned theatres belonging to this deactivated owner
        if (!ownedTheatres.isEmpty()) {
            for (Theatre t : ownedTheatres) {
                t.setIsDeleted(true);
            }
            theatreRepository.saveAll(ownedTheatres);
            log.info("Soft-deleted {} orphaned theatre(s) for deactivated owner {}", ownedTheatres.size(), user.getGmail());
        }

        log.info("User ID: {} successfully soft-deleted by Admin {}", userId, currentAdminGmail);
    }

    /**
     * Admin Operation: Computes high-level platform-wide KPIs for the Admin Dashboard.
     */
    @Transactional(readOnly = true)
    public AdminOverviewResponseDTO getPlatformOverview(String currentUserRole) {
        if (!"ROLE_ADMIN".equals(currentUserRole)) {
            throw new IllegalArgumentException("Access denied: Only Administrators can view platform overview metrics.");
        }

        long totalUsers = userRepository.countByIsDeletedFalse();
        long totalTheatres = theatreRepository.countByIsDeletedFalse();
        long totalShows = showRepository.count();
        long totalMovies = movieRepository.countByIsDeletedFalse();
        long totalBookings = bookingRepository.count();
        BigDecimal platformRevenue = bookingRepository.calculateTotalPlatformRevenue();

        return AdminOverviewResponseDTO.builder()
                .totalUsers(totalUsers)
                .totalTheatres(totalTheatres)
                .totalShows(totalShows)
                .totalMovies(totalMovies)
                .totalBookings(totalBookings)
                .totalPlatformRevenue(platformRevenue != null ? platformRevenue : BigDecimal.ZERO)
                .build();
    }

    /**
     * Admin Operation: Returns list of all active registered users and their roles.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers(String currentUserRole) {
        if (currentUserRole != null && !currentUserRole.toUpperCase().contains("ADMIN")) {
            throw new IllegalArgumentException("Access denied: Only Administrators can view registered users.");
        }

        return userRepository.findByIsDeletedFalseOrderByIdAsc().stream()
                .map(u -> UserResponseDTO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getGmail())
                        .role(u.getRole())
                        .isDeleted(u.getIsDeleted())
                        .createdAt(u.getCreatedAt())
                        .build())
                .toList();
    }
}
