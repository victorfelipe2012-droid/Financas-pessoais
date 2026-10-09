package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.app.DatePickerDialog
import com.example.data.FinanceItem
import com.example.data.LoanPayment
import com.example.ui.theme.*
import com.example.ui.utils.FormatUtils
import com.example.ui.utils.MoneyUtils
import java.util.Calendar

data class LentMovement(
    val id: String,
    val paymentId: Long? = null,
    val paymentObj: LoanPayment? = null,
    val amount: Double,
    val amountCents: Long = 0L,
    val date: Long,
    val rawLine: String = "",
    val note: String = "",
    val isCreation: Boolean = false
)

object LentMovementParser {
    fun getMovements(lent: FinanceItem, payments: List<LoanPayment> = emptyList()): List<LentMovement> {
        val itemPayments = payments.filter { it.loanId == lent.id }.sortedBy { it.paymentDate }
        if (itemPayments.isNotEmpty()) {
            val totalPaidCents = itemPayments.sumOf { it.amountCents }
            val originalCents = if (lent.targetAmountCents > 0) lent.targetAmountCents else (lent.amountCents + totalPaidCents)

            val creation = LentMovement(
                id = "creation_${lent.id}",
                amount = MoneyUtils.centsToDouble(originalCents),
                amountCents = originalCents,
                date = lent.date,
                rawLine = "",
                note = "Concessão do Empréstimo",
                isCreation = true
            )

            val list = itemPayments.map { p ->
                LentMovement(
                    id = "payment_${p.id}",
                    paymentId = p.id,
                    paymentObj = p,
                    amount = MoneyUtils.centsToDouble(p.amountCents),
                    amountCents = p.amountCents,
                    date = p.paymentDate,
                    rawLine = "",
                    note = p.note,
                    isCreation = false
                )
            }
            return listOf(creation) + list
        }
        return parseMovements(lent)
    }

    fun parseMovements(lent: FinanceItem): List<LentMovement> {
        val list = mutableListOf<LentMovement>()
        val initialDate = lent.date

        if (lent.description.isNotBlank()) {
            val lines = lent.description.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                if (trimmed.contains("Abatido", ignoreCase = true)) {
                    val amountMatch = Regex("""R\$\s*([\d\.,]+)""").find(trimmed)
                    val dateMatch = Regex("""\b(\d{1,2}/\d{1,2}/\d{4})\b""").find(trimmed)
                    val noteMatch = if (trimmed.contains("-")) trimmed.substringAfter("-").trim() else ""

                    val parsedAmount = amountMatch?.groupValues?.get(1)?.let {
                        FormatUtils.parseDouble(it)
                    } ?: 0.0

                    val parsedDate = dateMatch?.groupValues?.get(1)?.let { dateStr ->
                        try {
                            val parts = dateStr.split("/")
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.DAY_OF_MONTH, parts[0].toInt())
                            cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                            cal.set(Calendar.YEAR, parts[2].toInt())
                            cal.timeInMillis
                        } catch (e: Exception) {
                            null
                        }
                    } ?: initialDate

                    list.add(
                        LentMovement(
                            id = trimmed.hashCode().toString(),
                            amount = parsedAmount,
                            amountCents = MoneyUtils.toCents(parsedAmount),
                            date = parsedDate,
                            rawLine = trimmed,
                            note = noteMatch,
                            isCreation = false
                        )
                    )
                }
            }
        }

        val totalAbatedCents = list.sumOf { it.amountCents }
        val originalCents = if (lent.targetAmountCents > 0L) lent.targetAmountCents else (lent.amountCents + totalAbatedCents)
        val originalAmount = MoneyUtils.centsToDouble(originalCents)

        val creationMovement = LentMovement(
            id = "creation_${lent.id}",
            amount = originalAmount,
            amountCents = originalCents,
            date = initialDate,
            rawLine = "",
            note = "Concessão do Empréstimo",
            isCreation = true
        )

        return listOf(creationMovement) + list
    }
}

