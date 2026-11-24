@file:OptIn(ExperimentalMaterial3Api::class)

package com.cinesync.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinesync.app.data.*
import retrofit2.HttpException
import java.math.BigDecimal



@Composable
fun SeatScreen(showId: Long, onBack: () -> Unit, onBooked: () -> Unit) {
    var currentShowId by remember(showId) { mutableLongStateOf(showId) }
    val loader = rememberLoader(currentShowId) { Net.api.seats(currentShowId) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var paymentMethod by remember { mutableStateOf("MOCK_UPI") }
    var busy by remember { mutableStateOf(false) }
    var ticket by remember { mutableStateOf<BookingResponse?>(null) }
    var siblingShows by remember { mutableStateOf<List<Show>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val toast = rememberToast()
    val currentShow = Net.showCache[currentShowId] ?: Net.showCache[showId]

    LaunchedEffect(currentShowId) {
        val s = Net.showCache[currentShowId] ?: Net.showCache[showId]
        val mId = s?.movieId
        if (mId != null) {
            runCatching {
                val list = Net.api.showsForMovie(mId)
                list.forEach { Net.showCache[it.showId] = it }
                siblingShows = list
            }
        }
    }

    ScreenScaffold(
        title = currentShow?.movieTitle ?: "Select Seats",
        onBack = onBack,
        badge = "IMAX Laser",
        badgeColor = CineColors.LaserCyan
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            LoadView(loader) { seats ->
                val byId = seats.associateBy { it.showSeatId }
                val total = selected.sumOf { byId[it]?.price ?: BigDecimal.ZERO }

                Column(Modifier.fillMaxSize()) {
                    // Scrollable Theater Map
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Movie & Theater Info Glass Card
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CineColors.PrimaryCoral.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ConfirmationNumber,
                                        contentDescription = null,
                                        tint = CineColors.PrimaryCoral,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${currentShow?.theatreName ?: "PVR INOX"} • ${currentShow?.screenName ?: "Audi 1"}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CineColors.TextPrimary
                                    )
                                    Text(
                                        text = "Laser IMAX Projection • ${fmtSmartDateTime(currentShow?.startTime)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CineColors.LaserCyan
                                    )
                                }
                            }

                            // Interactive Real Showtime Pills
                            val showList = if (siblingShows.isNotEmpty()) siblingShows else listOfNotNull(currentShow)
                            if (showList.isNotEmpty()) {
                                Spacer(Modifier.height(14.dp))
                                HorizontalDivider(color = CineColors.GlassBorderSubtle)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "SELECT TIMING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.2.sp,
                                    color = CineColors.TextMuted
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(showList.size) { i ->
                                        val sibling = showList[i]
                                        val isSelected = sibling.showId == currentShowId
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) CineColors.PrimaryCoral.copy(alpha = 0.25f) else CineColors.GlassWhiteSubtle,
                                            modifier = Modifier
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) CineColors.PrimaryCoral else CineColors.GlassBorderSubtle,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    if (currentShowId != sibling.showId) {
                                                        currentShowId = sibling.showId
                                                        selected = emptySet()
                                                    }
                                                }
                                        ) {
                                            Text(
                                                text = fmtShortTime(sibling.startTime),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) CineColors.PrimaryGlow else CineColors.TextSecondary,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // 2. Curved Neon "SCREEN THIS WAY"
                        ScreenArcVisualizer()

                        Spacer(Modifier.height(20.dp))

                        // 3. Status Legend Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendItem(color = CineColors.GlassBorder, label = "Available")
                            LegendItem(color = CineColors.PrimaryCoral, label = "Selected")
                            LegendItem(color = Color(0xFF2A2D3A), label = "Reserved")
                        }

                        Spacer(Modifier.height(24.dp))

                        // 4. Interactive Seats Layout
                        val rowsMap = seats.groupBy { it.row }.toSortedMap()
                        rowsMap.forEach { (row, rowSeats) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Row Letter Label
                                Text(
                                    text = row,
                                    modifier = Modifier.width(24.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CineColors.TextMuted,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.width(4.dp))

                                // Seat Blocks with aisle separation
                                val sorted = rowSeats.sortedBy { it.seatNumber }
                                val half = (sorted.size + 1) / 2

                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    sorted.take(half).forEach { s ->
                                        InteractiveSeatPill(
                                            seat = s,
                                            selected = s.showSeatId in selected,
                                            modifier = Modifier.weight(1f),
                                            onToggle = {
                                                selected = if (s.showSeatId in selected) selected - s.showSeatId else selected + s.showSeatId
                                            }
                                        )
                                    }
                                }

                                Spacer(Modifier.width(14.dp)) // Aisle gap

                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    sorted.drop(half).forEach { s ->
                                        InteractiveSeatPill(
                                            seat = s,
                                            selected = s.showSeatId in selected,
                                            modifier = Modifier.weight(1f),
                                            onToggle = {
                                                selected = if (s.showSeatId in selected) selected - s.showSeatId else selected + s.showSeatId
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // 5. Tier Price Summary Pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            seats.distinctBy { it.category }.forEach {
                                GlassPill(
                                    text = "${it.category} ${money(it.price)}",
                                    color = catColor(it.category)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }

                    // Bottom Floating Checkout Dock
                    Surface(
                        color = CineColors.GlassDock,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(CineColors.GlassBorder, Color.Transparent)
                                ),
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                            ),
                        shadowElevation = 24.dp
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Payment method selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pay via:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CineColors.TextMuted
                                )
                                listOf(
                                    "MOCK_UPI" to "UPI",
                                    "MOCK_CARD" to "Card",
                                    "MOCK_NET_BANKING" to "Net Banking"
                                ).forEach { (key, label) ->
                                    val isChosen = paymentMethod == key
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isChosen) CineColors.LaserCyan.copy(alpha = 0.2f) else CineColors.GlassWhiteSubtle,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { paymentMethod = key }
                                            .border(
                                                1.dp,
                                                if (isChosen) CineColors.LaserCyan else CineColors.GlassBorderSubtle,
                                                RoundedCornerShape(14.dp)
                                            )
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isChosen) CineColors.LaserCyan else CineColors.TextSecondary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Price and CTA Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (selected.isEmpty()) "Select Seats" else "${selected.size} Seat(s) Chosen",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CineColors.TextSecondary
                                    )
                                    Text(
                                        text = money(total),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CineColors.TextPrimary
                                    )
                                }

                                LiquidButton(
                                    text = if (busy) "Reserving…" else "Continue to Pay",
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    enabled = selected.isNotEmpty() && !busy,
                                    onClick = {
                                        scope.launchSafe(toast, { busy = false }) {
                                            busy = true
                                            try {
                                                ticket = Net.api.book(
                                                    BookingRequest(Session.userId, currentShowId, selected.toList(), paymentMethod)
                                                )
                                            } catch (e: HttpException) {
                                                if (e.code() == 409) {
                                                    selected = emptySet()
                                                    loader.reload()
                                                }
                                                throw e
                                            }
                                        }
                                    },
                                    modifier = Modifier.width(190.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Booking Success Modal
    ticket?.let { t ->
        AlertDialog(
            onDismissRequest = {},
            containerColor = CineColors.VoidCard,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = CineColors.EmeraldGreen,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Booking Confirmed! 🎟️",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CineColors.TextPrimary
                    )
                }
            },
            text = {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Reference: ${t.bookingReference ?: t.bookingId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CineColors.LaserCyan
                        )
                        Text(
                            text = "Seats: ${t.seatCodes.orEmpty().joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CineColors.TextPrimary
                        )
                        Text(
                            text = "Total Paid: ${money(t.totalAmountPaid)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = CineColors.EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                        t.transactionId?.let {
                            Text(
                                text = "Transaction ID: $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = CineColors.TextMuted
                            )
                        }
                    }
                }
            },
            confirmButton = {
                LiquidButton(
                    text = "View in Wallet",
                    icon = Icons.Default.ConfirmationNumber,
                    onClick = {
                        ticket = null
                        onBooked()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

// =========================================================================
//  Visualizer & Seat Item Helpers
// =========================================================================

/**
 * Curved neon blue laser projection arc representing "SCREEN THIS WAY"
 */
@Composable
private fun ScreenArcVisualizer() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(28.dp)
        ) {
            val w = size.width
            val h = size.height

            val arcPath = Path().apply {
                moveTo(0f, h * 0.75f)
                quadraticTo(w / 2f, -h * 0.2f, w, h * 0.75f)
            }

            // Glow line
            drawPath(
                path = arcPath,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        CineColors.LaserCyan.copy(alpha = 0.85f),
                        CineColors.LaserCyan,
                        CineColors.LaserCyan.copy(alpha = 0.85f),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "SCREEN THIS WAY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = CineColors.LaserCyan.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CineColors.TextSecondary
        )
    }
}

@Composable
private fun InteractiveSeatPill(
    seat: SeatInfo,
    selected: Boolean,
    modifier: Modifier,
    onToggle: () -> Unit
) {
    val isAvail = seat.status == "AVAILABLE"
    val cat = catColor(seat.category)
    val shape = RoundedCornerShape(8.dp)

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SeatScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .aspectRatio(1f)
            .clip(shape)
            .background(
                when {
                    selected -> CineColors.PrimaryCoral
                    !isAvail -> Color(0xFF242735)
                    else -> CineColors.GlassWhiteSubtle
                }
            )
            .border(
                width = 1.dp,
                color = when {
                    selected -> CineColors.PrimaryGlow
                    !isAvail -> Color.Transparent
                    else -> cat.copy(alpha = 0.5f)
                },
                shape = shape
            )
            .clickable(enabled = isAvail, onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${seat.seatNumber}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                selected -> Color.White
                !isAvail -> CineColors.TextMuted.copy(alpha = 0.5f)
                else -> cat
            }
        )
    }
}
