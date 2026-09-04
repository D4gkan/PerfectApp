package com.perfectapp.data.repository

import android.content.Context
import com.perfectapp.data.PerfectDatabase
import com.perfectapp.data.entities.BodyMeasurementEntity
import com.perfectapp.data.entities.SegmentalCompositionEntity
import com.perfectapp.data.entities.ActivityEntity
import kotlinx.coroutines.flow.Flow

class HealthRepository(db: PerfectDatabase, private val context: Context? = null) {
    private val dao = db.bodyMeasurementDao()
    private val segDao = db.segmentalCompositionDao()
    private val activityDao = db.activityDao()

    val latestMeasurement: Flow<BodyMeasurementEntity?> = dao.observeLatest()
    val allMeasurements: Flow<List<BodyMeasurementEntity>> = dao.observeAll()

    suspend fun saveMeasurement(measurement: BodyMeasurementEntity): Long = dao.insert(measurement).also { context?.let { com.perfectapp.ui.widgets.WidgetRefresh.request(it) } }
    suspend fun updateMeasurement(measurement: BodyMeasurementEntity) = dao.update(measurement).also { context?.let { com.perfectapp.ui.widgets.WidgetRefresh.request(it) } }
    suspend fun saveSegment(segment: SegmentalCompositionEntity) = segDao.insert(segment)
    fun segmentsFor(measurementId: Long) = segDao.observeForMeasurement(measurementId)
    suspend fun firstMeasurement(): BodyMeasurementEntity? = dao.getFirst()
    val activities = activityDao.observeAll()
    suspend fun saveActivity(activity: ActivityEntity) = activityDao.insert(activity)
    suspend fun updateActivity(activity: ActivityEntity) = activityDao.update(activity)

    fun weightChangeSince(first: BodyMeasurementEntity?, latest: BodyMeasurementEntity?): Double? {
        if (first == null || latest == null) return null
        return latest.weightKg - first.weightKg
    }

    fun fatMassChange(first: BodyMeasurementEntity?, latest: BodyMeasurementEntity?): Double? {
        val f = first?.fatMassKg ?: return null
        val l = latest?.fatMassKg ?: return null
        return l - f
    }

    fun muscleGained(first: BodyMeasurementEntity?, latest: BodyMeasurementEntity?): Double? {
        val f = first?.ffmKg ?: return null
        val l = latest?.ffmKg ?: return null
        return l - f
    }
}
