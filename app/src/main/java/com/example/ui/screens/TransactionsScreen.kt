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
    apartmentSubcategories: List<String> = emptyList(),
    onAddApartmentSubcategory: (String) -> Unit = {},
    onUpdateApartmentSubcategory: (String, String) -> Unit = { _, _ -> },
    onDeleteApartmentSubcategory: (String) -> Unit = {},
    onResetApartmentSubcategories: () -> Unit = {},
    initialFilter: String = "TUDO",
    onAddItem: (FinanceItem) -> Unit,
    onUpdateItem: (FinanceItem) -> Unit = {},
    onDeleteItem: (FinanceItem) -> Unit,
    onProfileClick: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var selectedFilter by remember(initialFilter) { mutableStateOf(initialFilter) }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<FinanceItem?>(null) }

    val filters = listOf(
        "TUDO" to "Tudo",
        "APARTMENT" to "Apartamento",
        "SALARY" to "Salário",
        "INVESTMENT" to "Investimentos",
        "BOX" to "Caixinhas",
        "LENT" to "Emprestado",
        "BILL" to "Contas"
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        Text(
                            text = if (selectedFilter == "APARTMENT") "Moradia" else if (selectedFilter == "SALARY") "Salário" else "Transações",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("add_transaction_btn"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
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

                // Filter horizontal list
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filters) { (key, label) ->
                        val isSelected = selectedFilter == key
                        val activeColor = when (key) {
                            "APARTMENT" -> ApartmentTeal
                            "SALARY" -> EmeraldGreen
                            "INVESTMENT" -> OceanBlue
                            "BOX" -> GoldAmber
                            "LENT" -> LavenderPurple
                            "BILL" -> CoralRed
                            else -> MaterialTheme.colorScheme.primary
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = activeColor,
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
                onEditItem = { itemToEdit = it },
                onDeleteItem = onDeleteItem,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            apartmentSubcategories = apartmentSubcategories,
            onAddApartmentSubcategory = onAddApartmentSubcategory,
            onUpdateApartmentSubcategory = onUpdateApartmentSubcategory,
            onDeleteApartmentSubcategory = onDeleteApartmentSubcategory,
            onResetApartmentSubcategories = onResetApartmentSubcategories,
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                onAddItem(newItem)
                showAddDialog = false
            }
        )
    }

    itemToEdit?.let { currentItem ->
        EditTransactionDialog(
            item = currentItem,
            apartmentSubcategories = apartmentSubcategories,
            onAddApartmentSubcategory = onAddApartmentSubcategory,
            onUpdateApartmentSubcategory = onUpdateApartmentSubcategory,
            onDeleteApartmentSubcategory = onDeleteApartmentSubcategory,
            onResetApartmentSubcategories = onResetApartmentSubcategories,
            onDismiss = { itemToEdit = null },
            onConfirm = { updatedItem ->
                onUpdateItem(updatedItem)
                itemToEdit = null
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
        "APARTMENT" -> Triple(Icons.Rounded.Apartment, ApartmentTeal, "Apartamento")
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

                    if (item.type == "BILL" || item.type == "LENT" || item.type == "BOX" || item.type == "APARTMENT") {
                        val statusText = if (item.isCompleted) {
                            when (item.type) {
                                "BILL", "APARTMENT" -> "Pago"
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
    apartmentSubcategories: List<String> = emptyList(),
    onAddApartmentSubcategory: (String) -> Unit = {},
    onUpdateApartmentSubcategory: (String, String) -> Unit = { _, _ -> },
    onDeleteApartmentSubcategory: (String) -> Unit = {},
    onResetApartmentSubcategories: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (FinanceItem) -> Unit
) {
    var selectedType by remember { mutableStateOf("APARTMENT") }
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (apartmentSubcategories.isNotEmpty()) apartmentSubcategories.first() else "") }
    var description by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isCompleted by remember { mutableStateOf(true) } // true for Paid, false for Pending (Bills/Apartment)
    
    // Status validation
    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var targetAmountError by remember { mutableStateOf(false) }

    // Dialog state for subcategory management
    var showManageSubcategoriesDialog by remember { mutableStateOf(false) }
    var showNewSubcategoryDialog by remember { mutableStateOf(false) }
    var newSubcategoryInput by remember { mutableStateOf("") }

    val types = listOf(
        "APARTMENT" to "Apartamento",
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
                                val activeColor = when (key) {
                                    "APARTMENT" -> ApartmentTeal
                                    "SALARY" -> EmeraldGreen
                                    "INVESTMENT" -> OceanBlue
                                    "BOX" -> GoldAmber
                                    "LENT" -> LavenderPurple
                                    "BILL" -> CoralRed
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                FilterChip(
                                    selected = selectedType == key,
                                    onClick = { 
                                        selectedType = key 
                                        category = if (key == "APARTMENT" && apartmentSubcategories.isNotEmpty()) {
                                            apartmentSubcategories.first()
                                        } else {
                                            ""
                                        }
                                    },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = activeColor,
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

                // Apartment Subcategory Dynamic Selection
                if (selectedType == "APARTMENT") {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Subcategoria do Apartamento",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                TextButton(
                                    onClick = { showManageSubcategoriesDialog = true },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = "Gerenciar",
                                        modifier = Modifier.size(16.dp),
                                        tint = ApartmentTeal
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Editar / Gerenciar", style = MaterialTheme.typography.labelSmall, color = ApartmentTeal)
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(apartmentSubcategories) { sub ->
                                    val isSelected = category == sub
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { 
                                            category = sub
                                            if (title.isBlank()) {
                                                title = sub
                                            }
                                        },
                                        label = { Text(sub) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ApartmentTeal,
                                            selectedLabelColor = Color.Black,
                                            containerColor = MaterialTheme.colorScheme.background,
                                            labelColor = TextSecondary
                                        ),
                                        border = null
                                    )
                                }

                                item {
                                    FilledTonalButton(
                                        onClick = { showNewSubcategoryDialog = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = ApartmentTeal.copy(alpha = 0.15f),
                                            contentColor = ApartmentTeal
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Nova", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Nova", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
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
                                "APARTMENT" -> "Identificação (Ex: Condomínio Março, Conta de Luz)"
                                "SALARY" -> "Origem (Ex: Salário Mensal, Freelance)"
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

                // Status Pago / Pendente for APARTMENT and BILL
                if (selectedType == "APARTMENT" || selectedType == "BILL") {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Status do Pagamento", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text(
                                    if (isCompleted) "Já foi pago" else "Ainda pendente / A pagar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCompleted) EmeraldGreen else GoldAmber
                                )
                            }
                            Switch(
                                checked = isCompleted,
                                onCheckedChange = { isCompleted = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldGreen,
                                    checkedTrackColor = EmeraldGreen.copy(alpha = 0.3f),
                                    uncheckedThumbColor = GoldAmber,
                                    uncheckedTrackColor = GoldAmber.copy(alpha = 0.3f)
                                )
                            )
                        }
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
                        label = { Text(if (selectedType == "APARTMENT" || selectedType == "BILL") "Data de Pagamento / Vencimento" else "Data do Lançamento") },
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

                // Custom Category Input for Investment or free edit
                if (selectedType == "INVESTMENT") {
                    item {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { 
                                Text("Categoria (Ex: Renda Fixa, Ações, FIIs)")
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
                                    val finalCompleted = when (selectedType) {
                                        "SALARY" -> true
                                        "APARTMENT", "BILL" -> isCompleted
                                        else -> false
                                    }

                                    onConfirm(
                                        FinanceItem(
                                            title = title,
                                            amount = parsedAmount,
                                            type = selectedType,
                                            category = category.ifBlank { 
                                                when (selectedType) {
                                                    "APARTMENT" -> "Apartamento"
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
                                            isCompleted = finalCompleted
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.testTag("dialog_confirm_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Salvar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal to add a new subcategory directly
    if (showNewSubcategoryDialog) {
        Dialog(onDismissRequest = { showNewSubcategoryDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Nova Subcategoria de Apartamento",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    OutlinedTextField(
                        value = newSubcategoryInput,
                        onValueChange = { newSubcategoryInput = it },
                        label = { Text("Nome da Subcategoria (Ex: Faxina, Garagem)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { 
                            showNewSubcategoryDialog = false
                            newSubcategoryInput = ""
                        }) {
                            Text("Cancelar", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newSubcategoryInput.isNotBlank()) {
                                    val trimmed = newSubcategoryInput.trim()
                                    onAddApartmentSubcategory(trimmed)
                                    category = trimmed
                                    if (title.isBlank()) {
                                        title = trimmed
                                    }
                                    showNewSubcategoryDialog = false
                                    newSubcategoryInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ApartmentTeal, contentColor = Color.Black)
                        ) {
                            Text("Adicionar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal to manage/edit/delete all subcategories
    if (showManageSubcategoriesDialog) {
        ManageSubcategoriesDialog(
            subcategories = apartmentSubcategories,
            onAdd = onAddApartmentSubcategory,
            onUpdate = onUpdateApartmentSubcategory,
            onDelete = onDeleteApartmentSubcategory,
            onReset = onResetApartmentSubcategories,
            onDismiss = { showManageSubcategoriesDialog = false }
        )
    }
}

@Composable
fun ManageSubcategoriesDialog(
    subcategories: List<String>,
    onAdd: (String) -> Unit,
    onUpdate: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var editingSubcategory by remember { mutableStateOf<String?>(null) }
    var editInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.8f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Gerenciar Subcategorias",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            "Adicione, renomeie ou exclua subcategorias",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input to add new subcategory
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("Nova subcategoria...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ApartmentTeal,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onAdd(newName.trim())
                                newName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ApartmentTeal, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Adicionar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = BorderColor.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                // List of existing subcategories with edit & delete buttons
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subcategories) { sub ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                        ) {
                            if (editingSubcategory == sub) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editInput,
                                        onValueChange = { editInput = it },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        if (editInput.isNotBlank()) {
                                            onUpdate(sub, editInput.trim())
                                        }
                                        editingSubcategory = null
                                    }) {
                                        Icon(Icons.Rounded.Check, contentDescription = "Salvar", tint = EmeraldGreen)
                                    }
                                    IconButton(onClick = { editingSubcategory = null }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Cancelar", tint = TextSecondary)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingSubcategory = sub
                                                editInput = sub
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Edit,
                                                contentDescription = "Editar",
                                                tint = OceanBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDelete(sub) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                contentDescription = "Excluir",
                                                tint = CoralRed.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom actions: Reset to defaults & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onReset) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = "Restaurar", modifier = Modifier.size(16.dp), tint = TextSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restaurar Padrões", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Pronto", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    item: FinanceItem,
    apartmentSubcategories: List<String> = emptyList(),
    onAddApartmentSubcategory: (String) -> Unit = {},
    onUpdateApartmentSubcategory: (String, String) -> Unit = { _, _ -> },
    onDeleteApartmentSubcategory: (String) -> Unit = {},
    onResetApartmentSubcategories: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (FinanceItem) -> Unit
) {
    var selectedType by remember { mutableStateOf(item.type) }
    var title by remember { mutableStateOf(item.title) }
    var amountStr by remember { mutableStateOf(FormatUtils.formatCurrency(item.amount).replace("R$", "").trim()) }
    var category by remember { mutableStateOf(item.category) }
    var description by remember { mutableStateOf(item.description) }
    var targetAmountStr by remember { mutableStateOf(if (item.targetAmount > 0) FormatUtils.formatCurrency(item.targetAmount).replace("R$", "").trim() else "") }
    var selectedDate by remember { mutableLongStateOf(item.date) }
    var isCompleted by remember { mutableStateOf(item.isCompleted) }

    // Status validation
    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var targetAmountError by remember { mutableStateOf(false) }

    // Dialog state for subcategory management
    var showManageSubcategoriesDialog by remember { mutableStateOf(false) }
    var showNewSubcategoryDialog by remember { mutableStateOf(false) }
    var newSubcategoryInput by remember { mutableStateOf("") }

    val types = listOf(
        "APARTMENT" to "Apartamento",
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
                .testTag("edit_transaction_dialog"),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Editar Lançamento",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = TextSecondary)
                        }
                    }
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
                                val activeColor = when (key) {
                                    "APARTMENT" -> ApartmentTeal
                                    "SALARY" -> EmeraldGreen
                                    "INVESTMENT" -> OceanBlue
                                    "BOX" -> GoldAmber
                                    "LENT" -> LavenderPurple
                                    "BILL" -> CoralRed
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                FilterChip(
                                    selected = selectedType == key,
                                    onClick = { 
                                        selectedType = key 
                                        if (key == "APARTMENT" && category.isBlank() && apartmentSubcategories.isNotEmpty()) {
                                            category = apartmentSubcategories.first()
                                        }
                                    },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = activeColor,
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

                // Subcategories for Apartment
                if (selectedType == "APARTMENT") {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Subcategoria do Apartamento", style = MaterialTheme.typography.labelMedium, color = ApartmentTeal)
                                TextButton(
                                    onClick = { showManageSubcategoriesDialog = true },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = ApartmentTeal)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Gerenciar", style = MaterialTheme.typography.labelSmall, color = ApartmentTeal)
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(apartmentSubcategories) { sub ->
                                    FilterChip(
                                        selected = category == sub,
                                        onClick = { category = sub },
                                        label = { Text(sub) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ApartmentTeal,
                                            selectedLabelColor = Color.Black,
                                            containerColor = MaterialTheme.colorScheme.background,
                                            labelColor = TextSecondary
                                        ),
                                        border = null
                                    )
                                }

                                item {
                                    FilledTonalButton(
                                        onClick = { showNewSubcategoryDialog = true },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = ApartmentTeal.copy(alpha = 0.15f),
                                            contentColor = ApartmentTeal
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("+ Nova", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
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
                        label = { Text("Título do Lançamento") },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text("Por favor, preencha o título.", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                        label = { Text("Valor (R$)") },
                        isError = amountError,
                        supportingText = if (amountError) {
                            { Text("Por favor, insira um valor válido maior que zero.", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Target Amount for Boxes
                if (selectedType == "BOX") {
                    item {
                        OutlinedTextField(
                            value = targetAmountStr,
                            onValueChange = { 
                                targetAmountStr = it
                                targetAmountError = false
                            },
                            label = { Text("Meta Final em R$ (Ex: 5000)") },
                            isError = targetAmountError,
                            supportingText = if (targetAmountError) {
                                { Text("Por favor, insira uma meta válida.", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Date Picker
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Status Switch for Bills / Apartment / Loans
                if (selectedType == "BILL" || selectedType == "APARTMENT" || selectedType == "LENT") {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (selectedType == "LENT") "Status do Empréstimo" else "Status de Pagamento",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isCompleted) {
                                        if (selectedType == "LENT") "Devolvido / Recebido" else "Pago / Quitado"
                                    } else {
                                        "Pendente"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCompleted) EmeraldGreen else GoldAmber
                                )
                            }

                            Switch(
                                checked = isCompleted,
                                onCheckedChange = { isCompleted = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldGreen,
                                    checkedTrackColor = EmeraldGreen.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }

                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Observações (Opcional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Action Buttons
                item {
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
                                val amount = FormatUtils.parseDouble(amountStr) ?: -1.0
                                if (amount <= 0) {
                                    amountError = true
                                    hasError = true
                                }

                                var targetAmount = 0.0
                                if (selectedType == "BOX") {
                                    val parsedTarget = FormatUtils.parseDouble(targetAmountStr) ?: -1.0
                                    if (parsedTarget <= 0) {
                                        targetAmountError = true
                                        hasError = true
                                    } else {
                                        targetAmount = parsedTarget
                                    }
                                }

                                if (!hasError) {
                                    val finalCategory = if (selectedType == "APARTMENT") {
                                        category.ifBlank { "Geral" }
                                    } else {
                                        category.ifBlank { "Geral" }
                                    }

                                    val updated = item.copy(
                                        title = title.trim(),
                                        amount = amount,
                                        type = selectedType,
                                        category = finalCategory,
                                        date = selectedDate,
                                        description = description.trim(),
                                        isCompleted = if (selectedType == "SALARY") true else isCompleted,
                                        targetAmount = targetAmount
                                    )
                                    onConfirm(updated)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Salvar Alterações", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showManageSubcategoriesDialog) {
        ManageSubcategoriesDialog(
            subcategories = apartmentSubcategories,
            onAdd = onAddApartmentSubcategory,
            onUpdate = onUpdateApartmentSubcategory,
            onDelete = onDeleteApartmentSubcategory,
            onReset = onResetApartmentSubcategories,
            onDismiss = { showManageSubcategoriesDialog = false }
        )
    }

    if (showNewSubcategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewSubcategoryDialog = false },
            title = { Text("Nova Subcategoria", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSubcategoryInput,
                    onValueChange = { newSubcategoryInput = it },
                    label = { Text("Nome da Subcategoria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubcategoryInput.isNotBlank()) {
                            onAddApartmentSubcategory(newSubcategoryInput.trim())
                            category = newSubcategoryInput.trim()
                            newSubcategoryInput = ""
                        }
                        showNewSubcategoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ApartmentTeal, contentColor = Color.Black)
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSubcategoryDialog = false }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
