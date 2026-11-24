package com.cinesync.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * CineSync Official Brand Emblem:
 * Cinematic rounded glass squircle with radiant coral clapperboard and twin sparkle stars.
 */
@Composable
fun CineSyncLogo(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LogoPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Ambient soft coral bloom behind logo
        Box(
            modifier = Modifier
                .fillMaxSize(1.35f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CineColors.PrimaryCoral.copy(alpha = pulseAlpha * 0.5f),
                            CineColors.PrimaryCoral.copy(alpha = pulseAlpha * 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Squircle Glass Container
        Surface(
            shape = RoundedCornerShape(size * 0.28f),
            color = Color(0xFF141724),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CineColors.PrimaryCoral.copy(alpha = 0.75f),
                            CineColors.PrimaryCoral.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.08f)
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ),
                    shape = RoundedCornerShape(size * 0.28f)
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height

                // Clapperboard body
                val bodyX = w * 0.22f
                val bodyY = h * 0.42f
                val bodyW = w * 0.56f
                val bodyH = h * 0.36f
                val bodyCorner = w * 0.06f

                val bodyPath = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = bodyX,
                            top = bodyY,
                            right = bodyX + bodyW,
                            bottom = bodyY + bodyH,
                            radiusX = bodyCorner,
                            radiusY = bodyCorner
                        )
                    )
                }

                drawPath(
                    path = bodyPath,
                    brush = Brush.verticalGradient(
                        listOf(CineColors.PrimaryGlow, CineColors.PrimaryCoral, CineColors.PrimaryDark)
                    )
                )

                // Top specular highlight on body
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                        startY = bodyY,
                        endY = bodyY + bodyH * 0.45f
                    ),
                    topLeft = Offset(bodyX, bodyY),
                    size = Size(bodyW, bodyH * 0.45f)
                )

                // Slanted Clapper Arm
                val armX = w * 0.20f
                val armY = h * 0.26f
                val armW = w * 0.60f
                val armH = h * 0.14f

                drawRoundRect(
                    color = Color(0xFF1A1D2C),
                    topLeft = Offset(armX, armY),
                    size = Size(armW, armH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f, w * 0.04f)
                )

                // Clapper Stripes (Coral)
                val stripeWidth = armW * 0.15f
                for (i in 0..3) {
                    val sx = armX + i * stripeWidth * 1.8f
                    val stripe = Path().apply {
                        moveTo(sx + stripeWidth * 0.4f, armY)
                        lineTo(sx + stripeWidth * 1.1f, armY)
                        lineTo(sx + stripeWidth * 0.7f, armY + armH)
                        lineTo(sx, armY + armH)
                        close()
                    }
                    drawPath(stripe, color = CineColors.PrimaryCoral)
                }

                // Star Sparkle 1 (Large, White) at (w * 0.42, h * 0.56)
                drawSparkleStar(
                    center = Offset(w * 0.42f, h * 0.58f),
                    radius = w * 0.09f,
                    color = Color.White
                )

                // Star Sparkle 2 (Small, Gold) at (w * 0.65, h * 0.51)
                drawSparkleStar(
                    center = Offset(w * 0.63f, h * 0.52f),
                    radius = w * 0.06f,
                    color = CineColors.RatingGold
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkleStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    val p = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(p, color = color)
}

/**
 * CineSync Fluid Splash Screen (Matches User Inspiration Screenshot 2)
 */
@Composable
fun SplashScreen(
    onFinish: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.82f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SplashScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "SplashAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1800) // Display splash screen for 1.8 seconds
        onFinish()
    }

    AtmosphericBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Main Branding Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .scale(scale)
                    .alpha(alpha)
            ) {
                // Official CineSync Brand Logo with pulse glow
                CineSyncLogo(size = 92.dp)

                Spacer(Modifier.height(24.dp))

                // Brand Name
                Text(
                    text = "CineSync",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.TextPrimary,
                    letterSpacing = (-0.5).sp
                )

                Spacer(Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = "The Modern Cinematic Experience",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 15.sp,
                    color = CineColors.TextSecondary
                )

                Spacer(Modifier.height(36.dp))

                // Circular Progress Spinner
                CircularProgressIndicator(
                    color = CineColors.PrimaryCoral,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Bottom Pinned Verified Ticketing Tagline
            Text(
                text = "VERIFIED THEATRICAL TICKETING",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.5.sp,
                color = CineColors.TextMuted.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 42.dp)
                    .alpha(alpha)
            )
        }
    }
}
