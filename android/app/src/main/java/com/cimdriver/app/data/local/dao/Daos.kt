package com.cimdriver.app.data.local.dao

import androidx.room.*
import com.cimdriver.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles")
    fun getAllVehicles(): Flow<List<Vehicle>>
    
    @Query("SELECT * FROM vehicles")
    suspend fun getAllVehiclesSync(): List<Vehicle>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Long): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("UPDATE vehicles SET isDefault = 0")
    suspend fun clearDefaultVehicles()
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    suspend fun getAllTripsSync(): List<Trip>

    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId ORDER BY startTime DESC")
    fun getTripsByVehicle(vehicleId: Long): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId ORDER BY startTime ASC")
    suspend fun getTripsByVehicleSync(vehicleId: Long): List<Trip>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Long): Trip?

    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestTripForVehicle(vehicleId: Long): Trip?

    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId AND status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveTripForVehicle(vehicleId: Long): Trip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)
    
    @Query("SELECT COUNT(*) FROM trips WHERE vehicleId = :vehicleId")
    suspend fun getTripCountForVehicle(vehicleId: Long): Int

    @Query("SELECT COUNT(*) FROM trips WHERE startAddress = :address OR endAddress = :address")
    suspend fun getTripCountForAddress(address: String): Int

    @Query("SELECT COUNT(*) FROM trips WHERE startAddress = :startAddress AND endAddress = :endAddress AND tripType = :tripType AND status = 'DONE'")
    suspend fun getTripCountForRoute(startAddress: String, endAddress: String, tripType: String): Int

    @Query("SELECT DISTINCT projectCode FROM trips WHERE projectCode IS NOT NULL AND projectCode != '' ORDER BY projectCode ASC")
    fun getUniqueProjectCodes(): Flow<List<String>>

    @Delete
    suspend fun deleteTrip(trip: Trip)
}

@Dao
interface LocationPointDao {
    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getPointsForTrip(tripId: Long): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPointForTrip(tripId: Long): LocationPoint?

    @Query("UPDATE location_points SET tripId = :newTripId WHERE tripId = :oldTripId")
    suspend fun updatePointsTripId(oldTripId: Long, newTripId: Long)

    @Insert
    suspend fun insertPoint(point: LocationPoint)

    @Insert
    suspend fun insertPoints(points: List<LocationPoint>)

    @Query("DELETE FROM location_points WHERE tripId = :tripId")
    suspend fun deletePointsForTrip(tripId: Long)

    @Query("DELETE FROM location_points WHERE timestamp < :cutoffTimestamp")
    suspend fun deletePointsOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM location_points")
    suspend fun deleteAllPoints()
}

@Dao
interface BluetoothDeviceDao {
    @Query("SELECT * FROM bluetooth_devices")
    fun getAllDevices(): Flow<List<BluetoothDevice>>

    @Query("SELECT * FROM bluetooth_devices WHERE macAddress = :macAddress")
    suspend fun getDeviceByMac(macAddress: String): BluetoothDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: BluetoothDevice)

    @Delete
    suspend fun deleteDevice(device: BluetoothDevice)
}

@Dao
interface WorkplaceDao {
    @Query("SELECT * FROM workplaces")
    fun getAllWorkplaces(): Flow<List<Workplace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkplace(workplace: Workplace)

    @Delete
    suspend fun deleteWorkplace(workplace: Workplace)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<Settings?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettingsSync(): Settings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: Settings)
}

@Dao
interface SavedAddressDao {
    @Query("SELECT * FROM saved_addresses ORDER BY label ASC")
    fun getAllAddresses(): Flow<List<SavedAddress>>

    @Query("SELECT * FROM saved_addresses")
    suspend fun getAllSavedAddressesSync(): List<SavedAddress>

    @Query("SELECT * FROM saved_addresses ORDER BY label ASC")
    suspend fun getAllAddressesSync(): List<SavedAddress>

    @Query("SELECT * FROM saved_addresses WHERE isWorkLocation = 1")
    fun getWorkLocations(): Flow<List<SavedAddress>>
    
    @Query("SELECT * FROM saved_addresses WHERE isWorkLocation = 1")
    suspend fun getWorkLocationsSync(): List<SavedAddress>

