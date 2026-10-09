package com.example.ui.screens

import android.app.DatePickerDialog
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
import com.example.data.BoxMovement
import com.example.data.FinanceItem
import com.example.ui.theme.*
import com.example.ui.utils.FormatUtils
import com.example.ui.utils.MoneyUtils
import java.util.Calendar

object BoxCalculator {
    fun calculateBalanceCents(box: FinanceItem, movements: List<BoxMovement>): Long {
        val boxMovs = movements.filter { it.boxId == box.id }
        return if (boxMovs.isNotEmpty()) {
            boxMovs.fold(0L) { acc, m ->
                if (m.isDeposit) acc + m.amountCents else acc - m.amountCents
            }.coerceAtLeast(0L)
        } else {
            box.amountCents
        }
    }
}

@Composable
fun BoxesScreen(
    items: List<FinanceItem>,
    boxMovements: List<BoxMovement> = emptyList(),
    onAddItem: (FinanceItem) -> Unit,
    onAddMovement: (boxId: Int, amountCents: Long, isDeposit: Boolean, date: Long, note: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _, s, _ -> s() },
    onDeleteMovement: (movement: BoxMovement) -> Unit = {},
    onUpdateItem: (FinanceItem) -> Unit,
    onDeleteItem: (FinanceItem) -> Unit,
    onProfileClick: () -> Unit
) {
    val boxes = items.filter { it.type == "BOX" }
    val totalBoxesAmountCents = boxes.sumOf { BoxCalculator.calculateBalanceCents(it, boxMovements) }
    val completedBoxesCount = boxes.count { box ->
        val balance = BoxCalculator.calculateBalanceCents(box, boxMovements)
        val target = box.targetAmountCents
        target > 0L && balance >= target
    }

    var selectedBoxForManage by remember { mutableStateOf<FinanceItem?>(null) }
    var selectedBoxForDetails by remember { mutableStateOf<FinanceItem?>(null) }
    var isDepositMode by remember { mutableStateOf(true) }
    var showCreateBoxDialog by remember { mutableStateOf(false) }

    // Keep active detail box in sync with list
    val activeDetailsBox = selectedBoxForDetails?.let { current ->
        boxes.find { it.id == current.id }
    }

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
                            text = "Caixinhas & Metas",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Guarde dinheiro com foco em seus objetivos e sonhos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { showCreateBoxDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = GoldAmber.copy(alpha = 0.15f),
                                contentColor = GoldAmber
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Nova Caixinha", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nova")
                        }

                        IconButton(
                            onClick = onProfileClick,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
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
            // Summary Hero Card
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
                                "Total Poupado em Caixinhas",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                MoneyUtils.formatCents(totalBoxesAmountCents),
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = GoldAmber
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${boxes.size} caixinhas criadas • $completedBoxesCount concluídas",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(GoldAmber.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Savings,
                                contentDescription = "Caixinhas",
                                tint = GoldAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Suas Metas Ativas",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    if (boxes.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(GoldAmber.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${boxes.size} ${if (boxes.size == 1) "Caixinha" else "Caixinhas"}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = GoldAmber
                            )
                        }
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
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(GoldAmber.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Savings,
                                    contentDescription = "Nenhuma caixinha",
                                    tint = GoldAmber,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Nenhuma caixinha criada ainda",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Crie caixinhas para metas específicas como 'Viagem', 'Reserva de Emergência' ou 'Carro Novo'. Toque no card para ver o histórico de depósitos e resgates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateBoxDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAmber, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Criar Primeira Caixinha", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(boxes, key = { it.id }) { box ->
                    val movements = boxMovements.filter { it.boxId == box.id }
                    val currentBalanceCents = BoxCalculator.calculateBalanceCents(box, movements)

                    BoxGoalCard(
                        box = box,
                        currentBalanceCents = currentBalanceCents,
                        movementsCount = movements.size,
                        onClick = { selectedBoxForDetails = box },
                        onManage = { isDeposit ->
                            selectedBoxForManage = box
                            isDepositMode = isDeposit
                        },
                        onDelete = { onDeleteItem(box) }
                    )
                }
            }
        }
    }

    // Modal de Extrato e Detalhes da Caixinha
    activeDetailsBox?.let { box ->
        val movements = boxMovements.filter { it.boxId == box.id }.sortedByDescending { it.date }
        val currentBalanceCents = BoxCalculator.calculateBalanceCents(box, movements)

        BoxDetailsDialog(
            box = box,
            currentBalanceCents = currentBalanceCents,
            movements = movements,
            onDismiss = { selectedBoxForDetails = null },
            onDepositClick = {
                selectedBoxForManage = box
                isDepositMode = true
            },
            onWithdrawClick = {
                selectedBoxForManage = box
                isDepositMode = false
            },
            onDeleteMovement = onDeleteMovement,
            onDeleteBox = {
                onDeleteItem(box)
                selectedBoxForDetails = null
            }
        )
    }

    // Dialog to create a new box directly
    if (showCreateBoxDialog) {
        CreateBoxDialog(
            onDismiss = { showCreateBoxDialog = false },
            onConfirm = { newBox, initialCents ->
                onAddItem(newBox)
                showCreateBoxDialog = false
            }
        )
    }

    // Dialog for managing Box Deposits or Withdraws
    selectedBoxForManage?.let { box: FinanceItem ->
        val movements = boxMovements.filter { it.boxId == box.id }
        val currentBalanceCents = BoxCalculator.calculateBalanceCents(box, movements)

        ManageBoxDialog(
            box = box,
            currentBalanceCents = currentBalanceCents,
            isDeposit = isDepositMode,
            onDismiss = { selectedBoxForManage = null },
            onConfirm = { amountCents: Long, selectedDate: Long, note: String ->
                onAddMovement(box.id, amountCents, isDepositMode, selectedDate, note, {
                    selectedBoxForManage = null
                }, { _ -> })
            }
        )
    }
}

