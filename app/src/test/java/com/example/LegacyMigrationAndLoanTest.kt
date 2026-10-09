package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LegacyMigrationAndLoanTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(db.financeDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testLegacyMigrationWithTwoIdenticalTextualLines() = runBlocking {
        // Empréstimo legado com duas linhas de abatimento IDÊNTICAS no texto
        val legacyDescription = """
            Empréstimo concedido para reforma.
            Abatido R$ 100,00 em 15/05/2026 - Parcela em dinheiro
            Abatido R$ 100,00 em 15/05/2026 - Parcela em dinheiro
            Anotação qualquer sem formato de abatimento
        """.trimIndent()

        val legacyLoan = FinanceItem(
            id = 1,
            title = "Empréstimo Irmão",
            type = "LENT",
            category = "Empréstimo",
            amountCents = 30000L, // Saldo restante R$ 300,00
            targetAmountCents = 50000L, // Principal original R$ 500,00
            date = 1715731200000L,
            description = legacyDescription,
            isCompleted = false
        )

        repository.insertItem(legacyLoan)

        // Executar migração de histórico legado
        repository.ensureLegacyDataMigrated()

        // Verificar que foram gerados 2 pagamentos estruturados independentes
        val payments = repository.getPaymentsForLoanSync(1)
        assertEquals("Devem ser gerados exatamente 2 pagamentos para as 2 linhas", 2, payments.size)

        // IDs devem ser estáveis, distintos e independentes
        assertNotEquals("IDs dos pagamentos devem ser distintos", payments[0].id, payments[1].id)
        assertEquals(10000L, payments[0].amountCents)
        assertEquals(10000L, payments[1].amountCents)
        assertEquals("Parcela em dinheiro", payments[0].note)
        assertEquals("Parcela em dinheiro", payments[1].note)

        // Descrição original deve ser 100% preservada
        val reloadedLoan = repository.getItemById(1)!!
        assertEquals(legacyDescription, reloadedLoan.description)
    }

    @Test
    fun testSettleAndReopenLoanRules() = runBlocking {
        val loan = FinanceItem(
            id = 10,
            title = "Empréstimo Colega",
            type = "LENT",
            amountCents = 40000L, // R$ 400,00 restante
            targetAmountCents = 40000L, // R$ 400,00 principal
            date = 1000L,
            isCompleted = false
        )
        repository.insertItem(loan)

        // 1. Quitar integralmente
        db.financeDao().settleLoan(loanId = 10, remainingCents = 40000L, paymentDate = 2000L, note = "Quitação")
        val settledLoan = repository.getItemById(10)!!
        assertTrue("Empréstimo quitado deve ter isCompleted = true", settledLoan.isCompleted)

        val paymentsAfterSettle = repository.getPaymentsForLoanSync(10)
        assertEquals(1, paymentsAfterSettle.size)
        assertEquals(40000L, paymentsAfterSettle[0].amountCents)

        // 2. Reabrir estornando quitação
        db.financeDao().reopenLoan(loanId = 10, removeLastPayment = true)
        val reopenedLoan = repository.getItemById(10)!!
        assertFalse("Empréstimo reaberto deve ter isCompleted = false", reopenedLoan.isCompleted)
        assertNull("Empréstimo reaberto deve zerar data de quitação", reopenedLoan.paymentDate)

        val paymentsAfterReopen = repository.getPaymentsForLoanSync(10)
        assertEquals("Pagamento de quitação deve ser removido no estorno", 0, paymentsAfterReopen.size)
    }

    @Test
    fun testEstornoOperatesOnSpecificPaymentOnly() = runBlocking {
        val loan = FinanceItem(id = 5, title = "Empréstimo X", type = "LENT", amountCents = 20000L, targetAmountCents = 50000L, isCompleted = false)
        repository.insertItem(loan)

        val p1 = LoanPayment(loanId = 5, amountCents = 15000L, paymentDate = 1000L, note = "P1")
        val p2 = LoanPayment(loanId = 5, amountCents = 15000L, paymentDate = 2000L, note = "P2")
        val p1Id = db.financeDao().insertLoanPayment(p1)
        val p2Id = db.financeDao().insertLoanPayment(p2)

        val initialPayments = repository.getPaymentsForLoanSync(5)
        assertEquals(2, initialPayments.size)

        // Estornar apenas o pagamento p1
        val paymentToDelete = initialPayments.first { it.id == p1Id }
        repository.deleteLoanPayment(paymentToDelete)

        val remainingPayments = repository.getPaymentsForLoanSync(5)
        assertEquals(1, remainingPayments.size)
        assertEquals(p2Id, remainingPayments[0].id)
        assertEquals("P2", remainingPayments[0].note)
    }
}
