package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FinanceItem
import com.example.ui.theme.*
import com.example.ui.utils.FormatUtils
import com.example.ui.utils.MoneyUtils
import com.example.ui.utils.MonthlyFinanceCalculator
import java.util.Calendar

@Composable
fun DashboardScreen(
    items: List<FinanceItem>,
    onNavigateToTab: (Int) -> Unit,
    onOpenCategory: (String) -> Unit = {},
    onQuickAdd: (String) -> Unit,
    onDeleteItem: (FinanceItem) -> Unit,
    onProfileClick: () -> Unit
) {
    val initialCal = remember { Calendar.getInstance() }
    val thisYear = initialCal.get(Calendar.YEAR)
    val thisMonth = initialCal.get(Calendar.MONTH) + 1

    var selectedYear by remember { mutableIntStateOf(thisYear) }
    var selectedMonth by remember { mutableIntStateOf(thisMonth) } // 1 a 12

    val monthNames = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    val summary = remember(items, selectedYear, selectedMonth) {
        MonthlyFinanceCalculator.calculateSummary(items, selectedYear, selectedMonth)
    }

    val isCurrentMonth = selectedYear == thisYear && selectedMonth == thisMonth

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
                    .padding(vertical = 4.dp),
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

        // Navegador de Mês / Ano (Competência Contábil)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedMonth == 1) {
                            selectedMonth = 12
                            selectedYear -= 1
                        } else {
                            selectedMonth -= 1
                        }
                    }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Mês Anterior", tint = TextPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${monthNames[selectedMonth - 1]} $selectedYear",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        if (!isCurrentMonth) {
                            Text(
                                text = "Toque para voltar ao mês atual",
                                style = MaterialTheme.typography.labelSmall,
                                color = OceanBlue,
                                modifier = Modifier.clickable {
                                    selectedYear = thisYear
                                    selectedMonth = thisMonth
                                }
                            )
                        } else {
                            Text(
                                text = "Mês Atual",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGreen
                            )
                        }
                    }

                    IconButton(onClick = {
                        if (selectedMonth == 12) {
                            selectedMonth = 1
                            selectedYear += 1
                        } else {
                            selectedMonth += 1
                        }
                    }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Próximo Mês", tint = TextPrimary)
                    }
                }
            }
        }

        // Hero Card: Fluxo de Caixa Realizado do Mês (Sem inventar saldo bancário)
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
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RESULTADO REALIZADO DO MÊS",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold),
                                color = TextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (summary.realizedNetCashflowCents >= 0L) EmeraldGreen.copy(alpha = 0.15f) else CoralRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (summary.realizedNetCashflowCents >= 0L) "SUPERÁVIT" else "DÉFICIT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (summary.realizedNetCashflowCents >= 0L) EmeraldGreen else CoralRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = MoneyUtils.formatCents(summary.realizedNetCashflowCents),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.SansSerif
                            ),
                            color = if (summary.realizedNetCashflowCents >= 0L) EmeraldGreen else CoralRed
                        )
                        Text(
                            text = "Receitas recebidas menos despesas pagas no período",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = BorderColor.copy(alpha = 0.4f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Linha dupla: Receitas Realizadas vs Despesas Pagas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Receitas Recebidas", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    MoneyUtils.formatCents(summary.realizedIncomeCents),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Despesas Pagas", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    MoneyUtils.formatCents(summary.realizedExpensesCents),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CoralRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // Alerta de Contas Vencidas (se houver)
        if (summary.overdueBillsCents > 0L) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CoralRed.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = CoralRed, modifier = Modifier.size(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Contas Vencidas Pendentes",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = CoralRed
                            )
                            Text(
                                text = "Existem contas com vencimento ultrapassado totalizando ${MoneyUtils.formatCents(summary.overdueBillsCents)}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Card com Contas a Pagar e Empréstimos Ativos
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Contas a Pagar (Mês)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            MoneyUtils.formatCents(summary.pendingBillsCents),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoldAmber
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Empréstimos a Receber", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            MoneyUtils.formatCents(summary.activeLoansReceivableCents),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LavenderPurple
                        )
                    }
                }
            }
        }

        // Card de Patrimônio Guardado (Distinguindo de receita/despesa)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("PATRIMÔNIO ACUMULADO", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold), color = TextSecondary)
                            Text(
                                MoneyUtils.formatCents(summary.totalInvestedAndBoxesCents),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = GoldAmber
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(GoldAmber.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Savings, contentDescription = null, tint = GoldAmber, modifier = Modifier.size(22.dp))
                        }
                    }

                    Divider(color = BorderColor.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Em Caixinhas & Metas", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                MoneyUtils.formatCents(summary.totalBoxesCents),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Investimentos Gerais", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                MoneyUtils.formatCents(summary.totalInvestmentsCents),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = OceanBlue
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Atalhos Rápidos
        item {
            Text(
                text = "Módulos Financeiros",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Categorized Hub List
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FinanceHubCard(
                    title = "Apartamento & Moradia",
                    subtitle = "Despesas fixas e variáveis",
                    icon = Icons.Rounded.Apartment,
                    color = ApartmentTeal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onOpenCategory("APARTMENT")
                }

                FinanceHubCard(
                    title = "Salário & Rendas",
                    subtitle = "Receitas do período",
                    icon = Icons.Rounded.AttachMoney,
                    color = EmeraldGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onOpenCategory("SALARY")
                }

                FinanceHubCard(
                    title = "Caixinhas & Metas",
                    subtitle = "${MoneyUtils.formatCents(summary.totalBoxesCents)} guardados",
                    icon = Icons.Rounded.Savings,
                    color = GoldAmber,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onNavigateToTab(1) // Caixinhas
                }

                FinanceHubCard(
                    title = "Desafio 52 Semanas",
                    subtitle = "Poupança Progressiva",
                    icon = Icons.Rounded.EmojiEvents,
                    color = EmeraldGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onNavigateToTab(2) // Desafio 52 Semanas
                }

                FinanceHubCard(
                    title = "Empréstimos Concedidos",
                    subtitle = "${MoneyUtils.formatCents(summary.activeLoansReceivableCents)} a receber",
                    icon = Icons.Rounded.Handshake,
                    color = LavenderPurple,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onNavigateToTab(3) // Empréstimos
                }
            }
        }

        // Section Title: Atividades Recentes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lançamentos Recentes",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = { onOpenCategory("TUDO") }) {
                    Text("Ver Todos", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Recent items list
        val recentItems = items.take(5)
        if (recentItems.isEmpty()) {
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
                            "Nenhum lançamento salvo ainda.",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Cadastre seus primeiros lançamentos nos módulos acima.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(recentItems, key = { it.id }) { item ->
                RecentTransactionRow(item = item, onDelete = { onDeleteItem(item) })
            }
        }
    }
}

@Composable
fun FinanceHubCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
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
                            "+${MoneyUtils.formatCents(item.amountCents)}"
                        } else {
                            "-${MoneyUtils.formatCents(item.amountCents)}"
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
