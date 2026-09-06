package com.perfectapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.perfectapp.data.entities.CarEntity
import com.perfectapp.data.entities.OdometerEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @androidx.room.Upsert
    suspend fun upsertCar(car: CarEntity): Long

    @Query("SELECT * FROM cars ORDER BY id")
    fun observeCars(): Flow<List<CarEntity>>

    @androidx.room.Delete
    suspend fun deleteCar(car: CarEntity)

    @Query("DELETE FROM odometer_entries WHERE carId = :carId")
    suspend fun deleteOdometerHistory(carId: Long)

    @Query("UPDATE reminders SET carId = NULL WHERE carId = :carId")
    suspend fun detachRenewals(carId: Long)

    @Query("SELECT * FROM cars LIMIT 1")
    fun observePrimaryCar(): Flow<CarEntity?>

    @Insert
    suspend fun insertOdometerEntry(entry: OdometerEntryEntity): Long

    @Query("SELECT * FROM odometer_entries WHERE carId = :carId ORDER BY date DESC")
    fun observeOdometerHistory(carId: Long): Flow<List<OdometerEntryEntity>>
}
