package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
        MigrationTracker.resetForTests(context)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(db.financeDao(), context)
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

    @Test
    fun testV2MigrationPreservesBalanceAndSetsSinglePrincipal() = runBlocking {
        val v2Loan = FinanceItem(
            id = 99,
            title = "Empréstimo v2",
            type = "LENT",
            category = "Empréstimo",
            amountCents = 30000L, // Saldo no formato v2: 300
            targetAmountCents = 50000L, // Principal original no formato v2: 500
            date = 1000L,
            description = "Empréstimo contratado na versão 2",
            isCompleted = false,
            isHistoryMigrated = false
        )
        repository.insertItem(v2Loan)

        val v2Payment = LoanPayment(
            id = 501L,
            loanId = 99,
            amountCents = 20000L, // Pagamento de R$ 200,00 existente na v2
            paymentDate = 2000L,
            note = "Pagamento parcela v2",
            createdAt = 2000L
        )
        db.financeDao().insertLoanPayment(v2Payment)

        // Executar migração
        repository.ensureLegacyDataMigrated()

        // Verificar o item migrado
        val migratedLoan = repository.getItemById(99)!!
        assertTrue(migratedLoan.isHistoryMigrated)
        // amountCents agora deve ser a fonte única do principal = 500 (50000L)
        assertEquals(50000L, migratedLoan.amountCents)
        assertEquals(50000L, migratedLoan.targetAmountCents)

        // Verificar que o saldo remanescente calculado continua exatamente 300 (30000L)
        val payments = repository.getPaymentsForLoanSync(99)
        val remainingBalance = com.example.ui.utils.LoanCalculator.calculateRemainingBalanceCents(migratedLoan, payments)
        assertEquals(30000L, remainingBalance)
    }

    @Test
    fun testUpdateLoanDetailsAtomicPreservesSinglePrincipalAndRejectsInvalid() = runBlocking {
        val loan = FinanceItem(
            id = 15,
            title = "Empréstimo Original",
            type = "LENT",
            amountCents = 50000L, // Principal 500
            date = 1000L
        )
        repository.insertItem(loan)
        repository.addLoanPaymentAtomic(15, 20000L, 2000L, "Pago 200")

        // 1. Atualizar título, data e principal para 600 (60000L)
        val updateOk = repository.updateLoanDetailsAtomic(15, "Novo Título", 60000L, 3000L)
        assertTrue(updateOk)

        val updated = repository.getItemById(15)!!
        assertEquals("Novo Título", updated.title)
        assertEquals(60000L, updated.amountCents)
        assertEquals(3000L, updated.date)

        val payments = repository.getPaymentsForLoanSync(15)
        // Saldo = 600 - 200 = 400
        val remaining = com.example.ui.utils.LoanCalculator.calculateRemainingBalanceCents(updated, payments)
        assertEquals(40000L, remaining)

        // 2. Tentar reduzir principal para abaixo do total já amortizado (ex: 150 < 200) -> deve rejeitar
        val rejectOk = repository.updateLoanDetailsAtomic(15, "Invalido", 15000L, 4000L)
        assertFalse("Não deve permitir reduzir principal abaixo do total pago", rejectOk)
        val unchanged = repository.getItemById(15)!!
        assertEquals(60000L, unchanged.amountCents)
    }

    @Test
    fun testConcurrentRecurringBillOccurrenceGenerationIsIdempotent() = runBlocking {
        val bill = RecurringBill(
            id = 10,
            title = "Conta de Água",
            category = "Casa",
            amountCents = 8500L,
            dueDay = 15,
            startDate = 1000L,
            isActive = true
        )
        repository.insertRecurringBill(bill)

        val item1 = com.example.ui.utils.RecurringBillManager.buildOccurrenceItem(bill, 2026, 10)
        val item2 = com.example.ui.utils.RecurringBillManager.buildOccurrenceItem(bill, 2026, 10)

        // Dois fluxos concorrentes tentam gerar a mesma ocorrência
        val deferred1 = async(kotlinx.coroutines.Dispatchers.IO) {
            repository.generateRecurringBillOccurrenceAtomic(item1)
        }
        val deferred2 = async(kotlinx.coroutines.Dispatchers.IO) {
            repository.generateRecurringBillOccurrenceAtomic(item2)
        }

        val res1 = deferred1.await()
        val res2 = deferred2.await()

        // Exatamente um deve retornar true, o outro false
        assertTrue("Exatamente uma geração concorrente deve ter sucesso", res1 != res2)

        val allItems = repository.getAllItemsSync().filter { it.recurringBillId == 10L && it.competence == "2026-10" }
        assertEquals("Deve existir apenas 1 registro no banco de dados", 1, allItems.size)
        assertEquals(8500L, allItems[0].amountCents)
    }

    @Test
    fun testEditingCallbackPreservesSinglePrincipalWithoutStaleCopy() = runBlocking {
        val loan = FinanceItem(
            id = 55,
            title = "Empréstimo Amigo",
            type = "LENT",
            amountCents = 50000L, // Principal 500
            date = 1000L
        )
        repository.insertItem(loan)
        repository.addLoanPaymentAtomic(55, 20000L, 2000L, "Pago 200") // Pago 200, saldo 300

        // Callback real de edição chamado pela LentAndBillsScreen:
        // Atualiza título para "Amigo Atualizado", principal para 700 (70000L), data 3000L
        var callbackSuccess = false
        var callbackError: String? = null

        val onUpdateLoan: (Int, String, Long, Long, () -> Unit, (String) -> Unit) -> Unit =
            { loanId, title, principalCents, date, onSuccess, onError ->
                runBlocking {
                    val ok = repository.updateLoanDetailsAtomic(loanId, title, principalCents, date)
                    if (ok) onSuccess() else onError("Valor menor que amortizado")
                }
            }

        // 1. Edição válida com sucesso
        onUpdateLoan(55, "Amigo Atualizado", 70000L, 3000L, {
            callbackSuccess = true
        }, { callbackError = it })

        assertTrue("Callback onSuccess deve ser invocado", callbackSuccess)
        assertNull(callbackError)

        val reloaded = repository.getItemById(55)!!
        assertEquals("Amigo Atualizado", reloaded.title)
        assertEquals(70000L, reloaded.amountCents) // Fonte única do principal preservada como 700
        assertEquals(70000L, reloaded.targetAmountCents)
        assertEquals(3000L, reloaded.date)

        val payments = repository.getPaymentsForLoanSync(55)
        val balance = com.example.ui.utils.LoanCalculator.calculateRemainingBalanceCents(reloaded, payments)
        assertEquals(50000L, balance) // 700 - 200 = 500 de saldo restante

        // 2. Edição com principal abaixo do total já pago -> dispara onError
        var failCallback = false
        var failMessage: String? = null
        onUpdateLoan(55, "Valor Menor", 10000L, 4000L, {
            failCallback = true
        }, { failMessage = it })

        assertFalse(failCallback)
        assertEquals("Valor menor que amortizado", failMessage)
    }

    @Test
    fun testExactRepairExecutedOnlyOnce() = runBlocking {
        MigrationTracker.resetForTests(context)
        // Cenário exato: saldo antigo 300 (amountCents = 30000L), principal 500 (targetAmountCents = 50000L),
        // pagamento 200 (payment = 20000L), com marcador isHistoryMigrated = true
        val affectedLoan = FinanceItem(
            id = 77,
            title = "Empréstimo Afetado por Versão Anterior",
            type = "LENT",
            category = "Empréstimo",
            amountCents = 30000L,
            targetAmountCents = 50000L,
            date = 1000L,
            description = "Empréstimo com marcador já true",
            isCompleted = false,
            isHistoryMigrated = true
        )
        repository.insertItem(affectedLoan)

        val payment = LoanPayment(
            id = 701L,
            loanId = 77,
            amountCents = 20000L,
            paymentDate = 2000L,
            note = "Amortização de R$ 200,00",
            createdAt = 2000L
        )
        db.financeDao().insertLoanPayment(payment)

        // 1. Primeira execução: executa reparo e converte principal para 500
        val countFirstRun = repository.repairV2MigratedLoansAtomic()
        assertEquals("Primeira execução deve reparar exatamente 1 empréstimo", 1, countFirstRun)

        val repaired = repository.getItemById(77)!!
        assertTrue(repaired.isHistoryMigrated)
        assertEquals(50000L, repaired.amountCents)
        assertEquals(50000L, repaired.targetAmountCents)

        val payments = repository.getPaymentsForLoanSync(77)
        assertEquals(1, payments.size)
        assertEquals(20000L, payments[0].amountCents)
        val remaining = com.example.ui.utils.LoanCalculator.calculateRemainingBalanceCents(repaired, payments)
        assertEquals(30000L, remaining)

        // 2. Segunda execução: marcador persistente de versão impede reexecução
        val countSecondRun = repository.repairV2MigratedLoansAtomic()
        assertEquals("Segunda execução não deve reexecutar reparo (marcador persistente já registrado)", 0, countSecondRun)

        // 3. Execução via ensureLegacyDataMigrated: também não deve reexecutar
        repository.ensureLegacyDataMigrated()
        val afterEnsure = repository.getItemById(77)!!
        assertEquals(50000L, afterEnsure.amountCents)
        val balanceAfterEnsure = com.example.ui.utils.LoanCalculator.calculateRemainingBalanceCents(afterEnsure, repository.getPaymentsForLoanSync(77))
        assertEquals(30000L, balanceAfterEnsure)
    }

    @Test
    fun testAmbiguousCasePreservesFinancialFieldsAndRegistersPendingReview() = runBlocking {
        MigrationTracker.resetForTests(context)
        // Cenário ambíguo: principal registrado 500, saldo guardado 300, mas pagamento é 100 (100 != 500 - 300)
        val ambiguousLoan = FinanceItem(
            id = 88,
            title = "Empréstimo com Divergência",
            type = "LENT",
            category = "Empréstimo",
            amountCents = 30000L,
            targetAmountCents = 50000L,
            date = 1000L,
            description = "Nota original sem tag de auditoria",
            isCompleted = false,
            isHistoryMigrated = true
        )
        repository.insertItem(ambiguousLoan)

        val payment = LoanPayment(
            id = 801L,
            loanId = 88,
            amountCents = 10000L,
            paymentDate = 2000L,
            note = "Parcela única",
            createdAt = 2000L
        )
        db.financeDao().insertLoanPayment(payment)

        // Executar reparo
        repository.repairV2MigratedLoansAtomic()

        val reloaded = repository.getItemById(88)!!
        // NÃO aumentar principal para total pago nem escolher targetAmountCents silenciosamente:
        // Campos financeiros DEVEM SER PRESERVADOS!
        assertEquals("amountCents original deve ser estritamente preservado em caso ambíguo", 30000L, reloaded.amountCents)
        assertEquals("targetAmountCents original deve ser estritamente preservado em caso ambíguo", 50000L, reloaded.targetAmountCents)

        // Registra pendência de revisão na descrição
        assertTrue("Deve registrar pendência de revisão na descrição", reloaded.description.contains("[Reparo Migração: pendência de revisão - valores divergentes preservados]"))

        // Pagamentos existentes continuam preservados
        val payments = repository.getPaymentsForLoanSync(88)
        assertEquals(1, payments.size)
        assertEquals(10000L, payments[0].amountCents)
    }

    @Test
    fun testReversedPaymentsAreNotRecreated() = runBlocking {
        MigrationTracker.resetForTests(context)
        // Empréstimo com pagamentos estornados: descrição contém abatimentos textuais, mas tabela loan_payments vazia
        val reversedLoan = FinanceItem(
            id = 89,
            title = "Empréstimo com Estorno",
            type = "LENT",
            category = "Empréstimo",
            amountCents = 50000L,
            targetAmountCents = 50000L,
            date = 1000L,
            description = "Abatido R$ 200,00 em 01/01/2026 - Pagamento que foi estornado",
            isCompleted = false,
            isHistoryMigrated = true
        )
        repository.insertItem(reversedLoan)

        assertEquals(0, repository.getPaymentsForLoanSync(89).size)

        repository.ensureLegacyDataMigrated()

        // Verifica que estornos são estritamente preservados (nenhum pagamento recriado)
        val payments = repository.getPaymentsForLoanSync(89)
        assertEquals("Estornos não podem ser recriados no banco de dados", 0, payments.size)
    }
}
