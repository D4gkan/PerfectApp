package com.perfectapp

import android.content.Context
import com.perfectapp.data.PerfectDatabase
import com.perfectapp.data.repository.*

class AppContainer(context: Context) {
    private val database = PerfectDatabase.getInstance(context)

    val healthRepository = HealthRepository(database, context)
    val waterRepository = WaterRepository(database, context)
    val dietRepository = DietRepository(database)
    val wealthRepository = WealthRepository(database, context)
    val calendarRepository = CalendarRepository(database, context)
    val carRepository = CarRepository(database, context)
    val reminderRepository = ReminderRepository(database, context)
    val settingsRepository = SettingsRepository(context)
    val backupRepository = BackupRepository(context, database)
}
