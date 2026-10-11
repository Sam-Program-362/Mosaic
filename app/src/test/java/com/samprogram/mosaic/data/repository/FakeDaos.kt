package com.samprogram.mosaic.data.repository

import com.samprogram.mosaic.data.local.CommunityDao
import com.samprogram.mosaic.data.local.CommunityEntity
import com.samprogram.mosaic.data.local.FolderDao
import com.samprogram.mosaic.data.local.FolderMediaCount
import com.samprogram.mosaic.data.local.MediaDao
import com.samprogram.mosaic.data.local.MediaItemEntity
import com.samprogram.mosaic.data.local.SourceFolderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory stand-ins for the Room DAOs.
 *
 * They mirror the constraints the repository depends on: the unique index on
 * `treeUri`, case-insensitive ordering, per-folder counts and the
 * `insertIfAbsent` behaviour of the community unique index.
 */

class FakeFolderDao : FolderDao {

    private val rows = linkedMapOf<String, SourceFolderEntity>()
    private val flow = MutableStateFlow<List<SourceFolderEntity>>(emptyList())

    private fun publish() {
        flow.value = rows.values.sortedBy { it.addedAt }
    }

    override fun observeAll(): Flow<List<SourceFolderEntity>> = flow

    override suspend fun getAll(): List<SourceFolderEntity> = rows.values.sortedBy { it.addedAt }

    override suspend fun findById(id: String): SourceFolderEntity? = rows[id]

    override suspend fun findByTreeUri(treeUri: String): SourceFolderEntity? =
        rows.values.firstOrNull { it.treeUri == treeUri }

    override suspend fun upsert(folder: SourceFolderEntity) {
        // Emulate the unique index on treeUri.
        rows.entries.removeAll { (_, row) -> row.treeUri == folder.treeUri && row.id != folder.id }
        rows[folder.id] = folder
        publish()
    }

    override suspend fun deleteById(id: String) {
        rows.remove(id)
        publish()
    }
}

class FakeMediaDao : MediaDao {

    private val rows = linkedMapOf<String, MediaItemEntity>()
    private val byFolder = MutableStateFlow<List<MediaItemEntity>>(emptyList())
    private val counts = MutableStateFlow<List<FolderMediaCount>>(emptyList())

    private fun publish() {
        val all = rows.values.toList()
        byFolder.value = all
        counts.value = all.groupBy { it.folderId }
            .map { (folderId, items) -> FolderMediaCount(folderId, items.size) }
    }

    /** Every stored row, for direct assertions. */
    fun all(): List<MediaItemEntity> = rows.values.toList()

    override fun observeByFolder(folderId: String): Flow<List<MediaItemEntity>> =
        byFolder.map { items ->
            items.filter { it.folderId == folderId }
                .sortedBy { it.displayName.lowercase() }
        }

    override suspend fun getByFolder(folderId: String): List<MediaItemEntity> =
        rows.values.filter { it.folderId == folderId }

    override fun observeCounts(): Flow<List<FolderMediaCount>> = counts

    override suspend fun upsertAll(items: List<MediaItemEntity>) {
        items.forEach { rows[it.id] = it }
        publish()
    }

    override suspend fun deleteMissing(folderId: String, keepIds: List<String>) {
        rows.entries.removeAll { (_, row) -> row.folderId == folderId && row.id !in keepIds }
        publish()
    }

    override suspend fun deleteAllInFolder(folderId: String) {
        rows.entries.removeAll { (_, row) -> row.folderId == folderId }
        publish()
    }
}

class FakeCommunityDao : CommunityDao {

    private val rows = linkedMapOf<String, CommunityEntity>()
    private val flow = MutableStateFlow<List<CommunityEntity>>(emptyList())

    private fun publish() {
        flow.value = rows.values.toList()
    }

    fun all(): List<CommunityEntity> = rows.values.toList()

    override fun observeAll(): Flow<List<CommunityEntity>> = flow

    override suspend fun findByFolderId(folderId: String): CommunityEntity? =
        rows.values.firstOrNull { it.folderId == folderId }

    override suspend fun insertIfAbsent(community: CommunityEntity) {
        // Emulate the unique index on folderId; multiple nulls are allowed.
        val conflicts = community.folderId != null &&
            rows.values.any { it.folderId == community.folderId }
        if (!conflicts) {
            rows[community.id] = community
            publish()
        }
    }

    override suspend fun deleteByFolderId(folderId: String) {
        rows.entries.removeAll { (_, community) -> community.folderId == folderId }
        publish()
    }
}
