package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FinanceItem
import com.example.ui.theme.*
import com.example.ui.utils.FormatUtils

@Composable
fun ChallengeSection(
    items: List<FinanceItem>,
    onUpdateItem: (FinanceItem) -> Unit,
    onStartChallenge: (Double) -> Unit,
    onResetChallenge: () -> Unit,
    onArchiveChallenge: () -> Unit
) {
    val challengeItems = items.filter { it.type == "CHALLENGE" }.sortedBy {
        it.title.substringAfter("Semana ").toIntOrNull() ?: 0
    }

    val archivedItems = items.filter { it.type == "CHALLENGE_ARCHIVED" }
    val archivedGroups = archivedItems.groupBy { item ->
        val parts = item.category.split("_")
        if (parts.size >= 2 && parts[0] == "ARCHIVED") {
            parts[1].toLongOrNull() ?: 0L
        } else {
            0L
        }
    }.filter { it.key > 0L }.toList().sortedByDescending { it.first }

    var customMultiplierInput by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }

    if (challengeItems.isEmpty()) {
        // --- SETUP VIEW ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("challenge_setup_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(EmeraldGreen.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Desafio das 52 Semanas",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Comece poupando uma pequena quantia na semana 1 e aumente progressivamente a cada semana. Ao final de um ano, você acumulará um patrimônio expressivo sem sofrimento!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Escolha o valor base do depósito:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        val presets = listOf(
                            PresetOption(1.0, "Acumula R$ 1.378,00", "Excelente para começar"),
                            PresetOption(2.0, "Acumula R$ 2.756,00", "Ótimo equilíbrio"),
                            PresetOption(5.0, "Acumula R$ 6.890,00", "Recomendado ★", true),
                            PresetOption(10.0, "Acumula R$ 13.780,00", "Para poupadores focados")
                        )

                        presets.forEach { preset ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onStartChallenge(preset.multiplier) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (preset.isRecommended) EmeraldGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                ),
                                border = if (preset.isRecommended) {
                                    BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                                } else {
                                    BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f))
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "R$ ${FormatUtils.formatCurrency(preset.multiplier).replace("R$", "").trim()} / semana",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (preset.isRecommended) EmeraldGreen else TextPrimary
                                        )
                                        Text(
                                            text = preset.desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = preset.target,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Custom Option
                        Divider(color = BorderColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = "Ou defina um valor personalizado por semana:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customMultiplierInput,
                                onValueChange = { customMultiplierInput = it.filter { char -> char.isDigit() || char == '.' || char == ',' } },
                                label = { Text("Valor base (ex: 3,00)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = BorderColor
                                )
                            )

                            Button(
                                onClick = {
                                    val multiplier = FormatUtils.parseDouble(customMultiplierInput)
                                    if (multiplier != null && multiplier > 0.0) {
                                        onStartChallenge(multiplier)
                                    }
                                },
                                enabled = customMultiplierInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Iniciar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (archivedGroups.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Histórico de Desafios Arquivados",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }

                items(archivedGroups) { (timestamp, groupItems) ->
                    val totalSavedArchived = groupItems.filter { it.isCompleted }.sumOf { it.amount }
                    val totalWeeksArchived = groupItems.size
                    val completedWeeksArchived = groupItems.count { it.isCompleted }
                    
                    val multiplierPart = groupItems.firstOrNull()?.category?.substringAfter("MULTIPLIER_")?.toDoubleOrNull() ?: 1.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Desafio - Base R$ ${FormatUtils.formatCurrency(multiplierPart).replace("R$", "").trim()}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Arquivado em ${FormatUtils.formatDate(timestamp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "$completedWeeksArchived de $totalWeeksArchived semanas completadas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (completedWeeksArchived == totalWeeksArchived) EmeraldGreen else TextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Acumulado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(totalSavedArchived),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- ACTIVE CHALLENGE VIEW ---
        val completedWeeks = challengeItems.count { it.isCompleted }
        val totalWeeks = challengeItems.size
        val progressPercentage = if (totalWeeks > 0) completedWeeks.toFloat() / totalWeeks else 0f
        
        val totalSaved = challengeItems.filter { it.isCompleted }.sumOf { it.amount }
        val totalTarget = challengeItems.sumOf { it.amount }
        val nextPendingWeek = challengeItems.firstOrNull { !it.isCompleted }

        // Find multiplier based on Week 1 amount or database category
        val firstItem = challengeItems.firstOrNull()
        val multiplier = firstItem?.amount ?: 5.0

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("challenge_active_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (completedWeeks == totalWeeks && totalWeeks > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(EmeraldGreen.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Text(
                                text = "🏆 Desafio Concluído!",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldGreen,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = "Parabéns! Você persistiu e completou todas as 52 semanas do desafio de poupança! Você acumulou um total de ${FormatUtils.formatCurrency(totalSaved)}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Button(
                                onClick = { onArchiveChallenge() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("archive_challenge_btn")
                            ) {
                                Icon(imageVector = Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Arquivar e Começar Novo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Hero Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Progresso do Desafio",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "$completedWeeks de $totalWeeks semanas concluídas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(EmeraldGreen.copy(alpha = 0.12f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${(progressPercentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldGreen
                                )
                            }
                        }

                        // Progress bar
                        LinearProgressIndicator(
                            progress = progressPercentage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = EmeraldGreen,
                            trackColor = BorderColor.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Valor Guardado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(totalSaved),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Meta Final",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(totalTarget),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                        }

                        // Quick Deposit section
                        if (nextPendingWeek != null) {
                            Divider(color = BorderColor.copy(alpha = 0.3f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Próximo Depósito",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${nextPendingWeek.title}: ${FormatUtils.formatCurrency(nextPendingWeek.amount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                }

                                Button(
                                    onClick = {
                                        onUpdateItem(nextPendingWeek.copy(isCompleted = true))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Depositar", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }

            // Section Header & Reset button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cronograma de Depósitos",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    IconButton(
                        onClick = { showResetConfirm = true },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Rounded.RestartAlt, contentDescription = "Reiniciar Desafio")
                    }
                }
            }

            // Spreadsheet headers
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Semana / Depósito",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "Total Acumul.",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                        modifier = Modifier.weight(0.9f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.End
                    )
                }
            }

            // Split and chunk list by 2 so they can be viewed twin-column styled
            val chunkedWeeks = challengeItems.chunked(2)
            items(chunkedWeeks) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { weekItem ->
                        val weekNum = weekItem.title.substringAfter("Semana ").toIntOrNull() ?: 1
                        val accumulatedForWeek = (weekNum * (weekNum + 1) / 2) * multiplier

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onUpdateItem(weekItem.copy(isCompleted = !weekItem.isCompleted))
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (weekItem.isCompleted) {
                                    EmeraldGreen.copy(alpha = 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (weekItem.isCompleted) EmeraldGreen.copy(alpha = 0.4f) else BorderColor.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Semana $weekNum",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    if (weekItem.isCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .background(EmeraldGreen, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "OK",
                                                tint = Color.Black,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Depósito: ${FormatUtils.formatCurrency(weekItem.amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (weekItem.isCompleted) EmeraldGreen else TextSecondary
                                )

                                Text(
                                    text = "Acumul.: ${FormatUtils.formatCurrency(accumulatedForWeek)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    if (pair.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (archivedGroups.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Histórico de Desafios Arquivados",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }

                items(archivedGroups) { (timestamp, groupItems) ->
                    val totalSavedArchived = groupItems.filter { it.isCompleted }.sumOf { it.amount }
                    val totalWeeksArchived = groupItems.size
                    val completedWeeksArchived = groupItems.count { it.isCompleted }
                    
                    val multiplierPart = groupItems.firstOrNull()?.category?.substringAfter("MULTIPLIER_")?.toDoubleOrNull() ?: 1.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Desafio - Base R$ ${FormatUtils.formatCurrency(multiplierPart).replace("R$", "").trim()}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Arquivado em ${FormatUtils.formatDate(timestamp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "$completedWeeksArchived de $totalWeeksArchived semanas completadas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (completedWeeksArchived == totalWeeksArchived) EmeraldGreen else TextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Acumulado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(totalSavedArchived),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Reiniciar Desafio?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Isso limpará todo o progresso atual do Desafio de 52 Semanas. Deseja recomeçar?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetChallenge()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sim, Reiniciar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

data class PresetOption(
    val multiplier: Double,
    val target: String,
    val desc: String,
    val isRecommended: Boolean = false
)
