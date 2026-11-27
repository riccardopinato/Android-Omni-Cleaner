package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.cos
import kotlin.math.sin
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.gallery.GalleryController
import com.riccardopinato.omnicleaner.ui.CleanerUiState

/**
 * Color Bento Home: the user's Linktree visual direction translated into
 * real Compose components, independent of Linktree source/brand assets.
 *
 * This screen does NOT write to storage, present synthetic metrics, or
 * invoke a trash request. The parent owns all real navigation/actions.
 */
@Composable
internal fun OmniColorBentoHome(
    state: CleanerUiState,
    galleryController: GalleryController,
    premium: Boolean,
    modifier: Modifier = Modifier,
    dark: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onEnableDeepClean: () -> Unit,
    onOpenClean: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenPro: () -> Unit,
    onAnalyzeStorage: () -> Unit,
    onFindDuplicates: () -> Unit,
    onOpenLargeFiles: () -> Unit,
    onOpenTrashQueue: () -> Unit,
) {
    var gallery by remember(galleryController) { mutableStateOf(galleryController.state) }
    DisposableEffect(galleryController) {
        val disposeObserver = galleryController.observe { gallery = it }
        onDispose { disposeObserver() }
    }
    val colors = OmniColorBento
    val compact = LocalConfiguration.current.screenWidthDp < 360 ||
        LocalDensity.current.fontScale > 1.35f
    val canvas = if (dark) colors.backgroundDark else colors.background
    val text = if (dark) colors.white else colors.ink
    val subtext = if (dark) colors.white.copy(alpha = .75f) else colors.ink.copy(alpha = .75f)
    val duplicates = bentoVerifiedCount(
        state.duplicatesScanned,
        state.duplicates.sumOf { it.totalDuplicatesCount },
    )
    val largeFiles = if (state.bigFilesScanned)
        bentoByteLabel(state.bigFiles.fold(0L) { acc, file ->
            if (Long.MAX_VALUE - acc < file.sizeBytes.coerceAtLeast(0L)) Long.MAX_VALUE
            else acc + file.sizeBytes.coerceAtLeast(0L)
        })
    else "—"
    // Gallery's loadedSourceCount is a page-level fact, never the total
    // number of photos on the device.
    val loadedMedia = if (gallery.loadedSourceCount > 0)
        gallery.loadedSourceCount.toString()
    else "—"
    val settingsLabel = stringResource(R.string.audit_settings)
    val compactTagline = LocalDensity.current.fontScale > 1.6f
    // Stable real data owners. Reuse the same card in narrow one-column and
    // standard asymmetric bento arrangements: never shrink text into noise.
    val storageCard: @Composable (Modifier) -> Unit = { m ->
        OmniBentoStorageCard(state.storageInfo, onAnalyzeStorage, m)
    }
    val loadedMediaCard: @Composable (Modifier) -> Unit = { m ->
        OmniBentoMetricCard(
            value = loadedMedia,
            label = stringResource(R.string.bento_loaded_media),
            symbol = OmniNavigationSymbol.GALLERY,
            background = colors.pink,
            foreground = colors.cobalt,
            onClick = onOpenGallery,
            modifier = m,
            tag = "omni_bento_media_count",
        )
    }
    val duplicatesCard: @Composable (Modifier) -> Unit = { m ->
        OmniBentoMetricCard(
            value = duplicates,
            label = stringResource(R.string.bento_duplicates_verified),
            symbol = OmniNavigationSymbol.CLEAN,
            background = colors.magenta,
            foreground = colors.white,
            onClick = onFindDuplicates,
            modifier = m,
            tag = "omni_bento_duplicates",
        )
    }
    val galleryCard: @Composable (Modifier) -> Unit = { m ->
        OmniBentoGalleryCard(
            title = stringResource(R.string.bento_gallery_title),
            caption = stringResource(R.string.bento_gallery_subtitle),
            onClick = onOpenGallery,
            modifier = m,
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize()
            .background(canvas)
            .testTag("omni_color_bento_home"),
        contentPadding = PaddingValues(
            start = if (compact) 13.dp else 17.dp,
            end = if (compact) 13.dp else 17.dp,
            top = 19.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Original non-Linktree brand mark; no third-party logo.
                Box(
                    Modifier.size(44.dp)
                        .background(colors.lime, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    OmniNavigationIcon(
                        symbol = OmniNavigationSymbol.CLEAN,
                        tint = colors.ink,
                        modifier = Modifier.size(29.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.bento_app_title),
                        color = text,
                        fontFamily = colors.display,
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp,
                        lineHeight = 28.sp,
                        maxLines = 2,
                    )
                    Text(
                        stringResource(
                            if (compactTagline) R.string.bento_compact_tagline
                            else R.string.redesign_home_subtitle,
                        ),
                        color = subtext,
                        fontFamily = colors.body,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier.size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.ink)
                        .clickable(role = Role.Button, onClick = onOpenSettings)
                        .testTag("omni_bento_settings")
                        .semantics { contentDescription = settingsLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    OmniBentoSettingsGlyph()
                }
            }
        }
        item(key = "main_metrics") {
            if (compact) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    storageCard(Modifier.fillMaxWidth())
                    loadedMediaCard(Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    storageCard(Modifier.weight(1.75f))
                    loadedMediaCard(Modifier.weight(1f))
                }
            }
        }
        item(key = "feature_metrics") {
            if (compact) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    duplicatesCard(Modifier.fillMaxWidth())
                    galleryCard(Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    duplicatesCard(Modifier.weight(1f))
                    galleryCard(Modifier.weight(1.75f))
                }
            }
        }
        item(key = "large_files") {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = 145.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.lime)
                    .clickable(role = Role.Button, onClick = onOpenLargeFiles)
                    .testTag("omni_bento_large_files")
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(R.string.bento_large_title),
                        color = colors.ink,
                        fontFamily = colors.display,
                        fontWeight = FontWeight.Bold,
                        fontSize = 23.sp,
                        lineHeight = 26.sp,
                        maxLines = 2,
                    )
                    Text(
                        if (state.bigFilesScanned) stringResource(R.string.bento_large_verified)
                        else stringResource(R.string.bento_scan_required),
                        color = colors.ink.copy(alpha = .76f),
                        fontFamily = colors.body,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 3,
                    )
                    Text(
                        largeFiles,
                        color = colors.ink,
                        fontFamily = colors.display,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                // Original static stack of document shapes. The count comes
                // from a verified scan, never an illustration.
                OmniBentoFileArt()
            }
        }
        item(key = "quick_tools") {
            BentoToolDeck(
                onOpenClean = onOpenClean,
                onOpenLargeFiles = onOpenLargeFiles,
                onFindDuplicates = onFindDuplicates,
                onOpenGallery = onOpenGallery,
                onOpenTrashQueue = onOpenTrashQueue,
            )
        }
        item(key = "secondary") {
            Text(
                stringResource(R.string.bento_explore_title),
                color = text,
                fontFamily = colors.display,
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp,
                lineHeight = 27.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                BentoShortcut(
                    label = stringResource(R.string.nav_clean),
                    icon = OmniNavigationSymbol.CLEAN,
                    onClick = onOpenClean,
                    modifier = Modifier.weight(1f),
                )
                BentoShortcut(
                    label = stringResource(R.string.nav_privacy),
                    icon = OmniNavigationSymbol.PRIVACY,
                    onClick = onOpenPrivacy,
                    modifier = Modifier.weight(1f),
                )
                BentoShortcut(
                    label = if (premium) stringResource(R.string.pro_active)
                    else stringResource(R.string.nav_pro),
                    icon = OmniNavigationSymbol.PRO,
                    onClick = onOpenPro,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item(key = "safety") {
            Column(
                Modifier.fillMaxWidth()
                    .background(
                        if (dark) colors.panel else colors.sageBackground,
                        RoundedCornerShape(20.dp),
                    )
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    stringResource(R.string.bento_safety_title),
                    fontFamily = colors.display,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dark) colors.white else colors.forest,
                )
                Text(
                    stringResource(R.string.home_subtitle),
                    fontFamily = colors.body,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = if (dark) colors.white else colors.ink,
                )
                val status = when {
                    state.hasBroadAccess -> stringResource(R.string.access_ready)
                    state.storageCapability.standardCleaningAvailable ->
                        stringResource(R.string.redesign_standard_ready)
                    else -> stringResource(R.string.standard_cleaning_unavailable)
                }
                Text(
                    status,
                    fontFamily = colors.body,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dark) colors.lime else colors.forest,
                )
                if (state.storageCapability.canRequestDeepAccess) {
                    BentoSafetyAction(
                        title = stringResource(R.string.grant_deep_access),
                        onClick = onEnableDeepClean,
                    )
                }
                BentoSafetyAction(
                    title = stringResource(R.string.refresh),
                    onClick = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun OmniBentoFileArt() {
    Box(Modifier.size(width = 98.dp, height = 110.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(width = 62.dp, height = 84.dp)
                .graphicsLayer { rotationZ = 15f; translationX = 17.dp.toPx() }
                .background(OmniColorBento.violet, RoundedCornerShape(13.dp)),
        )
        Box(
            Modifier.size(width = 62.dp, height = 84.dp)
                .graphicsLayer { rotationZ = -9f; translationX = -8.dp.toPx() }
                .background(OmniColorBento.ochre, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            OmniBentoUtilityIcon(kind = 1, tint = OmniColorBento.ink, modifier = Modifier.size(33.dp))
        }
    }
}

private data class BentoTool(
    val title: Int,
    val symbol: OmniNavigationSymbol,
    val color: Color,
    val foreground: Color,
)

@Composable
private fun BentoToolDeck(
    onOpenClean: () -> Unit,
    onOpenLargeFiles: () -> Unit,
    onFindDuplicates: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenTrashQueue: () -> Unit,
) {
    val colors = OmniColorBento
    var expanded by rememberSaveable { mutableStateOf(false) }
    val actions = listOf(
        onOpenClean, onOpenLargeFiles, onFindDuplicates, onOpenGallery, onOpenTrashQueue,
    )
    val tools = listOf(
        BentoTool(R.string.bento_tool_clean, OmniNavigationSymbol.CLEAN, colors.blue, colors.white),
        BentoTool(R.string.bento_tool_large, OmniNavigationSymbol.CLEAN, Color(0xFFC02D25), colors.white),
        BentoTool(R.string.bento_tool_duplicates, OmniNavigationSymbol.GALLERY, colors.olive, colors.white),
        BentoTool(R.string.bento_tool_gallery, OmniNavigationSymbol.GALLERY, colors.magenta, colors.white),
        BentoTool(R.string.bento_tool_trash, OmniNavigationSymbol.CLEAN, colors.pink, colors.ink),
    )

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(27.dp))
            .background(colors.panel)
            .testTag("omni_bento_quick_tools")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.bento_quick_tools),
                    fontFamily = colors.display,
                    color = colors.white,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                )
                Text(
                    stringResource(
                        if (expanded) R.string.bento_deck_opened else R.string.bento_deck_tap_to_open,
                    ),
                    fontFamily = colors.body,
                    color = colors.white.copy(alpha = .82f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            Text(
                if (expanded) "−" else "+",
                color = colors.ink,
                fontSize = 26.sp,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.lime)
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .testTag("omni_bento_deck_toggle")
                    .wrapContentSize(Alignment.Center),
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = if (expanded) ((maxWidth - 12.dp) / 2f) else (maxWidth * .35f)
            val step = (maxWidth - cardWidth - 12.dp) / 4f
            val rowHeight = 115.dp
            val totalHeight by animateDpAsState(
                targetValue = if (expanded) rowHeight * 3 + 22.dp else 166.dp,
                animationSpec = tween(420),
                label = "deck-height",
            )
            Box(Modifier.fillMaxWidth().height(totalHeight)) {
                tools.forEachIndexed { index, tool ->
                    val x by animateDpAsState(
                        targetValue = if (expanded) (cardWidth + 12.dp) * (index % 2)
                        else step * index,
                        animationSpec = tween(420),
                        label = "deck-x-$index",
                    )
                    val y by animateDpAsState(
                        targetValue = if (expanded) (rowHeight + 10.dp) * (index / 2)
                        else if (index % 2 == 0) 12.dp else 23.dp,
                        animationSpec = tween(420),
                        label = "deck-y-$index",
                    )
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 0f else (index - 2) * 3f,
                        animationSpec = tween(420),
                        label = "deck-r-$index",
                    )
                    Box(
                        Modifier
                            .offset(x = x, y = y)
                            .width(cardWidth)
                            .height(if (expanded) 113.dp else 137.dp)
                            .graphicsLayer { rotationZ = rotation }
                            .zIndex(if (expanded) 0f else (tools.size - index).toFloat())
                            .clip(RoundedCornerShape(17.dp))
                            .background(tool.color)
                            .clickable(role = Role.Button) {
                                if (expanded) actions[index]() else expanded = true
                            }
                            .testTag("omni_bento_tool_$index")
                            .padding(12.dp),
                    ) {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            if (index == 1 || index == 2 || index == 4) {
                                OmniBentoUtilityIcon(
                                    kind = index,
                                    tint = tool.foreground,
                                    modifier = Modifier.size(26.dp),
                                )
                            } else {
                                OmniNavigationIcon(
                                    symbol = tool.symbol,
                                    tint = tool.foreground,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            Text(
                                stringResource(tool.title),
                                color = tool.foreground,
                                fontFamily = colors.body,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 14.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoShortcut(
    label: String,
    icon: OmniNavigationSymbol,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.heightIn(min = 103.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(OmniColorBento.white)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OmniNavigationIcon(icon, OmniColorBento.cobalt, Modifier.size(27.dp))
        Text(
            label,
            color = OmniColorBento.ink,
            fontFamily = OmniColorBento.body,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BentoSafetyAction(title: String, onClick: () -> Unit) {
    Text(
        title,
        color = OmniColorBento.ink,
        fontFamily = OmniColorBento.body,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(OmniColorBento.white)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

/** First-party settings glyph. Never render a platform-dependent emoji icon. */
@Composable
private fun OmniBentoSettingsGlyph() {
    Canvas(Modifier.size(23.dp)) {
        val radius = size.minDimension * .47f
        val line = 2.2.dp.toPx()
        for (tooth in 0 until 8) {
            val angle = tooth * (Math.PI / 4.0)
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            drawLine(
                color = OmniColorBento.white,
                start = center + Offset(dx * radius * .68f, dy * radius * .68f),
                end = center + Offset(dx * radius, dy * radius),
                strokeWidth = line,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(
            color = OmniColorBento.white,
            radius = radius * .72f,
            style = Stroke(width = line),
        )
        drawCircle(
            color = OmniColorBento.white,
            radius = radius * .21f,
            style = Stroke(width = line),
        )
    }
}
