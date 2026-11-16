package com.riccardopinato.omnicleaner.ui.redesign

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.BuildConfig
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.BigFileSizeFilter
import com.riccardopinato.omnicleaner.cleaner.DeepStorageAccessStatus
import com.riccardopinato.omnicleaner.cleaner.FolderStatus
import com.riccardopinato.omnicleaner.cleaner.PhysicalPerformanceBenchmarkResult
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.cleaner.ThresholdUnit
import com.riccardopinato.omnicleaner.exif.ExifCleanerSection
import com.riccardopinato.omnicleaner.gallery.GalleryCleanerSection
import com.riccardopinato.omnicleaner.monetization.AdManager
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus
import com.riccardopinato.omnicleaner.monetization.MonetizationSection
import com.riccardopinato.omnicleaner.monetization.PremiumEntitlementStore
import com.riccardopinato.omnicleaner.monetization.findActivity
import com.riccardopinato.omnicleaner.monetization.rememberMonetizationRuntime
import com.riccardopinato.omnicleaner.ui.CleanerController
import com.riccardopinato.omnicleaner.ui.COMMON_FILE_EXTENSION_FILTERS
import com.riccardopinato.omnicleaner.ui.OmniCleanerViewModel
import com.riccardopinato.omnicleaner.ui.CleanerUiState
import com.riccardopinato.omnicleaner.ui.LanguageSelectorSection
import com.riccardopinato.omnicleaner.ui.MaintenanceSection
import com.riccardopinato.omnicleaner.ui.ManualMaintenanceCard
import com.riccardopinato.omnicleaner.ui.SafeTrashSection
import com.riccardopinato.omnicleaner.ui.SmartCategoriesSection
import com.riccardopinato.omnicleaner.ui.StorageCleanupSection
import com.riccardopinato.omnicleaner.ui.theme.OmniBlue
import com.riccardopinato.omnicleaner.ui.theme.OmniMint
import com.riccardopinato.omnicleaner.ui.theme.OmniOrange
import com.riccardopinato.omnicleaner.ui.theme.OmniPurple
import com.riccardopinato.omnicleaner.ui.theme.OmniRose
import java.util.Locale

/** Dedicated tool routes. The activity-scoped CleanerController owns scan results. */
internal enum class CleanerToolRoute(val labelRes: Int) {
    OVERVIEW(R.string.scan_overview),
    LARGE(R.string.scan_big_files),
    DUPLICATES(R.string.scan_duplicates),
    FOLDERS(R.string.scan_folders),
    FILTERS(R.string.redesign_filters_title),
    TRASH(R.string.module_safe_trash),
    ADVANCED(R.string.audit_advanced_maintenance),
}

