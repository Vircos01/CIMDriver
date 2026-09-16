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
