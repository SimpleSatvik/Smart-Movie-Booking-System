@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.cinesync.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavController
import com.cinesync.app.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

// =========================================================================
//  Movies Tab: Hero 3D Coverflow + "For You" + "Now in Theaters" + AI Vibe
// =========================================================================

@Composable
fun MoviesTab(nav: NavController) {
    val loader = rememberLoader { Net.api.movies() }
    var query by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var aiMode by remember { mutableStateOf(false) }
    var vibeResults by remember { mutableStateOf<List<VibeResult>?>(null) }
    var vibeBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val toast = rememberToast()

    fun runVibe(prompt: String = query) {
        if (prompt.isBlank()) return
        scope.launchSafe(toast, { vibeBusy = false }) {
            vibeBusy = true
            vibeResults = Net.api.vibeSearch(VibeRequest(prompt.trim()))
        }
    }

    LoadView(loader) { movies ->
        val genres = remember(movies) {
            movies.flatMap { m ->
                m.genre?.split(',', '/', '•')?.map { it.trim() }.orEmpty()
            }.distinct().sorted()
        }

        val filteredMovies = remember(movies, query, selectedGenre) {
            movies.filter { m ->
                (selectedGenre == null || m.genre?.contains(selectedGenre!!, ignoreCase = true) == true) &&
                        (query.isBlank() || m.title.contains(query, ignoreCase = true))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Floating Liquid Glass Search Bar
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = CineColors.GlassDarkSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        if (aiMode) CineColors.NeonPurple.copy(alpha = 0.6f) else CineColors.GlassBorder,
                                        CineColors.GlassBorderSubtle
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = CineColors.TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            TextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = {
                                    Text(
                                        text = if (aiMode) "Ask AI: “mind-bending sci-fi tonight”" else "Search movies, vibes…",
                                        color = CineColors.TextMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = CineColors.PrimaryCoral,
                                    focusedTextColor = CineColors.TextPrimary,
                                    unfocusedTextColor = CineColors.TextPrimary
                                ),
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { if (aiMode) runVibe() })
                            )

                            // AI Vibe Search Toggle Pill
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (aiMode) CineColors.NeonPurple.copy(alpha = 0.25f) else CineColors.GlassWhiteSubtle,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        aiMode = !aiMode
                                        vibeResults = null
                                    }
                                    .border(
                                        1.dp,
                                        if (aiMode) CineColors.NeonPurple.copy(alpha = 0.5f) else CineColors.GlassBorderSubtle,
                                        RoundedCornerShape(50)
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "AI Vibe",
                                        tint = if (aiMode) CineColors.NeonPurple else CineColors.LaserCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "AI Vibe",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (aiMode) CineColors.NeonPurple else CineColors.LaserCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Vibe Search Mode Content
            if (aiMode) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Instant Vibe Suggestions",
                            style = MaterialTheme.typography.labelMedium,
                            color = CineColors.TextSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val presets = listOf(
                                "Mind-bending sci-fi",
                                "Adrenaline mass action",
                                "Heartfelt emotional",
                                "Late night thriller"
                            )
                            items(presets) { p ->
                                SuggestionChip(
                                    onClick = {
                                        query = p
                                        runVibe(p)
                                    },
                                    label = { Text(p, style = MaterialTheme.typography.labelSmall) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = CineColors.GlassWhiteSubtle,
                                        labelColor = CineColors.TextPrimary
                                    ),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        enabled = true,
                                        borderColor = CineColors.GlassBorderSubtle
                                    )
                                )
                            }
                        }

                        LiquidButton(
                            text = if (vibeBusy) "Analyzing Cinema Vibes…" else "Find Movies For My Vibe",
                            icon = Icons.Default.AutoAwesome,
                            enabled = !vibeBusy && query.isNotBlank(),
                            onClick = { runVibe() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                val results = vibeResults
                when {
                    vibeBusy -> item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CineColors.NeonPurple)
                        }
                    }
                    results == null -> item {
                        EmptyState(
                            title = "AI Vibe Matcher",
                            body = "Tell CineSync how you feel or what themes you crave, and Google Gemini AI will match you with upcoming shows.",
                            icon = Icons.Default.AutoAwesome
                        )
                    }
                    results.isEmpty() -> item {
                        EmptyState(
                            title = "No Matching Shows",
                            body = "No current screenings matched this specific vibe. Try another description or browse the full catalogue below.",
                            icon = Icons.Default.Movie
                        )
                    }
                    else -> items(results, key = { it.movieId }) { v ->
                        VibeResultCard(v, nav)
                    }
                }
            } else {
                // 2. Genre / Format Glass Filter Chips
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            GlassFilterChip(
                                selected = selectedGenre == null,
                                text = "All Movies",
                                onClick = { selectedGenre = null }
                            )
                        }
                        item {
                            GlassFilterChip(
                                selected = selectedGenre == "IMAX",
                                text = "IMAX Laser",
                                icon = Icons.Default.Stars,
                                onClick = { selectedGenre = if (selectedGenre == "IMAX") null else "IMAX" }
                            )
                        }
                        items(genres) { g ->
                            GlassFilterChip(
                                selected = selectedGenre.equals(g, true),
                                text = g,
                                onClick = { selectedGenre = if (selectedGenre.equals(g, true)) null else g }
                            )
                        }
                    }
                }

                // 3. Featured Hero 3D Coverflow Carousel (if movies exist)
                if (movies.isNotEmpty() && query.isBlank() && selectedGenre == null) {
                    item {
                        FeaturedHeroCarousel(movies = movies, onMovieSelected = { m ->
                            nav.navigate("movie/${m.id}")
                        })
                    }
                }

                // 4. "For You" Section
                if (filteredMovies.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "For You",
                            subtitle = "Personalized selections based on your taste",
                            onSeeAll = { /* already in browse */ }
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredMovies, key = { "foryou-${it.id}" }) { m ->
                                ForYouMovieCard(movie = m, onClick = { nav.navigate("movie/${m.id}") })
                            }
                        }
                    }

                    // 5. "Now in Theaters" Section
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionHeader(
                            title = "Now in Theaters",
                            subtitle = "Browse showtimes across premium IMAX auditoriums",
                            onSeeAll = { /* browse */ }
                        )
                    }

                    // Grid-like rows for Now in Theaters
                    val chunked = filteredMovies.chunked(2)
                    items(chunked) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            for (m in pair) {
                                Box(modifier = Modifier.weight(1f)) {
                                    TheaterMovieCard(movie = m, onClick = { nav.navigate("movie/${m.id}") })
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    item {
                        EmptyState(
                            title = "No Movies Found",
                            body = "No titles match “$query”. Try picking a different genre or clearing filters."
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
//  Subcomponents: Hero 3D Coverflow, Movie Cards, Section Headers
// =========================================================================

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CineColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = CineColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "See all",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = CineColors.PrimaryCoral,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSeeAll)
                .padding(4.dp)
        )
    }
}

