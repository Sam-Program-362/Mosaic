package com.samprogram.mosaic.data.source

import android.content.ContentResolver
import android.content.Intent
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.samprogram.mosaic.core.model.ScanFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * [MediaTreeSource] backed by Android's Storage Access Framework.
 *
 * Only folders the user picked are ever read, and only their immediate
 * children are queried: the child URI is built from the tree document id, so
 * nested folders are never traversed. All provider work runs on [Dispatchers.IO].
 */
class AndroidMediaTreeSource(
    private val contentResolver: ContentResolver,
) : MediaTreeSource {

    override fun hasPersistedAccess(treeUri: String): Boolean {
        val uri = treeUri.toUri()
        return contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
    }

    override fun takePersistableAccess(treeUri: String, readGranted: Boolean): Boolean {
        if (!readGranted) return false
        return try {
            // Only the read grant is persisted. Any write grant the picker also
            // offered is deliberately dropped: Mosaic never modifies sources.
            contentResolver.takePersistableUriPermission(
                treeUri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            true
        } catch (e: SecurityException) {
            false
        }
    }

    override fun releasePersistedAccess(treeUri: String) {
        try {
            contentResolver.releasePersistableUriPermission(
                treeUri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            // Already released, or the provider revoked it first.
        }
    }

    override suspend fun queryTreeDisplayName(treeUri: String): String? = withContext(Dispatchers.IO) {
        val uri = treeUri.toUri()
        try {
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri),
            )
            contentResolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun listDirectChildren(treeUri: String): TreeScanResult = withContext(Dispatchers.IO) {
        val tree = treeUri.toUri()

        val childrenUri = try {
            DocumentsContract.buildChildDocumentsUriUsingTree(
                tree,
                DocumentsContract.getTreeDocumentId(tree),
            )
        } catch (e: Exception) {
            return@withContext TreeScanResult.Failure(ScanFailure.PROVIDER_ERROR)
        }

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )

        try {
            val cursor = contentResolver.query(childrenUri, projection, null, null, null)
                ?: return@withContext TreeScanResult.Failure(ScanFailure.PROVIDER_ERROR)

            cursor.use { c ->
                val idIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                if (idIndex < 0 || nameIndex < 0) {
                    return@withContext TreeScanResult.Failure(ScanFailure.PROVIDER_ERROR)
                }

                val children = ArrayList<DocumentChild>(c.count.coerceAtLeast(0))
                while (c.moveToNext()) {
                    val documentId = c.getString(idIndex) ?: continue
                    val displayName = c.getString(nameIndex) ?: continue
                    val mimeType = if (mimeIndex >= 0 && !c.isNull(mimeIndex)) c.getString(mimeIndex) else null

                    children += DocumentChild(
                        documentId = documentId,
                        contentUri = DocumentsContract.buildDocumentUriUsingTree(tree, documentId).toString(),
                        displayName = displayName,
                        mimeType = mimeType,
                        sizeBytes = if (sizeIndex >= 0 && !c.isNull(sizeIndex)) c.getLong(sizeIndex) else null,
                        lastModified = if (modifiedIndex >= 0 && !c.isNull(modifiedIndex)) {
                            c.getLong(modifiedIndex)
                        } else {
                            null
                        },
                        isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR,
                    )
                }

                TreeScanResult.Success(children)
            }
        } catch (e: SecurityException) {
            TreeScanResult.Failure(ScanFailure.ACCESS_DENIED)
        } catch (e: FileNotFoundException) {
            TreeScanResult.Failure(ScanFailure.MISSING_DOCUMENT)
        } catch (e: Exception) {
            TreeScanResult.Failure(ScanFailure.IO_ERROR)
        }
    }
}
