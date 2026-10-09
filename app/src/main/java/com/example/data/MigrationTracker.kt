package com.example.data

import android.content.Context

/**
 * Rastreador persistente de versão de migrações e reparos do PrivaFin.
 * Utiliza SharedPreferences para garantir que migrações versionadas rodem apenas uma vez,
 * mesmo após reinicializações do app.
 */
object MigrationTracker {
    const val CURRENT_LOAN_REPAIR_VERSION = 1
    private const val PREFS_NAME = "privafin_migration_prefs"
    private const val KEY_LOAN_REPAIR_VERSION = "loan_repair_migration_version"

    @Volatile
    private var inMemoryVersion: Int = 0

    fun getLoanRepairVersion(context: Context?): Int {
        if (context == null) return inMemoryVersion
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getInt(KEY_LOAN_REPAIR_VERSION, inMemoryVersion)
        } catch (_: Exception) {
            inMemoryVersion
        }
    }

    fun setLoanRepairVersion(context: Context?, version: Int) {
        inMemoryVersion = version
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putInt(KEY_LOAN_REPAIR_VERSION, version).commit()
            } catch (_: Exception) {
            }
        }
    }

    fun resetForTests(context: Context? = null) {
        inMemoryVersion = 0
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().remove(KEY_LOAN_REPAIR_VERSION).commit()
            } catch (_: Exception) {
            }
        }
    }
}
