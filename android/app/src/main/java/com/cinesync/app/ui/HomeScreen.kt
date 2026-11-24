@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cinesync.app.data.Session

data class HomeNavTab(
    val label: String,
    val icon: ImageVector,
    val headerTitle: String,
    val badge: String? = null,
    val badgeColor: Color = CineColors.LaserCyan
)

val HomeTabs = listOf(
    HomeNavTab("Movies", Icons.Default.Movie, "CineSync"),
    HomeNavTab("Wallet", Icons.Default.ConfirmationNumber, "Pass Wallet", "Offline Ready", CineColors.EmeraldGreen),
    HomeNavTab("Resale", Icons.Default.SwapHoriz, "Ticket Exchange", "P2P Marketplace", CineColors.RatingGold),
    HomeNavTab("Budget", Icons.Default.Savings, "Budget Optimizer", "0/1 Knapsack AI", CineColors.NeonPurple),
    HomeNavTab("Ledger", Icons.AutoMirrored.Filled.ReceiptLong, "Group Splitter", "Outing Ledger", CineColors.LaserCyan),
)

@Composable
fun HomeScreen(nav: NavController, initialTab: Int, onLogout: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, HomeTabs.lastIndex)) }
    val currentTab = HomeTabs[tab]

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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (tab == 0) {
                                    // Movie tab branding
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = CineColors.PrimaryCoral.copy(alpha = 0.2f),
                                            modifier = Modifier.size(32.dp).border(1.dp, CineColors.PrimaryCoral.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Movie,
                                                    contentDescription = null,
                                                    tint = CineColors.PrimaryCoral,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "CineSync",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = CineColors.TextPrimary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = currentTab.headerTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = CineColors.TextPrimary
                                    )
                                    currentTab.badge?.let {
                                        GlassPill(text = it, color = currentTab.badgeColor)
                                    }
                                }
                            }
                        },
                        actions = {
                            // User Profile Avatar Pill
                            val userName = Session.name.ifBlank { "User" }
                            val initial = userName.firstOrNull()?.uppercase() ?: "U"

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = CineColors.GlassWhiteSubtle,
                                modifier = Modifier
                                    .border(1.dp, CineColors.GlassBorderSubtle, RoundedCornerShape(50))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(CineColors.PrimaryCoral, CineColors.PrimaryGlow)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initial,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = userName.substringBefore(' '),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CineColors.TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(Modifier.width(6.dp))

                            // Subtle Sign Out Icon Button
                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CineColors.GlassWhiteSubtle)
                                    .border(1.dp, CineColors.GlassBorderSubtle, CircleShape)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign out",
                                    tint = CineColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    )
                }
            },
            bottomBar = {
                // Floating Liquid Glass Dock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassDock(
                        selectedTab = tab,
                        onTabSelected = { tab = it }
                    )
                }
            }
        ) { pad ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                            .togetherWith(fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                    },
                    label = "TabContent"
                ) { current ->
                    when (current) {
                        0 -> MoviesTab(nav)
                        1 -> TicketsTab(nav) // Wallet
                        2 -> ResaleTab()     // Resale
                        3 -> BudgetTab(nav)  // Budget
                        else -> OutingsTab(nav) // Ledger
                    }
                }
            }
        }
    }
}

/**
 * Floating Liquid Glass Dock navigation bar:
 * iOS 26/27 inspired glass pill floating above content with glowing active indicator.
 */
@Composable
private fun LiquidGlassDock(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = CineColors.GlassDock,
        shadowElevation = 18.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTabs.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "DockIconScale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Fluid glowing liquid glass capsule behind active tab
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(58.dp, 48.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            CineColors.PrimaryCoral.copy(alpha = 0.28f),
                                            CineColors.PrimaryCoral.copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            CineColors.PrimaryGlow.copy(alpha = 0.70f),
                                            CineColors.PrimaryCoral.copy(alpha = 0.15f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.scale(scale)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) CineColors.PrimaryCoral else CineColors.TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) CineColors.TextPrimary else CineColors.TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
