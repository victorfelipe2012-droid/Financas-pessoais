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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FinanceItem
import com.example.ui.theme.*
import com.example.ui.utils.FormatUtils
import java.util.Calendar
import androidx.compose.foundation.interaction.collectIsPressedAsState

@Composable
fun BoxesScreen(
    items: List<FinanceItem>,
    onAddItem: (FinanceItem) -> Unit,
    onUpdateItem: (FinanceItem) -> Unit,
    onDeleteItem: (FinanceItem) -> Unit,
    onProfileClick: () -> Unit
) {
    val boxes = items.filter { it.type == "BOX" }
    val totalBoxesAmount = boxes.sumOf { it.amount }
    val completedBoxesCount = boxes.count { it.isCompleted }

    var selectedBoxForManage by remember { mutableStateOf<FinanceItem?>(null) }
    var isDepositMode by remember { mutableStateOf(true) } // true for Deposit, false for Withdraw
    var showCreateBoxDialog by remember { mutableStateOf(false) }

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
                                FormatUtils.formatCurrency(totalBoxesAmount),
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
                                "Crie caixinhas para metas específicas como 'Viagem', 'Reserva de Emergência' ou 'Carro Novo'.",
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
        }
    }

    // Dialog to create a new box directly
    if (showCreateBoxDialog) {
        CreateBoxDialog(
            onDismiss = { showCreateBoxDialog = false },
            onConfirm = { newBox ->
                onAddItem(newBox)
                showCreateBoxDialog = false
            }
        )
    }

    // Dialog for managing Box Deposits or Withdraws
    selectedBoxForManage?.let { box: FinanceItem ->
        ManageBoxDialog(
            box = box,
            isDeposit = isDepositMode,
            onDismiss = { selectedBoxForManage = null },
            onConfirm = { amount: Double, selectedDate: Long ->
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
}

@Composable
fun BoxGoalCard(
    box: FinanceItem,
    onManage: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (box.targetAmount > 0) {
        (box.amount / box.targetAmount).toFloat().coerceIn(0f, 1f)
    } else 0f
    val progressPercent = (progress * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("box_card_${box.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.12f))
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
                            imageVector = if (box.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Savings,
                            contentDescription = null,
                            tint = if (box.isCompleted) EmeraldGreen else GoldAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = box.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        if (box.description.isNotBlank()) {
                            Text(
                                text = box.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                        FormatUtils.formatCurrency(box.amount),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (box.isCompleted) EmeraldGreen else GoldAmber
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Meta", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        FormatUtils.formatCurrency(box.targetAmount),
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
                    color = if (box.isCompleted) EmeraldGreen else GoldAmber,
                    trackColor = BorderColor.copy(alpha = 0.2f)
                )
                Text(
                    text = if (box.isCompleted) "🎉 Meta Atingida!" else "$progressPercent% concluído",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (box.isCompleted) EmeraldGreen else TextSecondary,
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
                    enabled = box.amount > 0
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
fun CreateBoxDialog(
    onDismiss: () -> Unit,
    onConfirm: (FinanceItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var initialAmountStr by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var titleError by remember { mutableStateOf(false) }
    var targetError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
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
                    label = { Text("Nome da Meta (Ex: Viagem de Férias, Reserva)") },
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
                            val target = FormatUtils.parseDouble(targetAmountStr) ?: -1.0
                            if (target <= 0) {
                                targetError = true
                                hasError = true
                            }
                            val initial = FormatUtils.parseDouble(initialAmountStr) ?: 0.0

                            if (!hasError) {
                                onConfirm(
                                    FinanceItem(
                                        title = title.trim(),
                                        amount = maxOf(0.0, initial),
                                        type = "BOX",
                                        category = "Caixinha",
                                        targetAmount = target,
                                        description = description.trim(),
                                        date = System.currentTimeMillis(),
                                        isCompleted = initial >= target
                                    )
                                )
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
    isDeposit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, Long) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

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
                    text = "${box.title} • Saldo Atual: ${FormatUtils.formatCurrency(box.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldAmber
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        amountError = false
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

                // Date Selection
                val context = androidx.compose.ui.platform.LocalContext.current
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selectedDate
                val datePickerDialog = android.app.DatePickerDialog(
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
                            val parsed = FormatUtils.parseDouble(amountStr) ?: -1.0
                            if (parsed <= 0) {
                                amountError = true
                            } else {
                                onConfirm(parsed, selectedDate)
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
