package ru.coffeepoint.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {

        return INSTANCE ?: synchronized(this) {

            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "coffee_point.db"
            )
                .addMigrations(
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_20_21,
                    MIGRATION_21_22,
                    MIGRATION_22_23,
                    MIGRATION_23_24,
                    MIGRATION_24_25,
                    MIGRATION_25_26,
                    MIGRATION_26_27,
                    MIGRATION_27_28,
                    MIGRATION_28_29,
                    MIGRATION_29_30
                )
                .build()
                .also {
                    INSTANCE = it
                }

        }

    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN rentPaidUntil TEXT NOT NULL DEFAULT ''"
            )

        }

    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN excludeFromSupplyPlanning INTEGER NOT NULL DEFAULT 0"
            )

        }

    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN liquidWasteContainerServiceDate TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN brewUnitServiceDate TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN coffeeGrinderServiceDate TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN exhaustServiceDate TEXT NOT NULL DEFAULT ''"
            )

        }

    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN mixersServiceDate TEXT NOT NULL DEFAULT ''"
            )

        }

    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN expansionTankServiceDate TEXT NOT NULL DEFAULT ''"
            )

        }

    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1"
            )
            db.execSQL(
                "ALTER TABLE station_inventory ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1"
            )
            db.execSQL(
                "ALTER TABLE station_settings ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1"
            )

        }

    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE coffee_station_new (
                    id INTEGER NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    terminalId INTEGER,
                    address TEXT NOT NULL,
                    lastServiceDate TEXT NOT NULL,
                    salesAfterService INTEGER NOT NULL,
                    rentPaidUntil TEXT NOT NULL,
                    excludeFromSupplyPlanning INTEGER NOT NULL,
                    liquidWasteContainerServiceDate TEXT NOT NULL,
                    brewUnitServiceDate TEXT NOT NULL,
                    coffeeGrinderServiceDate TEXT NOT NULL,
                    mixersServiceDate TEXT NOT NULL,
                    expansionTankServiceDate TEXT NOT NULL,
                    exhaustServiceDate TEXT NOT NULL,
                    machinePhotoPath TEXT,
                    overviewPhotoPath TEXT,
                    isSynced INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO coffee_station_new (
                    id,
                    name,
                    terminalId,
                    address,
                    lastServiceDate,
                    salesAfterService,
                    rentPaidUntil,
                    excludeFromSupplyPlanning,
                    liquidWasteContainerServiceDate,
                    brewUnitServiceDate,
                    coffeeGrinderServiceDate,
                    mixersServiceDate,
                    expansionTankServiceDate,
                    exhaustServiceDate,
                    machinePhotoPath,
                    overviewPhotoPath,
                    isSynced
                )
                SELECT
                    id,
                    name,
                    terminalId,
                    address,
                    lastServiceDate,
                    salesAfterService,
                    rentPaidUntil,
                    excludeFromSupplyPlanning,
                    liquidWasteContainerServiceDate,
                    brewUnitServiceDate,
                    coffeeGrinderServiceDate,
                    mixersServiceDate,
                    expansionTankServiceDate,
                    exhaustServiceDate,
                    machinePhotoPath,
                    overviewPhotoPath,
                    isSynced
                FROM coffee_station
                """.trimIndent()
            )
            db.execSQL("DROP TABLE coffee_station")
            db.execSQL("ALTER TABLE coffee_station_new RENAME TO coffee_station")

        }

    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN syncError INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE station_inventory ADD COLUMN syncError INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE station_settings ADD COLUMN syncError INTEGER NOT NULL DEFAULT 0"
            )

        }

    }

    private val MIGRATION_11_12 = object : Migration(11, 12) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN excludeFromOperatorEarnings INTEGER NOT NULL DEFAULT 0"
            )

        }

    }

    private val MIGRATION_12_13 = object : Migration(12, 13) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE station_inventory_new (
                    stationId INTEGER NOT NULL PRIMARY KEY,
                    cups INTEGER NOT NULL,
                    lids INTEGER NOT NULL,
                    coffee INTEGER NOT NULL,
                    milk INTEGER NOT NULL,
                    chocolate INTEGER NOT NULL,
                    topping INTEGER NOT NULL,
                    sugar INTEGER NOT NULL,
                    stirrers INTEGER NOT NULL,
                    isSynced INTEGER NOT NULL,
                    syncError INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO station_inventory_new (
                    stationId,
                    cups,
                    lids,
                    coffee,
                    milk,
                    chocolate,
                    topping,
                    sugar,
                    stirrers,
                    isSynced,
                    syncError
                )
                SELECT
                    stationId,
                    cups,
                    lids,
                    coffee,
                    milk,
                    chocolate,
                    topping,
                    CASE sugar
                        WHEN 'EMPTY' THEN 0
                        WHEN 'LOW' THEN 1
                        WHEN 'MEDIUM' THEN 1
                        WHEN 'FULL' THEN 2
                        ELSE 2
                    END,
                    CASE stirrers
                        WHEN 'EMPTY' THEN 0
                        WHEN 'LOW' THEN 1
                        WHEN 'MEDIUM' THEN 2
                        WHEN 'FULL' THEN 4
                        ELSE 4
                    END,
                    isSynced,
                    syncError
                FROM station_inventory
                """.trimIndent()
            )
            db.execSQL("DROP TABLE station_inventory")
            db.execSQL(
                "ALTER TABLE station_inventory_new RENAME TO station_inventory"
            )
            db.execSQL(
                "ALTER TABLE station_settings ADD COLUMN sugarMax INTEGER NOT NULL DEFAULT 2"
            )
            db.execSQL(
                "ALTER TABLE station_settings ADD COLUMN stirrersMax INTEGER NOT NULL DEFAULT 4"
            )

        }

    }

    private val MIGRATION_13_14 = object : Migration(13, 14) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE station_inventory ADD COLUMN garbageBags INTEGER NOT NULL DEFAULT 2"
            )

        }

    }

    private val MIGRATION_14_15 = object : Migration(14, 15) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE station_settings ADD COLUMN garbageBagsMax INTEGER NOT NULL DEFAULT 2"
            )

        }

    }

    private val MIGRATION_15_16 = object : Migration(15, 16) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN visitDays TEXT NOT NULL DEFAULT '1,2,3,4,5'"
            )
            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN visitTimeFrom TEXT NOT NULL DEFAULT '09:00'"
            )
            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN visitTimeTo TEXT NOT NULL DEFAULT '18:00'"
            )

        }

    }

    private val MIGRATION_16_17 = object : Migration(16, 17) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL("DROP TABLE IF EXISTS purchase_options")
            db.execSQL(
                """
                CREATE TABLE purchase_options (
                    id INTEGER NOT NULL PRIMARY KEY,
                    itemType TEXT NOT NULL,
                    title TEXT NOT NULL,
                    supplier TEXT NOT NULL,
                    article TEXT NOT NULL,
                    url TEXT NOT NULL,
                    maxPrice INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    priority INTEGER NOT NULL,
                    comment TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    isSynced INTEGER NOT NULL,
                    syncError INTEGER NOT NULL
                )
                """.trimIndent()
            )

        }

    }

    private val MIGRATION_17_18 = object : Migration(17, 18) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE purchase_options RENAME TO supply_catalog"
            )

        }

    }

    private val MIGRATION_18_19 = object : Migration(18, 19) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE station_inventory ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE station_settings ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''"
            )

        }

    }

    private val MIGRATION_19_20 = object : Migration(19, 20) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE coffee_station ADD COLUMN syncErrorType TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE station_inventory ADD COLUMN syncErrorType TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE station_settings ADD COLUMN syncErrorType TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE supply_catalog ADD COLUMN syncErrorType TEXT NOT NULL DEFAULT ''"
            )

        }

    }

    private val MIGRATION_20_21 = object : Migration(20, 21) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS warehouse_stock (
                    itemType TEXT NOT NULL PRIMARY KEY,
                    quantity INTEGER NOT NULL,
                    updatedAt TEXT NOT NULL
                )
                """.trimIndent()
            )

        }

    }

    private val MIGRATION_21_22 = object : Migration(21, 22) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE warehouse_stock ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1"
            )
            db.execSQL(
                "ALTER TABLE warehouse_stock ADD COLUMN syncError INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE warehouse_stock ADD COLUMN syncErrorType TEXT NOT NULL DEFAULT ''"
            )

        }

    }

    private val MIGRATION_22_23 = object : Migration(22, 23) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS operator_payment (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    paymentDate TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    comment TEXT NOT NULL,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL
                )
                """.trimIndent()
            )

        }

    }

    private val MIGRATION_23_24 = object : Migration(23, 24) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS employees (
                    id INTEGER NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    workStartDate TEXT NOT NULL,
                    isActive INTEGER NOT NULL,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO employees (
                    id,
                    name,
                    role,
                    workStartDate,
                    isActive,
                    createdAt,
                    updatedAt
                ) VALUES (
                    1,
                    'Максим',
                    'OPERATOR',
                    '2026-09-13',
                    1,
                    '2026-09-13T00:00:00',
                    '2026-09-13T00:00:00'
                )
                """.trimIndent()
            )
            db.execSQL(
                "ALTER TABLE operator_payment ADD COLUMN employeeId INTEGER NOT NULL DEFAULT 1"
            )

        }

    }

    private val MIGRATION_24_25 = object : Migration(24, 25) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE operator_payment RENAME TO employees_payment"
            )

        }

    }

    private val MIGRATION_25_26 = object : Migration(25, 26) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS employees_earnings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    employeeId INTEGER NOT NULL,
                    periodFrom TEXT NOT NULL,
                    periodTo TEXT NOT NULL,
                    salesCount INTEGER NOT NULL,
                    amount INTEGER NOT NULL,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_employees_earnings_employeeId_periodFrom_periodTo
                ON employees_earnings(employeeId, periodFrom, periodTo)
                """.trimIndent()
            )

        }

    }

    private val MIGRATION_26_27 = object : Migration(26, 27) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL("DROP TABLE IF EXISTS employees_payment")
            db.execSQL("DROP TABLE IF EXISTS employees_earnings")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS employees_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    employeeId INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    operationDate TEXT NOT NULL,
                    periodFrom TEXT,
                    periodTo TEXT,
                    salesCount INTEGER,
                    amount INTEGER NOT NULL,
                    comment TEXT NOT NULL,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_employees_transactions_employeeId_type_periodFrom_periodTo
                ON employees_transactions(employeeId, type, periodFrom, periodTo)
                """.trimIndent()
            )

        }

    }

    private val MIGRATION_27_28 = object : Migration(27, 28) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS employee_station_assignments (
                    id TEXT PRIMARY KEY NOT NULL,
                    employeeId INTEGER NOT NULL,
                    stationId INTEGER NOT NULL,
                    validFrom TEXT NOT NULL,
                    validTo TEXT,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_employee_station_assignments_employeeId_stationId_validFrom_validTo
                ON employee_station_assignments(employeeId, stationId, validFrom, validTo)
                """.trimIndent()
            )

            db.execSQL("ALTER TABLE employees_transactions RENAME TO employees_transactions_old")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS employees_transactions (
                    id TEXT PRIMARY KEY NOT NULL,
                    employeeId INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    operationDate TEXT NOT NULL,
                    periodFrom TEXT,
                    periodTo TEXT,
                    salesCount INTEGER,
                    amount INTEGER NOT NULL,
                    comment TEXT NOT NULL,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO employees_transactions (
                    id,
                    employeeId,
                    type,
                    operationDate,
                    periodFrom,
                    periodTo,
                    salesCount,
                    amount,
                    comment,
                    createdAt,
                    updatedAt
                )
                SELECT
                    'legacy-' || id,
                    employeeId,
                    type,
                    operationDate,
                    periodFrom,
                    periodTo,
                    salesCount,
                    amount,
                    comment,
                    createdAt,
                    updatedAt
                FROM employees_transactions_old
                """.trimIndent()
            )
            db.execSQL("DROP TABLE employees_transactions_old")
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_employees_transactions_employeeId_type_periodFrom_periodTo
                ON employees_transactions(employeeId, type, periodFrom, periodTo)
                """.trimIndent()
            )

        }

    }

    private val MIGRATION_28_29 = object : Migration(28, 29) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                "ALTER TABLE employees ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE employees ADD COLUMN syncError INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE employees ADD COLUMN syncErrorType TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE employee_station_assignments ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE employee_station_assignments ADD COLUMN syncError INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE employee_station_assignments ADD COLUMN syncErrorType TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE employees_transactions ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE employees_transactions ADD COLUMN syncError INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE employees_transactions ADD COLUMN syncErrorType TEXT NOT NULL DEFAULT ''"
            )

        }

    }

    private val MIGRATION_29_30 = object : Migration(29, 30) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                UPDATE employees
                SET isSynced = 0,
                    syncError = 0,
                    syncErrorType = ''
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE employee_station_assignments
                SET isSynced = 0,
                    syncError = 0,
                    syncErrorType = ''
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE employees_transactions
                SET isSynced = 0,
                    syncError = 0,
                    syncErrorType = ''
                """.trimIndent()
            )

        }

    }

}