/**
 * 3D Parallax Coverflow Carousel (from Inspiration Screenshot Image 6 & 9)
 */
@Composable
private fun FeaturedHeroCarousel(
    movies: List<Movie>,
    onMovieSelected: (Movie) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { movies.size })

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 60.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) { page ->
            val movie = movies[page]
            val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    ).absoluteValue

            val scale = lerp(0.86f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
            val alpha = lerp(0.55f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
            val rotationY = (page - pagerState.currentPage - pagerState.currentPageOffsetFraction) * 12f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        this.rotationY = rotationY
                        cameraDistance = 14f * density
                    }
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { onMovieSelected(movie) }
            ) {
                // Movie Poster with gradient overlay
                Poster(
                    url = movie.posterUrl,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 26.dp
                )

                // Atmospheric dark gradient to enhance glass text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.85f)
                                ),
                                startY = 150f
                            )
                        )
                )

                // Top badges (Rating + Format)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rating Badge (Gold Star)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.border(0.8.dp, CineColors.GlassBorder, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = CineColors.RatingGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "8.9",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Format Badge (IMAX Laser)
                    GlassPill(
                        text = "IMAX Laser",
                        color = CineColors.LaserCyan
                    )
                }
            }
        }

        val activeMovie = movies.getOrNull(pagerState.currentPage) ?: movies.first()

        Spacer(Modifier.height(14.dp))

        // Release year & metadata
        Text(
            text = "2024",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = CineColors.LaserCyan
        )

        Spacer(Modifier.height(2.dp))

        // Large Title (support 2 lines cleanly)
        Text(
            text = activeMovie.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = CineColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(6.dp))

        // Tags Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val genreFirst = activeMovie.genre?.split(',', '/', '•')?.firstOrNull()?.trim() ?: "Action"
            GlassPill(text = genreFirst, color = CineColors.TextSecondary)
            activeMovie.duration?.let {
                GlassPill(text = "${it / 60}h ${it % 60}m", color = CineColors.TextSecondary)
            }
            GlassPill(text = "★ 8.9", color = CineColors.RatingGold)
            GlassPill(text = "U/A 16+", color = CineColors.TextSecondary)
        }

        Spacer(Modifier.height(10.dp))

        // Pager Dot Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(movies.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .height(5.dp)
                        .width(if (isSelected) 22.dp else 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSelected) CineColors.PrimaryCoral else CineColors.GlassWhiteMedium
                        )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Primary "Book Tickets" CTA
        LiquidButton(
            text = "Book Tickets",
            icon = Icons.Default.ConfirmationNumber,
            onClick = { onMovieSelected(activeMovie) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
    }
}

