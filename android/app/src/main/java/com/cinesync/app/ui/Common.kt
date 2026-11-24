@file:OptIn(ExperimentalMaterial3Api::class)

package com.cinesync.app.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinesync.app.data.errorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

// =========================================================================
//  Liquid Glass UI Design System & Ambient Background
// =========================================================================

/**
 * Organic, continuous ambient atmospheric glow background with parallax support.
 * Renders subtle floating light orbs (cinematic coral and neon cyan/indigo) that shift
 * as the user interacts and scrolls, creating genuine liquid glass refraction.
 */
@Composable
fun AtmosphericBackground(
    modifier: Modifier = Modifier,
    parallaxY: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AtmosphericDrift")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineColors.VoidDark)
    ) {
        // Ambient glow canvas with fluid radial lighting and dynamic drift
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val rad1 = Math.toRadians(phase1.toDouble())
            val rad2 = Math.toRadians(phase2.toDouble())
            val dx1 = (cos(rad1) * 36f).toFloat()
            val dy1 = (sin(rad1) * 26f).toFloat()
            val dx2 = (sin(rad2) * 32f).toFloat()
            val dy2 = (cos(rad2) * 38f).toFloat()

            // Top-right warm coral ambient glow
            val coralCenter = Offset(width * 0.85f + dx1, height * 0.12f + dy1 - parallaxY * 0.2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CineColors.PrimaryCoral.copy(alpha = 0.20f),
                        CineColors.PrimaryCoral.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = coralCenter,
                    radius = width * 0.8f
                ),
                center = coralCenter,
                radius = width * 0.8f
            )

            // Middle-left laser cyan atmospheric glow
            val cyanCenter = Offset(width * 0.1f + dx2, height * 0.45f + dy2 - parallaxY * 0.35f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CineColors.LaserCyan.copy(alpha = 0.14f),
                        CineColors.LaserBlue.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = cyanCenter,
                    radius = width * 0.7f
                ),
                center = cyanCenter,
                radius = width * 0.7f
            )

            // Bottom-right deep neon purple glow
            val purpleCenter = Offset(width * 0.9f - dx1 * 0.7f, height * 0.82f - dy2 * 0.7f - parallaxY * 0.15f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CineColors.NeonPurple.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = purpleCenter,
                    radius = width * 0.75f
                ),
                center = purpleCenter,
                radius = width * 0.75f
            )
        }

        content()
    }
}

/**
 * Liquid Glass Card Container:
 * Features frosted translucent surface, specular rim border gradient, and smooth squircle corners.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = CineColors.GlassDarkSurface,
    borderColor: Color = CineColors.GlassBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = onClick
        )
    } else {
        modifier
    }

    Surface(
        modifier = cardModifier
            .clip(shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.45f),
                        borderColor.copy(alpha = 0.15f),
                        borderColor.copy(alpha = 0.04f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                ),
                shape = shape
            ),
        shape = shape,
        color = backgroundColor,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Reusable Glass Pill Badge with specular sheen and frosted backdrop.
 */
@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = CineColors.LaserCyan,
    active: Boolean = true
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(
                width = 0.8.dp,
                brush = Brush.horizontalGradient(
                    listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0.15f))
                ),
                shape = RoundedCornerShape(50)
            ),
        color = if (active) color.copy(alpha = 0.14f) else CineColors.GlassWhiteSubtle,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                color = if (active) color else CineColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Vibrant Liquid Gradient Action Button (Primary CTA) with spring press feedback and gloss
 */
@Composable
fun LiquidButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    gradient: Brush = Brush.horizontalGradient(
        listOf(CineColors.PrimaryCoral, CineColors.PrimaryGlow)
    )
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "LiquidBtnScale"
    )

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        shadowElevation = if (enabled) 12.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(if (enabled) gradient else Brush.linearGradient(listOf(Color(0xFF2A2D3A), Color(0xFF20222D))))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = if (enabled) 0.40f else 0.05f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Subtle specular top reflection
            if (enabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.15f), Color.Transparent, Color.Transparent)
                            )
                        )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) Color.White else CineColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = text,
                    color = if (enabled) Color.White else CineColors.TextMuted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Secondary Frosted Glass Button with spring click feedback
 */
@Composable
fun LiquidSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    textColor: Color = CineColors.TextPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "SecondaryBtnScale"
    )

    Surface(
        modifier = modifier
            .height(44.dp)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(CineColors.GlassBorder, CineColors.GlassBorderSubtle)
                ),
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        color = CineColors.GlassWhiteSubtle
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// =========================================================================
//  Loading State & View
// =========================================================================

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Data<T>(val value: T) : UiState<T>
}

class Loader<T>(val state: UiState<T>, val reload: () -> Unit)

