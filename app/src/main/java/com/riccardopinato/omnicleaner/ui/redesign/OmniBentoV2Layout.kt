package com.riccardopinato.omnicleaner.ui.redesign

import android.content.Context
import com.riccardopinato.omnicleaner.R
import kotlin.math.roundToInt

/** Color Bento 2.0: one widget per real feature, not one per shortcut appearance. */
internal data class BentoV2Tile(
    val id: String,
    val columns: Int,
    val rows: Int,
    val visible: Boolean = true,
) {
    fun sizeIsValid() = (columns to rows) in BENTO_V2_SIZES
}

internal data class BentoV2Position(val tile: BentoV2Tile, val column: Int, val row: Int)

/**
 * Snaps handle movement to the six allowed tile formats. Used for both the live
 * dashed guide and the final committed size so the preview cannot lie.
 */
internal fun bentoV2ResizeFromDrag(
    tile: BentoV2Tile,
    dxPx: Float,
    dyPx: Float,
    stepPx: Float,
): Pair<Int, Int> {
    if (stepPx <= 0f || !stepPx.isFinite()) return tile.columns to tile.rows
    val columns = (tile.columns + (dxPx / stepPx).roundToInt()).coerceIn(1, 3)
    val rows = (tile.rows + (dyPx / stepPx).roundToInt()).coerceIn(1, 2)
    return columns to rows
}
/**
 * Accessible Bento cards keep their 3-column widths, but grow vertically
 * at large Android font sizes instead of clipping titles or status labels.
 * This is a presentation-only rule: saved tile sizes and grid positions stay
 * unchanged and no user preferences are migrated.
 */
internal fun bentoV2RowHeightDp(cellWidthDp: Float, fontScale: Float): Float {
    if (cellWidthDp <= 0f || !cellWidthDp.isFinite()) return 0f
    if (!fontScale.isFinite() || fontScale <= 1.1f) return cellWidthDp
    return maxOf(cellWidthDp, 90f * fontScale)
}

internal val BENTO_V2_SIZES = setOf(1 to 1, 2 to 1, 1 to 2, 2 to 2, 3 to 1, 3 to 2)

internal data class BentoV2Definition(
    val id: String,
    val title: Int,
    val initialColumns: Int,
    val initialRows: Int,
    val initiallyVisible: Boolean,
    val scan: Boolean = false,
)

/** All destinations are already present in the app; tiles do not introduce new cleanup actions. */
/**
 * Short, localized *visual* labels for one-column cards at large Android font
 * scales. Names stay full in accessibility semantics and dedicated screens.
 * Never derive these by truncating words: short labels are intentionally
 * translated and readable at 360dp / 200% text.
 */
internal fun bentoV2AccessibleNarrowTitleRes(id: String): Int? = when (id) {
    "storage" -> R.string.bento_v2_narrow_storage
    "duplicates" -> R.string.bento_v2_narrow_duplicates
    "large" -> R.string.bento_v2_narrow_large
    "gallery" -> R.string.bento_v2_narrow_gallery
    "privacy" -> R.string.bento_v2_narrow_privacy
    "trash" -> R.string.bento_v2_narrow_trash
    "folders" -> R.string.bento_v2_narrow_folders
    "review" -> R.string.bento_v2_narrow_review
    "galleryReview" -> R.string.bento_v2_narrow_gallery_review
    "intelligence" -> R.string.bento_v2_narrow_intelligence
    "galleryPro" -> R.string.bento_v2_narrow_gallery_pro
    "filters" -> R.string.bento_v2_narrow_filters
    "pro" -> R.string.bento_v2_narrow_pro
    "categories" -> R.string.bento_v2_narrow_categories
    "advanced" -> R.string.bento_v2_narrow_advanced
    "privacyBatch" -> R.string.bento_v2_narrow_privacy_batch
    else -> null
}

