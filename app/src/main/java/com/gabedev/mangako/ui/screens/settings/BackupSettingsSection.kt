package com.gabedev.mangako.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabedev.mangako.R
import com.gabedev.mangako.backup.BackupManager
import com.gabedev.mangako.backup.ParsedBackup
import com.gabedev.mangako.backup.RestoreMode
import com.gabedev.mangako.data.local.BackupFrequency
import com.gabedev.mangako.data.local.BackupPreferences
import com.gabedev.mangako.data.local.getBackupPreferences
import com.gabedev.mangako.data.local.saveBackupFrequency
import com.gabedev.mangako.data.local.saveBackupTreeUri
import kotlinx.coroutines.launch

@Composable
fun BackupSettingsSection(
    backupManager: BackupManager,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupSuccess = stringResource(R.string.backup_success)
    val restoreSuccess = stringResource(R.string.backup_restore_success)
    val actionFailed = stringResource(R.string.backup_action_failed)
    val preferences by context.getBackupPreferences().collectAsState(
        initial = BackupPreferences(
            treeUri = null,
            frequency = BackupFrequency.ON_CHANGE,
            onboardingCompleted = true,
            lastBackupAt = null,
            lastBackupError = null,
        )
    )
    var parsedBackup by remember { mutableStateOf<ParsedBackup?>(null) }

    fun showResult(success: Boolean, successMessage: String) {
        Toast.makeText(
            context,
            if (success) successMessage else actionFailed,
            Toast.LENGTH_LONG,
        ).show()
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                backupManager.createBackup(uri)
                context.saveBackupTreeUri(uri.toString())
            }
            showResult(result.isSuccess, backupSuccess)
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { backupManager.readBackup(uri) }
                .onSuccess { parsedBackup = it }
                .onFailure { showResult(false, restoreSuccess) }
        }
    }

    fun restore(mode: RestoreMode) {
        val backup = parsedBackup ?: return
        parsedBackup = null
        scope.launch {
            val result = runCatching { backupManager.restore(backup, mode) }
            if (result.isSuccess) {
                preferences.treeUri?.let {
                    runCatching { backupManager.createBackup(Uri.parse(it)) }
                }
            }
            showResult(result.isSuccess, restoreSuccess)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.backup_settings_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.backup_settings_description),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val folderUri = preferences.treeUri?.let(Uri::parse)
        val folderAvailable = folderUri?.let(backupManager::canUseFolder) == true
        Text(
            text = stringResource(
                if (folderAvailable) R.string.backup_folder_configured
                else R.string.backup_folder_missing
            ),
            color = if (folderAvailable) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.error,
        )
        OutlinedButton(
            onClick = { folderLauncher.launch(folderUri) },
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(
                stringResource(
                    if (folderAvailable) R.string.backup_change_folder
                    else R.string.backup_choose_folder
                )
            )
        }
        Button(
            onClick = {
                scope.launch {
                    val result = runCatching { backupManager.createBackup() }
                    showResult(result.isSuccess, backupSuccess)
                }
            },
            modifier = Modifier.padding(top = 8.dp),
            enabled = folderAvailable,
        ) {
            Text(stringResource(R.string.backup_now))
        }
        OutlinedButton(
            onClick = { fileLauncher.launch(arrayOf("application/json", "text/json", "*/*")) },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.backup_restore))
        }
        preferences.lastBackupAt?.let {
            Text(
                text = stringResource(R.string.backup_last_success, it),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        preferences.lastBackupError?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = stringResource(R.string.backup_last_error, it),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = stringResource(R.string.backup_frequency_title),
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        BackupFrequency.entries.forEach { frequency ->
            BackupFrequencyOption(
                frequency = frequency,
                selected = preferences.frequency == frequency,
                onClick = { scope.launch { context.saveBackupFrequency(frequency) } },
            )
        }
    }

    parsedBackup?.let { backup ->
        AlertDialog(
            onDismissRequest = { parsedBackup = null },
            title = { Text(stringResource(R.string.backup_restore_title)) },
            text = {
                Column {
                    Text(
                        stringResource(
                            R.string.backup_restore_summary,
                            backup.preview.mangaCount,
                            backup.preview.volumeCount,
                            backup.preview.ownedVolumeCount,
                            backup.preview.createdAt,
                        )
                    )
                    Text(
                        text = stringResource(R.string.backup_merge_description),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        text = stringResource(R.string.backup_replace_description),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                Button(onClick = { restore(RestoreMode.MERGE) }) {
                    Text(stringResource(R.string.backup_merge))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { restore(RestoreMode.REPLACE) }) {
                    Text(stringResource(R.string.backup_replace))
                }
            },
        )
    }
}

@Composable
private fun BackupFrequencyOption(
    frequency: BackupFrequency,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val title = when (frequency) {
        BackupFrequency.ON_CHANGE -> R.string.backup_frequency_change
        BackupFrequency.DAILY -> R.string.backup_frequency_daily
        BackupFrequency.WEEKLY -> R.string.backup_frequency_weekly
    }
    val description = when (frequency) {
        BackupFrequency.ON_CHANGE -> R.string.backup_frequency_change_description
        BackupFrequency.DAILY -> R.string.backup_frequency_daily_description
        BackupFrequency.WEEKLY -> R.string.backup_frequency_weekly_description
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
