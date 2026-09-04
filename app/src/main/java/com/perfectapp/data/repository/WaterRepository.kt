package com.perfectapp.data.repository

import android.content.Context
import com.perfectapp.data.PerfectDatabase
import com.perfectapp.data.entities.WaterEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class WaterRepository(db: PerfectDatabase, private val context: Context? = null) {
    private val dao = db.waterDao()
    fun totalForDate(date: LocalDate): Flow<Int> = dao.observeTotalForDate(date)
    fun entriesForDate(date: LocalDate): Flow<List<WaterEntryEntity>> = dao.observeForDate(date)
    val allEntries: Flow<List<WaterEntryEntity>> = dao.observeAll()
    suspend fun addWater(date: LocalDate, amountMl: Int) = dao.insert(WaterEntryEntity(date = date, amountMl = amountMl)).also { context?.let { com.perfectapp.ui.widgets.WidgetRefresh.request(it) } }
    suspend fun removeEntry(entry: WaterEntryEntity) = dao.delete(entry).also { context?.let { com.perfectapp.ui.widgets.WidgetRefresh.request(it) } }
}
