package com.example.data

import com.example.ui.utils.MoneyUtils
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale

class FinanceRepository(private val financeDao: FinanceDao) {

    // --- FINANCE ITEMS ---
    val allItems: Flow<List<FinanceItem>> = financeDao.getAllItems()

    suspend fun getAllItemsSync(): List<FinanceItem> = financeDao.getAllItemsSync()

    fun getItemsByType(type: String): Flow<List<FinanceItem>> = financeDao.getItemsByType(type)

    suspend fun getItemById(id: Int): FinanceItem? = financeDao.getItemById(id)

    suspend fun insertItem(item: FinanceItem): Long = financeDao.insertItem(item)

    suspend fun insertAll(items: List<FinanceItem>) = financeDao.insertAll(items)

    suspend fun updateItem(item: FinanceItem) = financeDao.updateItem(item)

    suspend fun deleteItem(item: FinanceItem) = financeDao.deleteItem(item)

    suspend fun deleteItemById(id: Int) = financeDao.deleteItemById(id)

    suspend fun clearAll() = financeDao.clearAll()

    // --- LOAN PAYMENTS ---
    val allLoanPayments: Flow<List<LoanPayment>> = financeDao.getAllLoanPayments()

    suspend fun getAllLoanPaymentsSync(): List<LoanPayment> = financeDao.getAllLoanPaymentsSync()

    fun getPaymentsForLoan(loanId: Int): Flow<List<LoanPayment>> = financeDao.getPaymentsForLoan(loanId)

    suspend fun getPaymentsForLoanSync(loanId: Int): List<LoanPayment> = financeDao.getPaymentsForLoanSync(loanId)

    suspend fun insertLoanPayment(payment: LoanPayment): Long = financeDao.insertLoanPayment(payment)

    suspend fun deleteLoanPayment(payment: LoanPayment) = financeDao.deleteLoanPayment(payment)

    suspend fun deleteLoanWithPayments(loanId: Int) = financeDao.deleteLoanWithPayments(loanId)

    suspend fun addLoanPaymentAtomic(loanId: Int, amountCents: Long, paymentDate: Long, note: String): Boolean =
        financeDao.addLoanPaymentAtomic(loanId, amountCents, paymentDate, note)

    suspend fun deleteLoanPaymentAtomic(paymentId: Long): Boolean =
        financeDao.deleteLoanPaymentAtomic(paymentId)

    suspend fun settleLoanAtomic(loanId: Int, paymentDate: Long, note: String): Boolean =
        financeDao.settleLoanAtomic(loanId, paymentDate, note)

    suspend fun reopenLoanAtomic(loanId: Int, removeLastPayment: Boolean = false): Boolean =
        financeDao.reopenLoanAtomic(loanId, removeLastPayment)

    suspend fun updateLoanPrincipalAtomic(loanId: Int, newPrincipalCents: Long): Boolean =
        financeDao.updateLoanPrincipalAtomic(loanId, newPrincipalCents)

    suspend fun updateLoanDetailsAtomic(loanId: Int, newTitle: String, newPrincipalCents: Long, newDate: Long): Boolean =
        financeDao.updateLoanDetailsAtomic(loanId, newTitle, newPrincipalCents, newDate)

    suspend fun settleLoan(loanId: Int, remainingCents: Long, paymentDate: Long, note: String) =
        financeDao.settleLoan(loanId, remainingCents, paymentDate, note)

    suspend fun reopenLoan(loanId: Int, removeLastPayment: Boolean = false) =
        financeDao.reopenLoan(loanId, removeLastPayment)

    // --- BOX MOVEMENTS ---
    val allBoxMovements: Flow<List<BoxMovement>> = financeDao.getAllBoxMovements()

    suspend fun getAllBoxMovementsSync(): List<BoxMovement> = financeDao.getAllBoxMovementsSync()

    fun getMovementsForBox(boxId: Int): Flow<List<BoxMovement>> = financeDao.getMovementsForBox(boxId)

