package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class BackupManager(
    private val context: Context,
    private val repository: FinanceRepository
) {
    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, FinanceItem::class.java)
    private val adapter = moshi.adapter<List<FinanceItem>>(listType)

    private val AUTO_BACKUP_PASSWORD = "PrivaFin_AutoBackup_SecureKey_2026"

    val autoBackupFile: File
        get() = File(context.filesDir, "auto_backup.bin")

    /**
     * Performs a backup to a specified file with a password.
     */
    suspend fun performBackup(password: String, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val items = repository.allItems.first()
            val json = adapter.toJson(items) ?: "[]"
            val keySpec = CryptoHelper.deriveKey(password)
            val encryptedData = CryptoHelper.encrypt(json, keySpec)
            targetFile.writeText(encryptedData)
            Log.d("BackupManager", "Backup created successfully in ${targetFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "Failed to perform backup", e)
            false
        }
    }

    /**
     * Performs silent automatic backup.
     */
    suspend fun performAutoBackup(): Boolean {
        return performBackup(AUTO_BACKUP_PASSWORD, autoBackupFile)
    }

    /**
     * Decrypts and restores the database from a specified file with a password.
     */
    suspend fun restoreBackup(password: String, sourceFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) return@withContext false
            val encryptedData = sourceFile.readText()
            val keySpec = CryptoHelper.deriveKey(password)
            val json = CryptoHelper.decrypt(encryptedData, keySpec)
            val items = adapter.fromJson(json) ?: return@withContext false
            
            // Clear current DB and restore
            repository.clearAll()
            repository.insertAll(items)
            
            Log.d("BackupManager", "Backup restored successfully from ${sourceFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "Failed to restore backup", e)
            false
        }
    }

    /**
     * Restores from silent automatic backup.
     */
    suspend fun restoreAutoBackup(): Boolean {
        return restoreBackup(AUTO_BACKUP_PASSWORD, autoBackupFile)
    }

    /**
     * Validates a backup file and returns the number of items it contains.
     * Returns null if decryption fails.
     */
    suspend fun getBackupItemCount(password: String, sourceFile: File): Int? = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) return@withContext null
            val encryptedData = sourceFile.readText()
            val keySpec = CryptoHelper.deriveKey(password)
            val json = CryptoHelper.decrypt(encryptedData, keySpec)
            val items = adapter.fromJson(json)
            items?.size
        } catch (e: Exception) {
            Log.e("BackupManager", "Failed to check backup content", e)
            null
        }
    }
}
