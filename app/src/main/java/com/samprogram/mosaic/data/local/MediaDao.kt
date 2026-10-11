package com.samprogram.mosaic.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Per-folder media counts for the folder list. */
data class FolderMediaCount(
    val folderId: String,
    val itemCount: Int,
)

@Dao
interface MediaDao {

    @Query("SELECT * FROM media_items WHERE folderId = :folderId ORDER BY displayName COLLATE NOCASE ASC")
    fun observeByFolder(folderId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE folderId = :folderId")
    suspend fun getByFolder(folderId: String): List<MediaItemEntity>

    @Query("SELECT folderId, COUNT(*) AS itemCount FROM media_items GROUP BY folderId")
    fun observeCounts(): Flow<List<FolderMediaCount>>

    @Upsert
    suspend fun upsertAll(items: List<MediaItemEntity>)

    @Query("DELETE FROM media_items WHERE folderId = :folderId AND id NOT IN (:keepIds)")
    suspend fun deleteMissing(folderId: String, keepIds: List<String>)

    @Query("DELETE FROM media_items WHERE folderId = :folderId")
    suspend fun deleteAllInFolder(folderId: String)

    /**
     * Applies the result of one successful scan atomically: inserts or updates
     * every scanned item, then drops the records that disappeared.
     *
     * This is only ever called after a scan succeeds, so a provider failure can
     * never be mistaken for an empty folder.
     */
    @Transaction
    suspend fun replaceFolderMedia(folderId: String, items: List<MediaItemEntity>) {
        upsertAll(items)
        if (items.isEmpty()) {
            deleteAllInFolder(folderId)
        } else {
            deleteMissing(folderId, items.map { it.id })
        }
    }
}
