package com.riccardopinato.omnicleaner.ui.redesign

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
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
import com.riccardopinato.omnicleaner.ui.theme.OmniSecondaryButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.riccardopinato.omnicleaner.ui.theme.OmniTextButton as TextButton
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
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
import com.riccardopinato.omnicleaner.ui.DuplicateScanMode
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
    INTELLIGENCE(R.string.si_title),
    REVIEW(R.string.review_center_title),
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
                eyebrow = null,
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
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
        if (state.scanning) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // AMX-006 only while the real scanner is active.
                        OmniAccordionLoader(
                            modifier = Modifier.testTag("omni_scan_accordion_hub"),
                        )
                        Text(
                            state.currentOperation?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.omni_scan_running),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedButton(onClick = controller::cancelScan) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
        state.error?.let { error ->
            item { Text(error, modifier = Modifier.padding(horizontal = 20.dp),
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
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Intentional bento, not nine indistinguishable blue actions.
                // Every original route and its deterministic test tag is preserved.
                CleanHubToolGroup(
                    title = stringResource(R.string.omni_clean_group_analyze),
                    tools = listOf(
                        CleanerToolRoute.OVERVIEW,
                        CleanerToolRoute.INTELLIGENCE,
                        CleanerToolRoute.LARGE,
                        CleanerToolRoute.DUPLICATES,
                    ),
                    onOpenTool = onOpenTool,
                )
                CleanHubToolGroup(
                    title = stringResource(R.string.omni_clean_group_review),
                    tools = listOf(
                        CleanerToolRoute.REVIEW,
                        CleanerToolRoute.FOLDERS,
                        CleanerToolRoute.TRASH,
                    ),
                    onOpenTool = onOpenTool,
                )
                CleanHubToolGroup(
                    title = stringResource(R.string.omni_clean_group_advanced),
                    tools = listOf(
                        CleanerToolRoute.FILTERS,
                        CleanerToolRoute.ADVANCED,
                    ),
                    onOpenTool = onOpenTool,
                )
            }
        }
        if (state.overview == null && !state.foldersScanned &&
            !state.bigFilesScanned && !state.duplicatesScanned &&
            !state.scanning && state.error == null
        ) {
            item { CleanWelcomePanel() }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun CleanHubToolGroup(
    title: String,
    tools: List<CleanerToolRoute>,
    onOpenTool: (CleanerToolRoute) -> Unit,
) {
    val singleColumn = LocalConfiguration.current.screenWidthDp < 360 ||
        LocalDensity.current.fontScale > 1.25f
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        val rows = when {
            singleColumn -> tools.map { listOf(it) }
            // Storage overview is the true primary module, not just a random wide tile.
            tools.firstOrNull() == CleanerToolRoute.OVERVIEW ->
                listOf(listOf(CleanerToolRoute.OVERVIEW)) + tools.drop(1).chunked(2)
            else -> tools.chunked(2)
        }
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { tool ->
                    Card(
                        onClick = { onOpenTool(tool) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = if (tool == CleanerToolRoute.OVERVIEW && !singleColumn) 108.dp else 92.dp)
                            .testTag("omni_clean_tool_${tool.name.lowercase(Locale.ROOT)}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OmniCleanToolIcon(
                                tool = tool,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Text(
                                    stringResource(tool.labelRes),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
                if (pair.size == 1 && !singleColumn && pair.first() != CleanerToolRoute.OVERVIEW) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
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
    onOpenTool: (CleanerToolRoute) -> Unit,
    galleryController: com.riccardopinato.omnicleaner.gallery.GalleryController,
    onOpenGalleryReview: () -> Unit,
) {
    val onCleanupFinished = {
        controller.refreshAccessAndStorage()
        controller.scanOverview()
    }
    LaunchedEffect(state.duplicateScanEpoch) {
        if (selectionState.lastDuplicateScanEpoch != state.duplicateScanEpoch) {
            selectionState.selectedDuplicates.value = emptySet()
            selectionState.lastDuplicateScanEpoch = state.duplicateScanEpoch
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("omni_clean_detail_${tool.name.lowercase(Locale.ROOT)}"),
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
        if (tool !in listOf(CleanerToolRoute.TRASH, CleanerToolRoute.REVIEW) && !state.hasBroadAccess) {
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
                            if (tool == CleanerToolRoute.DUPLICATES && !state.scanning) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = state.duplicateScanMode == DuplicateScanMode.REUSE_VERIFIED_INDEX,
                                        onClick = {
                                            controller.updateDuplicateScanMode(DuplicateScanMode.REUSE_VERIFIED_INDEX)
                                        },
                                        label = { Text(stringResource(R.string.scan2_mode_fast)) },
                                    )
                                    FilterChip(
                                        selected = state.duplicateScanMode == DuplicateScanMode.REFRESH_INVENTORY,
                                        onClick = {
                                            controller.updateDuplicateScanMode(DuplicateScanMode.REFRESH_INVENTORY)
                                        },
                                        label = { Text(stringResource(R.string.scan2_mode_full)) },
                                    )
                                }
                                Text(
                                    stringResource(
                                        if (state.duplicateScanMode == DuplicateScanMode.REFRESH_INVENTORY)
                                            R.string.scan2_mode_full_hint
                                        else R.string.scan2_mode_fast_hint
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (state.duplicateResumeAvailable) {
                                    Text(
                                        stringResource(R.string.scan2_resume_explain),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    OutlinedButton(
                                        onClick = controller::resumeDuplicates,
                                        modifier = Modifier.fillMaxWidth()
                                            .testTag("omni_resume_duplicates"),
                                    ) {
                                        Text(stringResource(R.string.scan2_resume))
                                    }
                                }
                            }
                            if (state.scanning) {
                                if (tool == CleanerToolRoute.DUPLICATES &&
                                    state.duplicateProgressTotal > 0 &&
                                    state.duplicateProgressProcessed < state.duplicateProgressTotal
                                ) {
                                    LinearProgressIndicator(
                                        progress = {
                                            (state.duplicateProgressProcessed.toFloat() /
                                                state.duplicateProgressTotal).coerceIn(0f, 1f)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Text(
                                        stringResource(
                                            R.string.duplicate_scan_progress,
                                            state.duplicateProgressProcessed,
                                            state.duplicateProgressTotal,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                } else {
                                    // A known total uses the real determinate indicator above.
                                    OmniAccordionLoader(
                                        modifier = Modifier.testTag("omni_scan_accordion_detail"),
                                    )
                                    Text(
                                        when {
                                            tool == CleanerToolRoute.DUPLICATES &&
                                                state.duplicateProgressTotal > 0 ->
                                                stringResource(R.string.omni_scan_verifying_matches)
                                            tool == CleanerToolRoute.DUPLICATES &&
                                                state.indexedFilesChecked > 0 ->
                                                stringResource(
                                                    R.string.scan_indexing_files_count,
                                                    state.indexedFilesChecked,
                                                )
                                            else -> state.currentOperation.orEmpty()
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                OutlinedButton(onClick = controller::cancelScan) {
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
                CleanerToolRoute.REVIEW -> item {
                    ReviewCenterSection(
                        state = state,
                        selection = selectionState,
                        galleryController = galleryController,
                        onOpenLarge = { onOpenTool(CleanerToolRoute.LARGE) },
                        onOpenDuplicates = { onOpenTool(CleanerToolRoute.DUPLICATES) },
                        onOpenFolders = { onOpenTool(CleanerToolRoute.FOLDERS) },
                        onOpenGallery = onOpenGalleryReview,
                    )
                }
                CleanerToolRoute.INTELLIGENCE -> item {
                    StorageIntelligenceSection(
                        onOpenAdvanced = { onOpenTool(CleanerToolRoute.ADVANCED) },
                        onOpenLargeFiles = { onOpenTool(CleanerToolRoute.LARGE) },
                    )
                }
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
                    if (!state.duplicatesScanned && state.duplicatePreviewGroups.isNotEmpty()) {
                        item {
                            ResultSummaryCard(
                                title = stringResource(R.string.module_duplicates),
                                subtitle = stringResource(
                                    R.string.scan2_partial_groups,
                                    state.duplicatePreviewGroups.size,
                                ),
                                accent = OmniRose,
                            )
                        }
                    }
                    if (!state.duplicatesScanned && state.duplicatePreviewGroups.isNotEmpty()) {
                        items(
                            state.duplicatePreviewGroups.take(20),
                            key = { "scan-preview:" + it.files.first().path },
                        ) { group ->
                            ResultLine(
                                title = group.files.first().name,
                                value = stringResource(
                                    R.string.pro_duplicate_group,
                                    group.files.size,
                                    formatCleanerBytes(group.sizeBytes),
                                ),
                            )
                        }
                    }
                    if (state.duplicatesScanned && state.visibleDuplicates.isNotEmpty()) {
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
                eyebrow = null,
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
                if (state.duplicateProgressTotal > 0 &&
                    state.duplicateProgressProcessed < state.duplicateProgressTotal
                ) {
                    LinearProgressIndicator(
                        progress = {
                            (state.duplicateProgressProcessed.toFloat() /
                                state.duplicateProgressTotal).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OmniAccordionLoader(
                        modifier = Modifier.testTag("omni_scan_accordion_actions"),
                    )
                }
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
                    val isSelected = filter == state.bigFileSizeFilter
                    if (isSelected) {
                        Button(onClick = { controller.updateBigFileSizeFilter(filter) },
                            modifier = Modifier.semantics { selected = true }) {
                            Text("✓  " + filter.label)
                        }
                    } else {
                        OutlinedButton(onClick = { controller.updateBigFileSizeFilter(filter) },
                            modifier = Modifier.semantics { selected = false }) {
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
                    val isSelected = state.thresholdUnit == unit
                    val choiceModifier = Modifier
                        .testTag("omni_threshold_unit_${unit.name.lowercase(Locale.ROOT)}")
                        .semantics { selected = isSelected }
                    if (isSelected) {
                        Button(
                            onClick = { controller.updateThresholdUnit(unit) },
                            modifier = choiceModifier,
                        ) { Text("✓  " + unit.label) }
                    } else {
                        OutlinedButton(
                            onClick = { controller.updateThresholdUnit(unit) },
                            modifier = choiceModifier,
                        ) { Text(unit.label) }
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
