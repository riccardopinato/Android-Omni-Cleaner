package com.riccardopinato.omnicleaner.gallery

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as OutlinedButton
import androidx.compose.material3.Text
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.ui.redesign.OmniWelcomeKind
import com.riccardopinato.omnicleaner.ui.redesign.OmniWelcomePanel
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus

@Composable
fun GalleryCleanerSection(
    controller: GalleryController,
) {
    val context = LocalContext.current

    var state by remember { mutableStateOf(controller.state) }
    var showTrashConfirmation by remember { mutableStateOf(false) }
    var showQueueReview by remember { mutableStateOf(false) }
    var showGalleryOptions by remember { mutableStateOf(false) }

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.gallery_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Show guidance only before the first review, not for completed
            // sessions, queued trash, loading states or read errors.
            if (state.items.isEmpty() && state.totalReviewedCount == 0 &&
                !state.loading && !state.loadingNextPage &&
                !state.hasPersistedUndo && state.pendingTrashItems.isEmpty() &&
                state.error == null
            ) {
                OmniWelcomePanel(
                    kind = OmniWelcomeKind.GALLERY,
                    title = stringResource(R.string.welcome_gallery_title),
                    message = stringResource(R.string.welcome_gallery_body),
                    highlights = listOf(
                        stringResource(R.string.welcome_gallery_step1),
                        stringResource(R.string.welcome_gallery_step2),
                        stringResource(R.string.welcome_gallery_step3),
                    ),
                )
            }

            if (!state.hasAccess) {
                Button(
                    onClick = { permissionLauncher.launch(GalleryAccess.requiredPermissions()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.gallery_allow_media))
                }
            } else {
                if (state.items.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showGalleryOptions = !showGalleryOptions },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.audit_gallery_options))
                    }
                }
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

                if (showGalleryOptions || state.items.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GalleryMediaFilter.entries.forEach { filter ->
                        val selectedFilter = state.filter == filter
                        Button(
                            onClick = { controller.setFilter(filter) },
                            modifier = Modifier.semantics { selected = selectedFilter },
                            enabled = filter.isAvailableFor(state.accessHealth),
                            border = BorderStroke(
                                if (selectedFilter) 2.dp else 1.dp,
                                if (selectedFilter) Color(0xFF9DD8FF)
                                else Color.White.copy(alpha = 0.28f),
                            ),
                        ) {
                            Text((if (selectedFilter) "✓  " else "") + galleryFilterLabel(filter))
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
            }

            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.currentItem?.let { current ->
                Text(
                    stringResource(R.string.audit_review_progress, state.totalReviewedCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val displayDate = if (current.dateTaken > 0L) {
                    current.formattedDate
                } else {
                    stringResource(R.string.gallery_unknown_date)
                }

                GalleryMediaPreview(
                    item = current,
                    canUndo = state.history.isNotEmpty() || state.hasPersistedUndo,
                    onKeep = controller::keepCurrent,
                    onTrash = controller::trashCurrent,
                    onUndo = controller::undoLastAction,
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

                // Controls belong to the photo preview, next to its directional animations.
            }

            // The final reviewed photo has no currentItem; keep Undo available.
            if (state.currentItem == null &&
                (state.history.isNotEmpty() || state.hasPersistedUndo)) {
                OutlinedButton(
                    onClick = controller::undoLastAction,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("↶  " + stringResource(R.string.action_undo))
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
                OutlinedButton(
                    onClick = { showQueueReview = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.audit_gallery_review_queue, state.selectedForTrash.size))
                }
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

    if (showQueueReview) {
        AlertDialog(
            onDismissRequest = { showQueueReview = false },
            title = { Text(stringResource(R.string.audit_gallery_review_title)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(state.selectedForTrash, key = { it.id }) { media ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(media.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    media.uri.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                            TextButton(onClick = {
                                controller.removeFromTrashSelection(media.id)
                                if (state.selectedForTrash.isEmpty()) showQueueReview = false
                            }) {
                                Text(stringResource(R.string.audit_gallery_unmark))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQueueReview = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
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
