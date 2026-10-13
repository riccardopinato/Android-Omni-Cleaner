package com.riccardopinato.omnicleaner.gallery

import android.app.PendingIntent
import android.content.Context
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.MediaStoreHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class GalleryUiState(
    val accessHealth: GalleryAccessHealth = GalleryAccessHealth(
        level = GalleryAccessLevel.NONE,
        canReadImages = false,
        canReadVideos = false,
    ),
    val loading: Boolean = false,
    val loadingNextPage: Boolean = false,
    val hasMore: Boolean = false,
    val items: List<GalleryMediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val loadedSourceCount: Int = 0,
    val sourceOffset: Int = 0,
    val history: List<GallerySwipeHistoryItem> = emptyList(),
    val pendingTrashItems: List<GalleryMediaItem> = emptyList(),
    val filter: GalleryMediaFilter = GalleryMediaFilter.ALL,
    val sortOrder: GallerySortOrder = GallerySortOrder.DATE_DESC,
    val totalReviewedCount: Int = 0,
    val error: String? = null,
) {
    val hasAccess: Boolean
        get() = accessHealth.hasAccess

    val currentItem: GalleryMediaItem?
        get() = items.getOrNull(currentIndex)

    val selectedForTrash: List<GalleryMediaItem>
        get() = (pendingTrashItems.asSequence() +
            items.asSequence().filter { it.isSelectedForTrash })
            .distinctBy { it.id }
            .toList()
}

