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
        CategoryBudget::class
    ],
    version = 3,
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "privafin_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
