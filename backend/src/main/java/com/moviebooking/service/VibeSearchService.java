package com.moviebooking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.dto.VibeSearchRequestDTO;
import com.moviebooking.dto.VibeSearchResponseDTO;
import com.moviebooking.dto.VibeSearchResponseDTO.UpcomingShowInfoDTO;
import com.moviebooking.entity.Movie;
import com.moviebooking.entity.Show;
import com.moviebooking.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VibeSearchService {

    private final ShowRepository showRepository;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    /**
     * Interprets a user's natural-language mood using Google Gemini AI,
     * filtering strictly to movies that have FUTURE scheduled shows available to book.
     */
    @Transactional(readOnly = true)
    public List<VibeSearchResponseDTO> searchByVibe(VibeSearchRequestDTO request) {
        log.info("Processing AI Vibe Search for query: '{}'", request.getVibe());

        // 1. Fetch only shows scheduled for the future
        List<Show> upcomingShows = showRepository.findByStartTimeAfter(LocalDateTime.now());
        if (upcomingShows.isEmpty()) {
            log.info("No upcoming future shows found for vibe search.");
            return Collections.emptyList();
        }

        // 2. Map shows by movie ID and extract unique movies that actually have shows
        Map<Long, List<UpcomingShowInfoDTO>> showsByMovieId = new HashMap<>();
        Map<Long, Movie> movieMap = new LinkedHashMap<>();

        for (Show show : upcomingShows) {
            Movie movie = show.getMovie();
            movieMap.putIfAbsent(movie.getId(), movie);

            showsByMovieId.computeIfAbsent(movie.getId(), k -> new ArrayList<>())
                    .add(UpcomingShowInfoDTO.builder()
                            .showId(show.getId())
                            .theatreName(show.getScreen().getTheatre().getName())
                            .screenName(show.getScreen().getName())
                            .startTime(show.getStartTime())
                            .endTime(show.getEndTime())
                            .build());
        }

        List<Movie> activeMovies = new ArrayList<>(movieMap.values());

        // 3. Query Gemini AI with active movies only
        try {
            return queryGemini(request.getVibe(), activeMovies, movieMap, showsByMovieId);
        } catch (Exception e) {
            log.warn("Gemini API call failed or timed out ({}). Falling back to smart keyword/mood matching.", e.getMessage());
            return fallbackMoodSearch(request.getVibe(), activeMovies, showsByMovieId);
        }
    }

    /**
     * Sends prompt to Google Gemini and parses the structured JSON recommendations.
     */
    private List<VibeSearchResponseDTO> queryGemini(
            String userVibe,
            List<Movie> movies,
            Map<Long, Movie> movieMap,
            Map<Long, List<UpcomingShowInfoDTO>> showsByMovieId) throws Exception {

        // Build lightweight movie catalog for the AI prompt
        List<Map<String, Object>> catalog = new ArrayList<>();
        for (Movie m : movies) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("title", m.getTitle());
            item.put("genre", m.getGenre());
            item.put("mood", m.getMood());
            catalog.add(item);
        }
        String catalogJson = objectMapper.writeValueAsString(catalog);

        String prompt = """
            You are an expert film concierge for a movie booking platform.
            The user is searching for a movie with this vibe/request: "%s"

            Here is the current cinema movie catalogue (only movies with upcoming shows):
            %s

            Analyze the user's request against each movie's genre, themes, and mood.
            CRITICAL ACCURACY RULES:
            1. ONLY recommend movies that genuinely match the user's requested vibe, genre, or mood.
            2. If the user asks for a genre, mood, or vibe that does NOT match ANY movie in the catalogue (e.g. user asks for 'horror', 'slasher', 'haunted house', 'romance', but the catalogue only has Sci-Fi, Animation, or Historical Drama), you MUST return an empty JSON array: []
            3. Do NOT force-fit or hallucinate matches. A Sci-Fi or Drama movie is NOT a Horror movie. Users expect an empty list rather than an irrelevant recommendation.
            4. Only return movies with a genuine confidenceScore >= 0.70.

            You MUST respond ONLY with a raw valid JSON array (no markdown code blocks, no backticks, no explanations outside JSON) in this exact schema:
            [
              {
                "movieId": 1,
                "confidenceScore": 0.95,
                "matchReason": "One or two sentences explaining why this movie genuinely matches the user's vibe."
              }
            ]
            """.formatted(userVibe, catalogJson);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        RestClient restClient = RestClient.create();
        String urlWithKey = geminiApiUrl + "?key=" + geminiApiKey;

        String rawResponse = restClient.post()
                .uri(urlWithKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        // Extract generated text from Gemini response structure: candidates[0].content.parts[0].text
        JsonNode rootNode = objectMapper.readTree(rawResponse);
        JsonNode candidates = rootNode.path("candidates");
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        String aiText = candidates.get(0).path("content").path("parts").get(0).path("text").asText();

        // Strip any markdown backticks if Gemini added them (e.g. ```json ... ```)
        String cleanJson = aiText.replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        JsonNode recommendationsArray = objectMapper.readTree(cleanJson);
        List<VibeSearchResponseDTO> results = new ArrayList<>();

        for (JsonNode rec : recommendationsArray) {
            Long movieId = rec.path("movieId").asLong();
            Movie movie = movieMap.get(movieId);
            if (movie != null) {
                results.add(VibeSearchResponseDTO.builder()
                        .movieId(movie.getId())
                        .movieTitle(movie.getTitle())
                        .genre(movie.getGenre())
                        .language(movie.getLanguage())
                        .mood(movie.getMood())
                        .matchReason(rec.path("matchReason").asText())
                        .confidenceScore(rec.path("confidenceScore").asDouble(0.90))
                        .upcomingShows(showsByMovieId.getOrDefault(movieId, Collections.emptyList()))
                        .build());
            }
        }

        return results;
    }

    /**
     * Graceful fallback matching in case of API network drops or rate limits.
     * Evaluates true semantic relevance and never force-fits unrelated films.
     */
    private List<VibeSearchResponseDTO> fallbackMoodSearch(
            String userVibe,
            List<Movie> movies,
            Map<Long, List<UpcomingShowInfoDTO>> showsByMovieId) {

        String lowerVibe = userVibe.toLowerCase();
        List<VibeSearchResponseDTO> results = new ArrayList<>();

        // If user specifically asked for genres/themes not in our catalogue (e.g. horror, slasher, zombie)
        Set<String> horrorKeywords = Set.of("horror", "scary", "slasher", "spooky", "gore", "jump scare", "ghost", "paranormal", "zombie");
        boolean isLookingForHorror = horrorKeywords.stream().anyMatch(lowerVibe::contains);

        for (Movie m : movies) {
            String mGenre = (m.getGenre() != null ? m.getGenre().toLowerCase() : "");
            String mMood = (m.getMood() != null ? m.getMood().toLowerCase() : "");

            // If user asked for horror and movie is not horror, strictly skip
            if (isLookingForHorror && !mGenre.contains("horror") && !mMood.contains("horror")) {
                continue;
            }

            boolean matchesMood = !mMood.isEmpty() && Arrays.stream(mMood.split(","))
                    .anyMatch(mood -> lowerVibe.contains(mood.trim()));
            boolean matchesGenre = !mGenre.isEmpty() && Arrays.stream(mGenre.split(","))
                    .anyMatch(genre -> lowerVibe.contains(genre.trim()));

            if (matchesMood || matchesGenre) {
                results.add(VibeSearchResponseDTO.builder()
                        .movieId(m.getId())
                        .movieTitle(m.getTitle())
                        .genre(m.getGenre())
                        .language(m.getLanguage())
                        .mood(m.getMood())
                        .matchReason("Matched based on cinema mood and genre relevance.")
                        .confidenceScore(0.85)
                        .upcomingShows(showsByMovieId.getOrDefault(m.getId(), Collections.emptyList()))
                        .build());
            }
        }

        // Return only true matches. If nothing matches, return empty list rather than forcing irrelevant movies.
        return results;
    }
}
