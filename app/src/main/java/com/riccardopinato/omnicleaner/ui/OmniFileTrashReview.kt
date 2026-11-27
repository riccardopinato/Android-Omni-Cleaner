package com.riccardopinato.omnicleaner.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.CleanupFileSnapshot
import com.riccardopinato.omnicleaner.cleaner.FileCleanupPreparation
import com.riccardopinato.omnicleaner.cleaner.StorageCleanupEngine
import com.riccardopinato.omnicleaner.cleaner.MediaStoreHelper
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Reusable, conservative selection -> review -> Android Trash / SafeTrash flow.
 * A checkbox never mutates a file. The user approves the exact list first.
 * MediaStore media must use OS Trash confirmation on supported Android versions.
 * Non-media enters reversible app SafeTrash only after snapshot validation.
 */
@Composable
internal fun OmniFileTrashReview(
    selected: List<CleanupFileSnapshot>,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val engine = remember(context.applicationContext) {
        StorageCleanupEngine(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    // Compose-aware resources update when app locale or Configuration changes.
    val resourceLocale = LocalConfiguration.current.locales[0]
    val verifiedTemplate = stringResource(R.string.cleanup_media_verified_summary)
    val movedTemplate = stringResource(R.string.file_trash_moved_summary)
    val movementIncomplete = stringResource(R.string.file_trash_movement_incomplete)
    val cancelled = stringResource(R.string.file_trash_cancelled)
    val requestUnavailable = stringResource(R.string.file_trash_request_unavailable)
    val prepareError = stringResource(R.string.file_trash_prepare_error)
    var confirm by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<FileCleanupPreparation?>(null) }
    var running by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { response ->
        val prepared = pending
        pending = null
        if (response.resultCode == Activity.RESULT_OK && prepared != null) {
            scope.launch {
                try {
                    val verified = MediaStoreHelper.verifyTrashedUris(
                        context.contentResolver, prepared.mediaUris,
                    )
                    val result = engine.movePreparedSnapshotsToSafeTrash(prepared.safeTrashCandidates)
                    message = String.format(
                        resourceLocale, verifiedTemplate,
                        verified.confirmedUris.size, verified.unverifiedUris.size,
                    ) + " · " + String.format(
                        resourceLocale, movedTemplate, result.moved,
                        prepared.blockedFiles.size, prepared.staleFiles.size, result.rejected,
                    )
                    if (result.moved > 0 || verified.confirmedUris.isNotEmpty()) {
                        CleanupEventBus.publishCompleted()
                    }
                    // Do not discard the user's outstanding selection after partial confirmation.
                    if (verified.unverifiedUris.isEmpty()) onFinished()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    message = movementIncomplete
                } finally {
                    running = false
                }
            }
        } else {
            message = cancelled
            running = false
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { confirm = true },
            enabled = selected.isNotEmpty() && !running,
            modifier = Modifier.fillMaxWidth().testTag("omni_file_trash_selected"),
        ) { Text(stringResource(R.string.file_trash_move_selected_count, selected.size)) }
        if (running) CircularProgressIndicator()
        message?.let { Text(it) }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.file_trash_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.file_trash_confirm_body))
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(selected, key = { it.file.absolutePath }) { candidate ->
                            Text(candidate.file.name + " · " + candidate.file.parent.orEmpty())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = false
                        running = true
                        scope.launch {
                            try {
                                val prepared = engine.prepareSnapshots(selected)
                                if (prepared.mediaUris.isNotEmpty()) {
                                    val request = engine.createSystemTrashRequest(prepared)
                                    if (request != null) {
                                        pending = prepared
                                        launcher.launch(
                                            IntentSenderRequest.Builder(request.intentSender).build(),
                                        )
                                    } else {
                                        running = false
                                        message = requestUnavailable
                                    }
                                } else {
                                    val moved = engine.movePreparedSnapshotsToSafeTrash(prepared.safeTrashCandidates)
                                    message = String.format(resourceLocale, movedTemplate, moved.moved, prepared.blockedFiles.size, prepared.staleFiles.size, moved.rejected)
                                    if (moved.moved > 0) {
                                        CleanupEventBus.publishCompleted()
                                        onFinished()
                                    }
                                    running = false
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                running = false
                                message = prepareError
                            }
                        }
                    },
                ) { Text(stringResource(R.string.file_trash_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
