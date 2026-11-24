@file:OptIn(ExperimentalMaterial3Api::class)

package com.cinesync.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cinesync.app.data.*
import java.math.BigDecimal
import java.math.RoundingMode

// =========================================================================
//  Pass Wallet Tab (iOS Apple Wallet / Boarding Pass Aesthetic)
// =========================================================================

@Composable
fun TicketsTab(nav: NavController) {
    val loader = rememberLoader { Net.api.myBookings(Session.userId) }
    var cancelFor by remember { mutableStateOf<BookingResponse?>(null) }
    var resellFor by remember { mutableStateOf<BookingResponse?>(null) }
    var splitFor by remember { mutableStateOf<BookingResponse?>(null) }
    val scope = rememberCoroutineScope()
    val toast = rememberToast()

    LoadView(loader) { list ->
        if (list.isEmpty()) {
            EmptyState(
                title = "Pass Wallet is Empty",
                body = "Book your movie passes from the Movies tab, and your scannable digital tickets will appear right here.",
                icon = Icons.Default.ConfirmationNumber
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Text(
                        text = "${list.size} Active Cinema Passes",
                        style = MaterialTheme.typography.labelLarge,
                        color = CineColors.TextSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                items(list, key = { it.bookingId }) { booking ->
                    WalletPassCard(
                        booking = booking,
                        onSplit = { splitFor = booking },
                        onResell = { resellFor = booking },
                        onCancel = { cancelFor = booking }
                    )
                }
            }
        }
    }

    // Cancellation Dialog
    cancelFor?.let { b ->
        AlertDialog(
            containerColor = CineColors.VoidCard,
            onDismissRequest = { cancelFor = null },
            title = {
                Text(
                    text = "Cancel Booking?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to cancel this booking (${b.bookingReference ?: b.bookingId})? Per theater guidelines, cancellation is allowed up to 2 hours before showtime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CineColors.TextSecondary
                )
            },
            confirmButton = {
                LiquidButton(
                    text = "Confirm Cancellation",
                    onClick = {
                        cancelFor = null
                        scope.launchSafe(toast) {
                            Net.api.cancelBooking(b.bookingReference ?: b.bookingId.toString())
                            toast("Booking cancelled successfully")
                            loader.reload()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(onClick = { cancelFor = null }) {
                    Text("Keep Pass", color = CineColors.TextSecondary)
                }
            }
        )
    }

    // Resell Dialog
    resellFor?.let { b ->
        ResellDialog(
            b = b,
            onDismiss = { resellFor = null },
            onDone = {
                resellFor = null
                toast("Seats listed on P2P Ticket Exchange!")
                loader.reload()
            }
        )
    }

    // Split Outing Dialog
    splitFor?.let { b ->
        SplitDialog(
            b = b,
            onDismiss = { splitFor = null },
            onCreated = { outingId ->
                splitFor = null
                nav.navigate("outing/$outingId")
            }
        )
    }
}

/**
 * High-End Wallet Pass Card with Perforated Ticket Notch & Scannable QR Code
 */
@Composable
private fun WalletPassCard(
    booking: BookingResponse,
    onSplit: () -> Unit,
    onResell: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = CineColors.GlassDarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(CineColors.GlassBorder, CineColors.GlassBorderSubtle)
                ),
                shape = RoundedCornerShape(26.dp)
            ),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Movie & Theater Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineColors.PrimaryCoral.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = null,
                        tint = CineColors.PrimaryCoral,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.movieTitle ?: "Cinema Screening",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CineColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val theatre = booking.theatreName ?: "Cinema"
                    val screen = booking.screenName ?: "Audi 1"
                    val locationText = if (theatre.contains(screen, ignoreCase = true)) theatre else "$theatre • $screen"

                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.labelSmall,
                        color = CineColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = fmtSmartDateTime(booking.showTime),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CineColors.PrimaryGlow
                    )
                }

                Pill(
                    text = booking.status ?: "CONFIRMED",
                    color = statusColor(booking.status)
                )
            }

            // 2. Metrics Grid Row (Seats, Pass ID, Total)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SEATS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CineColors.TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = booking.seatCodes.orEmpty().joinToString(", ").ifBlank { "Unassigned" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CineColors.TextPrimary
                    )
                }

                Column {
                    Text(
                        text = "PASS ID",
                        style = MaterialTheme.typography.labelSmall,
                        color = CineColors.TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "CS-${(booking.bookingReference ?: booking.bookingId.toString()).takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CineColors.LaserCyan
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = CineColors.TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = money(booking.totalAmountPaid),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CineColors.TextPrimary
                    )
                }
            }

            // 3. Perforated Ticket Divider
            PerforatedDivider()

            // 4. Scannable QR Code Visualizer Card
            ScannableQRCard(bookingRef = booking.bookingReference ?: "CS-${booking.bookingId}")

            // 5. Actions Row (Split, Resell, Cancel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidSecondaryButton(
                    text = "Split Bill",
                    icon = Icons.Default.Groups,
                    onClick = onSplit,
                    modifier = Modifier.weight(1f)
                )

                LiquidSecondaryButton(
                    text = "Resell",
                    icon = Icons.Default.SwapHoriz,
                    onClick = onResell,
                    modifier = Modifier.weight(1f),
                    textColor = CineColors.RatingGold
                )

                if (booking.status?.uppercase() == "CONFIRMED") {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Perforated Ticket Divider line with dashed pattern
 */
@Composable
private fun PerforatedDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {
            drawLine(
                color = CineColors.GlassBorder.copy(alpha = 0.6f),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}

/**
 * Scannable Digital QR Code Component (from Mock Image 8)
 */
@Composable
private fun ScannableQRCard(bookingRef: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF10131E),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(CineColors.GlassBorder, CineColors.GlassBorderSubtle)
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // High-Contrast QR Container with Specular Border
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(148.dp)
                    .padding(8.dp)
            ) {
                // Procedural QR Matrix Pattern representation
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val cell = w / 7f

                    // 3 Finder Patterns (Corners)
                    drawRect(Color.Black, Offset(0f, 0f), androidx.compose.ui.geometry.Size(cell * 2.2f, cell * 2.2f))
                    drawRect(Color.White, Offset(cell * 0.4f, cell * 0.4f), androidx.compose.ui.geometry.Size(cell * 1.4f, cell * 1.4f))
                    drawRect(Color.Black, Offset(cell * 0.7f, cell * 0.7f), androidx.compose.ui.geometry.Size(cell * 0.8f, cell * 0.8f))

                    drawRect(Color.Black, Offset(w - cell * 2.2f, 0f), androidx.compose.ui.geometry.Size(cell * 2.2f, cell * 2.2f))
                    drawRect(Color.White, Offset(w - cell * 1.8f, cell * 0.4f), androidx.compose.ui.geometry.Size(cell * 1.4f, cell * 1.4f))
                    drawRect(Color.Black, Offset(w - cell * 1.5f, cell * 0.7f), androidx.compose.ui.geometry.Size(cell * 0.8f, cell * 0.8f))

                    drawRect(Color.Black, Offset(0f, w - cell * 2.2f), androidx.compose.ui.geometry.Size(cell * 2.2f, cell * 2.2f))
                    drawRect(Color.White, Offset(cell * 0.4f, w - cell * 1.8f), androidx.compose.ui.geometry.Size(cell * 1.4f, cell * 1.4f))
                    drawRect(Color.Black, Offset(cell * 0.7f, w - cell * 1.5f), androidx.compose.ui.geometry.Size(cell * 0.8f, cell * 0.8f))

                    // QR Data dots
                    val dot = cell * 0.6f
                    drawRect(Color.Black, Offset(cell * 3f, cell * 1f), androidx.compose.ui.geometry.Size(dot, dot))
                    drawRect(Color.Black, Offset(cell * 4f, cell * 1.5f), androidx.compose.ui.geometry.Size(dot, dot))
                    drawRect(Color.Black, Offset(cell * 2.5f, cell * 3f), androidx.compose.ui.geometry.Size(dot, dot))
                    drawRect(Color.Black, Offset(cell * 4f, cell * 3.2f), androidx.compose.ui.geometry.Size(dot, dot))
                    drawRect(Color.Black, Offset(cell * 3f, cell * 4.5f), androidx.compose.ui.geometry.Size(dot, dot))
                    drawRect(Color.Black, Offset(cell * 4.5f, cell * 5f), androidx.compose.ui.geometry.Size(dot, dot))
                    drawRect(Color.Black, Offset(cell * 5.5f, cell * 3.5f), androidx.compose.ui.geometry.Size(dot, dot))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CineColors.EmeraldGreen)
                )
                Text(
                    text = "CS-VERIFY-${bookingRef.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = CineColors.LaserCyan
                )
            }
        }
    }
}

