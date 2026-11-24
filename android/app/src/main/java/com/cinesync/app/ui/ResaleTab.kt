@file:OptIn(ExperimentalMaterial3Api::class)

package com.cinesync.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cinesync.app.data.*

// =========================================================================
//  Ticket Exchange / Resale Marketplace Tab (Mock Image 4)
// =========================================================================

@Composable
fun ResaleTab() {
    var selectedSegment by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Segmented Control Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SegmentPill(
                selected = selectedSegment == 0,
                text = "Marketplace",
                onClick = { selectedSegment = 0 },
                modifier = Modifier.weight(1f)
            )
            SegmentPill(
                selected = selectedSegment == 1,
                text = "My Listings",
                onClick = { selectedSegment = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        if (selectedSegment == 0) {
            MarketplaceView()
        } else {
            MyListingsView()
        }
    }
}

@Composable
private fun SegmentPill(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) CineColors.PrimaryCoral.copy(alpha = 0.22f) else CineColors.GlassDarkSurface,
        modifier = modifier
            .height(44.dp)
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
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else CineColors.TextSecondary
            )
        }
    }
}

@Composable
private fun MarketplaceView() {
    val loader = rememberLoader { Net.api.listings() }
    var buying by remember { mutableStateOf<ResaleListing?>(null) }
    val scope = rememberCoroutineScope()
    val toast = rememberToast()

    LoadView(loader) { list ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Fair P2P Resale Info Banner (Image 4)
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CineColors.LaserCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = CineColors.LaserCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Fair Peer-to-Peer Resale",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CineColors.TextPrimary
                            )
                            Text(
                                text = "Unable to make it? List tickets for fellow cinema lovers with automatic instant re-allocation.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CineColors.TextSecondary
                            )
                        }
                    }
                }
            }

            if (list.isEmpty()) {
                item {
                    EmptyState(
                        title = "No Tickets Listed Right Now",
                        body = "When someone can't make their show, their seats are listed here at fair verified prices.",
                        icon = Icons.Default.SwapHoriz
                    )
                }
            } else {
                items(list, key = { it.listingId }) { listing ->
                    ResaleListingCard(
                        listing = listing,
                        isOwner = listing.sellerUserId == Session.userId,
                        onBuy = { buying = listing },
                        onCancel = {}
                    )
                }
            }
        }
    }

    // Purchase Confirmation Modal
    buying?.let { l ->
        AlertDialog(
            containerColor = CineColors.VoidCard,
            onDismissRequest = { buying = null },
            title = {
                Text(
                    text = "Purchase Resale Ticket?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.TextPrimary
                )
            },
            text = {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = l.movieTitle ?: "Cinema Pass",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CineColors.TextPrimary
                        )
                        Text(
                            text = "${l.theatreName ?: "Theatre"} • Seat ${l.seatCode ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CineColors.TextSecondary
                        )
                        Text(
                            text = "Amount: ${money(l.askingPrice)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CineColors.PrimaryCoral
                        )
                    }
                }
            },
            confirmButton = {
                LiquidButton(
                    text = "Confirm & Instant Buy",
                    onClick = {
                        buying = null
                        scope.launchSafe(toast) {
                            Net.api.buyListing(l.listingId, Session.userId)
                            toast("Ticket purchased! Check your Pass Wallet.")
                            loader.reload()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(onClick = { buying = null }) {
                    Text("Cancel", color = CineColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MyListingsView() {
    val loader = rememberLoader { Net.api.myListings(Session.userId) }
    val scope = rememberCoroutineScope()
    val toast = rememberToast()

    LoadView(loader) { list ->
        if (list.isEmpty()) {
            EmptyState(
                title = "You Have No Active Listings",
                body = "To resell a ticket, navigate to the Wallet tab, find your pass, and tap 'Resell'.",
                icon = Icons.Default.SwapHoriz
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(list, key = { it.listingId }) { listing ->
                    ResaleListingCard(
                        listing = listing,
                        isOwner = true,
                        onBuy = {},
                        onCancel = {
                            scope.launchSafe(toast) {
                                Net.api.cancelListing(listing.listingId, Session.userId)
                                toast("Listing cancelled and ticket restored")
                                loader.reload()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * P2P Resale Listing Card (Image 4)
 */
@Composable
private fun ResaleListingCard(
    listing: ResaleListing,
    isOwner: Boolean,
    onBuy: () -> Unit,
    onCancel: () -> Unit
) {
    val saving = (listing.originalPrice ?: listing.askingPrice)?.let { orig ->
        listing.askingPrice?.let { ask -> orig - ask }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listing.movieTitle ?: "Movie Ticket",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${listing.theatreName ?: "Theatre"} • ${listing.screenName ?: "Audi 1"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CineColors.TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = fmtSmartDateTime(listing.showTime),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.RatingGold
                )
            }

            // Price Display
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = money(listing.askingPrice),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.TextPrimary
                )
                if (listing.originalPrice != null && listing.originalPrice!! > (listing.askingPrice ?: java.math.BigDecimal.ZERO)) {
                    Text(
                        text = "Orig ${money(listing.originalPrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        textDecoration = TextDecoration.LineThrough,
                        color = CineColors.TextMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Seats & Seller Info Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Seat: ${listing.seatCode ?: "TBD"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CineColors.TextPrimary
            )

            if (saving != null && saving.signum() > 0) {
                GlassPill(
                    text = "Save ${money(saving)}",
                    color = CineColors.EmeraldGreen
                )
            }
        }

        listing.sellerName?.let { seller ->
            Text(
                text = "Listed by $seller",
                style = MaterialTheme.typography.labelSmall,
                color = CineColors.TextMuted
            )
        }

        Spacer(Modifier.height(14.dp))

        // Action Button
        if (isOwner) {
            if (listing.status?.uppercase() == "ACTIVE") {
                LiquidSecondaryButton(
                    text = "Cancel Listing",
                    onClick = onCancel,
                    textColor = Color(0xFFEF4444),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Status: ${listing.status ?: "SOLD"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = CineColors.LaserCyan
                )
            }
        } else {
            LiquidButton(
                text = "Instant Buy",
                onClick = onBuy,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
