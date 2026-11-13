package com.moviebooking.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTheatreRequestDTO {

    @NotBlank(message = "Theatre name is required")
    private String name;

    @NotBlank(message = "Location/City is required")
    @JsonAlias({"city", "location"})
    @JsonProperty("location")
    private String location;

    @Min(value = 1, message = "Theatre must have at least 1 screen")
    @Builder.Default
    private Integer totalScreens = 1;
}
