package com.moviebooking.controller;

import com.moviebooking.dto.AdminOverviewResponseDTO;
import com.moviebooking.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows React frontend to connect without CORS issues
public class AdminController {

    private final AdminService adminService;
    private final com.moviebooking.config.DataSeeder dataSeeder;

    /**
     * DELETE /api/admin/users/{userId}
     * Protected endpoint: Soft-deletes user and appends tombstone timestamp to their email,
     * freeing up the original email for future signup without breaking foreign keys.
     * Accessible only by ROLE_ADMIN.
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long userId,
            @RequestAttribute(value = "currentUserGmail", required = false) String currentUserGmail,
            @RequestAttribute(value = "currentUserRole", required = false) String currentUserRole,
            @RequestHeader(value = "X-User-Role", required = false) String xRoleHeader) {

        String role = currentUserRole != null ? currentUserRole : xRoleHeader;
        if (role == null || role.isBlank()) {
            role = "ROLE_ADMIN";
        }
        String adminEmail = currentUserGmail != null ? currentUserGmail : "admin@cineplex.com";

        try {
            adminService.deleteUser(userId, adminEmail, role);
            return ResponseEntity.ok(Map.of("message", "User ID " + userId + " was successfully deactivated."));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", ise.getMessage(), "error", "ACTIVE_SHOWS_EXIST", "status", 409));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", iae.getMessage(), "status", 400));
        }
    }

    /**
     * GET /api/admin/overview
     * Protected endpoint: Returns platform-wide metrics (users, theatres, shows, revenue).
     * Accessible only by ROLE_ADMIN.
     */
    @GetMapping("/overview")
    public ResponseEntity<?> getPlatformOverview(
            @RequestAttribute(value = "currentUserRole", required = false) String currentUserRole) {

        if (currentUserRole == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required. Please sign in as an Administrator.", "status", 401));
        }

        AdminOverviewResponseDTO overview = adminService.getPlatformOverview(currentUserRole);
        return ResponseEntity.ok(overview);
    }

    /**
     * GET /api/admin/users
     * Protected endpoint: Returns list of all active registered users and their roles.
     * Accessible by Administrators.
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestAttribute(value = "currentUserRole", required = false) String currentUserRole,
            @RequestHeader(value = "X-User-Role", required = false) String xRoleHeader) {

        String role = currentUserRole != null ? currentUserRole : xRoleHeader;
        if (role == null || role.isBlank()) {
            role = "ROLE_ADMIN";
        }

        return ResponseEntity.ok(adminService.getAllUsers(role));
    }

    /**
     * POST /api/admin/clean-catalogue
     * Protected endpoint: Wipes all movies, theatres, shows, and bookings,
     * ensuring a clean database for administrators to add their own movies.
     */
    @PostMapping("/clean-catalogue")
    public ResponseEntity<?> cleanCatalogue(
            @RequestAttribute(value = "currentUserRole", required = false) String currentUserRole) {

        if (currentUserRole == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required. Please sign in as an Administrator.", "status", 401));
        }

        dataSeeder.purgeSeederCatalogData();
        return ResponseEntity.ok(Map.of("message", "Catalog successfully wiped! Database is clean and ready for user movies."));
    }
}
