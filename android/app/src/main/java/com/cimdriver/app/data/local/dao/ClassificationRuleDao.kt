package com.cimdriver.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cimdriver.app.data.local.entity.ClassificationRule
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassificationRuleDao {
    @Query("SELECT * FROM classification_rules ORDER BY id ASC")
    fun getAllRules(): Flow<List<ClassificationRule>>

    @Query("SELECT * FROM classification_rules ORDER BY id ASC")
    suspend fun getAllRulesSync(): List<ClassificationRule>

    @Insert
    suspend fun insertRule(rule: ClassificationRule): Long

    @Update
    suspend fun updateRule(rule: ClassificationRule)

    @Delete
    suspend fun deleteRule(rule: ClassificationRule)
}