package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.RecurringBill
import com.example.ui.theme.*
import com.example.ui.utils.MoneyUtils

@Composable
fun RecurringBillsDialog(
    recurringBills: List<RecurringBill>,
    onDismiss: () -> Unit,
    onSaveBill: (RecurringBill) -> Unit,
    onDeleteBill: (RecurringBill) -> Unit
) {
    var billToEdit by remember { mutableStateOf<RecurringBill?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

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
                                    text = "Contas Recorrentes",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Geração mensal automática sem duplicatas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                billToEdit = null
                                showAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nova", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            ) { innerPadding ->
                if (recurringBills.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Repeat, contentDescription = null, modifier = Modifier.size(56.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Nenhuma conta recorrente cadastrada.", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Cadastre contas que se repetem todo mês (luz, internet, condomínio) com o dia de vencimento desejado.",
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
                        items(recurringBills, key = { it.id }) { bill ->
                            RecurringBillCard(
                                bill = bill,
                                onEdit = {
                                    billToEdit = bill
                                    showAddDialog = true
                                },
                                onToggleActive = { active ->
                                    onSaveBill(bill.copy(isActive = active))
                                },
                                onDelete = { onDeleteBill(bill) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditRecurringBillDialog(
            initialBill = billToEdit,
            onDismiss = {
                showAddDialog = false
                billToEdit = null
            },
            onConfirm = { savedBill ->
                onSaveBill(savedBill)
                showAddDialog = false
                billToEdit = null
            }
        )
    }
}

@Composable
fun RecurringBillCard(
    bill: RecurringBill,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bill.isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = bill.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (bill.isActive) TextPrimary else TextSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (bill.type == "APARTMENT") ApartmentTeal.copy(alpha = 0.15f) else OceanBlue.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = bill.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (bill.type == "APARTMENT") ApartmentTeal else OceanBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${MoneyUtils.formatCents(bill.amountCents)} • Todo dia ${bill.dueDay}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (bill.isActive) CoralRed else TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Switch(
                    checked = bill.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldGreen
                    )
                )

                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = CoralRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AddEditRecurringBillDialog(
    initialBill: RecurringBill?,
    onDismiss: () -> Unit,
    onConfirm: (RecurringBill) -> Unit
) {
    var title by remember { mutableStateOf(initialBill?.title ?: "") }
    var amountStr by remember { mutableStateOf(if (initialBill != null) MoneyUtils.formatCentsToInput(initialBill.amountCents) else "") }
    var category by remember { mutableStateOf(initialBill?.category ?: "Geral") }
    var dueDayStr by remember { mutableStateOf(initialBill?.dueDay?.toString() ?: "10") }
    var type by remember { mutableStateOf(initialBill?.type ?: "BILL") }
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
                    text = if (initialBill == null) "Cadastrar Recorrência" else "Editar Recorrência",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome da Conta (ex: Aluguel, Internet)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Valor Mensal (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDayStr,
                    onValueChange = { dueDayStr = it },
                    label = { Text("Dia de Vencimento (1 a 31)") },
                    supportingText = {
                        Text("Para dias 29-31, meses curtos (ex: Fevereiro) serão ajustados com segurança para o último dia do mês.")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "BILL",
                        onClick = { type = "BILL" },
                        label = { Text("Conta Geral") }
                    )
                    FilterChip(
                        selected = type == "APARTMENT",
                        onClick = { type = "APARTMENT" },
                        label = { Text("Moradia / Apt") }
                    )
                }

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
                            val cleanTitle = title.trim()
                            if (cleanTitle.isEmpty()) {
                                errorMsg = "O nome da conta é obrigatório."
                                return@Button
                            }
                            val cents = MoneyUtils.parseBrlToCents(amountStr)
                            if (cents == null || cents <= 0L) {
                                errorMsg = "Digite um valor válido maior que zero."
                                return@Button
                            }
                            val day = dueDayStr.toIntOrNull()
                            if (day == null || day !in 1..31) {
                                errorMsg = "O dia de vencimento deve estar entre 1 e 31."
                                return@Button
                            }

                            val bill = RecurringBill(
                                id = initialBill?.id ?: 0L,
                                title = cleanTitle,
                                category = category.trim().ifBlank { "Geral" },
                                amountCents = cents,
                                dueDay = day,
                                type = type,
                                isActive = initialBill?.isActive ?: true,
                                startDate = initialBill?.startDate ?: System.currentTimeMillis()
                            )
                            onConfirm(bill)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
