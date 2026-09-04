package com.perfectapp.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.perfectapp.data.PerfectDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** A local, portable backup of all user-created records plus app preferences. */
class BackupRepository(
    private val context: Context,
    private val database: PerfectDatabase
) {
    private val databaseFile get() = context.getDatabasePath(DATABASE_NAME)
    private val settingsFile get() = File(context.filesDir, "datastore/perfect_settings.preferences_pb")

    fun exportBackup(): Uri {
        // Flush the WAL so copying only the main database file is a consistent snapshot.
        database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
        val directory = File(context.cacheDir, "backups").apply { mkdirs() }
        val output = File(directory, "perfect-app-backup-${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(output)).use { zip ->
            addFile(zip, databaseFile, DATABASE_ENTRY)
            if (settingsFile.exists()) addFile(zip, settingsFile, SETTINGS_ENTRY)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", output)
    }

    /** Replaces local data only after the caller has obtained the user's confirmation. */
    fun restoreBackup(uri: Uri) {
        val staging = File(context.cacheDir, "restore-${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            var hasDatabase = false
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry: ZipEntry?
                    while (zip.nextEntry.also { entry = it } != null) {
                        val name = entry!!.name
                        if (name != DATABASE_ENTRY && name != SETTINGS_ENTRY) continue
                        val destination = File(staging, name)
                        FileOutputStream(destination).use { zip.copyTo(it) }
                        if (name == DATABASE_ENTRY) hasDatabase = true
                    }
                }
            } ?: error("Could not read the selected backup")
            require(hasDatabase) { "This is not a Perfect App backup" }

            PerfectDatabase.closeInstance()
            copyReplace(File(staging, DATABASE_ENTRY), databaseFile)
            File(staging, SETTINGS_ENTRY).takeIf { it.exists() }?.let { copyReplace(it, settingsFile) }
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun addFile(zip: ZipOutputStream, file: File, name: String) {
        require(file.exists()) { "Your local database has not been created yet" }
        zip.putNextEntry(ZipEntry(name))
        FileInputStream(file).use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun copyReplace(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        FileInputStream(source).use { input -> FileOutputStream(destination).use { input.copyTo(it) } }
    }

    companion object {
        private const val DATABASE_NAME = "perfect_app.db"
        private const val DATABASE_ENTRY = "perfect_app.db"
        private const val SETTINGS_ENTRY = "perfect_settings.preferences_pb"
    }
}
