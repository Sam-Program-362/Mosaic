package com.samprogram.mosaic.data.repository

import com.samprogram.mosaic.core.model.FolderAccessStatus
import com.samprogram.mosaic.core.model.MediaItem
import com.samprogram.mosaic.core.model.ScanFailure
import com.samprogram.mosaic.core.model.SourceFolder
import com.samprogram.mosaic.data.local.CommunityDao
import com.samprogram.mosaic.data.local.CommunityEntity
import com.samprogram.mosaic.data.local.FolderDao
import com.samprogram.mosaic.data.local.MediaDao
import com.samprogram.mosaic.data.local.SourceFolderEntity
import com.samprogram.mosaic.data.local.toDomain
import com.samprogram.mosaic.data.local.toEntity
import com.samprogram.mosaic.data.source.MediaIndex
import com.samprogram.mosaic.data.source.MediaTreeSource
import com.samprogram.mosaic.data.source.TreeScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URLDecoder
import java.util.UUID

/**
 * Owns the folder library: registering selected folders, indexing the media
 * directly inside them, and reconciling the local index with what the provider
 * currently reports.
 *
 * Document-provider access lives behind [MediaTreeSource] and persistence
 * behind the DAOs, so neither leaks into the UI and both can be replaced with
 * fakes in tests.
 */
class FolderLibraryRepository(
    private val folderDao: FolderDao,
    private val mediaDao: MediaDao,
    private val communityDao: CommunityDao,
    private val treeSource: MediaTreeSource,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {

    fun observeFolders(): Flow<List<SourceFolder>> =
        folderDao.observeAll().map { folders -> folders.map { it.toDomain() } }

    fun observeMediaCounts(): Flow<Map<String, Int>> =
        mediaDao.observeCounts().map { rows -> rows.associate { it.folderId to it.itemCount } }

    fun observeMediaInFolder(folderId: String): Flow<List<MediaItem>> =
        mediaDao.observeByFolder(folderId).map { items -> items.map { it.toDomain() } }

    /**
     * Registers the folder the user picked, or rescans it when it is already
     * registered. Re-selecting a known folder never creates a duplicate and is
     * also the recovery path after access has been revoked.
     */
    suspend fun addFolder(treeUri: String, grantedRead: Boolean): AddFolderResult {
        val normalizedUri = treeUri.trim()
        if (normalizedUri.isEmpty()) return AddFolderResult.InvalidSelection
        if (!grantedRead) return AddFolderResult.AccessDenied

        // Persist the grant using only what the picker offered. Providers that
        // do not support persisted grants can still be scanned this session, so
        // a false result is not treated as a failure here.
        treeSource.takePersistableAccess(normalizedUri, readGranted = true)

        folderDao.findByTreeUri(normalizedUri)?.let { existing ->
            val indexed = (scan(existing) as? FolderScanOutcome.Indexed)?.indexedCount ?: 0
            return AddFolderResult.AlreadyRegistered(existing.id, indexed)
        }

        val displayName = treeSource.queryTreeDisplayName(normalizedUri)
            ?.takeIf { it.isNotBlank() }
            ?: fallbackNameFor(normalizedUri)

        val folder = SourceFolder(
            id = newId(),
            treeUri = normalizedUri,
            displayName = displayName,
            accessStatus = FolderAccessStatus.AVAILABLE,
            addedAt = clock(),
        )
        folderDao.upsert(folder.toEntity())

        // One community per selected folder, named after the folder.
        communityDao.insertIfAbsent(
            CommunityEntity(
                id = newId(),
                folderId = folder.id,
                name = displayName,
                createdAt = clock(),
            ),
        )

        val indexed = (scan(folder.toEntity()) as? FolderScanOutcome.Indexed)?.indexedCount ?: 0
        return AddFolderResult.Added(folder.id, indexed)
    }

    /** Rescans every registered folder, continuing past individual failures. */
    suspend fun refreshAll(): RefreshReport {
        var refreshed = 0
        var unavailable = 0
        for (folder in folderDao.getAll()) {
            when (scan(folder)) {
                is FolderScanOutcome.Indexed -> refreshed++
                is FolderScanOutcome.Unavailable -> unavailable++
                FolderScanOutcome.Missing -> Unit
            }
        }
        return RefreshReport(refreshed, unavailable)
    }

    suspend fun refreshFolder(folderId: String): FolderScanOutcome {
        val folder = folderDao.findById(folderId) ?: return FolderScanOutcome.Missing
        return scan(folder)
    }

    /**
     * Removes Mosaic's metadata for a folder and releases its persisted grant.
     *
     * Only Mosaic's own records are deleted. Source files are never touched, and
     * the foreign-key cascade in the database repeats this cleanup as a safety
     * net if a removal is interrupted.
     */
    suspend fun removeFolder(folderId: String) {
        val folder = folderDao.findById(folderId) ?: return
        mediaDao.deleteAllInFolder(folderId)
        communityDao.deleteByFolderId(folderId)
        folderDao.deleteById(folderId)
        treeSource.releasePersistedAccess(folder.treeUri)
    }

    /**
     * Scans one folder and reconciles the index with the result.
     *
     * Records are only ever removed after a successful scan. A failure marks the
     * folder unavailable and leaves its last-known media and community intact so
     * a later refresh can recover it.
     */
    private suspend fun scan(folder: SourceFolderEntity): FolderScanOutcome =
        when (val result = treeSource.listDirectChildren(folder.treeUri)) {
            is TreeScanResult.Failure -> {
                // Distinguish "the grant is gone and the user must re-select the
                // folder" from a genuine provider-side denial.
                val reason = if (
                    result.reason == ScanFailure.ACCESS_DENIED &&
                    !treeSource.hasPersistedAccess(folder.treeUri)
                ) {
                    ScanFailure.ACCESS_NOT_GRANTED
                } else {
                    result.reason
                }
                folderDao.upsert(
                    folder.copy(
                        accessStatus = FolderAccessStatus.UNAVAILABLE.name,
                        lastScanFailure = reason.name,
                    ),
                )
                FolderScanOutcome.Unavailable(reason)
            }

            is TreeScanResult.Success -> {
                val items = MediaIndex.index(folder.id, result.children).map { it.toEntity() }
                mediaDao.replaceFolderMedia(folder.id, items)
                folderDao.upsert(
                    folder.copy(
                        accessStatus = FolderAccessStatus.AVAILABLE.name,
                        lastScanSucceededAt = clock(),
                        lastScanFailure = null,
                    ),
                )
                // Self-heals a missing community without disturbing an existing one.
                communityDao.insertIfAbsent(
                    CommunityEntity(
                        id = newId(),
                        folderId = folder.id,
                        name = folder.displayName,
                        createdAt = clock(),
                    ),
                )
                FolderScanOutcome.Indexed(items.size)
            }
        }

    /** Best-effort folder name from the tree URI when the provider reports none. */
    private fun fallbackNameFor(treeUri: String): String {
        val encoded = treeUri.substringAfterLast('/')
        val decoded = runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(encoded)
        val name = decoded.substringAfterLast(':').trim()
        return name.ifBlank { "Selected folder" }
    }
}
