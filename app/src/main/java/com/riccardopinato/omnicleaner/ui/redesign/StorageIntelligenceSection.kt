package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.SmartCategory
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.cleaner.StorageFileSearchPage
import com.riccardopinato.omnicleaner.cleaner.StorageIntelligenceAnalyzer
import com.riccardopinato.omnicleaner.cleaner.StorageIntelligenceReport
import com.riccardopinato.omnicleaner.cleaner.StorageInventoryRepository
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Read-only Storage Intelligence; the parent Clean route already enforces
 * Deep Cleaning access before this composable can run.
 */
@Composable
internal fun StorageIntelligenceSection(
    onOpenAdvanced: () -> Unit,
    onOpenLargeFiles: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<StorageIntelligenceReport?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var folder by rememberSaveable { mutableStateOf<String?>(null) }
    var minimumBytes by rememberSaveable { mutableStateOf(0L) }
    var visibleLimit by rememberSaveable { mutableStateOf(20) }
    var searchPage by remember { mutableStateOf<StorageFileSearchPage?>(null) }

    LaunchedEffect(report, query, folder, minimumBytes, visibleLimit) {
        val snapshot = report
        searchPage = if (snapshot == null) null else withContext(Dispatchers.Default) {
            snapshot.search(
                text = query,
                folder = folder,
                minimumBytes = minimumBytes,
                limit = visibleLimit,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("omni_storage_intelligence"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.si_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OmniActionButton(
            onClick = {
                if (!loading) {
                    loading = true
                    failed = false
                    scope.launch {
                        try {
                            val root = StorageAccess.primaryRoot()
                            val inventory = StorageInventoryRepository.snapshot(root).inventory
                            val next = withContext(Dispatchers.Default) {
                                StorageIntelligenceAnalyzer().fromInventory(root, inventory)
                            }
                            report = next
                            folder = null
                            visibleLimit = 20
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            failed = true
                        } finally {
                            loading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("omni_storage_intelligence_scan"),
            enabled = !loading,
        ) {
            Text(stringResource(R.string.si_analyze))
        }

        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.si_loading), style = MaterialTheme.typography.bodySmall)
        }
        if (failed) {
            Text(stringResource(R.string.si_error), color = MaterialTheme.colorScheme.error)
        }

        val current = report
        if (current != null) {
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
                        stringResource(R.string.si_map_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(
                            R.string.si_indexed,
                            current.indexedFileCount,
                            formatIntelligenceBytes(current.indexedBytes),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(
                            R.string.si_coverage,
                            current.unreadable,
                            current.excludedDirectories,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(R.string.si_map_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    current.folders.take(10).forEachIndexed { index, share ->
                        val percent = if (current.indexedBytes > 0L) {
                            (share.bytes.toDouble() / current.indexedBytes.toDouble())
                                .toFloat().coerceIn(0f, 1f)
                        } else 0f
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    folder = share.name
                                    visibleLimit = 20
                                }
                                .testTag("omni_storage_folder_$index")
                                .padding(vertical = 3.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    if (share.name.isBlank()) stringResource(R.string.si_root_files)
                                    else share.name,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (folder == share.name) FontWeight.Bold
                                    else FontWeight.Normal,
                                )
                                Text(
                                    formatIntelligenceBytes(share.bytes),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            // Progress is a share of indexed bytes, never a scan percentage.
                            LinearProgressIndicator(
                                progress = { percent },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "${(percent * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(R.string.si_search_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.si_read_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (folder != null) {
                        Text(
                            stringResource(
                                R.string.si_folder_active,
                                if (folder!!.isBlank()) stringResource(R.string.si_root_files)
                                else folder!!,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OmniActionButton(
                            onClick = { folder = null; visibleLimit = 20 },
                            modifier = Modifier.fillMaxWidth().testTag("omni_storage_clear_folder"),
                        ) { Text(stringResource(R.string.si_all_folders)) }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; visibleLimit = 20 },
                        label = { Text(stringResource(R.string.si_search_label)) },
                        placeholder = { Text(stringResource(R.string.si_search_hint)) },
                        modifier = Modifier.fillMaxWidth().testTag("omni_storage_search"),
                        singleLine = true,
                    )
                    Text(stringResource(R.string.si_min_size), style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(
                            0L to R.string.si_size_all,
                            10L * 1024L * 1024L to R.string.si_size_10m,
                            100L * 1024L * 1024L to R.string.si_size_100m,
                        ).forEach { (size, title) ->
                            FilterChip(
                                selected = minimumBytes == size,
                                onClick = { minimumBytes = size; visibleLimit = 20 },
                                label = { Text(stringResource(title)) },
                            )
                        }
                    }
                    searchPage?.let { page ->
                        Text(
                            stringResource(R.string.si_matches, page.matchCount),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (page.matchCount == 0) {
                            Text(stringResource(R.string.si_no_matches))
                        }
                        page.files.forEach { entry ->
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        entry.name,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        formatIntelligenceBytes(entry.bytes),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Text(
                                    entry.relativePath,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (page.matchCount > page.files.size && visibleLimit < 200) {
                            OmniActionButton(
                                onClick = { visibleLimit = (visibleLimit + 20).coerceAtMost(200) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.si_show_more)) }
                        }
                    }
                    OmniActionButton(
                        onClick = onOpenLargeFiles,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.si_open_large)) }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.si_suggestions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.si_suggestion_note),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    current.suggestions.forEach { suggestion ->
                        val title = when (suggestion.category) {
                            SmartCategory.OLD_DOWNLOADS -> R.string.si_old_downloads
                            SmartCategory.INSTALLERS -> R.string.si_installers
                            SmartCategory.ARCHIVES -> R.string.si_archives
                            SmartCategory.SCREENSHOTS -> R.string.si_screenshots
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(title), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(
                                    R.string.si_suggestion_count,
                                    suggestion.fileCount,
                                    formatIntelligenceBytes(suggestion.bytes),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (current.suggestions.isEmpty()) {
                        Text(stringResource(R.string.si_no_suggestions))
                    }
                    OmniActionButton(
                        onClick = onOpenAdvanced,
                        modifier = Modifier.fillMaxWidth().testTag("omni_storage_open_categories"),
                    ) { Text(stringResource(R.string.si_open_categories)) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun formatIntelligenceBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}
