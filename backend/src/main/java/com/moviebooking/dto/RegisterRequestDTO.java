package com.moviebooking.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email address is required")
    @JsonAlias({"email", "gmail"})
    @JsonProperty("gmail")
    private String gmail;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Builder.Default
    private String role = "ROLE_USER";

    @NotBlank(message = "Verification OTP is required")
    private String otp;

    public String getCleanEmail() {
        return gmail != null ? gmail.trim().toLowerCase() : "";
    }
}
