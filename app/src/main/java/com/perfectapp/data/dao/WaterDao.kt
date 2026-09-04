package com.perfectapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.perfectapp.data.entities.WaterEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WaterDao {
    @Insert
    suspend fun insert(entry: WaterEntryEntity): Long

    @Delete
    suspend fun delete(entry: WaterEntryEntity)

    @Query("SELECT * FROM water_entries WHERE date = :date ORDER BY loggedAt DESC")
    fun observeForDate(date: LocalDate): Flow<List<WaterEntryEntity>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_entries WHERE date = :date")
    fun observeTotalForDate(date: LocalDate): Flow<Int>

    @Query("SELECT * FROM water_entries ORDER BY date DESC, loggedAt DESC")
    fun observeAll(): Flow<List<WaterEntryEntity>>
}
