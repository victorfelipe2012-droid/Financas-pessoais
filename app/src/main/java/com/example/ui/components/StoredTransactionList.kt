package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FinanceItem
import com.example.ui.theme.*
import com.example.ui.utils.FormatUtils

@Composable
fun StoredTransactionList(
    items: List<FinanceItem>,
    modifier: Modifier = Modifier,
    onDeleteItem: ((FinanceItem) -> Unit)? = null
) {
    if (items.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = "Sem transações",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nenhuma transação registrada",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Suas receitas e despesas salvas aparecerão aqui com detalhamento completo.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                StoredTransactionItem(
                    item = item,
                    onDelete = onDeleteItem
                )
            }
        }
    }
}

@Composable
fun StoredTransactionItem(
    item: FinanceItem,
    onDelete: ((FinanceItem) -> Unit)?,
    modifier: Modifier = Modifier
) {
    // Determine if the item is an income or an expense
    val isIncome = item.type == "SALARY" || (item.type == "LENT" && item.isCompleted)
    val amountColor = if (isIncome) EmeraldGreen else CoralRed
    val amountPrefix = if (isIncome) "+" else "-"

    val (icon, iconColor, labelType) = when (item.type) {
        "SALARY" -> Triple(Icons.Rounded.AttachMoney, EmeraldGreen, "Salário")
        "INVESTMENT" -> Triple(Icons.Rounded.TrendingUp, OceanBlue, "Investimento")
        "BOX" -> Triple(Icons.Rounded.Savings, GoldAmber, "Caixinha")
        "LENT" -> Triple(Icons.Rounded.Handshake, LavenderPurple, "Emprestado")
        else -> Triple(Icons.Rounded.ReceiptLong, CoralRed, "Conta")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stored_transaction_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Main Top Row: Icon, Info, and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circle badge with Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(iconColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = labelType,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Title and category
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
                        color = iconColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Amount and Status Badge
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.widthIn(min = 90.dp)
                ) {
                    Text(
                        text = "$amountPrefix${FormatUtils.formatCurrency(item.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = amountColor,
                        softWrap = false,
                        maxLines = 1
                    )

                    // Optional status indicator for Bills or Lent transactions
                    if (item.type == "BILL" || item.type == "LENT" || item.type == "BOX") {
                        val statusText = if (item.isCompleted) {
                            when (item.type) {
                                "BILL" -> "Pago"
                                "LENT" -> "Devolvido"
                                else -> "Concluído"
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

            // Divider line
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Date, Description, and Delete Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Date display with Calendar icon
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

                    // Description text if present
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

                // Delete button if a listener is supplied
                if (onDelete != null) {
                    IconButton(
                        onClick = { onDelete(item) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Excluir transação",
                            tint = TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
