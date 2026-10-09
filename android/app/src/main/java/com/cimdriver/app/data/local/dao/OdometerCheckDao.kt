package com.cimdriver.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cimdriver.app.data.local.entity.OdometerCheck
import kotlinx.coroutines.flow.Flow

@Dao
interface OdometerCheckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(check: OdometerCheck)

    @Query("SELECT * FROM odometer_checks WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    fun getChecksForVehicle(vehicleId: Long): Flow<List<OdometerCheck>>
}