// =========================================================================
//  Resell & Split Modals
// =========================================================================

@Composable
private fun ResellDialog(
    b: BookingResponse,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    val ids = b.showSeatIds.orEmpty()
    val codes = b.seatCodes.orEmpty()
    val perSeat = if (ids.isEmpty()) BigDecimal.ZERO
    else (b.totalAmountPaid ?: BigDecimal.ZERO).divide(BigDecimal(ids.size), 2, RoundingMode.HALF_UP)
    var chosen by remember { mutableStateOf(ids.toSet()) }
    var price by remember { mutableStateOf(perSeat.setScale(0, RoundingMode.HALF_UP).toPlainString()) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val toast = rememberToast()

    AlertDialog(
        containerColor = CineColors.VoidCard,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Resell Tickets",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CineColors.TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Fair-price policy: You cannot ask for more than your purchase price (${money(perSeat)}/seat).",
                    style = MaterialTheme.typography.bodySmall,
                    color = CineColors.LaserCyan
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Asking Price Per Seat (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Select Seats to List:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.TextPrimary
                )

                ids.forEachIndexed { i, id ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                chosen = if (id in chosen) chosen - id else chosen + id
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = id in chosen,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = CineColors.PrimaryCoral,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = codes.getOrNull(i) ?: "Seat $id",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CineColors.TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            LiquidButton(
                text = if (busy) "Listing…" else "List on Exchange",
                enabled = !busy && chosen.isNotEmpty(),
                onClick = {
                    val p = price.toBigDecimalOrNull()
                    if (p == null || p < BigDecimal.ONE || p > perSeat) {
                        toast("Enter a price between ₹1 and ${money(perSeat)}")
                        return@LiquidButton
                    }
                    scope.launchSafe(toast, { busy = false }) {
                        busy = true
                        chosen.forEach { Net.api.createListing(CreateListingRequest(Session.userId, it, p)) }
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CineColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun SplitDialog(
    b: BookingResponse,
    onDismiss: () -> Unit,
    onCreated: (Long) -> Unit
) {
    var name by remember { mutableStateOf("${b.movieTitle ?: "Movie"} Night") }
    var emails by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val toast = rememberToast()

    AlertDialog(
        containerColor = CineColors.VoidCard,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Group Bill Splitter",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CineColors.TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Initiates a new group outing ledger. The ticket cost (${money(b.totalAmountPaid)}) is automatically recorded as Expense #1.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CineColors.LaserCyan
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Outing Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = emails,
                    onValueChange = { emails = it },
                    label = { Text("Friends' Emails (comma separated)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            LiquidButton(
                text = if (busy) "Creating Outing…" else "Create Group Splitter",
                enabled = !busy && name.isNotBlank(),
                onClick = {
                    scope.launchSafe(toast, { busy = false }) {
                        busy = true
                        val list = emails.split(',', ';', ' ', '\n').map { it.trim() }.filter { it.contains('@') }
                        val r = Net.api.createOuting(
                            Session.userId,
                            CreateOutingRequest(b.bookingId, b.bookingReference, name.trim(), list)
                        )
                        onCreated(r.outingId)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CineColors.TextSecondary)
            }
        }
    )
}
