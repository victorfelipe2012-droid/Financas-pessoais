package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.ui.components.StoredTransactionList
import com.example.ui.theme.*
import com.example.ui.utils.FormatUtils
import java.util.Calendar
import androidx.compose.foundation.interaction.collectIsPressedAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    items: List<FinanceItem>,
    onAddItem: (FinanceItem) -> Unit,
    onDeleteItem: (FinanceItem) -> Unit,
    onProfileClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("TUDO") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filters = listOf(
        "TUDO" to "Tudo",
        "SALARY" to "Salário",
        "INVESTMENT" to "Investimentos",
        "BOX" to "Caixinhas",
        "LENT" to "Emprestado"
    )

    val filteredItems = if (selectedFilter == "TUDO") {
        items
    } else {
        items.filter { it.type == selectedFilter }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transações",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("add_transaction_btn"),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Adicionar", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Adicionar")
                        }

                        IconButton(
                            onClick = onProfileClick,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .testTag("transactions_profile_btn")
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

                // Filters Horizontal Scroll Row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filters) { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.Black,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = TextSecondary
                            ),
                            border = null,
                            shape = RoundedCornerShape(12.dp)
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
            StoredTransactionList(
                items = filteredItems,
                onDeleteItem = onDeleteItem,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                onAddItem(newItem)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TransactionCard(
    item: FinanceItem,
    onDelete: () -> Unit
) {
    val (icon, color, labelType) = when (item.type) {
        "SALARY" -> Triple(Icons.Rounded.AttachMoney, EmeraldGreen, "Salário")
        "INVESTMENT" -> Triple(Icons.Rounded.TrendingUp, OceanBlue, "Investimento")
        "BOX" -> Triple(Icons.Rounded.Savings, GoldAmber, "Caixinha")
        "LENT" -> Triple(Icons.Rounded.Handshake, LavenderPurple, "Emprestado")
        else -> Triple(Icons.Rounded.ReceiptLong, CoralRed, "Conta")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${item.id}"),
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
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = labelType,
                        tint = color,
                        modifier = Modifier.size(22.dp)
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
                        color = if (item.type == "SALARY" || (item.type == "LENT" && item.isCompleted)) EmeraldGreen else TextPrimary,
                        softWrap = false,
                        maxLines = 1
                    )

                    if (item.type == "BILL" || item.type == "LENT" || item.type == "BOX") {
                        val statusText = if (item.isCompleted) {
                            when (item.type) {
                                "BILL" -> "Pago"
                                "LENT" -> "Devolvido"
                                else -> "Meta Atingida"
                            }
                        } else {
                            "Pendente"
                        }
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
                            text = FormatUtils.formatDate(item.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (item.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (FinanceItem) -> Unit
) {
    var selectedType by remember { mutableStateOf("SALARY") }
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // Status validation
    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var targetAmountError by remember { mutableStateOf(false) }

    val types = listOf(
        "SALARY" to "Salário",
        "INVESTMENT" to "Investimento",
        "BOX" to "Caixinha",
        "LENT" to "Emprestado",
        "BILL" to "Conta"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .testTag("add_transaction_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Nova Transação",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                // Type selector row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tipo de Registro", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(types) { (key, label) ->
                                FilterChip(
                                    selected = selectedType == key,
                                    onClick = { 
                                        selectedType = key 
                                        category = "" // reset default category
                                    },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.Black,
                                        containerColor = MaterialTheme.colorScheme.background,
                                        labelColor = TextSecondary
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }

                // Title Input
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            titleError = false
                        },
                        label = {
                            val hint = when (selectedType) {
                                "SALARY" -> "Origem (Ex: Salário Mensal)"
                                "INVESTMENT" -> "Nome do Investimento (Ex: CDB Sofisa)"
                                "BOX" -> "Meta (Ex: Viagem de Férias)"
                                "LENT" -> "Quem pegou emprestado? (Ex: João Silva)"
                                else -> "Nome da Conta (Ex: Conta de Luz)"
                            }
                            Text(hint)
                        },
                        isError = titleError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("title_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }

                // Amount Input
                item {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            amountError = false
                        },
                        label = {
                            val hint = when (selectedType) {
                                "BOX" -> "Valor Inicial Guardado"
                                else -> "Valor (R$)"
                            }
                            Text(hint)
                        },
                        isError = amountError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_field"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }

                // If Box: Target Goal Amount
                if (selectedType == "BOX") {
                    item {
                        OutlinedTextField(
                            value = targetAmountStr,
                            onValueChange = {
                                targetAmountStr = it
                                targetAmountError = false
                            },
                            label = { Text("Meta Final da Caixinha (R$)") },
                            isError = targetAmountError,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_amount_field"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                    }
                }

                // Date Selection Field
                item {
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
                        label = { Text("Data do Lançamento") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = "Selecionar data",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("date_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }

                // Category Input
                if (selectedType == "INVESTMENT") {
                    item {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { 
                                Text("Categoria (Ex: Renda Fixa, Ações)")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                    }
                }

                // Description Input
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Observações (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }

                // Action buttons row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
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
                                val parsedAmount = FormatUtils.parseDouble(amountStr) ?: -1.0
                                if (parsedAmount < 0) {
                                    amountError = true
                                    hasError = true
                                }
                                val parsedTargetAmount = if (selectedType == "BOX") {
                                    val t = FormatUtils.parseDouble(targetAmountStr) ?: -1.0
                                    if (t <= 0) {
                                        targetAmountError = true
                                        hasError = true
                                    }
                                    t
                                } else 0.0

                                if (!hasError) {
                                    onConfirm(
                                        FinanceItem(
                                            title = title,
                                            amount = parsedAmount,
                                            type = selectedType,
                                            category = category.ifBlank { 
                                                when (selectedType) {
                                                    "SALARY" -> "Salário"
                                                    "BOX" -> "Caixinha"
                                                    "LENT" -> "Emprestado"
                                                    "BILL" -> "Conta"
                                                    else -> "Geral"
                                                }
                                            },
                                            description = description,
                                            targetAmount = parsedTargetAmount,
                                            date = selectedDate,
                                            isCompleted = selectedType == "SALARY" // Salary is auto-completed
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.testTag("dialog_confirm_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Salvar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
