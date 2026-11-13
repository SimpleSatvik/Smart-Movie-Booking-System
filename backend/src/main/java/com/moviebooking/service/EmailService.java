package com.moviebooking.service;

public interface EmailService {

    /**
     * Sends a 6-digit OTP to the recipient email.
     *
     * @param toEmail recipient's email address
     * @param otp     6-digit verification code
     * @param purpose human-readable reason (e.g., "Registration", "Two-Factor Login", "Password Reset")
     */
    void sendOtpEmail(String toEmail, String otp, String purpose);
}
