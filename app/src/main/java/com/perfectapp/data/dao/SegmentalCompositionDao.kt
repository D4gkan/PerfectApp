package com.perfectapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.perfectapp.data.entities.SegmentalCompositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SegmentalCompositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SegmentalCompositionEntity): Long

    @Query("SELECT * FROM segmental_composition WHERE measurementId = :measurementId")
    fun observeForMeasurement(measurementId: Long): Flow<List<SegmentalCompositionEntity>>
}
