package com.cimdriver.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cimdriver.app.data.local.entity.OdometerGapResolution
import kotlinx.coroutines.flow.Flow

@Dao
interface OdometerGapResolutionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resolution: OdometerGapResolution): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(resolutions: List<OdometerGapResolution>)

    @Update
    suspend fun update(resolution: OdometerGapResolution)

    @Delete
    suspend fun delete(resolution: OdometerGapResolution)

    @Query("SELECT * FROM odometer_gap_resolutions WHERE id = :id LIMIT 1")
    suspend fun getResolutionById(id: Long): OdometerGapResolution?

    @Query("SELECT * FROM odometer_gap_resolutions WHERE vehicleId = :vehicleId ORDER BY gapStartTimestamp DESC")
    fun getResolutionsForVehicle(vehicleId: Long): Flow<List<OdometerGapResolution>>

    @Query("SELECT * FROM odometer_gap_resolutions WHERE vehicleId = :vehicleId ORDER BY gapStartTimestamp DESC")
    suspend fun getResolutionsForVehicleSync(vehicleId: Long): List<OdometerGapResolution>

    @Query("SELECT * FROM odometer_gap_resolutions WHERE vehicleId = :vehicleId AND status = 'OPEN' ORDER BY gapStartTimestamp DESC")
    fun getOpenResolutionsForVehicle(vehicleId: Long): Flow<List<OdometerGapResolution>>

    @Query("SELECT * FROM odometer_gap_resolutions WHERE vehicleId = :vehicleId AND status = 'OPEN' ORDER BY gapStartTimestamp DESC")
    suspend fun getOpenResolutionsForVehicleSync(vehicleId: Long): List<OdometerGapResolution>

    @Query("SELECT * FROM odometer_gap_resolutions WHERE status = 'OPEN' ORDER BY gapStartTimestamp DESC")
    fun getAllOpenResolutions(): Flow<List<OdometerGapResolution>>

    @Query("SELECT * FROM odometer_gap_resolutions WHERE status = 'OPEN' ORDER BY gapStartTimestamp DESC")
    suspend fun getAllOpenResolutionsSync(): List<OdometerGapResolution>

    @Query("SELECT COUNT(*) FROM odometer_gap_resolutions WHERE status = 'OPEN'")
    fun getOpenResolutionCount(): Flow<Int>

    @Query("SELECT * FROM odometer_gap_resolutions ORDER BY gapStartTimestamp DESC")
    fun getAllResolutions(): Flow<List<OdometerGapResolution>>

    @Query("SELECT * FROM odometer_gap_resolutions ORDER BY gapStartTimestamp DESC")
    suspend fun getAllResolutionsSync(): List<OdometerGapResolution>
}
