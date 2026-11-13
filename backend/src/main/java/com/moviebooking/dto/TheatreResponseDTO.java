package com.moviebooking.dto;

import com.moviebooking.entity.Theatre;
import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheatreResponseDTO {

    private Long id;
    private String name;
    private String location;
    private Integer totalScreens;
    private String ownerName;
    private String ownerEmail;
    private List<ScreenSummaryDTO> screens;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScreenSummaryDTO {
        private Long screenId;
        private String screenName;
        private Integer totalSeats;
    }

    public static TheatreResponseDTO fromEntity(Theatre theatre) {
        List<ScreenSummaryDTO> screenList = theatre.getScreens() == null ? Collections.emptyList() :
                theatre.getScreens().stream()
                        .map(s -> ScreenSummaryDTO.builder()
                                .screenId(s.getId())
                                .screenName(s.getName())
                                .totalSeats(s.getTotalSeats())
                                .build())
                        .toList();

        return TheatreResponseDTO.builder()
                .id(theatre.getId())
                .name(theatre.getName())
                .location(theatre.getLocation())
                .totalScreens(theatre.getTotalScreens())
                .ownerName(theatre.getOwner() != null ? theatre.getOwner().getName() : null)
                .ownerEmail(theatre.getOwner() != null ? theatre.getOwner().getGmail() : null)
                .screens(screenList)
                .build();
    }
}
