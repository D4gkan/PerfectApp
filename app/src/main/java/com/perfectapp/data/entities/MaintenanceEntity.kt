package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "maintenance_items",
    foreignKeys = [ForeignKey(
        entity = CarEntity::class,
        parentColumns = ["id"],
        childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("carId")]
)
data class MaintenanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val title: String,
    val dueAtOdometerKm: Long? = null,
    val dueDate: LocalDate? = null,
    val completedAt: LocalDate? = null,
    val notes: String? = null
) {
    val isCompleted: Boolean get() = completedAt != null
}
