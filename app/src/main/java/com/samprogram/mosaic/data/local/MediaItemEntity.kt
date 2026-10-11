package com.samprogram.mosaic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Indexed metadata for one media file inside a selected folder.
 *
 * Only metadata and the provider identity are stored; the media itself stays in
 * the user's folder and is read through its content URI. [id] is the document
 * URI, which keeps files with identical display names distinct.
 */
@Entity(
    tableName = "media_items",
    foreignKeys = [
        ForeignKey(
            entity = SourceFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["folderId"])],
)
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val folderId: String,
    val documentId: String,
    val contentUri: String,
    val displayName: String,
    val mediaKind: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val lastModified: Long? = null,
)
