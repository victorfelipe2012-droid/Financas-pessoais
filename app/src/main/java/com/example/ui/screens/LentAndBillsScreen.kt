package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.BorderStroke
import com.example.data.FinanceItem
import com.example.ui.theme.*
import com.example.ui.utils.FormatUtils
import java.util.Calendar
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.interaction.collectIsPressedAsState

@Composable
fun LentAndBillsScreen(
    items: List<FinanceItem>,
    onUpdateItem: (FinanceItem) -> Unit,
    onDeleteItem: (FinanceItem) -> Unit,
    onProfileClick: () -> Unit
) {
    var selectedLentForAbatement by remember { mutableStateOf<FinanceItem?>(null) }

    val lentItems = items.filter { it.type == "LENT" }
    val totalActiveLent = lentItems.filter { !it.isCompleted }.sumOf { it.amount }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Empréstimos Ativos",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Monitore seus valores emprestados e controle de pagamentos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (lentItems.isEmpty()) {
                EmptyListPlaceholder(
                    icon = Icons.Rounded.Handshake,
                    title = "Nenhum valor emprestado!",
                    subtitle = "Tudo em dia por aqui. Se emprestar dinheiro para alguém, registre aqui para não esquecer.",
                    color = LavenderPurple
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        KPISummaryCard(
                            title = "Total Ativo Emprestado",
                            amount = totalActiveLent,
                            color = LavenderPurple,
                            icon = Icons.Rounded.AttachMoney
                        )
                    }
                    
                    items(lentItems, key = { it.id }) { lent ->
                        LentItemCard(
                            lent = lent,
                            onToggleReturned = {
                                onUpdateItem(lent.copy(isCompleted = !lent.isCompleted))
                            },
                            onAbateClick = {
                                selectedLentForAbatement = lent
                            },
                            onDelete = { onDeleteItem(lent) }
                        )
                    }
                }
            }
        }
    }

    selectedLentForAbatement?.let { lent ->
        AbateLentDialog(
            lent = lent,
            onDismiss = { selectedLentForAbatement = null },
            onConfirm = { amount, selectedDate ->
                val newAmount = maxOf(0.0, lent.amount - amount)
                val logEntry = "• Abatido R$ ${FormatUtils.formatCurrency(amount).replace("R$", "").trim()} em ${FormatUtils.formatDate(selectedDate)}"
                val updatedLent = lent.copy(
                    amount = newAmount,
                    isCompleted = newAmount <= 0.0,
                    date = selectedDate,
                    description = if (lent.description.isBlank()) {
                        logEntry
                    } else {
                        "${lent.description}\n$logEntry"
                    }
                )
                onUpdateItem(updatedLent)
                selectedLentForAbatement = null
            }
        )
    }
}

@Composable
fun KPISummaryCard(
    title: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.15f)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    FormatUtils.formatCurrency(amount),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun BillItemCard(
    bill: FinanceItem,
    onTogglePaid: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bill_card_${bill.id}"),
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
                        .background(CoralRed.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ReceiptLong,
                        contentDescription = "Conta",
                        tint = CoralRed,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (bill.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        ),
                        color = if (bill.isCompleted) TextSecondary else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = bill.category.ifBlank { "Conta" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = CoralRed,
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
                        text = FormatUtils.formatCurrency(bill.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (bill.isCompleted) TextSecondary else CoralRed,
                        softWrap = false,
                        maxLines = 1
                    )

                    val statusText = if (bill.isCompleted) "Pago" else "Pendente"
                    val statusColor = if (bill.isCompleted) EmeraldGreen else GoldAmber
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
            Divider(color = BorderColor.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer row with Date, Description, and Delete button
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
                            contentDescription = "Vencimento",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Vencimento: ${FormatUtils.formatDate(bill.date)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (bill.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bill.description,
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

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTogglePaid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bill.isCompleted) GoldAmber else EmeraldGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = if (bill.isCompleted) Icons.Rounded.Undo else Icons.Rounded.Check,
                        contentDescription = if (bill.isCompleted) "Reabrir" else "Pagar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (bill.isCompleted) "Marcar como Pendente" else "Marcar como Pago",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun LentItemCard(
    lent: FinanceItem,
    onToggleReturned: () -> Unit,
    onAbateClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lent_card_${lent.id}"),
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
                        .background(LavenderPurple.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Handshake,
                        contentDescription = "Emprestado",
                        tint = LavenderPurple,
                        modifier = Modifier.size(22.dp)
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
                    Text(
                        text = "Empréstimo",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LavenderPurple,
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
                        text = FormatUtils.formatCurrency(lent.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (lent.isCompleted) EmeraldGreen else LavenderPurple,
                        softWrap = false,
                        maxLines = 1
                    )

                    val statusText = if (lent.isCompleted) "Devolvido" else "Pendente"
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

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BorderColor.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer row with Date, Description, and Delete button
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
                            text = FormatUtils.formatDate(lent.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (lent.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lent.description,
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

            // Action Buttons (Toggle state, Abate)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleReturned,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (lent.isCompleted) GoldAmber else EmeraldGreen
                    )
                ) {
                    Icon(
                        imageVector = if (lent.isCompleted) Icons.Rounded.Undo else Icons.Rounded.Check,
                        contentDescription = if (lent.isCompleted) "Reabrir" else "Devolvido",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (lent.isCompleted) "Reabrir" else "Marcar Devolvido",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (!lent.isCompleted) {
                    Button(
                        onClick = onAbateClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LavenderPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RemoveCircleOutline,
                            contentDescription = "Abater Parcela",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Abater Parcela",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AbateLentDialog(
    lent: FinanceItem,
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
                    text = "Abater Parcela",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Text(
                    text = "Registre um pagamento parcial para o empréstimo feito a ${lent.title}.",
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
                    label = { Text("Valor da Parcela (R$)") },
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

                if (amountError && errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = CoralRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Date Selection Field
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
                    label = { Text("Data do Abatimento") },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val parsedAmount = FormatUtils.parseDouble(amountStr) ?: -1.0
                            if (parsedAmount <= 0) {
                                amountError = true
                                errorMsg = "Por favor, digite um valor maior que zero."
                            } else if (parsedAmount > lent.amount) {
                                  amountError = true
                                  errorMsg = "O valor não pode ser maior que o saldo devedor atual (${FormatUtils.formatCurrency(lent.amount)})."
                            } else {
                                onConfirm(parsedAmount, selectedDate)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("abate_confirm_btn")
                    ) {
                        Text("Abater", fontWeight = FontWeight.Bold)
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
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
