package com.perfectapp.ui.screens.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.perfectapp.PerfectApp

@Composable
fun EventAlarmPermissions() {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        revision++
        (context.applicationContext as PerfectApp).rescheduleDailySummaryWork()
    }
    val exact = remember(revision) { Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }
    val fullScreen = remember(revision) { Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent() }
    Text("At event time rings and vibrates until you press Stop. Sound uses your phone's alarm volume. Advance reminders are notifications.")
    if (!exact) {
        Text("Allow precise timing for alarms to ring reliably while the app is closed. Without it, Android may deliver a regular notification instead.")
        TextButton(onClick = { launcher.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }) { Text("Allow precise alarms") }
    }
    if (!fullScreen && Build.VERSION.SDK_INT >= 34) {
        TextButton(onClick = { launcher.launch(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))) }) { Text("Allow alarm screen on lock screen") }
    }
}
