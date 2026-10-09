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
}
