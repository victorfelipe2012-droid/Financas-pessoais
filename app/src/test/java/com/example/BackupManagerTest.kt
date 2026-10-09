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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var backupManager: BackupManager
    private lateinit var categoryPreferences: CategoryPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(db.financeDao())
        categoryPreferences = CategoryPreferences(context)
        backupManager = BackupManager(context, repository, categoryPreferences)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testExportAndRestoreV2Backup() = runBlocking {
        val item1 = FinanceItem(title = "Salário Empresa", amountCents = 500000L, type = "SALARY")
        val item2 = FinanceItem(title = "Empréstimo Carlos", amountCents = 100000L, type = "LENT")
        repository.insertAll(listOf(item1, item2))

        val file = File(context.cacheDir, "test_backup.bin")
        val password = "StrongPassword2026!"

        val exportSuccess = backupManager.performBackup(password, file)
        assertTrue(exportSuccess)
        assertTrue(file.exists())

        repository.clearAllDataAtomic()
        assertEquals(0, repository.allItems.first().size)

        val restoreSuccess = backupManager.restoreBackup(password, file)
        assertTrue(restoreSuccess)

        val restoredItems = repository.allItems.first()
        assertEquals(2, restoredItems.size)
        assertTrue(restoredItems.any { it.title == "Salário Empresa" && it.amountCents == 500000L })
    }

    @Test
    fun testWrongPasswordDoesNotAlterDatabase() = runBlocking {
        val originalItem = FinanceItem(title = "Original Item", amountCents = 150000L, type = "SALARY")
        repository.insertItem(originalItem)

        val file = File(context.cacheDir, "test_backup_wrong_pwd.bin")
        val correctPassword = "CorrectPassword123"
        backupManager.performBackup(correctPassword, file)

        val restoreSuccess = backupManager.restoreBackup("WrongPassword", file)
        assertFalse(restoreSuccess)

        val items = repository.allItems.first()
        assertEquals(1, items.size)
        assertEquals("Original Item", items.first().title)
    }

    @Test
    fun testCorruptedFileRejection() = runBlocking {
        val file = File(context.cacheDir, "corrupted.bin")
        file.writeText("THIS IS NOT A VALID ENCRYPTED BACKUP")

        val restoreSuccess = backupManager.restoreBackup("AnyPassword", file)
        assertFalse(restoreSuccess)
    }

    @Test
    fun testRestoreLegacyBackupEncryptedWithOriginalAlgorithm() = runBlocking {
        // Formato legado original v1 em JSON puro com amount e targetAmount em reais (Doubles)
        val legacyJson = """
            [
                {
                    "id": 10,
                    "title": "Conta de Energia",
                    "amount": 150.75,
                    "type": "BILL",
                    "category": "Casa",
                    "date": 1715000000000,
                    "description": "Fatura CEMIG",
                    "isCompleted": true,
                    "targetAmount": 0.0
                },
                {
                    "id": 20,
                    "title": "Empréstimo Amigo",
                    "amount": 300.0,
                    "type": "LENT",
                    "category": "Empréstimos",
                    "date": 1715100000000,
                    "description": "Abatido R$ 100,00 em 15/05/2026 - Sinal\nAbatido R$ 100,00 em 20/05/2026 - Segunda",
                    "isCompleted": false,
                    "targetAmount": 500.0
                }
            ]
        """.trimIndent()

        val password = "LegacyPasswordOriginal123"
        // Criptografado estritamente com o algoritmo original: PBKDF2-HMAC-SHA1, 1000 iterações, salt PrivaFinSalt123#, AES-GCM com IV de 12 bytes prefixado
        val encryptedLegacy = CryptoHelper.encryptLegacy(legacyJson, password.toCharArray())
        assertFalse("Não deve ter cabeçalho PRIVAFIN2", encryptedLegacy.startsWith("PRIVAFIN2:"))

        val file = File(context.cacheDir, "legacy_original_backup.bin")
        file.writeText(encryptedLegacy)

        val restoreSuccess = backupManager.restoreBackup(password, file)
        assertTrue("Restauração do formato legado original deve ter sucesso", restoreSuccess)

        val restoredItems = repository.allItems.first()
        assertEquals(2, restoredItems.size)

        // Comprovar conversão de valores para centavos
        val item1 = restoredItems.first { it.id == 10 }
        assertEquals("Conta de Energia", item1.title)
        assertEquals(15075L, item1.amountCents)
        assertEquals(1715000000000L, item1.date)
        assertTrue(item1.isCompleted)

        val loan = restoredItems.first { it.id == 20 }
        assertEquals("Empréstimo Amigo", loan.title)
        // O principal deve ter sido preservado/ajustado com base nas parcelas textuais: 300 + 200 = 500 (50000L)
        assertEquals(50000L, loan.amountCents)
        assertTrue("isHistoryMigrated deve ser true", loan.isHistoryMigrated)

        // Comprovar restauração e extração do histórico em pagamentos
        val payments = repository.getPaymentsForLoanSync(20)
        assertEquals(2, payments.size)
        assertEquals(10000L, payments[0].amountCents)
        assertEquals(10000L, payments[1].amountCents)
    }

    @Test
    fun testRejectInvalidReferencesWhenItemsIsEmpty() = runBlocking {
        // Payload v2 com items vazio e pagamento apontando para empréstimo inexistente (ID 999)
        val invalidPayload = BackupPayloadV2(
            version = 2,
            items = emptyList(),
            loanPayments = listOf(LoanPayment(id = 1, loanId = 999, amountCents = 500L, paymentDate = 1000L, note = "Órfão"))
        )

        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val json = moshi.adapter(BackupPayloadV2::class.java).toJson(invalidPayload)

        val password = "TestPassword"
        val encrypted = CryptoHelper.encryptModern(json, password.toCharArray())
        val file = File(context.cacheDir, "invalid_empty_items.bin")
        file.writeText(encrypted)

        val restoreSuccess = backupManager.restoreBackup(password, file)
        assertFalse("Backup com referências quebradas mesmo com items vazio DEVE ser rejeitado", restoreSuccess)
    }

    @Test
    fun testRejectDuplicateIds() = runBlocking {
        // Itens com IDs duplicados
        val duplicatePayload = BackupPayloadV2(
            version = 2,
            items = listOf(
                FinanceItem(id = 1, title = "Item A", amountCents = 1000L, type = "BILL"),
                FinanceItem(id = 1, title = "Item B com mesmo ID", amountCents = 2000L, type = "BILL")
            )
        )

        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val json = moshi.adapter(BackupPayloadV2::class.java).toJson(duplicatePayload)

        val password = "TestPassword"
        val encrypted = CryptoHelper.encryptModern(json, password.toCharArray())
        val file = File(context.cacheDir, "duplicate_ids.bin")
        file.writeText(encrypted)

        val restoreSuccess = backupManager.restoreBackup(password, file)
        assertFalse("Backup com IDs duplicados deve ser rejeitado", restoreSuccess)
    }

    @Test
    fun testExactCategoryReplacementIncludingEmptyList() = runBlocking {
        categoryPreferences.setApartmentSubcategories(listOf("Internet", "Água", "Luz"))
        assertEquals(3, categoryPreferences.apartmentSubcategories.value.size)

        // Backup que contém lista vazia de subcategorias
        val emptyCatsPayload = BackupPayloadV2(
            version = 2,
            items = listOf(FinanceItem(id = 1, title = "Conta", amountCents = 1000L, type = "BILL")),
            apartmentSubcategories = emptyList()
        )

        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val json = moshi.adapter(BackupPayloadV2::class.java).toJson(emptyCatsPayload)

        val password = "PasswordCat"
        val encrypted = CryptoHelper.encryptModern(json, password.toCharArray())
        val file = File(context.cacheDir, "empty_cats.bin")
        file.writeText(encrypted)

        val restoreSuccess = backupManager.restoreBackup(password, file)
        assertTrue(restoreSuccess)

        // Deve substituir exatamente, ficando vazia
        assertEquals("Subcategorias devem ser substituídas exatamente pela lista vazia", 0, categoryPreferences.apartmentSubcategories.value.size)
    }
}