class GalleryController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: GalleryPreferencesManager = GalleryPreferencesManager(context),
) {
    private var uiContext: Context = context

    fun updateUiContext(localizedContext: Context) {
        uiContext = localizedContext
        update(state.copy(error = null))
    }

    var state: GalleryUiState = GalleryUiState()
        private set

    private val observers = LinkedHashSet<(GalleryUiState) -> Unit>()
    private var loadJob: Job? = null
    private var persistJob: Job? = null
    private var sessionLoaded = false
    private var resumeMediaId = 0L

    init {
        refreshAccess()
        scope.launch { restoreSessionIfNeeded() }
    }

    private suspend fun restoreSessionIfNeeded() {
        if (sessionLoaded) return
        val session = preferences.sessionFlow.first()
        val restoredFilter = compatibleFilter(session.filter, state.accessHealth)
        val cursorCompatible = restoredFilter == session.filter
        resumeMediaId = if (cursorCompatible) session.lastMediaId else 0L
        update(
            state.copy(
                filter = restoredFilter,
                sortOrder = session.sortOrder,
                totalReviewedCount = session.totalReviewedCount,
                sourceOffset = if (cursorCompatible) session.sourceOffset.coerceAtLeast(0) else 0,
                pendingTrashItems = if (cursorCompatible) {
                    restorePendingTrashItems(session.pendingTrashRefs)
                } else {
                    emptyList()
                },
            ),
        )
        sessionLoaded = true
        if (!cursorCompatible) {
            preferences.saveFilterAndSort(restoredFilter, session.sortOrder)
        }
    }

    fun observe(observer: (GalleryUiState) -> Unit): () -> Unit {
        observers += observer
        observer(state)
        return { observers -= observer }
    }

    fun refreshAccess(clearLoadedMedia: Boolean = false) {
        val health = GalleryAccess.health(context)
        val compatibleFilter = when {
            health.level == GalleryAccessLevel.NONE -> state.filter
            health.canReadImages && !health.canReadVideos -> GalleryMediaFilter.IMAGES
            health.canReadVideos && !health.canReadImages -> GalleryMediaFilter.VIDEOS
            else -> state.filter
        }
        update(
            state.copy(
                accessHealth = health,
                filter = compatibleFilter,
                items = if (clearLoadedMedia) emptyList() else state.items,
                currentIndex = if (clearLoadedMedia) 0 else state.currentIndex,
                loadedSourceCount = if (clearLoadedMedia) 0 else state.loadedSourceCount,
                sourceOffset = if (clearLoadedMedia) 0 else state.sourceOffset,
                history = if (clearLoadedMedia) emptyList() else state.history,
                pendingTrashItems = if (clearLoadedMedia) emptyList() else state.pendingTrashItems,
                hasMore = if (clearLoadedMedia) false else state.hasMore,
                loading = if (clearLoadedMedia) false else state.loading,
                loadingNextPage = if (clearLoadedMedia) false else state.loadingNextPage,
                error = null,
            ),
        )
        if (clearLoadedMedia) {
            resumeMediaId = 0L
            persistProgress()
        }
    }

    fun setFilter(filter: GalleryMediaFilter) {
        val allowed = when (filter) {
            GalleryMediaFilter.ALL ->
                state.accessHealth.canReadImages && state.accessHealth.canReadVideos
            GalleryMediaFilter.IMAGES -> state.accessHealth.canReadImages
            GalleryMediaFilter.VIDEOS -> state.accessHealth.canReadVideos
        }
        if (!allowed) {
            update(state.copy(error = uiContext.getString(R.string.gallery_error_type_unavailable)))
            return
        }
        loadJob?.cancel()
        loadJob = null
        update(
            state.copy(
                filter = filter,
                loading = false,
                loadingNextPage = false,
                hasMore = false,
                items = emptyList(),
                currentIndex = 0,
                loadedSourceCount = 0,
                sourceOffset = 0,
                history = emptyList(),
                pendingTrashItems = emptyList(),
                error = null,
            ),
        )
        resumeMediaId = 0L
        persistJob?.cancel()
        persistJob = scope.launch {
            preferences.saveFilterAndSort(filter, state.sortOrder)
        }
    }

    fun cycleSortOrder() {
        val values = GallerySortOrder.entries
        val next = values[(state.sortOrder.ordinal + 1) % values.size]
        loadJob?.cancel()
        loadJob = null
        update(
            state.copy(
                sortOrder = next,
                loading = false,
                loadingNextPage = false,
                hasMore = false,
                items = emptyList(),
                currentIndex = 0,
                loadedSourceCount = 0,
                sourceOffset = 0,
                history = emptyList(),
                pendingTrashItems = emptyList(),
            ),
        )
        resumeMediaId = 0L
        persistJob?.cancel()
        persistJob = scope.launch {
            preferences.saveFilterAndSort(state.filter, next)
        }
    }

    fun load(pageSize: Int = DEFAULT_PAGE_SIZE) {
        if (!state.hasAccess) {
            update(state.copy(error = uiContext.getString(R.string.gallery_error_permission_required)))
            return
        }
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                restoreSessionIfNeeded()
                update(state.copy(loading = true, loadingNextPage = false, error = null))

                val requestedOffset = state.sourceOffset.coerceAtLeast(0)
                var loaded = MediaStoreHelper.loadGalleryMediaCancellable(
                    context = context,
                    filter = state.filter,
                    sortOrder = state.sortOrder,
                    limit = pageSize + 1,
                    offset = requestedOffset,
                )
                var effectiveOffset = validatedGalleryResumeOffset(
                    requestedOffset = requestedOffset,
                    expectedMediaId = resumeMediaId,
                    actualFirstMediaId = loaded.firstOrNull()?.id,
                )
                var pendingTrash = state.pendingTrashItems

                if (effectiveOffset != requestedOffset) {
                    loaded = MediaStoreHelper.loadGalleryMediaCancellable(
                        context = context,
                        filter = state.filter,
                        sortOrder = state.sortOrder,
                        limit = pageSize + 1,
                        offset = 0,
                    )
                    effectiveOffset = 0
                    pendingTrash = emptyList()
                }

                val hasMore = loaded.size > pageSize
                val page = loaded.take(pageSize)
                resumeMediaId = page.firstOrNull()?.id ?: 0L
                update(
                    state.copy(
                        loading = false,
                        loadingNextPage = false,
                        hasMore = hasMore,
                        items = page,
                        currentIndex = 0,
                        loadedSourceCount = effectiveOffset + page.size,
                        sourceOffset = effectiveOffset,
                        history = emptyList(),
                        pendingTrashItems = pendingTrash,
                    ),
                )
                if (effectiveOffset != requestedOffset) persistProgress()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                update(
                    state.copy(
                        loading = false,
                        loadingNextPage = false,
                        error = uiContext.getString(R.string.gallery_error_load_failed),
                    ),
                )
            }
        }
    }

    fun loadNextPage(pageSize: Int = DEFAULT_PAGE_SIZE) {
        if (!state.hasAccess || !state.hasMore || state.loading || state.loadingNextPage) return

        loadJob = scope.launch {
            val offset = state.loadedSourceCount
            update(state.copy(loadingNextPage = true, error = null))
            try {
                val loaded = MediaStoreHelper.loadGalleryMediaCancellable(
                    context = context,
                    filter = state.filter,
                    sortOrder = state.sortOrder,
                    limit = pageSize + 1,
                    offset = offset,
                )
                val hasMore = loaded.size > pageSize
                val consumed = loaded.take(pageSize)
                val existingIds = buildSet {
                    state.items.mapTo(this) { it.id }
                    state.pendingTrashItems.mapTo(this) { it.id }
                }
                val page = consumed.filterNot { it.id in existingIds }
                update(
                    state.copy(
                        loadingNextPage = false,
                        hasMore = hasMore,
                        loadedSourceCount = offset + consumed.size,
                        items = state.items + page,
                    ),
                )
                // The previous persisted anchor can legitimately be 0 when the
                // reviewer reached the end of the loaded window before prefetch
                // completed. Appending a page exposes a new current item, so
                // persist that anchor immediately for process-death recovery.
                persistProgress()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                update(
                    state.copy(
                        loadingNextPage = false,
                        error = uiContext.getString(R.string.gallery_error_load_failed),
                    ),
                )
            }
        }
    }

    fun keepCurrent() = recordCurrent(SwipeDirection.KEEP)

    fun trashCurrent() = recordCurrent(SwipeDirection.TRASH)

    fun undoLastAction() {
        val last = state.history.lastOrNull() ?: return
        val restoredItems = if (last.direction == SwipeDirection.TRASH) {
            state.items.map { item ->
                if (item.id == last.item.id) item.copy(isSelectedForTrash = false) else item
            }
        } else {
            state.items
        }

        update(
            state.copy(
                items = restoredItems,
                currentIndex = last.previousIndex.coerceAtLeast(0),
                history = state.history.dropLast(1),
                pendingTrashItems = state.pendingTrashItems.filterNot { it.id == last.item.id },
                sourceOffset = (state.sourceOffset - 1).coerceAtLeast(0),
                totalReviewedCount = (state.totalReviewedCount - 1).coerceAtLeast(0),
            ),
        )
        persistProgress()
    }

    fun createTrashRequest(): PendingIntent? {
        val uris = state.selectedForTrash.map { it.uri }
        if (uris.isEmpty()) {
            update(state.copy(error = uiContext.getString(R.string.gallery_error_no_selection)))
            return null
        }
        val pendingIntent = MediaStoreHelper.trashMediaFiles(context.contentResolver, uris)
        if (pendingIntent == null) {
            update(state.copy(error = uiContext.getString(R.string.gallery_error_trash_android11)))
        }
        return pendingIntent
    }

    fun onTrashResult(success: Boolean) {
        if (!success) return
        val removedIds = state.selectedForTrash.mapTo(HashSet()) { it.id }
        val currentId = state.currentItem?.id
        val remainingItems = state.items.filterNot { it.id in removedIds }
        val nextIndex = currentId
            ?.let { id -> remainingItems.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 }
            ?: 0
        val adjustedOffset = (state.sourceOffset - removedIds.size).coerceAtLeast(0)
        val adjustedLoadedCount = (state.loadedSourceCount - removedIds.size).coerceAtLeast(0)

        update(
            state.copy(
                items = remainingItems,
                currentIndex = nextIndex.coerceAtMost(remainingItems.size),
                loadedSourceCount = adjustedLoadedCount,
                sourceOffset = adjustedOffset,
                history = emptyList(),
                pendingTrashItems = emptyList(),
                error = null,
            ),
        )
        persistProgress()
    }

    fun clearError() {
        update(state.copy(error = null))
    }

    private fun recordCurrent(direction: SwipeDirection) {
        val item = state.currentItem ?: return
        val previousIndex = state.currentIndex
        val changedItems = if (direction == SwipeDirection.TRASH) {
            state.items.map { candidate ->
                if (candidate.id == item.id) candidate.copy(isSelectedForTrash = true) else candidate
            }
        } else {
            state.items
        }

        val nextState = state.copy(
            items = changedItems,
            currentIndex = (previousIndex + 1).coerceAtMost(changedItems.size),
            history = (state.history + GallerySwipeHistoryItem(item, direction, previousIndex))
                .takeLast(MAX_UNDO_HISTORY),
            sourceOffset = state.sourceOffset + 1,
            totalReviewedCount = state.totalReviewedCount + 1,
        )
        update(compactReviewWindow(nextState))
        persistProgress()
        if (
            state.hasMore &&
            !state.loadingNextPage &&
            state.currentIndex >= (state.items.size - PREFETCH_DISTANCE).coerceAtLeast(0)
        ) {
            loadNextPage()
        }
    }

    private fun persistProgress() {
        val current = state.currentItem
        resumeMediaId = current?.id ?: 0L
        val pendingRefs = state.selectedForTrash
            .mapTo(linkedSetOf()) { encodePersistedTrashRef(it.id, it.uri.toString()) }
        persistJob?.cancel()
        persistJob = scope.launch {
            preferences.saveProgress(
                lastMediaId = resumeMediaId,
                lastMediaIndex = state.currentIndex,
                sourceOffset = state.sourceOffset,
                pendingTrashRefs = pendingRefs,
                totalReviewedCount = state.totalReviewedCount,
            )
        }
    }

    private fun restorePendingTrashItems(rawRefs: Set<String>): List<GalleryMediaItem> =
        rawRefs.mapNotNull(::decodePersistedTrashRef).map { ref ->
            val isVideo = ref.uri.contains("/video/", ignoreCase = true)
            GalleryMediaItem(
                id = ref.id,
                uri = android.net.Uri.parse(ref.uri),
                name = "Media_${ref.id}",
                sizeBytes = 0L,
                dateTaken = 0L,
                mimeType = if (isVideo) "video/*" else "image/*",
                album = null,
                isSelectedForTrash = true,
            )
        }

    private fun compatibleFilter(
        requested: GalleryMediaFilter,
        health: GalleryAccessHealth,
    ): GalleryMediaFilter = when {
        health.level == GalleryAccessLevel.NONE -> requested
        health.canReadImages && !health.canReadVideos -> GalleryMediaFilter.IMAGES
        health.canReadVideos && !health.canReadImages -> GalleryMediaFilter.VIDEOS
        else -> requested
    }

    private fun compactReviewWindow(candidate: GalleryUiState): GalleryUiState {
        val compaction = compactGalleryReviewWindow(
            items = candidate.items,
            currentIndex = candidate.currentIndex,
            maxReviewedItemsInMemory = MAX_REVIEWED_ITEMS_IN_MEMORY,
            retainReviewedItems = RETAIN_REVIEWED_ITEMS,
        )
        if (compaction.removedPrefix.isEmpty()) return candidate

        val removedCount = compaction.removedPrefix.size
        val retainedTrash = (candidate.pendingTrashItems.asSequence() +
            compaction.removedPrefix.asSequence().filter { it.isSelectedForTrash })
            .distinctBy { it.id }
            .toList()

        val adjustedHistory = candidate.history.mapNotNull { entry ->
            val adjustedIndex = entry.previousIndex - removedCount
            if (adjustedIndex < 0) null else entry.copy(previousIndex = adjustedIndex)
        }

        return candidate.copy(
            items = compaction.items,
            currentIndex = compaction.currentIndex,
            history = adjustedHistory,
            pendingTrashItems = retainedTrash,
        )
    }

    private fun update(newState: GalleryUiState) {
        state = newState
        observers.toList().forEach { it(newState) }
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 120
        const val PREFETCH_DISTANCE = 12
        const val MAX_UNDO_HISTORY = 50
        const val MAX_REVIEWED_ITEMS_IN_MEMORY = 240
        const val RETAIN_REVIEWED_ITEMS = 60
    }
}


internal data class PersistedTrashRef(
    val id: Long,
    val uri: String,
)

internal fun encodePersistedTrashRef(id: Long, uri: String): String =
    "$id\t$uri"

internal fun decodePersistedTrashRef(raw: String): PersistedTrashRef? {
    val separator = raw.indexOf('\t')
    if (separator <= 0 || separator >= raw.lastIndex) return null
    val id = raw.substring(0, separator).toLongOrNull()?.takeIf { it > 0L } ?: return null
    val uri = raw.substring(separator + 1).takeIf { it.startsWith("content://") } ?: return null
    return PersistedTrashRef(id = id, uri = uri)
}

internal fun validatedGalleryResumeOffset(
    requestedOffset: Int,
    expectedMediaId: Long,
    actualFirstMediaId: Long?,
): Int {
    val safeOffset = requestedOffset.coerceAtLeast(0)
    if (safeOffset == 0) return 0
    if (expectedMediaId <= 0L) {
        return if (actualFirstMediaId == null) safeOffset else 0
    }
    return if (actualFirstMediaId == expectedMediaId) safeOffset else 0
}
