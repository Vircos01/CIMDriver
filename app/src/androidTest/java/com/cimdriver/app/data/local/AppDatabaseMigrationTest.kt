package com.cimdriver.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @Test
    @Throws(IOException::class)
    fun migratesVersion14To18() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(object : SupportSQLiteOpenHelper.Callback(14) {
                override fun onCreate(database: SupportSQLiteDatabase) = createVersion14Schema(database)
                override fun onUpgrade(database: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)

        helper.writableDatabase.use { database ->
            AppDatabase.MIGRATION_14_15.migrate(database)
            AppDatabase.MIGRATION_15_16.migrate(database)
            AppDatabase.MIGRATION_16_17.migrate(database)
            AppDatabase.MIGRATION_17_18.migrate(database)
            assertHasColumn(database, "settings", "locationRetentionDays")
            assertHasColumn(database, "vehicles", "usageType")
            assertHasTable(database, "classification_rules")
        }
        helper.close()
    }

    private fun createVersion14Schema(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE settings (id INTEGER NOT NULL PRIMARY KEY, defaultTripType TEXT NOT NULL, gracePeriodSec INTEGER NOT NULL, defaultBreakMinutes INTEGER NOT NULL, trackingInterval INTEGER NOT NULL, workStartTime TEXT NOT NULL, workEndTime TEXT NOT NULL, workDays TEXT NOT NULL, autoMergeEnabled INTEGER NOT NULL, mergeTimeLimitMin INTEGER NOT NULL, mergeDistanceM INTEGER NOT NULL, odometerReminder INTEGER NOT NULL, backupReminder INTEGER NOT NULL, distanceUnit TEXT NOT NULL, themeMode TEXT NOT NULL, preferredNavAppPackage TEXT)")
        database.execSQL("CREATE TABLE vehicles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, licensePlate TEXT NOT NULL, make TEXT NOT NULL, model TEXT NOT NULL, year TEXT, odometerStart INTEGER NOT NULL, odometerCurrent INTEGER NOT NULL, notes TEXT, isActive INTEGER NOT NULL, usageType TEXT NOT NULL)")
        database.execSQL("CREATE TABLE fuel_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, vehicleId INTEGER, tripId INTEGER, stationName TEXT NOT NULL, stationAddress TEXT, pricePerLiter REAL NOT NULL, litersPurchased REAL NOT NULL, totalCost REAL NOT NULL, timestamp INTEGER NOT NULL)")
    }

    private fun assertHasColumn(database: SupportSQLiteDatabase, table: String, column: String) {
        database.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            var found = false
            while (cursor.moveToNext()) found = found || cursor.getString(nameIndex) == column
            assertTrue("Expected $table.$column", found)
        }
    }

    private fun assertHasTable(database: SupportSQLiteDatabase, table: String) {
        database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$table'").use { cursor ->
            assertEquals(1, cursor.count)
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}