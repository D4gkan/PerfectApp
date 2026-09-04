package com.perfectapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.perfectapp.data.entities.MaintenanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Insert
    suspend fun insert(item: MaintenanceEntity): Long

    @Update
    suspend fun update(item: MaintenanceEntity)

    @Delete
    suspend fun delete(item: MaintenanceEntity)

    @Query("SELECT * FROM maintenance_items WHERE carId = :carId ORDER BY (completedAt IS NOT NULL) ASC, dueDate ASC")
    fun observeForCar(carId: Long): Flow<List<MaintenanceEntity>>
}