@Composable
internal fun CleanHubScreen(
    state: CleanerUiState,
    controller: CleanerController,
    modifier: Modifier,
    onOpenTool: (CleanerToolRoute) -> Unit,
    onEnableDeepClean: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OmniSectionHeader(
                eyebrow = stringResource(R.string.redesign_clean_eyebrow),
                title = stringResource(R.string.nav_clean),
                subtitle = stringResource(R.string.redesign_clean_subtitle),
                modifier = Modifier.padding(top = 18.dp),
            )
        }
        if (!state.hasBroadAccess) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Text(stringResource(R.string.redesign_deep_locked_title),
                            style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.redesign_deep_locked_body),
                            style = MaterialTheme.typography.bodySmall)
                        if (state.storageCapability.canRequestDeepAccess) {
                            Button(onClick = onEnableDeepClean) {
                                Text(stringResource(R.string.grant_deep_access))
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(23.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(
                        text = stringResource(R.string.scan_tools),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    // Compact dashboard: no accordion or seven-button timeline.
                    CleanerToolRoute.entries.chunked(2).forEach { group ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            group.forEach { tool ->
                                Button(
                                    onClick = { onOpenTool(tool) },
                                    modifier = Modifier.weight(1f).testTag(
                                        "omni_clean_tool_${tool.name.lowercase(Locale.ROOT)}"
                                    ),
                                ) {
                                    Text(stringResource(tool.labelRes))
                                }
                            }
                            if (group.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        if (state.overview == null && !state.foldersScanned &&
            !state.bigFilesScanned && !state.duplicatesScanned &&
            !state.scanning && state.error == null
        ) {
            item { CleanWelcomePanel() }
        }
        if (state.scanning) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(state.currentOperation.orEmpty())
                        Button(onClick = controller::cancelScan) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
        state.error?.let { error ->
            item { Text(error, modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.error) }
        }
        state.overview?.let { overview ->
            item {
                ResultSummaryCard(
                    title = stringResource(R.string.module_storage_overview),
                    subtitle = stringResource(
                        R.string.overview_summary,
                        overview.files,
                        formatCleanerBytes(overview.logicalBytes),
                    ),
                    accent = OmniBlue,
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
internal fun CleanDetailScreen(
    tool: CleanerToolRoute,
    state: CleanerUiState,
    controller: CleanerController,
    modifier: Modifier,
    selectionState: com.riccardopinato.omnicleaner.ui.CleanupSelectionUiState,
    onEnableDeepClean: () -> Unit,
) {
    val onCleanupFinished = {
        controller.refreshAccessAndStorage()
        controller.scanOverview()
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OmniSectionHeader(
                eyebrow = stringResource(R.string.nav_clean),
                title = stringResource(tool.labelRes),
                subtitle = stringResource(R.string.redesign_clean_subtitle),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (tool != CleanerToolRoute.TRASH && !state.hasBroadAccess) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(stringResource(R.string.redesign_deep_locked_body))
                        if (state.storageCapability.canRequestDeepAccess) {
                            Button(onClick = onEnableDeepClean) {
                                Text(stringResource(R.string.grant_deep_access))
                            }
                        }
                    }
                }
            }
        } else {
            if (tool in listOf(CleanerToolRoute.OVERVIEW, CleanerToolRoute.LARGE,
                    CleanerToolRoute.DUPLICATES, CleanerToolRoute.FOLDERS)) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (state.scanning) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text(state.currentOperation.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall)
                                Button(onClick = controller::cancelScan) {
                                    Text(stringResource(R.string.cancel))
                                }
                            } else {
                                val scanAction: () -> Unit = when (tool) {
                                    CleanerToolRoute.OVERVIEW -> ({ controller.scanOverview() })
                                    CleanerToolRoute.LARGE -> ({ controller.scanBigFiles() })
                                    CleanerToolRoute.DUPLICATES -> ({ controller.scanDuplicates() })
                                    CleanerToolRoute.FOLDERS -> ({ controller.scanFolders() })
                                    else -> ({ controller.scanOverview() })
                                }
                                Button(
                                    onClick = scanAction,
                                    modifier = Modifier.fillMaxWidth().testTag(
                                        "omni_scan_${tool.name.lowercase(Locale.ROOT)}"
                                    ),
                                ) {
                                    Text(stringResource(tool.labelRes))
                                }
                            }
                        }
                    }
                }
            }
            when (tool) {
                CleanerToolRoute.OVERVIEW -> {
                    state.overview?.let { overview ->
                        item {
                            ResultSummaryCard(
                                title = stringResource(R.string.module_storage_overview),
                                subtitle = stringResource(
                                    R.string.overview_summary,
                                    overview.files,
                                    formatCleanerBytes(overview.logicalBytes),
                                ),
                                accent = OmniBlue,
                            )
                        }
                        items(overview.byCategory.take(20), key = { "overview:" + it.path }) {
                            ResultLine(it.path, formatCleanerBytes(it.bytes))
                        }
                    }
                }
                CleanerToolRoute.LARGE -> {
                    if (state.bigFilesScanned) {
                        item {
                            ResultSummaryCard(
                                title = stringResource(R.string.module_large_files),
                                subtitle = stringResource(
                                    R.string.big_files_summary,
                                    state.visibleBigFiles.size,
                                ),
                                accent = OmniOrange,
                            )
                        }
                    }
                    if (state.visibleBigFiles.isNotEmpty()) {
                        item {
                            StorageCleanupSection(
                                folders = emptyList(),
                                bigFiles = state.visibleBigFiles,
                                duplicateGroups = emptyList(),
                                onCleanupFinished = onCleanupFinished,
                                selectionState = selectionState,
                            )
                        }
                    }
                }
                CleanerToolRoute.DUPLICATES -> {
                    if (state.duplicatesScanned) {
                        item {
                            ResultSummaryCard(
                                title = stringResource(R.string.module_duplicates),
                                subtitle = stringResource(
                                    R.string.duplicates_summary,
                                    state.visibleDuplicates.size,
                                    formatCleanerBytes(state.visibleDuplicates.sumOf { it.totalWasteBytes }),
                                ),
                                accent = OmniRose,
                            )
                        }
                    }
                    if (state.visibleDuplicates.isNotEmpty()) {
                        item {
                            StorageCleanupSection(
                                folders = emptyList(),
                                bigFiles = emptyList(),
                                duplicateGroups = state.visibleDuplicates,
                                onCleanupFinished = onCleanupFinished,
                                selectionState = selectionState,
                            )
                        }
                    }
                }
                CleanerToolRoute.FOLDERS -> {
                    if (state.foldersScanned) {
                        item {
                            ResultSummaryCard(
                                title = stringResource(R.string.module_folders),
                                subtitle = stringResource(
                                    R.string.folder_summary,
                                    state.visibleFolders.size,
                                    state.visibleFolders.count {
                                        it.status == FolderStatus.COMPLETELY_EMPTY
                                    },
                                ),
                                accent = OmniMint,
                            )
                        }
                    }
                    if (state.visibleFolders.isNotEmpty()) {
                        item {
                            StorageCleanupSection(
                                folders = state.visibleFolders,
                                bigFiles = emptyList(),
                                duplicateGroups = emptyList(),
                                onCleanupFinished = onCleanupFinished,
                                selectionState = selectionState,
                            )
                        }
                    }
                }
                CleanerToolRoute.FILTERS -> item {
                    CleanFiltersCard(state = state, controller = controller)
                }
                CleanerToolRoute.TRASH -> item {
                    SafeTrashSection()
                }
                CleanerToolRoute.ADVANCED -> {
                    item { SmartCategoriesSection(onCleanupFinished = onCleanupFinished) }
                    item { MaintenanceSection() }
                    item { ManualMaintenanceCard() }
                    if (BuildConfig.DEBUG) {
                        item { PerformanceBenchmarkCard(state = state, controller = controller) }
                    }
                }
            }
            state.error?.let { error ->
                item { Text(error, modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.error) }
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
internal fun CleanScreen(
    state: CleanerUiState,
    controller: CleanerController,
    modifier: Modifier,
    onEnableDeepClean: () -> Unit,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showSafeTrash by rememberSaveable { mutableStateOf(false) }
    var showAdvancedMaintenance by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OmniSectionHeader(
                eyebrow = stringResource(R.string.redesign_clean_eyebrow),
                title = stringResource(R.string.nav_clean),
                subtitle = stringResource(R.string.redesign_clean_subtitle),
                modifier = Modifier.padding(top = 18.dp),
            )
        }

        if (!state.hasBroadAccess) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            stringResource(R.string.redesign_deep_locked_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.redesign_deep_locked_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.storageCapability.canRequestDeepAccess) {
                            Button(onClick = onEnableDeepClean) {
                                Text(stringResource(R.string.grant_deep_access))
                            }
                        }
                    }
                }
            }
            // Permission request remains above the guide: do not hide the real CTA.
            item { CleanWelcomePanel() }
        } else {
            item { CleanActionsCard(state = state, controller = controller) }
            // Before the first scan, explain the opt-in scan/review/confirm flow.
            if (state.overview == null && !state.foldersScanned &&
                !state.bigFilesScanned && !state.duplicatesScanned &&
                !state.scanning && state.error == null
            ) {
                item { CleanWelcomePanel() }
            }
            item {
                OutlinedButton(
                    onClick = { showFilters = !showFilters },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) {
                    Text(stringResource(R.string.redesign_filters_title) + if (showFilters) " −" else " +")
                }
            }
            if (showFilters) {
                item { CleanFiltersCard(state = state, controller = controller) }
            }

            state.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Text(
                            error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            state.overview?.let { overview ->
                item {
                    ResultSummaryCard(
                        title = stringResource(R.string.module_storage_overview),
                        subtitle = stringResource(
                            R.string.overview_summary,
                            overview.files,
                            formatCleanerBytes(overview.logicalBytes),
                        ),
                        accent = OmniBlue,
                    )
                }
                items(overview.byCategory.take(10), key = { "cat:" + it.path }) {
                    ResultLine(it.path, formatCleanerBytes(it.bytes))
                }
            }

            if (state.visibleBigFiles.isNotEmpty()) {
                item {
                    ResultSummaryCard(
                        title = stringResource(R.string.module_large_files),
                        subtitle = stringResource(
                            R.string.big_files_summary,
                            state.visibleBigFiles.size,
                        ),
                        accent = OmniOrange,
                    )
                }
            }

            if (state.visibleDuplicates.isNotEmpty()) {
                item {
                    val waste = state.visibleDuplicates.sumOf { it.totalWasteBytes }
                    ResultSummaryCard(
                        title = stringResource(R.string.module_duplicates),
                        subtitle = stringResource(
                            R.string.duplicates_summary,
                            state.visibleDuplicates.size,
                            formatCleanerBytes(waste),
                        ),
                        accent = OmniRose,
                    )
                }
            }

            if (
                state.duplicatesScanned &&
                state.visibleDuplicates.isEmpty() &&
                !state.scanning
            ) {
                item {
                    ResultSummaryCard(
                        title = stringResource(R.string.module_duplicates),
                        subtitle = stringResource(
                            R.string.duplicates_none_found,
                            state.fileExtensionFilter?.let { ".$it" }
                                ?: stringResource(R.string.file_type_filter_all),
                        ),
                        accent = OmniRose,
                    )
                }
            }

            if (state.visibleFolders.isNotEmpty()) {
                item {
                    val empty = state.visibleFolders.count {
                        it.status == FolderStatus.COMPLETELY_EMPTY
                    }
                    ResultSummaryCard(
                        title = stringResource(R.string.module_folders),
                        subtitle = stringResource(
                            R.string.folder_summary,
                            state.visibleFolders.size,
                            empty,
                        ),
                        accent = OmniMint,
                    )
                }
            }

            item {
                StorageCleanupSection(
                    folders = state.visibleFolders,
                    bigFiles = state.visibleBigFiles,
                    duplicateGroups = state.visibleDuplicates,
                    onCleanupFinished = {
                        controller.refreshAccessAndStorage()
                        controller.scanOverview()
                    },
                )
            }

            item {
                OutlinedButton(
                    onClick = { showAdvancedMaintenance = !showAdvancedMaintenance },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) {
                    Text(
                        stringResource(R.string.audit_advanced_maintenance) +
                            if (showAdvancedMaintenance) " −" else " +"
                    )
                }
            }
            if (showAdvancedMaintenance) {
                item {
                    SmartCategoriesSection(
                        onCleanupFinished = {
                            controller.refreshAccessAndStorage()
                            controller.scanOverview()
                        },
                    )
                }
                item { MaintenanceSection() }
                item { ManualMaintenanceCard() }
                // Benchmark is a developer tool, not a primary cleaning action.
                if (BuildConfig.DEBUG) {
                    item { PerformanceBenchmarkCard(state = state, controller = controller) }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { showSafeTrash = !showSafeTrash },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) {
                Text(stringResource(R.string.module_safe_trash) + if (showSafeTrash) " −" else " +")
            }
        }
        if (showSafeTrash) {
            item { SafeTrashSection() }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun CleanWelcomePanel() {
    OmniWelcomePanel(
        kind = OmniWelcomeKind.CLEAN,
        title = stringResource(R.string.welcome_clean_title),
        message = stringResource(R.string.welcome_clean_body),
        highlights = listOf(
            stringResource(R.string.welcome_clean_step1),
            stringResource(R.string.welcome_clean_step2),
            stringResource(R.string.welcome_clean_step3),
        ),
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun CleanActionsCard(
    state: CleanerUiState,
    controller: CleanerController,
) {
    var showMoreTools by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.scan_tools), style = MaterialTheme.typography.titleLarge)
            if (state.scanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    state.currentOperation.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.duplicateProgressTotal > 0) {
                    Text(
                        stringResource(
                            R.string.duplicate_scan_progress,
                            state.duplicateProgressProcessed,
                            state.duplicateProgressTotal,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (state.indexedFilesChecked > 0) {
                    Text(
                        stringResource(
                            R.string.scan_indexing_files_count,
                            state.indexedFilesChecked,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = controller::cancelScan) {
                    Text(stringResource(R.string.cancel))
                }
            } else {
                Button(onClick = controller::scanOverview, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_overview))
                }
                OutlinedButton(onClick = { controller.scanBigFiles() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_big_files))
                }
                OutlinedButton(onClick = controller::scanDuplicates, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_duplicates))
                }
                TextButton(
                    onClick = { showMoreTools = !showMoreTools },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(
                        if (showMoreTools) R.string.audit_hide_tools else R.string.audit_more_tools
                    ))
                }
                if (showMoreTools) {
                    OutlinedButton(onClick = controller::scanFolders, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.scan_folders))
                    }
                    OutlinedButton(
                        onClick = controller::refreshPersistentIndex,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.refresh_storage_index))
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceBenchmarkCard(
    state: CleanerUiState,
    controller: CleanerController,
) {
    val context = LocalContext.current
    var reportCopied by remember { mutableStateOf(false) }
    val result = state.performanceBenchmark

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                stringResource(R.string.performance_lab_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.performance_lab_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                stringResource(R.string.performance_lab_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )

            OutlinedButton(
                onClick = {
                    reportCopied = false
                    controller.runPhysicalPerformanceBenchmark()
                },
                enabled = !state.scanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.performance_benchmark_run))
            }

            if (result != null) {
                PerformanceBenchmarkResultBody(result)

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText(
                                "Omni Cleaner performance report",
                                result.toReport(),
                            ),
                        )
                        reportCopied = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.performance_benchmark_copy_report))
                }

                if (reportCopied) {
                    Text(
                        stringResource(R.string.performance_benchmark_report_copied),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceBenchmarkResultBody(
    result: PhysicalPerformanceBenchmarkResult,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            stringResource(
                R.string.performance_benchmark_summary,
                result.coldScanMillis,
                result.warmScanMillis,
                result.hotMemoryScanMillis,
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(
                R.string.performance_benchmark_files,
                result.filesTotal,
                formatCleanerBytes(result.benchmarkBytes),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            stringResource(
                R.string.performance_benchmark_persistent_hit,
                if (result.warmPersistentHit) {
                    stringResource(R.string.performance_benchmark_hit)
                } else {
                    stringResource(R.string.performance_benchmark_miss)
                },
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        result.hashRuns.sortedBy { it.workers }.forEach { run ->
            Text(
                stringResource(
                    R.string.performance_benchmark_hash_summary,
                    run.workers,
                    run.elapsedMillis,
                    run.mebibytesPerSecond,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            stringResource(
                R.string.performance_benchmark_device_summary,
                result.device,
                result.sdk,
                result.availableProcessors,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CleanFiltersCard(
    state: CleanerUiState,
    controller: CleanerController,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.redesign_filters_title), style = MaterialTheme.typography.titleMedium)

            var searchMode by rememberSaveable { mutableStateOf(0) }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                listOf(
                    R.string.module_folders,
                    R.string.module_large_files,
                    R.string.module_duplicates,
                ).forEachIndexed { index, label ->
                    FilterChip(
                        selected = searchMode == index,
                        onClick = { searchMode = index },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            if (searchMode != 0) {
            var extensionMenuExpanded by rememberSaveable { mutableStateOf(false) }
            Text(
                stringResource(R.string.file_type_filter_title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { extensionMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        state.fileExtensionFilter?.let { ".$it" }
                            ?: stringResource(R.string.file_type_filter_all),
                    )
                }
                DropdownMenu(
                    expanded = extensionMenuExpanded,
                    onDismissRequest = { extensionMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.file_type_filter_all)) },
                        onClick = {
                            controller.updateFileExtensionFilter(null)
                            extensionMenuExpanded = false
                        },
                    )
                    COMMON_FILE_EXTENSION_FILTERS.forEach { extension ->
                        DropdownMenuItem(
                            text = { Text(".$extension") },
                            onClick = {
                                controller.updateFileExtensionFilter(extension)
                                extensionMenuExpanded = false
                            },
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.file_type_filter_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (
                state.duplicateScanExtension != state.fileExtensionFilter &&
                !state.duplicatesScanned
            ) {
                Text(
                    stringResource(R.string.duplicate_filter_requires_rescan),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            }
            if (searchMode == 0) {
            OutlinedTextField(
                value = state.folderSearchQuery,
                onValueChange = controller::updateFolderSearchQuery,
                label = { Text(stringResource(R.string.redesign_search_folders)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            }
            if (searchMode == 1) {
            OutlinedTextField(
                value = state.bigFileSearchQuery,
                onValueChange = controller::updateBigFileSearchQuery,
                label = { Text(stringResource(R.string.redesign_search_large)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            }
            if (searchMode == 2) {
            OutlinedTextField(
                value = state.duplicateSearchQuery,
                onValueChange = controller::updateDuplicateSearchQuery,
                label = { Text(stringResource(R.string.redesign_search_duplicates)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            }
            if (searchMode == 1) {
            Text(
                stringResource(R.string.redesign_large_threshold),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                BigFileSizeFilter.entries.forEach { filter ->
                    if (filter == state.bigFileSizeFilter) {
                        Button(onClick = { controller.updateBigFileSizeFilter(filter) }) {
                            Text(filter.label)
                        }
                    } else {
                        OutlinedButton(onClick = { controller.updateBigFileSizeFilter(filter) }) {
                            Text(filter.label)
                        }
                    }
                }
            }
            }
            if (searchMode == 0) {
            OutlinedTextField(
                value = state.thresholdValue,
                onValueChange = controller::updateThresholdValue,
                label = { Text(stringResource(R.string.redesign_small_folder_threshold)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThresholdUnit.entries.forEach { unit ->
                    if (state.thresholdUnit == unit) {
                        Button(onClick = { controller.updateThresholdUnit(unit) }) {
                            Text(unit.label)
                        }
                    } else {
                        OutlinedButton(onClick = { controller.updateThresholdUnit(unit) }) {
                            Text(unit.label)
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ResultSummaryCard(
    title: String,
    subtitle: String,
    accent: Color,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = accent)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultLine(
    title: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


private fun formatCleanerBytes(bytes: Long): String {
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