@Composable
fun LentAndBillsScreen(
    items: List<FinanceItem>,
    loanPayments: List<LoanPayment> = emptyList(),
    onAddItem: (FinanceItem) -> Unit = {},
    onUpdateItem: (FinanceItem) -> Unit,
    onDeleteItem: (FinanceItem) -> Unit,
    onAddPayment: (loanId: Int, amountCents: Long, date: Long, note: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, s, _ -> s() },
    onDeletePayment: (payment: LoanPayment) -> Unit = {},
    onSettleLoan: (loanId: Int, date: Long, note: String) -> Unit = { _, _, _ -> },
    onReopenLoan: (loanId: Int, removeLastPayment: Boolean) -> Unit = { _, _ -> },
    onUpdatePrincipal: (loanId: Int, newPrincipalCents: Long, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, s, _ -> s() },
    onProfileClick: () -> Unit
) {
    val lentItems = items.filter { it.type == "LENT" }
    
    var selectedTabFilter by remember { mutableIntStateOf(0) } // 0: Todos, 1: Em Aberto, 2: Quitados
    var selectedLentForDetails by remember { mutableStateOf<FinanceItem?>(null) }
    var selectedLentForAbatement by remember { mutableStateOf<FinanceItem?>(null) }
    var selectedLentForEdit by remember { mutableStateOf<FinanceItem?>(null) }
    var showCreateLentDialog by remember { mutableStateOf(false) }
    var lentToDelete by remember { mutableStateOf<FinanceItem?>(null) }
    var loanToReopenChoice by remember { mutableStateOf<FinanceItem?>(null) }

    // Keep selectedLentForDetails updated if items list updates
    val activeDetailsLent = selectedLentForDetails?.let { current ->
        lentItems.find { it.id == current.id }
    }

    val filteredLentItems = remember(lentItems, selectedTabFilter) {
        when (selectedTabFilter) {
            1 -> lentItems.filter { !it.isCompleted }
            2 -> lentItems.filter { it.isCompleted }
            else -> lentItems
        }
    }

    val totalActiveLentCents = lentItems.filter { !it.isCompleted }.sumOf { item ->
        val payments = loanPayments.filter { it.loanId == item.id }
        if (payments.isNotEmpty()) {
            val orig = if (item.targetAmountCents > 0) item.targetAmountCents else (item.amountCents + payments.sumOf { it.amountCents })
            (orig - payments.sumOf { it.amountCents }).coerceAtLeast(0L)
        } else {
            item.amountCents
        }
    }

    val totalOriginalLentCents = lentItems.sumOf { item ->
        val payments = loanPayments.filter { it.loanId == item.id }
        if (payments.isNotEmpty()) {
            if (item.targetAmountCents > 0) item.targetAmountCents else (item.amountCents + payments.sumOf { it.amountCents })
        } else {
            val movements = LentMovementParser.parseMovements(item)
            val abated = movements.filter { !it.isCreation }.sumOf { it.amountCents }
            if (item.targetAmountCents > 0) item.targetAmountCents else (item.amountCents + abated)
        }
    }
    val totalRecoveredCents = (totalOriginalLentCents - totalActiveLentCents).coerceAtLeast(0L)

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Empréstimos",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Acompanhe devedores, pagamentos e extratos individuais.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { showCreateLentDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = LavenderPurple.copy(alpha = 0.15f),
                                contentColor = LavenderPurple
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Novo Empréstimo", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Novo", fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onProfileClick,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .testTag("lent_bills_profile_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = "Perfil",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Tabs (Todos, Em Aberto, Quitados)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTabFilter == 0,
                        onClick = { selectedTabFilter = 0 },
                        label = { Text("Todos (${lentItems.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LavenderPurple.copy(alpha = 0.2f),
                            selectedLabelColor = LavenderPurple
                        )
                    )
                    FilterChip(
                        selected = selectedTabFilter == 1,
                        onClick = { selectedTabFilter = 1 },
                        label = { Text("Em Aberto (${lentItems.count { !it.isCompleted }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAmber.copy(alpha = 0.2f),
                            selectedLabelColor = GoldAmber
                        )
                    )
                    FilterChip(
                        selected = selectedTabFilter == 2,
                        onClick = { selectedTabFilter = 2 },
                        label = { Text("Quitados (${lentItems.count { it.isCompleted }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldGreen
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredLentItems.isEmpty()) {
                EmptyListPlaceholder(
                    icon = Icons.Rounded.Handshake,
                    title = if (lentItems.isEmpty()) "Nenhum empréstimo registrado!" else "Nenhum empréstimo neste filtro!",
                    subtitle = if (lentItems.isEmpty()) 
                        "Clique em '+ Novo' para registrar seu primeiro empréstimo e acompanhar as parcelas recebidas."
                        else "Selecione outra aba para visualizar os empréstimos cadastrados.",
                    color = LavenderPurple
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // KPI Overview Card
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
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Saldo Ativo a Receber",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = MoneyUtils.formatCents(totalActiveLentCents),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.SansSerif
                                            ),
                                            color = LavenderPurple
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(LavenderPurple.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Handshake,
                                            contentDescription = null,
                                            tint = LavenderPurple,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Divider(color = BorderColor.copy(alpha = 0.15f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Emprestado", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Text(
                                            MoneyUtils.formatCents(totalOriginalLentCents),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Já Recuperado", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Text(
                                            MoneyUtils.formatCents(totalRecoveredCents),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldGreen
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section Title
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Meus Empréstimos",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Toque no card para ver movimentações",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // List of loans
                    items(filteredLentItems, key = { it.id }) { lent ->
                        val paymentsForLent = loanPayments.filter { it.loanId == lent.id }
                        LentItemCard(
                            lent = lent,
                            payments = paymentsForLent,
                            onClick = { selectedLentForDetails = lent },
                            onToggleReturned = {
                                if (lent.isCompleted) {
                                    loanToReopenChoice = lent
                                } else {
                                    onSettleLoan(lent.id, System.currentTimeMillis(), "Quitação direta")
                                }
                            },
                            onAbateClick = { selectedLentForAbatement = lent },
                            onDelete = { lentToDelete = lent }
                        )
                    }
                }
            }
        }
    }

    // Modal de Detalhes e Movimentações
    activeDetailsLent?.let { lent ->
        val paymentsForLent = loanPayments.filter { it.loanId == lent.id }
        LentDetailsDialog(
            lent = lent,
            payments = paymentsForLent,
            onDismiss = { selectedLentForDetails = null },
            onAbateClick = { selectedLentForAbatement = lent },
            onEditClick = { selectedLentForEdit = lent },
            onSettle = {
                onSettleLoan(lent.id, System.currentTimeMillis(), "Quitação integral")
            },
            onReopen = {
                loanToReopenChoice = lent
            },
            onDeleteMovement = { movement ->
                if (movement.paymentObj != null) {
                    onDeletePayment(movement.paymentObj)
                } else {
                    // Fallback para histórico legado em texto
                    val lines = lent.description.lines().filterNot { it.trim() == movement.rawLine.trim() }
                    val newDesc = lines.joinToString("\n").trim()
                    val movements = LentMovementParser.parseMovements(lent).filterNot { it.id == movement.id }
                    val totalAbatedCents = movements.filter { !it.isCreation }.sumOf { it.amountCents }
                    val originalAmountCents = if (lent.targetAmountCents > 0L) lent.targetAmountCents else (lent.amountCents + movement.amountCents + totalAbatedCents)
                    val newAmountCents = maxOf(0L, originalAmountCents - totalAbatedCents)

                    val updated = lent.copy(
                        amountCents = newAmountCents,
                        targetAmountCents = originalAmountCents,
                        isCompleted = newAmountCents <= 0L,
                        description = newDesc
                    )
                    onUpdateItem(updated)
                }
            },
            onDeleteLent = {
                onDeleteItem(lent)
                selectedLentForDetails = null
            }
        )
    }

    // Modal de Novo Empréstimo
    if (showCreateLentDialog) {
        CreateLentDialog(
            onDismiss = { showCreateLentDialog = false },
            onConfirm = { title, amount, date, notes ->
                val cents = MoneyUtils.toCents(amount)
                val item = FinanceItem(
                    title = title.trim(),
                    amountCents = cents,
                    targetAmountCents = cents,
                    type = "LENT",
                    category = "Empréstimo",
                    date = date,
                    description = notes.trim(),
                    isCompleted = false
                )
                onAddItem(item)
                showCreateLentDialog = false
            }
        )
    }

    // Modal de Edição de Empréstimo
    selectedLentForEdit?.let { lent ->
        val payments = loanPayments.filter { it.loanId == lent.id }
        val totalPaidCents = if (payments.isNotEmpty()) payments.sumOf { it.amountCents } else {
            val movements = LentMovementParser.parseMovements(lent)
            movements.filter { !it.isCreation }.sumOf { it.amountCents }
        }
        val originalCents = if (lent.targetAmountCents > 0) lent.targetAmountCents else (lent.amountCents + totalPaidCents)

        EditLentDialog(
            lent = lent,
            originalAmount = MoneyUtils.centsToDouble(originalCents),
            minAllowedCents = totalPaidCents,
            onDismiss = { selectedLentForEdit = null },
            onConfirm = { newTitle, newOrigAmount, newDate ->
                val newOrigCents = MoneyUtils.toCents(newOrigAmount)
                onUpdatePrincipal(lent.id, newOrigCents, {
                    val currentCents = (newOrigCents - totalPaidCents).coerceAtLeast(0L)
                    onUpdateItem(lent.copy(
                        title = newTitle.trim(),
                        amountCents = currentCents,
                        targetAmountCents = newOrigCents,
                        date = newDate,
                        isCompleted = currentCents == 0L
                    ))
                    selectedLentForEdit = null
                }, { error ->
                    // Exibido via Toast ou diálogo interno
                })
            }
        )
    }

    // Modal de Abater Parcela
    selectedLentForAbatement?.let { lent ->
        val payments = loanPayments.filter { it.loanId == lent.id }
        val remainingCents = if (payments.isNotEmpty()) {
            val orig = if (lent.targetAmountCents > 0) lent.targetAmountCents else (lent.amountCents + payments.sumOf { it.amountCents })
            (orig - payments.sumOf { it.amountCents }).coerceAtLeast(0L)
        } else lent.amountCents

        AbateLentDialog(
            lent = lent,
            maxAllowedCents = remainingCents,
            onDismiss = { selectedLentForAbatement = null },
            onConfirm = { amount, selectedDate, note ->
                val cents = MoneyUtils.toCents(amount)
                onAddPayment(lent.id, cents, selectedDate, note, {
                    selectedLentForAbatement = null
                }, { _ -> })
            }
        )
    }

    // Modal de Escolha ao Reabrir
    loanToReopenChoice?.let { lent ->
        AlertDialog(
            onDismissRequest = { loanToReopenChoice = null },
            icon = { Icon(Icons.Rounded.Undo, contentDescription = null, tint = GoldAmber) },
            title = { Text("Reabrir Empréstimo") },
            text = {
                Text("Este empréstimo está marcado como quitado. Como deseja reabri-lo?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReopenLoan(lent.id, true)
                        loanToReopenChoice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAmber, contentColor = Color.Black)
                ) {
                    Text("Estornar Última Quitação", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        onReopenLoan(lent.id, false)
                        loanToReopenChoice = null
                    }
                ) {
                    Text("Manter Pagamentos & Reabrir")
                }
            }
        )
    }

    // Confirmação de Exclusão
    lentToDelete?.let { lent ->
        AlertDialog(
            onDismissRequest = { lentToDelete = null },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = CoralRed) },
            title = { Text("Excluir Empréstimo") },
            text = { Text("Deseja realmente excluir o empréstimo para '${lent.title}'? Todo o histórico de amortizações será apagado.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(lent)
                        lentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = Color.White)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { lentToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun LentItemCard(
    lent: FinanceItem,
    payments: List<LoanPayment> = emptyList(),
    onClick: () -> Unit,
    onToggleReturned: () -> Unit,
    onAbateClick: () -> Unit,
    onDelete: () -> Unit
) {
    val movements = remember(lent, payments) {
        LentMovementParser.getMovements(lent, payments)
    }
    val totalAbatedCents = movements.filter { !it.isCreation }.sumOf { it.amountCents }
    val originalAmountCents = if (lent.targetAmountCents > 0) lent.targetAmountCents else (lent.amountCents + totalAbatedCents)
    val remainingCents = (originalAmountCents - totalAbatedCents).coerceAtLeast(0L)
    val progressPercent = if (originalAmountCents > 0) {
        ((totalAbatedCents.toDouble() / originalAmountCents) * 100).toInt().coerceIn(0, 100)
    } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("lent_card_${lent.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Icon, Title, Status & Saldo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (lent.isCompleted) EmeraldGreen.copy(alpha = 0.15f) else LavenderPurple.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (lent.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Handshake,
                        contentDescription = "Empréstimo",
                        tint = if (lent.isCompleted) EmeraldGreen else LavenderPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lent.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (lent.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        ),
                        color = if (lent.isCompleted) TextSecondary else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Original: ${MoneyUtils.formatCents(originalAmountCents)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text("•", color = TextSecondary.copy(alpha = 0.5f))
                        Text(
                            text = "${movements.size - 1} amortizações",
                            style = MaterialTheme.typography.bodySmall,
                            color = OceanBlue
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = MoneyUtils.formatCents(remainingCents),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif
                        ),
                        color = if (lent.isCompleted) EmeraldGreen else LavenderPurple,
                        softWrap = false,
                        maxLines = 1
                    )

                    val statusText = if (lent.isCompleted) "Quitado" else "Em Aberto"
                    val statusColor = if (lent.isCompleted) EmeraldGreen else GoldAmber
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar: % Devolvido
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progresso de Quitação",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "$progressPercent% devolvido",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (lent.isCompleted) EmeraldGreen else LavenderPurple
                    )
                }

                LinearProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (lent.isCompleted) EmeraldGreen else LavenderPurple,
                    trackColor = BorderColor.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BorderColor.copy(alpha = 0.12f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer row with Tap Guide and Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = OceanBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Ver extrato & parcelas",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = OceanBlue
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!lent.isCompleted) {
                        FilledTonalButton(
                            onClick = onAbateClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = LavenderPurple.copy(alpha = 0.15f),
                                contentColor = LavenderPurple
                            ),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Abater", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Excluir",
                            tint = TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LentDetailsDialog(
    lent: FinanceItem,
    payments: List<LoanPayment> = emptyList(),
    onDismiss: () -> Unit,
    onAbateClick: () -> Unit,
    onEditClick: () -> Unit,
    onSettle: () -> Unit,
    onReopen: () -> Unit,
    onDeleteMovement: (LentMovement) -> Unit,
    onDeleteLent: () -> Unit
) {
    val movements = remember(lent, payments) {
        LentMovementParser.getMovements(lent, payments)
    }
    val totalAbatedCents = movements.filter { !it.isCreation }.sumOf { it.amountCents }
    val originalAmountCents = if (lent.targetAmountCents > 0) lent.targetAmountCents else (lent.amountCents + totalAbatedCents)
    val remainingCents = (originalAmountCents - totalAbatedCents).coerceAtLeast(0L)
    val progressPercent = if (originalAmountCents > 0) {
        ((totalAbatedCents.toDouble() / originalAmountCents) * 100).toInt().coerceIn(0, 100)
    } else 0

    var movementToDelete by remember { mutableStateOf<LentMovement?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(MaterialTheme.colorScheme.background, CircleShape)
                                ) {
                                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                                }
                                Column {
                                    Text(
                                        text = "Ficha do Empréstimo",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = lent.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LavenderPurple
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = onEditClick,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(MaterialTheme.colorScheme.background, CircleShape)
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Editar", tint = TextPrimary, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(CoralRed.copy(alpha = 0.12f), CircleShape)
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = CoralRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (lent.isCompleted) onReopen() else onSettle()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (lent.isCompleted) GoldAmber else EmeraldGreen)
                            ) {
                                Icon(
                                    imageVector = if (lent.isCompleted) Icons.Rounded.Undo else Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = if (lent.isCompleted) GoldAmber else EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (lent.isCompleted) "Reabrir" else "Quitar",
                                    color = if (lent.isCompleted) GoldAmber else EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = onAbateClick,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LavenderPurple,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Novo Abatimento", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Card: Overview do Empréstimo
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text(
                                            text = "Saldo Devedor Restante",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = MoneyUtils.formatCents(remainingCents),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.SansSerif
                                            ),
                                            color = if (lent.isCompleted) EmeraldGreen else LavenderPurple
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (lent.isCompleted) EmeraldGreen.copy(alpha = 0.15f) else GoldAmber.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (lent.isCompleted) "QUITADO" else "EM ABERTO",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (lent.isCompleted) EmeraldGreen else GoldAmber,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // 3 Metric Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricChip(
                                        label = "Original",
                                        value = MoneyUtils.formatCents(originalAmountCents),
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricChip(
                                        label = "Total Pago",
                                        value = MoneyUtils.formatCents(totalAbatedCents),
                                        color = EmeraldGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricChip(
                                        label = "Progresso",
                                        value = "$progressPercent%",
                                        color = LavenderPurple,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Progress Bar
                                LinearProgressIndicator(
                                    progress = { progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (lent.isCompleted) EmeraldGreen else LavenderPurple,
                                    trackColor = BorderColor.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    // Section Title: Extrato de Movimentações
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extrato de Movimentações (${movements.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Ordem cronológica",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // List of timeline movements
                    items(movements, key = { it.id }) { movement ->
                        MovementItemRow(
                            movement = movement,
                            onDelete = if (!movement.isCreation) {
                                { movementToDelete = movement }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Modal de Confirmação para Excluir Movimentação Específica
    movementToDelete?.let { movement ->
        AlertDialog(
            onDismissRequest = { movementToDelete = null },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = GoldAmber) },
            title = { Text("Excluir Abatimento") },
            text = { Text("Deseja cancelar o abatimento de ${FormatUtils.formatCurrency(movement.amount)} de ${FormatUtils.formatDate(movement.date)}? O valor retornará ao saldo devedor.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMovement(movement)
                        movementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = Color.White)
                ) {
                    Text("Excluir Abatimento")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { movementToDelete = null }) {
                    Text("Voltar")
                }
            }
        )
    }

    // Modal de Confirmação para Excluir Empréstimo Inteiro
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = CoralRed) },
            title = { Text("Excluir Empréstimo") },
            text = { Text("Tem certeza que deseja excluir o empréstimo '${lent.title}' e todo o seu histórico?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteLent()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = Color.White)
                ) {
                    Text("Excluir Definitivamente")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun MetricChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MovementItemRow(
    movement: LentMovement,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (movement.isCreation) 
                LavenderPurple.copy(alpha = 0.08f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            1.dp, 
            if (movement.isCreation) LavenderPurple.copy(alpha = 0.25f) else BorderColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (movement.isCreation) LavenderPurple.copy(alpha = 0.18f) else EmeraldGreen.copy(alpha = 0.18f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (movement.isCreation) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    contentDescription = null,
                    tint = if (movement.isCreation) LavenderPurple else EmeraldGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (movement.isCreation) "Empréstimo Concedido" else "Amortização / Parcela Recebida",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = FormatUtils.formatDate(movement.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (movement.note.isNotBlank() && !movement.isCreation) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = movement.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = OceanBlue
                    )
                }
            }

            // Amount and Action
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (movement.isCreation) "-" else "+"} ${FormatUtils.formatCurrency(movement.amount)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif
                    ),
                    color = if (movement.isCreation) LavenderPurple else EmeraldGreen
                )

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Remover amortização",
                            tint = TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateLentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Long, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var errorMsg by remember { mutableStateOf("") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = selectedDate

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCal = Calendar.getInstance()
            selectedCal.set(Calendar.YEAR, year)
            selectedCal.set(Calendar.MONTH, month)
            selectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            selectedDate = selectedCal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Novo Empréstimo",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMsg = ""
                    },
                    label = { Text("Nome da Pessoa / Finalidade") },
                    placeholder = { Text("Ex: João Silva, Reforma Primo...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPurple,
                        unfocusedBorderColor = BorderColor
                    )
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorMsg = ""
                    },
                    label = { Text("Valor Emprestado (R$)") },
                    placeholder = { Text("Ex: 500,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPurple,
                        unfocusedBorderColor = BorderColor
                    )
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
                    label = { Text("Data do Empréstimo") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Selecionar data",
                            tint = LavenderPurple
                        )
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPurple,
                        unfocusedBorderColor = BorderColor
                    )
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (Opcional)") },
                    placeholder = { Text("Ex: Combinado pagar em 2x") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPurple,
                        unfocusedBorderColor = BorderColor
                    )
                )

                if (errorMsg.isNotBlank()) {
                    Text(text = errorMsg, color = CoralRed, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val parsed = FormatUtils.parseDouble(amountStr) ?: 0.0
                            if (title.isBlank()) {
                                errorMsg = "Informe o nome da pessoa ou finalidade."
                            } else if (parsed <= 0.0) {
                                errorMsg = "Informe um valor válido maior que zero."
                            } else {
                                onConfirm(title, parsed, selectedDate, notes)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderPurple, contentColor = Color.White)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditLentDialog(
    lent: FinanceItem,
    originalAmount: Double,
    minAllowedCents: Long = 0L,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Long) -> Unit
) {
    var title by remember { mutableStateOf(lent.title) }
    var amountStr by remember { mutableStateOf(FormatUtils.formatCurrency(originalAmount).replace("R$", "").trim()) }
    var selectedDate by remember { mutableLongStateOf(lent.date) }
    var errorMsg by remember { mutableStateOf("") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = selectedDate

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCal = Calendar.getInstance()
            selectedCal.set(Calendar.YEAR, year)
            selectedCal.set(Calendar.MONTH, month)
            selectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            selectedDate = selectedCal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Editar Empréstimo",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMsg = ""
                    },
                    label = { Text("Nome da Pessoa / Finalidade") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPurple,
                        unfocusedBorderColor = BorderColor
                    )
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorMsg = ""
                    },
                    label = { Text("Valor Total Original (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPurple,
                        unfocusedBorderColor = BorderColor
                    )
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
                    label = { Text("Data de Início") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Selecionar data",
                            tint = LavenderPurple
                        )
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPurple,
                        unfocusedBorderColor = BorderColor
                    )
                )

                if (errorMsg.isNotBlank()) {
                    Text(text = errorMsg, color = CoralRed, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val parsed = FormatUtils.parseDouble(amountStr) ?: 0.0
                            val parsedCents = MoneyUtils.toCents(parsed)
                            if (title.isBlank()) {
                                errorMsg = "Informe o nome da pessoa ou finalidade."
                            } else if (parsedCents <= 0L) {
                                errorMsg = "Informe um valor válido maior que zero."
                            } else if (parsedCents < minAllowedCents) {
                                errorMsg = "O principal não pode ser menor que o total já pago (${MoneyUtils.formatCents(minAllowedCents)})."
                            } else {
                                onConfirm(title, parsed, selectedDate)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderPurple, contentColor = Color.White)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AbateLentDialog(
    lent: FinanceItem,
    maxAllowedCents: Long = Long.MAX_VALUE,
    onDismiss: () -> Unit,
    onConfirm: (Double, Long, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = selectedDate
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCal = Calendar.getInstance()
            selectedCal.set(Calendar.YEAR, year)
            selectedCal.set(Calendar.MONTH, month)
            selectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            selectedDate = selectedCal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Registrar Amortização",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Text(
                    text = "Registre um valor recebido para o empréstimo feito a ${lent.title}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Saldo Devedor Atual", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(
                        FormatUtils.formatCurrency(lent.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LavenderPurple
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        amountError = false
                        errorMsg = ""
                    },
                    label = { Text("Valor Recebido (R$)") },
                    isError = amountError,
                    placeholder = { Text("Ex: 100,00") },
                    modifier = Modifier.fillMaxWidth().testTag("abate_amount_field"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPurple,
                        unfocusedBorderColor = BorderColor
                    )
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
                    label = { Text("Data do Pagamento") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Selecionar data",
                            tint = LavenderPurple
                        )
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPurple,
                        unfocusedBorderColor = BorderColor
                    )
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Observação (Opcional)") },
                    placeholder = { Text("Ex: Pix 1ª parcela, Em dinheiro...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPurple,
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val parsedAmount = FormatUtils.parseDouble(amountStr) ?: -1.0
                            val parsedCents = MoneyUtils.toCents(parsedAmount)
                            if (parsedCents <= 0L) {
                                amountError = true
                                errorMsg = "Por favor, digite um valor maior que zero."
                            } else if (parsedCents > maxAllowedCents) {
                                amountError = true
                                errorMsg = "O valor não pode ser maior que o saldo devedor atual (${MoneyUtils.formatCents(maxAllowedCents)})."
                            } else {
                                onConfirm(parsedAmount, selectedDate, note.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("abate_confirm_btn")
                    ) {
                        Text("Confirmar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyListPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
