package com.moviebooking.controller;

import com.moviebooking.dto.*;
import com.moviebooking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows React frontend to connect without CORS errors
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/send-registration-otp?email=... (also accepts JSON body)
     * Sends a 6-digit verification code to the recipient email before signup.
     */
    @PostMapping("/send-registration-otp")
    public ResponseEntity<Map<String, String>> sendRegistrationOtp(
            @RequestParam(required = false) String email,
            @RequestBody(required = false) Map<String, Object> body) {
        String targetEmail = (email != null && !email.isBlank()) ? email :
                             (body != null && body.get("email") != null) ? body.get("email").toString() :
                             (body != null && body.get("gmail") != null) ? body.get("gmail").toString() : null;
        if (targetEmail == null || targetEmail.isBlank()) {
            throw new IllegalArgumentException("Email address is required to dispatch verification code");
        }
        authService.sendRegistrationOtp(targetEmail);
        return ResponseEntity.ok(Map.of("message", "Registration OTP sent successfully to " + targetEmail));
    }

    /**
     * POST /api/auth/register
     * Signs up a new Customer or Theatre Owner, verifies their registration OTP,
     * hashes password with BCrypt, and returns a signed JWT token.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        AuthResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Step 1 of Login: Authenticates email + password.
     * If valid, dispatches a 6-digit 2FA login code to their email.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/verify-login-otp
     * Step 2 of Login: Verifies the 2FA OTP code and returns the signed JWT session token.
     */
    @PostMapping("/verify-login-otp")
    public ResponseEntity<AuthResponseDTO> verifyLoginOtp(@Valid @RequestBody VerifyLoginOtpRequestDTO request) {
        AuthResponseDTO response = authService.verifyLoginOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/forgot-password?email=...
     * Dispatches a 6-digit password reset OTP to the user's email.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestParam String email) {
        authService.sendForgotPasswordOtp(email);
        return ResponseEntity.ok(Map.of("message", "Password reset OTP sent successfully to " + email));
    }

    /**
     * POST /api/auth/reset-password
     * Verifies the password reset OTP and sets a new BCrypt-hashed password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successful! You can now log in with your new password."));
    }

    /**
     * POST /api/auth/demo-login?role=ROLE_ADMIN
     * Returns an authentic signed JWT token and user profile for testing roles.
     */
    @PostMapping("/demo-login")
    public ResponseEntity<AuthResponseDTO> demoLogin(@RequestParam(defaultValue = "ROLE_USER") String role) {
        AuthResponseDTO response = authService.demoLogin(role);
        return ResponseEntity.ok(response);
    }
}
