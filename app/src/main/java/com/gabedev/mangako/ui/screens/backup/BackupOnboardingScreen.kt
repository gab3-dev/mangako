package com.gabedev.mangako.ui.screens.backup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.gabedev.mangako.data.local.completeBackupOnboarding
import com.gabedev.mangako.data.local.saveBackupTreeUri
import kotlinx.coroutines.launch

@Composable
fun BackupOnboardingScreen(
    backupManager: BackupManager,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chooseFileError = stringResource(R.string.backup_choose_file_error)
    val chooseFolderError = stringResource(R.string.backup_choose_folder_error)
    val actionFailed = stringResource(R.string.backup_action_failed)
    var parsedBackup by remember { mutableStateOf<ParsedBackup?>(null) }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSkipWarning by remember { mutableStateOf(false) }
    var waitingForDestination by remember { mutableStateOf(false) }

    fun finishWithoutFolder() {
        scope.launch {
            context.completeBackupOnboarding()
            onComplete()
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) {
            showSkipWarning = true
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            busy = true
            errorMessage = null
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                backupManager.createBackup(uri)
                context.saveBackupTreeUri(uri.toString())
                context.completeBackupOnboarding()
            }.onSuccess {
                onComplete()
            }.onFailure {
                errorMessage = chooseFolderError
            }
            busy = false
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            errorMessage = null
            runCatching { backupManager.readBackup(uri) }
                .onSuccess { parsedBackup = it }
                .onFailure { errorMessage = chooseFileError }
            busy = false
        }
    }

    fun restore(mode: RestoreMode) {
        val backup = parsedBackup ?: return
        scope.launch {
            busy = true
            errorMessage = null
            runCatching { backupManager.restore(backup, mode) }
                .onSuccess {
                    parsedBackup = null
                    waitingForDestination = true
                    folderLauncher.launch(null)
                }
                .onFailure {
                    errorMessage = actionFailed
                }
            busy = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.backup_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.backup_welcome_description),
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (busy) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.backup_processing),
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            Button(
                onClick = { fileLauncher.launch(arrayOf("application/json", "text/json", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_have_backup))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    waitingForDestination = true
                    folderLauncher.launch(null)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_no_backup))
            }
            OutlinedButton(
                onClick = { showSkipWarning = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Text(stringResource(R.string.backup_skip))
            }
        }
        errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(top = 20.dp),
                color = MaterialTheme.colorScheme.error,
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

    if (showSkipWarning) {
        AlertDialog(
            onDismissRequest = { showSkipWarning = false },
            title = { Text(stringResource(R.string.backup_skip_title)) },
            text = { Text(stringResource(R.string.backup_skip_description)) },
            confirmButton = {
                Button(onClick = { finishWithoutFolder() }) {
                    Text(stringResource(R.string.backup_skip))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSkipWarning = false
                        if (waitingForDestination) folderLauncher.launch(null)
                    }
                ) {
                    Text(stringResource(R.string.dialog_cancel_button))
                }
            },
        )
    }
}
