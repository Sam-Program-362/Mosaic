package com.samprogram.mosaic.data.source

import com.samprogram.mosaic.core.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaIndexTest {

    @Test
    fun `nested directories are excluded from indexed results`() {
        val items = MediaIndex.index(
            FOLDER_ID,
            listOf(
                child("dir-1", "Holiday", mimeType = MIME_TYPE_DIRECTORY, isDirectory = true),
                child("doc-1", "photo.jpg", mimeType = "image/jpeg"),
            ),
        )

        assertEquals(listOf("photo.jpg"), items.map { it.displayName })
    }

    @Test
    fun `a directory identified only by its mime type is excluded`() {
        val items = MediaIndex.index(
            FOLDER_ID,
            listOf(child("dir-1", "Holiday", mimeType = MIME_TYPE_DIRECTORY, isDirectory = false)),
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `unsupported files are excluded`() {
        val items = MediaIndex.index(
            FOLDER_ID,
            listOf(
                child("doc-1", "notes.txt", mimeType = "text/plain"),
                child("doc-2", "clip.mov", mimeType = "video/quicktime"),
                child("doc-3", "photo.jpg", mimeType = "image/jpeg"),
            ),
        )

        assertEquals(listOf("photo.jpg"), items.map { it.displayName })
    }

    @Test
    fun `every supported kind is indexed`() {
        val items = MediaIndex.index(
            FOLDER_ID,
            listOf(
                child("doc-1", "photo.jpg", mimeType = "image/jpeg"),
                child("doc-2", "anim.gif", mimeType = "image/gif"),
                child("doc-3", "clip.mp4", mimeType = "video/mp4"),
            ),
        )

        assertEquals(3, items.size)
        assertEquals(
            listOf(MediaKind.VIDEO, MediaKind.GIF, MediaKind.IMAGE).toSet(),
            items.map { it.mediaKind }.toSet(),
        )
    }

    @Test
    fun `indexed items keep the provider identity and metadata`() {
        val item = MediaIndex.index(
            FOLDER_ID,
            listOf(
                child(
                    documentId = "doc-9",
                    displayName = "photo.jpg",
                    mimeType = "image/jpeg",
                    sizeBytes = 1234L,
                    lastModified = 9999L,
                ),
            ),
        ).single()

        assertEquals("doc-9", item.documentId)
        assertEquals("content://doc-9", item.contentUri)
        assertEquals("content://doc-9", item.id)
        assertEquals("photo.jpg", item.displayName)
        assertEquals("image/jpeg", item.mimeType)
        assertEquals(1234L, item.sizeBytes ?: -1L)
        assertEquals(9999L, item.lastModified ?: -1L)
        assertEquals(FOLDER_ID, item.folderId)
    }

    @Test
    fun `files with duplicate display names keep distinct stable identities`() {
        val items = MediaIndex.index(
            FOLDER_ID,
            listOf(
                child("doc-1", "IMG_0001.jpg", mimeType = "image/jpeg"),
                child("doc-2", "IMG_0001.jpg", mimeType = "image/jpeg"),
            ),
        )

        assertEquals(2, items.size)
        assertEquals(2, items.map { it.id }.toSet().size)
        assertEquals(items.map { it.id }, items.map { it.contentUri })
    }

    @Test
    fun `an empty folder indexes to nothing`() {
        assertTrue(MediaIndex.index(FOLDER_ID, emptyList()).isEmpty())
    }

    @Test
    fun `children without a display name are skipped`() {
        val items = MediaIndex.index(FOLDER_ID, listOf(child("doc-1", "   ", mimeType = "image/jpeg")))

        assertTrue(items.isEmpty())
    }

    @Test
    fun `indexing is ordered by display name ignoring case`() {
        val items = MediaIndex.index(
            FOLDER_ID,
            listOf(
                child("doc-1", "zebra.jpg", mimeType = "image/jpeg"),
                child("doc-2", "Apple.jpg", mimeType = "image/jpeg"),
                child("doc-3", "mango.jpg", mimeType = "image/jpeg"),
            ),
        )

        assertEquals(listOf("Apple.jpg", "mango.jpg", "zebra.jpg"), items.map { it.displayName })
    }

    private fun child(
        documentId: String,
        displayName: String,
        mimeType: String?,
        sizeBytes: Long? = null,
        lastModified: Long? = null,
        isDirectory: Boolean = false,
    ) = DocumentChild(
        documentId = documentId,
        contentUri = "content://$documentId",
        displayName = displayName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        lastModified = lastModified,
        isDirectory = isDirectory,
    )

    private companion object {
        const val FOLDER_ID = "folder-1"
    }
}
