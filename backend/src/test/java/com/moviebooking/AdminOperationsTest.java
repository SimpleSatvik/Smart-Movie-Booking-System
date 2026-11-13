package com.moviebooking;

import com.moviebooking.dto.AdminOverviewResponseDTO;
import com.moviebooking.dto.CreateMovieRequestDTO;
import com.moviebooking.dto.MovieResponseDTO;
import com.moviebooking.dto.RegisterRequestDTO;
import com.moviebooking.entity.EmailOtp;
import com.moviebooking.entity.OtpPurpose;
import com.moviebooking.entity.User;
import com.moviebooking.repository.EmailOtpRepository;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.UserRepository;
import com.moviebooking.service.AdminService;
import com.moviebooking.service.AuthService;
import com.moviebooking.service.MovieService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AdminOperationsTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailOtpRepository emailOtpRepository;

    @Test
    @DisplayName("Admin Operation 1: Add canonical movie with cover poster URL (Admin succeeds, Non-Admin blocked)")
    void testAdminAddMovie() {
        String adminRole = "ROLE_ADMIN";
        String userRole = "ROLE_USER";

        CreateMovieRequestDTO movieReq = CreateMovieRequestDTO.builder()
                .title("Interstellar " + System.currentTimeMillis())
                .genre("Sci-Fi, Adventure")
                .language("English")
                .duration(169)
                .mood("awe-inspiring")
                .posterUrl("https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg")
                .build();

        // 1. Non-admin must be blocked
        assertThrows(IllegalArgumentException.class, () ->
                movieService.createMovie(movieReq, userRole)
        );

        // 2. Admin successfully adds movie
        MovieResponseDTO created = movieService.createMovie(movieReq, adminRole);
        assertNotNull(created.getId());
        assertEquals("Sci-Fi, Adventure", created.getGenre());
        assertEquals("https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg", created.getPosterUrl());

        // Verify in DB
        assertTrue(movieRepository.findById(created.getId()).isPresent());
    }

    @Test
    @DisplayName("Admin Operation 2: User Soft-Delete & Tombstone Email Unlinking (Freeing original email)")
    void testAdminSoftDeleteUserAndEmailFreeing() {
        String originalEmail = "tobedeleted_" + System.currentTimeMillis() + "@gmail.com";

        // 1. Register a test user with OTP
        authService.sendRegistrationOtp(originalEmail);
        EmailOtp otp = emailOtpRepository
                .findTopByEmailIgnoreCaseAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(originalEmail, OtpPurpose.REGISTRATION)
                .orElseThrow();

        var regResponse = authService.register(RegisterRequestDTO.builder()
                .name("Delete Me")
                .gmail(originalEmail)
                .password("password123")
                .role("ROLE_USER")
                .otp(otp.getOtpCode())
                .build());

        Long userId = regResponse.getUserId();

        // 2. Admin soft-deletes this user
        adminService.deleteUser(userId, "admin@cineplex.com", "ROLE_ADMIN");

        // Verify in DB that isDeleted=true and email was tombstoned
        User deletedUser = userRepository.findById(userId).orElseThrow();
        assertTrue(deletedUser.getIsDeleted(), "isDeleted must be set to true");
        assertTrue(deletedUser.getGmail().startsWith(originalEmail + "_deleted_"),
                "Email should have tombstone timestamp appended");

        // 3. Verify original email is now completely FREE: user can re-register with it!
        authService.sendRegistrationOtp(originalEmail);
        EmailOtp reOtp = emailOtpRepository
                .findTopByEmailIgnoreCaseAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(originalEmail, OtpPurpose.REGISTRATION)
                .orElseThrow();

        var reRegisterResponse = authService.register(RegisterRequestDTO.builder()
                .name("Delete Me Returned")
                .gmail(originalEmail)
                .password("password123")
                .role("ROLE_USER")
                .otp(reOtp.getOtpCode())
                .build());

        assertNotNull(reRegisterResponse.getUserId());
        assertNotEquals(userId, reRegisterResponse.getUserId(), "New account gets fresh ID");
        assertEquals(originalEmail, reRegisterResponse.getGmail(), "User successfully registered with original email again");
    }

    @Test
    @DisplayName("Admin Operation 3: Platform Overview Metrics (Admin succeeds, Non-Admin blocked)")
    void testPlatformOverview() {
        // Non-admin blocked
        assertThrows(IllegalArgumentException.class, () ->
                adminService.getPlatformOverview("ROLE_USER")
        );

        // Admin retrieves KPIs
        AdminOverviewResponseDTO overview = adminService.getPlatformOverview("ROLE_ADMIN");
        assertNotNull(overview);
        assertTrue(overview.getTotalUsers() >= 1, "Should count active users");
        assertNotNull(overview.getTotalPlatformRevenue(), "Revenue should not be null");
    }

    @Test
    @DisplayName("Admin Operation 4: Get All Users and Selectively Delete Movie")
    void testGetAllUsersAndDeleteMovie() {
        // 1. Get users list
        assertThrows(IllegalArgumentException.class, () -> adminService.getAllUsers("ROLE_USER"));
        var users = adminService.getAllUsers("ROLE_ADMIN");
        assertNotNull(users);
        assertFalse(users.isEmpty(), "Users list should contain active accounts");

        // 2. Create movie then selectively delete it
        CreateMovieRequestDTO movieReq = CreateMovieRequestDTO.builder()
                .title("Test Deletable Movie " + System.currentTimeMillis())
                .genre("Action")
                .language("English")
                .duration(120)
                .build();
        MovieResponseDTO created = movieService.createMovie(movieReq, "ROLE_ADMIN");
        assertNotNull(created.getId());

        // Non-admin cannot delete movie
        assertThrows(IllegalArgumentException.class, () -> movieService.deleteMovie(created.getId(), "ROLE_USER"));

        // Admin can delete movie
        movieService.deleteMovie(created.getId(), "ROLE_ADMIN");
        assertFalse(movieRepository.findById(created.getId()).isPresent(), "Movie should be deleted");
    }
}
