package com.riccardopinato.omnicleaner.ui.redesign

import android.Manifest
import androidx.compose.animation.core.animateFloatAsState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
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
import com.riccardopinato.omnicleaner.exif.ExifWorkflowState
import com.riccardopinato.omnicleaner.ui.CleanupSelectionUiState
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
import com.riccardopinato.omnicleaner.ui.theme.AppAppearance
import java.util.Locale

private enum class OmniDestination(
    val labelRes: Int,
    val icon: OmniNavigationSymbol,
) {
    HOME(R.string.nav_home, OmniNavigationSymbol.HOME),
    CLEAN(R.string.nav_clean, OmniNavigationSymbol.CLEAN),
    GALLERY(R.string.nav_gallery, OmniNavigationSymbol.GALLERY),
    PRIVACY(R.string.nav_privacy, OmniNavigationSymbol.PRIVACY),
    PRO(R.string.nav_pro, OmniNavigationSymbol.PRO),
}

private enum class OmniDetailRoute(val parent: OmniDestination) {
    CLEAN_OVERVIEW(OmniDestination.CLEAN),
    CLEAN_INTELLIGENCE(OmniDestination.CLEAN),
    CLEAN_REVIEW(OmniDestination.CLEAN),
    CLEAN_LARGE(OmniDestination.CLEAN),
    CLEAN_DUPLICATES(OmniDestination.CLEAN),
    CLEAN_FOLDERS(OmniDestination.CLEAN),
    CLEAN_FILTERS(OmniDestination.CLEAN),
    CLEAN_TRASH(OmniDestination.CLEAN),
    CLEAN_ADVANCED(OmniDestination.CLEAN),
    GALLERY_REVIEW(OmniDestination.GALLERY),
    GALLERY_INSIGHTS(OmniDestination.GALLERY),
    PRIVACY_SINGLE(OmniDestination.PRIVACY),
    PRIVACY_BATCH(OmniDestination.PRIVACY),
    PRO_MANAGE(OmniDestination.PRO),
}

private fun CleanerToolRoute.toDetailRoute(): OmniDetailRoute = when (this) {
    CleanerToolRoute.OVERVIEW -> OmniDetailRoute.CLEAN_OVERVIEW
    CleanerToolRoute.INTELLIGENCE -> OmniDetailRoute.CLEAN_INTELLIGENCE
    CleanerToolRoute.REVIEW -> OmniDetailRoute.CLEAN_REVIEW
    CleanerToolRoute.LARGE -> OmniDetailRoute.CLEAN_LARGE
    CleanerToolRoute.DUPLICATES -> OmniDetailRoute.CLEAN_DUPLICATES
    CleanerToolRoute.FOLDERS -> OmniDetailRoute.CLEAN_FOLDERS
    CleanerToolRoute.FILTERS -> OmniDetailRoute.CLEAN_FILTERS
    CleanerToolRoute.TRASH -> OmniDetailRoute.CLEAN_TRASH
    CleanerToolRoute.ADVANCED -> OmniDetailRoute.CLEAN_ADVANCED
}

