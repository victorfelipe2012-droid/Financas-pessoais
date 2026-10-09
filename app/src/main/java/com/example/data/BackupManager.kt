package com.example.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupValidationResult(
    val isValid: Boolean,
    val itemCount: Int = 0,
    val paymentCount: Int = 0,
    val movementCount: Int = 0,
    val errorMessage: String? = null,
    val payload: BackupPayloadV2? = null
)

class BackupManager(
    private val context: Context,
    private val repository: FinanceRepository,
    private val categoryPreferences: CategoryPreferences? = null
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val v2Adapter = moshi.adapter(BackupPayloadV2::class.java)
    private val legacyListType = Types.newParameterizedType(List::class.java, LegacyFinanceItemDto::class.java)
    private val legacyAdapter = moshi.adapter<List<LegacyFinanceItemDto>>(legacyListType)

    private val backupMutex = Mutex()

    private val backupDir: File
        get() = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }

    val autoBackupFile: File
        get() = File(context.filesDir, "auto_backup_v2.bin")

    val legacyAutoBackupFile: File
        get() = File(context.filesDir, "auto_backup.bin")

    val preRestoreSnapshotFile: File
        get() = File(backupDir, "pre_restore_snapshot.bin")

    private val LEGACY_AUTO_BACKUP_PASSWORD = "PrivaFin_AutoBackup_SecureKey_2026"
    private val SNAPSHOT_LOCAL_KEY = "PrivaFin_Local_Snapshot_Key_2026"

    /**
     * Escreve conteúdo de arquivo de forma estritamente atômica com sync de descritor.
     * Preserva o arquivo de destino original caso ocorra qualquer falha na substituição.
     */
    fun writeAtomically(targetFile: File, content: String) {
        val parentDir = targetFile.parentFile ?: context.filesDir
        if (!parentDir.exists()) parentDir.mkdirs()
        val tempFile = File(parentDir, "${targetFile.name}.${System.nanoTime()}.tmp")
        FileOutputStream(tempFile).use { fos ->
            fos.write(content.toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.fd.sync()
        }

        var replaced = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                try {
                    Files.move(
                        tempFile.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                    replaced = true
                } catch (e: AtomicMoveNotSupportedException) {
                    Files.move(
                        tempFile.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    replaced = true
                }
            } catch (_: Exception) {
                replaced = false
            }
        }

        if (!replaced) {
            if (!targetFile.exists()) {
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
            } else {
                val bakFile = File(parentDir, "${targetFile.name}.${System.nanoTime()}.bak")
                if (targetFile.renameTo(bakFile)) {
                    if (tempFile.renameTo(targetFile)) {
                        bakFile.delete()
                    } else {
                        // Restaura cópia original preservada
                        bakFile.renameTo(targetFile)
                        tempFile.delete()
                        throw IOException("Falha ao substituir arquivo de destino atomicamente.")
                    }
                } else {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
            }
        }
    }

    /**
     * Gera e exporta um backup completo protegido por senha (PBKDF2-HMAC-SHA256 + AES-GCM).
     */
    suspend fun performBackup(password: String, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            try {
                if (password.length < 4) return@withContext false

                val payload = buildCurrentPayload()
                val json = v2Adapter.toJson(payload) ?: return@withContext false
                val encrypted = CryptoHelper.encryptModern(json, password.toCharArray())

                writeAtomically(targetFile, encrypted)
                Log.d("BackupManager", "Backup exportado com sucesso em ${targetFile.absolutePath}")
                true
            } catch (e: Exception) {
                Log.e("BackupManager", "Falha ao exportar backup", e)
                false
            }
        }
    }

    /**
     * Executa auto-backup local seguro com rotação de até 5 versões e chave protegida pelo KeyStore.
     * Sem fallback de senha fixa: falha do KeyStore é comunicada sem substituir nem corromper a cópia válida.
     */
    suspend fun performAutoBackup(): Boolean = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            try {
                val payload = buildCurrentPayload()
                val json = v2Adapter.toJson(payload) ?: return@withContext false

                val encrypted = try {
                    AndroidKeyStoreHelper.encrypt(json)
                } catch (e: Exception) {
                    Log.e("BackupManager", "KeyStore indisponível ou falhou ao realizar auto-backup", e)
                    return@withContext false
                }

                writeAtomically(autoBackupFile, encrypted)
                rotateAutoBackups(encrypted)
                Log.d("BackupManager", "Auto-backup local atualizado com sucesso.")
                true
            } catch (e: Exception) {
                Log.e("BackupManager", "Falha ao realizar auto-backup", e)
                false
            }
        }
    }

    /**
     * Valida integralmente um arquivo de backup antes de qualquer alteração no banco:
     * - Versão suportada (1, 2, 3);
     * - IDs duplicados em todas as entidades;
     * - Vínculos de chaves estrangeiras (mesmo com items vazio);
     * - Valores e datas.
     */
    fun validateBackupContent(decryptedJson: String): BackupValidationResult {
        try {
            // Tenta formato moderno (v2 / v3)
            val v2 = try { v2Adapter.fromJson(decryptedJson) } catch (_: Exception) { null }
            if (v2 != null && v2.version >= 2) {
                if (v2.version !in 2..3) {
                    return BackupValidationResult(false, errorMessage = "Versão de backup não suportada: ${v2.version}")
                }

                for (item in v2.items) {
                    if (item.title.isBlank()) return BackupValidationResult(false, errorMessage = "Item sem título encontrado.")
                    if (item.amountCents < 0L) return BackupValidationResult(false, errorMessage = "Valor financeiro negativo inválido encontrado.")
                    if (item.date <= 0L) return BackupValidationResult(false, errorMessage = "Data de lançamento inválida.")
                }

                val itemIds = v2.items.map { it.id }
                if (itemIds.size != itemIds.distinct().size) {
                    return BackupValidationResult(false, errorMessage = "IDs duplicados encontrados na lista de lançamentos.")
                }

                val paymentIds = v2.loanPayments.map { it.id }
                if (paymentIds.size != paymentIds.distinct().size) {
                    return BackupValidationResult(false, errorMessage = "IDs duplicados encontrados em pagamentos de empréstimos.")
                }

                val movementIds = v2.boxMovements.map { it.id }
                if (movementIds.size != movementIds.distinct().size) {
                    return BackupValidationResult(false, errorMessage = "IDs duplicados encontrados em movimentações de caixinhas.")
                }

                val itemIdSet = itemIds.toSet()

                // Validação estrita de chave estrangeira (rejeita se apontar para item inexistente, inclusive se items estiver vazio)
                for (p in v2.loanPayments) {
                    if (p.amountCents <= 0L) return BackupValidationResult(false, errorMessage = "Pagamento com valor zerado ou negativo.")
                    if (p.loanId !in itemIdSet) {
                        return BackupValidationResult(false, errorMessage = "Pagamento vinculado a empréstimo inexistente (ID ${p.loanId}).")
                    }
                }

                for (m in v2.boxMovements) {
                    if (m.amountCents <= 0L) return BackupValidationResult(false, errorMessage = "Movimentação vinculada a caixinha com valor zerado ou negativo.")
                    if (m.boxId !in itemIdSet) {
                        return BackupValidationResult(false, errorMessage = "Movimentação vinculada a caixinha inexistente (ID ${m.boxId}).")
                    }
                }

                return BackupValidationResult(
                    isValid = true,
                    itemCount = v2.items.size,
                    paymentCount = v2.loanPayments.size,
                    movementCount = v2.boxMovements.size,
                    payload = v2
                )
            }

            // Tenta formato legado v1 (Lista de LegacyFinanceItemDto)
            val legacyList = try { legacyAdapter.fromJson(decryptedJson) } catch (_: Exception) { null }
            if (legacyList != null) {
                for (leg in legacyList) {
                    if (leg.title.isBlank()) return BackupValidationResult(false, errorMessage = "Item legado com título inválido.")
                    if (leg.amount < 0.0) return BackupValidationResult(false, errorMessage = "Item legado com valor negativo inválido.")
                    if (leg.date <= 0L) return BackupValidationResult(false, errorMessage = "Item legado com data inválida.")
                }

                val convertedItems = legacyList.map { it.toFinanceItem() }
                val itemIds = convertedItems.map { it.id }
                if (itemIds.size != itemIds.distinct().size) {
                    return BackupValidationResult(false, errorMessage = "IDs duplicados encontrados no backup legado.")
                }

                val convertedPayload = BackupPayloadV2(
                    version = 1,
                    items = convertedItems
                )
                return BackupValidationResult(
                    isValid = true,
                    itemCount = convertedItems.size,
                    payload = convertedPayload
                )
            }

            return BackupValidationResult(false, errorMessage = "Formato de arquivo incompatível ou desconhecido.")
        } catch (e: Exception) {
            return BackupValidationResult(false, errorMessage = "Erro estrutural ao ler o backup: ${e.message}")
        }
    }

    /**
     * Restaura backup de um arquivo com senha, garantindo validação completa, snapshot de segurança criptografado e rollback.
     */
    suspend fun restoreBackup(password: String, sourceFile: File): Boolean = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            try {
                if (!sourceFile.exists()) return@withContext false

                val encryptedContent = sourceFile.readText()
                val decryptedJson = try {
                    CryptoHelper.decrypt(encryptedContent, password.toCharArray())
                } catch (e: Exception) {
                    Log.e("BackupManager", "Senha incorreta ou descriptografia falhou", e)
                    return@withContext false
                }

                val validation = validateBackupContent(decryptedJson)
                if (!validation.isValid || validation.payload == null) {
                    Log.e("BackupManager", "Backup rejeitado na validação: ${validation.errorMessage}")
                    return@withContext false
                }

                // 1. Criar cópia de recuperação anterior à restauração (pre-restore snapshot criptografado)
                val snapshotCreated = createPreRestoreSnapshot()
                if (!snapshotCreated) {
                    Log.e("BackupManager", "Falha de segurança: snapshot pré-restauração não pôde ser criado. Restauração abortada.")
                    return@withContext false
                }

                // 2. Aplicar a substituição transacional atômica
                val payload = validation.payload
                try {
                    repository.replaceFullData(
                        items = payload.items,
                        loanPayments = payload.loanPayments,
                        boxMovements = payload.boxMovements,
                        recurringBills = payload.recurringBills,
                        categoryBudgets = payload.categoryBudgets
                    )

                    // Substituição exata de subcategorias, inclusive lista vazia
                    categoryPreferences?.setApartmentSubcategories(payload.apartmentSubcategories)

                    // Se for backup legado v1, migra notas e saldos automaticamente
                    if (payload.version < 2) {
                        repository.ensureLegacyDataMigrated()
                    }

                    Log.d("BackupManager", "Restauração concluída com sucesso! Total: ${payload.items.size} itens.")
                    true
                } catch (dbError: Exception) {
                    Log.e("BackupManager", "Erro ao gravar no banco, realizando rollback...", dbError)
                    rollbackFromSnapshot()
                    false
                }
            } catch (e: Exception) {
                Log.e("BackupManager", "Falha geral na restauração", e)
                false
            }
        }
    }

    /**
     * Restaura a partir do último auto-backup local (Keystore v2 ou legado v1).
     */
    suspend fun restoreAutoBackup(): Boolean = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            try {
                if (autoBackupFile.exists()) {
                    val encryptedContent = autoBackupFile.readText()
                    val decryptedJson = try {
                        if (encryptedContent.startsWith("KEYSTORE_V2:")) {
                            AndroidKeyStoreHelper.decrypt(encryptedContent)
                        } else {
                            CryptoHelper.decrypt(encryptedContent, SNAPSHOT_LOCAL_KEY.toCharArray())
                        }
                    } catch (e: Exception) {
                        Log.w("BackupManager", "Falha ao decodificar auto_backup_v2, tentando legado", e)
                        null
                    }

                    if (decryptedJson != null) {
                        val validation = validateBackupContent(decryptedJson)
                        if (validation.isValid && validation.payload != null) {
                            val snapshotCreated = createPreRestoreSnapshot()
                            if (!snapshotCreated) {
                                Log.e("BackupManager", "Falha de segurança: snapshot não pôde ser criado. Auto-restauração cancelada.")
                                return@withContext false
                            }
                            val p = validation.payload
                            repository.replaceFullData(
                                items = p.items,
                                loanPayments = p.loanPayments,
                                boxMovements = p.boxMovements,
                                recurringBills = p.recurringBills,
                                categoryBudgets = p.categoryBudgets
                            )
                            categoryPreferences?.setApartmentSubcategories(p.apartmentSubcategories)
                            return@withContext true
                        }
                    }
                }

                // Fallback: tentar auto-backup legado v1
                if (legacyAutoBackupFile.exists()) {
                    val encryptedContent = legacyAutoBackupFile.readText()
                    val decryptedJson = try {
                        CryptoHelper.decrypt(encryptedContent, LEGACY_AUTO_BACKUP_PASSWORD.toCharArray())
                    } catch (_: Exception) { null }

                    if (decryptedJson != null) {
                        val validation = validateBackupContent(decryptedJson)
                        if (validation.isValid && validation.payload != null) {
                            val snapshotCreated = createPreRestoreSnapshot()
                            if (!snapshotCreated) {
                                Log.e("BackupManager", "Falha de segurança: snapshot não pôde ser criado. Auto-restauração legada cancelada.")
                                return@withContext false
                            }
                            repository.replaceFullData(
                                items = validation.payload.items,
                                loanPayments = emptyList(),
                                boxMovements = emptyList(),
                                recurringBills = emptyList(),
                                categoryBudgets = emptyList()
                            )
                            categoryPreferences?.setApartmentSubcategories(validation.payload.apartmentSubcategories)
                            repository.ensureLegacyDataMigrated()
                            return@withContext true
                        }
                    }
                }

                false
            } catch (e: Exception) {
                Log.e("BackupManager", "Falha ao restaurar auto-backup", e)
                false
            }
        }
    }

    /**
     * Cria snapshot seguro e criptografado antes da exclusão total de dados.
     * Retorna true apenas se uma NOVA cópia foi efetivamente criada e verificada no disco.
     */
    suspend fun createPreWipeSnapshot(): Boolean = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            createPreRestoreSnapshot()
        }
    }

    private suspend fun buildCurrentPayload(): BackupPayloadV2 {
        val snapshot = repository.getFullDataSnapshot()
        val subcategories = categoryPreferences?.apartmentSubcategories?.value ?: emptyList()

        return BackupPayloadV2(
            version = 3,
            exportedAt = System.currentTimeMillis(),
            appVersion = "3.0",
            items = snapshot.items,
            loanPayments = snapshot.loanPayments,
            boxMovements = snapshot.boxMovements,
            recurringBills = snapshot.recurringBills,
            categoryBudgets = snapshot.categoryBudgets,
            apartmentSubcategories = subcategories
        )
    }

    private suspend fun createPreRestoreSnapshot(): Boolean {
        return try {
            if (preRestoreSnapshotFile.exists()) {
                preRestoreSnapshotFile.delete()
            }
            val payload = buildCurrentPayload()
            val json = v2Adapter.toJson(payload) ?: return false

            // Criptografa exclusivamente pelo AndroidKeyStore (sem fallback de senha fixa em texto para novos snapshots)
            val encryptedSnapshot = AndroidKeyStoreHelper.encrypt(json)

            writeAtomically(preRestoreSnapshotFile, encryptedSnapshot)
            val success = preRestoreSnapshotFile.exists() && preRestoreSnapshotFile.length() > 0L
            if (success) {
                Log.d("BackupManager", "Snapshot pré-restauração criptografado salvo em ${preRestoreSnapshotFile.absolutePath}")
            }
            success
        } catch (e: Exception) {
            Log.e("BackupManager", "Falha ao criar snapshot pré-restauração seguro", e)
            if (preRestoreSnapshotFile.exists()) {
                preRestoreSnapshotFile.delete()
            }
            false
        }
    }

    private suspend fun rollbackFromSnapshot() {
        try {
            if (preRestoreSnapshotFile.exists()) {
                val encryptedContent = preRestoreSnapshotFile.readText()
                val json = try {
                    if (encryptedContent.startsWith("KEYSTORE_V2:")) {
                        AndroidKeyStoreHelper.decrypt(encryptedContent)
                    } else {
                        CryptoHelper.decrypt(encryptedContent, SNAPSHOT_LOCAL_KEY.toCharArray())
                    }
                } catch (_: Exception) { null }

                if (json != null) {
                    val p = v2Adapter.fromJson(json)
                    if (p != null) {
                        repository.replaceFullData(
                            items = p.items,
                            loanPayments = p.loanPayments,
                            boxMovements = p.boxMovements,
                            recurringBills = p.recurringBills,
                            categoryBudgets = p.categoryBudgets
                        )
                        categoryPreferences?.setApartmentSubcategories(p.apartmentSubcategories)
                        Log.d("BackupManager", "Rollback com snapshot executado com sucesso.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Falha crítica no rollback", e)
        }
    }

    private fun rotateAutoBackups(encryptedContent: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(backupDir, "auto_backup_$timestamp.bin")
            writeAtomically(file, encryptedContent)

            // Manter apenas as últimas 5 cópias
            val list = backupDir.listFiles { f -> f.name.startsWith("auto_backup_") && f.name.endsWith(".bin") }
                ?.sortedByDescending { it.lastModified() } ?: return

            if (list.size > 5) {
                list.drop(5).forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.w("BackupManager", "Falha na rotação de backups", e)
        }
    }
}
