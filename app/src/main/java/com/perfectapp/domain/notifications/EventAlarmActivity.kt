package com.perfectapp.domain.notifications

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class EventAlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        setContent {
            val alarms by EventAlarmService.active.collectAsState()
            var hadAlarm by remember { mutableStateOf(false) }
            LaunchedEffect(alarms) {
                if (alarms.isNotEmpty()) hadAlarm = true
                else if (hadAlarm) finish()
            }
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Event alarm", style = MaterialTheme.typography.headlineLarge)
                        alarms.forEach { (id, title) ->
                            Spacer(Modifier.height(24.dp))
                            Text(title, style = MaterialTheme.typography.headlineSmall)
                            Button(onClick = { startService(Intent(this@EventAlarmActivity, EventAlarmService::class.java)
                                .setAction(EventAlarmService.STOP).putExtra("id", id)) }, modifier = Modifier.fillMaxWidth()) { Text("Stop alarm") }
                        }
                        if (alarms.isEmpty()) TextButton(onClick = { finish() }) { Text("Close") }
                    }
                }
            }
        }
    }
}
