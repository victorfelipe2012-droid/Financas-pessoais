package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sync.AndroidSyncServer
import com.example.ui.theme.*

@Composable
fun SyncDialog(
    syncServer: AndroidSyncServer,
    onDismiss: () -> Unit
) {
    val isRunning by syncServer.isServerRunning.collectAsState()
    val pin by syncServer.currentPin.collectAsState()
    val status by syncServer.lastSyncStatus.collectAsState()
    val ipAddress = remember { syncServer.getLocalIpAddress() }

    LaunchedEffect(Unit) {
        if (!isRunning) {
            syncServer.startServer()
        }
    }

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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(OceanBlue.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, tint = OceanBlue)
                        }
                        Column {
                            Text(
                                "Sincronizar com Windows",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                "Conexão direta Wi-Fi / Rede Local",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = TextSecondary)
                    }
                }

                // Step 1: Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Endereço IP no Wi-Fi:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                "$ipAddress:8765",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = OceanBlue
                            )
                        }

                        Divider(color = BorderColor.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PIN de Emparelhamento:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("Digite este PIN no aplicativo Windows", style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = 0.8f))
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(EmeraldGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = pin,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 4.sp,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = EmeraldGreen
                                    )
                                }

                                IconButton(
                                    onClick = { syncServer.generateNewPin() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Novo PIN", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Instructions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = OceanBlue.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Como conectar no Computador:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = OceanBlue
                        )
                        Text(
                            "1. Abra o PrivaFin no seu computador Windows.\n" +
                            "2. Clique no menu 'Sincronizar com Android'.\n" +
                            "3. Insira o IP ($ipAddress) e o PIN ($pin) e clique em 'Sincronizar Agora'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }

                // Status Message
                if (status != null) {
                    Text(
                        text = status ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = if (status?.contains("sucesso", ignoreCase = true) == true) EmeraldGreen else GoldAmber,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Toggle and Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (isRunning) syncServer.stopServer() else syncServer.startServer()
                        }
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = if (isRunning) CoralRed else EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isRunning) "Parar Servidor" else "Iniciar Servidor",
                            color = if (isRunning) CoralRed else EmeraldGreen,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Fechar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
