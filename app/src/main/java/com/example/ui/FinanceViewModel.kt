package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BackupManager
import com.example.data.CategoryPreferences
import com.example.data.FinanceItem
import com.example.data.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val backupManager: BackupManager,
    private val categoryPreferences: CategoryPreferences
) : ViewModel() {

    val allItems: StateFlow<List<FinanceItem>> = repository.allItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val apartmentSubcategories: StateFlow<List<String>> = categoryPreferences.apartmentSubcategories

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    // Flag to keep track of automatic backup health
    private val _autoBackupTime = MutableStateFlow<Long?>(null)
    val autoBackupTime: StateFlow<Long?> = _autoBackupTime.asStateFlow()

    init {
        // Run an initial auto backup on startup if DB has items, or check if auto backup exists
        viewModelScope.launch {
            val file = backupManager.autoBackupFile
            if (file.exists()) {
                _autoBackupTime.value = file.lastModified()
            }
        }
    }

    fun insertItem(item: FinanceItem) {
        viewModelScope.launch {
            repository.insertItem(item)
            triggerAutoBackup()
        }
    }

    fun updateItem(item: FinanceItem) {
        viewModelScope.launch {
            repository.updateItem(item)
            triggerAutoBackup()
        }
    }

    fun deleteItem(item: FinanceItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
            triggerAutoBackup()
        }
    }

    fun deleteItemById(id: Int) {
        viewModelScope.launch {
            repository.deleteItemById(id)
            triggerAutoBackup()
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            repository.clearAll()
            triggerAutoBackup()
        }
    }

    fun start52WeekChallenge(multiplier: Double) {
        viewModelScope.launch {
            val existingChallenges = allItems.value.filter { it.type == "CHALLENGE" }
            existingChallenges.forEach { repository.deleteItem(it) }

            val newChallenges = (1..52).map { week ->
                FinanceItem(
                    title = "Semana $week",
                    amount = week * multiplier,
                    type = "CHALLENGE",
                    category = "MULTIPLIER_${multiplier.toInt()}",
                    isCompleted = false,
                    date = System.currentTimeMillis()
                )
            }
            repository.insertAll(newChallenges)
            triggerAutoBackup()
        }
    }

    fun reset52WeekChallenge() {
        viewModelScope.launch {
            val existingChallenges = allItems.value.filter { it.type == "CHALLENGE" }
            existingChallenges.forEach { repository.deleteItem(it) }
            triggerAutoBackup()
        }
    }

    fun archive52WeekChallenge() {
        viewModelScope.launch {
            val existingChallenges = allItems.value.filter { it.type == "CHALLENGE" }
            if (existingChallenges.isNotEmpty()) {
                val archiveTimestamp = System.currentTimeMillis()
                val archivedChallenges = existingChallenges.map {
                    it.copy(
                        type = "CHALLENGE_ARCHIVED",
                        category = "ARCHIVED_${archiveTimestamp}_${it.category}"
                    )
                }
                repository.insertAll(archivedChallenges)
                triggerAutoBackup()
            }
        }
    }

    fun restoreBackup(password: String, file: File, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = backupManager.restoreBackup(password, file)
            if (success) {
                _backupStatus.value = "Backup restaurado com sucesso!"
                onSuccess()
            } else {
                _backupStatus.value = "Falha ao restaurar backup. Senha incorreta ou arquivo inválido."
                onError("Senha incorreta ou arquivo de backup inválido.")
            }
        }
    }

    fun createBackup(password: String, file: File, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = backupManager.performBackup(password, file)
            if (success) {
                _backupStatus.value = "Backup exportado com sucesso!"
                onSuccess()
            } else {
                _backupStatus.value = "Falha ao exportar backup."
                onError()
            }
        }
    }

    fun restoreFromAutoBackup(onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = backupManager.restoreAutoBackup()
            if (success) {
                _backupStatus.value = "Backup automático restaurado!"
                onSuccess()
            } else {
                _backupStatus.value = "Nenhum backup automático encontrado ou falha ao restaurar."
                onError()
            }
        }
    }

    private fun triggerAutoBackup() {
        viewModelScope.launch {
            val success = backupManager.performAutoBackup()
            if (success) {
                _autoBackupTime.value = System.currentTimeMillis()
                Log.d("FinanceViewModel", "Backup automático criptografado localmente atualizado.")
            } else {
                Log.e("FinanceViewModel", "Falha no backup automático local.")
            }
        }
    }

    fun addApartmentSubcategory(name: String) {
        categoryPreferences.addApartmentSubcategory(name)
    }

    fun updateApartmentSubcategory(oldName: String, newName: String) {
        categoryPreferences.updateApartmentSubcategory(oldName, newName)
    }

    fun deleteApartmentSubcategory(name: String) {
        categoryPreferences.deleteApartmentSubcategory(name)
    }

    fun resetApartmentSubcategories() {
        categoryPreferences.resetToDefaults()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                val db = AppDatabase.getDatabase(context)
                val repository = FinanceRepository(db.financeDao())
                val backupManager = BackupManager(context, repository)
                val categoryPreferences = CategoryPreferences(context)
                @Suppress("UNCHECKED_CAST")
                return FinanceViewModel(repository, backupManager, categoryPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
