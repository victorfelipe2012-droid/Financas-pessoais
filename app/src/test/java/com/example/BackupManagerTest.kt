package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.utils.MoneyUtils
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
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(db.financeDao())
        backupManager = BackupManager(context, repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testExportAndRestoreV2Backup() = runBlocking {
        // Inserir dados iniciais
        val item1 = FinanceItem(title = "Salário Empresa", amountCents = 500000L, type = "SALARY")
        val item2 = FinanceItem(title = "Empréstimo Carlos", amountCents = 100000L, type = "LENT")
        repository.insertAll(listOf(item1, item2))

        val file = File(context.cacheDir, "test_backup.bin")
        val password = "StrongPassword2026!"

        // Exportar
        val exportSuccess = backupManager.performBackup(password, file)
        assertTrue(exportSuccess)
        assertTrue(file.exists())

        // Limpar o banco
        repository.clearAll()
        assertEquals(0, repository.allItems.first().size)

        // Restaurar
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

        // Tentar restaurar com senha errada
        val restoreSuccess = backupManager.restoreBackup("WrongPassword", file)
        assertFalse(restoreSuccess)

        // Banco deve permanecer intacto
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
    fun testRestoreLegacyV1Backup() = runBlocking {
        // Formato legado v1: Lista de FinanceItem em JSON
        val legacyJson = """
            [
                {
                    "id": 1,
                    "title": "Conta Legada",
                    "amountCents": 8500,
                    "type": "BILL",
                    "category": "Geral",
                    "date": 1700000000000,
                    "description": "",
                    "isCompleted": true,
                    "targetAmountCents": 0
                }
            ]
        """.trimIndent()

        val password = "LegacyPassword123"
        val encrypted = CryptoHelper.encryptModern(legacyJson, password.toCharArray())
        val file = File(context.cacheDir, "legacy_backup.bin")
        file.writeText(encrypted)

        val restoreSuccess = backupManager.restoreBackup(password, file)
        assertTrue(restoreSuccess)

        val items = repository.allItems.first()
        assertEquals(1, items.size)
        assertEquals("Conta Legada", items[0].title)
        assertEquals(8500L, items[0].amountCents)
    }

    @Test
    fun testInvalidReferencesRejectedAndDatabasePreserved() = runBlocking {
        // Inserir item prévio
        val initialItem = FinanceItem(id = 50, title = "Item Protegido", amountCents = 10000L, type = "SALARY")
        repository.insertItem(initialItem)

        // Criar payload v2 inválido: pagamento apontando para empréstimo inexistente (ID 999)
        val invalidPayload = BackupPayloadV2(
            version = 2,
            items = listOf(FinanceItem(id = 1, title = "Outro Item", amountCents = 2000L, type = "BILL")),
            loanPayments = listOf(LoanPayment(id = 1, loanId = 999, amountCents = 500L, paymentDate = 1000L, note = "Orfão"))
        )

        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val json = moshi.adapter(BackupPayloadV2::class.java).toJson(invalidPayload)

        val password = "TestPassword"
        val encrypted = CryptoHelper.encryptModern(json, password.toCharArray())
        val file = File(context.cacheDir, "invalid_ref_backup.bin")
        file.writeText(encrypted)

        // Restauração deve ser rejeitada pela validação estrutural
        val restoreSuccess = backupManager.restoreBackup(password, file)
        assertFalse("Backup com referências quebradas deve ser rejeitado", restoreSuccess)

        // O banco de dados NÃO pode ter sido alterado
        val currentItems = repository.allItems.first()
        assertEquals(1, currentItems.size)
        assertEquals("Item Protegido", currentItems[0].title)
    }
}
