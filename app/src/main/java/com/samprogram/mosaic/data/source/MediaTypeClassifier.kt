package com.samprogram.mosaic.data.source

import com.samprogram.mosaic.core.model.MediaKind

/**
 * Decides whether a document is supported media and which [MediaKind] it is.
 *
 * The document provider's MIME type is trusted when it is present and specific.
 * Providers that report nothing useful (null, blank, or the generic
 * `application/octet-stream`) fall back to a conservative file-extension check.
 *
 * Anything unsupported returns null and is simply skipped while indexing.
 */
object MediaTypeClassifier {

    private const val MIME_GIF = "image/gif"
    private const val MIME_MP4 = "video/mp4"
    private const val MIME_OCTET_STREAM = "application/octet-stream"

    /** Extensions treated as static still images when MIME data is unusable. */
    private val STILL_IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "jfif", "png", "webp", "bmp", "heic", "heif", "avif",
    )

    fun classify(displayName: String, mimeType: String?): MediaKind? {
        val normalizedMime = mimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }

        if (normalizedMime != null && normalizedMime != MIME_OCTET_STREAM) {
            return when {
                normalizedMime == MIME_GIF -> MediaKind.GIF
                normalizedMime == MIME_MP4 -> MediaKind.VIDEO
                normalizedMime.startsWith("image/") -> MediaKind.IMAGE
                else -> null
            }
        }

        return when (val extension = extensionOf(displayName)) {
            "gif" -> MediaKind.GIF
            "mp4" -> MediaKind.VIDEO
            else -> if (extension in STILL_IMAGE_EXTENSIONS) MediaKind.IMAGE else null
        }
    }

    /** The lowercase extension of [displayName], or an empty string when absent. */
    private fun extensionOf(displayName: String): String {
        val dot = displayName.lastIndexOf('.')
        if (dot <= 0 || dot == displayName.length - 1) return ""
        return displayName.substring(dot + 1).lowercase()
    }
}
