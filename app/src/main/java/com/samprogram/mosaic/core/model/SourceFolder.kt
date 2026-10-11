package com.samprogram.mosaic.core.model

/**
 * A folder the user explicitly selected through the Storage Access Framework.
 *
 * [id] is the app's stable internal identity for the folder. [treeUri] is the
 * document-provider tree URI that the picker returned and that the persisted
 * access grant applies to.
 */
data class SourceFolder(
    val id: String,
    val treeUri: String,
    val displayName: String,
    val accessStatus: FolderAccessStatus,
    val addedAt: Long,
    val lastScanSucceededAt: Long? = null,
    /** Set only when the most recent scan of this folder failed. */
    val lastScanFailure: ScanFailure? = null,
)
