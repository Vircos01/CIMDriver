package com.cimdriver.app.data.repository

import com.cimdriver.app.data.local.dao.FuelFillUpDao
import com.cimdriver.app.data.local.entity.FuelFillUp
import kotlinx.coroutines.flow.Flow

class FuelRepository(
    private val fuelFillUpDao: FuelFillUpDao
) {
    fun getAllFillUps(): Flow<List<FuelFillUp>> = fuelFillUpDao.getAllFillUps()
    
    fun getFillUpsForVehicle(vehicleId: Long): Flow<List<FuelFillUp>> = fuelFillUpDao.getFillUpsForVehicle(vehicleId)
    
    suspend fun insertFillUp(fillUp: FuelFillUp): Long {
        return fuelFillUpDao.insertFillUp(fillUp)
    }
    
    suspend fun updateFillUp(fillUp: FuelFillUp) {
        fuelFillUpDao.updateFillUp(fillUp)
    }
    
    suspend fun deleteFillUp(fillUp: FuelFillUp) {
        fuelFillUpDao.deleteFillUp(fillUp)
    }
}
