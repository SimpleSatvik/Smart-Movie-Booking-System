package com.moviebooking;

import com.moviebooking.dto.*;
import com.moviebooking.entity.EmailOtp;
import com.moviebooking.entity.OtpPurpose;
import com.moviebooking.entity.User;
import com.moviebooking.repository.EmailOtpRepository;
import com.moviebooking.repository.UserRepository;
import com.moviebooking.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OtpAuthTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailOtpRepository emailOtpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.moviebooking.service.OtpService otpService;

    @Test
    @DisplayName("Live Email Verification: Dispatches real OTP to user's personal inbox")
    void sendLiveEmailToUser() {
        otpService.generateAndSendOtp("satvikkapoor23859@gmail.com", OtpPurpose.REGISTRATION);
    }

    @Test
    @DisplayName("OTP Flow 1: Registration with OTP verification (Wrong OTP fails, Correct OTP succeeds)")
    void testRegistrationWithOtp() {
        String uniqueEmail = "satvik_test_" + System.currentTimeMillis() + "@gmail.com";

        // 1. Dispatch registration OTP
        authService.sendRegistrationOtp(uniqueEmail);

        // Fetch the generated OTP from repository
        EmailOtp otpEntity = emailOtpRepository
                .findTopByEmailIgnoreCaseAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(uniqueEmail, OtpPurpose.REGISTRATION)
                .orElseThrow();
        assertNotNull(otpEntity.getOtpCode());

        // 2. Attempt registration with wrong OTP -> must fail
        RegisterRequestDTO wrongRequest = RegisterRequestDTO.builder()
                .name("Test User")
                .gmail(uniqueEmail)
                .password("password123")
                .role("ROLE_USER")
                .otp("000000") // Wrong OTP
                .build();

        assertThrows(IllegalArgumentException.class, () -> authService.register(wrongRequest));

        // 3. Attempt registration with valid OTP -> must succeed
        RegisterRequestDTO validRequest = RegisterRequestDTO.builder()
                .name("Test User")
                .gmail(uniqueEmail)
                .password("password123")
                .role("ROLE_USER")
                .otp(otpEntity.getOtpCode())
                .build();

        AuthResponseDTO response = authService.register(validRequest);

        assertNotNull(response);
        assertNotNull(response.getToken(), "JWT token should be issued on successful registration");
        assertEquals(uniqueEmail, response.getGmail());
        assertEquals("ROLE_USER", response.getRole());

        // Verify OTP is marked as used
        EmailOtp usedOtp = emailOtpRepository.findById(otpEntity.getId()).orElseThrow();
        assertTrue(usedOtp.getIsUsed(), "OTP should be marked as used");
    }

    @Test
    @DisplayName("OTP Flow 2: Two-Factor (2FA) Login (Password check -> 2FA OTP dispatched -> verified -> JWT issued)")
    void testTwoFactorLogin() {
        String testEmail = "2fa_user_" + System.currentTimeMillis() + "@gmail.com";

        // Setup user directly
        authService.sendRegistrationOtp(testEmail);
        String regOtp = emailOtpRepository
                .findTopByEmailIgnoreCaseAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(testEmail, OtpPurpose.REGISTRATION)
                .orElseThrow().getOtpCode();

        authService.register(RegisterRequestDTO.builder()
                .name("2FA User")
                .gmail(testEmail)
                .password("secret123")
                .role("ROLE_THEATRE_OWNER")
                .otp(regOtp)
                .build());

        // Step 1: Login with password - directly logs in without OTP requirement
        LoginRequestDTO loginReq = LoginRequestDTO.builder()
                .gmail(testEmail)
                .password("secret123")
                .build();

        AuthResponseDTO step1 = authService.login(loginReq);
        assertFalse(step1.getRequires2fa(), "Login should not require 2FA OTP");
        assertNotNull(step1.getToken(), "JWT token should be issued directly upon valid password");
        assertEquals("ROLE_THEATRE_OWNER", step1.getRole());
    }

    @Test
    @DisplayName("OTP Flow 3: Forgot and Reset Password with OTP verification")
    void testForgotPasswordFlow() {
        String resetEmail = "reset_user_" + System.currentTimeMillis() + "@gmail.com";

        // Register user
        authService.sendRegistrationOtp(resetEmail);
        String regOtp = emailOtpRepository
                .findTopByEmailIgnoreCaseAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(resetEmail, OtpPurpose.REGISTRATION)
                .orElseThrow().getOtpCode();

        authService.register(RegisterRequestDTO.builder()
                .name("Reset User")
                .gmail(resetEmail)
                .password("oldPassword123")
                .otp(regOtp)
                .build());

        // 1. Request Forgot Password OTP
        authService.sendForgotPasswordOtp(resetEmail);

        String resetOtp = emailOtpRepository
                .findTopByEmailIgnoreCaseAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(resetEmail, OtpPurpose.PASSWORD_RESET)
                .orElseThrow().getOtpCode();

        // 2. Reset Password
        ResetPasswordRequestDTO resetReq = ResetPasswordRequestDTO.builder()
                .email(resetEmail)
                .otp(resetOtp)
                .newPassword("brandNewPassword456")
                .build();

        authService.resetPassword(resetReq);

        // 3. Old password should now fail
        LoginRequestDTO oldLogin = LoginRequestDTO.builder()
                .gmail(resetEmail)
                .password("oldPassword123")
                .build();
        assertThrows(IllegalArgumentException.class, () -> authService.login(oldLogin));

        // 4. New password should succeed directly without OTP
        LoginRequestDTO newLogin = LoginRequestDTO.builder()
                .gmail(resetEmail)
                .password("brandNewPassword456")
                .build();
        AuthResponseDTO newLoginResp = authService.login(newLogin);
        assertFalse(newLoginResp.getRequires2fa());
        assertNotNull(newLoginResp.getToken());
    }
}
