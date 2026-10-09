package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // --- FINANCE ITEMS ---

    @Query("SELECT * FROM finance_items ORDER BY date DESC")
    fun getAllItems(): Flow<List<FinanceItem>>

    @Query("SELECT * FROM finance_items ORDER BY date DESC")
    suspend fun getAllItemsSync(): List<FinanceItem>

    @Query("SELECT * FROM finance_items WHERE type = :type ORDER BY date DESC")
    fun getItemsByType(type: String): Flow<List<FinanceItem>>

    @Query("SELECT * FROM finance_items WHERE id = :id")
    suspend fun getItemById(id: Int): FinanceItem?

    @Query("SELECT * FROM finance_items WHERE (type = 'BILL' OR type = 'APARTMENT') AND isCompleted = 0")
    suspend fun getPendingBillsAndApartmentSync(): List<FinanceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FinanceItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FinanceItem>)

    @Update
    suspend fun updateItem(item: FinanceItem)

    @Delete
    suspend fun deleteItem(item: FinanceItem)

    @Query("DELETE FROM finance_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("DELETE FROM finance_items")
    suspend fun clearAll()

    // --- LOAN PAYMENTS ---

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY paymentDate ASC, id ASC")
    fun getPaymentsForLoan(loanId: Int): Flow<List<LoanPayment>>

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY paymentDate ASC, id ASC")
    suspend fun getPaymentsForLoanSync(loanId: Int): List<LoanPayment>

    @Query("SELECT * FROM loan_payments ORDER BY paymentDate ASC, id ASC")
    fun getAllLoanPayments(): Flow<List<LoanPayment>>

    @Query("SELECT * FROM loan_payments ORDER BY paymentDate ASC, id ASC")
    suspend fun getAllLoanPaymentsSync(): List<LoanPayment>

    @Query("SELECT * FROM loan_payments WHERE id = :id")
    suspend fun getLoanPaymentById(id: Long): LoanPayment?

    @Query("SELECT * FROM finance_items WHERE recurringBillId = :billId AND competence = :competence LIMIT 1")
    suspend fun getRecurringBillOccurrence(billId: Long, competence: String): FinanceItem?

    @Query("SELECT EXISTS(SELECT 1 FROM finance_items WHERE recurringBillId = :billId AND competence = :competence)")
    suspend fun hasRecurringBillOccurrence(billId: Long, competence: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanPayment(payment: LoanPayment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanPayments(payments: List<LoanPayment>)

    @Delete
    suspend fun deleteLoanPayment(payment: LoanPayment)

    @Query("DELETE FROM loan_payments WHERE id = :id")
    suspend fun deleteLoanPaymentById(id: Long)

    @Query("DELETE FROM loan_payments WHERE loanId = :loanId")
    suspend fun deletePaymentsForLoan(loanId: Int)

    @Query("DELETE FROM loan_payments")
    suspend fun clearLoanPayments()

    // --- BOX MOVEMENTS ---

    @Query("SELECT * FROM box_movements WHERE boxId = :boxId ORDER BY date ASC, id ASC")
    fun getMovementsForBox(boxId: Int): Flow<List<BoxMovement>>

    @Query("SELECT * FROM box_movements WHERE boxId = :boxId ORDER BY date ASC, id ASC")
    suspend fun getMovementsForBoxSync(boxId: Int): List<BoxMovement>

    @Query("SELECT * FROM box_movements ORDER BY date ASC, id ASC")
    fun getAllBoxMovements(): Flow<List<BoxMovement>>

    @Query("SELECT * FROM box_movements ORDER BY date ASC, id ASC")
    suspend fun getAllBoxMovementsSync(): List<BoxMovement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoxMovement(movement: BoxMovement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoxMovements(movements: List<BoxMovement>)

    @Delete
    suspend fun deleteBoxMovement(movement: BoxMovement)

    @Query("DELETE FROM box_movements WHERE id = :id")
    suspend fun deleteBoxMovementById(id: Long)

    @Query("DELETE FROM box_movements WHERE boxId = :boxId")
    suspend fun deleteMovementsForBox(boxId: Int)

    @Query("DELETE FROM box_movements")
    suspend fun clearBoxMovements()

    // --- RECURRING BILLS ---

    @Query("SELECT * FROM recurring_bills ORDER BY dueDay ASC, id ASC")
    fun getAllRecurringBills(): Flow<List<RecurringBill>>

    @Query("SELECT * FROM recurring_bills ORDER BY dueDay ASC, id ASC")
    suspend fun getAllRecurringBillsSync(): List<RecurringBill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringBill(bill: RecurringBill): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringBills(bills: List<RecurringBill>)

    @Update
    suspend fun updateRecurringBill(bill: RecurringBill)

    @Delete
    suspend fun deleteRecurringBill(bill: RecurringBill)

    @Query("DELETE FROM recurring_bills")
    suspend fun clearRecurringBills()

    // --- CATEGORY BUDGETS ---

    @Query("SELECT * FROM category_budgets ORDER BY category ASC")
    fun getAllCategoryBudgets(): Flow<List<CategoryBudget>>

    @Query("SELECT * FROM category_budgets ORDER BY category ASC")
    suspend fun getAllCategoryBudgetsSync(): List<CategoryBudget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryBudget(budget: CategoryBudget): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryBudgets(budgets: List<CategoryBudget>)

    @Delete
    suspend fun deleteCategoryBudget(budget: CategoryBudget)

    @Query("DELETE FROM category_budgets WHERE id = :id")
    suspend fun deleteCategoryBudgetById(id: Long)

    @Query("DELETE FROM category_budgets")
    suspend fun clearCategoryBudgets()

    // --- OPERAÇÕES TRANSACIONAIS SEGURAS (FASE 1) ---

    @Transaction
    suspend fun replaceAll(items: List<FinanceItem>) {
        clearAll()
        insertAll(items)
    }

    @Transaction
    suspend fun replaceFullData(
        items: List<FinanceItem>,
        loanPayments: List<LoanPayment>,
        boxMovements: List<BoxMovement>,
        recurringBills: List<RecurringBill>,
        categoryBudgets: List<CategoryBudget>
    ) {
        clearAll()
        clearLoanPayments()
        clearBoxMovements()
        clearRecurringBills()
        clearCategoryBudgets()
        if (items.isNotEmpty()) insertAll(items)
        if (loanPayments.isNotEmpty()) insertLoanPayments(loanPayments)
        if (boxMovements.isNotEmpty()) insertBoxMovements(boxMovements)
        if (recurringBills.isNotEmpty()) insertRecurringBills(recurringBills)
        if (categoryBudgets.isNotEmpty()) insertCategoryBudgets(categoryBudgets)
    }

    @Transaction
    suspend fun deleteLoanWithPayments(loanId: Int) {
        deletePaymentsForLoan(loanId)
        deleteItemById(loanId)
    }

    @Transaction
    suspend fun deleteBoxWithMovements(boxId: Int) {
        deleteMovementsForBox(boxId)
        deleteItemById(boxId)
    }

    @Transaction
    suspend fun getFullDataSnapshot(): FullDataSnapshot {
        return FullDataSnapshot(
            items = getAllItemsSync(),
            loanPayments = getAllLoanPaymentsSync(),
            boxMovements = getAllBoxMovementsSync(),
            recurringBills = getAllRecurringBillsSync(),
            categoryBudgets = getAllCategoryBudgetsSync()
        )
    }

    @Transaction
    suspend fun clearAllDataAtomic() {
        clearAll()
        clearLoanPayments()
        clearBoxMovements()
        clearRecurringBills()
        clearCategoryBudgets()
    }

    @Transaction
    suspend fun addLoanPaymentAtomic(loanId: Int, amountCents: Long, paymentDate: Long, note: String): Boolean {
        val loan = getItemById(loanId) ?: return false
        val payments = getPaymentsForLoanSync(loanId)
        val alreadyPaid = payments.sumOf { it.amountCents }
        val remaining = (loan.amountCents - alreadyPaid).coerceAtLeast(0L)
        if (amountCents <= 0L || amountCents > remaining) {
            return false
        }
        insertLoanPayment(
            LoanPayment(
                loanId = loanId,
                amountCents = amountCents,
                paymentDate = paymentDate,
                note = note.trim()
            )
        )
        val newPaid = alreadyPaid + amountCents
        val isCompleted = newPaid >= loan.amountCents && loan.amountCents > 0L
        updateItem(
            loan.copy(
                isCompleted = isCompleted,
                paymentDate = if (isCompleted) paymentDate else null
            )
        )
        return true
    }

    @Transaction
    suspend fun deleteLoanPaymentAtomic(paymentId: Long): Boolean {
        val payment = getLoanPaymentById(paymentId) ?: return false
        deleteLoanPayment(payment)
        val loan = getItemById(payment.loanId) ?: return true
        val remainingPayments = getPaymentsForLoanSync(loan.id)
        val totalPaid = remainingPayments.sumOf { it.amountCents }
        val isCompleted = totalPaid >= loan.amountCents && loan.amountCents > 0L
        updateItem(
            loan.copy(
                isCompleted = isCompleted,
                paymentDate = if (isCompleted) loan.paymentDate else null
            )
        )
        return true
    }

    @Transaction
    suspend fun settleLoanAtomic(loanId: Int, paymentDate: Long, note: String): Boolean {
        val loan = getItemById(loanId) ?: return false
        val payments = getPaymentsForLoanSync(loanId)
        val alreadyPaid = payments.sumOf { it.amountCents }
        val remaining = (loan.amountCents - alreadyPaid).coerceAtLeast(0L)
        if (remaining > 0L) {
            insertLoanPayment(
                LoanPayment(
                    loanId = loanId,
                    amountCents = remaining,
                    paymentDate = paymentDate,
                    note = note.ifBlank { "Quitação integral do saldo" }
                )
            )
        }
        updateItem(loan.copy(isCompleted = true, paymentDate = paymentDate))
        return true
    }

    @Transaction
    suspend fun reopenLoanAtomic(loanId: Int, removeLastPayment: Boolean): Boolean {
        val loan = getItemById(loanId) ?: return false
        val payments = getPaymentsForLoanSync(loanId)
        if (removeLastPayment && payments.isNotEmpty()) {
            deleteLoanPayment(payments.last())
        }
        val updatedPayments = getPaymentsForLoanSync(loanId)
        val totalPaid = updatedPayments.sumOf { it.amountCents }
        val isCompleted = totalPaid >= loan.amountCents && loan.amountCents > 0L
        updateItem(
            loan.copy(
                isCompleted = isCompleted,
                paymentDate = if (isCompleted) loan.paymentDate else null
            )
        )
        return true
    }

    @Transaction
    suspend fun updateLoanPrincipalAtomic(loanId: Int, newPrincipalCents: Long): Boolean {
        val loan = getItemById(loanId) ?: return false
        val payments = getPaymentsForLoanSync(loanId)
        val totalPaid = payments.sumOf { it.amountCents }
        if (newPrincipalCents < totalPaid) {
            return false
        }
        val isCompleted = totalPaid >= newPrincipalCents && newPrincipalCents > 0L
        updateItem(
            loan.copy(
                amountCents = newPrincipalCents,
                isCompleted = isCompleted,
                paymentDate = if (isCompleted) loan.paymentDate ?: System.currentTimeMillis() else null
            )
        )
        return true
    }

    @Transaction
    suspend fun generateRecurringBillOccurrenceAtomic(item: FinanceItem): Boolean {
        val billId = item.recurringBillId ?: return false
        val comp = item.competence ?: return false
        if (hasRecurringBillOccurrence(billId, comp)) {
            return false
        }
        insertItem(item)
        return true
    }

    @Transaction
    suspend fun settleLoan(loanId: Int, remainingCents: Long, paymentDate: Long, note: String) {
        settleLoanAtomic(loanId, paymentDate, note)
    }

    @Transaction
    suspend fun reopenLoan(loanId: Int, removeLastPayment: Boolean = false) {
        reopenLoanAtomic(loanId, removeLastPayment)
    }
}

data class FullDataSnapshot(
    val items: List<FinanceItem>,
    val loanPayments: List<LoanPayment>,
    val boxMovements: List<BoxMovement>,
    val recurringBills: List<RecurringBill>,
    val categoryBudgets: List<CategoryBudget>
)
