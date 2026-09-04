package com.perfectapp.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.perfectapp.data.repository.BackupRepository
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.SectionHeader

@Composable
fun BackupScreen(repository: BackupRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    var backupToRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) backupToRestore = uri
    }

    backupToRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = { Text("Replace all local data?") },
            text = { Text("This will replace every record and setting with the selected backup. The app will restart afterwards.") },
            confirmButton = {
                Button(onClick = {
                    runCatching { repository.restoreBackup(uri) }
                        .onSuccess {
                            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            if (launch != null) context.startActivity(launch)
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                        .onFailure { message = it.message ?: "Could not restore this backup" }
                    backupToRestore = null
                }) { Text("Restore") }
            },
            dismissButton = { OutlinedButton(onClick = { backupToRestore = null }) { Text("Cancel") } }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Backup & Restore") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { SectionHeader(title = "Your data") }
            item { PremiumCard(Modifier.fillMaxWidth()) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Create a backup", style = MaterialTheme.typography.titleMedium)
                Text("Saves all local records and settings in a portable ZIP file.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = {
                    runCatching { repository.exportBackup() }
                        .onSuccess { uri ->
                            context.startActivity(Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }.let { Intent.createChooser(it, "Save Perfect App backup") })
                        }
                        .onFailure { message = it.message ?: "Could not create a backup" }
                }) { Text("Create backup") }
            } } }
            item { PremiumCard(Modifier.fillMaxWidth()) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Restore a backup", style = MaterialTheme.typography.titleMedium)
                Text("Restoring permanently replaces this device's current local data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { restorePicker.launch(arrayOf("application/zip", "application/octet-stream")) }) { Text("Choose backup") }
            } } }
            message?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }
}
