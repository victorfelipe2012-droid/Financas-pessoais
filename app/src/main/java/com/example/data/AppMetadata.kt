package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "app_metadata")
@JsonClass(generateAdapter = true)
data class AppMetadata(
    @PrimaryKey val key: String,
    val value: Long
) {
    companion object {
        const val CURRENT_LOAN_REPAIR_VERSION = 1L
        const val KEY_LOAN_REPAIR_VERSION = "loan_repair_migration_version"
    }
}
