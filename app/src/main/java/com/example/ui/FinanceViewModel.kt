package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.utils.MoneyUtils
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

    fun wipeAllData() {
        viewModelScope.launch {
            repository.clearAll()
            triggerAutoBackup()
        }
    }

    // --- REGRAS DE EMPRÉSTIMOS ESTRUTURADOS (FASE 3) ---

    fun addLoanPayment(loanId: Int, amountCents: Long, paymentDate: Long, note: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            if (amountCents <= 0L) {
                onError("O valor do abatimento deve ser maior que zero.")
                return@launch
            }

            val loan = repository.getItemById(loanId)
            if (loan == null) {
                onError("Empréstimo não encontrado.")
                return@launch
            }

            val payments = repository.getPaymentsForLoanSync(loanId)
            val alreadyPaid = payments.sumOf { it.amountCents }
            val remaining = loan.amountCents - alreadyPaid

            if (amountCents > remaining) {
                onError("O valor do abatimento (${MoneyUtils.formatCents(amountCents)}) não pode ultrapassar o saldo restante (${MoneyUtils.formatCents(remaining)}).")
                return@launch
            }

            repository.insertLoanPayment(
                LoanPayment(
                    loanId = loanId,
                    amountCents = amountCents,
                    paymentDate = paymentDate,
                    note = note.trim()
                )
            )

            // Se quitou exatamente o total restante, marca como concluído
            if (amountCents == remaining) {
                repository.updateItem(loan.copy(isCompleted = true, paymentDate = paymentDate))
            }

            triggerAutoBackup()
            onSuccess()
        }
    }

    fun deleteLoanPayment(payment: LoanPayment, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteLoanPayment(payment)
            // Se o empréstimo estava quitado, reabre automaticamente pois um pagamento foi excluído
            val loan = repository.getItemById(payment.loanId)
            if (loan != null && loan.isCompleted) {
                repository.updateItem(loan.copy(isCompleted = false, paymentDate = null))
            }
            triggerAutoBackup()
            onSuccess()
        }
    }

    fun settleLoan(loanId: Int, paymentDate: Long = System.currentTimeMillis(), note: String = "Quitação integral", onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val loan = repository.getItemById(loanId) ?: return@launch
            val payments = repository.getPaymentsForLoanSync(loanId)
            val alreadyPaid = payments.sumOf { it.amountCents }
            val remaining = (loan.amountCents - alreadyPaid).coerceAtLeast(0L)

            repository.settleLoan(loanId, remaining, paymentDate, note)
            triggerAutoBackup()
            onSuccess()
        }
    }

    fun reopenLoan(loanId: Int, removeLastPayment: Boolean = false, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.reopenLoan(loanId, removeLastPayment)
            triggerAutoBackup()
            onSuccess()
        }
    }

    fun updateLoanPrincipal(loanId: Int, newPrincipalCents: Long, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val loan = repository.getItemById(loanId)
            if (loan == null) {
                onError("Empréstimo não encontrado.")
                return@launch
            }
            val payments = repository.getPaymentsForLoanSync(loanId)
            val totalPaid = payments.sumOf { it.amountCents }

            if (newPrincipalCents < totalPaid) {
                onError("O valor principal não pode ser menor do que o total já pago (${MoneyUtils.formatCents(totalPaid)}).")
                return@launch
            }

            val isNowCompleted = (newPrincipalCents == totalPaid)
            repository.updateItem(loan.copy(
                amountCents = newPrincipalCents,
                targetAmountCents = newPrincipalCents,
                isCompleted = isNowCompleted
            ))
            triggerAutoBackup()
            onSuccess()
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
            triggerAutoBackup()
        }
    }

    fun deleteRecurringBill(bill: RecurringBill) {
        viewModelScope.launch {
            repository.deleteRecurringBill(bill)
            triggerAutoBackup()
        }
    }

    private suspend fun generateCurrentMonthRecurringBills() {
        val recurringBills = repository.getAllRecurringBillsSync().filter { it.isActive }
        if (recurringBills.isEmpty()) return

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-based
        val competenceTag = String.format(Locale.US, "%04d-%02d", currentYear, currentMonth + 1)

        val existingItems = repository.getAllItemsSync()

        for (rule in recurringBills) {
            val descriptionTag = "[Recorrência #RecID_${rule.id}_$competenceTag]"
            val alreadyGenerated = existingItems.any { it.description.contains(descriptionTag) }

            if (!alreadyGenerated) {
                val billCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                    val actualDay = rule.dueDay.coerceIn(1, maxDay)
                    set(Calendar.DAY_OF_MONTH, actualDay)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }

                val newItem = FinanceItem(
                    title = rule.title,
                    amountCents = rule.amountCents,
                    type = rule.type,
                    category = rule.category,
                    date = billCal.timeInMillis,
                    dueDate = billCal.timeInMillis,
                    isCompleted = false,
                    description = "$descriptionTag Conta mensal recorrente."
                )
                repository.insertItem(newItem)
            }
        }
    }

    // --- ORÇAMENTOS POR CATEGORIA (FASE 9) ---

    fun setCategoryBudget(category: String, limitCents: Long) {
        viewModelScope.launch {
            val existing = repository.getAllCategoryBudgetsSync().find { it.category == category }
            if (existing != null) {
                repository.deleteCategoryBudget(existing)
            }
            if (limitCents > 0L) {
                repository.insertCategoryBudget(CategoryBudget(category = category, limitCents = limitCents))
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
