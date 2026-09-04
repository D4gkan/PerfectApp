package com.perfectapp.ui.screens.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.perfectapp.domain.notifications.NotificationHelper
import com.perfectapp.data.repository.SettingsRepository
import com.perfectapp.data.repository.AppSettings
import com.perfectapp.ui.components.MetricRow
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.SectionHeader

@Composable
fun NotificationsScreen(settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SectionHeader(title = "Daily Summary") }
        item { Text(if (settings.notificationsEnabled) "Daily alerts are enabled. Change this in Settings." else "Daily alerts are disabled in Settings.") }
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "Once a day (around 8:00 AM), Perfect App checks for anything that " +
                            "needs your attention and sends at most one notification per category:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    MetricRow(label = "Renewals", value = "Due within 3 days")
                    MetricRow(label = "Subscriptions", value = "Renewing within 3 days")
                    MetricRow(label = "Vehicle service", value = "Within 300 km")
                    MetricRow(label = "Calendar", value = "Today's next event")
                }
            }
        }

        item { SectionHeader(title = "Test") }
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "Send a sample notification to confirm permissions are set up correctly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            NotificationHelper(context).showNotification(
                                id = NotificationHelper.NOTIFICATION_ID_TEST,
                                title = "Perfect App",
                                text = "Notifications are working."
                            )
                        }
                    ) {
                        Text("Send Test Notification")
                    }
                }
            }
        }
    }
}
