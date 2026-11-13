package com.moviebooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOutingRequestDTO {

    private Long bookingId;

    private String bookingReference;

    @NotBlank(message = "Outing name is required")
    private String name; // e.g. "Jawan with Friends"

    private List<String> memberEmails;

    public List<String> getMemberEmails() {
        return memberEmails != null ? memberEmails : new ArrayList<>();
    }
}
