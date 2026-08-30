package com.example.sync

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.CategoryPreferences
import com.example.data.CryptoHelper
import com.example.data.FinanceItem
import com.example.data.FinanceRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random

class AndroidSyncServer(
    private val context: Context,
    private val repository: FinanceRepository,
    private val categoryPreferences: CategoryPreferences
) {
    private val moshi = Moshi.Builder().build()
    private val payloadAdapter = moshi.adapter(SyncPayload::class.java)
    private val responseAdapter = moshi.adapter(SyncResponse::class.java)

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _currentPin = MutableStateFlow(generateNewPin())
    val currentPin: StateFlow<String> = _currentPin.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow<String?>(null)
    val lastSyncStatus: StateFlow<String?> = _lastSyncStatus.asStateFlow()

    fun generateNewPin(): String {
        val pin = String.format("%04d", Random.nextInt(10000))
        _currentPin.value = pin
        return pin
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    fun startServer(port: Int = 8765) {
        if (_isServerRunning.value) return
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(port)
                _isServerRunning.value = true
                _lastSyncStatus.value = "Servidor aguardando conexão do Windows na porta $port..."
                Log.d("SyncServer", "Sync server started on port $port with PIN ${_currentPin.value}")

                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch { handleClient(clientSocket) }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e("SyncServer", "Error starting sync server", e)
                _lastSyncStatus.value = "Erro ao iniciar servidor: ${e.message}"
                _isServerRunning.value = false
            }
        }
    }

    fun stopServer() {
        try {
            serverJob?.cancel()
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            serverSocket = null
            serverJob = null
            _isServerRunning.value = false
            _lastSyncStatus.value = "Servidor de sincronização parado."
        }
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 15000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = OutputStreamWriter(socket.getOutputStream())

            // Read HTTP request line
            val requestLine = reader.readLine() ?: return@withContext
            var contentLength = 0
            var clientPinHeader: String? = null

            var line: String? = reader.readLine()
            while (!line.isNullOrBlank()) {
                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                }
                if (line.startsWith("X-Sync-Pin:", ignoreCase = true)) {
                    clientPinHeader = line.substringAfter(":").trim()
                }
                line = reader.readLine()
            }

            if (requestLine.startsWith("GET /info", ignoreCase = true)) {
                val ip = getLocalIpAddress()
                val json = """{"status":"ready","device":"Android","ip":"$ip","port":8765}"""
                sendHttpResponse(writer, 200, json)
                return@withContext
            }

            // Verify PIN
            val activePin = _currentPin.value
            if (clientPinHeader != activePin) {
                _lastSyncStatus.value = "Tentativa de conexão com PIN incorreto."
                val errorJson = responseAdapter.toJson(
                    SyncResponse(
                        success = false,
                        message = "PIN de emparelhamento incorreto.",
                        serverItems = emptyList(),
                        serverApartmentSubcategories = emptyList()
                    )
                )
                sendHttpResponse(writer, 401, errorJson)
                return@withContext
            }

            // Read JSON body
            val charBuffer = CharArray(contentLength)
            var bytesRead = 0
            while (bytesRead < contentLength) {
                val read = reader.read(charBuffer, bytesRead, contentLength - bytesRead)
                if (read == -1) break
                bytesRead += read
            }
            val requestBody = String(charBuffer, 0, bytesRead)

            // Parse client payload
            val clientPayload = payloadAdapter.fromJson(requestBody)
            if (clientPayload != null) {
                // Merge items bidirectionally
                val localItems = repository.allItems.first()
                val mergedItems = mergeItems(localItems, clientPayload.items)
                
                // Update local repository with new/merged items
                repository.clearAll()
                repository.insertAll(mergedItems)

                // Merge apartment subcategories
                clientPayload.apartmentSubcategories.forEach { sub ->
                    categoryPreferences.addApartmentSubcategory(sub)
                }
                val updatedSubcategories = categoryPreferences.apartmentSubcategories.value

                _lastSyncStatus.value = "Sincronizado com sucesso com ${clientPayload.deviceName} (${mergedItems.size} registros)."

                val responsePayload = SyncResponse(
                    success = true,
                    message = "Sincronização concluída com sucesso.",
                    serverItems = mergedItems,
                    serverApartmentSubcategories = updatedSubcategories
                )

                val responseJson = responseAdapter.toJson(responsePayload)
                sendHttpResponse(writer, 200, responseJson)
            } else {
                sendHttpResponse(writer, 400, """{"success":false,"message":"Payload inválido."}""")
            }
        } catch (e: Exception) {
            Log.e("SyncServer", "Client handling error", e)
            _lastSyncStatus.value = "Erro na sincronização: ${e.message}"
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendHttpResponse(writer: OutputStreamWriter, statusCode: Int, body: String) {
        val statusText = if (statusCode == 200) "OK" else if (statusCode == 401) "Unauthorized" else "Bad Request"
        val response = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n" +
                "Connection: close\r\n\r\n" +
                body
        writer.write(response)
        writer.flush()
    }

    private fun mergeItems(local: List<FinanceItem>, remote: List<FinanceItem>): List<FinanceItem> {
        val map = mutableMapOf<String, FinanceItem>()

        // Unique key generator for identifying items even across different devices
        fun makeKey(item: FinanceItem): String {
            return "${item.type}_${item.title.trim().lowercase()}_${item.amount}_${item.date}"
        }

        local.forEach { item ->
            map[makeKey(item)] = item
        }

        remote.forEach { remoteItem ->
            val key = makeKey(remoteItem)
            val existing = map[key]
            if (existing == null) {
                // New item from remote (assign id 0 so Room generates new primary key)
                map[key] = remoteItem.copy(id = 0)
            } else {
                // If existing, keep the completed status if either marked it completed
                if (remoteItem.isCompleted && !existing.isCompleted) {
                    map[key] = existing.copy(isCompleted = true)
                }
            }
        }

        return map.values.toList().sortedByDescending { it.date }
    }
}
