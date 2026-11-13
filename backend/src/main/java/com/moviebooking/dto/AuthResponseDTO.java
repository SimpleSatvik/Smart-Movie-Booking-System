package com.moviebooking.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {

    private Long userId;
    private String name;
    private String gmail;

    // e.g. "ROLE_USER", "ROLE_THEATRE_OWNER", "ROLE_ADMIN"
    private String role;

    // Signed JWT token for subsequent authenticated API requests
    private String token;

    // True when 2FA code has been dispatched to email and user must verify it
    private Boolean requires2fa;

    // Optional user-facing greeting or token message
    private String message;
}