private fun OmniDetailRoute.toCleanerTool(): CleanerToolRoute = when (this) {
    OmniDetailRoute.CLEAN_OVERVIEW -> CleanerToolRoute.OVERVIEW
    OmniDetailRoute.CLEAN_INTELLIGENCE -> CleanerToolRoute.INTELLIGENCE
    OmniDetailRoute.CLEAN_REVIEW -> CleanerToolRoute.REVIEW
    OmniDetailRoute.CLEAN_LARGE -> CleanerToolRoute.LARGE
    OmniDetailRoute.CLEAN_DUPLICATES -> CleanerToolRoute.DUPLICATES
    OmniDetailRoute.CLEAN_FOLDERS -> CleanerToolRoute.FOLDERS
    OmniDetailRoute.CLEAN_FILTERS -> CleanerToolRoute.FILTERS
    OmniDetailRoute.CLEAN_TRASH -> CleanerToolRoute.TRASH
    OmniDetailRoute.CLEAN_ADVANCED -> CleanerToolRoute.ADVANCED
    else -> error("Not a Clean tool route: $this")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedesignedOmniCleanerApp(
    viewModel: OmniCleanerViewModel,
    darkTheme: Boolean,
    themeMode: AppAppearance.Mode,
    onThemeModeChange: (AppAppearance.Mode) -> Unit,
) {
    val context = LocalContext.current
    val controller = viewModel.cleanerController
    val monetizationRuntime = rememberMonetizationRuntime()
    val entitlement by PremiumEntitlementStore.state.collectAsState()
    val consentState by monetizationRuntime.consent.state.collectAsState()
    val state = controller.state
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(context) {
        viewModel.updateUiContext(context)
    }

    var destinationName by rememberSaveable { mutableStateOf(OmniDestination.HOME.name) }
    val destination = OmniDestination.valueOf(destinationName)
    var detailName by rememberSaveable { mutableStateOf<String?>(null) }
    val activeDetail = OmniDetailRoute.entries.firstOrNull {
        it.name == detailName && it.parent == destination
    }
    val cleanSelectionState = remember { CleanupSelectionUiState() }
    val privacyWorkflow = remember { ExifWorkflowState() }
    val workflowScope = rememberCoroutineScope()
    BackHandler(enabled = activeDetail != null) { detailName = null }
    var showDeepAccessDisclosure by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

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
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (activeDetail == null) {
            NavigationBar(
                containerColor = if (darkTheme) Color(0xFF111B38) else Color(0xFFF7FAFF),
                tonalElevation = 0.dp,
            ) {
                OmniDestination.entries.forEach { item ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("omni_nav_${item.name.lowercase(Locale.ROOT)}"),
                        selected = destination == item,
                        onClick = {
                            detailName = null
                            destinationName = item.name
                        },
                        icon = {
                            OmniNavigationIcon(
                                symbol = item.icon,
                                animateOnEnter = destination == item,
                                tint = if (destination == item) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        },
                        label = { Text(stringResource(item.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = if (darkTheme) Color(0xFF4D278F) else Color(0xFFD4E4FF),
                            selectedTextColor = if (darkTheme) Color.White else Color(0xFF174EE5),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
            } // No bottom navigation in dedicated functional screens.
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Brand waves belong to the rich Home dashboard; functional pages
            // use a quieter surface so sparse states remain intentional.
            OmniPremiumWaveBackground(
                dark = darkTheme,
                showBottomWave = destination == OmniDestination.HOME && activeDetail == null,
            )
            OmniDestination.entries.forEach { item ->
                val active = destination == item
                // A short layer-only fade responds immediately to taps, avoiding
                // an abrupt swap while leaving in-flight Clean/SAF state retained.
                val layerAlpha by animateFloatAsState(
                    targetValue = if (active) 1f else 0f,
                    animationSpec = tween(durationMillis = 125),
                    label = "destination-${item.name}",
                )
                // Clean/Privacy keep local selection and SAF workflow state alive.
                // Home, Gallery and Pro have no expensive offscreen UI owner:
                // their domain state already lives in the Activity ViewModel.
                val retainsWorkflow = item == OmniDestination.CLEAN ||
                    item == OmniDestination.PRIVACY
                if (active || retainsWorkflow) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (active) 1f else 0f)
                        .alpha(layerAlpha)
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
                    if (active && activeDetail != null) {
                        OmniDetailFrame(onBack = { detailName = null }) {
                            when (activeDetail) {
                                OmniDetailRoute.CLEAN_OVERVIEW,
                                OmniDetailRoute.CLEAN_INTELLIGENCE,
                                OmniDetailRoute.CLEAN_REVIEW,
                                OmniDetailRoute.CLEAN_LARGE,
                                OmniDetailRoute.CLEAN_DUPLICATES,
                                OmniDetailRoute.CLEAN_FOLDERS,
                                OmniDetailRoute.CLEAN_FILTERS,
                                OmniDetailRoute.CLEAN_TRASH,
                                OmniDetailRoute.CLEAN_ADVANCED -> androidx.compose.runtime.key(activeDetail) {
                                    CleanDetailScreen(
                                        tool = activeDetail.toCleanerTool(),
                                        state = state,
                                        controller = controller,
                                        modifier = Modifier.fillMaxSize(),
                                        selectionState = cleanSelectionState,
                                        onEnableDeepClean = { showDeepAccessDisclosure = true },
                                        onOpenTool = { detailName = it.toDetailRoute().name },
                                        galleryController = viewModel.galleryController,
                                        onOpenGalleryReview = {
                                            destinationName = OmniDestination.GALLERY.name
                                            detailName = OmniDetailRoute.GALLERY_REVIEW.name
                                        },
                                    )
                                }
                                OmniDetailRoute.GALLERY_INSIGHTS -> ModuleScreen(
                                    eyebrow = stringResource(R.string.redesign_media_eyebrow),
                                    title = stringResource(R.string.gp_title),
                                    subtitle = stringResource(R.string.gp_intro),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    com.riccardopinato.omnicleaner.gallery.GalleryProSection(
                                        controller = viewModel.galleryController,
                                        onOpenReview = { detailName = OmniDetailRoute.GALLERY_REVIEW.name },
                                    )
                                }
                                OmniDetailRoute.GALLERY_REVIEW -> ModuleScreen(
                                    eyebrow = stringResource(R.string.redesign_media_eyebrow),
                                    title = stringResource(R.string.nav_gallery_review),
                                    subtitle = stringResource(R.string.redesign_gallery_subtitle),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    GalleryCleanerSection(
                                        controller = viewModel.galleryController,
                                        reviewOnly = true,
                                    )
                                }
                                OmniDetailRoute.PRIVACY_SINGLE,
                                OmniDetailRoute.PRIVACY_BATCH -> ModuleScreen(
                                    eyebrow = stringResource(R.string.redesign_privacy_eyebrow),
                                    title = stringResource(
                                        if (activeDetail == OmniDetailRoute.PRIVACY_SINGLE)
                                            R.string.nav_privacy_quick
                                        else R.string.nav_privacy_batch
                                    ),
                                    subtitle = stringResource(R.string.redesign_privacy_subtitle),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    ExifCleanerSection(
                                        advancedPage = activeDetail == OmniDetailRoute.PRIVACY_BATCH,
                                        workflow = privacyWorkflow,
                                        scope = workflowScope,
                                    )
                                }
                                OmniDetailRoute.PRO_MANAGE -> ModuleScreen(
                                    eyebrow = stringResource(R.string.redesign_pro_eyebrow),
                                    title = stringResource(R.string.nav_pro_manage),
                                    subtitle = stringResource(R.string.redesign_pro_subtitle),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    MonetizationSection(
                                        runtime = monetizationRuntime,
                                        batch = viewModel.proBatchController,
                                        showWelcome = false,
                                    )
                                }
                            }
                        }
                    } else when (item) {
                        OmniDestination.HOME -> OmniPremiumHome(
                            dark = darkTheme,
                            onOpenSettings = { showSettings = true },
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
                                detailName = OmniDetailRoute.CLEAN_OVERVIEW.name
                                when {
                                    state.hasBroadAccess -> controller.scanOverview()
                                    state.storageCapability.canRequestDeepAccess ->
                                        showDeepAccessDisclosure = true
                                }
                            },
                            onFindDuplicates = {
                                destinationName = OmniDestination.CLEAN.name
                                detailName = OmniDetailRoute.CLEAN_DUPLICATES.name
                                when {
                                    state.hasBroadAccess -> controller.scanDuplicates()
                                    state.storageCapability.canRequestDeepAccess ->
                                        showDeepAccessDisclosure = true
                                }
                            },
                        )

                        OmniDestination.CLEAN -> CleanHubScreen(
                            state = state,
                            controller = controller,
                            modifier = Modifier.fillMaxSize(),
                            onOpenTool = { detailName = it.toDetailRoute().name },
                            onEnableDeepClean = { showDeepAccessDisclosure = true },
                        )

                        OmniDestination.GALLERY -> ModuleScreen(
                            eyebrow = stringResource(R.string.redesign_media_eyebrow),
                            title = stringResource(R.string.module_gallery_cleaner),
                            subtitle = stringResource(R.string.redesign_gallery_subtitle),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            GalleryCleanerSection(
                                controller = viewModel.galleryController,
                                onOpenReview = { detailName = OmniDetailRoute.GALLERY_REVIEW.name },
                                onOpenInsights = { detailName = OmniDetailRoute.GALLERY_INSIGHTS.name },
                            )
                        }

                        OmniDestination.PRIVACY -> ModuleScreen(
                            eyebrow = stringResource(R.string.redesign_privacy_eyebrow),
                            title = stringResource(R.string.module_exif_privacy),
                            subtitle = stringResource(R.string.redesign_privacy_subtitle),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            OmniFunctionsHub(
                                kind = OmniWelcomeKind.PRIVACY,
                                titleRes = R.string.welcome_privacy_title,
                                bodyRes = R.string.welcome_privacy_body,
                                actions = listOf(
                                    R.string.nav_privacy_quick to {
                                        detailName = OmniDetailRoute.PRIVACY_SINGLE.name
                                    },
                                    R.string.nav_privacy_batch to {
                                        detailName = OmniDetailRoute.PRIVACY_BATCH.name
                                    },
                                ),
                            )
                        }

                        OmniDestination.PRO -> ModuleScreen(
                            eyebrow = stringResource(R.string.redesign_pro_eyebrow),
                            title = stringResource(R.string.pro_title),
                            subtitle = stringResource(R.string.redesign_pro_subtitle),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            OmniFunctionsHub(
                                kind = OmniWelcomeKind.PRO,
                                titleRes = R.string.welcome_pro_title,
                                bodyRes = R.string.welcome_pro_body,
                                actions = listOf(
                                    R.string.nav_pro_manage to {
                                        detailName = OmniDetailRoute.PRO_MANAGE.name
                                    },
                                ),
                                showWelcome = !entitlement.isPremium,
                            )
                        }
                    }
                }
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(R.string.audit_settings),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    stringResource(R.string.audit_appearance),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppAppearance.Mode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = {
                                Text(stringResource(when (mode) {
                                    AppAppearance.Mode.SYSTEM -> R.string.audit_theme_system
                                    AppAppearance.Mode.LIGHT -> R.string.audit_theme_light
                                    AppAppearance.Mode.DARK -> R.string.audit_theme_dark
                                }))
                            },
                        )
                    }
                }
                LanguageSelectorSection()
                if (consentState.privacyOptionsRequired && activity != null) {
                    OutlinedButton(
                        onClick = { monetizationRuntime.consent.showPrivacyOptions(activity) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.privacy_options))
                    }
                }
                Spacer(Modifier.height(24.dp))
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

/** A bounded two-level router: navigation hub → dedicated tool → Android Back. */
@Composable
private fun OmniDetailFrame(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onBack,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text("‹  " + stringResource(R.string.nav_back))
        }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun OmniFunctionsHub(
    kind: OmniWelcomeKind,
    titleRes: Int,
    bodyRes: Int,
    actions: List<Pair<Int, () -> Unit>>,
    showWelcome: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Feature navigation is a set of distinct touch-friendly destinations,
        // rather than full-width buttons inside another generic material card.
        val accent = if (kind == OmniWelcomeKind.PRIVACY) OmniMint else OmniPurple
        actions.forEach { (labelRes, open) ->
            val tag = when (labelRes) {
                R.string.nav_privacy_quick -> "omni_privacy_quick"
                R.string.nav_privacy_batch -> "omni_privacy_batch"
                R.string.nav_pro_manage -> "omni_pro_manage"
                else -> "omni_tool_other"
            }
            Card(
                onClick = open,
                modifier = Modifier.fillMaxWidth().testTag(tag),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .22f)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(12.dp)
                            .background(accent, RoundedCornerShape(6.dp)),
                    )
                    Text(
                        stringResource(labelRes),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("›", color = accent, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        // The destination actions are the primary content; the long onboarding
        // checklist lives in the dedicated workflow. Keep truthful safety copy.
        if (showWelcome) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        stringResource(titleRes),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(bodyRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
