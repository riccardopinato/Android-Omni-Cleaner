package com.riccardopinato.omnicleaner.gallery

import android.app.PendingIntent
import android.content.Context
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.MediaStoreHelper
import com.riccardopinato.omnicleaner.cleaner.SafeOperationsJournal
import com.riccardopinato.omnicleaner.cleaner.SafeOperationPhase
import java.io.File
import java.security.MessageDigest
import com.riccardopinato.omnicleaner.cleaner.MediaTrashBatchPolicy
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val hasPersistedUndo: Boolean = false,
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
    // Exact selection for one in-flight Android system confirmation.
    private var inFlightTrashIds: Set<Long> = emptySet()
    private var inFlightOperationId: String? = null
    private val operationJournal by lazy {
        SafeOperationsJournal(File(context.filesDir, "omni_safe_operations"))
    }
    private var verifyingTrash = false
    private var loadJob: Job? = null
    private var persistJob: Job? = null
    private var sessionLoaded = false
    private var resumeMediaId = 0L
    private var lastUndo: PersistedGalleryUndo? = null

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
        lastUndo = if (cursorCompatible) decodePersistedGalleryUndo(session.lastUndoRef) else null
        update(
            state.copy(
                filter = restoredFilter,
                sortOrder = session.sortOrder,
                totalReviewedCount = session.totalReviewedCount,
                hasPersistedUndo = lastUndo != null,
                sourceOffset = if (cursorCompatible) session.sourceOffset.coerceAtLeast(0) else 0,
                // The Android Trash queue belongs to the user, not to the
                // currently active media filter or sort cursor.
                pendingTrashItems = restorePendingTrashItems(session.pendingTrashRefs),
            ),
        )
        sessionLoaded = true
        if (!cursorCompatible) {
            preferences.saveFilterAndSort(
                restoredFilter,
                session.sortOrder,
                session.pendingTrashRefs,
            )
        }
    }

    fun observe(observer: (GalleryUiState) -> Unit): () -> Unit {
        observers += observer
        observer(state)
        return { observers -= observer }
    }

    fun refreshAccess(clearLoadedMedia: Boolean = false) {
        // Media reload/revoked access must not silently discard reviewed Trash picks.
        val preservedTrash = if (clearLoadedMedia) state.selectedForTrash else state.pendingTrashItems
        if (clearLoadedMedia) lastUndo = null
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
                hasPersistedUndo = if (clearLoadedMedia) false else state.hasPersistedUndo,
                pendingTrashItems = preservedTrash,
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
        if (filter == state.filter) return
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
        val retainedTrash = state.selectedForTrash
        lastUndo = null
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
                hasPersistedUndo = false,
                pendingTrashItems = retainedTrash,
                error = null,
            ),
        )
        resumeMediaId = 0L
        persistJob?.cancel()
        persistJob = scope.launch {
            preferences.saveFilterAndSort(
                filter,
                state.sortOrder,
                retainedTrash.mapTo(linkedSetOf()) { encodePersistedTrashRef(it.id, it.uri.toString()) },
            )
        }
    }

    fun cycleSortOrder() {
        val retainedTrash = state.selectedForTrash
        lastUndo = null
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
                hasPersistedUndo = false,
                pendingTrashItems = retainedTrash,
            ),
        )
        resumeMediaId = 0L
        persistJob?.cancel()
        persistJob = scope.launch {
            preferences.saveFilterAndSort(
                state.filter,
                next,
                retainedTrash.mapTo(linkedSetOf()) { encodePersistedTrashRef(it.id, it.uri.toString()) },
            )
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
                    // A stale pagination cursor must never silently discard
                    // user-reviewed Trash selections. The system permission
                    // confirmation still validates each media URI.
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
        val last = state.history.lastOrNull()
        if (last == null) {
            // Process restart / reload: the previous screen's in-memory photo
            // is gone, but a single bounded action journal can recover it.
            val persisted = lastUndo ?: return
            lastUndo = null
            update(
                state.copy(
                    items = emptyList(),
                    currentIndex = 0,
                    history = emptyList(),
                    hasPersistedUndo = false,
                    pendingTrashItems = state.selectedForTrash
                        .filterNot { it.id == persisted.mediaId },
                    sourceOffset = persisted.sourceOffsetBefore,
                    loadedSourceCount = persisted.sourceOffsetBefore,
                    totalReviewedCount = (state.totalReviewedCount - 1).coerceAtLeast(0),
                    hasMore = false,
                    error = null,
                ),
            )
            persistProgress(anchorOverride = persisted.mediaId)
            load()
            return
        }
        val remainingHistory = state.history.dropLast(1)
        lastUndo = remainingHistory.lastOrNull()?.let { prior ->
            PersistedGalleryUndo(
                mediaId = prior.item.id,
                direction = prior.direction,
                sourceOffsetBefore = (state.sourceOffset - 2).coerceAtLeast(0),
            )
        }
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
                history = remainingHistory,
                hasPersistedUndo = lastUndo != null,
                pendingTrashItems = state.pendingTrashItems.filterNot { it.id == last.item.id },
                sourceOffset = (state.sourceOffset - 1).coerceAtLeast(0),
                totalReviewedCount = (state.totalReviewedCount - 1).coerceAtLeast(0),
            ),
        )
        persistProgress()
    }

    /**
     * Direct, non-destructive checkbox for any loaded Gallery Pro item.
     * Does not advance the swipe cursor or trigger an Android Trash request.
     * The user's exact selection is persisted and reviewed before deletion.
     */
    fun setTrashSelection(mediaId: Long, selected: Boolean) {
        val item = state.items.firstOrNull { it.id == mediaId } ?: return
        if (item.isSelectedForTrash == selected) return
        update(
            state.copy(
                items = state.items.map { media ->
                    if (media.id == mediaId) media.copy(isSelectedForTrash = selected) else media
                },
                pendingTrashItems = if (selected) state.pendingTrashItems
                    else state.pendingTrashItems.filterNot { it.id == mediaId },
            ),
        )
        persistProgress()
    }

    /** Unmark a reviewed photo without deleting or changing the media file. */
    fun removeFromTrashSelection(mediaId: Long) {
        if (state.selectedForTrash.none { it.id == mediaId }) return
        update(
            state.copy(
                items = state.items.map { media ->
                    if (media.id == mediaId) media.copy(isSelectedForTrash = false) else media
                },
                pendingTrashItems = state.pendingTrashItems.filterNot { it.id == mediaId },
            ),
        )
        persistProgress()
    }

    fun createTrashRequest(): PendingIntent? {
        if (inFlightTrashIds.isNotEmpty() || verifyingTrash) return null
        val selectedBatch = MediaTrashBatchPolicy.firstBatch(state.selectedForTrash)
        if (selectedBatch.isEmpty()) {
            update(state.copy(error = uiContext.getString(R.string.gallery_error_no_selection)))
            return null
        }
        // A persisted Gallery queue can outlive a file replacement or a
        // revoked MediaStore permission: verify its identity before prompting.
        val changed = selectedBatch.filterNot { media ->
            val sameProviderEpoch = media.mediaStoreVersion != null &&
                media.mediaStoreVersion == MediaStoreHelper.mountedVolumeVersion(
                    context, media.volumeName,
                )
            sameProviderEpoch && MediaStoreHelper.isCurrentMediaMatch(
                context.contentResolver, media.uri, media.sizeBytes, media.name,
                media.dateModifiedSeconds, media.generationModified,
            )
        }
        if (changed.isNotEmpty()) {
            update(state.copy(error = uiContext.getString(
                R.string.gallery_error_trash_unverified, changed.size,
            )))
            return null
        }
        val details = selectedBatch.joinToString("\n") { media ->
            listOf(media.uri.toString(), media.sizeBytes.toString(), media.name,
                media.generationModified.toString(),
            ).joinToString("|")
        }
        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(details.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val prepared = operationJournal.begin("MEDIA_TRASH", fingerprint, details)
        val approved = prepared?.let {
            operationJournal.advance(it, SafeOperationPhase.USER_APPROVED)
        }
        if (approved == null) {
            update(state.copy(error = uiContext.getString(R.string.gallery_error_trash_android11)))
            return null
        }
        val pendingIntent = MediaStoreHelper.trashMediaFiles(
            context.contentResolver, selectedBatch.map { it.uri },
        )
        if (pendingIntent == null) {
            operationJournal.advance(approved, SafeOperationPhase.REJECTED)
            update(state.copy(error = uiContext.getString(R.string.gallery_error_trash_android11)))
        } else {
            inFlightTrashIds = selectedBatch.mapTo(HashSet()) { it.id }
            inFlightOperationId = approved.id
        }
        return pendingIntent
    }

    /**
     * RESULT_OK is user consent; only MediaStore IS_TRASHED is verified state.
     * Failed/unknown rows remain in the explicit review queue.
     */
    fun onTrashResult(success: Boolean) {
        val requested = inFlightTrashIds
        inFlightTrashIds = emptySet()
        val operationId = inFlightOperationId
        inFlightOperationId = null
        if (!success || requested.isEmpty()) {
            operationId?.let { id ->
                operationJournal.latest(id)?.let {
                    operationJournal.advance(it, SafeOperationPhase.REJECTED)
                }
            }
            return
        }
        val selected = state.selectedForTrash.filter { it.id in requested }
        if (selected.isEmpty()) return
        verifyingTrash = true
        scope.launch {
            try {
                val verified = MediaStoreHelper.verifyTrashedUris(
                    context.contentResolver, selected.map { it.uri },
                )
                val flags = selected.associate { media ->
                    media.id to if (media.uri in verified.confirmedUris) true else null
                }
                val evidence = evaluateGalleryTrashEvidence(requested, flags)
                operationId?.let { id ->
                    operationJournal.latest(id)?.let {
                        operationJournal.advance(it,
                            if (evidence.unverifiedIds.isEmpty()) SafeOperationPhase.COMMITTED
                            else SafeOperationPhase.UNKNOWN_NEEDS_REVIEW)
                    }
                }
                if (evidence.confirmedIds.isNotEmpty()) {
                    lastUndo = null
                    val confirmed = evidence.confirmedIds
                    val removedFromLoaded = state.items.count { it.id in confirmed }
                    val removedBeforeCurrent = state.items.take(state.currentIndex)
                        .count { it.id in confirmed }
                    val remainingItems = state.items.filterNot { it.id in confirmed }
                    update(
                        state.copy(
                            items = remainingItems,
                            currentIndex = (state.currentIndex - removedBeforeCurrent)
                                .coerceIn(0, remainingItems.size),
                            loadedSourceCount = (state.loadedSourceCount - removedFromLoaded).coerceAtLeast(0),
                            sourceOffset = (state.sourceOffset - confirmed.size).coerceAtLeast(0),
                            history = emptyList(),
                            hasPersistedUndo = false,
                            pendingTrashItems = state.pendingTrashItems.filterNot { it.id in confirmed },
                            error = null,
                        ),
                    )
                    persistProgress()
                    // Only an independently confirmed move is an ad-eligible cleanup.
                    CleanupEventBus.publishCompleted()
                }
                if (evidence.unverifiedIds.isNotEmpty()) {
                    update(state.copy(
                        error = uiContext.getString(
                            R.string.gallery_error_trash_unverified,
                            evidence.unverifiedIds.size,
                        ),
                    ))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                update(state.copy(
                    error = uiContext.getString(R.string.gallery_error_trash_unverified, selected.size),
                ))
            } finally {
                verifyingTrash = false
            }
        }
    }

    fun clearError() {
        update(state.copy(error = null))
    }

    private fun recordCurrent(direction: SwipeDirection) {
        val item = state.currentItem ?: return
        val previousIndex = state.currentIndex
        lastUndo = PersistedGalleryUndo(
            mediaId = item.id,
            direction = direction,
            sourceOffsetBefore = state.sourceOffset,
        )
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
            hasPersistedUndo = true,
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

    private fun persistProgress(anchorOverride: Long? = null) {
        val current = state.currentItem
        resumeMediaId = anchorOverride ?: current?.id ?: 0L
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
                lastUndoRef = lastUndo?.let(::encodePersistedGalleryUndo),
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


/** One durable action, never an authorization to delete media. */
internal data class PersistedGalleryUndo(
    val mediaId: Long,
    val direction: SwipeDirection,
    val sourceOffsetBefore: Int,
)

internal fun encodePersistedGalleryUndo(value: PersistedGalleryUndo): String =
    "${value.mediaId}\t${value.direction.name}\t${value.sourceOffsetBefore}"

internal fun decodePersistedGalleryUndo(raw: String?): PersistedGalleryUndo? {
    val parts = raw?.split('\t') ?: return null
    if (parts.size != 3) return null
    val id = parts[0].toLongOrNull()?.takeIf { it > 0 } ?: return null
    val direction = SwipeDirection.entries.firstOrNull { it.name == parts[1] } ?: return null
    val before = parts[2].toIntOrNull()?.takeIf { it >= 0 } ?: return null
    return PersistedGalleryUndo(id, direction, before)
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
