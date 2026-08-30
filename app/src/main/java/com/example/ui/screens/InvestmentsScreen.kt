package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.FinanceItem
import com.example.ui.theme.*
import com.example.ui.utils.FormatUtils
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.collectIsPressedAsState

@Composable
fun InvestmentsScreen(
    items: List<FinanceItem>,
    onUpdateItem: (FinanceItem) -> Unit,
    onDeleteItem: (FinanceItem) -> Unit,
    onStartChallenge: (Double) -> Unit,
    onResetChallenge: () -> Unit,
    onArchiveChallenge: () -> Unit,
    onProfileClick: () -> Unit
) {
    val investments = items.filter { it.type == "INVESTMENT" }
    val boxes = items.filter { it.type == "BOX" }

    var selectedBoxForManage by remember { mutableStateOf<FinanceItem?>(null) }
    var isDepositMode by remember { mutableStateOf(true) } // true for Deposit, false for Withdraw

    var selectedInvestmentForManage by remember { mutableStateOf<FinanceItem?>(null) }
    var isInvestmentDepositMode by remember { mutableStateOf(true) }

    val totalInvestments = investments.sumOf { it.amount }
    val totalBoxes = boxes.sumOf { it.amount }

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Heading
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Investimentos & Poupança",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Guarde e rentabilize seu dinheiro de forma inteligente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .testTag("investments_profile_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = "Perfil",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Metas & Caixinhas", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                selectedContentColor = OceanBlue,
                unselectedContentColor = TextSecondary
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Desafio 52 Semanas", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                selectedContentColor = EmeraldGreen,
                unselectedContentColor = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.weight(1f)
        ) {
            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("investments_scroll_column"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Patrimônio Guardado",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            FormatUtils.formatCurrency(totalInvestments + totalBoxes),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = OceanBlue
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(OceanBlue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TrendingUp,
                            contentDescription = "Investimento",
                            tint = OceanBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Section: Caixinhas (Metas)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Minhas Caixinhas",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                
                Box(
                    modifier = Modifier
                        .background(GoldAmber.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Total: ${FormatUtils.formatCurrency(totalBoxes)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = GoldAmber
                    )
                }
            }
        }

        if (boxes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Savings,
                            contentDescription = "Nenhuma caixinha",
                            tint = TextSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Nenhuma caixinha criada.",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            "Vá na aba 'Transações' e adicione um item do tipo 'Caixinha' para começar a poupar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        } else {
            items(boxes, key = { it.id }) { box ->
                BoxGoalCard(
                    box = box,
                    onManage = { isDeposit ->
                        selectedBoxForManage = box
                        isDepositMode = isDeposit
                    },
                    onDelete = { onDeleteItem(box) }
                )
            }
        }

        // Section: Outros Investimentos
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Investimentos Gerais",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                
                Box(
                    modifier = Modifier
                        .background(OceanBlue.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Total: ${FormatUtils.formatCurrency(totalInvestments)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = OceanBlue
                    )
                }
            }
        }

        if (investments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Assessment,
                            contentDescription = "Nenhum investimento",
                            tint = TextSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Nenhum investimento registrado.",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            "Adicione seus investimentos gerais (CDB, Ações, Tesouro) na aba 'Transações'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        } else {
            items(investments, key = { it.id }) { investment ->
                InvestmentCard(
                    investment = investment,
                    onManage = { isDeposit ->
                        selectedInvestmentForManage = investment
                        isInvestmentDepositMode = isDeposit
                    },
                    onDelete = { onDeleteItem(investment) }
                )
            }
        }
    }
            } else {
                ChallengeSection(
                    items = items,
                    onUpdateItem = onUpdateItem,
                    onStartChallenge = onStartChallenge,
                    onResetChallenge = onResetChallenge,
                    onArchiveChallenge = onArchiveChallenge
                )
            }
        }
    }

    // Dialog for managing Box Deposits or Withdraws
    selectedBoxForManage?.let { box ->
        ManageBoxDialog(
            box = box,
            isDeposit = isDepositMode,
            onDismiss = { selectedBoxForManage = null },
            onConfirm = { amount, selectedDate ->
                val updatedAmount = if (isDepositMode) {
                    box.amount + amount
                } else {
                    maxOf(0.0, box.amount - amount)
                }
                
                onUpdateItem(
                    box.copy(
                        amount = updatedAmount,
                        isCompleted = updatedAmount >= box.targetAmount,
                        date = selectedDate
                    )
                )
                selectedBoxForManage = null
            }
        )
    }

    // Dialog for managing general Investment Deposits or Withdraws
    selectedInvestmentForManage?.let { investment ->
        ManageInvestmentDialog(
            investment = investment,
            isDeposit = isInvestmentDepositMode,
            onDismiss = { selectedInvestmentForManage = null },
            onConfirm = { amount, selectedDate ->
                val updatedAmount = if (isInvestmentDepositMode) {
                    investment.amount + amount
                } else {
                    maxOf(0.0, investment.amount - amount)
                }
                
                onUpdateItem(
                    investment.copy(
                        amount = updatedAmount,
                        date = selectedDate
                    )
                )
                selectedInvestmentForManage = null
            }
        )
    }
}

