package com.perfectapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.perfectapp.data.entities.FuelEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao interface FuelDao {
    @Insert suspend fun insert(entry: FuelEntryEntity): Long
    @Query("SELECT * FROM fuel_entries WHERE carId = :carId ORDER BY date DESC, id DESC") fun observeForCar(carId: Long): Flow<List<FuelEntryEntity>>
}
