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
    val history: List<GallerySwipeHistoryItem> = emptyList(),
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
        get() = items.filter { it.isSelectedForTrash }
}

class GalleryController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: GalleryPreferencesManager = GalleryPreferencesManager(context),
) {
    var state: GalleryUiState = GalleryUiState()
        private set

    private val observers = LinkedHashSet<(GalleryUiState) -> Unit>()
    private var loadJob: Job? = null

    init {
        refreshAccess()
        scope.launch {
            val session = preferences.sessionFlow.first()
            update(
                state.copy(
                    filter = session.filter,
                    sortOrder = session.sortOrder,
                    totalReviewedCount = session.totalReviewedCount,
                ),
            )
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
                history = if (clearLoadedMedia) emptyList() else state.history,
                hasMore = if (clearLoadedMedia) false else state.hasMore,
                loading = if (clearLoadedMedia) false else state.loading,
                loadingNextPage = if (clearLoadedMedia) false else state.loadingNextPage,
                error = null,
            ),
        )
    }

    fun setFilter(filter: GalleryMediaFilter) {
        val allowed = when (filter) {
            GalleryMediaFilter.ALL ->
                state.accessHealth.canReadImages && state.accessHealth.canReadVideos
            GalleryMediaFilter.IMAGES -> state.accessHealth.canReadImages
            GalleryMediaFilter.VIDEOS -> state.accessHealth.canReadVideos
        }
        if (!allowed) {
            update(state.copy(error = context.getString(R.string.gallery_error_type_unavailable)))
            return
        }
        update(state.copy(filter = filter, currentIndex = 0, history = emptyList(), error = null))
        scope.launch { preferences.saveFilterAndSort(filter, state.sortOrder) }
    }

    fun cycleSortOrder() {
        val values = GallerySortOrder.entries
        val next = values[(state.sortOrder.ordinal + 1) % values.size]
        update(state.copy(sortOrder = next, currentIndex = 0, history = emptyList()))
        scope.launch { preferences.saveFilterAndSort(state.filter, next) }
    }

    fun load(pageSize: Int = DEFAULT_PAGE_SIZE) {
        if (!state.hasAccess) {
            update(state.copy(error = context.getString(R.string.gallery_error_permission_required)))
            return
        }
        loadJob?.cancel()
        loadJob = scope.launch {
            update(state.copy(loading = true, loadingNextPage = false, error = null))
            runCatching {
                MediaStoreHelper.loadGalleryMediaCancellable(
                    context = context,
                    filter = state.filter,
                    sortOrder = state.sortOrder,
                    limit = pageSize + 1,
                    offset = 0,
                )
            }.onSuccess { loaded ->
                val hasMore = loaded.size > pageSize
                val page = loaded.take(pageSize)
                update(
                    state.copy(
                        loading = false,
                        loadingNextPage = false,
                        hasMore = hasMore,
                        items = page,
                        currentIndex = 0,
                        history = emptyList(),
                    ),
                )
            }.onFailure { error ->
                if (error !is CancellationException) {
                    update(state.copy(loading = false, loadingNextPage = false, error = error.message ?: error.javaClass.simpleName))
                }
            }
        }
    }

    fun loadNextPage(pageSize: Int = DEFAULT_PAGE_SIZE) {
        if (!state.hasAccess || !state.hasMore || state.loading || state.loadingNextPage) return

        loadJob = scope.launch {
            val offset = state.items.size
            update(state.copy(loadingNextPage = true, error = null))
            runCatching {
                MediaStoreHelper.loadGalleryMediaCancellable(
                    context = context,
                    filter = state.filter,
                    sortOrder = state.sortOrder,
                    limit = pageSize + 1,
                    offset = offset,
                )
            }.onSuccess { loaded ->
                val hasMore = loaded.size > pageSize
                val existingIds = state.items.mapTo(HashSet()) { it.id }
                val page = loaded.take(pageSize).filterNot { it.id in existingIds }
                update(
                    state.copy(
                        loadingNextPage = false,
                        hasMore = hasMore,
                        items = state.items + page,
                    ),
                )
            }.onFailure { error ->
                if (error !is CancellationException) {
                    update(state.copy(loadingNextPage = false, error = error.message ?: error.javaClass.simpleName))
                }
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
                totalReviewedCount = (state.totalReviewedCount - 1).coerceAtLeast(0),
            ),
        )
        persistProgress()
    }

    fun createTrashRequest(): PendingIntent? {
        val uris = state.selectedForTrash.map { it.uri }
        if (uris.isEmpty()) {
            update(state.copy(error = context.getString(R.string.gallery_error_no_selection)))
            return null
        }
        val pendingIntent = MediaStoreHelper.trashMediaFiles(context.contentResolver, uris)
        if (pendingIntent == null) {
            update(state.copy(error = context.getString(R.string.gallery_error_trash_android11)))
        }
        return pendingIntent
    }

    fun onTrashResult(success: Boolean) {
        if (!success) return
        val removedIds = state.selectedForTrash.mapTo(HashSet()) { it.id }
        update(
            state.copy(
                items = state.items.filterNot { it.id in removedIds },
                currentIndex = 0,
                history = emptyList(),
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

        update(
            state.copy(
                items = changedItems,
                currentIndex = (previousIndex + 1).coerceAtMost(changedItems.size),
                history = state.history + GallerySwipeHistoryItem(item, direction, previousIndex),
                totalReviewedCount = state.totalReviewedCount + 1,
            ),
        )
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
        scope.launch {
            preferences.saveProgress(
                lastMediaId = current?.id ?: 0L,
                lastMediaIndex = state.currentIndex,
                totalReviewedCount = state.totalReviewedCount,
            )
        }
    }

    private fun update(newState: GalleryUiState) {
        state = newState
        observers.toList().forEach { it(newState) }
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 120
        const val PREFETCH_DISTANCE = 12
    }
}
