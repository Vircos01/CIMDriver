package com.cimdriver.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cimdriver.app.data.local.entity.OdometerCheck
import kotlinx.coroutines.flow.Flow

@Dao
interface OdometerCheckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(check: OdometerCheck): Long

    @Update
    suspend fun update(check: OdometerCheck)

    @Delete
    suspend fun delete(check: OdometerCheck)

    @Query("SELECT * FROM odometer_checks WHERE id = :id LIMIT 1")
    suspend fun getCheckById(id: Long): OdometerCheck?

    @Query("SELECT * FROM odometer_checks WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    fun getChecksForVehicle(vehicleId: Long): Flow<List<OdometerCheck>>

    @Query("SELECT * FROM odometer_checks WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    suspend fun getChecksForVehicleSync(vehicleId: Long): List<OdometerCheck>

    @Query("SELECT * FROM odometer_checks ORDER BY timestamp DESC")
    fun getAllChecks(): Flow<List<OdometerCheck>>

    @Query("SELECT * FROM odometer_checks ORDER BY timestamp DESC")
    suspend fun getAllChecksSync(): List<OdometerCheck>
}

