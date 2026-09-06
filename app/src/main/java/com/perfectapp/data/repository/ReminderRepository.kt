package com.perfectapp.data.repository

import android.content.Context
import com.perfectapp.data.PerfectDatabase
import com.perfectapp.data.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ReminderRepository(db: PerfectDatabase, private val context: Context? = null) {
    private val dao = db.reminderDao()
    private val scheduler = context?.let { com.perfectapp.domain.notifications.DueDateReminderScheduler(it) }
    val upcoming: Flow<List<ReminderEntity>> = dao.observeUpcoming()
    val all: Flow<List<ReminderEntity>> = dao.observeAll()

    suspend fun add(reminder: ReminderEntity) = dao.insert(reminder).also { id -> scheduler?.scheduleRenewal(reminder.copy(id = id)); context?.let { com.perfectapp.ui.widgets.WidgetRefresh.request(it) } }
    suspend fun update(reminder: ReminderEntity) = dao.update(reminder).also { scheduler?.scheduleRenewal(reminder); context?.let { com.perfectapp.ui.widgets.WidgetRefresh.request(it) } }
    suspend fun delete(reminder: ReminderEntity) = dao.delete(reminder).also { scheduler?.cancelRenewal(reminder.id); context?.let { com.perfectapp.ui.widgets.WidgetRefresh.request(it) } }
    suspend fun rescheduleReminders() = all.first().filter { !it.isCompleted }.forEach { scheduler?.scheduleRenewal(it) }

    fun daysLeft(reminder: ReminderEntity): Long =
        ChronoUnit.DAYS.between(LocalDate.now(), reminder.dueDate)

    /**
     * Marks a reminder/renewal as paid. If it recurs, rolls the due date forward by its
     * interval and leaves it active (so the next cycle immediately shows up as upcoming)
     * rather than marking it permanently completed.
     */
    suspend fun markPaid(reminder: ReminderEntity) {
        if (reminder.isRecurring && reminder.recurrenceMonths != null) {
            val updated = reminder.copy(
                    dueDate = reminder.dueDate.plusMonths(reminder.recurrenceMonths.toLong()),
                    isCompleted = false
                )
            dao.update(updated)
            scheduler?.scheduleRenewal(updated)
        } else {
            dao.update(reminder.copy(isCompleted = true))
            scheduler?.cancelRenewal(reminder.id)
        }
    }

}
