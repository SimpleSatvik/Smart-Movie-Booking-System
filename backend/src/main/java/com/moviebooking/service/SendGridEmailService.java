package com.moviebooking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class SendGridEmailService implements EmailService {

    private static final String SENDGRID_URL = "https://api.sendgrid.com/v3/mail/send";

    @Value("${sendgrid.api.key:}")
    private String apiKey;

    @Value("${sendgrid.from.email:noreply@moviebooking.com}")
    private String fromEmail;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        // Prominently log the OTP immediately so it is 100% accessible in local terminal
        log.info("=========================================================================");
        log.info("  [CINESYNC OTP DISPATCH]");
        log.info("  Recipient:    {}", toEmail);
        log.info("  Purpose:      {}", purpose);
        log.info("  Generated OTP: {}", otp);
        log.info("  Master Dev Code: 123456 (Accepted everywhere in dev mode)");
        log.info("=========================================================================");

        // If SendGrid API Key is not configured yet, fallback to Console Log
        if (apiKey == null || apiKey.isBlank() || apiKey.equalsIgnoreCase("mock")) {
            return;
        }

        // Dispatch email asynchronously so it never blocks the request thread or delays the UI
        CompletableFuture.runAsync(() -> {
            try {
                String subject = "Your Verification Code: " + otp + " - Smart Movie Booking";
                String htmlContent = String.format(
                        "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;'>"
                                + "<h2 style='color: #4F46E5;'>Smart Movie Booking System</h2>"
                                + "<p>Hello,</p>"
                                + "<p>You requested a one-time verification code for <strong>%s</strong>.</p>"
                                + "<div style='font-size: 28px; font-weight: bold; letter-spacing: 5px; color: #111827; background-color: #F3F4F6; padding: 15px; text-align: center; border-radius: 6px; margin: 20px 0;'>"
                                + "%s"
                                + "</div>"
                                + "<p style='color: #6B7280; font-size: 14px;'>This code is valid for <strong>5 minutes</strong>. If you did not make this request, please ignore this email.</p>"
                                + "</div>",
                        purpose, otp
                );

                // SendGrid JSON v3 structure
                Map<String, Object> payload = Map.of(
                        "personalizations", List.of(
                                Map.of("to", List.of(Map.of("email", toEmail)))
                        ),
                        "from", Map.of(
                                "email", fromEmail,
                                "name", "Smart Movie Booking"
                        ),
                        "subject", subject,
                        "content", List.of(
                                Map.of(
                                        "type", "text/html",
                                        "value", htmlContent
                                )
                        )
                );

                String requestBody = objectMapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SENDGRID_URL))
                        .header("Authorization", "Bearer " + apiKey.trim())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .timeout(Duration.ofSeconds(6))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 202) {
                    log.info("Successfully dispatched OTP email via SendGrid to: {}", toEmail);
                } else {
                    log.warn("SendGrid API returned status: {}. Body: {}. (Dev code 123456 remains valid)", 
                            response.statusCode(), response.body());
                }

            } catch (Exception e) {
                log.warn("SendGrid dispatch exception: {}. Fallback OTP code is: {}", e.getMessage(), otp);
            }
        });
    }
}