/**
 * "For You" Card: Tall vertical aspect ratio with rating badge and elegant typography
 */
@Composable
private fun ForYouMovieCard(
    movie: Movie,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(168.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp)
        ) {
            Poster(
                url = movie.posterUrl,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 20.dp
            )

            // Rating Badge top-right
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .border(0.8.dp, CineColors.GlassBorder, RoundedCornerShape(10.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = CineColors.RatingGold,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "8.6",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = movie.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CineColors.TextPrimary,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val genre = movie.genre?.split(',', '•')?.firstOrNull()?.trim() ?: "Action"
            Text(
                text = "$genre • 2024",
                style = MaterialTheme.typography.labelSmall,
                color = CineColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            movie.duration?.let {
                Text(
                    text = "${it / 60}h ${it % 60}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = CineColors.TextMuted
                )
            }
        }
    }
}

/**
 * "Now in Theaters" 2-Column Grid Card with Frosted Bottom Details
 */
@Composable
private fun TheaterMovieCard(
    movie: Movie,
    onClick: () -> Unit
) {
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Poster(
                url = movie.posterUrl,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 16.dp
            )

            // Rating badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .border(0.8.dp, CineColors.GlassBorder, RoundedCornerShape(10.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = CineColors.RatingGold,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "8.8",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = movie.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CineColors.TextPrimary,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = listOfNotNull(movie.genre, movie.language).joinToString(" • "),
            style = MaterialTheme.typography.labelSmall,
            color = CineColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GlassFilterChip(
    selected: Boolean,
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) CineColors.PrimaryCoral.copy(alpha = 0.22f) else CineColors.GlassDarkSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        if (selected) CineColors.PrimaryCoral else CineColors.GlassBorder,
                        if (selected) CineColors.PrimaryDark.copy(alpha = 0.3f) else CineColors.GlassBorderSubtle
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) CineColors.PrimaryCoral else CineColors.TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else CineColors.TextSecondary
            )
        }
    }
}

