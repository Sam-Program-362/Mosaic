package com.samprogram.mosaic

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.samprogram.mosaic.data.local.MosaicDatabase
import com.samprogram.mosaic.data.repository.FolderLibraryRepository
import com.samprogram.mosaic.data.source.AndroidMediaTreeSource
import com.samprogram.mosaic.data.source.MediaTreeSource

/**
 * The app's object graph. It is written by hand so the dependencies stay
 * explicit; a DI framework is not warranted for a single feature.
 *
 * Only the application context is retained here, never an Activity.
 */
class AppContainer(context: Context) {

    private val applicationContext: Context = context.applicationContext

    private val database: MosaicDatabase = Room.databaseBuilder(
        applicationContext,
        MosaicDatabase::class.java,
        MosaicDatabase.NAME,
    ).build()

    private val treeSource: MediaTreeSource = AndroidMediaTreeSource(applicationContext.contentResolver)

    val folderLibraryRepository: FolderLibraryRepository = FolderLibraryRepository(
        folderDao = database.folderDao(),
        mediaDao = database.mediaDao(),
        communityDao = database.communityDao(),
        treeSource = treeSource,
    )
}

class MosaicApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
