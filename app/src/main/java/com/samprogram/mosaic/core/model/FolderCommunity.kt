package com.samprogram.mosaic.core.model

/**
 * The community record that is automatically created for each selected folder.
 *
 * [folderId] is null for custom communities, which are not implemented yet.
 * The community is kept even while its folder is unavailable so that its
 * identity survives a temporary loss of access.
 */
data class FolderCommunity(
    val id: String,
    val folderId: String?,
    val name: String,
    val createdAt: Long,
)
