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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCar(car: CarEntity): Long

    @Query("SELECT * FROM cars LIMIT 1")
    fun observePrimaryCar(): Flow<CarEntity?>

    @Insert
    suspend fun insertOdometerEntry(entry: OdometerEntryEntity): Long

    @Query("SELECT * FROM odometer_entries WHERE carId = :carId ORDER BY date DESC")
    fun observeOdometerHistory(carId: Long): Flow<List<OdometerEntryEntity>>
}
