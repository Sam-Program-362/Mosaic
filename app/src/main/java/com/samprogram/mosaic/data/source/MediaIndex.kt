package com.samprogram.mosaic.data.source

import com.samprogram.mosaic.core.model.MediaItem

/**
 * Turns the direct children of one selected folder into indexable media items.
 *
 * The rules here are deliberately pure and free of Android references so they
 * can be unit tested: directories and unsupported files are dropped, nested
 * folders are never descended into (the caller only ever passes direct
 * children), and every item keeps its provider document identity.
 */
object MediaIndex {

    fun index(folderId: String, children: List<DocumentChild>): List<MediaItem> =
        children
            .asSequence()
            .filterNot(::isDirectory)
            .filter { it.displayName.isNotBlank() }
            .mapNotNull { child -> child.toMediaItem(folderId) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
            .toList()

    private fun isDirectory(child: DocumentChild): Boolean =
        child.isDirectory || child.mimeType?.trim()?.lowercase() == MIME_TYPE_DIRECTORY

    private fun DocumentChild.toMediaItem(folderId: String): MediaItem? {
        val kind = MediaTypeClassifier.classify(displayName, mimeType) ?: return null
        return MediaItem(
            // The document URI is the stable identity. Using it instead of the
            // display name keeps files with duplicate names distinct.
            id = contentUri,
            folderId = folderId,
            documentId = documentId,
            contentUri = contentUri,
            displayName = displayName,
            mediaKind = kind,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            lastModified = lastModified,
        )
    }
}
