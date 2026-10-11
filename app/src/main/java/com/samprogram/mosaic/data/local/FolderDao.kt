package com.samprogram.mosaic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM source_folders ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<SourceFolderEntity>>

    @Query("SELECT * FROM source_folders ORDER BY addedAt ASC")
    suspend fun getAll(): List<SourceFolderEntity>

    @Query("SELECT * FROM source_folders WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SourceFolderEntity?

    @Query("SELECT * FROM source_folders WHERE treeUri = :treeUri LIMIT 1")
    suspend fun findByTreeUri(treeUri: String): SourceFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: SourceFolderEntity)

    @Query("DELETE FROM source_folders WHERE id = :id")
    suspend fun deleteById(id: String)
}
