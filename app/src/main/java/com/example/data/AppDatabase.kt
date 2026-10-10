package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FinanceItem::class,
        LoanPayment::class,
        BoxMovement::class,
        RecurringBill::class,
        CategoryBudget::class,
        AppMetadata::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Criar nova tabela finance_items_v2 com amountCents, targetAmountCents e paymentDate
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `finance_items_v2` (
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

                // 2. Migrar dados existentes com conversão exata para centavos (arredondamento explícito)
                db.execSQL("""
                    INSERT INTO `finance_items_v2` (
                        `id`, `title`, `amountCents`, `type`, `category`, `date`, `description`, `isCompleted`, `targetAmountCents`, `dueDate`, `paymentDate`
                    )
                    SELECT 
                        `id`, 
                        `title`, 
                        CAST(ROUND(`amount` * 100.0) AS INTEGER), 
                        `type`, 
                        `category`, 
                        `date`, 
                        `description`, 
                        `isCompleted`, 
                        CAST(ROUND(`targetAmount` * 100.0) AS INTEGER), 
                        `dueDate`,
                        CASE WHEN `isCompleted` = 1 THEN `date` ELSE NULL END
                    FROM `finance_items`
                """.trimIndent())

                // 3. Substituir a tabela antiga pela nova
                db.execSQL("DROP TABLE `finance_items`")
                db.execSQL("ALTER TABLE `finance_items_v2` RENAME TO `finance_items`")

                // 4. Criar tabela de pagamentos/abatimentos estruturados de empréstimos
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `loan_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `loanId` INTEGER NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `paymentDate` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loan_payments_loanId` ON `loan_payments`(`loanId`)")

                // 5. Criar tabela de movimentações de caixinhas (aportes e resgates)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `box_movements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `boxId` INTEGER NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `isDeposit` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_box_movements_boxId` ON `box_movements`(`boxId`)")

                // 6. Criar tabela de contas recorrentes mensais
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_bills` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `dueDay` INTEGER NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                // 7. Criar tabela de orçamentos por categoria
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `category_budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `category` TEXT NOT NULL,
                        `limitCents` INTEGER NOT NULL,
                        `monthCompetence` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `finance_items` ADD COLUMN `recurringBillId` INTEGER")
                db.execSQL("ALTER TABLE `finance_items` ADD COLUMN `competence` TEXT")
                db.execSQL("ALTER TABLE `finance_items` ADD COLUMN `isHistoryMigrated` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_items_recurringBillId_competence` ON `finance_items`(`recurringBillId`, `competence`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Remover índice anterior não-único se existir
                db.execSQL("DROP INDEX IF EXISTS `index_finance_items_recurringBillId_competence`")

                // 2. Migrar vínculos de ocorrências antigas identificadas por tag textual na descrição
                val cursor = db.query("SELECT id, description, recurringBillId, competence FROM finance_items")
                val tagRegex = Regex("""\[Recorrência #RecID_(\d+)_(\d{4}-\d{2})\]""")
                try {
                    while (cursor.moveToNext()) {
                        val id = cursor.getInt(0)
                        val desc = cursor.getString(1) ?: ""
                        val currentBillId = if (cursor.isNull(2)) null else cursor.getLong(2)
                        val currentComp = if (cursor.isNull(3)) null else cursor.getString(3)

                        if (currentBillId == null || currentComp == null) {
                            val match = tagRegex.find(desc)
                            if (match != null) {
                                val billId = match.groupValues[1].toLong()
                                val comp = match.groupValues[2]
                                db.execSQL(
                                    "UPDATE finance_items SET recurringBillId = ?, competence = ? WHERE id = ?",
                                    arrayOf<Any>(billId, comp, id)
                                )
                            }
                        }
                    }
                } finally {
                    cursor.close()
                }

                // 3. Resolver duplicatas sem apagar lançamentos: desvincular duplicatas preservando os registros
                val dupCursor = db.query("""
                    SELECT id, recurringBillId, competence 
                    FROM finance_items 
                    WHERE recurringBillId IS NOT NULL AND competence IS NOT NULL 
                    ORDER BY id ASC
                """)
                val seenKeys = mutableSetOf<String>()
                try {
                    while (dupCursor.moveToNext()) {
                        val id = dupCursor.getInt(0)
                        val billId = dupCursor.getLong(1)
                        val comp = dupCursor.getString(2)
                        val key = "$billId#$comp"
                        if (seenKeys.contains(key)) {
                            // Duplicata encontrada: desvincula mantendo o lançamento intacto no banco
                            db.execSQL(
                                "UPDATE finance_items SET recurringBillId = NULL, competence = NULL WHERE id = ?",
                                arrayOf(id)
                            )
                        } else {
                            seenKeys.add(key)
                        }
                    }
                } finally {
                    dupCursor.close()
                }

                // 4. Criar o índice estritamente UNIQUE
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_finance_items_recurringBillId_competence` ON `finance_items`(`recurringBillId`, `competence`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `app_metadata` (
                        `key` TEXT NOT NULL,
                        `value` INTEGER NOT NULL,
                        PRIMARY KEY(`key`)
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "privafin_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