    @Query("SELECT * FROM saved_addresses WHERE isHomeLocation = 1 LIMIT 1")
    suspend fun getHomeLocationSync(): SavedAddress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: SavedAddress): Long

    @Update
    suspend fun updateAddress(address: SavedAddress)

    @Delete
    suspend fun deleteAddress(address: SavedAddress)
}

@Dao
interface FuelFillUpDao {
    @Query("SELECT * FROM fuel_entries ORDER BY dateTimestamp DESC")
    fun getAllFillUps(): Flow<List<FuelFillUp>>

    @Query("SELECT * FROM fuel_entries ORDER BY dateTimestamp DESC")
    suspend fun getAllFillUpsSync(): List<FuelFillUp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFillUp(fillUp: FuelFillUp): Long

    @Update
    suspend fun updateFillUp(fillUp: FuelFillUp)

    @Delete
    suspend fun deleteFillUp(fillUp: FuelFillUp)
}

@Dao
interface WorkDayDao {
    @Query("SELECT * FROM work_days ORDER BY date DESC")
    fun getAllWorkDays(): Flow<List<WorkDay>>

    @Query("SELECT * FROM work_days WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getWorkDaysInRange(startDate: Long, endDate: Long): Flow<List<WorkDay>>
    
    @Query("SELECT * FROM work_days WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getWorkDaysInRangeSync(startDate: Long, endDate: Long): List<WorkDay>

    @Query("SELECT * FROM work_days WHERE date = :date LIMIT 1")
    suspend fun getWorkDayByDate(date: Long): WorkDay?
    
    @Query("SELECT * FROM work_days WHERE departureTime IS NULL ORDER BY date DESC")
    suspend fun getIncompleteWorkDaysSync(): List<WorkDay>
    
    @Query("SELECT COUNT(*) FROM work_days WHERE status = 'TO_REVIEW'")
    fun getToReviewCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDay(workDay: WorkDay): Long

    @Update
    suspend fun updateWorkDay(workDay: WorkDay)

    @Delete
    suspend fun deleteWorkDay(workDay: WorkDay)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)
}

@Dao
interface ProjectCodeDao {
    @Query("SELECT * FROM project_codes WHERE isActive = 1 ORDER BY code ASC")
    fun getActiveProjectCodes(): Flow<List<ProjectCode>>

    @Query("SELECT * FROM project_codes WHERE clientId = :clientId AND isActive = 1")
    fun getProjectCodesForClient(clientId: Long): Flow<List<ProjectCode>>

    @Query("SELECT * FROM project_codes WHERE isBillable = 1 AND isActive = 1")
    fun getBillableProjectCodes(): Flow<List<ProjectCode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectCode(projectCode: ProjectCode): Long

    @Update
    suspend fun updateProjectCode(projectCode: ProjectCode)

    @Delete
    suspend fun deleteProjectCode(projectCode: ProjectCode)
}

data class HoursTargetWithDetails(
    @Embedded val target: HoursTarget,
    val clientName: String?,
    val projectCodeStr: String?
)

@Dao
interface HoursTargetDao {
    @Query("""
        SELECT ht.*, c.name AS clientName, pc.code AS projectCodeStr
        FROM hours_targets ht
        LEFT JOIN clients c ON ht.clientId = c.id
        LEFT JOIN project_codes pc ON ht.projectCodeId = pc.id
        WHERE ht.year = :year AND ht.isActive = 1
        ORDER BY ht.name ASC
    """)
    fun getActiveTargetsForYear(year: Int): Flow<List<HoursTargetWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: HoursTarget): Long

    @Update
    suspend fun updateTarget(target: HoursTarget)

    @Delete
    suspend fun deleteTarget(target: HoursTarget)
interface FuelFillUpDao {
    @Query("SELECT * FROM fuel_fillups ORDER BY dateTimestamp DESC")
    fun getAllFillUps(): Flow<List<FuelFillUp>>

    @Query("SELECT * FROM fuel_fillups WHERE vehicleId = :vehicleId ORDER BY dateTimestamp DESC")
    fun getFillUpsForVehicle(vehicleId: Long): Flow<List<FuelFillUp>>

    @Query("SELECT * FROM fuel_fillups")
    suspend fun getAllFillUpsSync(): List<FuelFillUp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFillUp(fillUp: FuelFillUp): Long

    @Update
    suspend fun updateFillUp(fillUp: FuelFillUp)

    @Delete
    suspend fun deleteFillUp(fillUp: FuelFillUp)
}
