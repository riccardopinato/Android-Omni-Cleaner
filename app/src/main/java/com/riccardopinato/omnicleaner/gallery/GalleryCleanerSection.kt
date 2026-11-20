package com.riccardopinato.omnicleaner.gallery

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.riccardopinato.omnicleaner.ui.theme.OmniSecondaryButton as OutlinedButton
import androidx.compose.material3.Text
import com.riccardopinato.omnicleaner.ui.theme.OmniTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.MediaTrashBatchPolicy
import com.riccardopinato.omnicleaner.ui.redesign.OmniWelcomeKind
import com.riccardopinato.omnicleaner.ui.redesign.OmniWelcomePanel
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus
import java.util.Locale

@Composable
fun GalleryCleanerSection(
    controller: GalleryController,
    onOpenReview: (() -> Unit)? = null,
    onOpenInsights: (() -> Unit)? = null,
    reviewOnly: Boolean = false,
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

    // The outer Material Card previously nested a second welcome card and a
    // whole media workspace. Use a quiet product-owned surface: media is content.
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
            if (!reviewOnly) Text(
                stringResource(R.string.gallery_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Show guidance only before the first review, not for completed
            // sessions, queued trash, loading states or read errors.
            if (!reviewOnly && state.items.isEmpty() && state.totalReviewedCount == 0 &&
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

            if (!reviewOnly && onOpenInsights != null) {
                OutlinedButton(
                    onClick = onOpenInsights,
                    modifier = Modifier.fillMaxWidth().testTag("omni_gallery_open_pro"),
                ) { Text(stringResource(R.string.gp_open_insights)) }
            }

            if (!state.hasAccess) {
                Button(
                    onClick = { permissionLauncher.launch(GalleryAccess.requiredPermissions()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.gallery_allow_media))
                }
            } else {
                if (!reviewOnly && state.items.isNotEmpty()) {
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

                if (!reviewOnly && (showGalleryOptions || state.items.isEmpty())) {
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

                // DCL-007 / UIX-STYLE-009: all options remain in the viewport at 360dp
                // and font scale 1.5–2.0; a checked item differs without color alone.
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GalleryMediaFilter.entries.forEach { filter ->
                        val selectedFilter = state.filter == filter
                        val itemModifier = Modifier
                            .testTag("omni_gallery_filter_${filter.name.lowercase(Locale.ROOT)}")
                            .semantics { selected = selectedFilter }
                        if (selectedFilter) {
                            Button(
                                onClick = { controller.setFilter(filter) },
                                modifier = itemModifier,
                                enabled = filter.isAvailableFor(state.accessHealth),
                            ) {
                                Text("✓  " + galleryFilterLabel(filter))
                            }
                        } else {
                            OutlinedButton(
                                onClick = { controller.setFilter(filter) },
                                modifier = itemModifier,
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

                }
            }

            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.currentItem?.let { current ->
                if (reviewOnly) {
                    OmniTinderSessionSummary(state = state)
                } else {
                    Text(
                        stringResource(R.string.audit_review_progress, state.totalReviewedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val displayDate = if (current.dateTaken > 0L) {
                    current.formattedDate
                } else {
                    stringResource(R.string.gallery_unknown_date)
                }

                if (!reviewOnly && onOpenReview != null) {
                    Button(
                        onClick = onOpenReview,
                        modifier = Modifier.fillMaxWidth().testTag("omni_gallery_open_review"),
                    ) {
                        Text(stringResource(R.string.gallery_open_review))
                    }
                } else {
                    GalleryMediaPreview(
                        item = current,
                        canUndo = state.history.isNotEmpty() || state.hasPersistedUndo,
                        onKeep = controller::keepCurrent,
                        onTrash = controller::trashCurrent,
                        onUndo = controller::undoLastAction,
                    )
                }
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
                if (state.selectedForTrash.size > MediaTrashBatchPolicy.MAX_URIS_PER_REQUEST) {
                    Text(
                        stringResource(
                            R.string.gallery_trash_batches,
                            MediaTrashBatchPolicy.MAX_URIS_PER_REQUEST,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
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

    if (showQueueReview) {
        AlertDialog(
            onDismissRequest = { showQueueReview = false },
            modifier = Modifier.testTag("omni_gallery_queue_dialog"),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.audit_gallery_review_title))
                    Text(
                        text = stringResource(
                            R.string.gallery_selected_for_trash,
                            state.selectedForTrash.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp)
                            .testTag("omni_gallery_queue_list"),
                    ) {
                        items(state.selectedForTrash, key = { it.id }) { media ->
                            OmniTrashQueueItem(
                                media = media,
                                onRemove = {
                                    controller.removeFromTrashSelection(media.id)
                                    // This dialog only edits the pending selection.
                                    // It never invokes Android Trash or deletes a file.
                                    if (controller.state.selectedForTrash.isEmpty()) {
                                        showQueueReview = false
                                    }
                                },
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.gallery_queue_android_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
