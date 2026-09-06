package com.perfectapp.data.repository

import android.content.Context
import com.perfectapp.data.PerfectDatabase
import com.perfectapp.data.entities.CalendarEventEntity
import com.perfectapp.domain.notifications.EventReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class CalendarRepository(db: PerfectDatabase, private val context: Context? = null) {
    private val dao = db.calendarDao()
    private val scheduler = context?.let { EventReminderScheduler(it) }
    val allEvents: Flow<List<CalendarEventEntity>> = dao.observeAll()
    fun nextEvent(): Flow<CalendarEventEntity?> = allEvents.map { events ->
        com.perfectapp.domain.calendar.CalendarOccurrences.upcoming(events, LocalDateTime.now(), 1)
            .firstOrNull()?.let { it.event.copy(dateTime = it.occurrenceDateTime) }
    }
    suspend fun addEvent(event: CalendarEventEntity): Long = dao.insert(event).also { id ->
        scheduler?.schedule(event.copy(id = id)); context?.let { com.perfectapp.ui.widgets.WidgetRefresh.request(it) }
    }
    suspend fun updateEvent(event: CalendarEventEntity) = dao.update(event).also {
        scheduler?.schedule(event); context?.let { com.perfectapp.ui.widgets.WidgetRefresh.request(it) }
    }
    suspend fun deleteEvent(event: CalendarEventEntity) = dao.delete(event).also {
        scheduler?.cancel(event.id); context?.let { com.perfectapp.ui.widgets.WidgetRefresh.request(it) }
    }
    suspend fun eventById(id: Long) = dao.getById(id)
    suspend fun rescheduleReminders() = dao.futureWithReminders(LocalDateTime.now()).forEach { scheduler?.schedule(it) }
}
