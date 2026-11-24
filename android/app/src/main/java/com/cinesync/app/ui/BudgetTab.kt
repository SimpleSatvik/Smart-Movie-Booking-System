@file:OptIn(ExperimentalMaterial3Api::class)

package com.cinesync.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cinesync.app.data.*
import java.math.BigDecimal

// =========================================================================
//  Budget Optimizer Tab (0/1 Knapsack AI Optimizer - Mock Image 2)
// =========================================================================

@Composable
fun BudgetTab(nav: NavController) {
    var budgetValue by remember { mutableFloatStateOf(800f) }
    var seats by remember { mutableIntStateOf(2) }
    var genre by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BudgetRecommendation>?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val toast = rememberToast()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Spending Limit Glass Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Maximum Spending Limit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CineColors.TextPrimary
                        )
                        Text(
                            text = "Mathematical Value Maximizer",
                            style = MaterialTheme.typography.bodySmall,
                            color = CineColors.TextSecondary
                        )
                    }

                    Text(
                        text = "₹${budgetValue.toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = CineColors.PrimaryCoral
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Custom Slider
                Slider(
                    value = budgetValue,
                    onValueChange = { budgetValue = it },
                    valueRange = 300f..2500f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = CineColors.PrimaryCoral,
                        inactiveTrackColor = Color(0xFF2B2E3F),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("₹300 Min", style = MaterialTheme.typography.labelSmall, color = CineColors.TextMuted)
                    Text("₹2500 Max", style = MaterialTheme.typography.labelSmall, color = CineColors.TextMuted)
                }

                Spacer(Modifier.height(14.dp))

                // Quick Presets
                Text("Quick Presets", style = MaterialTheme.typography.labelSmall, color = CineColors.TextSecondary)
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChip(
                        title = "💕 Couple Date (₹850)",
                        selected = budgetValue == 850f,
                        onClick = { budgetValue = 850f; seats = 2 },
                        modifier = Modifier.weight(1f)
                    )
                    PresetChip(
                        title = "👑 VIP IMAX (₹1500)",
                        selected = budgetValue == 1500f,
                        onClick = { budgetValue = 1500f; seats = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Seats Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Number of Seats",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CineColors.TextPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { if (seats > 1) seats-- },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CineColors.GlassWhiteSubtle)
                                .border(1.dp, CineColors.GlassBorderSubtle, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Fewer", tint = CineColors.TextPrimary, modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "$seats",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CineColors.TextPrimary
                        )

                        IconButton(
                            onClick = { seats++ },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CineColors.GlassWhiteSubtle)
                                .border(1.dp, CineColors.GlassBorderSubtle, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "More", tint = CineColors.TextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Calculate Button
                LiquidButton(
                    text = if (busy) "Optimizing Algorithm…" else "Run Knapsack AI Optimizer",
                    icon = Icons.Default.AutoAwesome,
                    enabled = !busy,
                    onClick = {
                        val b = BigDecimal(budgetValue.toInt())
                        scope.launchSafe(toast, { busy = false }) {
                            busy = true
                            results = Net.api.optimize(
                                BudgetRequest(b, seats, date.trim().ifBlank { null }, genre.trim().ifBlank { null }, null)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Metrics Analytics Row (Image 2: Allocated, Savings, Utility)
        item {
            val totalAllocated = results?.firstOrNull()?.totalCost?.toInt() ?: (budgetValue * 0.95f).toInt()
            val totalSavings = (budgetValue.toInt() - totalAllocated).coerceAtLeast(0)
            val utilityScore = (totalAllocated / 2.6f).toInt() + 10

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricColumn(label = "ALLOCATED", value = "₹$totalAllocated", valueColor = CineColors.TextPrimary)
                    VerticalDivider(modifier = Modifier.height(36.dp), color = CineColors.GlassBorderSubtle)
                    MetricColumn(label = "SAVINGS", value = "₹$totalSavings", valueColor = CineColors.EmeraldGreen)
                    VerticalDivider(modifier = Modifier.height(36.dp), color = CineColors.GlassBorderSubtle)
                    MetricColumn(label = "UTILITY", value = "$utilityScore pts", valueColor = CineColors.LaserCyan)
                }
            }
        }

        // 3. Optimized Bundle Results (Image 2)
        results?.let { list ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Optimized Bundle (${list.size} Options)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CineColors.TextPrimary
                    )
                    GlassPill(text = "Best Value", color = CineColors.LaserCyan)
                }
            }

            if (list.isEmpty()) {
                item {
                    EmptyState(
                        title = "No Match Within Limit",
                        body = "No upcoming show fits ₹${budgetValue.toInt()} for $seats seat(s). Try raising your budget or reducing the seat count.",
                        icon = Icons.Default.Savings
                    )
                }
            } else {
                items(list, key = { "${it.showId}-${it.seatCategory}" }) { rec ->
                    BudgetRecCard(rec = rec, seats = seats, nav = nav)
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) CineColors.PrimaryCoral.copy(alpha = 0.2f) else CineColors.GlassWhiteSubtle,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (selected) CineColors.PrimaryCoral else CineColors.GlassBorderSubtle,
                RoundedCornerShape(12.dp)
            )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) CineColors.PrimaryGlow else CineColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CineColors.TextMuted,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun BudgetRecCard(
    rec: BudgetRecommendation,
    seats: Int,
    nav: NavController
) {
    val isBest = rec.recommendationType == "BEST_EXPERIENCE"
    val isMaxSavings = rec.recommendationType == "MAX_SAVINGS"

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isBest) CineColors.LaserCyan.copy(alpha = 0.4f) else CineColors.GlassBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CineColors.GlassWhiteSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Theaters,
                        contentDescription = null,
                        tint = if (isBest) CineColors.LaserCyan else CineColors.PrimaryCoral,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rec.movieTitle ?: "Movie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CineColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${rec.theatreName ?: "Theatre"} • ${rec.screenName ?: "Screen"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CineColors.TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = money(rec.totalCost),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.TextPrimary
                )
                Text(
                    text = "+${(rec.availableSeats ?: 10) * 8} pts",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.EmeraldGreen
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassPill(
                    text = rec.seatCategory ?: "Standard",
                    color = catColor(rec.seatCategory ?: "Silver")
                )
                GlassPill(
                    text = if (isBest) "Best Experience" else if (isMaxSavings) "Max Savings" else "Alternative",
                    color = if (isBest) CineColors.LaserCyan else if (isMaxSavings) CineColors.EmeraldGreen else CineColors.RatingGold
                )
            }

            LiquidSecondaryButton(
                text = "Choose Seats",
                onClick = {
                    Net.showCache[rec.showId] = Show(
                        rec.showId, rec.movieId, rec.movieTitle, rec.theatreName, null, rec.screenName, rec.startTime, null
                    )
                    nav.navigate("seats/${rec.showId}")
                }
            )
        }
    }
}
