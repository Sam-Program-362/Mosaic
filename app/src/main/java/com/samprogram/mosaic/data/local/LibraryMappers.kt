package com.samprogram.mosaic.data.local

import com.samprogram.mosaic.core.model.FolderAccessStatus
import com.samprogram.mosaic.core.model.MediaItem
import com.samprogram.mosaic.core.model.MediaKind
import com.samprogram.mosaic.core.model.ScanFailure
import com.samprogram.mosaic.core.model.SourceFolder

// Persistence entities deliberately stay separate from domain models, so the
// stored column types are converted here rather than leaking into the UI.

internal fun SourceFolderEntity.toDomain(): SourceFolder = SourceFolder(
    id = id,
    treeUri = treeUri,
    displayName = displayName,
    accessStatus = accessStatusOf(accessStatus),
    addedAt = addedAt,
    lastScanSucceededAt = lastScanSucceededAt,
    lastScanFailure = lastScanFailure?.let { stored ->
        ScanFailure.entries.firstOrNull { it.name == stored }
    },
)

internal fun SourceFolder.toEntity(): SourceFolderEntity = SourceFolderEntity(
    id = id,
    treeUri = treeUri,
    displayName = displayName,
    accessStatus = accessStatus.name,
    addedAt = addedAt,
    lastScanSucceededAt = lastScanSucceededAt,
    lastScanFailure = lastScanFailure?.name,
)

internal fun MediaItemEntity.toDomain(): MediaItem = MediaItem(
    id = id,
    folderId = folderId,
    documentId = documentId,
    contentUri = contentUri,
    displayName = displayName,
    mediaKind = mediaKindOf(mediaKind),
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    lastModified = lastModified,
)

internal fun MediaItem.toEntity(): MediaItemEntity = MediaItemEntity(
    id = id,
    folderId = folderId,
    documentId = documentId,
    contentUri = contentUri,
    displayName = displayName,
    mediaKind = mediaKind.name,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    lastModified = lastModified,
)

// Defensive lookups: only this app writes these columns, but an unexpected
// stored value should not crash the folder list.

private fun accessStatusOf(stored: String): FolderAccessStatus =
    FolderAccessStatus.entries.firstOrNull { it.name == stored } ?: FolderAccessStatus.UNAVAILABLE

private fun mediaKindOf(stored: String): MediaKind =
    MediaKind.entries.firstOrNull { it.name == stored } ?: MediaKind.IMAGE
