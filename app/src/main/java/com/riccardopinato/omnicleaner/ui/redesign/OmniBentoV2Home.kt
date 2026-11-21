package com.riccardopinato.omnicleaner.ui.redesign

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.gallery.GalleryController
import com.riccardopinato.omnicleaner.ui.CleanerUiState
import com.riccardopinato.omnicleaner.ui.BentoScanRuntime
import kotlin.math.roundToInt

/**
 * Native three-column Color Bento 2.0.
 *
 * UI controls and scan results are separate. Cards NEVER delete media,
 * trigger Android trash requests, or claim speculative savings.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun OmniBentoV2Home(
    state: CleanerUiState,
    galleryController: GalleryController,
    premium: Boolean,
    dark: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onEnableDeepClean: () -> Unit,
    onOpenTile: (String) -> Unit,
    onScanTile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = OmniColorBento
    val palette = OmniColorBentoV2Palette
    val preferences = remember(context) { BentoV2Preferences(context) }
    var tiles by remember(context) { mutableStateOf(preferences.load()) }
    var editing by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showLibrary by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showRestore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var galleryState by remember(galleryController) { mutableStateOf(galleryController.state) }
    DisposableEffect(galleryController) {
        val unsubscribe = galleryController.observe { galleryState = it }
        onDispose { unsubscribe() }
    }
    val definitions = remember { BENTO_V2_LIBRARY.associateBy { it.id } }
    val displayed = remember(tiles) { bentoV2Pack(tiles) ?: emptyList() }
    val rowCount = displayed.maxOfOrNull { it.row + it.tile.rows } ?: 0
    val bg = if (dark) palette.legibleInk else palette.cream
    val fg = if (dark) colors.white else palette.legibleInk
    val invalidLayoutText = stringResource(R.string.bento_v2_invalid_layout)
    val reducedMotion = remember(context) {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            .getOrDefault(false)
    }

    fun commit(candidate: List<BentoV2Tile>) {
        if (bentoV2Pack(candidate) == null) {
            errorMessage = invalidLayoutText
            return
        }
        tiles = candidate
        preferences.save(candidate)
        errorMessage = null
    }
    fun resize(id: String, width: Int, height: Int) {
        if ((width to height) !in BENTO_V2_SIZES) return
        commit(tiles.map { if (it.id == id) it.copy(columns = width, rows = height) else it })
    }
    fun reorder(id: String, offset: Int) {
        val from = tiles.indexOfFirst { it.id == id }
        if (from < 0) return
        val reordered = tiles.toMutableList()
        val entry = reordered.removeAt(from)
        reordered.add((from + offset).coerceIn(0, reordered.size), entry)
        commit(reordered)
    }
    fun tapTile(id: String) {
        if (editing) {
            selectedId = id
            return
        }
        val spec = definitions[id] ?: return
        if (spec.scan) {
            val scanned = when (id) {
                "storage" -> state.overview != null
                "duplicates" -> state.duplicatesScanned
                "large" -> state.bigFilesScanned
                "folders" -> state.foldersScanned
                else -> false
            }
            if (scanned && BentoScanRuntime.activeId != id) onOpenTile(id)
            else onScanTile(id)
        } else onOpenTile(id)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(bg).testTag("omni_color_bento_home"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 13.dp, end = 13.dp, top = 16.dp, bottom = 28.dp,
        ),
    ) {
        item("bento_header_v2") {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Omni Cleaner", color = fg, fontFamily = colors.display, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text(if (editing) stringResource(R.string.bento_v2_subtitle) else "Color Bento 2.0", color = fg.copy(alpha = .7f), fontFamily = colors.body, fontSize = 12.sp)
                    }
                    BentoV2Action(if (editing) stringResource(R.string.bento_v2_done) else stringResource(R.string.bento_v2_edit), palette.yellow, palette.legibleInk, tag = "omni_bento_v2_editor_toggle") {
                        editing = !editing
                        selectedId = null
                    }
                    if (!editing) BentoV2Action("⚙", palette.blue, palette.white, tag = "omni_home_settings", semanticLabel = stringResource(R.string.audit_settings), onClick = onOpenSettings)
                }
                if (editing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        BentoV2Action(stringResource(R.string.bento_v2_add), palette.yellow, palette.legibleInk) { showLibrary = true }
                        BentoV2Action(stringResource(R.string.bento_v2_reset), palette.blush, palette.legibleInk) { showRestore = true }
                    }
                }
                if (!editing && (BentoScanRuntime.activeId != null || BentoScanRuntime.pending.isNotEmpty())) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(palette.blue).combinedClickable(onClick = { showQueue = true })
                            .padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.bento_v2_scanning), color = palette.white, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.bento_v2_queue_count, BentoScanRuntime.pending.size) + "  ›", color = palette.white, fontSize = 12.sp)
                    }
                }
            }
        }
        item("bento_mosaic_v2") {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gap = 8.dp
                val cell = (maxWidth - gap * 2) / 3
                // Keep a ghost layout only while the user drags the resize handle.
                // Preview is never written to preferences before release.
                var resizePreview by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }
                val previewPosition = displayed.firstOrNull { it.tile.id == resizePreview?.first }
                val previewRowEnd = if (previewPosition != null && resizePreview != null) {
                    previewPosition.row + resizePreview!!.third
                } else 0
                val renderedRows = maxOf(rowCount, previewRowEnd)
                val height = cell * renderedRows + gap * (renderedRows - 1).coerceAtLeast(0)
                val density = LocalDensity.current
                val cellPx = with(density) { (cell + gap).toPx() }
                Box(Modifier.fillMaxWidth().height(height)) {
                    displayed.forEachIndexed { index, placement ->
                        val tile = placement.tile
                        val x by animateDpAsState((cell + gap) * placement.column, tween(210), label = "tile-x-${tile.id}")
                        val y by animateDpAsState((cell + gap) * placement.row, tween(210), label = "tile-y-${tile.id}")
                        val w = cell * tile.columns + gap * (tile.columns - 1)
                        val h = cell * tile.rows + gap * (tile.rows - 1)
                        var dragging by remember(tile.id) { mutableStateOf(Offset.Zero) }
                        val wiggle = if (editing && !reducedMotion) {
                            val transition = rememberInfiniteTransition(label = "wiggle-${tile.id}")
                            val a by transition.animateFloat(
                                initialValue = -0.85f, targetValue = 0.85f,
                                animationSpec = infiniteRepeatable(tween(640 + (index % 3) * 90), RepeatMode.Reverse),
                                label = "wiggle-angle-${tile.id}",
                            )
                            if (index % 2 == 0) a else -a
                        } else 0f
                        Box(
                            modifier = Modifier
                                .zIndex(if (dragging != Offset.Zero) 10f else 0f)
                                .graphicsLayer {
                                    translationX = dragging.x
                                    translationY = dragging.y
                                    rotationZ = if (dragging != Offset.Zero) 0f else wiggle
                                }
                                .offset(x = x, y = y)
                                .width(w).height(h)
                                .then(
                                    if (editing) Modifier.pointerInput(tile.id, editing, cellPx) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { selectedId = tile.id },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragging += dragAmount
                                            },
                                            onDragEnd = {
                                                val dx = (dragging.x / cellPx).roundToInt()
                                                val dy = (dragging.y / cellPx).roundToInt()
                                                dragging = Offset.Zero
                                                if (dx != 0 || dy != 0) reorder(tile.id, dy * 3 + dx)
                                            },
                                            onDragCancel = { dragging = Offset.Zero },
                                        )
                                    } else Modifier
                                )
                                .clip(RoundedCornerShape(22.dp))
                                .testTag("omni_bento_v2_${tile.id}")
                                .combinedClickable(
                                    role = Role.Button,
                                    onClick = { tapTile(tile.id) },
                                    onLongClick = { editing = true; selectedId = tile.id },
                                ),
                        ) {
                            BentoV2Card(
                                tile = tile,
                                state = state,
                                galleryCount = galleryState.loadedSourceCount,
                                premium = premium,
                                editing = editing,
                                showScan = BentoScanRuntime.activeId == tile.id,
                                queued = BentoScanRuntime.pending.contains(tile.id),
                                completedAt = BentoScanRuntime.completedAt[tile.id],
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (editing) {
                                Row(
                                    Modifier.align(Alignment.TopEnd).padding(5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    BentoV2MiniAction("−") {
                                        commit(tiles.map { if (it.id == tile.id) it.copy(visible = false) else it })
                                    }
                                    BentoV2MiniAction("↗") { selectedId = tile.id }
                                }
                                var resizeDelta by remember(tile.id) { mutableStateOf(Offset.Zero) }
                                Box(
                                    Modifier.align(Alignment.BottomEnd)
                                        .size(37.dp)
                                        .clip(RoundedCornerShape(topStart = 15.dp, bottomEnd = 18.dp))
                                        .background(palette.yellow)
                                        .pointerInput(tile.id, cellPx) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    resizeDelta = Offset.Zero
                                                    resizePreview = Triple(tile.id, tile.columns, tile.rows)
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    resizeDelta += amount
                                                    val (previewCols, previewRows) = bentoV2ResizeFromDrag(
                                                        tile, resizeDelta.x, resizeDelta.y, cellPx,
                                                    )
                                                    resizePreview = Triple(tile.id, previewCols, previewRows)
                                                },
                                                onDragEnd = {
                                                    val (columns, rows) = bentoV2ResizeFromDrag(
                                                        tile, resizeDelta.x, resizeDelta.y, cellPx,
                                                    )
                                                    resizeDelta = Offset.Zero
                                                    resizePreview = null
                                                    resize(tile.id, columns, rows)
                                                },
                                                onDragCancel = {
                                                    resizeDelta = Offset.Zero
                                                    resizePreview = null
                                                },
                                            )
                                        },
                                    contentAlignment = Alignment.Center,
                                ) { Text("⤡", fontSize = 22.sp, color = palette.legibleInk) }
                            }
                        }
                    }
                    // Overlay is drawn on the mosaic (not inside the clipped tile),
                    // so the dashed guide remains visible when enlarging a 1×1 tile.
                    val preview = resizePreview
                    val origin = previewPosition
                    if (preview != null && origin != null) {
                        val previewCols = preview.second
                        val previewRows = preview.third
                        val visualColumn = origin.column.coerceAtMost(3 - previewCols)
                        val guideWidth = cell * previewCols + gap * (previewCols - 1)
                        val guideHeight = cell * previewRows + gap * (previewRows - 1)
                        val borderColor = palette.raspberry
                        Box(
                            Modifier
                                .offset(
                                    x = (cell + gap) * visualColumn,
                                    y = (cell + gap) * origin.row,
                                )
                                .width(guideWidth).height(guideHeight)
                                .zIndex(30f)
                                .testTag("omni_bento_v2_resize_guide"),
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                val dash = PathEffect.dashPathEffect(
                                    floatArrayOf(9.dp.toPx(), 6.dp.toPx()),
                                )
                                val guideStroke = 2.5.dp.toPx()
                                val rounding = CornerRadius(19.dp.toPx())
                                drawRoundRect(
                                    color = Color.Black.copy(alpha = 0.2f),
                                    cornerRadius = rounding,
                                )
                                drawRoundRect(
                                    color = Color.Black.copy(alpha = .85f),
                                    cornerRadius = rounding,
                                    style = Stroke(width = 5.dp.toPx(), pathEffect = dash),
                                )
                                drawRoundRect(
                                    color = borderColor,
                                    cornerRadius = rounding,
                                    style = Stroke(width = guideStroke, pathEffect = dash),
                                )
                                val stride = cellPx
                                val lineOffset = gap.toPx() / 2f
                                for (c in 1 until previewCols) {
                                    val xx = stride * c - lineOffset
                                    drawLine(
                                        color = borderColor, start = Offset(xx, 0f),
                                        end = Offset(xx, size.height),
                                        strokeWidth = guideStroke, pathEffect = dash,
                                    )
                                }
                                for (r in 1 until previewRows) {
                                    val yy = stride * r - lineOffset
                                    drawLine(
                                        color = borderColor, start = Offset(0f, yy),
                                        end = Offset(size.width, yy),
                                        strokeWidth = guideStroke, pathEffect = dash,
                                    )
                                }
                            }
                            Text(
                                "${previewCols}×${previewRows}",
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(palette.legibleInk)
                                    .testTag("omni_bento_v2_resize_size")
                                    .padding(horizontal = 11.dp, vertical = 6.dp),
                                color = palette.cream,
                                fontFamily = colors.display,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }
        }
        item("bento_access") {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(palette.sage).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.bento_v2_access), color = palette.legibleInk, fontWeight = FontWeight.Bold)
                val accessStatus = when {
                    state.hasBroadAccess -> stringResource(R.string.access_ready)
                    state.storageCapability.standardCleaningAvailable -> stringResource(R.string.redesign_standard_ready)
                    else -> stringResource(R.string.standard_cleaning_unavailable)
                }
                Text(accessStatus, color = palette.legibleInk, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.storageCapability.canRequestDeepAccess) {
                        BentoV2Action(stringResource(R.string.grant_deep_access), palette.yellow, palette.legibleInk, onClick = onEnableDeepClean)
                    }
                    BentoV2Action(stringResource(R.string.refresh), palette.cream, palette.legibleInk, onClick = onRefresh)
                }
            }
        }
    }

    val selectedTile = tiles.firstOrNull { it.id == selectedId }
    if (editing && selectedTile != null) {
        val title = definitions[selectedTile.id]?.title?.let { stringResource(it) }.orEmpty()
        AlertDialog(
            onDismissRequest = { selectedId = null },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(stringResource(R.string.bento_v2_size))
                    BENTO_V2_SIZES.sortedWith(compareBy({ it.second }, { it.first }))
                        .chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (w, h) ->
                                    BentoV2Action(
                                        "${w}×${h}",
                                        if (selectedTile.columns == w && selectedTile.rows == h) palette.yellow else palette.blush,
                                        palette.legibleInk,
                                    ) { resize(selectedTile.id, w, h); selectedId = null }
                                }
                            }
                        }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { selectedId = null }) { Text(stringResource(R.string.bento_v2_close)) } },
        )
    }
    if (showLibrary) {
        AlertDialog(
            onDismissRequest = { showLibrary = false },
            title = { Text(stringResource(R.string.bento_v2_add_tile)) },
            text = {
                LazyColumn(Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BENTO_V2_LIBRARY.size) { index ->
                        val def = BENTO_V2_LIBRARY[index]
                        val exists = tiles.any { it.id == def.id && it.visible }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(def.title), modifier = Modifier.weight(1f), fontSize = 13.sp)
                            androidx.compose.material3.TextButton(
                                enabled = !exists,
                                onClick = {
                                    commit(tiles.map { if (it.id == def.id) it.copy(visible = true) else it })
                                    showLibrary = false
                                },
                            ) { Text(if (exists) stringResource(R.string.bento_v2_already) else stringResource(R.string.bento_v2_add_tile)) }
                        }
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { showLibrary = false }) { Text(stringResource(R.string.bento_v2_close)) } },
        )
    }
    if (showRestore) {
        AlertDialog(
            onDismissRequest = { showRestore = false },
            title = { Text(stringResource(R.string.bento_v2_reset_title)) },
            text = { Text(stringResource(R.string.bento_v2_reset_desc)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    preferences.reset(); tiles = bentoV2Defaults(); showRestore = false
                }) { Text(stringResource(R.string.bento_v2_reset)) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showRestore = false }) { Text(stringResource(R.string.bento_v2_cancel)) } },
        )
    }
    if (showQueue) {
        AlertDialog(
            onDismissRequest = { showQueue = false },
            title = { Text(stringResource(R.string.bento_v2_scan_queue)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.bento_v2_active, BentoScanRuntime.activeId ?: stringResource(R.string.bento_v2_none)))
                    Text(stringResource(R.string.bento_v2_waiting, BentoScanRuntime.pending.joinToString().ifBlank { stringResource(R.string.bento_v2_none) }))
                    Text(stringResource(R.string.bento_v2_no_delete), fontSize = 12.sp)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showQueue = false }) { Text(stringResource(R.string.bento_v2_close)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    BentoScanRuntime.cancel(context); showQueue = false
                }) { Text(stringResource(R.string.bento_v2_cancel_scans)) }
            },
        )
    }
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.bento_v2_unavailable)) },
            text = { Text(errorMessage.orEmpty()) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { errorMessage = null }) { Text("OK") } },
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun BentoV2Action(label: String, bg: Color, fg: Color, tag: String? = null, semanticLabel: String? = null, onClick: () -> Unit) {
    Box(
        Modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .then(if (semanticLabel != null) Modifier.semantics { contentDescription = semanticLabel } else Modifier)
            .clip(RoundedCornerShape(13.dp)).background(bg)
            .combinedClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = fg, fontFamily = OmniColorBento.body, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun BentoV2MiniAction(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape).background(OmniColorBentoV2Palette.cream)
            .combinedClickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = OmniColorBentoV2Palette.legibleInk, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
}

@Composable
private fun BentoV2Card(
    tile: BentoV2Tile,
    state: CleanerUiState,
    galleryCount: Int,
    premium: Boolean,
    editing: Boolean,
    showScan: Boolean,
    queued: Boolean,
    completedAt: Long?,
    modifier: Modifier,
) {
    val spec = BENTO_V2_LIBRARY.first { it.id == tile.id }
    val colors = OmniColorBento
    val palette = OmniColorBentoV2Palette
    val bg = palette.tileColor(tile.id)
    val fg = palette.tileForeground(tile.id)
    val scanDone = when (tile.id) {
        "storage" -> state.overview != null
        "duplicates" -> state.duplicatesScanned
        "large" -> state.bigFilesScanned
        "folders" -> state.foldersScanned
        else -> false
    }
    val metric = when (tile.id) {
        "storage" -> state.storageInfo?.let { bentoByteLabel(it.usedBytes) } ?: "—"
        "duplicates" -> bentoVerifiedCount(state.duplicatesScanned, state.duplicates.sumOf { it.totalDuplicatesCount })
        "large" -> if (state.bigFilesScanned) bentoByteLabel(state.bigFiles.sumOf { it.sizeBytes.coerceAtLeast(0L) }) else "—"
        "folders" -> if (state.foldersScanned) state.folders.size.toString() else "—"
        "gallery" -> if (galleryCount > 0) galleryCount.toString() else "—"
        "pro" -> if (premium) stringResource(R.string.bento_v2_premium_active) else ""
        else -> ""
    }
    val compact = tile.columns == 1 && tile.rows == 1
    val tall = tile.rows == 2
    val title = stringResource(spec.title)
    val status = when {
        queued -> stringResource(R.string.bento_v2_waiting_state)
        showScan -> stringResource(R.string.bento_v2_scan_state)
        spec.scan && scanDone -> stringResource(R.string.bento_v2_open_results)
        spec.scan -> stringResource(R.string.bento_v2_tap_scan)
        else -> stringResource(R.string.bento_v2_open)
    }
    Column(
        modifier = modifier.background(bg)
            .then(
                if (tile.id == "intelligence" || tile.id == "settings") {
                    Modifier.border(1.dp, palette.sage, RoundedCornerShape(22.dp))
                } else Modifier
            )
            .padding(if (compact) 9.dp else 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            val icon = when (tile.id) {
                "gallery", "galleryPro", "galleryReview" -> OmniNavigationSymbol.GALLERY
                "privacy", "privacyQuick", "privacyBatch" -> OmniNavigationSymbol.PRIVACY
                "pro" -> OmniNavigationSymbol.PRO
                "settings" -> OmniNavigationSymbol.HOME
                else -> OmniNavigationSymbol.CLEAN
            }
            OmniNavigationIcon(symbol = icon, tint = fg,
                modifier = Modifier.size(if (compact) 24.dp else 32.dp))
            if (!editing && scanDone && spec.scan && !showScan && !queued) {
                Text("↗", color = fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 5.dp)) {
            if (!compact && tall && metric.isNotBlank()) {
                Text(metric, color = fg, fontFamily = colors.display,
                    fontSize = if (tile.columns >= 2) 27.sp else 17.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(title, color = fg, fontFamily = colors.display,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 11.sp else if (tile.columns == 1) 13.sp else 20.sp,
                lineHeight = if (compact) 13.sp else if (tile.columns == 1) 16.sp else 24.sp,
                maxLines = if (compact) 2 else 3, overflow = TextOverflow.Ellipsis)
            if (!compact && !editing) {
                Text(
                    if (metric.isNotBlank() && !tall) "$metric · $status" else status,
                    color = if (fg == palette.white) fg else fg.copy(alpha = .84f),
                     fontFamily = colors.body, fontSize = 11.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (showScan) {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = fg, trackColor = fg.copy(alpha = .3f),
                    )
                }
                if (spec.scan && scanDone && completedAt != null &&
                    System.currentTimeMillis() - completedAt > 24L * 60L * 60L * 1000L
                ) {
                    Text(stringResource(R.string.bento_v2_stale), fontSize = 10.sp,
                        color = if (fg == palette.white) fg else fg.copy(alpha = .9f), maxLines = 2)
                }
            }
        }
    }
}
