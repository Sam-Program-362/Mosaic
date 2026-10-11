package com.samprogram.mosaic.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A folder the user selected. The unique index on [treeUri] is what makes
 * re-selecting an already registered folder a no-op instead of a duplicate.
 */
@Entity(
    tableName = "source_folders",
    indices = [Index(value = ["treeUri"], unique = true)],
)
data class SourceFolderEntity(
    @PrimaryKey val id: String,
    val treeUri: String,
    val displayName: String,
    val accessStatus: String,
    val addedAt: Long,
    val lastScanSucceededAt: Long? = null,
    /** Name of the [com.samprogram.mosaic.core.model.ScanFailure], or null. */
    val lastScanFailure: String? = null,
)
