package com.samprogram.mosaic.data.repository

import com.samprogram.mosaic.core.model.ScanFailure
import com.samprogram.mosaic.data.source.DocumentChild
import com.samprogram.mosaic.data.source.MediaTreeSource
import com.samprogram.mosaic.data.source.TreeScanResult

/**
 * An in-memory [MediaTreeSource]. Folder contents, display names, granted access
 * and forced failures are all driven directly by each test.
 */
class FakeMediaTreeSource : MediaTreeSource {

    val displayNames = mutableMapOf<String, String>()
    val children = mutableMapOf<String, MutableList<DocumentChild>>()
    val failures = mutableMapOf<String, ScanFailure>()
    val persistedAccess = mutableSetOf<String>()
    val releasedAccess = mutableListOf<String>()

    var scanCount: Int = 0
        private set

    /** Registers a readable folder with the given direct children. */
    fun registerFolder(treeUri: String, displayName: String, directChildren: List<DocumentChild> = emptyList()) {
        displayNames[treeUri] = displayName
        children[treeUri] = directChildren.toMutableList()
        persistedAccess += treeUri
    }

    fun setChildren(treeUri: String, directChildren: List<DocumentChild>) {
        children[treeUri] = directChildren.toMutableList()
    }

    /** Simulates the user (or the OS) revoking access to a folder. */
    fun revokeAccess(treeUri: String) {
        persistedAccess -= treeUri
    }

    fun failWith(treeUri: String, reason: ScanFailure) {
        failures[treeUri] = reason
    }

    fun clearFailure(treeUri: String) {
        failures -= treeUri
    }

    override fun hasPersistedAccess(treeUri: String): Boolean = treeUri in persistedAccess

    override fun takePersistableAccess(treeUri: String, readGranted: Boolean): Boolean {
        if (readGranted) persistedAccess += treeUri
        return readGranted
    }

    override fun releasePersistedAccess(treeUri: String) {
        releasedAccess += treeUri
        persistedAccess -= treeUri
    }

    override suspend fun queryTreeDisplayName(treeUri: String): String? = displayNames[treeUri]

    override suspend fun listDirectChildren(treeUri: String): TreeScanResult {
        scanCount++
        failures[treeUri]?.let { return TreeScanResult.Failure(it) }
        if (treeUri !in persistedAccess) return TreeScanResult.Failure(ScanFailure.ACCESS_DENIED)
        return TreeScanResult.Success(children[treeUri].orEmpty().toList())
    }
}