@Composable
fun BoxGoalCard(
    box: FinanceItem,
    currentBalanceCents: Long,
    movementsCount: Int,
    onClick: () -> Unit,
    onManage: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val targetCents = box.targetAmountCents
    val progress = if (targetCents > 0L) {
        (currentBalanceCents.toFloat() / targetCents.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val progressPercent = (progress * 100).toInt()
    val isCompleted = targetCents > 0L && currentBalanceCents >= targetCents

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("box_card_${box.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.12f))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(GoldAmber.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Savings,
                            contentDescription = null,
                            tint = if (isCompleted) EmeraldGreen else GoldAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = box.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (box.description.isNotBlank()) {
                                Text(
                                    text = box.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("•", color = TextSecondary.copy(alpha = 0.5f))
                            }
                            Text(
                                text = "$movementsCount movimentações",
                                style = MaterialTheme.typography.bodySmall,
                                color = OceanBlue
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Excluir",
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Balances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Guardado", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        MoneyUtils.formatCents(currentBalanceCents),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isCompleted) EmeraldGreen else GoldAmber
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Meta", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        MoneyUtils.formatCents(targetCents),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isCompleted) EmeraldGreen else GoldAmber,
                    trackColor = BorderColor.copy(alpha = 0.2f)
                )
                Text(
                    text = if (isCompleted) "🎉 Meta Atingida!" else "$progressPercent% concluído",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isCompleted) EmeraldGreen else TextSecondary,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Deposit & Withdraw buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onManage(false) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = currentBalanceCents > 0L
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resgatar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = { onManage(true) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAmber, contentColor = Color.Black)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Guardar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun BoxDetailsDialog(
    box: FinanceItem,
    currentBalanceCents: Long,
    movements: List<BoxMovement>,
    onDismiss: () -> Unit,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onDeleteMovement: (BoxMovement) -> Unit,
    onDeleteBox: () -> Unit
) {
    val targetCents = box.targetAmountCents
    val progress = if (targetCents > 0L) {
        (currentBalanceCents.toFloat() / targetCents.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val progressPercent = (progress * 100).toInt()
    val isCompleted = targetCents > 0L && currentBalanceCents >= targetCents

    var movementToDelete by remember { mutableStateOf<BoxMovement?>(null) }
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
                                        text = "Ficha da Caixinha",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = box.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GoldAmber
                                    )
                                }
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
                                onClick = onWithdrawClick,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = currentBalanceCents > 0L
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resgatar", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onDepositClick,
                                modifier = Modifier.weight(1.3f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAmber, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Novo Aporte", fontWeight = FontWeight.Bold)
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
                    // Hero Card: Overview
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
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text("Saldo Atual Guardado", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Text(
                                            MoneyUtils.formatCents(currentBalanceCents),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.SansSerif
                                            ),
                                            color = if (isCompleted) EmeraldGreen else GoldAmber
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCompleted) EmeraldGreen.copy(alpha = 0.15f) else GoldAmber.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isCompleted) "META CONCLUÍDA" else "EM ANDAMENTO",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isCompleted) EmeraldGreen else GoldAmber,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricChip(
                                        label = "Meta Final",
                                        value = MoneyUtils.formatCents(targetCents),
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricChip(
                                        label = "Movimentações",
                                        value = "${movements.size}",
                                        color = OceanBlue,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricChip(
                                        label = "Conclusão",
                                        value = "$progressPercent%",
                                        color = if (isCompleted) EmeraldGreen else GoldAmber,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (isCompleted) EmeraldGreen else GoldAmber,
                                    trackColor = BorderColor.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    // Section Title: Extrato de Depósitos e Resgates
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
                                text = "Ordem cronológica reversa",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    if (movements.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Nenhuma movimentação registrada",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextSecondary
                                    )
                                    Text(
                                        "Clique em 'Novo Aporte' para guardar recursos nesta caixinha.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(movements, key = { it.id }) { movement ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (movement.isDeposit) GoldAmber.copy(alpha = 0.18f) else CoralRed.copy(alpha = 0.18f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (movement.isDeposit) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (movement.isDeposit) GoldAmber else CoralRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (movement.isDeposit) "Aporte / Depósito" else "Resgate / Retirada",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
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
                                        if (movement.note.isNotBlank()) {
                                            Text(
                                                text = movement.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OceanBlue
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (movement.isDeposit) "+" else "-"} ${MoneyUtils.formatCents(movement.amountCents)}",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.SansSerif
                                            ),
                                            color = if (movement.isDeposit) GoldAmber else CoralRed
                                        )

                                        IconButton(
                                            onClick = { movementToDelete = movement },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "Estornar movimentação",
                                                tint = TextSecondary.copy(alpha = 0.6f),
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
    }

    // Modal de Estorno de Movimentação Específica
    movementToDelete?.let { movement ->
        AlertDialog(
            onDismissRequest = { movementToDelete = null },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = GoldAmber) },
            title = { Text("Estornar Movimentação") },
            text = {
                val tipo = if (movement.isDeposit) "o depósito" else "o resgate"
                Text("Deseja estornar $tipo de ${MoneyUtils.formatCents(movement.amountCents)} realizado em ${FormatUtils.formatDate(movement.date)}? O saldo da caixinha será recalculado automaticamente.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMovement(movement)
                        movementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = Color.White)
                ) {
                    Text("Confirmar Estorno")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { movementToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Confirmação para Excluir Caixinha Inteira
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = CoralRed) },
            title = { Text("Excluir Caixinha") },
            text = { Text("Tem certeza que deseja excluir esta caixinha e todo o histórico de aportes e resgates vinculados?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteBox()
                        onDismiss()
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
fun CreateBoxDialog(
    onDismiss: () -> Unit,
    onConfirm: (FinanceItem, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("") }
    var initialAmountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var targetError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Nova Caixinha / Meta",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Defina sua meta e comece a poupar",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = TextSecondary)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("Nome da Meta (Ex: Viagem, Reserva)") },
                    isError = titleError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetAmountStr,
                    onValueChange = {
                        targetAmountStr = it
                        targetError = false
                    },
                    label = { Text("Meta Final (R$)") },
                    isError = targetError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = initialAmountStr,
                    onValueChange = { initialAmountStr = it },
                    label = { Text("Valor Inicial Guardado (Opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Observações (Opcional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            var hasError = false
                            if (title.isBlank()) {
                                titleError = true
                                hasError = true
                            }
                            val targetCents = MoneyUtils.parseInputToCents(targetAmountStr)
                            if (targetCents <= 0L) {
                                targetError = true
                                hasError = true
                            }
                            val initialCents = MoneyUtils.parseInputToCents(initialAmountStr)

                            if (!hasError) {
                                val item = FinanceItem(
                                    title = title.trim(),
                                    amountCents = initialCents,
                                    type = "BOX",
                                    category = "Caixinha",
                                    targetAmountCents = targetCents,
                                    description = description.trim(),
                                    date = System.currentTimeMillis(),
                                    isCompleted = initialCents >= targetCents
                                )
                                onConfirm(item, initialCents)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Criar Caixinha", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ManageBoxDialog(
    box: FinanceItem,
    currentBalanceCents: Long,
    isDeposit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, String) -> Unit
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
                .wrapContentHeight(),
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
                    text = "${box.title} • Saldo Atual: ${MoneyUtils.formatCents(currentBalanceCents)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldAmber
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
                    label = { Text("Data da Operação") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Selecionar data",
                            tint = GoldAmber
                        )
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Observação (Opcional)") },
                    placeholder = { Text("Ex: Aporte do salário, Economia...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (amountError && errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        color = CoralRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val parsedCents = MoneyUtils.parseInputToCents(amountStr)
                            if (parsedCents <= 0L) {
                                amountError = true
                                errorMsg = "Por favor, digite um valor maior que zero."
                            } else if (!isDeposit && parsedCents > currentBalanceCents) {
                                amountError = true
                                errorMsg = "Saldo insuficiente! Saldo disponível: ${MoneyUtils.formatCents(currentBalanceCents)}."
                            } else {
                                onConfirm(parsedCents, selectedDate, note.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirmar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
