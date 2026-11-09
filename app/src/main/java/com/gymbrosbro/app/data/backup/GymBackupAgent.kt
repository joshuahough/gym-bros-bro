package com.gymbrosbro.app.data.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.database.sqlite.SQLiteDatabase
import android.os.ParcelFileDescriptor
import android.util.Log
import com.gymbrosbro.app.data.db.GymDatabase

/**
 * Auto Backup with one extra step (SPEC §9): fold the database's write-ahead
 * log into the main file first, so the backup is a single consistent file
 * rather than a database plus a log copied at slightly different moments.
 *
 * Android runs this without the app's Application class, so it opens the
 * file directly instead of going through Room. What gets backed up is still
 * decided by the backup rules XML.
 */
class GymBackupAgent : BackupAgent() {
    override fun onFullBackup(data: FullBackupDataOutput) {
        checkpoint()
        super.onFullBackup(data)
    }

    private fun checkpoint() {
        val file = getDatabasePath(GymDatabase.NAME)
        if (!file.exists()) return
        // A failed checkpoint must never stop the backup; the log is backed up too.
        runCatching {
            SQLiteDatabase.openDatabase(
                file.path,
                null,
                SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING,
            ).use { db -> db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() } }
        }.onFailure { Log.w(TAG, "Checkpoint before backup failed", it) }
    }

    // Key/value backup isn't used: fullBackupOnly sends everything through onFullBackup.
    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) = Unit
    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) = Unit

    private companion object {
        const val TAG = "GymBackupAgent"
    }
}
