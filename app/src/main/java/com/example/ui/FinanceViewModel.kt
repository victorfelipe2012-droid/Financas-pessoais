package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.utils.MoneyUtils
import com.example.ui.utils.RecurringBillManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.Locale

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

    val allLoanPayments: StateFlow<List<LoanPayment>> = repository.allLoanPayments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allBoxMovements: StateFlow<List<BoxMovement>> = repository.allBoxMovements
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allRecurringBills: StateFlow<List<RecurringBill>> = repository.allRecurringBills
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCategoryBudgets: StateFlow<List<CategoryBudget>> = repository.allCategoryBudgets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val apartmentSubcategories: StateFlow<List<String>> = categoryPreferences.apartmentSubcategories

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    private val _autoBackupTime = MutableStateFlow<Long?>(null)
    val autoBackupTime: StateFlow<Long?> = _autoBackupTime.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            // Migrar dados legados de descrições e caixinhas
            try {
                repository.ensureLegacyDataMigrated()
            } catch (e: Exception) {
                Log.w("FinanceViewModel", "Migração inicial de dados legados falhou", e)
            }

            // Gerar ocorrências de contas recorrentes para o mês atual
            try {
                generateCurrentMonthRecurringBills()
            } catch (e: Exception) {
                Log.w("FinanceViewModel", "Geração de contas recorrentes falhou", e)
            }

            // Checar auto backup existente
            val file = backupManager.autoBackupFile
            if (file.exists()) {
                _autoBackupTime.value = file.lastModified()
            } else if (backupManager.legacyAutoBackupFile.exists()) {
                _autoBackupTime.value = backupManager.legacyAutoBackupFile.lastModified()
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
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
            if (item.type == "LENT") {
                repository.deleteLoanWithPayments(item.id)
            } else if (item.type == "BOX") {
                repository.deleteBoxWithMovements(item.id)
            } else {
                repository.deleteItem(item)
            }
            triggerAutoBackup()
        }
    }

    fun deleteItemById(id: Int) {
        viewModelScope.launch {
            repository.deleteItemById(id)
            triggerAutoBackup()
        }
    }

    fun wipeAllData(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val snapshotCreated = backupManager.createPreWipeSnapshot()
                if (!snapshotCreated) {
                    onError("Falha de segurança: impossível criar cópia recuperável antes da exclusão. Operação cancelada.")
                    return@launch
                }
                repository.clearAllDataAtomic()
                categoryPreferences?.clearAllCategories()
                triggerAutoBackup()
                onSuccess()
            } catch (e: Exception) {
                onError("Falha ao apagar dados: ${e.message}")
            }
        }
    }

    // --- REGRAS DE EMPRÉSTIMOS ESTRUTURADOS (FASE 3) ---

    fun addLoanPayment(loanId: Int, amountCents: Long, paymentDate: Long, note: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            if (amountCents <= 0L) {
                onError("O valor do abatimento deve ser maior que zero.")
                return@launch
            }

            val success = repository.addLoanPaymentAtomic(loanId, amountCents, paymentDate, note)
            if (success) {
                triggerAutoBackup()
                onSuccess()
            } else {
                onError("Não foi possível registrar o abatimento. O valor ultrapassa o saldo restante ou o empréstimo não existe.")
            }
        }
    }

    fun deleteLoanPayment(payment: LoanPayment, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.deleteLoanPaymentAtomic(payment.id)
            if (success) {
                triggerAutoBackup()
                onSuccess()
            }
        }
    }

    fun settleLoan(loanId: Int, paymentDate: Long = System.currentTimeMillis(), note: String = "Quitação integral", onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.settleLoanAtomic(loanId, paymentDate, note)
            if (success) {
                triggerAutoBackup()
                onSuccess()
            }
        }
    }

    fun reopenLoan(loanId: Int, removeLastPayment: Boolean = false, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.reopenLoanAtomic(loanId, removeLastPayment)
            if (success) {
                triggerAutoBackup()
                onSuccess()
            }
        }
    }

    fun updateLoanPrincipal(loanId: Int, newPrincipalCents: Long, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.updateLoanPrincipalAtomic(loanId, newPrincipalCents)
            if (success) {
                triggerAutoBackup()
                onSuccess()
            } else {
                onError("O valor principal não pode ser menor do que o total já pago.")
            }
        }
    }

    fun updateLoan(
        loanId: Int,
        newTitle: String,
        newPrincipalCents: Long,
        newDate: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val trimmed = newTitle.trim()
            if (trimmed.isBlank()) {
                onError("O título do empréstimo não pode estar em branco.")
                return@launch
            }
            if (newPrincipalCents <= 0L) {
                onError("O valor do empréstimo deve ser maior que zero.")
                return@launch
            }
            val success = repository.updateLoanDetailsAtomic(loanId, trimmed, newPrincipalCents, newDate)
            if (success) {
                triggerAutoBackup()
                onSuccess()
            } else {
                onError("O novo valor principal não pode ser menor do que o total já amortizado.")
            }
        }
    }

    // --- REGRAS DE CAIXINHAS COM HISTÓRICO (FASE 4) ---

    fun addBoxMovement(boxId: Int, amountCents: Long, isDeposit: Boolean, date: Long, note: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            if (amountCents <= 0L) {
                onError("O valor da movimentação deve ser maior que zero.")
                return@launch
            }

            val box = repository.getItemById(boxId)
            if (box == null) {
                onError("Caixinha não encontrada.")
                return@launch
            }

            val movements = repository.getMovementsForBoxSync(boxId)
            val currentBalance = movements.sumOf { if (it.isDeposit) it.amountCents else -it.amountCents }

            if (!isDeposit && amountCents > currentBalance) {
                onError("Saldo insuficiente. O saldo atual é de ${MoneyUtils.formatCents(currentBalance)}.")
                return@launch
            }

            repository.insertBoxMovement(
                BoxMovement(
                    boxId = boxId,
                    amountCents = amountCents,
                    isDeposit = isDeposit,
                    date = date,
                    note = note.trim()
                )
            )

            val newBalance = currentBalance + (if (isDeposit) amountCents else -amountCents)
            val goalReached = box.targetAmountCents in 1..newBalance
            repository.updateItem(box.copy(
                amountCents = newBalance,
                isCompleted = goalReached
            ))

            triggerAutoBackup()
            onSuccess()
        }
    }

    fun deleteBoxMovement(movement: BoxMovement, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteBoxMovement(movement)
            val box = repository.getItemById(movement.boxId)
            if (box != null) {
                val movements = repository.getMovementsForBoxSync(movement.boxId)
                val newBalance = movements.sumOf { if (it.isDeposit) it.amountCents else -it.amountCents }
                val goalReached = box.targetAmountCents in 1..newBalance
                repository.updateItem(box.copy(amountCents = newBalance, isCompleted = goalReached))
            }
            triggerAutoBackup()
            onSuccess()
        }
    }

    // --- CONTAS RECORRENTES (FASE 8) ---

    fun insertRecurringBill(bill: RecurringBill) {
        viewModelScope.launch {
            repository.insertRecurringBill(bill)
            generateCurrentMonthRecurringBills()
            triggerAutoBackup()
        }
    }

    fun updateRecurringBill(bill: RecurringBill) {
        viewModelScope.launch {
            repository.updateRecurringBill(bill)
            if (bill.isActive) {
                generateCurrentMonthRecurringBills()
            }
            triggerAutoBackup()
        }
    }

    fun deleteRecurringBill(bill: RecurringBill) {
        viewModelScope.launch {
            repository.deleteRecurringBill(bill)
            triggerAutoBackup()
        }
    }

    suspend fun generateCurrentMonthRecurringBills() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // 1 a 12
        generateRecurringBillsForCompetence(currentYear, currentMonth)
    }

    suspend fun generateRecurringBillsForCompetence(year: Int, month: Int) {
        val recurringBills = repository.getAllRecurringBillsSync().filter { it.isActive }
        if (recurringBills.isEmpty()) return

        for (rule in recurringBills) {
            if (!RecurringBillManager.shouldGenerateForCompetence(rule, year, month)) {
                continue
            }
            val newItem = RecurringBillManager.buildOccurrenceItem(rule, year, month)
            repository.generateRecurringBillOccurrenceAtomic(newItem)
        }
    }

    // --- ORÇAMENTOS POR CATEGORIA (FASE 9) ---

    fun setCategoryBudget(category: String, limitCents: Long) {
        viewModelScope.launch {
            val trimmed = category.trim()
            if (trimmed.isEmpty()) return@launch
            val existing = repository.getAllCategoryBudgetsSync().find { it.category.equals(trimmed, ignoreCase = true) }
            if (existing != null) {
                repository.deleteCategoryBudget(existing)
            }
            if (limitCents > 0L) {
                repository.insertCategoryBudget(CategoryBudget(category = trimmed, limitCents = limitCents))
            }
            triggerAutoBackup()
        }
    }

    fun deleteCategoryBudget(budget: CategoryBudget) {
        viewModelScope.launch {
            repository.deleteCategoryBudget(budget)
            triggerAutoBackup()
        }
    }

    // --- DESAFIO 52 SEMANAS ---

    fun start52WeekChallenge(multiplier: Double) {
        viewModelScope.launch {
            val multCents = MoneyUtils.toCents(multiplier)
            val existingChallenges = allItems.value.filter { it.type == "CHALLENGE" }
            existingChallenges.forEach { repository.deleteItem(it) }

            val newChallenges = (1..52).map { week ->
                FinanceItem(
                    title = "Semana $week",
                    amountCents = week * multCents,
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

    // --- BACKUP E RESTAURAÇÃO ---

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
                Log.d("FinanceViewModel", "Auto-backup atualizado.")
            }
        }
    }

    // --- SUBCATEGORIAS ---

    fun addApartmentSubcategory(name: String) = categoryPreferences.addApartmentSubcategory(name)
    fun updateApartmentSubcategory(oldName: String, newName: String) = categoryPreferences.updateApartmentSubcategory(oldName, newName)
    fun deleteApartmentSubcategory(name: String) = categoryPreferences.deleteApartmentSubcategory(name)
    fun resetApartmentSubcategories() = categoryPreferences.resetToDefaults()

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                val db = AppDatabase.getDatabase(context)
                val repository = FinanceRepository(db.financeDao())
                val categoryPreferences = CategoryPreferences(context)
                val backupManager = BackupManager(context, repository, categoryPreferences)
                @Suppress("UNCHECKED_CAST")
                return FinanceViewModel(repository, backupManager, categoryPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
