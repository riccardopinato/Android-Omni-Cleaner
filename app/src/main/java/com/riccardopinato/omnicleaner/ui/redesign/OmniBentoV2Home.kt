package com.riccardopinato.omnicleaner.ui.redesign

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
    val bg = if (dark) colors.backgroundDark else colors.background
    val fg = if (dark) colors.white else colors.ink
    val reducedMotion = remember(context) {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            .getOrDefault(false)
    }

    fun commit(candidate: List<BentoV2Tile>) {
        if (bentoV2Pack(candidate) == null) {
            errorMessage = "Questa combinazione non riempie la griglia senza spazi interni."
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
                        Text(if (editing) "Trascina, ridimensiona e personalizza" else "Color Bento 2.0", color = fg.copy(alpha = .7f), fontFamily = colors.body, fontSize = 12.sp)
                    }
                    BentoV2Action(if (editing) "Fine" else "Modifica", colors.lime, colors.forest) {
                        editing = !editing
                        selectedId = null
                    }
                    if (!editing) BentoV2Action("⚙", colors.panel, colors.white, onOpenSettings)
                }
                if (editing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        BentoV2Action("+ Aggiungi", colors.lime, colors.forest) { showLibrary = true }
                        BentoV2Action("Ripristina", colors.pink, colors.ink) { showRestore = true }
                    }
                }
                if (!editing && (BentoScanRuntime.activeId != null || BentoScanRuntime.pending.isNotEmpty())) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(colors.panel).combinedClickable(onClick = { showQueue = true })
                            .padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Analisi in corso", color = colors.white, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("In coda: ${BentoScanRuntime.pending.size}  ›", color = colors.lime, fontSize = 12.sp)
                    }
                }
            }
        }
        item("bento_mosaic_v2") {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gap = 8.dp
                val cell = (maxWidth - gap * 2) / 3
                val height = cell * rowCount + gap * (rowCount - 1).coerceAtLeast(0)
                val density = LocalDensity.current
                val cellPx = with(density) { (cell + gap).toPx() }
                Box(Modifier.fillMaxWidth().height(height)) {
                    displayed.forEachIndexed { index, placement ->
                        val tile = placement.tile
                        val x by animateDpAsState(placement.column * (cell + gap), tween(210), label = "tile-x-${tile.id}")
                        val y by animateDpAsState(placement.row * (cell + gap), tween(210), label = "tile-y-${tile.id}")
                        val w = cell * tile.columns + gap * (tile.columns - 1)
                        val h = cell * tile.rows + gap * (tile.rows - 1)
                        var dragging by remember(tile.id) { mutableStateOf(Offset.Zero) }
                        val wiggle = if (editing && !reducedMotion) {
                            val transition = rememberInfiniteTransition(label = "wiggle-${tile.id}")
                            val a by transition.animateFloat(
                                initialValue = -0.85f, targetValue = 0.85f,
                                animationSpec = infiniteRepeatable(tween(320 + (index % 3) * 45), RepeatMode.Reverse),
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
                                        .background(colors.lime)
                                        .pointerInput(tile.id, cellPx) {
                                            detectDragGestures(
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    resizeDelta += amount
                                                },
                                                onDragEnd = {
                                                    val cw = (tile.columns + (resizeDelta.x / cellPx).roundToInt()).coerceIn(1, 3)
                                                    val rh = (tile.rows + (resizeDelta.y / cellPx).roundToInt()).coerceIn(1, 2)
                                                    resizeDelta = Offset.Zero
                                                    resize(tile.id, cw, rh)
                                                },
                                                onDragCancel = { resizeDelta = Offset.Zero },
                                            )
                                        },
                                    contentAlignment = Alignment.Center,
                                ) { Text("⤡", fontSize = 22.sp, color = colors.forest) }
                            }
                        }
                    }
                }
            }
        }
        item("bento_access") {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(if (dark) colors.panel else colors.sageBackground).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Accesso e protezione", color = if (dark) colors.white else colors.forest, fontWeight = FontWeight.Bold)
                val accessStatus = when {
                    state.hasBroadAccess -> stringResource(R.string.access_ready)
                    state.storageCapability.standardCleaningAvailable -> stringResource(R.string.redesign_standard_ready)
                    else -> stringResource(R.string.standard_cleaning_unavailable)
                }
                Text(accessStatus, color = if (dark) colors.white else colors.forest, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.storageCapability.canRequestDeepAccess) {
                        BentoV2Action(stringResource(R.string.grant_deep_access), colors.lime, colors.ink, onEnableDeepClean)
                    }
                    BentoV2Action(stringResource(R.string.refresh), colors.white, colors.ink, onRefresh)
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
                    Text("Dimensioni della tessera")
                    BENTO_V2_SIZES.sortedWith(compareBy({ it.second }, { it.first }))
                        .chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (w, h) ->
                                    BentoV2Action(
                                        "${w}×${h}",
                                        if (selectedTile.columns == w && selectedTile.rows == h) colors.lime else colors.pink,
                                        colors.ink,
                                    ) { resize(selectedTile.id, w, h); selectedId = null }
                                }
                            }
                        }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { selectedId = null }) { Text("Chiudi") } },
        )
    }
    if (showLibrary) {
        AlertDialog(
            onDismissRequest = { showLibrary = false },
            title = { Text("Aggiungi tessera") },
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
                            ) { Text(if (exists) "Presente" else "Aggiungi") }
                        }
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { showLibrary = false }) { Text("Chiudi") } },
        )
    }
    if (showRestore) {
        AlertDialog(
            onDismissRequest = { showRestore = false },
            title = { Text("Ripristinare la Home?") },
            text = { Text("L'ordine e le dimensioni personalizzate saranno sostituiti. I file non vengono modificati.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    preferences.reset(); tiles = bentoV2Defaults(); showRestore = false
                }) { Text("Ripristina") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showRestore = false }) { Text("Annulla") } },
        )
    }
    if (showQueue) {
        AlertDialog(
            onDismissRequest = { showQueue = false },
            title = { Text("Coda analisi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("In corso: ${BentoScanRuntime.activeId ?: "nessuna"}")
                    Text("In attesa: ${BentoScanRuntime.pending.joinToString().ifBlank { "nessuna" }}")
                    Text("Le analisi non eliminano alcun file.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showQueue = false }) { Text("Chiudi") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    BentoScanRuntime.cancel(context); showQueue = false
                }) { Text("Annulla analisi") }
            },
        )
    }
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Disposizione non disponibile") },
            text = { Text(errorMessage.orEmpty()) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { errorMessage = null }) { Text("OK") } },
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun BentoV2Action(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(13.dp)).background(bg)
            .combinedClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = fg, fontFamily = OmniColorBento.body, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun BentoV2MiniAction(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape).background(Color(0xEAF3F3F1))
            .combinedClickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = OmniColorBento.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
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
    val bg = when (tile.id) {
        "storage", "folders" -> colors.olive
        "duplicates", "filters" -> colors.magenta
        "large", "categories" -> colors.lime
        "gallery", "galleryPro", "galleryReview" -> colors.cobalt
        "privacy", "privacyQuick", "privacyBatch" -> colors.sageBackground
        "trash", "advanced" -> colors.panel
        "review", "intelligence" -> colors.blue
        "pro", "settings" -> colors.pink
        else -> colors.panel
    }
    val fg = if (bg == colors.lime || bg == colors.sageBackground || bg == colors.pink)
        colors.ink else colors.white
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
        "pro" -> if (premium) "Pro attivo" else ""
        else -> ""
    }
    val compact = tile.columns == 1 && tile.rows == 1
    val tall = tile.rows == 2
    val title = stringResource(spec.title)
    val status = when {
        queued -> "In attesa"
        showScan -> "Analisi in corso…"
        spec.scan && scanDone -> "Apri risultati"
        spec.scan -> "Tocca per analizzare"
        else -> "Apri"
    }
    Column(
        modifier = modifier.background(bg).padding(if (compact) 9.dp else 14.dp),
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
            OmniNavigationIcon(symbol = icon, tint = if (bg == colors.lime) colors.forest else fg,
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
                    color = fg.copy(alpha = .84f), fontFamily = colors.body, fontSize = 11.sp,
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
                    Text("Risultato precedente · Aggiornamento consigliato", fontSize = 10.sp,
                        color = fg.copy(alpha = .9f), maxLines = 2)
                }
            }
        }
    }
}