internal val BENTO_V2_LIBRARY = listOf(
    // Every distinct feature is visible on the unified Home by default.
    // Privacy opens the single-photo workflow; batch privacy is a separate tile.
    // Settings remains in the Home header, not as a duplicate tile.
    BentoV2Definition("storage", R.string.module_storage_overview, 2, 2, true, true),
    BentoV2Definition("duplicates", R.string.scan_duplicates, 1, 1, true, true),
    BentoV2Definition("large", R.string.scan_big_files, 1, 1, true, true),
    BentoV2Definition("gallery", R.string.nav_gallery, 3, 1, true),
    BentoV2Definition("privacy", R.string.nav_privacy, 1, 2, true),
    BentoV2Definition("trash", R.string.module_safe_trash, 2, 1, true),
    BentoV2Definition("folders", R.string.scan_folders, 2, 1, true, true),
    BentoV2Definition("review", R.string.review_center_title, 1, 1, true),
    BentoV2Definition("galleryReview", R.string.nav_gallery_review, 2, 1, true),
    BentoV2Definition("intelligence", R.string.si_title, 3, 2, true),
    BentoV2Definition("galleryPro", R.string.gp_title, 2, 2, true),
    BentoV2Definition("filters", R.string.redesign_filters_title, 1, 1, true),
    BentoV2Definition("pro", R.string.nav_pro, 1, 1, true),
    BentoV2Definition("categories", R.string.smart_categories_title, 2, 2, true),
    BentoV2Definition("advanced", R.string.audit_advanced_maintenance, 1, 1, true),
    BentoV2Definition("privacyBatch", R.string.nav_privacy_batch, 1, 1, true),

)

internal fun bentoV2Defaults() = BENTO_V2_LIBRARY.map {
    BentoV2Tile(it.id, it.initialColumns, it.initialRows, it.initiallyVisible)
}

/**
 * Places all widgets into a three-column strip with no interior empty cells.
 * Any unavoidable blank cells are confined to the trailing end of the last row.
 * A size/order that cannot be packed without holes is rejected, never silently resized.
 */
internal fun bentoV2Pack(tiles: List<BentoV2Tile>): List<BentoV2Position>? {
    val selected = tiles.filter { it.visible }
    if (selected.isEmpty()) return emptyList()
    if (selected.any { !it.sizeIsValid() } || selected.map { it.id }.distinct().size != selected.size) return null
    val totalArea = selected.sumOf { it.columns * it.rows }
    val rowCount = (totalArea + 2) / 3
    // In a strip, a 2-row tile requires at least 2 rows.
    if (selected.any { it.rows > rowCount }) return null
    val cells = BooleanArray(rowCount * 3)
    val trailing = rowCount * 3 - totalArea
    repeat(trailing) { offset -> cells[cells.lastIndex - offset] = true }
    val positions = arrayOfNulls<BentoV2Position>(selected.size)
    val used = BooleanArray(selected.size)
    val budget = intArrayOf(18000)

    fun search(filled: Int): Boolean {
        if (--budget[0] < 0) return false
        if (filled == selected.size) return true
        val firstEmpty = cells.indexOfFirst { !it }
        if (firstEmpty == -1) return filled == selected.size
        val row = firstEmpty / 3
        val col = firstEmpty % 3
        for (i in selected.indices) {
            if (used[i]) continue
            val tile = selected[i]
            if (col + tile.columns > 3 || row + tile.rows > rowCount) continue
            if ((0 until tile.rows).any { dy ->
                    (0 until tile.columns).any { dx -> cells[(row + dy) * 3 + col + dx] }
                }) continue
            for (dy in 0 until tile.rows) for (dx in 0 until tile.columns) {
                cells[(row + dy) * 3 + col + dx] = true
            }
            used[i] = true
            positions[i] = BentoV2Position(tile, col, row)
            if (search(filled + 1)) return true
            positions[i] = null
            used[i] = false
            for (dy in 0 until tile.rows) for (dx in 0 until tile.columns) {
                cells[(row + dy) * 3 + col + dx] = false
            }
        }
        return false
    }
    return if (search(0)) positions.filterNotNull() else null
}

/**
 * Pop Fresh positional harmonizer.
 *
 * The nine approved swatches are assigned to VISIBLE RECTANGLES in reading
 * order, not to feature IDs. Shape/edge adjacency (including 2x2 cards) and
 * distribution are considered. No randomness, persisted color choices, or
 * user data are involved: a given packed mosaic always produces one palette.
 *
 * The editor computes colors only for the last committed packed layout, not
 * a transient drag/resize preview. Changes are applied on release/commit.
 */
internal const val BENTO_V2_SWATCH_COUNT = 9
private val BENTO_V2_COLOR_FAMILY = intArrayOf(
    0, // Lime Pop: green
    1, // Electric Blue: blue
    2, // Tangerine: warm
    3, // Soft Violet: violet/pink
    2, // Sunshine: warm
    1, // Aqua Fresh: blue/aqua
    2, // Coral Pop: warm
    0, // Spring Green: green
    3, // Bubblegum: violet/pink
)

