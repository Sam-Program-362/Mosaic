package com.samprogram.mosaic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityDao {

    @Query("SELECT * FROM communities ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<CommunityEntity>>

    @Query("SELECT * FROM communities WHERE folderId = :folderId LIMIT 1")
    suspend fun findByFolderId(folderId: String): CommunityEntity?

    /**
     * Creates the community if it does not exist yet. The unique index on
     * `folderId` plus [OnConflictStrategy.IGNORE] makes this idempotent, so a
     * folder keeps the same community across scans.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(community: CommunityEntity)

    @Query("DELETE FROM communities WHERE folderId = :folderId")
    suspend fun deleteByFolderId(folderId: String)
}
