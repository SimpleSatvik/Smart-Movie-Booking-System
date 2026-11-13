package com.moviebooking;

import com.moviebooking.dto.CreateMovieRequestDTO;
import com.moviebooking.dto.MovieResponseDTO;
import com.moviebooking.service.MovieService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CachingTest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("Spring Cache: Movie catalogue is cached in RAM and evicted on new movie creation")
    void testMovieCatalogueCachingAndEviction() {
        // 1. Initial Call: Populates the cache
        List<MovieResponseDTO> firstCall = movieService.getAllMovies();
        assertNotNull(firstCall);

        // Verify cache contains the "movies" entry
        var moviesCache = cacheManager.getCache("movies");
        assertNotNull(moviesCache);
        assertNotNull(moviesCache.get(org.springframework.cache.interceptor.SimpleKey.EMPTY), "Cache must hold the movie list");

        // 2. Second Call: Serves directly from RAM cache (identical instance/data)
        List<MovieResponseDTO> secondCall = movieService.getAllMovies();
        assertEquals(firstCall.size(), secondCall.size());

        // 3. Admin creates a new movie -> Must trigger @CacheEvict
        CreateMovieRequestDTO newMovie = CreateMovieRequestDTO.builder()
                .title("Cache Test Movie " + System.currentTimeMillis())
                .genre("Thriller")
                .language("English")
                .duration(120)
                .mood("suspenseful")
                .build();

        movieService.createMovie(newMovie, "ROLE_ADMIN");

        // 4. Verify cache was cleared by @CacheEvict
        assertNull(moviesCache.get(org.springframework.cache.interceptor.SimpleKey.EMPTY), "Cache must be cleared after movie creation");

        // 5. Subsequent call: Re-fetches fresh data and includes the new movie
        List<MovieResponseDTO> thirdCall = movieService.getAllMovies();
        assertEquals(firstCall.size() + 1, thirdCall.size(), "Fresh catalogue must include newly created movie");
    }
}