@Composable
private fun VibeResultCard(r: VibeResult, nav: NavController) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        borderColor = CineColors.NeonPurple.copy(alpha = 0.35f),
        onClick = { nav.navigate("movie/${r.movieId}") }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = r.movieTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CineColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            r.confidenceScore?.let {
                GlassPill(
                    text = "${(it * 100).toInt()}% Vibe Match",
                    color = CineColors.NeonPurple
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = listOfNotNull(r.genre, r.language, r.mood).joinToString(" • "),
            style = MaterialTheme.typography.labelSmall,
            color = CineColors.LaserCyan
        )

        r.matchReason?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = CineColors.TextSecondary
            )
        }

        val shows = r.upcomingShows.orEmpty()
        if (shows.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Available Showtimes:",
                style = MaterialTheme.typography.labelSmall,
                color = CineColors.TextMuted
            )
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shows) { s ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CineColors.GlassWhiteSubtle,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                Net.showCache[s.showId] = Show(
                                    s.showId, r.movieId, r.movieTitle, s.theatreName, null, s.screenName, s.startTime, s.endTime
                                )
                                nav.navigate("seats/${s.showId}")
                            }
                            .border(1.dp, CineColors.GlassBorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "${fmtShortTime(s.startTime)} · ${s.theatreName ?: "Cinema"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = CineColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
//  Movie Detail & Showtimes Screen
// =========================================================================

@Composable
fun MovieDetailScreen(movieId: Long, nav: NavController) {
    val movieL = rememberLoader(movieId) { Net.api.movie(movieId) }
    val showsL = rememberLoader(movieId) {
        Net.api.showsForMovie(movieId).also { list ->
            list.forEach { Net.showCache[it.showId] = it }
        }
    }

    ScreenScaffold(
        title = (movieL.state as? UiState.Data)?.value?.title ?: "Movie Details",
        onBack = { nav.popBackStack() },
        badge = "IMAX Laser",
        badgeColor = CineColors.LaserCyan
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            LoadView(movieL) { m ->
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Movie Header Banner
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Poster(
                                    url = m.posterUrl,
                                    modifier = Modifier
                                        .width(110.dp)
                                        .aspectRatio(2f / 3f),
                                    cornerRadius = 16.dp
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = m.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = CineColors.TextPrimary
                                    )
                                    Text(
                                        text = listOfNotNull(m.genre, m.language).joinToString(" • "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CineColors.TextSecondary
                                    )
                                    m.duration?.let {
                                        Text(
                                            text = "${it / 60}h ${it % 60}m (${it} mins)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = CineColors.TextMuted
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        GlassPill(text = "★ 8.9", color = CineColors.RatingGold)
                                        m.mood?.let { GlassPill(text = it, color = CineColors.NeonPurple) }
                                    }
                                }
                            }
                        }
                    }

                    // Auditorium Tag
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = CineColors.LaserCyan.copy(alpha = 0.35f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(CineColors.LaserCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Hd,
                                        contentDescription = null,
                                        tint = CineColors.LaserCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "PVR INOX • Laser IMAX Auditorium",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CineColors.TextPrimary
                                    )
                                    Text(
                                        text = "Massive 1.43:1 aspect ratio with 12-channel spatial audio",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CineColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Showtimes Header
                    item {
                        Text(
                            text = "Select Showtime",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CineColors.TextPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    when (val s = showsL.state) {
                        is UiState.Loading -> item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = CineColors.PrimaryCoral)
                            }
                        }
                        is UiState.Error -> item {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                        }
                        is UiState.Data -> {
                            if (s.value.isEmpty()) {
                                item {
                                    EmptyState(
                                        title = "No Upcoming Shows",
                                        body = "New screenings will be scheduled soon for ${m.title}."
                                    )
                                }
                            }
                            // Shows grouped by Date
                            s.value.groupBy { it.startTime?.take(10) ?: "" }.forEach { (dateKey, list) ->
                                item {
                                    val dateLabel = runCatching {
                                        val d = LocalDate.parse(dateKey)
                                        val today = LocalDate.now()
                                        when {
                                            d.isEqual(today) -> "Today, ${d.format(DateTimeFormatter.ofPattern("d MMM"))}"
                                            d.isEqual(today.plusDays(1)) -> "Tomorrow, ${d.format(DateTimeFormatter.ofPattern("d MMM"))}"
                                            else -> d.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
                                        }
                                    }.getOrDefault(dateKey)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(CineColors.LaserCyan)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = dateLabel,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CineColors.LaserCyan
                                        )
                                    }
                                }

                                // Group shows by Theatre for a true cinematic layout
                                val byTheatre = list.groupBy { it.theatreName ?: "Cinema" }
                                byTheatre.forEach { (theatreName, theatreShows) ->
                                    item(key = "${dateKey}_$theatreName") {
                                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                                            // Theatre Header
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(CineColors.PrimaryCoral.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.ConfirmationNumber,
                                                        contentDescription = null,
                                                        tint = CineColors.PrimaryCoral,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = theatreName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CineColors.TextPrimary
                                                    )
                                                    Text(
                                                        text = listOfNotNull(
                                                            theatreShows.firstOrNull()?.screenName,
                                                            theatreShows.firstOrNull()?.theatreLocation
                                                        ).joinToString(" • "),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = CineColors.TextSecondary
                                                    )
                                                }
                                                GlassPill(
                                                    text = "Laser IMAX",
                                                    color = CineColors.LaserCyan
                                                )
                                            }

                                            Spacer(Modifier.height(14.dp))
                                            HorizontalDivider(color = CineColors.GlassBorderSubtle)
                                            Spacer(Modifier.height(12.dp))

                                            Text(
                                                text = "AVAILABLE SHOWTIMES",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp,
                                                color = CineColors.TextMuted
                                            )

                                            Spacer(Modifier.height(10.dp))

                                            // Showtime Pills Grid
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                theatreShows.forEach { show ->
                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = Color(0x331E293B),
                                                        modifier = Modifier
                                                            .border(
                                                                width = 1.dp,
                                                                color = CineColors.GlassBorderSubtle,
                                                                shape = RoundedCornerShape(14.dp)
                                                            )
                                                            .clickable {
                                                                Net.showCache[show.showId] = show
                                                                nav.navigate("seats/${show.showId}")
                                                            }
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Text(
                                                                text = fmtShortTime(show.startTime),
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = CineColors.PrimaryGlow
                                                            )
                                                            Spacer(Modifier.height(2.dp))
                                                            Text(
                                                                text = show.screenName ?: "Screen",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontSize = 11.sp,
                                                                color = CineColors.TextSecondary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
