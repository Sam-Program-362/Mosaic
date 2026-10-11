package com.samprogram.mosaic.data.source

/**
 * Access to folders the user selected through Android's Storage Access
 * Framework.
 *
 * This is the seam between the app and the document provider. Production uses
 * [AndroidMediaTreeSource]; tests use a fake, so scan and reconciliation rules
 * can be verified without a device or a real provider.
 */
interface MediaTreeSource {

    /** True when a persisted read grant for [treeUri] is currently held. */
    fun hasPersistedAccess(treeUri: String): Boolean

    /**
     * Persists the read grant for [treeUri] using only the flags the picker
     * actually granted. Returns true when persisted access is now held.
     */
    fun takePersistableAccess(treeUri: String, readGranted: Boolean): Boolean

    /** Releases the persisted grant for [treeUri]. Never touches source files. */
    fun releasePersistedAccess(treeUri: String)

    /** The provider's display name for the tree itself, or null when unknown. */
    suspend fun queryTreeDisplayName(treeUri: String): String?

    /**
     * Enumerates the immediate children of [treeUri]. Implementations must not
     * descend into nested folders and must run off the main thread.
     */
    suspend fun listDirectChildren(treeUri: String): TreeScanResult
}
