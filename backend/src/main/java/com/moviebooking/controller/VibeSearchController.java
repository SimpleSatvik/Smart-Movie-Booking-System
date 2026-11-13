package com.moviebooking.controller;

import com.moviebooking.dto.VibeSearchRequestDTO;
import com.moviebooking.dto.VibeSearchResponseDTO;
import com.moviebooking.service.VibeSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows React frontend to call without CORS issues
public class VibeSearchController {

    private final VibeSearchService vibeSearchService;

    /**
     * POST /api/movies/vibe-search
     * Takes a natural-language description of how the user feels or what they want to watch,
     * uses Google Gemini AI to analyze the prompt against movies with upcoming shows,
     * and returns personalized movie recommendations with live showtimes.
     */
    @PostMapping("/vibe-search")
    public ResponseEntity<List<VibeSearchResponseDTO>> searchByVibe(
            @RequestBody VibeSearchRequestDTO request) {
        if (request == null || request.getVibe() == null || request.getVibe().isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Collections.emptyList());
        }
        List<VibeSearchResponseDTO> recommendations = vibeSearchService.searchByVibe(request);
        return ResponseEntity.ok(recommendations);
    }
}
