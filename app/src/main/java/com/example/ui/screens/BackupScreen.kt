package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.os.Build
import com.example.data.FinanceItem
import com.example.reminder.BillReminderWorker
import com.example.ui.theme.*
import com.example.ui.utils.CsvExporter
import com.example.ui.utils.FormatUtils
import com.example.ui.utils.MonthlyFinanceCalculator
import java.io.File

@Composable
fun BackupScreen(
    items: List<FinanceItem> = emptyList(),
    autoBackupTime: Long?,
    onRestoreAutoBackup: (onSuccess: () -> Unit, onError: () -> Unit) -> Unit,
    onManualExport: (password: String, file: File, onSuccess: () -> Unit, onError: () -> Unit) -> Unit,
    onManualRestore: (password: String, file: File, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onWipeAllData: () -> Unit
) {
    val context = LocalContext.current
    
    var password by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var selectedFileToRestore by remember { mutableStateOf<File?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }

    // Launcher to select a file from local storage using System SAF
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    val tempFile = File(context.cacheDir, "temp_restore.bin")
                    tempFile.writeText(text)
                    selectedFileToRestore = tempFile
                    showRestoreDialog = true
                } else {
                    Toast.makeText(context, "Arquivo de backup vazio ou inválido.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao abrir o arquivo.", Toast.LENGTH_LONG).show()
            }
        }
    }

    var remindersEnabled by remember { mutableStateOf(BillReminderWorker.isRemindersEnabled(context)) }
    var csvFilterMonthOnly by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            BillReminderWorker.setRemindersEnabled(context, true)
            remindersEnabled = true
            Toast.makeText(context, "Lembretes diários ativados com sucesso!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão de notificação necessária para lembretes.", Toast.LENGTH_LONG).show()
        }
    }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                val itemsToExport = if (csvFilterMonthOnly) {
                    val cal = java.util.Calendar.getInstance()
                    MonthlyFinanceCalculator.filterItemsByMonth(items, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
                } else items

                val csvContent = CsvExporter.generateCsv(itemsToExport)
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Planilha CSV salva com sucesso no dispositivo!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao salvar arquivo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("backup_scroll_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Backup & Privacidade",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Garantia de privacidade total. Suas finanças pertencem exclusivamente a você.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Privacy Warning Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreen.copy(alpha = 0.15f)))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = "Privacidade",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Soberania dos seus dados",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            "Este aplicativo não possui servidores, não envia dados para a internet e funciona totalmente offline. Toda a criptografia ocorre localmente no seu dispositivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Section: Automatic Backup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EmeraldGreen.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Autorenew,
                                contentDescription = "Automático",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                "Backup Automático Local",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                "Proteção criptografada AES-GCM ativa",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Divider(color = BorderColor.copy(alpha = 0.4f), thickness = 1.dp)

                    val statusMsg = if (autoBackupTime != null) {
                        "Ativo e atualizado em: ${FormatUtils.formatDate(autoBackupTime)} às ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(autoBackupTime))}"
                    } else {
                        "Aguardando a primeira alteração para salvar."
                    }

                    Text(
                        text = statusMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    Button(
                        onClick = {
                            onRestoreAutoBackup(
                                { Toast.makeText(context, "Backup automático restaurado com sucesso!", Toast.LENGTH_LONG).show() },
                                { Toast.makeText(context, "Falha ao restaurar backup automático.", Toast.LENGTH_LONG).show() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restaurar Último Auto-Backup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Manual Backup & Transfer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(OceanBlue.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DriveFolderUpload,
                                contentDescription = "Manual",
                                tint = OceanBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                "Backup Manual & Exportação",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                "Transfira seus dados com segurança",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Divider(color = BorderColor.copy(alpha = 0.4f), thickness = 1.dp)

                    Text(
                        "Deseja fazer uma cópia dos seus dados ou mudar de celular? Defina uma senha para criptografar seus dados e exportar o arquivo de backup para onde quiser.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha para Criptografar o Backup") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OceanBlue,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Export button
                        Button(
                            onClick = {
                                if (password.length < 4) {
                                    Toast.makeText(context, "A senha deve conter no mínimo 4 caracteres.", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                val backupFile = File(context.cacheDir, "financasprivadas_backup.bin")
                                onManualExport(password, backupFile, {
                                    // Trigger Share intent
                                    try {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "com.example.fileprovider",
                                            backupFile
                                        )
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/octet-stream"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar Backup Criptografado"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Falha ao compartilhar o arquivo.", Toast.LENGTH_LONG).show()
                                    }
                                }, {
                                    Toast.makeText(context, "Erro ao criar backup criptografado.", Toast.LENGTH_LONG).show()
                                })
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportar", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }

                        // Import button
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch("application/octet-stream")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_backup_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OceanBlue),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importar", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Section: Lembretes de Contas (Offline)
        item {
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(LavenderPurple.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = "Lembretes",
                                    tint = LavenderPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Lembretes Diários de Contas",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    "Avisos locais às 09h para contas a vencer e vencidas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked && Build.VERSION.SDK_INT >= 33) {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    BillReminderWorker.setRemindersEnabled(context, isChecked)
                                    remindersEnabled = isChecked
                                    val msg = if (isChecked) "Lembretes ativados." else "Lembretes desativados."
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGreen
                            )
                        )
                    }

                    Text(
                        text = "100% offline. Prevenção de repetição no mesmo dia e privacidade na tela de bloqueio (valores ocultos).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Section: CSV Export
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                    .size(36.dp)
                                    .background(EmeraldGreen.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TableChart,
                                contentDescription = "CSV",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                "Exportar para Planilha (CSV)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                "UTF-8 BOM • Proteção contra Injeção de Fórmulas",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Text(
                        text = "Escolha o período para exportar:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !csvFilterMonthOnly,
                            onClick = { csvFilterMonthOnly = false },
                            label = { Text("Todos (${items.size})") }
                        )
                        FilterChip(
                            selected = csvFilterMonthOnly,
                            onClick = { csvFilterMonthOnly = true },
                            label = { Text("Mês Atual") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val itemsToExport = if (csvFilterMonthOnly) {
                                    val cal = java.util.Calendar.getInstance()
                                    MonthlyFinanceCalculator.filterItemsByMonth(items, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
                                } else items

                                if (itemsToExport.isEmpty()) {
                                    Toast.makeText(context, "Nenhum lançamento no período selecionado.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                try {
                                    val csvFile = CsvExporter.exportToTempFile(context, itemsToExport)
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "com.example.fileprovider",
                                        csvFile
                                    )
                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Exportar Finanças em CSV"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Erro ao gerar arquivo CSV: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartilhar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedButton(
                            onClick = {
                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                saveCsvLauncher.launch("privafin_$timestamp.csv")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salvar em Pasta", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Danger Zone
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.3f)))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Perigo",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "Zona de Perigo",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        "Deseja apagar todos os registros de finanças salvos localmente neste aparelho? Esta ação é irreversível.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Button(
                        onClick = { showWipeConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Limpar Todos os Dados", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Restore dialog prompting password
    if (showRestoreDialog && selectedFileToRestore != null) {
        Dialog(onDismissRequest = { showRestoreDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Restaurar Backup",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    Text(
                        "Digite a senha configurada no momento da exportação para descriptografar os dados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("Senha do Backup") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OceanBlue,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showRestoreDialog = false }) {
                            Text("Cancelar", color = TextSecondary)
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Button(
                            onClick = {
                                if (restorePassword.isBlank()) {
                                    Toast.makeText(context, "Por favor, digite a senha.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onManualRestore(restorePassword, selectedFileToRestore!!, {
                                    Toast.makeText(context, "Backup restaurado com sucesso!", Toast.LENGTH_LONG).show()
                                    showRestoreDialog = false
                                    restorePassword = ""
                                    selectedFileToRestore = null
                                }, { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                })
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Descriptografar & Restaurar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Wipe confirmation Dialog
    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Confirmar Exclusão Total?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Esta ação irá apagar definitivamente todos os registros de Salários, Investimentos, Caixinhas e Empréstimos do dispositivo. Deseja prosseguir?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onWipeAllData()
                        showWipeConfirm = false
                        Toast.makeText(context, "Todos os dados foram excluídos com sucesso.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sim, Apagar Tudo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
