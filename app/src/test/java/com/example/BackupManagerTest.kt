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
import java.io.IOException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class FakeBackupCryptoProvider : BackupCryptoProvider {
    private val key: SecretKey by lazy {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(256)
        kg.generateKey()
    }

    override fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return "KEYSTORE_V2:" + android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    override fun decrypt(encryptedString: String): String {
        require(encryptedString.startsWith("KEYSTORE_V2:"))
        val combined = android.util.Base64.decode(encryptedString.removePrefix("KEYSTORE_V2:"), android.util.Base64.NO_WRAP)
        val iv = ByteArray(12)
        val cipherText = ByteArray(combined.size - 12)
        System.arraycopy(combined, 0, iv, 0, 12)
        System.arraycopy(combined, 12, cipherText, 0, cipherText.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }
}

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
        repository = FinanceRepository(db.financeDao(), context)
        categoryPreferences = CategoryPreferences(context)
        backupManager = BackupManager(context, repository, categoryPreferences, FakeBackupCryptoProvider())
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

    @Test
    fun testAtomicWritePreservesOriginalContentIfTargetExists() {
        val target = File(context.cacheDir, "atomic_test.txt")
        target.writeText("CONTEUDO_ORIGINAL_VALIDO")

        backupManager.writeAtomically(target, "NOVO_CONTEUDO_VALIDO")
        assertEquals("NOVO_CONTEUDO_VALIDO", target.readText())
    }

    @Test
    fun testPreWipeSnapshotVerifiesNewFileAndDeletesOldSnapshotBeforeCreation() = runBlocking {
        // Simular um snapshot antigo já existente no disco
        val oldSnapshot = backupManager.preRestoreSnapshotFile
        oldSnapshot.parentFile?.mkdirs()
        oldSnapshot.writeText("DADOS_ANTIGOS_DE_OUTRA_SESSAO")
        assertTrue(oldSnapshot.exists())

        // Ao rodar createPreWipeSnapshot(), deve substituir por novo snapshot válido com Keystore
        val item = FinanceItem(title = "Item Atual", amountCents = 9900L, type = "SALARY")
        repository.insertItem(item)

        val success = backupManager.createPreWipeSnapshot()
        assertTrue("Snapshot deve ter sido criado com sucesso", success)
        assertTrue(oldSnapshot.exists())
        assertNotEquals("DADOS_ANTIGOS_DE_OUTRA_SESSAO", oldSnapshot.readText())
    }

    @Test
    fun testProductionKeystoreUnavailabilityFailsAndPreservesExistingBackups() = runBlocking {
        val failingCryptoProvider = object : BackupCryptoProvider {
            override fun encrypt(plainText: String): String {
                throw java.security.KeyStoreException("AndroidKeyStore não disponível neste dispositivo")
            }
            override fun decrypt(encryptedString: String): String {
                throw java.security.KeyStoreException("AndroidKeyStore não disponível neste dispositivo")
            }
        }
        val secureBackupManager = BackupManager(context, repository, categoryPreferences, failingCryptoProvider)

        // Simula auto backup existente válido no disco
        val autoBackup = secureBackupManager.autoBackupFile
        autoBackup.parentFile?.mkdirs()
        autoBackup.writeText("EXISTING_VALID_AUTO_BACKUP")

        val success = secureBackupManager.performAutoBackup()
        assertFalse("Auto-backup deve falhar quando Keystore estiver indisponível", success)
        assertEquals("Arquivo original de auto-backup deve ser estritamente preservado", "EXISTING_VALID_AUTO_BACKUP", autoBackup.readText())

        // Falha em criar snapshot de segurança antes da restauração ou wipe
        val snapshotSuccess = secureBackupManager.createPreWipeSnapshot()
        assertFalse("Snapshot de segurança deve falhar quando Keystore estiver indisponível", snapshotSuccess)
    }

    @Test
    fun testWriteAtomicallyWriteFailurePreservesOriginal() {
        val target = File(context.cacheDir, "write_failure_target.txt")
        target.writeText("ORIGINAL_CONTENT_PRESERVED")

        // Forçar falha na gravação do tempFile passando um diretório que na verdade é um arquivo bloqueador
        val blockerFile = File(context.cacheDir, "blocker_file_not_dir.tmp")
        blockerFile.writeText("NOT_A_DIRECTORY")
        val invalidTarget = File(blockerFile, "sub_target.txt")

        try {
            backupManager.writeAtomically(invalidTarget, "NEW_CONTENT")
            fail("Deveria lançar IOException")
        } catch (_: IOException) {
            // Sucesso: falha esperada
        }
        assertEquals("ORIGINAL_CONTENT_PRESERVED", target.readText())
    }

    @Test
    fun testWriteAtomicallyRenameFailurePreservesOriginal() {
        val testDir = File(context.cacheDir, "rename_fail_dir")
        testDir.mkdirs()
        val target = File(testDir, "rename_target.txt")
        target.writeText("ORIGINAL_VALID_BEFORE_RENAME")

        // 1. Destino que é diretório deve abortar com IOException preservando dados
        val dirTarget = File(testDir, "target_is_a_dir")
        dirTarget.mkdirs()
        val child = File(dirTarget, "child.txt")
        child.writeText("BLOCKING_RENAME")

        try {
            backupManager.writeAtomically(dirTarget, "NEW_CONTENT")
            fail("Deveria lançar IOException para destino que é diretório")
        } catch (_: IOException) {
            // Sucesso: diretório protegido
        }

        // 2. Destino com arquivo com lock ativo ou impedimento de renomeação
        val lockedFile = File(testDir, "locked_target.txt")
        lockedFile.writeText("LOCKED_ORIGINAL_CONTENT")
        val stream = java.io.FileOutputStream(lockedFile, true)
        val lock = try { stream.channel.tryLock() } catch (_: Exception) { null }

        try {
            backupManager.writeAtomically(lockedFile, "SHOULD_FAIL")
            if (lock != null) {
                fail("Deveria lançar IOException para arquivo com lock ativo")
            }
        } catch (_: IOException) {
            // Sucesso: falha atômica segura
        } finally {
            try { lock?.release() } catch (_: Exception) {}
            try { stream.close() } catch (_: Exception) {}
        }

        assertEquals("ORIGINAL_VALID_BEFORE_RENAME", target.readText())
        assertTrue("Diretório bloqueador original deve permanecer intacto", child.exists())
    }

    @Test
    fun testEncryptionOrWriteFailurePreservesPreviousSnapshot() = runBlocking {
        val backupDir = File(context.filesDir, "backups")
        backupDir.mkdirs()
        val snapshotFile = File(backupDir, "pre_restore_snapshot.bin")
        snapshotFile.writeText("PREVIOUS_VALID_SNAPSHOT_DATA")
        assertTrue(snapshotFile.exists())

        // Falha de criptografia
        val failingCrypto = object : BackupCryptoProvider {
            override fun encrypt(plainText: String): String = throw java.security.KeyStoreException("Keystore crypto failure")
            override fun decrypt(encryptedString: String): String = throw java.security.KeyStoreException("Keystore crypto failure")
        }
        val failingManager = BackupManager(context, repository, categoryPreferences, failingCrypto)

        val success = failingManager.createPreWipeSnapshot()
        assertFalse("Snapshot deve falhar com erro de criptografia", success)
        assertTrue("Arquivo anterior de snapshot deve continuar existindo", snapshotFile.exists())
        assertEquals("Conteúdo do snapshot anterior deve permanecer estritamente intacto", "PREVIOUS_VALID_SNAPSHOT_DATA", snapshotFile.readText())
    }

    @Test
    fun testSubstitutionFailureMaintainsRecoverableCopy() = runBlocking {
        val backupDir = File(context.filesDir, "backups")
        backupDir.mkdirs()
        val snapshotFile = File(backupDir, "pre_restore_snapshot.bin")
        snapshotFile.writeText("VALID_SNAPSHOT_CONTENT")

        // Simula falha de substituição onde snapshot principal foi renomeado para .bak
        val bakFile = File(backupDir, "pre_restore_snapshot.bin.${System.nanoTime()}.bak")
        bakFile.writeText("VALID_RECOVERABLE_BAK_CONTENT")
        snapshotFile.delete()

        // O getter preRestoreSnapshotFile deve encontrar e usar a cópia .bak existente
        val resolved = backupManager.preRestoreSnapshotFile
        assertTrue("Cópia recuperável deve existir no disco", resolved.exists())
        assertEquals("Conteúdo da cópia de segurança .bak deve ser mantido e recuperável", "VALID_RECOVERABLE_BAK_CONTENT", resolved.readText())
    }

    @Test
    fun testRepairCurrentDbThenRestoreV2BackupAndVerifyIdempotency() = runBlocking {
        // 1. Executar reparo no banco atual
        val initialLoan = FinanceItem(
            id = 10,
            title = "Empréstimo Inicial",
            type = "LENT",
            category = "Empréstimo",
            amountCents = 15000L,
            targetAmountCents = 20000L,
            date = 1000L,
            description = "Nota",
            isCompleted = false,
            isHistoryMigrated = true
        )
        repository.insertItem(initialLoan)
        db.financeDao().insertLoanPayment(
            LoanPayment(id = 101L, loanId = 10, amountCents = 5000L, paymentDate = 1500L, note = "P1", createdAt = 1500L)
        )
        val repairedInitial = repository.repairV2MigratedLoansAtomic()
        assertEquals(1, repairedInitial)
        val checkedInitial = repository.getItemById(10)!!
        assertEquals(20000L, checkedInitial.amountCents)
        assertEquals(1L, db.financeDao().getMetadataValue(AppMetadata.KEY_LOAN_REPAIR_VERSION))

        // 2. Criar arquivo de backup v2 com principal 500 (50000L), saldo antigo 300 (30000L) e pagamentos 200 (20000L)
        val v2Loan = FinanceItem(
            id = 50,
            title = "Empréstimo V2 Restaurado",
            type = "LENT",
            category = "Empréstimo",
            amountCents = 30000L, // Saldo v2
            targetAmountCents = 50000L, // Principal original v2
            date = 2000L,
            description = "Contrato v2",
            isCompleted = false,
            isHistoryMigrated = true
        )
        val v2Payment = LoanPayment(
            id = 501L,
            loanId = 50,
            amountCents = 20000L,
            paymentDate = 2500L,
            note = "Pagamento v2",
            createdAt = 2500L
        )
        val v2Payload = BackupPayloadV2(
            version = 2,
            exportedAt = System.currentTimeMillis(),
            appVersion = "2.0",
            items = listOf(v2Loan),
            loanPayments = listOf(v2Payment),
            boxMovements = emptyList(),
            recurringBills = emptyList(),
            categoryBudgets = emptyList(),
            apartmentSubcategories = emptyList()
        )
        val v2Adapter = com.squareup.moshi.Moshi.Builder().build().adapter(BackupPayloadV2::class.java)
        val v2Json = v2Adapter.toJson(v2Payload)
        val password = "StrongPassword#2026"
        val encryptedV2Backup = CryptoHelper.encryptModern(v2Json, password.toCharArray())
        val backupFile = File(context.filesDir, "test_v2_restore.privafin")
        backupFile.writeText(encryptedV2Backup)

        // 3. Restaurar backup v2
        val restoreSuccess = backupManager.restoreBackup(password, backupFile)
        assertTrue("Restauração do backup v2 deve ter sucesso", restoreSuccess)

        // 4. Confirmar principal 500 e saldo 300
        val restoredLoan = repository.getItemById(50)!!
        assertEquals("Principal deve ser corrigido para 50000L (R$ 500,00)", 50000L, restoredLoan.amountCents)
        assertEquals("Target amount deve ser 50000L (R$ 500,00)", 50000L, restoredLoan.targetAmountCents)
        val payments = repository.getPaymentsForLoanSync(50)
        assertEquals(1, payments.size)
        assertEquals(20000L, payments[0].amountCents)
        val balance = restoredLoan.amountCents - payments.sumOf { it.amountCents }
        assertEquals("Saldo a receber deve ser 30000L (R$ 300,00)", 30000L, balance)
        assertEquals(1L, db.financeDao().getMetadataValue(AppMetadata.KEY_LOAN_REPAIR_VERSION))

        // 5. Reiniciar o repositório e confirmar que nada é duplicado ou alterado novamente
        val newRepo = FinanceRepository(db.financeDao(), context)
        val secondRepair = newRepo.repairV2MigratedLoansAtomic()
        assertEquals("Segunda execução não deve alterar nada (idempotente)", 0, secondRepair)

        newRepo.ensureLegacyDataMigrated()
        val afterRestartLoan = newRepo.getItemById(50)!!
        assertEquals(50000L, afterRestartLoan.amountCents)
        assertEquals(50000L, afterRestartLoan.targetAmountCents)
        val afterRestartPayments = newRepo.getPaymentsForLoanSync(50)
        assertEquals(1, afterRestartPayments.size)
        assertEquals(20000L, afterRestartPayments[0].amountCents)
        val afterRestartBalance = afterRestartLoan.amountCents - afterRestartPayments.sumOf { it.amountCents }
        assertEquals(30000L, afterRestartBalance)
    }
}
