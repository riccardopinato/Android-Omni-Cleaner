package com.riccardopinato.omnicleaner.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton
import com.riccardopinato.omnicleaner.ui.theme.OmniSecondaryButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Gallery Pro is exclusively an advisory viewer of the already loaded MediaStore page. */
@Composable
internal fun GalleryProSection(
    controller: GalleryController,
    onOpenReview: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(controller) { mutableStateOf(controller.state) }
    var videoFilter by remember { mutableStateOf(GalleryVideoReviewFilter.ALL) }
    var videoQuery by remember { mutableStateOf("") }
    var analyzing by remember { mutableStateOf(false) }
    var photoSummary by remember { mutableStateOf<GalleryPhotoScanSummary?>(null) }
    var photoError by remember { mutableStateOf(false) }
    var comparedIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    DisposableEffect(controller) {
        val dispose = controller.observe { state = it }
        onDispose(dispose)
    }
    // Previous fingerprints may refer to a different load/filter/permission scope.
    androidx.compose.runtime.LaunchedEffect(state.items) {
        photoSummary = null
        photoError = false
        comparedIds = emptyList()
    }
    val videos = remember(state.items) {
        state.items.filter { it.isVideo }.map {
            GalleryVideoCandidate(
                id = it.id,
                name = it.name,
                bytes = it.sizeBytes,
                durationMs = it.durationMs,
                width = it.width,
                height = it.height,
                album = it.album,
            )
        }
    }
    val videoMatches = remember(videos, videoFilter, videoQuery) {
        filterGalleryVideos(videos, videoFilter, videoQuery)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .testTag("omni_gallery_pro"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.gp_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.gp_loaded_scope, state.items.size),
            style = MaterialTheme.typography.bodySmall,
        )
        if (state.items.isEmpty()) {
            Text(
                stringResource(R.string.gp_load_first),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.hasAccess) {
                OmniActionButton(
                    onClick = { controller.load() },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.gallery_load)) }
            } else {
                OmniSecondaryButton(onClick = onOpenReview, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.gallery_allow_media))
                }
            }
        }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.gp_video_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(R.string.gp_video_scope), style = MaterialTheme.typography.bodySmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GalleryVideoReviewFilter.entries.forEach { choice ->
                        val label = when (choice) {
                            GalleryVideoReviewFilter.ALL -> R.string.gp_video_all
                            GalleryVideoReviewFilter.LARGE_100_MB -> R.string.gp_video_large
                            GalleryVideoReviewFilter.LONG_5_MIN -> R.string.gp_video_long
                            GalleryVideoReviewFilter.UHD_4K -> R.string.gp_video_4k
                        }
                        FilterChip(
                            selected = videoFilter == choice,
                            onClick = { videoFilter = choice },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = videoQuery,
                    onValueChange = { videoQuery = it },
                    label = { Text(stringResource(R.string.gp_video_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("omni_gallery_pro_video_search"),
                )
                Text(
                    stringResource(
                        R.string.gp_video_count,
                        videoMatches.totalMatching,
                        formatMediaSize(videoMatches.bytesMatching),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                videoMatches.results.take(20).forEach { item ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(
                                R.string.gp_video_details,
                                formatMediaSize(item.bytes),
                                item.width,
                                item.height,
                                item.durationMs / 1000L,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (videoMatches.totalMatching > 20) {
                    Text(
                        stringResource(R.string.gp_results_limited),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OmniSecondaryButton(onClick = onOpenReview, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.gallery_open_review))
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.gp_similar_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.gp_similar_explain),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                val availablePhotos = state.items.count { !it.isVideo && it.dateTaken > 0L }
                Text(stringResource(R.string.gp_photo_available, availablePhotos))
                OmniActionButton(
                    onClick = {
                        if (!analyzing) {
                            analyzing = true
                            photoError = false
                            scope.launch {
                                try {
                                    val requested = state.items
                                    val next = GalleryProPhotoScanner.analyze(context, requested)
                                    // Fail closed on a permission, filter or pagination change.
                                    if (controller.state.items == requested &&
                                        controller.state.accessHealth.canReadImages
                                    ) photoSummary = next
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    photoError = true
                                } finally {
                                    analyzing = false
                                }
                            }
                        }
                    },
                    enabled = state.accessHealth.canReadImages && availablePhotos > 1 && !analyzing,
                    modifier = Modifier.fillMaxWidth().testTag("omni_gallery_pro_similar_scan"),
                ) {
                    Text(stringResource(R.string.gp_scan_similar))
                }
                if (analyzing) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (photoError) {
                    Text(stringResource(R.string.gp_photo_error), color = MaterialTheme.colorScheme.error)
                }
                photoSummary?.let { result ->
                    Text(
                        stringResource(
                            R.string.gp_similar_results,
                            result.analyzed,
                            result.eligible,
                            result.candidateGroups.size,
                        ),
                    )
                    val byId = state.items.associateBy { it.id }
                    result.candidateGroups.take(15).forEachIndexed { index, group ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(
                                stringResource(R.string.gp_possible_group, index + 1, group.photoIds.size),
                                fontWeight = FontWeight.SemiBold,
                            )
                            group.photoIds.take(6).forEach { id ->
                                Text(
                                    byId[id]?.name ?: stringResource(R.string.gp_unavailable_photo),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            OmniActionButton(
                                onClick = { comparedIds = group.photoIds },
                                modifier = Modifier.fillMaxWidth()
                                    .testTag("omni_gallery_compare_group_$index"),
                            ) { Text(stringResource(R.string.gp_compare_open)) }
                        }
                    }
                    if (result.candidateGroups.isEmpty()) {
                        Text(stringResource(R.string.gp_none_detected))
                    }
                    if (comparedIds.size >= 2) {
                        val photosToCompare = comparedIds.mapNotNull { byId[it] }
                            .filter { !it.isVideo }
                        if (photosToCompare.size >= 2) {
                            GalleryPhotoComparison(
                                photos = photosToCompare,
                                onClose = { comparedIds = emptyList() },
                            )
                        }
                    }
                }
                OmniActionButton(onClick = onOpenReview, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.gallery_open_review))
                }
                Text(
                    stringResource(R.string.gp_no_auto_delete),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
