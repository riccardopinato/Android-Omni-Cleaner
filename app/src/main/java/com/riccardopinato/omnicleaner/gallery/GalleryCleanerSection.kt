package com.riccardopinato.omnicleaner.gallery

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus

@Composable
fun GalleryCleanerSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(context, scope) {
        GalleryController(context, scope)
    }

    var state by remember { mutableStateOf(controller.state) }
    var showTrashConfirmation by remember { mutableStateOf(false) }

    DisposableEffect(controller) {
        val dispose = controller.observe { state = it }
        onDispose(dispose)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        controller.refreshAccess(clearLoadedMedia = true)
    }

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val success = result.resultCode == Activity.RESULT_OK
        controller.onTrashResult(success)
        if (success) CleanupEventBus.publishCompleted()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.module_gallery_cleaner), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.gallery_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!state.hasAccess) {
                Button(
                    onClick = { permissionLauncher.launch(GalleryAccess.requiredPermissions()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.gallery_allow_media))
                }
            } else {
                if (state.accessHealth.level == GalleryAccessLevel.PARTIAL) {
                    Text(
                        stringResource(R.string.gallery_partial_access),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { permissionLauncher.launch(GalleryAccess.requiredPermissions()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.gallery_modify_selection))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GalleryMediaFilter.entries.forEach { filter ->
                        if (state.filter == filter) {
                            Button(
                                onClick = { controller.setFilter(filter) },
                                enabled = filter.isAvailableFor(state.accessHealth),
                            ) {
                                Text(galleryFilterLabel(filter))
                            }
                        } else {
                            OutlinedButton(
                                onClick = { controller.setFilter(filter) },
                                enabled = filter.isAvailableFor(state.accessHealth),
                            ) {
                                Text(galleryFilterLabel(filter))
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = controller::cycleSortOrder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.gallery_sort, gallerySortLabel(state.sortOrder)))
                }

                Button(
                    onClick = { controller.load() },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.items.isEmpty()) stringResource(R.string.gallery_load)
                        else stringResource(R.string.gallery_reload),
                    )
                }
            }

            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.currentItem?.let { current ->
                val displayDate = if (current.dateTaken > 0L) {
                    current.formattedDate
                } else {
                    stringResource(R.string.gallery_unknown_date)
                }

                GalleryMediaPreview(
                    item = current,
                    onKeep = controller::keepCurrent,
                    onTrash = controller::trashCurrent,
                )
                Text(current.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append(current.formattedSize)
                        append(" · ")
                        append(displayDate)
                        current.album?.takeIf { it.isNotBlank() }?.let {
                            append(" · ")
                            append(it)
                        }
                        if (current.isVideo && current.formattedDuration.isNotBlank()) {
                            append(" · ")
                            append(current.formattedDuration)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = controller::keepCurrent) {
                        Text(stringResource(R.string.action_keep))
                    }
                    Button(onClick = controller::trashCurrent) {
                        Text(stringResource(R.string.action_trash))
                    }
                    if (state.history.isNotEmpty()) {
                        OutlinedButton(onClick = controller::undoLastAction) {
                            Text(stringResource(R.string.action_undo))
                        }
                    }
                }
            }

            if (state.loadingNextPage) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.gallery_loading_next), style = MaterialTheme.typography.bodySmall)
            }

            if (state.hasMore && !state.loadingNextPage) {
                OutlinedButton(
                    onClick = controller::loadNextPage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.gallery_load_more))
                }
            }

            if (state.items.isNotEmpty() && state.currentItem == null && !state.hasMore) {
                Text(stringResource(R.string.gallery_review_complete))
            }

            if (state.selectedForTrash.isNotEmpty()) {
                Text(
                    stringResource(R.string.gallery_selected_for_trash, state.selectedForTrash.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = { showTrashConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.gallery_move_android_trash))
                }
            }

            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = controller::clearError) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    }

    if (showTrashConfirmation) {
        AlertDialog(
            onDismissRequest = { showTrashConfirmation = false },
            title = { Text(stringResource(R.string.gallery_trash_confirm_title)) },
            text = {
                Text(stringResource(R.string.gallery_trash_confirm_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTrashConfirmation = false
                        val pendingIntent = controller.createTrashRequest() ?: return@TextButton
                        trashLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                        )
                    },
                ) {
                    Text(stringResource(R.string.action_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrashConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun GalleryMediaFilter.isAvailableFor(health: GalleryAccessHealth): Boolean = when (this) {
    GalleryMediaFilter.ALL -> health.canReadImages && health.canReadVideos
    GalleryMediaFilter.IMAGES -> health.canReadImages
    GalleryMediaFilter.VIDEOS -> health.canReadVideos
}


@Composable
private fun galleryFilterLabel(filter: GalleryMediaFilter): String =
    stringResource(
        when (filter) {
            GalleryMediaFilter.ALL -> R.string.gallery_filter_all
            GalleryMediaFilter.IMAGES -> R.string.gallery_filter_images
            GalleryMediaFilter.VIDEOS -> R.string.gallery_filter_videos
        },
    )

@Composable
private fun gallerySortLabel(sortOrder: GallerySortOrder): String =
    stringResource(
        when (sortOrder) {
            GallerySortOrder.DATE_DESC -> R.string.gallery_sort_date_desc
            GallerySortOrder.DATE_ASC -> R.string.gallery_sort_date_asc
            GallerySortOrder.SIZE_DESC -> R.string.gallery_sort_size_desc
            GallerySortOrder.SIZE_ASC -> R.string.gallery_sort_size_asc
            GallerySortOrder.NAME_ASC -> R.string.gallery_sort_name
            GallerySortOrder.ALBUM -> R.string.gallery_sort_album
        },
    )
