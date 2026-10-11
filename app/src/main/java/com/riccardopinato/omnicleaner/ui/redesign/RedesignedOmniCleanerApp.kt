package com.riccardopinato.omnicleaner.ui.redesign

import android.Manifest
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.BigFileSizeFilter
import com.riccardopinato.omnicleaner.cleaner.DeepStorageAccessStatus
import com.riccardopinato.omnicleaner.cleaner.FolderStatus
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
import com.riccardopinato.omnicleaner.ui.CleanerUiState
import com.riccardopinato.omnicleaner.ui.MaintenanceSection
import com.riccardopinato.omnicleaner.ui.SafeTrashSection
import com.riccardopinato.omnicleaner.ui.StorageCleanupSection
import com.riccardopinato.omnicleaner.ui.theme.OmniBlue
import com.riccardopinato.omnicleaner.ui.theme.OmniMint
import com.riccardopinato.omnicleaner.ui.theme.OmniOrange
import com.riccardopinato.omnicleaner.ui.theme.OmniPurple
import com.riccardopinato.omnicleaner.ui.theme.OmniRose
import java.util.Locale

private enum class OmniDestination(
    val labelRes: Int,
    val glyph: String,
) {
    HOME(R.string.nav_home, "H"),
    CLEAN(R.string.nav_clean, "C"),
    GALLERY(R.string.nav_gallery, "G"),
    PRIVACY(R.string.nav_privacy, "P"),
    PRO(R.string.nav_pro, "★"),
}

