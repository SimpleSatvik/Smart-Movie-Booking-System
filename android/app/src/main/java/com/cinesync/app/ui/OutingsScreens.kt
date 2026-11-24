@file:OptIn(ExperimentalMaterial3Api::class)

package com.cinesync.app.ui

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cinesync.app.data.*
import java.math.BigDecimal

// =========================================================================
//  Outings Tab & Group Splitter Ledger (Mock Image 1)
// =========================================================================

@Composable
fun OutingsTab(nav: NavController) {
    val loader = rememberLoader { Net.api.myOutings(Session.userId) }

    LoadView(loader) { list ->
        if (list.isEmpty()) {
            EmptyState(
                title = "No Group Outings Yet",
                body = "Open the Wallet tab and tap 'Split Bill' on any ticket booking to launch a group expense ledger.",
                icon = Icons.Default.Groups
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(list, key = { it.outingId }) { outing ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { nav.navigate("outing/${outing.outingId}") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = outing.outingName ?: "Cinema Outing",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = CineColors.TextPrimary
                                )
                                Text(
                                    text = listOfNotNull(outing.movieTitle, outing.theatreName).joinToString(" • "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CineColors.TextSecondary
                                )
                                if (outing.showTime != null) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = fmtTime(outing.showTime),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CineColors.LaserCyan
                                    )
                                }
                            }

                            Text(
                                text = money(outing.totalSpend ?: outing.totalGroupSpend),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = CineColors.TextPrimary
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassPill(
                                text = "${outing.memberCount ?: 2} Friends",
                                color = CineColors.LaserCyan
                            )
                            LiquidSecondaryButton(
                                text = "Open Ledger",
                                onClick = { nav.navigate("outing/${outing.outingId}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
//  Outing Detail: Group Splitter Ledger (Mock Image 1)
// =========================================================================

@Composable
fun OutingDetailScreen(outingId: Long, onBack: () -> Unit) {
    val loader = rememberLoader(outingId) { Net.api.outingSummary(outingId) }
    var addExpense by remember { mutableStateOf(false) }
    var addMember by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val toast = rememberToast()

    val title = (loader.state as? UiState.Data)?.value?.outingName ?: "Group Splitter"

    ScreenScaffold(
        title = "Group Splitter",
        onBack = onBack,
        badge = "Outing Ledger",
        badgeColor = CineColors.LaserCyan,
        actions = {
            IconButton(
                onClick = { addMember = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CineColors.GlassWhiteSubtle)
                    .border(1.dp, CineColors.GlassBorderSubtle, CircleShape)
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = "Add friend",
                    tint = CineColors.LaserCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
        }
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            LoadView(loader) { s ->
                val totalSpend = s.totalGroupSpend ?: BigDecimal.ZERO
                val perHead = s.perPersonShare ?: BigDecimal.ZERO
                val members = s.memberBalances.orEmpty()

                // Calculate recovered vs pending
                val recovered = members.filter { (it.netBalance ?: BigDecimal.ZERO) >= BigDecimal.ZERO }
                    .sumOf { it.totalPaid ?: BigDecimal.ZERO }
                val pending = (totalSpend - recovered).max(BigDecimal.ZERO)
                val recoveredProgress = if (totalSpend > BigDecimal.ZERO) {
                    (recovered.toDouble() / totalSpend.toDouble()).coerceIn(0.0, 1.0).toFloat()
                } else 0.5f

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Outing Expense Balances",
                            style = MaterialTheme.typography.labelLarge,
                            color = CineColors.TextSecondary
                        )
                    }

                    // 1. Primary Outing Glass Card (Exact Match to Image 1)
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.outingName ?: "IMAX Group Outing",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = CineColors.TextPrimary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Yesterday at 9:30 PM",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CineColors.TextMuted
                                    )
                                }

                                Text(
                                    text = money(totalSpend),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CineColors.TextPrimary
                                )
                            }

                            Spacer(Modifier.height(14.dp))

                            // Two-Tone Progress Bar (Green Recovered vs Grey Pending)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF232635))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(recoveredProgress)
                                        .background(CineColors.EmeraldGreen)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Recovered: ${money(recovered)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CineColors.EmeraldGreen
                                )
                                Text(
                                    text = "Pending: ${money(pending)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = CineColors.TextSecondary
                                )
                            }

                            Spacer(Modifier.height(18.dp))

                            // 2. Members Rows with Initial Avatar & "✓ Settled" / "Mark Paid"
                            members.forEach { m ->
                                val isHost = m.isOrganizer == true
                                val net = m.netBalance ?: BigDecimal.ZERO
                                val isSettled = net.compareTo(BigDecimal.ZERO) == 0
                                val initial = (m.name?.firstOrNull() ?: m.email?.firstOrNull() ?: 'U').uppercase()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Avatar Circle
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = CineColors.GlassWhiteSubtle,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .border(1.dp, CineColors.GlassBorderSubtle, CircleShape)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = initial,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CineColors.TextPrimary
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = if (isHost) "${m.name ?: "Member"} (Host)" else m.name ?: m.email ?: "Member",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = CineColors.TextPrimary
                                            )
                                            Text(
                                                text = if (isSettled) "Paid in full" else "Owes ${money(net.abs())}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CineColors.TextSecondary
                                            )
                                        }
                                    }

                                    // Action / Status Badge
                                    if (isSettled) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = CineColors.EmeraldGreen.copy(alpha = 0.15f),
                                            modifier = Modifier.border(1.dp, CineColors.EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = CineColors.EmeraldGreen,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Settled",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CineColors.EmeraldGreen
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = CineColors.GlassWhiteSubtle,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    scope.launchSafe(toast) {
                                                        Net.api.settle(outingId, m.userId, Session.userId, net.abs())
                                                        toast("Payment recorded!")
                                                        loader.reload()
                                                    }
                                                }
                                                .border(1.dp, CineColors.GlassBorderSubtle, RoundedCornerShape(12.dp))
                                        ) {
                                            Text(
                                                text = "Mark Paid",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = CineColors.TextSecondary,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Debt Minimization Transfers ("Settle Up")
                    val debts = s.debtInstructions.orEmpty()
                    if (debts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Fewest Transfer Settlements",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CineColors.TextPrimary
                            )
                        }

                        items(debts) { d ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${d.fromUserName ?: "Member"} → ${d.toUserName ?: "Host"}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CineColors.TextPrimary
                                        )
                                        Text(
                                            text = "Amount: ${money(d.amount)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CineColors.LaserCyan
                                        )
                                    }

                                    LiquidButton(
                                        text = "Settle",
                                        onClick = {
                                            scope.launchSafe(toast) {
                                                Net.api.settle(outingId, d.fromUserId, d.toUserId, d.amount)
                                                toast("Settlement recorded")
                                                loader.reload()
                                            }
                                        },
                                        modifier = Modifier.height(38.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Expenses Breakdown (Tickets + Popcorn etc.)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Itemized Expenses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CineColors.TextPrimary
                            )
                            LiquidSecondaryButton(
                                text = "+ Add Expense",
                                icon = Icons.Default.Add,
                                onClick = { addExpense = true }
                            )
                        }
                    }

                    items(s.expenses.orEmpty(), key = { it.id }) { exp ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exp.reason ?: "Cinema Expense",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CineColors.TextPrimary
                                    )
                                    Text(
                                        text = listOfNotNull(exp.paidByName?.let { "Paid by $it" }, exp.createdAt?.let { fmtTime(it) }).joinToString(" • "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CineColors.TextMuted
                                    )
                                }

                                Text(
                                    text = money(exp.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CineColors.TextPrimary
                                )

                                if (exp.isTicket != true) {
                                    IconButton(
                                        onClick = {
                                            scope.launchSafe(toast) {
                                                Net.api.deleteExpense(outingId, exp.id)
                                                loader.reload()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
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

    if (addExpense) {
        AddExpenseDialog(
            onDismiss = { addExpense = false },
            onSave = { amount, reason ->
                addExpense = false
                scope.launchSafe(toast) {
                    Net.api.addExpense(outingId, AddExpenseRequest(Session.userId, amount, reason))
                    loader.reload()
                }
            }
        )
    }

    if (addMember) {
        AddMemberDialog(
            onDismiss = { addMember = false },
            onSave = { email ->
                addMember = false
                scope.launchSafe(toast) {
                    Net.api.addMember(outingId, email)
                    toast("Friend added to outing")
                    loader.reload()
                }
            }
        )
    }
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (BigDecimal, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val toast = rememberToast()

    AlertDialog(
        containerColor = CineColors.VoidCard,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Shared Expense",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CineColors.TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Expense Item (e.g. Popcorn Combo, Uber)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount Paid (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            LiquidButton(
                text = "Save Expense",
                onClick = {
                    val a = amount.toBigDecimalOrNull()
                    if (a == null || a < BigDecimal.ONE || reason.isBlank()) {
                        toast("Enter a valid reason and amount (min ₹1)")
                    } else {
                        onSave(a, reason.trim())
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
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        containerColor = CineColors.VoidCard,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Friend to Outing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CineColors.TextPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Friend's CineSync Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            LiquidButton(
                text = "Add Member",
                enabled = email.contains('@'),
                onClick = { onSave(email.trim()) },
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
