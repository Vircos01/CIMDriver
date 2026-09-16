package com.cimdriver.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase

import com.cimdriver.app.data.local.dao.*
import com.cimdriver.app.data.local.entity.*

@Database(
    entities = [
        Trip::class, 
        Vehicle::class, 
        Settings::class,
        ClassificationRule::class,
        SavedAddress::class,
        BluetoothDevice::class,
        WorkDay::class,
        LocationPoint::class,
        Workplace::class
    ], 
    version = 29, 
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

	abstract fun vehicleDao(): VehicleDao
	abstract fun tripDao(): TripDao
	abstract fun locationPointDao(): LocationPointDao
	abstract fun bluetoothDeviceDao(): BluetoothDeviceDao
	abstract fun workplaceDao(): WorkplaceDao
	abstract fun settingsDao(): SettingsDao
	abstract fun savedAddressDao(): SavedAddressDao
	abstract fun workDayDao(): WorkDayDao
	abstract fun classificationRuleDao(): ClassificationRuleDao

	companion object {
		const val DATABASE_NAME = "cimdriver_database"
		@Volatile private var INSTANCE: AppDatabase? = null

		val MIGRATION_1_2 = object : Migration(1, 2) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'SYSTEM'") } }
		val MIGRATION_2_3 = object : Migration(2, 3) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE vehicles ADD COLUMN usageType TEXT NOT NULL DEFAULT 'MIXED'") } }
		val MIGRATION_3_4 = object : Migration(3, 4) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS `saved_addresses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `label` TEXT NOT NULL, `address` TEXT NOT NULL, `isWorkLocation` INTEGER NOT NULL)"); db.execSQL("CREATE TABLE IF NOT EXISTS `work_days` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `firstDepartureTime` INTEGER NOT NULL, `arrivalTime` INTEGER NOT NULL, `departureTime` INTEGER, `lastArrivalTime` INTEGER, `breakMinutes` INTEGER NOT NULL, `workLocationLabel` TEXT, `status` TEXT NOT NULL)"); db.execSQL("ALTER TABLE settings ADD COLUMN defaultBreakMinutes INTEGER NOT NULL DEFAULT 30") } }
		val MIGRATION_4_5 = object : Migration(4, 5) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE saved_addresses ADD COLUMN isHomeLocation INTEGER NOT NULL DEFAULT 0") } }
		val MIGRATION_5_6 = object : Migration(5, 6) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE saved_addresses ADD COLUMN isCustomerLocation INTEGER NOT NULL DEFAULT 0") } }
		val MIGRATION_6_7 = object : Migration(6, 7) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE saved_addresses ADD COLUMN projectCode TEXT DEFAULT NULL"); db.execSQL("ALTER TABLE trips ADD COLUMN projectCode TEXT DEFAULT NULL") } }
		val MIGRATION_7_8 = object : Migration(7, 8) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE saved_addresses ADD COLUMN notes TEXT DEFAULT NULL") } }
		val MIGRATION_8_9 = object : Migration(8, 9) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE work_days ADD COLUMN projectCode TEXT DEFAULT NULL") } }
		val MIGRATION_9_10 = object : Migration(9, 10) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE work_days ADD COLUMN roundedArrivalTime INTEGER DEFAULT NULL"); db.execSQL("ALTER TABLE work_days ADD COLUMN roundedDepartureTime INTEGER DEFAULT NULL") } }
		val MIGRATION_10_11 = object : Migration(10, 11) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE settings ADD COLUMN preferredNavAppPackage TEXT DEFAULT NULL") } }
		val MIGRATION_11_12 = object : Migration(11, 12) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE saved_addresses ADD COLUMN defaultTripType TEXT DEFAULT NULL"); db.execSQL("UPDATE saved_addresses SET defaultTripType = 'Home To Work' WHERE isHomeLocation = 1"); db.execSQL("UPDATE saved_addresses SET defaultTripType = 'Business Meeting' WHERE isWorkLocation = 1"); db.execSQL("UPDATE saved_addresses SET defaultTripType = 'Customer Visit' WHERE isCustomerLocation = 1") } }
		val MIGRATION_12_13 = object : Migration(12, 13) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("""CREATE TABLE IF NOT EXISTS `fuel_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER, `tripId` INTEGER DEFAULT NULL, `stationName` TEXT NOT NULL, `stationAddress` TEXT DEFAULT NULL, `pricePerLiter` REAL NOT NULL, `litersPurchased` REAL NOT NULL, `totalCost` REAL NOT NULL, `timestamp` INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP)"""); db.execSQL("CREATE INDEX IF NOT EXISTS idx_fuel_vehicleId ON `fuel_entries`(`vehicleId`)"); db.execSQL("CREATE INDEX IF NOT EXISTS idx_fuel_tripId ON `fuel_entries`(`tripId`)"); db.execSQL("ALTER TABLE vehicles ADD COLUMN lastFillDate INTEGER DEFAULT NULL"); db.execSQL("ALTER TABLE vehicles ADD COLUMN currentTankLiters REAL DEFAULT NULL"); db.execSQL("ALTER TABLE vehicles ADD COLUMN avgCostPerLiter REAL DEFAULT NULL") } }
		val MIGRATION_13_14 = object : Migration(13, 14) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE saved_addresses ADD COLUMN addressType TEXT DEFAULT NULL"); db.execSQL("UPDATE saved_addresses SET addressType = 'THUIS' WHERE isHomeLocation = 1"); db.execSQL("UPDATE saved_addresses SET addressType = 'WERK' WHERE isWorkLocation = 1 AND isHomeLocation = 0"); db.execSQL("UPDATE saved_addresses SET addressType = 'KLANT' WHERE isCustomerLocation = 1 AND isHomeLocation = 0 AND isWorkLocation = 0") } }
		val MIGRATION_14_15 = object : Migration(14, 15) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("DROP TABLE IF EXISTS fuel_entries"); db.execSQL("CREATE TABLE vehicles_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, licensePlate TEXT NOT NULL, make TEXT NOT NULL, model TEXT NOT NULL, year TEXT, odometerStart INTEGER NOT NULL, odometerCurrent INTEGER NOT NULL, notes TEXT, isActive INTEGER NOT NULL, usageType TEXT NOT NULL)"); db.execSQL("INSERT INTO vehicles_new (id, name, licensePlate, make, model, year, odometerStart, odometerCurrent, notes, isActive, usageType) SELECT id, name, licensePlate, make, model, year, odometerStart, odometerCurrent, notes, isActive, usageType FROM vehicles"); db.execSQL("DROP TABLE vehicles"); db.execSQL("ALTER TABLE vehicles_new RENAME TO vehicles") } }
		val MIGRATION_15_16 = object : Migration(15, 16) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE settings ADD COLUMN classificationDefault TEXT NOT NULL DEFAULT 'PRIVATE'"); db.execSQL("ALTER TABLE settings ADD COLUMN classifyHomeWorkAsCommute INTEGER NOT NULL DEFAULT 1"); db.execSQL("ALTER TABLE settings ADD COLUMN classifyCustomerAsBusiness INTEGER NOT NULL DEFAULT 1") } }
		val MIGRATION_16_17 = object : Migration(16, 17) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS classification_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, startAddressType TEXT, endAddressType TEXT, tripType TEXT, category TEXT NOT NULL)"); db.execSQL("INSERT INTO classification_rules (name, startAddressType, endAddressType, tripType, category) VALUES ('Thuis naar Werk', 'THUIS', 'WERK', 'Home To Work', 'COMMUTE'), ('Werk naar Thuis', 'WERK', 'THUIS', 'Home To Work', 'COMMUTE'), ('Woon-werk rit', NULL, NULL, 'COMMUTE', 'COMMUTE'), ('Klantbezoek', NULL, NULL, 'Customer Visit', 'BUSINESS'), ('Zakelijke afspraak', NULL, NULL, 'Business Meeting', 'BUSINESS'), ('Klant factureerbaar', NULL, NULL, 'Customer Billable', 'BUSINESS'), ('Opdracht CIMSOLUTIONS', NULL, NULL, 'Commissioned By CIMSOLUTIONS', 'BUSINESS'), ('Opleiding', NULL, NULL, 'Exam Course', 'BUSINESS'), ('Auto onderhoud', NULL, NULL, 'Car Maintenance', 'BUSINESS'), ('Privérit', NULL, NULL, 'PERSONAL', 'PRIVATE'), ('Privé rit', NULL, NULL, 'PRIVATE', 'PRIVATE')") } }
		val MIGRATION_17_18 = object : Migration(17, 18) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE settings ADD COLUMN locationRetentionDays INTEGER NOT NULL DEFAULT 365") } }
		val MIGRATION_18_19 = object : Migration(18, 19) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE settings ADD COLUMN workHoursToleranceMinutes INTEGER NOT NULL DEFAULT 30") } }
		val MIGRATION_19_20 = object : Migration(19, 20) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE saved_addresses ADD COLUMN latitude REAL DEFAULT NULL"); db.execSQL("ALTER TABLE saved_addresses ADD COLUMN longitude REAL DEFAULT NULL") } }
		val MIGRATION_20_21 = object : Migration(20, 21) { 
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { 
                db.execSQL("ALTER TABLE vehicles ADD COLUMN lastOdometerCheckTimestamp INTEGER NOT NULL DEFAULT 0") 
                
                // Recalculate odometer for all existing trips
                val vehiclesCursor = db.query("SELECT id, odometerStart FROM vehicles")
                while (vehiclesCursor.moveToNext()) {
                    val vehicleId = vehiclesCursor.getLong(0)
                    var currentOdo = vehiclesCursor.getInt(1)
                    
                    val tripsCursor = db.query("SELECT id, distanceMeters FROM trips WHERE vehicleId = ? ORDER BY startTime ASC", arrayOf<Any>(vehicleId))
                    while (tripsCursor.moveToNext()) {
                        val tripId = tripsCursor.getLong(0)
                        val distanceMeters = tripsCursor.getInt(1)
                        
                        val odoStart = currentOdo
                        val odoEnd = odoStart + Math.round(distanceMeters / 1000f)
                        
                        db.execSQL("UPDATE trips SET odometerStart = ?, odometerEnd = ? WHERE id = ?", arrayOf<Any>(odoStart, odoEnd, tripId))
                        
                        currentOdo = odoEnd
                    }
                    tripsCursor.close()
                    
                    // Update vehicle's current odometer to match the last trip
                    db.execSQL("UPDATE vehicles SET odometerCurrent = ? WHERE id = ?", arrayOf<Any>(currentOdo, vehicleId))
                }
                vehiclesCursor.close()
            } 
        }

		val MIGRATION_21_22 = object : Migration(21, 22) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE settings ADD COLUMN odometerReminderIntervalDays INTEGER NOT NULL DEFAULT 30") } }
		val MIGRATION_22_23 = object : Migration(22, 23) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("ALTER TABLE vehicles ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0") } }
		val MIGRATION_23_24 = object : Migration(23, 24) { 
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { 
                db.execSQL("DELETE FROM classification_rules WHERE name = 'Werk naar Thuis'")
                db.execSQL("DELETE FROM classification_rules WHERE name = 'Privé rit'")
                db.execSQL("UPDATE classification_rules SET name = 'Thuis <-> Werk' WHERE name = 'Thuis naar Werk'")
            } 
        }

		val MIGRATION_24_25 = object : Migration(24, 25) { 
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { 
                db.execSQL("ALTER TABLE settings ADD COLUMN privateKmYearlyLimit INTEGER NOT NULL DEFAULT 500")
                db.execSQL("ALTER TABLE settings ADD COLUMN showPrivateKmWarning INTEGER NOT NULL DEFAULT 1")
            } 
        }

		val MIGRATION_25_26 = object : Migration(25, 26) { 
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { 
                // Columns already exist from MIGRATION_19_20, but were missing from Kotlin data class.
                // We just need to satisfy Room's version check without executing ALTER TABLE again.
            } 
        }

		val MIGRATION_26_27 = object : Migration(26, 27) { 
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { 
                db.execSQL("ALTER TABLE vehicles ADD COLUMN privateKmYearlyLimit INTEGER NOT NULL DEFAULT 500")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN showPrivateKmWarning INTEGER NOT NULL DEFAULT 1")
                
                db.execSQL("""
                    UPDATE vehicles 
                    SET privateKmYearlyLimit = COALESCE((SELECT privateKmYearlyLimit FROM settings LIMIT 1), 500),
                        showPrivateKmWarning = COALESCE((SELECT showPrivateKmWarning FROM settings LIMIT 1), 1)
                """)

                db.execSQL("""
                    CREATE TABLE settings_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        defaultTripType TEXT NOT NULL,
                        gracePeriodSec INTEGER NOT NULL,
                        defaultBreakMinutes INTEGER NOT NULL,
                        workHoursToleranceMinutes INTEGER NOT NULL,
                        trackingInterval INTEGER NOT NULL,
                        workStartTime TEXT NOT NULL,
                        workEndTime TEXT NOT NULL,
                        workDays TEXT NOT NULL,
                        autoMergeEnabled INTEGER NOT NULL,
                        mergeTimeLimitMin INTEGER NOT NULL,
                        mergeDistanceM INTEGER NOT NULL,
                        odometerReminder INTEGER NOT NULL,
                        odometerReminderIntervalDays INTEGER NOT NULL,
                        backupReminder INTEGER NOT NULL,
                        distanceUnit TEXT NOT NULL,
                        themeMode TEXT NOT NULL,
                        preferredNavAppPackage TEXT,
                        classificationDefault TEXT NOT NULL,
                        classifyHomeWorkAsCommute INTEGER NOT NULL,
                        classifyCustomerAsBusiness INTEGER NOT NULL,
                        locationRetentionDays INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO settings_new (
                        id, defaultTripType, gracePeriodSec, defaultBreakMinutes, workHoursToleranceMinutes,
                        trackingInterval, workStartTime, workEndTime, workDays, autoMergeEnabled,
                        mergeTimeLimitMin, mergeDistanceM, odometerReminder, odometerReminderIntervalDays,
                        backupReminder, distanceUnit, themeMode, preferredNavAppPackage,
                        classificationDefault, classifyHomeWorkAsCommute, classifyCustomerAsBusiness, locationRetentionDays
                    )
                    SELECT 
                        id, defaultTripType, gracePeriodSec, defaultBreakMinutes, workHoursToleranceMinutes,
                        trackingInterval, workStartTime, workEndTime, workDays, autoMergeEnabled,
                        mergeTimeLimitMin, mergeDistanceM, odometerReminder, odometerReminderIntervalDays,
                        backupReminder, distanceUnit, themeMode, preferredNavAppPackage,
                        classificationDefault, classifyHomeWorkAsCommute, classifyCustomerAsBusiness, locationRetentionDays
                    FROM settings
                """)
                db.execSQL("DROP TABLE settings")
                db.execSQL("ALTER TABLE settings_new RENAME TO settings")
            } 
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_addresses ADD COLUMN classificationRuleId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE classification_rules ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE classification_rules ADD COLUMN isEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }
        
        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN maxRecoveryAttempts INTEGER NOT NULL DEFAULT 3")
            }
        }

		fun getDatabase(context: Context): AppDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29).addCallback(object : RoomDatabase.Callback() { override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("INSERT INTO classification_rules (name, startAddressType, endAddressType, tripType, category) VALUES ('Thuis <-> Werk', 'THUIS', 'WERK', 'Home To Work', 'COMMUTE'), ('Woon-werk rit', NULL, NULL, 'COMMUTE', 'COMMUTE'), ('Klantbezoek', NULL, NULL, 'Customer Visit', 'BUSINESS'), ('Zakelijke afspraak', NULL, NULL, 'Business Meeting', 'BUSINESS'), ('Klant factureerbaar', NULL, NULL, 'Customer Billable', 'BUSINESS'), ('Opdracht CIMSOLUTIONS', NULL, NULL, 'Commissioned By CIMSOLUTIONS', 'BUSINESS'), ('Opleiding', NULL, NULL, 'Exam Course', 'BUSINESS'), ('Auto onderhoud', NULL, NULL, 'Car Maintenance', 'BUSINESS'), ('Privérit', NULL, NULL, 'PERSONAL', 'PRIVATE')") } }).build()
				INSTANCE = instance
				instance
			}
		}

		fun closeDatabase() {
			synchronized(this) {
				INSTANCE?.close()
				INSTANCE = null
			}
		}
	}

}