@Composable
fun BoxGoalCard(
    box: FinanceItem,
    onManage: (Boolean) -> Unit, // True: Deposit, False: Withdraw
    onDelete: () -> Unit
) {
    val percentage = if (box.targetAmount > 0) {
        minOf(100f, ((box.amount / box.targetAmount) * 100).toFloat())
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("box_card_${box.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Main row with Icon, Title/Category and Amount/Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(GoldAmber.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Savings,
                        contentDescription = "Caixinha",
                        tint = GoldAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = box.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Meta: ${FormatUtils.formatCurrency(box.targetAmount)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = GoldAmber,
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
                        text = FormatUtils.formatCurrency(box.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        softWrap = false,
                        maxLines = 1
                    )
                    
                    val statusText = if (box.isCompleted) "Meta Atingida" else "Pendente"
                    val statusColor = if (box.isCompleted) EmeraldGreen else GoldAmber
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress Bar and Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progresso",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "${percentage.toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (percentage >= 100) EmeraldGreen else GoldAmber
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (percentage >= 100) EmeraldGreen else GoldAmber,
                trackColor = BorderColor.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BorderColor.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer with Date, Description, and Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Data",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = FormatUtils.formatDate(box.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (box.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = box.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Excluir",
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons (Deposit / Withdraw)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onManage(false) }, // Withdraw
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Resgatar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resgatar", style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = { onManage(true) }, // Deposit
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAmber, contentColor = Color.Black)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Guardar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Guardar", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun InvestmentCard(
    investment: FinanceItem,
    onManage: (Boolean) -> Unit, // True: Deposit, False: Withdraw
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("investment_card_${investment.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(OceanBlue.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = "Investimento",
                        tint = OceanBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = investment.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = investment.category.ifBlank { "Investimento" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OceanBlue,
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
                        text = FormatUtils.formatCurrency(investment.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BorderColor.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer with Date, Description, and Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Data",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = FormatUtils.formatDate(investment.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (investment.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = investment.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Excluir",
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons for Investments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onManage(false) }, // Withdraw (Resgatar)
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Resgatar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resgatar", style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = { onManage(true) }, // Deposit (Investir mais)
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue, contentColor = Color.Black)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Investir",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Investir", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun ManageInvestmentDialog(
    investment: FinanceItem,
    isDeposit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, Long) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("manage_investment_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isDeposit) "Adicionar ao Investimento" else "Resgatar do Investimento",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                
                Text(
                    text = "${investment.title} (Saldo Atual: ${FormatUtils.formatCurrency(investment.amount)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        amountError = false
                        errorMsg = ""
                    },
                    label = { Text("Valor (R$)") },
                    isError = amountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OceanBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )

                if (amountError && errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = CoralRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Date Selection Field
                val context = androidx.compose.ui.platform.LocalContext.current
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = selectedDate
                val datePickerDialog = android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedCal = java.util.Calendar.getInstance()
                        selectedCal.set(java.util.Calendar.YEAR, year)
                        selectedCal.set(java.util.Calendar.MONTH, month)
                        selectedCal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                        selectedDate = selectedCal.timeInMillis
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                )

                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                LaunchedEffect(isPressed) {
                    if (isPressed) {
                        datePickerDialog.show()
                    }
                }

                OutlinedTextField(
                    value = FormatUtils.formatDate(selectedDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data do Lançamento") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Selecionar data",
                            tint = OceanBlue
                        )
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OceanBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = {
                            val parsedAmount = FormatUtils.parseDouble(amountStr) ?: -1.0
                            if (parsedAmount <= 0) {
                                amountError = true
                                errorMsg = "Por favor, digite um valor maior que zero."
                            } else if (!isDeposit && parsedAmount > investment.amount) {
                                amountError = true
                                errorMsg = "O valor não pode ser maior do que o saldo atual (${FormatUtils.formatCurrency(investment.amount)})."
                            } else {
                                onConfirm(parsedAmount, selectedDate)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirmar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ManageBoxDialog(
    box: FinanceItem,
    isDeposit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, Long) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("manage_box_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isDeposit) "Guardar na Caixinha" else "Resgatar da Caixinha",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                
                Text(
                    text = "${box.title} (Saldo: ${FormatUtils.formatCurrency(box.amount)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        amountError = false
                        errorMsg = ""
                    },
                    label = { Text("Valor (R$)") },
                    isError = amountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAmber,
                        unfocusedBorderColor = BorderColor
                    )
                )

                if (amountError && errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = CoralRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Date Selection Field
                val context = androidx.compose.ui.platform.LocalContext.current
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = selectedDate
                val datePickerDialog = android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedCal = java.util.Calendar.getInstance()
                        selectedCal.set(java.util.Calendar.YEAR, year)
                        selectedCal.set(java.util.Calendar.MONTH, month)
                        selectedCal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                        selectedDate = selectedCal.timeInMillis
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                )

                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                LaunchedEffect(isPressed) {
                    if (isPressed) {
                        datePickerDialog.show()
                    }
                }

                OutlinedTextField(
                    value = FormatUtils.formatDate(selectedDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data do Lançamento") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Selecionar data",
                            tint = GoldAmber
                        )
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAmber,
                        unfocusedBorderColor = BorderColor
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = {
                            val parsedAmount = FormatUtils.parseDouble(amountStr) ?: -1.0
                            if (parsedAmount <= 0) {
                                amountError = true
                                errorMsg = "Por favor, digite um valor maior que zero."
                            } else if (!isDeposit && parsedAmount > box.amount) {
                                amountError = true
                                errorMsg = "O valor não pode ser maior do que o saldo devedor atual (${FormatUtils.formatCurrency(box.amount)})."
                            } else {
                                onConfirm(parsedAmount, selectedDate)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirmar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
