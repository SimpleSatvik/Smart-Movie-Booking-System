package com.moviebooking.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyLoginOtpRequestDTO {

    @NotBlank(message = "Email address is required")
    @JsonAlias({"email", "gmail"})
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "OTP verification code is required")
    private String otp;

    public String getCleanEmail() {
        return email != null ? email.trim().toLowerCase() : "";
    }
}
