package com.perfectapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.perfectapp.data.entities.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Insert suspend fun insert(activity: ActivityEntity): Long
    @Update suspend fun update(activity: ActivityEntity)
    @Query("SELECT * FROM activities ORDER BY date DESC, time DESC") fun observeAll(): Flow<List<ActivityEntity>>
}
