package ru.coffeepoint.core.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant

internal object LocalTestEmployees {
    fun ensureSupplier(db: SupportSQLiteDatabase) {
        val now = Instant.now().toString()
        db.execSQL("""
            INSERT INTO employees (
                id, name, role, workStartDate, isActive, createdAt, updatedAt,
                isSynced, syncError, syncErrorType, isLocalOnly
            )
            SELECT MAX(1, COALESCE((SELECT MAX(id) FROM employees), 1)) + 1,
                ?, 'SUPPLY_OPERATOR', '', 1, ?, ?, 0, 0, '', 1
            WHERE NOT EXISTS (
                SELECT 1 FROM employees WHERE name = ? AND role = 'SUPPLY_OPERATOR'
            )
        """.trimIndent(), arrayOf("Денис", now, now, "Денис"))
    }
}