@Composable
fun <T> rememberLoader(vararg keys: Any?, block: suspend () -> T): Loader<T> {
    var tick by remember { mutableIntStateOf(0) }
    val state = remember { mutableStateOf<UiState<T>>(UiState.Loading) }
    LaunchedEffect(*keys, tick) {
        state.value = UiState.Loading
        state.value = try {
            UiState.Data(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiState.Error(errorMessage(e))
        }
    }
    return Loader(state.value) { tick++ }
}

@Composable
fun <T> LoadView(loader: Loader<T>, content: @Composable (T) -> Unit) {
    when (val s = loader.state) {
        is UiState.Loading -> {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CineColors.PrimaryCoral,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
        is UiState.Error -> {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text(
                            text = "Connection Notice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = s.message,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CineColors.TextSecondary
                        )
                        Spacer(Modifier.height(18.dp))
                        LiquidButton(
                            text = "Try Again",
                            icon = Icons.Default.Refresh,
                            onClick = loader.reload,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                    }
                }
            }
        }
        is UiState.Data -> content(s.value)
    }
}

/** Fire-and-forget action: shows toast on error and calls [onDone]. */
fun CoroutineScope.launchSafe(toast: (String) -> Unit, onDone: () -> Unit = {}, block: suspend () -> Unit) {
    launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            toast(errorMessage(e))
        } finally {
            onDone()
        }
    }
}

@Composable
fun rememberToast(): (String) -> Unit {
    val ctx = LocalContext.current
    return remember { { m: String -> Toast.makeText(ctx, m, Toast.LENGTH_LONG).show() } }
}

// =========================================================================
//  Formatting & Helpers
// =========================================================================

fun money(v: BigDecimal?): String = "₹" + (v ?: BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP).toPlainString()

private val timeFmt = DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")
private val shortTimeFmt = DateTimeFormatter.ofPattern("h:mm a")

fun fmtTime(s: String?): String =
    if (s == null) "—" else runCatching { LocalDateTime.parse(s).format(timeFmt) }.getOrDefault(s)

fun fmtShortTime(s: String?): String =
    if (s == null) "—" else runCatching { LocalDateTime.parse(s).format(shortTimeFmt) }.getOrDefault(s)

fun fmtSmartDateTime(s: String?): String {
    if (s == null) return "—"
    return runCatching {
        val dt = LocalDateTime.parse(s)
        val today = LocalDate.now()
        val showDate = dt.toLocalDate()
        val timeStr = dt.format(shortTimeFmt)
        when {
            showDate.isEqual(today) -> "Today • $timeStr"
            showDate.isEqual(today.plusDays(1)) -> "Tomorrow • $timeStr"
            showDate.isEqual(today.minusDays(1)) -> "Yesterday • $timeStr"
            else -> dt.format(DateTimeFormatter.ofPattern("EEE, d MMM • h:mm a"))
        }
    }.getOrDefault(s)
}

fun statusColor(status: String?): Color = when (status?.uppercase()) {
    "CONFIRMED", "ACTIVE", "SETTLED" -> CineColors.EmeraldGreen
    "SOLD", "RESOLD" -> CineColors.LaserCyan
    "CANCELLED" -> Color(0xFFEF4444)
    else -> CineColors.RatingGold
}

fun catColor(category: String?): Color = when (category?.uppercase()) {
    "SILVER" -> Color(0xFF94A3B8)
    "GOLD" -> CineColors.RatingGold
    "PLATINUM" -> CineColors.LaserCyan
    "VIP" -> CineColors.NeonPurple
    else -> CineColors.PrimaryGlow
}

// =========================================================================
//  Shared UI Elements
// =========================================================================

@Composable
fun Pill(text: String, color: Color = CineColors.LaserCyan) {
    GlassPill(text = text, color = color)
}

@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    badge: String? = null,
    badgeColor: Color = CineColors.LaserCyan,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    AtmosphericBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    color = CineColors.GlassDarkSurface.copy(alpha = 0.85f),
                    modifier = Modifier.border(
                        width = 0.5.dp,
                        color = CineColors.GlassBorderSubtle,
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    ),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                ) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = CineColors.TextPrimary
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (badge != null) {
                                    GlassPill(text = badge, color = badgeColor)
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CineColors.GlassWhiteSubtle)
                                    .border(1.dp, CineColors.GlassBorderSubtle, CircleShape)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = CineColors.TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        actions = actions
                    )
                }
            },
            content = content
        )
    }
}

@Composable
fun EmptyState(title: String, body: String, icon: ImageVector = Icons.Default.Movie) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = CineColors.GlassWhiteSubtle,
            modifier = Modifier
                .size(68.dp)
                .border(1.dp, CineColors.GlassBorderSubtle, CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CineColors.PrimaryGlow,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CineColors.TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = CineColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun Poster(
    url: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    if (url.isNullOrBlank()) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(CineColors.VoidCard)
                .border(1.dp, CineColors.GlassBorderSubtle, shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Movie,
                contentDescription = null,
                tint = CineColors.TextMuted,
                modifier = Modifier.size(28.dp)
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(shape)
                .border(1.dp, CineColors.GlassBorderSubtle, shape)
        )
    }
}
