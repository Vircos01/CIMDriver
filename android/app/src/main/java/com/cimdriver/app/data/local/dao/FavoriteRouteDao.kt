package com.cimdriver.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.cimdriver.app.data.local.entity.FavoriteRoute
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteRouteDao {
    @Query("SELECT * FROM favorite_routes ORDER BY name ASC")
    fun getAllFavoriteRoutes(): Flow<List<FavoriteRoute>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: FavoriteRoute): Long

    @Update
    suspend fun update(route: FavoriteRoute)

    @Delete
    suspend fun delete(route: FavoriteRoute)
}
