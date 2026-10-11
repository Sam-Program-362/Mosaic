package com.samprogram.mosaic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A community record. One is created automatically for each selected folder and
 * is kept while its folder is unavailable so its identity stays stable.
 *
 * [folderId] is nullable for the custom communities planned for a later task;
 * the unique index allows many nulls, so those can coexist with the folder
 * communities without conflicting.
 */
@Entity(
    tableName = "communities",
    foreignKeys = [
        ForeignKey(
            entity = SourceFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["folderId"], unique = true)],
)
data class CommunityEntity(
    @PrimaryKey val id: String,
    val folderId: String?,
    val name: String,
    val createdAt: Long,
)
