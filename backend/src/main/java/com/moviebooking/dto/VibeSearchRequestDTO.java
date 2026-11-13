package com.moviebooking.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VibeSearchRequestDTO {

    private String vibe;   // e.g. "I want an adrenaline-pumping action movie with mass appeal"
    private String prompt; // Alias accepted from frontend

    public String getVibe() {
        if (vibe != null && !vibe.isBlank()) {
            return vibe.trim();
        }
        if (prompt != null && !prompt.isBlank()) {
            return prompt.trim();
        }
        return "";
    }
}
