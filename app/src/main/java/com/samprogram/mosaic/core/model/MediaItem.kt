package com.samprogram.mosaic.core.model

/**
 * An indexed media file discovered directly inside a selected folder.
 *
 * [id] is the stable internal identity of the item. It is derived from the
 * document URI rather than from [displayName], so two files that share a
 * display name in the same folder stay distinct.
 */
data class MediaItem(
    val id: String,
    val folderId: String,
    val documentId: String,
    val contentUri: String,
    val displayName: String,
    val mediaKind: MediaKind,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val lastModified: Long? = null,
)