    suspend fun getMovementsForBoxSync(boxId: Int): List<BoxMovement> = financeDao.getMovementsForBoxSync(boxId)

    suspend fun insertBoxMovement(movement: BoxMovement): Long = financeDao.insertBoxMovement(movement)

    suspend fun deleteBoxMovement(movement: BoxMovement) = financeDao.deleteBoxMovement(movement)

    suspend fun deleteBoxWithMovements(boxId: Int) = financeDao.deleteBoxWithMovements(boxId)

    // --- RECURRING BILLS ---
    val allRecurringBills: Flow<List<RecurringBill>> = financeDao.getAllRecurringBills()

    suspend fun getAllRecurringBillsSync(): List<RecurringBill> = financeDao.getAllRecurringBillsSync()

    suspend fun insertRecurringBill(bill: RecurringBill): Long = financeDao.insertRecurringBill(bill)

    suspend fun updateRecurringBill(bill: RecurringBill) = financeDao.updateRecurringBill(bill)

    suspend fun deleteRecurringBill(bill: RecurringBill) = financeDao.deleteRecurringBill(bill)

    // --- CATEGORY BUDGETS ---
    val allCategoryBudgets: Flow<List<CategoryBudget>> = financeDao.getAllCategoryBudgets()

    suspend fun getAllCategoryBudgetsSync(): List<CategoryBudget> = financeDao.getAllCategoryBudgetsSync()

    suspend fun insertCategoryBudget(budget: CategoryBudget): Long = financeDao.insertCategoryBudget(budget)

    suspend fun deleteCategoryBudget(budget: CategoryBudget) = financeDao.deleteCategoryBudget(budget)

    // --- TRANSACTIONS ---
    suspend fun replaceAll(items: List<FinanceItem>) = financeDao.replaceAll(items)

    suspend fun getFullDataSnapshot(): FullDataSnapshot = financeDao.getFullDataSnapshot()

    suspend fun clearAllDataAtomic() = financeDao.clearAllDataAtomic()

    suspend fun generateRecurringBillOccurrenceAtomic(item: FinanceItem): Boolean =
        financeDao.generateRecurringBillOccurrenceAtomic(item)

    suspend fun replaceFullData(
        items: List<FinanceItem>,
        loanPayments: List<LoanPayment>,
        boxMovements: List<BoxMovement>,
        recurringBills: List<RecurringBill>,
        categoryBudgets: List<CategoryBudget>
    ) = financeDao.replaceFullData(items, loanPayments, boxMovements, recurringBills, categoryBudgets)

    /**
     * Migra com segurança dados legados em formato texto (descrições de empréstimos e saldos de caixinhas)
     * para as novas tabelas estruturadas sem perda de dados e sem alterar descrições originais.
     * Utiliza marcador persistente `isHistoryMigrated` para impedir remigração após estorno de pagamentos.
     */
    suspend fun ensureLegacyDataMigrated() {
        val allItems = financeDao.getAllItemsSync()

        // 1. Migração transacional de empréstimos (v1/v2 -> v3)
        val loans = allItems.filter { it.type == "LENT" && !it.isHistoryMigrated }
        for (loan in loans) {
            financeDao.migrateLoanRecordAtomic(loan.id)
        }

        // 2. Migração de caixinhas para histórico estruturado
        val boxes = allItems.filter { it.type == "BOX" }
        for (box in boxes) {
            val movements = financeDao.getMovementsForBoxSync(box.id)
            if (movements.isEmpty() && box.amountCents > 0L) {
                // Cria o aporte inicial para corresponder ao saldo registrado
                financeDao.insertBoxMovement(
                    BoxMovement(
                        boxId = box.id,
                        amountCents = box.amountCents,
                        isDeposit = true,
                        date = box.date,
                        note = "Saldo inicial migrado",
                        createdAt = box.date
                    )
                )
            }
        }
    }
}
