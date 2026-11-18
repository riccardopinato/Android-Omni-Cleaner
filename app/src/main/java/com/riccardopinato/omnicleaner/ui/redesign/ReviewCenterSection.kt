package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.gallery.GalleryController
import com.riccardopinato.omnicleaner.gallery.formatMediaSize
import com.riccardopinato.omnicleaner.ui.CleanerUiState
import com.riccardopinato.omnicleaner.ui.CleanupSelectionUiState
import com.riccardopinato.omnicleaner.ui.ReviewQueueEntry
import com.riccardopinato.omnicleaner.ui.buildReviewCenterSnapshot
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton
import com.riccardopinato.omnicleaner.ui.theme.OmniSecondaryButton

/**
 * Cross-module selection overview, but NOT a new combined delete operation.
 * Android MediaStore Trash and local file-cleaner confirmations have distinct
 * permissions and revalidation; they stay in the original review screens.
 */
@Composable
internal fun ReviewCenterSection(
    state: CleanerUiState,
    selection: CleanupSelectionUiState,
    galleryController: GalleryController,
    onOpenLarge: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenGallery: () -> Unit,
) {
    var galleryState by remember(galleryController) { mutableStateOf(galleryController.state) }
    DisposableEffect(galleryController) {
        val unsubscribe = galleryController.observe { galleryState = it }
        onDispose(unsubscribe)
    }
    val selectedLarge = selection.selectedBigFiles.value
    val selectedDup = selection.selectedDuplicates.value
    val selectedFolders = selection.selectedFolders.value

    val large = state.bigFiles.filter { it.path in selectedLarge }
        .map { ReviewQueueEntry(it.path, it.name, it.sizeBytes) }
    val duplicate = state.duplicates.flatMap { it.files }
        .filter { it.path in selectedDup }
        .map { ReviewQueueEntry(it.path, it.name, it.sizeBytes) }
    val folders = state.folders.filter { it.path in selectedFolders }
    val media = galleryState.selectedForTrash.map {
        ReviewQueueEntry(it.id.toString(), it.name, it.sizeBytes)
    }
    val staleCount = (selectedLarge.size - large.size).coerceAtLeast(0) +
        (selectedDup.size - duplicate.distinctBy { it.id }.size).coerceAtLeast(0) +
        (selectedFolders.size - folders.size).coerceAtLeast(0)

    val preview = buildReviewCenterSnapshot(
        largeFiles = large,
        duplicateFiles = duplicate,
        androidMedia = media,
        selectedFolders = folders.size,
        staleSelections = staleCount,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("omni_review_center"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.review_center_explain),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.review_center_filesystem_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(
                    R.string.review_center_file_summary,
                    preview.filesystemEntries.size,
                    formatMediaSize(preview.filesystemIndexedBytes),
                    preview.selectedFolders,
                ))
                Text(
                    stringResource(R.string.review_center_no_guarantee),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                preview.filesystemEntries.take(12).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            item.title,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            formatMediaSize(item.sizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (preview.staleSelections > 0) {
                    Text(
                        stringResource(R.string.review_center_stale, preview.staleSelections),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (preview.filesystemEntries.isEmpty() && preview.selectedFolders == 0) {
                    Text(
                        stringResource(R.string.review_center_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OmniActionButton(onClick = onOpenLarge, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_big_files))
                }
                OmniSecondaryButton(onClick = onOpenDuplicates, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_duplicates))
                }
                OmniSecondaryButton(onClick = onOpenFolders, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_folders))
                }
                if (selectedLarge.isNotEmpty() || selectedDup.isNotEmpty() ||
                    selectedFolders.isNotEmpty()
                ) {
                    OmniActionButton(
                        onClick = {
                            selection.selectedBigFiles.value = emptySet()
                            selection.selectedDuplicates.value = emptySet()
                            selection.selectedFolders.value = emptySet()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("omni_review_clear_local"),
                    ) { Text(stringResource(R.string.review_center_clear)) }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.review_center_gallery_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(
                    R.string.review_center_gallery_summary,
                    preview.mediaEntries.size,
                    formatMediaSize(preview.mediaQueuedBytes),
                ))
                Text(
                    stringResource(R.string.review_center_gallery_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                preview.mediaEntries.take(12).forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            entry.title,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(formatMediaSize(entry.sizeBytes))
                    }
                }
                if (preview.mediaEntries.isEmpty()) {
                    Text(stringResource(R.string.review_center_none))
                }
                OmniActionButton(
                    onClick = onOpenGallery,
                    modifier = Modifier.fillMaxWidth().testTag("omni_review_open_gallery"),
                ) { Text(stringResource(R.string.review_center_open_gallery)) }
            }
        }
        Text(
            stringResource(R.string.review_center_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
