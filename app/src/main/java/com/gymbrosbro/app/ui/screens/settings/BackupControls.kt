package com.gymbrosbro.app.ui.screens.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.data.backup.BackupFile
import com.gymbrosbro.app.data.backup.BackupFormatException
import com.gymbrosbro.app.data.backup.BackupRepository
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.ConfirmDialog
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelDialog
import com.gymbrosbro.app.ui.display
import com.gymbrosbro.app.ui.localDate
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Picker types for "open a backup". Drive sometimes labels JSON as plain text or binary. */
private val BACKUP_MIME_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")

class BackupViewModel(
    private val backup: BackupRepository,
    /** Restores and exports finish even if the user leaves the screen. */
    private val appScope: CoroutineScope,
) : ViewModel() {
    private val _pending = MutableStateFlow<BackupFile?>(null)
    /** A file that was read and checked, waiting for the user to confirm the restore. */
    val pending: StateFlow<BackupFile?> = _pending.asStateFlow()

    private val _message = MutableStateFlow<Pair<String, String>?>(null)
    /** Title and body of a result to show, or null. */
    val message: StateFlow<Pair<String, String>?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun export(resolver: ContentResolver, uri: Uri) = work {
        try {
            val out = resolver.openOutputStream(uri, "w") ?: error("no stream")
            out.use { backup.export(it) }
            _message.value = "SAVED" to "Backup saved. Keep it somewhere safe, like Google Drive."
        } catch (e: Exception) {
            _message.value = "COULDN'T SAVE" to "The backup couldn't be written there. Try another folder."
        }
    }

    fun read(resolver: ContentResolver, uri: Uri) = work {
        try {
            val input = resolver.openInputStream(uri) ?: error("no stream")
            _pending.value = input.use { backup.read(it) }
        } catch (e: BackupFormatException) {
            _message.value = "CAN'T RESTORE" to (e.message ?: "")
        } catch (e: Exception) {
            _message.value = "CAN'T RESTORE" to "That file couldn't be opened."
        }
    }

    fun confirmRestore() {
        val file = _pending.value ?: return
        _pending.value = null
        work {
            try {
                backup.restore(file)
                _message.value = "RESTORED" to "Welcome back! ${plural(file.workoutCount, "workout")} restored."
            } catch (e: Exception) {
                _message.value = "CAN'T RESTORE" to
                    "That backup doesn't fit this version of the app. Nothing was changed."
            }
        }
    }

    fun cancelRestore() {
        _pending.value = null
    }

    fun dismissMessage() {
        _message.value = null
    }

    private fun work(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        appScope.launch {
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }
}

private fun plural(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"

/**
 * Export and restore buttons, with their file pickers and dialogs (SPEC §9).
 * Onboarding shows only the restore half.
 */
@Composable
fun BackupControls(
    modifier: Modifier = Modifier,
    showExport: Boolean = true,
    restoreLabel: String = "RESTORE",
) {
    val viewModel = appViewModel { c, _ -> BackupViewModel(c.backupRepository, c.appScope) }
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.export(resolver, uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.read(resolver, uri)
    }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showExport) {
            PixelButton(
                if (busy) "WORKING..." else "EXPORT",
                onClick = { exportLauncher.launch(BackupRepository.fileName()) },
                enabled = !busy,
                small = true,
                modifier = Modifier.weight(1f),
            )
        }
        PixelButton(
            if (busy && !showExport) "WORKING..." else restoreLabel,
            onClick = { importLauncher.launch(BACKUP_MIME_TYPES) },
            enabled = !busy,
            small = true,
            color = GbbColors.Steel,
            contentColor = GbbColors.Chalk,
            modifier = Modifier.weight(1f),
        )
    }

    pending?.let { file ->
        ConfirmDialog(
            title = "RESTORE BACKUP?",
            message = "From ${file.exportedAt.localDate().display()}: ${plural(file.workoutCount, "workout")}. " +
                "This replaces everything in the app now, and can't be undone.",
            confirmText = "RESTORE",
            onConfirm = viewModel::confirmRestore,
            onDismiss = viewModel::cancelRestore,
            destructive = true,
        )
    }

    message?.let { (title, body) ->
        PixelDialog(title, onDismiss = viewModel::dismissMessage) {
            Text(body, style = MaterialTheme.typography.bodyLarge, color = GbbColors.Chalk)
            PixelButton("OK", onClick = viewModel::dismissMessage, modifier = Modifier.fillMaxWidth())
        }
    }
}
