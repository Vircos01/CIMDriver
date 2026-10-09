package com.cimdriver.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val licensePlate: String,
    val make: String,
    val model: String,
    val year: String?,
    val odometerStart: Int,
    val odometerCurrent: Int,
    val notes: String?,
    val isActive: Boolean = true,
    val privateKmYearlyLimit: Int = 500,
    val showPrivateKmWarning: Boolean = true,
    val usageType: String = "MIXED", // "MIXED", "BUSINESS_ONLY", "PRIVATE_ONLY"
    val engineType: String = "ICE", // "ICE", "PHEV", "EV"
    val lastOdometerCheckTimestamp: Long = 0L,
    val isDefault: Boolean = false,
    val odometerCorrectionStrategy: String = "DISTRIBUTE", // "DISTRIBUTE", "CREATE_TRIP", "LEAVE_GAP"
    val inServiceDate: Long? = null,
    val endServiceDate: Long? = null
)

@Entity(
    tableName = "bluetooth_devices",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class BluetoothDevice(
    @PrimaryKey val macAddress: String,
    val deviceName: String,
    val vehicleId: Long,
    val isAutoTrigger: Boolean = true,
    val lastConnected: Long = 0L
)

@Entity(
    tableName = "trips",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("vehicleId")]
)
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long?,
    val startTime: Long,
    val endTime: Long?,
    val startAddress: String?,
    val endAddress: String?,
    val distanceMeters: Int,
    val tripType: String, // "BUSINESS", "PERSONAL", "COMMUTE"
    val note: String?,
    val status: String, // "ACTIVE", "DONE", "MERGED"
    val isManual: Boolean,
    val odometerStart: Int,
    val odometerEnd: Int?,
    val expectedDistanceMeters: Int? = null,
    val projectCode: String? = null,
    val appliedRuleName: String? = null,
    val projectCodeId: Long? = null
)

@Entity(
    tableName = "location_points",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class LocationPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Float,
    val accuracy: Float,
    val timestamp: Long
)

@Entity(tableName = "workplaces")
data class Workplace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val autoClassify: String // "BUSINESS", "PERSONAL"
)

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val defaultTripType: String = "PERSONAL",
    val gracePeriodSec: Int = 180,
    val defaultBreakMinutes: Int = 30,
    val workHoursToleranceMinutes: Int = 30,
    val trackingInterval: Int = 5,
    val workStartTime: String = "08:00",
    val workEndTime: String = "18:00",
    val workDays: String = "1,2,3,4,5", // 1=Mon, 5=Fri
    val autoMergeEnabled: Boolean = true,
    val mergeTimeLimitMin: Int = 15,
    val mergeDistanceM: Int = 500,
    val odometerReminder: Boolean = true,
    val odometerReminderIntervalDays: Int = 30,
    val backupReminder: Boolean = true,
    val distanceUnit: String = "KM",
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val preferredNavAppPackage: String? = null,
    val classificationDefault: String = "PRIVATE",
    val classifyHomeWorkAsCommute: Boolean = true,
    val classifyCustomerAsBusiness: Boolean = true,
    val locationRetentionDays: Int = 365,
    val maxRecoveryAttempts: Int = 3,
    val businessCompensation: Float = 0.23f,
    val skippedUpdateVersionCode: Int = 0,
    val autoArchiveProjectDays: Int = 0 // 0 means disabled
)

@Entity(tableName = "saved_addresses")
data class SavedAddress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val address: String,
    val isWorkLocation: Boolean = false,
    val isHomeLocation: Boolean = false,
    val isCustomerLocation: Boolean = false,
    val defaultTripType: String? = null,
    val projectCode: String? = null,
    val notes: String? = null,
    val addressType: String? = null, // "THUIS", "WERK", "KLANT"
    val latitude: Double? = null,
    val longitude: Double? = null,
    val defaultProjectCodeId: Long? = null
)

@Entity(tableName = "work_days")
data class WorkDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val firstDepartureTime: Long,
    val arrivalTime: Long,
    val departureTime: Long?,
    val lastArrivalTime: Long?,
    val roundedArrivalTime: Long? = null,
    val roundedDepartureTime: Long? = null,
    val breakMinutes: Int = 30,
    val workLocationLabel: String?,
    val projectCode: String? = null,
    val status: String = "TO_REVIEW",
    val projectCodeId: Long? = null
)

@Entity(tableName = "classification_rules")
data class ClassificationRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startAddressType: String? = null,
    val endAddressType: String? = null,
    @ColumnInfo(defaultValue = "NULL") val startAddress: String? = null,
    @ColumnInfo(defaultValue = "NULL") val endAddress: String? = null,
    val tripType: String? = null,
    val category: String,
    @ColumnInfo(defaultValue = "0") val orderIndex: Int = 0,
    @ColumnInfo(defaultValue = "1") val isEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "0") val autoApprove: Boolean = false
)

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "'#1976D2'") val color: String = "#1976D2",
    @ColumnInfo(defaultValue = "1") val isActive: Boolean = true,
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "project_codes",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["clientId"], name = "idx_project_codes_clientId"), 
        Index(value = ["code"], unique = true, name = "idx_project_codes_code")
    ]

)
data class ProjectCode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long? = null,
    val code: String,
    val description: String? = null,
    @ColumnInfo(defaultValue = "1") val isBillable: Boolean = true,
    @ColumnInfo(defaultValue = "1") val isActive: Boolean = true,
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "hours_targets",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProjectCode::class,
            parentColumns = ["id"],
            childColumns = ["projectCodeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["clientId"], name = "idx_hours_targets_clientId"), 
        Index(value = ["projectCodeId"], name = "idx_hours_targets_projectCodeId")
    ]

)
data class HoursTarget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long? = null,
    val projectCodeId: Long? = null,
    val name: String,
    val targetHours: Double,
    val year: Int,
    @ColumnInfo(defaultValue = "'#4CAF50'") val color: String = "#4CAF50",
    @ColumnInfo(defaultValue = "1") val isActive: Boolean = true,
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_routes")
data class FavoriteRoute(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startAddress: String,
    val endAddress: String,
    val tripType: String,
    val projectCode: String? = null
)

@Entity(
    tableName = "odometer_checks",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vehicleId"])]
)
data class OdometerCheck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val timestamp: Long,
    val registeredOdometer: Int,
    val correctedOdometer: Int,
    val photoPath: String?
)

@Entity(
    tableName = "fuel_fillups",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vehicleId"])]
)
data class FuelFillUp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateTimestamp: Long,
    val liters: Double,
    val pricePerLiter: Double,
    val totalCost: Double,
    val odometer: Int,
    val stationName: String? = null,
    val notes: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val status: String = "COMPLETED" // "DRAFT" or "COMPLETED"
)
