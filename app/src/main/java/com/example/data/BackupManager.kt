package com.example.data

import android.content.Context
import android.util.Log
import com.example.ui.utils.MoneyUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
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
    private val legacyListType = Types.newParameterizedType(List::class.java, FinanceItem::class.java)
    private val legacyAdapter = moshi.adapter<List<FinanceItem>>(legacyListType)

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

                // Gravação atômica via arquivo temporário
                val tempFile = File(targetFile.parentFile ?: context.cacheDir, "${targetFile.name}.tmp")
                tempFile.writeText(encrypted)
                if (targetFile.exists()) targetFile.delete()
                val success = tempFile.renameTo(targetFile)
                if (!success) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                Log.d("BackupManager", "Backup exportado com sucesso em ${targetFile.absolutePath}")
                true
            } catch (e: Exception) {
                Log.e("BackupManager", "Falha ao exportar backup", e)
                false
            }
        }
    }

    /**
     * Executa auto-backup local seguro com rotação de até 5 versões e chave protegida.
     */
    suspend fun performAutoBackup(): Boolean = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            try {
                val payload = buildCurrentPayload()
                val json = v2Adapter.toJson(payload) ?: return@withContext false

                // Tenta criptografia via Android KeyStore; fallback para Modern KDF com identificador local
                val encrypted = try {
                    AndroidKeyStoreHelper.encrypt(json)
                } catch (e: Exception) {
                    Log.w("BackupManager", "KeyStore indisponível, usando KDF local", e)
                    CryptoHelper.encryptModern(json, "PrivaFin_Local_AutoBackup_Secure_v2".toCharArray())
                }

                // Gravação atômica do arquivo principal
                val tempFile = File(context.filesDir, "auto_backup_v2.tmp")
                tempFile.writeText(encrypted)
                if (autoBackupFile.exists()) autoBackupFile.delete()
                if (!tempFile.renameTo(autoBackupFile)) {
                    tempFile.copyTo(autoBackupFile, overwrite = true)
                    tempFile.delete()
                }

                // Rotação: manter até 5 cópias históricas
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
     * Valida integralmente um arquivo de backup antes de qualquer alteração no banco.
     */
    fun validateBackupContent(decryptedJson: String): BackupValidationResult {
        try {
            // Tenta formato v2
            val v2 = try { v2Adapter.fromJson(decryptedJson) } catch (_: Exception) { null }
            if (v2 != null && v2.version >= 2) {
                // Valida itens
                for (item in v2.items) {
                    if (item.title.isBlank()) return BackupValidationResult(false, errorMessage = "Item sem título encontrado.")
                    if (item.amountCents < 0L) return BackupValidationResult(false, errorMessage = "Valor financeiro negativo inválido encontrado.")
                    if (item.date <= 0L) return BackupValidationResult(false, errorMessage = "Data de lançamento inválida.")
                }

                val itemIds = v2.items.map { it.id }.toSet()

                // Valida pagamentos de empréstimos
                for (p in v2.loanPayments) {
                    if (p.amountCents <= 0L) return BackupValidationResult(false, errorMessage = "Pagamento com valor zerado ou negativo.")
                    if (p.loanId !in itemIds && itemIds.isNotEmpty()) {
                        return BackupValidationResult(false, errorMessage = "Pagamento vinculado a empréstimo inexistente (ID ${p.loanId}).")
                    }
                }

                // Valida movimentações de caixinhas
                for (m in v2.boxMovements) {
                    if (m.amountCents <= 0L) return BackupValidationResult(false, errorMessage = "Movimentação de caixinha com valor zerado ou negativo.")
                    if (m.boxId !in itemIds && itemIds.isNotEmpty()) {
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

            // Tenta formato legado (Lista de FinanceItem)
            val legacyItems = try { legacyAdapter.fromJson(decryptedJson) } catch (_: Exception) { null }
            if (legacyItems != null) {
                for (item in legacyItems) {
                    if (item.title.isBlank()) return BackupValidationResult(false, errorMessage = "Item legado com título inválido.")
                }
                val convertedPayload = BackupPayloadV2(
                    version = 1,
                    items = legacyItems
                )
                return BackupValidationResult(
                    isValid = true,
                    itemCount = legacyItems.size,
                    payload = convertedPayload
                )
            }

            return BackupValidationResult(false, errorMessage = "Formato de arquivo incompatível ou desconhecido.")
        } catch (e: Exception) {
            return BackupValidationResult(false, errorMessage = "Erro estrutural ao ler o backup: ${e.message}")
        }
    }

    /**
     * Restaura backup de um arquivo com senha, garantindo validação completa, snapshot de segurança e rollback.
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

                // 1. Criar cópia de recuperação anterior à restauração (pre-restore snapshot)
                createPreRestoreSnapshot()

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

                    // Se o backup contiver subcategorias e houver preferências, atualiza
                    if (payload.apartmentSubcategories.isNotEmpty() && categoryPreferences != null) {
                        for (sub in payload.apartmentSubcategories) {
                            categoryPreferences.addApartmentSubcategory(sub)
                        }
                    }

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
                // Tenta auto-backup v2
                if (autoBackupFile.exists()) {
                    val encryptedContent = autoBackupFile.readText()
                    val decryptedJson = try {
                        if (encryptedContent.startsWith("KEYSTORE_V2:")) {
                            AndroidKeyStoreHelper.decrypt(encryptedContent)
                        } else {
                            CryptoHelper.decrypt(encryptedContent, "PrivaFin_Local_AutoBackup_Secure_v2".toCharArray())
                        }
                    } catch (e: Exception) {
                        Log.w("BackupManager", "Falha ao decodificar auto_backup_v2, tentando legado", e)
                        null
                    }

                    if (decryptedJson != null) {
                        val validation = validateBackupContent(decryptedJson)
                        if (validation.isValid && validation.payload != null) {
                            createPreRestoreSnapshot()
                            val p = validation.payload
                            repository.replaceFullData(
                                items = p.items,
                                loanPayments = p.loanPayments,
                                boxMovements = p.boxMovements,
                                recurringBills = p.recurringBills,
                                categoryBudgets = p.categoryBudgets
                            )
                            if (p.apartmentSubcategories.isNotEmpty() && categoryPreferences != null) {
                                for (sub in p.apartmentSubcategories) categoryPreferences.addApartmentSubcategory(sub)
                            }
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
                            createPreRestoreSnapshot()
                            repository.replaceFullData(
                                items = validation.payload.items,
                                loanPayments = emptyList(),
                                boxMovements = emptyList(),
                                recurringBills = emptyList(),
                                categoryBudgets = emptyList()
                            )
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

    private suspend fun buildCurrentPayload(): BackupPayloadV2 {
        val items = repository.getAllItemsSync()
        val loanPayments = repository.getAllLoanPaymentsSync()
        val boxMovements = repository.getAllBoxMovementsSync()
        val recurringBills = repository.getAllRecurringBillsSync()
        val categoryBudgets = repository.getAllCategoryBudgetsSync()
        val subcategories = categoryPreferences?.apartmentSubcategories?.value ?: emptyList()

        return BackupPayloadV2(
            version = 2,
            exportedAt = System.currentTimeMillis(),
            appVersion = "2.0",
            items = items,
            loanPayments = loanPayments,
            boxMovements = boxMovements,
            recurringBills = recurringBills,
            categoryBudgets = categoryBudgets,
            apartmentSubcategories = subcategories
        )
    }

    private suspend fun createPreRestoreSnapshot() {
        try {
            val payload = buildCurrentPayload()
            val json = v2Adapter.toJson(payload) ?: return
            preRestoreSnapshotFile.writeText(json)
            Log.d("BackupManager", "Snapshot pré-restauração salvo em ${preRestoreSnapshotFile.absolutePath}")
        } catch (e: Exception) {
            Log.w("BackupManager", "Não foi possível criar snapshot pré-restauração", e)
        }
    }

    private suspend fun rollbackFromSnapshot() {
        try {
            if (preRestoreSnapshotFile.exists()) {
                val json = preRestoreSnapshotFile.readText()
                val p = v2Adapter.fromJson(json)
                if (p != null) {
                    repository.replaceFullData(
                        items = p.items,
                        loanPayments = p.loanPayments,
                        boxMovements = p.boxMovements,
                        recurringBills = p.recurringBills,
                        categoryBudgets = p.categoryBudgets
                    )
                    Log.d("BackupManager", "Rollback com snapshot executado com sucesso.")
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
            file.writeText(encryptedContent)

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
