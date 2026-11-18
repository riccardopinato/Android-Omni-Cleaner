package com.riccardopinato.omnicleaner.ui.redesign

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.riccardopinato.omnicleaner.monetization.ProBatchController
import com.riccardopinato.omnicleaner.monetization.findActivity
import com.riccardopinato.omnicleaner.monetization.rememberMonetizationRuntime
import com.riccardopinato.omnicleaner.ui.CleanerController
import com.riccardopinato.omnicleaner.ui.OmniCleanerViewModel
import com.riccardopinato.omnicleaner.ui.CleanerUiState
import com.riccardopinato.omnicleaner.ui.LanguageSelectorSection
import com.riccardopinato.omnicleaner.ui.MaintenanceSection
import com.riccardopinato.omnicleaner.ui.SafeTrashSection
import com.riccardopinato.omnicleaner.ui.SmartCategoriesSection
import com.riccardopinato.omnicleaner.ui.StorageCleanupSection
import com.riccardopinato.omnicleaner.ui.theme.OmniBlue
import com.riccardopinato.omnicleaner.ui.theme.OmniMint
import com.riccardopinato.omnicleaner.ui.theme.OmniOrange
import com.riccardopinato.omnicleaner.ui.theme.OmniPurple
import com.riccardopinato.omnicleaner.ui.theme.OmniRose
import java.util.Locale

private enum class OmniDestination(
    val labelRes: Int,
    val iconRes: Int,
) {
    HOME(R.string.nav_home, R.drawable.omni_nav_home),
    CLEAN(R.string.nav_clean, R.drawable.omni_nav_clean),
    GALLERY(R.string.nav_gallery, R.drawable.omni_nav_gallery),
    PRIVACY(R.string.nav_privacy, R.drawable.omni_nav_privacy),
    PRO(R.string.nav_pro, R.drawable.omni_nav_pro),
}

@Composable
fun RedesignedOmniCleanerApp(viewModel: OmniCleanerViewModel) {
    val context = LocalContext.current
    val controller = viewModel.cleanerController
    val monetizationRuntime = rememberMonetizationRuntime()
    val entitlement by PremiumEntitlementStore.state.collectAsState()
    val state = controller.state
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(context) {
        viewModel.updateUiContext(context)
    }

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
            val dark = androidx.compose.foundation.isSystemInDarkTheme()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (dark) androidx.compose.ui.graphics.Color(0xFF182130)
                        else androidx.compose.ui.graphics.Color.White)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                OmniDestination.entries.forEach { item ->
                    val selected = destination == item
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) {
                                if (dark) androidx.compose.ui.graphics.Color(0xFF314162)
                                else androidx.compose.ui.graphics.Color(0xFFE9EFFF)
                            } else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable(
                                role = androidx.compose.ui.semantics.Role.Tab,
                                onClick = { destinationName = item.name },
                            )
                            .padding(horizontal = 2.dp, vertical = 8.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) {
                                if (dark) androidx.compose.ui.graphics.Color(0xFFAEC6FF)
                                else androidx.compose.ui.graphics.Color(0xFF3159D6)
                            } else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(item.labelRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) {
                                if (dark) androidx.compose.ui.graphics.Color(0xFFE1E9FF)
                                else androidx.compose.ui.graphics.Color(0xFF3159D6)
                            } else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
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

                        OmniDestination.GALLERY -> GalleryCleanerSection(
                            controller = viewModel.galleryController,
                        )

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
                            Column(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                MonetizationSection(
                                    runtime = monetizationRuntime,
                                    batch = viewModel.proBatchController,
                                )
                                LanguageSelectorSection()
                            }
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
