package com.perfectapp.data.dao

import androidx.room.*
import com.perfectapp.data.entities.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: BodyMeasurementEntity): Long

    @Update
    suspend fun update(measurement: BodyMeasurementEntity)

    @Delete
    suspend fun delete(measurement: BodyMeasurementEntity)

    @Query("SELECT * FROM body_measurements ORDER BY measurementDate DESC, measurementTime DESC")
    fun observeAll(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements ORDER BY measurementDate DESC, measurementTime DESC LIMIT 1")
    fun observeLatest(): Flow<BodyMeasurementEntity?>

    @Query("SELECT * FROM body_measurements ORDER BY measurementDate ASC, measurementTime ASC LIMIT 1")
    suspend fun getFirst(): BodyMeasurementEntity?
}
