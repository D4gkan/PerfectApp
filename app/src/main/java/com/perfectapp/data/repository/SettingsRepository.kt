package com.perfectapp.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.perfectSettingsDataStore by preferencesDataStore(name = "perfect_settings")

data class AppSettings(
    val displayCurrency: String = "USD",
    val glassMl: Int = 250,
    val smallBottleMl: Int = 500,
    val bigBottleMl: Int = 1000,
    val onboardingComplete: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val calendarNotifications: Boolean = true,
    val renewalNotifications: Boolean = true,
    val subscriptionNotifications: Boolean = true,
    val carNotifications: Boolean = true,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0
)

/** Local-only preferences shared by the dashboard, wealth, and hydration surfaces. */
class SettingsRepository(private val context: Context) {
    private object Keys {
        val Currency = stringPreferencesKey("display_currency")
        val Glass = intPreferencesKey("glass_ml")
        val SmallBottle = intPreferencesKey("small_bottle_ml")
        val BigBottle = intPreferencesKey("big_bottle_ml")
        val OnboardingComplete = androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_complete")
        val NotificationsEnabled = androidx.datastore.preferences.core.booleanPreferencesKey("notifications_enabled")
        val CalendarNotifications = androidx.datastore.preferences.core.booleanPreferencesKey("calendar_notifications")
        val RenewalNotifications = androidx.datastore.preferences.core.booleanPreferencesKey("renewal_notifications")
        val SubscriptionNotifications = androidx.datastore.preferences.core.booleanPreferencesKey("subscription_notifications")
        val CarNotifications = androidx.datastore.preferences.core.booleanPreferencesKey("car_notifications")
        val NotificationHour = intPreferencesKey("notification_hour")
        val NotificationMinute = intPreferencesKey("notification_minute")
    }

    val settings: Flow<AppSettings> = context.perfectSettingsDataStore.data.map { values ->
        AppSettings(
            displayCurrency = values[Keys.Currency] ?: "USD",
            glassMl = values[Keys.Glass] ?: 250,
            smallBottleMl = values[Keys.SmallBottle] ?: 500,
            bigBottleMl = values[Keys.BigBottle] ?: 1000,
            onboardingComplete = values[Keys.OnboardingComplete] ?: false
            ,notificationsEnabled = values[Keys.NotificationsEnabled] ?: true,
            calendarNotifications = values[Keys.CalendarNotifications] ?: true,
            renewalNotifications = values[Keys.RenewalNotifications] ?: true,
            subscriptionNotifications = values[Keys.SubscriptionNotifications] ?: true,
            carNotifications = values[Keys.CarNotifications] ?: true,
            notificationHour = values[Keys.NotificationHour] ?: 8,
            notificationMinute = values[Keys.NotificationMinute] ?: 0
        )
    }

    suspend fun save(settings: AppSettings) {
        require(settings.displayCurrency in setOf("USD", "TRY", "EUR", "GBP"))
        require(settings.glassMl > 0 && settings.smallBottleMl > 0 && settings.bigBottleMl > 0)
        require(settings.notificationHour in 0..23 && settings.notificationMinute in 0..59)
        context.perfectSettingsDataStore.edit { values ->
            values[Keys.Currency] = settings.displayCurrency
            values[Keys.Glass] = settings.glassMl
            values[Keys.SmallBottle] = settings.smallBottleMl
            values[Keys.BigBottle] = settings.bigBottleMl
            values[Keys.OnboardingComplete] = settings.onboardingComplete
            values[Keys.NotificationsEnabled] = settings.notificationsEnabled
            values[Keys.CalendarNotifications] = settings.calendarNotifications
            values[Keys.RenewalNotifications] = settings.renewalNotifications
            values[Keys.SubscriptionNotifications] = settings.subscriptionNotifications
            values[Keys.CarNotifications] = settings.carNotifications
            values[Keys.NotificationHour] = settings.notificationHour
            values[Keys.NotificationMinute] = settings.notificationMinute
        }
    }
}
