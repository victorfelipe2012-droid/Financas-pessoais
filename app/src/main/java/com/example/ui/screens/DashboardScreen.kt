package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FinanceItem
import com.example.ui.theme.*
import com.example.ui.utils.FormatUtils

@Composable
fun DashboardScreen(
    items: List<FinanceItem>,
    onNavigateToTab: (Int) -> Unit,
    onQuickAdd: (String) -> Unit,
    onDeleteItem: (FinanceItem) -> Unit,
    onSyncClick: () -> Unit = {},
    onProfileClick: () -> Unit
) {
    // Calculate metrics
    val totalSalary = items.filter { it.type == "SALARY" }.sumOf { it.amount }
    

    
    // Active loans (money lent that is NOT repaid yet)
    val activeLent = items.filter { it.type == "LENT" && !it.isCompleted }.sumOf { it.amount }
    val repaidLent = items.filter { it.type == "LENT" && it.isCompleted }.sumOf { it.amount }

    // Saved in general investments and caixinhas
    val generalInvestments = items.filter { it.type == "INVESTMENT" }.sumOf { it.amount }
    val boxSavings = items.filter { it.type == "BOX" }.sumOf { it.amount }
    val totalInvested = generalInvestments + boxSavings

    // Apartment expenses
    val totalApartment = items.filter { it.type == "APARTMENT" }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_scroll_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Greeting Block
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Olá, bem-vindo de volta!",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Suas Finanças Privadas",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = "Offline verificado",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "100% Offline • Privacidade Garantida",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(OceanBlue.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = "Sincronizar com Windows",
                            tint = OceanBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("dashboard_profile_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = "Perfil e Segurança",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // Main Balance Hero Card (Dynamic Gradient Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("balance_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "TOTAL INVESTIDO",
                            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = FormatUtils.formatCurrency(totalInvested),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = BorderColor.copy(alpha = 0.5f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Mini metrics row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("A Receber (Empréstimos)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Text(
                                    FormatUtils.formatCurrency(activeLent),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OceanBlue
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Gastos Apartamento", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Text(
                                    FormatUtils.formatCurrency(totalApartment),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ApartmentTeal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Atalhos Rápidos
        item {
            Text(
                text = "Gerenciar Finanças",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Categorized Hub List
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FinanceHubCard(
                    title = "Apartamento & Moradia",
                    subtitle = FormatUtils.formatCurrency(totalApartment),
                    icon = Icons.Rounded.Apartment,
                    color = ApartmentTeal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onNavigateToTab(1) // Transactions
                }

                FinanceHubCard(
                    title = "Salário / Renda",
                    subtitle = FormatUtils.formatCurrency(totalSalary),
                    icon = Icons.Rounded.AttachMoney,
                    color = EmeraldGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onNavigateToTab(1) // Transactions
                }

                FinanceHubCard(
                    title = "Caixinhas & Metas",
                    subtitle = FormatUtils.formatCurrency(boxSavings),
                    icon = Icons.Rounded.Savings,
                    color = GoldAmber,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onNavigateToTab(2) // Caixinhas
                }

                FinanceHubCard(
                    title = "Desafio 52 Semanas",
                    subtitle = "Poupança Progressiva",
                    icon = Icons.Rounded.EmojiEvents,
                    color = EmeraldGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onNavigateToTab(3) // Desafio 52 Semanas
                }

                FinanceHubCard(
                    title = "Emprestados",
                    subtitle = FormatUtils.formatCurrency(activeLent),
                    icon = Icons.Rounded.Handshake,
                    color = LavenderPurple,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onNavigateToTab(4) // Loans & Bills
                }
            }
        }

        // Section Title: Atividades Recentes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transações Recentes",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = { onNavigateToTab(1) }) {
                    Text("Ver Todas", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Recent items list
        if (items.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Inbox,
                            contentDescription = "Sem dados",
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Nenhuma transação salva ainda.",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            "Use as abas abaixo para organizar seu dinheiro com segurança.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(items.take(5)) { item ->
                RecentTransactionRow(item = item, onDelete = { onDeleteItem(item) })
            }
        }
    }
}

@Composable
fun FinanceHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RecentTransactionRow(
    item: FinanceItem,
    onDelete: () -> Unit
) {
    val (icon, color, labelType) = when (item.type) {
        "SALARY" -> Triple(Icons.Rounded.AttachMoney, EmeraldGreen, "Salário")
        "INVESTMENT" -> Triple(Icons.Rounded.TrendingUp, OceanBlue, "Investimento")
        "BOX" -> Triple(Icons.Rounded.Savings, GoldAmber, "Caixinha")
        "LENT" -> Triple(Icons.Rounded.Handshake, LavenderPurple, "Emprestado")
        "APARTMENT" -> Triple(Icons.Rounded.Apartment, ApartmentTeal, "Apartamento")
        else -> Triple(Icons.Rounded.ReceiptLong, CoralRed, "Conta")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Main row with Icon, Title/Category and Amount/Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = labelType,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.category.ifBlank { labelType },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.widthIn(min = 80.dp)
                ) {
                    Text(
                        text = if (item.type == "SALARY" || (item.type == "LENT" && item.isCompleted)) {
                            "+${FormatUtils.formatCurrency(item.amount)}"
                        } else {
                            "-${FormatUtils.formatCurrency(item.amount)}"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (item.type == "SALARY" || (item.type == "LENT" && item.isCompleted)) EmeraldGreen else CoralRed,
                        softWrap = false,
                        maxLines = 1
                    )

                    if (item.type == "LENT") {
                        val statusText = if (item.isCompleted) "Devolvido" else "Pendente"
                        val statusColor = if (item.isCompleted) EmeraldGreen else GoldAmber
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Footer row with Date and Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = "Data",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = FormatUtils.formatDate(item.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (item.description.isNotBlank()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Excluir",
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
