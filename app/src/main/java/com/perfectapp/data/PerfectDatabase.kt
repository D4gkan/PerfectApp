package com.perfectapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.perfectapp.data.dao.*
import com.perfectapp.data.entities.*

@Database(
    entities = [
        BodyMeasurementEntity::class,
        SegmentalCompositionEntity::class,
        WaterEntryEntity::class,
        DietGoalEntity::class,
        MealEntryEntity::class,
        AssetEntity::class,
        TransactionEntity::class,
        ExchangeRateEntity::class,
        NetWorthSnapshotEntity::class,
        SubscriptionEntity::class,
        CalendarEventEntity::class,
        CarEntity::class,
        OdometerEntryEntity::class,
        MaintenanceEntity::class,
        ReminderEntity::class
        ,ActivityEntity::class
        ,FuelEntryEntity::class
        ,GoldSettingsEntity::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PerfectDatabase : RoomDatabase() {
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun segmentalCompositionDao(): SegmentalCompositionDao
    abstract fun waterDao(): WaterDao
    abstract fun dietDao(): DietDao
    abstract fun wealthDao(): WealthDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun calendarDao(): CalendarDao
    abstract fun carDao(): CarDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun reminderDao(): ReminderDao
    abstract fun activityDao(): ActivityDao
    abstract fun fuelDao(): FuelDao

    companion object {
        @Volatile private var INSTANCE: PerfectDatabase? = null

        fun getInstance(context: Context): PerfectDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PerfectDatabase::class.java,
                    "perfect_app.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11).build().also { INSTANCE = it }
            }

        /** Releases Room's file handles before a user-approved backup restore. */
        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN time TEXT NOT NULL DEFAULT '00:00:00'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN assetId INTEGER")
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN fundingAssetId INTEGER")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_entries ADD COLUMN description TEXT")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN itemType TEXT NOT NULL DEFAULT 'EVENT'")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS activities (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, date TEXT NOT NULL, time TEXT, notes TEXT, isCompleted INTEGER NOT NULL, calendarEventId INTEGER)")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS fuel_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, carId INTEGER NOT NULL, date TEXT NOT NULL, odometerKm INTEGER, liters REAL NOT NULL, totalCost REAL NOT NULL, currencyCode TEXT NOT NULL, notes TEXT, FOREIGN KEY(carId) REFERENCES cars(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fuel_entries_carId ON fuel_entries(carId)")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE reminders ADD COLUMN carId INTEGER") }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN subscriptionId INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isAutomatic INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fuel_entries ADD COLUMN transactionId INTEGER")
            }
        }
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN reminderMinutesBefore INTEGER")
            }
        }
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assets ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE TABLE IF NOT EXISTS gold_settings (id INTEGER NOT NULL PRIMARY KEY, gramGoldPriceTry REAL NOT NULL, quarterQuantity REAL NOT NULL, halfQuantity REAL NOT NULL, fullQuantity REAL NOT NULL, republicQuantity REAL NOT NULL)")
            }
        }
    }
}
