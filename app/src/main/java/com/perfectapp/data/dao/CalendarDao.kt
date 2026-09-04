package com.perfectapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.perfectapp.data.entities.CalendarEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface CalendarDao {
    @Insert
    suspend fun insert(event: CalendarEventEntity): Long

    @Update
    suspend fun update(event: CalendarEventEntity)

    @Delete
    suspend fun delete(event: CalendarEventEntity)

    @Query("SELECT * FROM calendar_events ORDER BY dateTime ASC")
    fun observeAll(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE dateTime >= :now ORDER BY dateTime ASC LIMIT 1")
    fun observeNextEvent(now: LocalDateTime): Flow<CalendarEventEntity?>

    @Query("SELECT * FROM calendar_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CalendarEventEntity?

    @Query("SELECT * FROM calendar_events WHERE dateTime >= :now AND reminderMinutesBefore IS NOT NULL")
    suspend fun futureWithReminders(now: LocalDateTime): List<CalendarEventEntity>
}