/** Number of shared grid-cell edges; corner-only contacts are not neighbors. */
internal fun bentoV2SharedBorderUnits(a: BentoV2Position, b: BentoV2Position): Int {
    val aRight = a.column + a.tile.columns
    val bRight = b.column + b.tile.columns
    val aBottom = a.row + a.tile.rows
    val bBottom = b.row + b.tile.rows
    return when {
        aRight == b.column || bRight == a.column ->
            (minOf(aBottom, bBottom) - maxOf(a.row, b.row)).coerceAtLeast(0)
        aBottom == b.row || bBottom == a.row ->
            (minOf(aRight, bRight) - maxOf(a.column, b.column)).coerceAtLeast(0)
        else -> 0
    }
}

internal fun bentoV2HarmonizedColorIndices(
    positions: List<BentoV2Position>,
): Map<BentoV2Position, Int> {
    val placed = linkedMapOf<BentoV2Position, Int>()
    val visualOrder = positions.sortedWith(
        compareBy<BentoV2Position> { it.row }.thenBy { it.column },
    )
    for ((slot, position) in visualOrder.withIndex()) {
        val preferred = slot % BENTO_V2_SWATCH_COUNT
        // Every candidate is scored against *actual* touching rectangles, not
        // the position of a feature in the persisted list.
        val selected = (0 until BENTO_V2_SWATCH_COUNT).minWithOrNull(
            compareBy<Int> { candidate ->
                val reuse = placed.values.count { it == candidate }
                var score = reuse * 300 +
                    ((candidate - preferred + BENTO_V2_SWATCH_COUNT) % BENTO_V2_SWATCH_COUNT) * 11
                for ((neighbor, oldColor) in placed) {
                    val contact = bentoV2SharedBorderUnits(position, neighbor)
                    if (contact == 0) continue
                    score += when {
                        candidate == oldColor -> 1800 * contact
                        BENTO_V2_COLOR_FAMILY[candidate] == BENTO_V2_COLOR_FAMILY[oldColor] ->
                            175 * contact
                        else -> 0
                    }
                }
                score
            }.thenBy { it },
        ) ?: preferred
        placed[position] = selected
    }
    return placed
}

/** Persist only the order, size and visibility. Never persist file names or personal media. */
internal class BentoV2Preferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("omni_color_bento_v2", Context.MODE_PRIVATE)

    fun load(): List<BentoV2Tile> {
        val raw = prefs.getString("tiles", null) ?: return bentoV2Defaults()
        // Migrate only the untouched legacy preset. Personal layouts keep
        // their chosen tile visibility, sizes and positions.
        if (raw == "storage,2,2,1;duplicates,1,1,1;large,1,1,1;gallery,3,1,1;privacy,1,2,1;trash,2,1,1;folders,2,1,1;review,1,1,1;intelligence,3,2,1;galleryPro,2,2,0;galleryReview,2,1,0;privacyQuick,1,1,0;privacyBatch,2,1,0;filters,1,1,0;categories,2,2,0;advanced,2,1,0;pro,1,1,0;settings,1,1,0") {
            reset()
            return bentoV2Defaults()
        }
        val definitions = BENTO_V2_LIBRARY.associateBy { it.id }
        val loaded = raw.split(";").mapNotNull { chunk ->
            val p = chunk.split(",")
            if (p.size != 4 || !definitions.containsKey(p[0])) return@mapNotNull null
            val w = p[1].toIntOrNull() ?: return@mapNotNull null
            val h = p[2].toIntOrNull() ?: return@mapNotNull null
            BentoV2Tile(p[0], w, h, p[3] == "1").takeIf { it.sizeIsValid() }
        }.distinctBy { it.id }
        return (loaded + bentoV2Defaults().filter { def -> loaded.none { it.id == def.id } })
            .takeIf { bentoV2Pack(it) != null } ?: bentoV2Defaults()
    }

    fun save(tiles: List<BentoV2Tile>) {
        require(bentoV2Pack(tiles) != null)
        prefs.edit().putString("tiles", tiles.joinToString(";") {
            "${it.id},${it.columns},${it.rows},${if (it.visible) 1 else 0}"
        }).apply()
    }

    fun reset() = prefs.edit().remove("tiles").apply()
}
