package com.samprogram.mosaic.data.repository

import com.samprogram.mosaic.core.model.ScanFailure

/** The outcome of adding a folder that the user picked. */
sealed interface AddFolderResult {

    /** A new folder was registered and scanned. */
    data class Added(val folderId: String, val indexedCount: Int) : AddFolderResult

    /**
     * The folder was already registered. No duplicate record is created; the
     * existing folder is rescanned, which is also how access is restored.
     */
    data class AlreadyRegistered(val folderId: String, val indexedCount: Int) : AddFolderResult

    /** The picker did not grant read access, so no folder record was created. */
    data object AccessDenied : AddFolderResult

    /** The returned location was empty or unusable. */
    data object InvalidSelection : AddFolderResult
}

/** The outcome of scanning one folder. */
sealed interface FolderScanOutcome {

    /** The scan succeeded; [indexedCount] is the number of media files found. */
    data class Indexed(val indexedCount: Int) : FolderScanOutcome

    /** The folder could not be read; its last-known media were preserved. */
    data class Unavailable(val reason: ScanFailure) : FolderScanOutcome

    /** The folder is no longer registered. */
    data object Missing : FolderScanOutcome
}

/** A summary of refreshing every registered folder. */
data class RefreshReport(
    val refreshedCount: Int,
    val unavailableCount: Int,
)