@Composable
fun RedesignedOmniCleanerApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(context.applicationContext, scope) {
        CleanerController(context.applicationContext, scope)
    }
    val monetizationRuntime = rememberMonetizationRuntime()
    val entitlement by PremiumEntitlementStore.state.collectAsState()
    val state = controller.state
    val activity = remember(context) { context.findActivity() }

    var destinationName by rememberSaveable { mutableStateOf(OmniDestination.HOME.name) }
    val destination = OmniDestination.valueOf(destinationName)
    var showDeepAccessDisclosure by remember { mutableStateOf(false) }

    LaunchedEffect(activity) {
        if (activity != null) {
            CleanupEventBus.events.collect {
                AdManager.onCleanupCompleted(
                    activity = activity,
                    isPremium = PremiumEntitlementStore.state.value.isPremium,
                )
            }
        }
    }

    val storageSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        controller.refreshAccessAndStorage()
    }

    val legacyStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        controller.refreshAccessAndStorage()
    }

    fun requestStorageAccess() {
        when (state.storageCapability.status) {
            DeepStorageAccessStatus.NEEDS_ALL_FILES_ACCESS ->
                storageSettingsLauncher.launch(StorageAccess.settingsIntent(context))

            DeepStorageAccessStatus.NEEDS_LEGACY_PERMISSION ->
                legacyStorageLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ),
                )

            else -> Unit
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                OmniDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destinationName = item.name },
                        icon = {
                            Text(
                                item.glyph,
                                fontWeight = FontWeight.Bold,
                                color = if (destination == item) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        },
                        label = { Text(stringResource(item.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OmniDestination.entries.forEach { item ->
                val active = destination == item
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (active) 1f else 0f)
                        .alpha(if (active) 1f else 0f)
                        .then(
                            if (active) {
                                Modifier
                            } else {
                                Modifier
                                    .clearAndSetSemantics { }
                                    .pointerInput(item) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitPointerEvent().changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                            },
                        ),
                ) {
                    when (item) {
                        OmniDestination.HOME -> HomeScreen(
                            state = state,
                            premium = entitlement.isPremium,
                            modifier = Modifier.fillMaxSize(),
                            onRefresh = controller::refreshAccessAndStorage,
                            onEnableDeepClean = { showDeepAccessDisclosure = true },
                            onOpenClean = { destinationName = OmniDestination.CLEAN.name },
                            onOpenGallery = { destinationName = OmniDestination.GALLERY.name },
                            onOpenPrivacy = { destinationName = OmniDestination.PRIVACY.name },
                            onOpenPro = { destinationName = OmniDestination.PRO.name },
                            onAnalyzeStorage = {
                                destinationName = OmniDestination.CLEAN.name
                                when {
                                    state.hasBroadAccess -> controller.scanOverview()
                                    state.storageCapability.canRequestDeepAccess ->
                                        showDeepAccessDisclosure = true
                                }
                            },
                            onFindDuplicates = {
                                destinationName = OmniDestination.CLEAN.name
                                when {
                                    state.hasBroadAccess -> controller.scanDuplicates()
                                    state.storageCapability.canRequestDeepAccess ->
                                        showDeepAccessDisclosure = true
                                }
                            },
                        )

                        OmniDestination.CLEAN -> CleanScreen(
                            state = state,
                            controller = controller,
                            modifier = Modifier.fillMaxSize(),
                            onEnableDeepClean = { showDeepAccessDisclosure = true },
                        )

                        OmniDestination.GALLERY -> ModuleScreen(
                            eyebrow = stringResource(R.string.redesign_media_eyebrow),
                            title = stringResource(R.string.module_gallery_cleaner),
                            subtitle = stringResource(R.string.redesign_gallery_subtitle),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            GalleryCleanerSection()
                        }

                        OmniDestination.PRIVACY -> ModuleScreen(
                            eyebrow = stringResource(R.string.redesign_privacy_eyebrow),
                            title = stringResource(R.string.module_exif_privacy),
                            subtitle = stringResource(R.string.redesign_privacy_subtitle),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            ExifCleanerSection()
                        }

                        OmniDestination.PRO -> ModuleScreen(
                            eyebrow = stringResource(R.string.redesign_pro_eyebrow),
                            title = stringResource(R.string.pro_title),
                            subtitle = stringResource(R.string.redesign_pro_subtitle),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            MonetizationSection(runtime = monetizationRuntime)
                        }
                    }
                }
            }
        }
    }

    if (showDeepAccessDisclosure) {
        AlertDialog(
            onDismissRequest = { showDeepAccessDisclosure = false },
            title = { Text(stringResource(R.string.deep_access_dialog_title)) },
            text = { Text(stringResource(R.string.deep_access_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeepAccessDisclosure = false
                        requestStorageAccess()
                    },
                ) {
                    Text(stringResource(R.string.deep_access_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeepAccessDisclosure = false }) {
                    Text(stringResource(R.string.deep_access_dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun HomeScreen(
    state: CleanerUiState,
    premium: Boolean,
    modifier: Modifier,
    onRefresh: () -> Unit,
    onEnableDeepClean: () -> Unit,
    onOpenClean: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenPro: () -> Unit,
    onAnalyzeStorage: () -> Unit,
    onFindDuplicates: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(R.string.redesign_home_eyebrow),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    stringResource(R.string.redesign_home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            val info = state.storageInfo
            OmniHeroCard(
                title = stringResource(R.string.storage),
                value = info?.let { formatBytes(it.usedBytes) } ?: "—",
                detail = info?.let {
                    stringResource(
                        R.string.redesign_storage_detail,
                        formatBytes(it.freeBytes),
                        formatBytes(it.totalBytes),
                    )
                } ?: stringResource(R.string.redesign_storage_waiting),
                progress = info?.usedPercent,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OmniMetricCard(
                    label = stringResource(R.string.redesign_metric_large_files),
                    value = if (state.bigFilesScanned) state.bigFiles.size.toString() else "—",
                    accent = OmniOrange,
                    modifier = Modifier.width(128.dp),
                )
                OmniMetricCard(
                    label = stringResource(R.string.redesign_metric_duplicates),
                    value = if (state.duplicatesScanned) state.duplicates.size.toString() else "—",
                    accent = OmniRose,
                    modifier = Modifier.width(128.dp),
                )
                OmniMetricCard(
                    label = stringResource(R.string.redesign_metric_folders),
                    value = if (state.foldersScanned) state.folders.size.toString() else "—",
                    accent = OmniMint,
                    modifier = Modifier.width(128.dp),
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OmniModuleCard(
                    title = stringResource(R.string.redesign_quick_analyze),
                    subtitle = stringResource(R.string.redesign_quick_analyze_subtitle),
                    glyph = "↗",
                    accent = OmniBlue,
                    onClick = onAnalyzeStorage,
                    modifier = Modifier.width(180.dp),
                )
                OmniModuleCard(
                    title = stringResource(R.string.redesign_quick_duplicates),
                    subtitle = stringResource(R.string.redesign_quick_duplicates_subtitle),
                    glyph = "≋",
                    accent = OmniRose,
                    onClick = onFindDuplicates,
                    modifier = Modifier.width(180.dp),
                )
                OmniModuleCard(
                    title = stringResource(R.string.module_gallery_cleaner),
                    subtitle = stringResource(R.string.redesign_quick_gallery_subtitle),
                    glyph = "▣",
                    accent = OmniPurple,
                    onClick = onOpenGallery,
                    modifier = Modifier.width(180.dp),
                )
                OmniModuleCard(
                    title = stringResource(R.string.module_exif_privacy),
                    subtitle = stringResource(R.string.redesign_quick_privacy_subtitle),
                    glyph = "◎",
                    accent = OmniMint,
                    onClick = onOpenPrivacy,
                    modifier = Modifier.width(180.dp),
                )
            }
        }

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
                        stringResource(R.string.redesign_access_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OmniStatusPill(
                        text = when {
                            state.hasBroadAccess -> stringResource(R.string.access_ready)
                            state.storageCapability.standardCleaningAvailable ->
                                stringResource(R.string.redesign_standard_ready)
                            else -> stringResource(R.string.standard_cleaning_unavailable)
                        },
                        accent = when {
                            state.hasBroadAccess -> OmniMint
                            state.storageCapability.standardCleaningAvailable -> OmniBlue
                            else -> MaterialTheme.colorScheme.error
                        },
                    )
                    Text(
                        when (state.storageCapability.status) {
                            DeepStorageAccessStatus.READY -> stringResource(R.string.redesign_deep_ready)
                            DeepStorageAccessStatus.NEEDS_ALL_FILES_ACCESS,
                            DeepStorageAccessStatus.NEEDS_LEGACY_PERMISSION,
                            -> stringResource(R.string.redesign_deep_optional)
                            DeepStorageAccessStatus.SCOPED_ONLY_ANDROID_10 ->
                                stringResource(R.string.deep_access_android10)
                            DeepStorageAccessStatus.STORAGE_UNAVAILABLE ->
                                stringResource(R.string.deep_access_unavailable)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.storageCapability.canRequestDeepAccess) {
                            Button(onClick = onEnableDeepClean) {
                                Text(stringResource(R.string.grant_deep_access))
                            }
                        }
                        OutlinedButton(onClick = onRefresh) {
                            Text(stringResource(R.string.refresh))
                        }
                    }
                    OutlinedButton(onClick = onOpenClean) {
                        Text(stringResource(R.string.redesign_open_clean_workspace))
                    }
                }
            }
        }

        item {
            Card(
                onClick = onOpenPro,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        if (premium) stringResource(R.string.pro_active)
                        else stringResource(R.string.redesign_pro_card_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.redesign_pro_card_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun CleanScreen(
    state: CleanerUiState,
    controller: CleanerController,
    modifier: Modifier,
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
        } else {
            item { CleanActionsCard(state = state, controller = controller) }
            item { CleanFiltersCard(state = state, controller = controller) }

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
                            formatBytes(overview.logicalBytes),
                        ),
                        accent = OmniBlue,
                    )
                }
                items(overview.byCategory.take(10), key = { "cat:" + it.path }) {
                    ResultLine(it.path, formatBytes(it.bytes))
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
                items(state.visibleBigFiles.take(24), key = { "big:" + it.path }) {
                    ResultLine(it.name, formatBytes(it.sizeBytes))
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
                            formatBytes(waste),
                        ),
                        accent = OmniRose,
                    )
                }
                items(state.visibleDuplicates.take(20), key = { "dup:" + it.hash }) { group ->
                    ResultLine(
                        group.files.firstOrNull()?.name ?: group.hash.take(12),
                        group.files.size.toString() + " × " + formatBytes(group.sizeBytes),
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
                items(state.visibleFolders.take(20), key = { "folder:" + it.path }) {
                    ResultLine(
                        it.name,
                        formatBytes(it.sizeBytes) + " · " + it.fileCount.toString(),
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

            item { MaintenanceSection() }
        }

        item { SafeTrashSection() }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun CleanActionsCard(
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
            Text(stringResource(R.string.scan_tools), style = MaterialTheme.typography.titleLarge)
            if (state.scanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    state.currentOperation.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = controller::cancelScan) {
                    Text(stringResource(R.string.cancel))
                }
            } else {
                Button(onClick = controller::scanOverview, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_overview))
                }
                OutlinedButton(onClick = controller::scanFolders, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_folders))
                }
                Button(onClick = { controller.scanBigFiles() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_big_files))
                }
                OutlinedButton(onClick = controller::scanDuplicates, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_duplicates))
                }
            }
        }
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
            OutlinedTextField(
                value = state.folderSearchQuery,
                onValueChange = controller::updateFolderSearchQuery,
                label = { Text(stringResource(R.string.redesign_search_folders)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.bigFileSearchQuery,
                onValueChange = controller::updateBigFileSearchQuery,
                label = { Text(stringResource(R.string.redesign_search_large)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.duplicateSearchQuery,
                onValueChange = controller::updateDuplicateSearchQuery,
                label = { Text(stringResource(R.string.redesign_search_duplicates)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
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

@Composable
private fun ModuleScreen(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OmniSectionHeader(
                eyebrow = eyebrow,
                title = title,
                subtitle = subtitle,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
        item { content() }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

private fun formatBytes(bytes: Long): String {
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
