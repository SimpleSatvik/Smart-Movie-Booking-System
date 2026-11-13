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
public class LoginRequestDTO {

    @NotBlank(message = "Email address is required")
    @JsonAlias({"email", "gmail"})
    @JsonProperty("gmail")
    private String gmail;

    @NotBlank(message = "Password is required")
    private String password;

    public String getCleanEmail() {
        return gmail != null ? gmail.trim().toLowerCase() : "";
    }
}
