package com.samprogram.mosaic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Local metadata store. It holds folder grants, community records and indexed
 * media metadata only; media never leaves the user's folder.
 *
 * Room enables foreign keys, so deleting a folder cascades to its media and its
 * community, which keeps the database consistent on removal.
 */
@Database(
    entities = [
        SourceFolderEntity::class,
        CommunityEntity::class,
        MediaItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MosaicDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao

    abstract fun mediaDao(): MediaDao

    abstract fun communityDao(): CommunityDao

    companion object {
        const val NAME: String = "mosaic.db"
    }
}
