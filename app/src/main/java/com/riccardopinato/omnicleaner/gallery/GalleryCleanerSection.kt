package com.riccardopinato.omnicleaner.gallery

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus

@Composable
fun GalleryCleanerSection(
    controller: GalleryController,
) {
    val context = LocalContext.current

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

    val dark = isSystemInDarkTheme()
    val page = if (dark) Color(0xFF111722) else Color(0xFFF7F9FD)
    val panel = if (dark) Color(0xFF202938) else Color.White
    val border = if (dark) Color(0xFF344054) else Color(0xFFE3E8F1)
    val ink = if (dark) Color(0xFFF5F7FC) else Color(0xFF18233B)
    val muted = if (dark) Color(0xFFB4BECF) else Color(0xFF68758A)

    // Full-width workspace instead of the old nested Material Card/ModuleScreen.
    // Decisions are docked above app navigation, not scrolled off the screen.
    Column(
        modifier = Modifier.fillMaxSize().background(page),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (dark) Color(0xFF2A3E66) else Color(0xFFE8EEFF),
                            RoundedCornerShape(16.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.omni_nav_gallery),
                        contentDescription = null,
                        tint = if (dark) Color(0xFFAEC6FF) else Color(0xFF3159D6),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column {
                    Text(
                        stringResource(R.string.redesign_media_eyebrow).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (dark) Color(0xFFAEC6FF) else Color(0xFF3159D6),
                    )
                    Text(
                        stringResource(R.string.module_gallery_cleaner),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ink,
                    )
                }
            }
            Text(
                stringResource(R.string.redesign_gallery_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
            )
            if (!state.hasAccess) {
                GalleryCustomButton(
                    text = stringResource(R.string.gallery_allow_media),
                    onClick = { permissionLauncher.launch(GalleryAccess.requiredPermissions()) },
                    modifier = Modifier.fillMaxWidth(),
                    highlighted = true,
                )
            } else {
                if (state.accessHealth.level == GalleryAccessLevel.PARTIAL) {
                    Text(
                        stringResource(R.string.gallery_partial_access),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    GalleryCustomButton(
                        text = stringResource(R.string.gallery_modify_selection),
                        onClick = { permissionLauncher.launch(GalleryAccess.requiredPermissions()) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panel, RoundedCornerShape(18.dp))
                        .border(1.dp, border, RoundedCornerShape(18.dp))
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    GalleryMediaFilter.entries.forEach { filter ->
                        GalleryFilterTab(
                            text = galleryFilterLabel(filter),
                            selected = state.filter == filter,
                            enabled = filter.isAvailableFor(state.accessHealth),
                            modifier = Modifier.weight(1f),
                            onClick = { controller.setFilter(filter) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GalleryCustomButton(
                        text = stringResource(R.string.gallery_sort, gallerySortLabel(state.sortOrder)),
                        onClick = controller::cycleSortOrder,
                        modifier = Modifier.weight(1f),
                    )
                    GalleryCustomButton(
                        text = if (state.items.isEmpty()) stringResource(R.string.gallery_load)
                            else stringResource(R.string.gallery_reload),
                        onClick = { controller.load() },
                        modifier = Modifier.weight(1f),
                        highlighted = true,
                        enabled = !state.loading,
                    )
                }
            }

            if (state.totalReviewedCount > 0) {
                Text(
                    stringResource(R.string.gallery_reviewed_count, state.totalReviewedCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (
                state.hasAccess &&
                !state.loadingNextPage &&
                state.items.isEmpty() &&
                !state.hasMore &&
                state.error == null
            ) {
                // Also useful before the first load: never imply a full-device scan
                // has run when the current filter simply has no loaded results.
                Text(
                    text = stringResource(R.string.gallery_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.currentItem?.let { current ->
                val displayDate = if (current.dateTaken > 0L) {
                    current.formattedDate
                } else {
                    stringResource(R.string.gallery_unknown_date)
                }

                Text(
                    stringResource(
                        R.string.gallery_current_loaded,
                        state.currentIndex + 1,
                        state.items.size,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panel, RoundedCornerShape(25.dp))
                        .border(1.dp, border, RoundedCornerShape(25.dp))
                        .padding(12.dp),
                ) {
                    GalleryMediaPreview(
                        item = current,
                        onKeep = controller::keepCurrent,
                        onTrash = controller::trashCurrent,
                    )
                }
                Text(
                    current.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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

            }

            if (state.loadingNextPage) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.gallery_loading_next), style = MaterialTheme.typography.bodySmall)
            }

            if (state.hasMore && !state.loadingNextPage) {
                GalleryCustomButton(
                    text = stringResource(R.string.gallery_load_more),
                    onClick = controller::loadNextPage,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.items.isNotEmpty() && state.currentItem == null && !state.hasMore) {
                Text(stringResource(R.string.gallery_review_complete))
            }

            if (state.selectedForTrash.isNotEmpty()) {
                Text(
                    stringResource(R.string.gallery_selected_for_trash, state.selectedForTrash.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                GalleryCustomButton(
                    text = stringResource(R.string.gallery_move_android_trash),
                    onClick = { showTrashConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = controller::clearError) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }

        if (state.currentItem != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(panel)
                    .padding(horizontal = 20.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (state.history.isNotEmpty()) {
                    Text(
                        stringResource(R.string.action_undo),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (dark) Color(0xFFAEC6FF) else Color(0xFF3159D6),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button, onClick = controller::undoLastAction)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GalleryCustomButton(
                        text = stringResource(R.string.action_keep),
                        onClick = controller::keepCurrent,
                        modifier = Modifier.weight(1f),
                        positive = true,
                    )
                    GalleryCustomButton(
                        text = stringResource(R.string.action_trash),
                        onClick = controller::trashCurrent,
                        modifier = Modifier.weight(1f),
                        destructive = true,
                    )
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

@Composable
private fun GalleryFilterTab(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val bg = if (selected) {
        if (dark) Color(0xFF38527E) else Color(0xFFE7EEFF)
    } else Color.Transparent
    val ink = when {
        !enabled -> if (dark) Color(0xFF758197) else Color(0xFFA0AABD)
        selected -> if (dark) Color(0xFFEAF0FF) else Color(0xFF234EB9)
        else -> if (dark) Color(0xFFB9C5D7) else Color(0xFF65728A)
    }
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .clickable(enabled = enabled, role = Role.Tab, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = ink,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 2,
        )
    }
}

@Composable
private fun GalleryCustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    highlighted: Boolean = false,
    positive: Boolean = false,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    val dark = isSystemInDarkTheme()
    val background = when {
        !enabled -> if (dark) Color(0xFF29323F) else Color(0xFFE8EBF0)
        positive -> if (dark) Color(0xFF91E8C7) else Color(0xFFB1F0D8)
        destructive -> if (dark) Color(0xFF3A262B) else Color(0xFFFFEBEE)
        highlighted -> if (dark) Color(0xFF577DDF) else Color(0xFF3159D6)
        else -> if (dark) Color(0xFF283344) else Color.White
    }
    val ink = when {
        !enabled -> if (dark) Color(0xFF8290A3) else Color(0xFF8793A8)
        positive -> Color(0xFF0D4434)
        destructive -> if (dark) Color(0xFFFFAEB7) else Color(0xFFAD2F41)
        highlighted -> Color.White
        else -> if (dark) Color(0xFFE8EEF8) else Color(0xFF293857)
    }
    val outline = if (highlighted || positive) background else {
        if (dark) Color(0xFF47566B) else Color(0xFFDFE5EF)
    }
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, outline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = ink,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            textAlign = TextAlign.Center,
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
