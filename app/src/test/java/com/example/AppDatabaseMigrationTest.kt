package com.example

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {

    @Test
    fun testMigration1To2PreservesDataAndRoundsToCents() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbFile = File(context.cacheDir, "test_migration_v1_v2.db")
        if (dbFile.exists()) dbFile.delete()

        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.name)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Esquema v1 legado do Room
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `finance_items` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `amount` REAL NOT NULL,
                            `type` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `date` INTEGER NOT NULL,
                            `description` TEXT NOT NULL,
                            `isCompleted` INTEGER NOT NULL,
                            `targetAmount` REAL NOT NULL,
                            `dueDate` INTEGER
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Inserir dados v1 realistas
        db.execSQL("""
            INSERT INTO `finance_items` (`id`, `title`, `amount`, `type`, `category`, `date`, `description`, `isCompleted`, `targetAmount`, `dueDate`)
            VALUES (101, 'Salário Outubro', 3450.75, 'SALARY', 'Renda', 1715000000000, 'Salário CLT', 1, 0.0, NULL)
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `finance_items` (`id`, `title`, `amount`, `type`, `category`, `date`, `description`, `isCompleted`, `targetAmount`, `dueDate`)
            VALUES (102, 'Empréstimo Pedro', 800.00, 'LENT', 'Empréstimo', 1715100000000, 'Abatido R$ 200,00', 0, 1000.00, 1716000000000)
        """.trimIndent())

        // Executar a migração MIGRATION_1_2
        AppDatabase.MIGRATION_1_2.migrate(db)

        // Verificar tabela finance_items v2 migrada
        val cursor = db.query("SELECT id, title, amountCents, targetAmountCents, isCompleted, paymentDate FROM finance_items ORDER BY id ASC")
        assertTrue(cursor.moveToFirst())

        // Item 101: 3450.75 -> 345075 centavos, isCompleted = 1 -> paymentDate preservado
        assertEquals(101, cursor.getInt(0))
        assertEquals("Salário Outubro", cursor.getString(1))
        assertEquals(345075L, cursor.getLong(2))
        assertEquals(0L, cursor.getLong(3))
        assertEquals(1, cursor.getInt(4))
        assertEquals(1715000000000L, cursor.getLong(5))

        // Item 102: 800.00 -> 80000 centavos, targetAmount 1000.00 -> 100000 centavos
        assertTrue(cursor.moveToNext())
        assertEquals(102, cursor.getInt(0))
        assertEquals("Empréstimo Pedro", cursor.getString(1))
        assertEquals(80000L, cursor.getLong(2))
        assertEquals(100000L, cursor.getLong(3))
        assertEquals(0, cursor.getInt(4))
        assertTrue(cursor.isNull(5))

        cursor.close()

        // Verificar que as novas tabelas foram criadas com sucesso
        val tablesCursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
        val tableNames = mutableListOf<String>()
        while (tablesCursor.moveToNext()) {
            tableNames.add(tablesCursor.getString(0))
        }
        tablesCursor.close()

        assertTrue("Tabela loan_payments deve existir", tableNames.contains("loan_payments"))
        assertTrue("Tabela box_movements deve existir", tableNames.contains("box_movements"))
        assertTrue("Tabela recurring_bills deve existir", tableNames.contains("recurring_bills"))
        assertTrue("Tabela category_budgets deve existir", tableNames.contains("category_budgets"))

        db.close()
    }

    @Test
    fun testMigration2To3AddsColumnsAndIndex() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbFile = File(context.cacheDir, "test_migration_v2_v3.db")
        if (dbFile.exists()) dbFile.delete()

        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.name)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Esquema v2 do Room
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `finance_items` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `amountCents` INTEGER NOT NULL,
                            `type` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `date` INTEGER NOT NULL,
                            `description` TEXT NOT NULL,
                            `isCompleted` INTEGER NOT NULL,
                            `targetAmountCents` INTEGER NOT NULL,
                            `dueDate` INTEGER,
                            `paymentDate` INTEGER
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        db.execSQL("""
            INSERT INTO `finance_items` (`id`, `title`, `amountCents`, `type`, `category`, `date`, `description`, `isCompleted`, `targetAmountCents`, `dueDate`, `paymentDate`)
            VALUES (201, 'Conta Internet', 12000, 'BILL', 'Casa', 1715000000000, 'Fibra', 0, 0, 1716000000000, NULL)
        """.trimIndent())

        // Executar MIGRATION_2_3
        AppDatabase.MIGRATION_2_3.migrate(db)

        val cursor = db.query("SELECT id, title, amountCents, recurringBillId, competence, isHistoryMigrated FROM finance_items WHERE id = 201")
        assertTrue(cursor.moveToFirst())
        assertEquals(201, cursor.getInt(0))
        assertEquals("Conta Internet", cursor.getString(1))
        assertEquals(12000L, cursor.getLong(2))
        assertTrue(cursor.isNull(3)) // recurringBillId deve ser null por padrão
        assertTrue(cursor.isNull(4)) // competence deve ser null por padrão
        assertEquals(0, cursor.getInt(5)) // isHistoryMigrated default 0
        cursor.close()

        // Verificar que o índice foi criado
        val indexCursor = db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_finance_items_recurringBillId_competence'")
        assertTrue("Índice index_finance_items_recurringBillId_competence deve existir", indexCursor.moveToFirst())
        indexCursor.close()

        db.close()
    }

    @Test
    fun testMigration3To4MigratesTagsResolvesDuplicatesAndCreatesUniqueIndex() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbFile = File(context.cacheDir, "test_migration_v3_v4.db")
        if (dbFile.exists()) dbFile.delete()

        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.name)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Esquema v3 do Room
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `finance_items` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `amountCents` INTEGER NOT NULL,
                            `type` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `date` INTEGER NOT NULL,
                            `description` TEXT NOT NULL,
                            `isCompleted` INTEGER NOT NULL,
                            `targetAmountCents` INTEGER NOT NULL,
                            `dueDate` INTEGER,
                            `paymentDate` INTEGER,
                            `recurringBillId` INTEGER,
                            `competence` TEXT,
                            `isHistoryMigrated` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_items_recurringBillId_competence` ON `finance_items`(`recurringBillId`, `competence`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // 1. Item com tag legada na descrição
        db.execSQL("""
            INSERT INTO `finance_items` (`id`, `title`, `amountCents`, `type`, `category`, `date`, `description`, `isCompleted`, `targetAmountCents`, `recurringBillId`, `competence`)
            VALUES (301, 'Conta Luz', 15000, 'BILL', 'Casa', 1715000000000, 'Nota [Recorrência #RecID_7_2026-10]', 0, 0, NULL, NULL)
        """.trimIndent())

        // 2. Item com mesmo vínculo estrutural já preenchido (duplicata de 301 para 7#2026-10)
        db.execSQL("""
            INSERT INTO `finance_items` (`id`, `title`, `amountCents`, `type`, `category`, `date`, `description`, `isCompleted`, `targetAmountCents`, `recurringBillId`, `competence`)
            VALUES (302, 'Conta Luz Duplicada', 15000, 'BILL', 'Casa', 1715000000000, 'Lançamento manual', 0, 0, 7, '2026-10')
        """.trimIndent())

        // 3. Item com vínculo diferente
        db.execSQL("""
            INSERT INTO `finance_items` (`id`, `title`, `amountCents`, `type`, `category`, `date`, `description`, `isCompleted`, `targetAmountCents`, `recurringBillId`, `competence`)
            VALUES (303, 'Internet', 10000, 'BILL', 'Casa', 1715000000000, 'Internet fibra', 0, 0, 8, '2026-10')
        """.trimIndent())

        // Executar MIGRATION_3_4
        AppDatabase.MIGRATION_3_4.migrate(db)

        // Verificar item 301: tag migrada para campos estruturados
        val c301 = db.query("SELECT recurringBillId, competence FROM finance_items WHERE id = 301")
        assertTrue(c301.moveToFirst())
        assertEquals(7L, c301.getLong(0))
        assertEquals("2026-10", c301.getString(1))
        c301.close()

        // Verificar item 302: duplicata desvinculada sem apagar lançamento
        val c302 = db.query("SELECT recurringBillId, competence FROM finance_items WHERE id = 302")
        assertTrue(c302.moveToFirst())
        assertTrue("Duplicata deve ter recurringBillId desvinculado (null)", c302.isNull(0))
        assertTrue("Duplicata deve ter competence desvinculada (null)", c302.isNull(1))
        c302.close()

        // Verificar item 303: preservado
        val c303 = db.query("SELECT recurringBillId, competence FROM finance_items WHERE id = 303")
        assertTrue(c303.moveToFirst())
        assertEquals(8L, c303.getLong(0))
        assertEquals("2026-10", c303.getString(1))
        c303.close()

        // Verificar que o índice agora é UNIQUE (inserir duplicata direta deve falhar com restrição UNIQUE)
        var uniqueConstraintTriggered = false
        try {
            db.execSQL("""
                INSERT INTO `finance_items` (`id`, `title`, `amountCents`, `type`, `category`, `date`, `description`, `isCompleted`, `targetAmountCents`, `recurringBillId`, `competence`)
                VALUES (304, 'Internet Clone', 10000, 'BILL', 'Casa', 1715000000000, 'Clone', 0, 0, 8, '2026-10')
            """.trimIndent())
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            uniqueConstraintTriggered = true
        }
        assertTrue("Índice deve ser UNIQUE e rejeitar chave duplicada", uniqueConstraintTriggered)

        db.close()
    }
}
