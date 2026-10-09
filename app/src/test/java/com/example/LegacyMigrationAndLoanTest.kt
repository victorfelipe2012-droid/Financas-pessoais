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
            amountCents = 30000L, // Saldo restante legado R$ 300,00
            targetAmountCents = 50000L, // Principal original R$ 500,00
            date = 1715731200000L,
            description = legacyDescription,
            isCompleted = false,
            isHistoryMigrated = false
        )

        repository.insertItem(legacyLoan)

        // Executar migração de histórico legado
        repository.ensureLegacyDataMigrated()

        // Verificar que foram gerados 2 pagamentos estruturados independentes
        val payments = repository.getPaymentsForLoanSync(1)
        assertEquals("Devem ser gerados exatamente 2 pagamentos para as 2 linhas", 2, payments.size)

        // IDs devem ser distintos e estáveis
        assertNotEquals("IDs dos pagamentos devem ser distintos", payments[0].id, payments[1].id)
        assertEquals(10000L, payments[0].amountCents)
        assertEquals(10000L, payments[1].amountCents)
        assertEquals("Parcela em dinheiro", payments[0].note)
        assertEquals("Parcela em dinheiro", payments[1].note)

        // O principal deve ser corrigido para 50000L e o marcador isHistoryMigrated = true
        val reloadedLoan = repository.getItemById(1)!!
        assertEquals(50000L, reloadedLoan.amountCents)
        assertTrue("isHistoryMigrated deve ser true após migração", reloadedLoan.isHistoryMigrated)
        assertEquals(legacyDescription, reloadedLoan.description)
    }

    @Test
    fun testLoanLifecyclePrincipal500Paid200Balance300Pay100Leaves200EstornoLeaves300() = runBlocking {
        // 1. Criar empréstimo com principal 500 (50000L) e já pago 200 (20000L), saldo 300 (30000L)
        val loan = FinanceItem(
            id = 42,
            title = "Empréstimo Negocial",
            type = "LENT",
            amountCents = 50000L, // Fonte única do principal: R$ 500,00
            date = 1000L,
            isCompleted = false,
            isHistoryMigrated = true
        )
        repository.insertItem(loan)

        // Registrar primeiro pagamento de R$ 200,00
        val p1Success = repository.addLoanPaymentAtomic(loanId = 42, amountCents = 20000L, paymentDate = 2000L, note = "P1 R$ 200")
        assertTrue(p1Success)

        // Validar estado inicial: principal 500, já pago 200, saldo 300
        val payments1 = repository.getPaymentsForLoanSync(42)
        val loanAfterP1 = repository.getItemById(42)!!
        assertEquals(50000L, loanAfterP1.amountCents) // Principal inalterado
        assertEquals(20000L, payments1.sumOf { it.amountCents }) // Já pago 200
        val balance1 = loanAfterP1.amountCents - payments1.sumOf { it.amountCents }
        assertEquals("Saldo deve ser 300 (30000 centavos)", 30000L, balance1)
        assertFalse(loanAfterP1.isCompleted)

        // 2. Pagar 100 (10000L) -> deixa 200 (20000L)
        val p2Success = repository.addLoanPaymentAtomic(loanId = 42, amountCents = 10000L, paymentDate = 3000L, note = "P2 R$ 100")
        assertTrue(p2Success)

        val payments2 = repository.getPaymentsForLoanSync(42)
        val loanAfterP2 = repository.getItemById(42)!!
        assertEquals(50000L, loanAfterP2.amountCents)
        assertEquals(30000L, payments2.sumOf { it.amountCents })
        val balance2 = loanAfterP2.amountCents - payments2.sumOf { it.amountCents }
        assertEquals("Após pagar 100, saldo restante deve ser 200 (20000 centavos)", 20000L, balance2)
        assertFalse(loanAfterP2.isCompleted)

        // 3. Estornar o pagamento de 100 -> deixa 300 (30000L)
        val payment100 = payments2.last()
        val estornoSuccess = repository.deleteLoanPaymentAtomic(payment100.id)
        assertTrue(estornoSuccess)

        val payments3 = repository.getPaymentsForLoanSync(42)
        val loanAfterEstorno = repository.getItemById(42)!!
        assertEquals(50000L, loanAfterEstorno.amountCents) // Principal preservado
        assertEquals(20000L, payments3.sumOf { it.amountCents }) // Total pago voltou a 200
        val balance3 = loanAfterEstorno.amountCents - payments3.sumOf { it.amountCents }
        assertEquals("Após estorno de 100, saldo restante deve voltar a 300 (30000 centavos)", 30000L, balance3)
        assertFalse(loanAfterEstorno.isCompleted)
    }

    @Test
    fun testPersistentMarkerPreventsRemigrationAfterEstornoTotal() = runBlocking {
        // Empréstimo legado inicial com histórico textual
        val legacyDescription = "Abatido R$ 150,00 em 01/06/2026 - Parcela única"
        val loan = FinanceItem(
            id = 77,
            title = "Empréstimo Teste Remigração",
            type = "LENT",
            amountCents = 15000L, // Saldo restante legado 150
            description = legacyDescription,
            isHistoryMigrated = false
        )
        repository.insertItem(loan)

        // Migração inicial
        repository.ensureLegacyDataMigrated()
        val payments = repository.getPaymentsForLoanSync(77)
        assertEquals(1, payments.size)
        assertEquals(15000L, payments[0].amountCents)

        val migratedLoan = repository.getItemById(77)!!
        assertTrue("isHistoryMigrated deve estar marcado como true", migratedLoan.isHistoryMigrated)

        // Usuário estorna todos os pagamentos
        repository.deleteLoanPaymentAtomic(payments[0].id)
        val paymentsAfterDelete = repository.getPaymentsForLoanSync(77)
        assertEquals(0, paymentsAfterDelete.size)

        // Executar ensureLegacyDataMigrated() novamente
        repository.ensureLegacyDataMigrated()

        // Como isHistoryMigrated é true, NENHUM pagamento deve ser recriado!
        val paymentsAfterSecondMigration = repository.getPaymentsForLoanSync(77)
        assertEquals("Não deve recriar pagamentos do texto após estorno devido ao marcador persistente", 0, paymentsAfterSecondMigration.size)
    }

    @Test
    fun testSettleAndReopenLoanRules() = runBlocking {
        val loan = FinanceItem(
            id = 10,
            title = "Empréstimo Colega",
            type = "LENT",
            amountCents = 40000L, // R$ 400,00 principal
            date = 1000L,
            isCompleted = false
        )
        repository.insertItem(loan)

        // 1. Quitar integralmente
        val settleSuccess = repository.settleLoanAtomic(loanId = 10, paymentDate = 2000L, note = "Quitação")
        assertTrue(settleSuccess)
        val settledLoan = repository.getItemById(10)!!
        assertTrue("Empréstimo quitado deve ter isCompleted = true", settledLoan.isCompleted)

        val paymentsAfterSettle = repository.getPaymentsForLoanSync(10)
        assertEquals(1, paymentsAfterSettle.size)
        assertEquals(40000L, paymentsAfterSettle[0].amountCents)

        // 2. Reabrir estornando quitação
        val reopenSuccess = repository.reopenLoanAtomic(loanId = 10, removeLastPayment = true)
        assertTrue(reopenSuccess)
        val reopenedLoan = repository.getItemById(10)!!
        assertFalse("Empréstimo reaberto deve ter isCompleted = false", reopenedLoan.isCompleted)
        assertNull("Empréstimo reaberto deve zerar data de quitação", reopenedLoan.paymentDate)

        val paymentsAfterReopen = repository.getPaymentsForLoanSync(10)
        assertEquals("Pagamento de quitação deve ser removido no estorno", 0, paymentsAfterReopen.size)
    }

    @Test
    fun testPaymentExceedingBalanceIsRejectedAtomically() = runBlocking {
        val loan = FinanceItem(
            id = 20,
            title = "Empréstimo Restrito",
            type = "LENT",
            amountCents = 10000L // R$ 100,00 principal
        )
        repository.insertItem(loan)

        // Tentar pagar 150,00 quando o principal é 100,00
        val result = repository.addLoanPaymentAtomic(loanId = 20, amountCents = 15000L, paymentDate = 1000L, note = "Excesso")
        assertFalse("Pagamento concorrente ou excedente ao saldo deve ser rejeitado", result)

        val payments = repository.getPaymentsForLoanSync(20)
        assertEquals(0, payments.size)
    }
}
