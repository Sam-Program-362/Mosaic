package com.samprogram.mosaic.core.model

/**
 * Whether the app can currently read a selected folder.
 *
 * A folder that cannot be read is never deleted: it keeps this status and its
 * last-known media until a later refresh finds that access has been restored.
 */
enum class FolderAccessStatus {
    AVAILABLE,
    UNAVAILABLE,
}
