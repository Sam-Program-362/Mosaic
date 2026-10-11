package com.samprogram.mosaic.data.source

import com.samprogram.mosaic.core.model.ScanFailure

/**
 * Mirrors `DocumentsContract.Document.MIME_TYPE_DIR`.
 *
 * It is declared here as a plain constant so that direct-child filtering stays
 * free of Android framework references and can be unit tested on the JVM.
 */
const val MIME_TYPE_DIRECTORY: String = "vnd.android.document/directory"

/**
 * One direct child entry returned by a document provider, before any filtering.
 *
 * This is the raw shape of a provider row. Whether the entry is a directory or
 * an unsupported file is decided later by [MediaIndex], which keeps the
 * provider query and the indexing rules independently testable.
 */
data class DocumentChild(
    val documentId: String,
    val contentUri: String,
    val displayName: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val lastModified: Long? = null,
    val isDirectory: Boolean = false,
)

/** The outcome of enumerating the direct children of one selected folder. */
sealed interface TreeScanResult {

    /**
     * The folder was enumerated successfully. An empty list is a valid,
     * successfully-scanned empty folder and is never treated as an error.
     */
    data class Success(val children: List<DocumentChild>) : TreeScanResult

    /** The folder could not be read. Last-known media must be preserved. */
    data class Failure(val reason: ScanFailure) : TreeScanResult
}
