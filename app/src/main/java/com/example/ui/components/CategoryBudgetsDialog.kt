package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CategoryBudget
import com.example.data.FinanceItem
import com.example.ui.theme.*
import com.example.ui.utils.CategoryBudgetCalculator
import com.example.ui.utils.CategoryBudgetStatus
import com.example.ui.utils.MoneyUtils

@Composable
fun CategoryBudgetsDialog(
    budgets: List<CategoryBudget>,
    items: List<FinanceItem>,
    selectedYear: Int,
    selectedMonth: Int,
    onDismiss: () -> Unit,
    onSaveBudget: (category: String, limitCents: Long) -> Unit,
    onDeleteBudget: (CategoryBudget) -> Unit
) {
    var showSetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<String?>(null) }
    var initialLimitForEdit by remember { mutableStateOf<Long?>(null) }

    // Obter todas as categorias existentes nos itens para facilitar o cadastro
    val existingCategories = remember(items, budgets) {
        val fromItems = items.map { it.category.trim() }.filter { it.isNotBlank() }
        val fromBudgets = budgets.map { it.category.trim() }
        (fromItems + fromBudgets + listOf("Alimentação", "Transporte", "Saúde", "Lazer", "Moradia", "Educação")).distinct().sorted()
    }

    val budgetStatuses = remember(budgets, items, selectedYear, selectedMonth) {
        budgets.map { budget ->
            CategoryBudgetCalculator.calculateCategoryStatus(budget, items, selectedYear, selectedMonth)
        }
    }

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
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
                                    text = "Orçamentos por Categoria",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Competência: $selectedMonth/$selectedYear",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldGreen
                                )
                            }
                        }

                        Button(
                            onClick = {
                                selectedCategoryForEdit = null
                                initialLimitForEdit = null
                                showSetDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Definir", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            ) { innerPadding ->
                if (budgets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.PieChart, contentDescription = null, modifier = Modifier.size(56.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Nenhum teto de gasto configurado.", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Defina metas de gastos mensais por categoria. Aportes em investimentos e caixinhas são rigorosamente excluídos para não distorcer suas despesas de consumo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(budgetStatuses, key = { it.category }) { status ->
                            val rawBudget = budgets.firstOrNull { it.category.equals(status.category, ignoreCase = true) }
                            CategoryBudgetCard(
                                status = status,
                                onEdit = {
                                    selectedCategoryForEdit = status.category
                                    initialLimitForEdit = status.limitCents
                                    showSetDialog = true
                                },
                                onDelete = {
                                    rawBudget?.let { onDeleteBudget(it) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSetDialog) {
        SetCategoryBudgetDialog(
            initialCategory = selectedCategoryForEdit,
            initialLimitCents = initialLimitForEdit,
            availableCategories = existingCategories,
            onDismiss = { showSetDialog = false },
            onConfirm = { cat, limit ->
                onSaveBudget(cat, limit)
                showSetDialog = false
            }
        )
    }
}

@Composable
fun CategoryBudgetCard(
    status: CategoryBudgetStatus,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progressFloat = (status.percentageUsed / 100f).coerceIn(0f, 1f)
    val statusColor = when {
        status.percentageUsed > 100 -> CoralRed
        status.percentageUsed >= 80 -> GoldAmber
        else -> EmeraldGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = status.category,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${status.percentageUsed}% do limite",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = CoralRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progressFloat },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.background
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Gasto Realizado", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        MoneyUtils.formatCents(status.spentCents),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (status.percentageUsed > 100) CoralRed else TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Limite Mensal", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        MoneyUtils.formatCents(status.limitCents),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (status.remainingCents >= 0L) "Disponível" else "Excedido",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        MoneyUtils.formatCents(kotlin.math.abs(status.remainingCents)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (status.remainingCents >= 0L) EmeraldGreen else CoralRed
                    )
                }
            }
        }
    }
}

@Composable
fun SetCategoryBudgetDialog(
    initialCategory: String?,
    initialLimitCents: Long?,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (category: String, limitCents: Long) -> Unit
) {
    var category by remember { mutableStateOf(initialCategory ?: availableCategories.firstOrNull() ?: "") }
    var limitStr by remember { mutableStateOf(if (initialLimitCents != null) MoneyUtils.formatCentsToInput(initialLimitCents) else "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (initialCategory == null) "Definir Limite de Gasto" else "Editar Limite: $initialCategory",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                if (initialCategory == null) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Nome da Categoria") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Limite Mensal Máximo (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let {
                    Text(it, color = CoralRed, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleanCat = category.trim()
                            if (cleanCat.isEmpty()) {
                                errorMsg = "Digite ou selecione a categoria."
                                return@Button
                            }
                            val cents = MoneyUtils.parseBrlToCents(limitStr)
                            if (cents == null || cents <= 0L) {
                                errorMsg = "Digite um valor limite válido maior que zero."
                                return@Button
                            }
                            onConfirm(cleanCat, cents)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Salvar Limite", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
