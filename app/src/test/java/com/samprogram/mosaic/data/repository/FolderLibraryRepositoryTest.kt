package com.samprogram.mosaic.data.repository

import com.samprogram.mosaic.core.model.FolderAccessStatus
import com.samprogram.mosaic.core.model.ScanFailure
import com.samprogram.mosaic.data.source.DocumentChild
import com.samprogram.mosaic.data.source.MIME_TYPE_DIRECTORY
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderLibraryRepositoryTest {

    private val folderDao = FakeFolderDao()
    private val mediaDao = FakeMediaDao()
    private val communityDao = FakeCommunityDao()
    private val source = FakeMediaTreeSource()

    private var now = 1_000L
    private var idSeed = 0

    private val repository = FolderLibraryRepository(
        folderDao = folderDao,
        mediaDao = mediaDao,
        communityDao = communityDao,
        treeSource = source,
        clock = { now++ },
        newId = { "id-${idSeed++}" },
    )

    @Test
    fun `adding a folder registers it, creates its community and indexes direct children`() = runTest {
        source.registerFolder(
            CAMERA_URI,
            displayName = "Camera",
            directChildren = listOf(
                file("doc-1", "photo.jpg", "image/jpeg"),
                file("doc-2", "clip.mp4", "video/mp4"),
            ),
        )

        val result = repository.addFolder(CAMERA_URI, grantedRead = true)

        assertTrue(result is AddFolderResult.Added)
        assertEquals(2, (result as AddFolderResult.Added).indexedCount)

        val folder = folderDao.getAll().single()
        assertEquals("Camera", folder.displayName)
        assertEquals(CAMERA_URI, folder.treeUri)
        assertEquals(FolderAccessStatus.AVAILABLE.name, folder.accessStatus)
        assertNull(folder.lastScanFailure)
        assertNotNull(folder.lastScanSucceededAt)

        assertEquals(2, mediaDao.all().size)
        assertEquals(setOf("photo.jpg", "clip.mp4"), mediaDao.all().map { it.displayName }.toSet())

        val community = communityDao.all().single()
        assertEquals("Camera", community.name)
        assertEquals(folder.id, community.folderId)
    }

    @Test
    fun `selecting an already registered folder does not create a duplicate`() = runTest {
        source.registerFolder(CAMERA_URI, "Camera", listOf(file("doc-1", "photo.jpg", "image/jpeg")))
        repository.addFolder(CAMERA_URI, grantedRead = true)

        source.setChildren(CAMERA_URI, listOf(file("doc-1", "photo.jpg", "image/jpeg")))
        val second = repository.addFolder(CAMERA_URI, grantedRead = true)

        assertTrue(second is AddFolderResult.AlreadyRegistered)
        assertEquals(1, folderDao.getAll().size)
        assertEquals(1, communityDao.all().size)
        assertEquals(1, mediaDao.all().size)
    }

    @Test
    fun `a folder picked without read access is not registered`() = runTest {
        val result = repository.addFolder(CAMERA_URI, grantedRead = false)

        assertEquals(AddFolderResult.AccessDenied, result)
        assertTrue(folderDao.getAll().isEmpty())
        assertTrue(communityDao.all().isEmpty())
    }

    @Test
    fun `an empty selection is rejected`() = runTest {
        assertEquals(AddFolderResult.InvalidSelection, repository.addFolder("   ", grantedRead = true))
        assertTrue(folderDao.getAll().isEmpty())
    }

    @Test
    fun `a successfully scanned empty folder is a valid empty state, not an error`() = runTest {
        source.registerFolder(CAMERA_URI, "Empty", directChildren = emptyList())

        val result = repository.addFolder(CAMERA_URI, grantedRead = true)

        assertEquals(0, (result as AddFolderResult.Added).indexedCount)
        val folder = folderDao.getAll().single()
        assertEquals(FolderAccessStatus.AVAILABLE.name, folder.accessStatus)
        assertNull(folder.lastScanFailure)
        assertNotNull(folder.lastScanSucceededAt)
        assertTrue(mediaDao.all().isEmpty())
    }

    @Test
    fun `nested directories are not indexed`() = runTest {
        source.registerFolder(
            CAMERA_URI,
            "Camera",
            directChildren = listOf(
                directory("dir-1", "Nested holiday folder"),
                file("doc-1", "photo.jpg", "image/jpeg"),
            ),
        )

        repository.addFolder(CAMERA_URI, grantedRead = true)

        assertEquals(listOf("photo.jpg"), mediaDao.all().map { it.displayName })
    }

    @Test
    fun `a later successful scan adds newly discovered media`() = runTest {
        source.registerFolder(CAMERA_URI, "Camera", listOf(file("doc-1", "photo.jpg", "image/jpeg")))
        val folderId = (repository.addFolder(CAMERA_URI, grantedRead = true) as AddFolderResult.Added).folderId

        source.setChildren(
            CAMERA_URI,
            listOf(
                file("doc-1", "photo.jpg", "image/jpeg"),
                file("doc-2", "anim.gif", "image/gif"),
            ),
        )
        val outcome = repository.refreshFolder(folderId)

        assertEquals(2, (outcome as FolderScanOutcome.Indexed).indexedCount)
        assertEquals(2, mediaDao.all().size)
        assertEquals(1, communityDao.all().size)
    }

    @Test
    fun `a successful scan removes media that disappeared`() = runTest {
        source.registerFolder(
            CAMERA_URI,
            "Camera",
            listOf(
                file("doc-1", "photo.jpg", "image/jpeg"),
                file("doc-2", "clip.mp4", "video/mp4"),
            ),
        )
        val folderId = (repository.addFolder(CAMERA_URI, grantedRead = true) as AddFolderResult.Added).folderId
        assertEquals(2, mediaDao.all().size)

        source.setChildren(CAMERA_URI, listOf(file("doc-1", "photo.jpg", "image/jpeg")))
        val outcome = repository.refreshFolder(folderId)

        assertEquals(1, (outcome as FolderScanOutcome.Indexed).indexedCount)
        assertEquals(listOf("photo.jpg"), mediaDao.all().map { it.displayName })
    }

    @Test
    fun `a scan that finds nothing removes the previously indexed media`() = runTest {
        source.registerFolder(CAMERA_URI, "Camera", listOf(file("doc-1", "photo.jpg", "image/jpeg")))
        val folderId = (repository.addFolder(CAMERA_URI, grantedRead = true) as AddFolderResult.Added).folderId

        source.setChildren(CAMERA_URI, emptyList())
        repository.refreshFolder(folderId)

        assertTrue(mediaDao.all().isEmpty())
    }

    @Test
    fun `a failed scan preserves indexed media and marks the folder unavailable`() = runTest {
        source.registerFolder(
            CAMERA_URI,
            "Camera",
            listOf(
                file("doc-1", "photo.jpg", "image/jpeg"),
                file("doc-2", "clip.mp4", "video/mp4"),
            ),
        )
        val folderId = (repository.addFolder(CAMERA_URI, grantedRead = true) as AddFolderResult.Added).folderId
        val succeededAt = folderDao.getAll().single().lastScanSucceededAt

        source.failWith(CAMERA_URI, ScanFailure.PROVIDER_ERROR)
        val outcome = repository.refreshFolder(folderId)

        assertEquals(FolderScanOutcome.Unavailable(ScanFailure.PROVIDER_ERROR), outcome)

        // Last-known media and the community survive a provider failure.
        assertEquals(2, mediaDao.all().size)
        assertEquals(1, communityDao.all().size)

        val folder = folderDao.getAll().single()
        assertEquals(FolderAccessStatus.UNAVAILABLE.name, folder.accessStatus)
        assertEquals(ScanFailure.PROVIDER_ERROR.name, folder.lastScanFailure)
        assertEquals(succeededAt, folder.lastScanSucceededAt)
    }

    @Test
    fun `a provider failure is never reported as an empty folder`() = runTest {
        source.registerFolder(CAMERA_URI, "Camera", listOf(file("doc-1", "photo.jpg", "image/jpeg")))
        val folderId = (repository.addFolder(CAMERA_URI, grantedRead = true) as AddFolderResult.Added).folderId

        source.failWith(CAMERA_URI, ScanFailure.IO_ERROR)
        repository.refreshFolder(folderId)

        val folder = repository.observeFolders().first().single()
        assertEquals(FolderAccessStatus.UNAVAILABLE, folder.accessStatus)
        assertEquals(listOf("photo.jpg"), repository.observeMediaInFolder(folderId).first().map { it.displayName })
    }

    @Test
    fun `revoked access is reported as a missing grant and keeps existing media`() = runTest {
        source.registerFolder(CAMERA_URI, "Camera", listOf(file("doc-1", "photo.jpg", "image/jpeg")))
        val folderId = (repository.addFolder(CAMERA_URI, grantedRead = true) as AddFolderResult.Added).folderId

        source.revokeAccess(CAMERA_URI)
        val outcome = repository.refreshFolder(folderId)

        assertEquals(FolderScanOutcome.Unavailable(ScanFailure.ACCESS_NOT_GRANTED), outcome)
        assertEquals(1, mediaDao.all().size)

        val folder = folderDao.getAll().single()
        assertEquals(FolderAccessStatus.UNAVAILABLE.name, folder.accessStatus)
        assertEquals(ScanFailure.ACCESS_NOT_GRANTED.name, folder.lastScanFailure)
    }

    @Test
    fun `restored access recovers an unavailable folder without duplicating it`() = runTest {
        source.registerFolder(CAMERA_URI, "Camera", listOf(file("doc-1", "photo.jpg", "image/jpeg")))
        val folderId = (repository.addFolder(CAMERA_URI, grantedRead = true) as AddFolderResult.Added).folderId

        source.revokeAccess(CAMERA_URI)
        repository.refreshFolder(folderId)
        assertEquals(FolderAccessStatus.UNAVAILABLE.name, folderDao.getAll().single().accessStatus)

        // The user selects the same folder again once access is available.
        source.setChildren(
            CAMERA_URI,
            listOf(
                file("doc-1", "photo.jpg", "image/jpeg"),
                file("doc-2", "anim.gif", "image/gif"),
            ),
        )
        val result = repository.addFolder(CAMERA_URI, grantedRead = true)

        assertEquals(2, (result as AddFolderResult.AlreadyRegistered).indexedCount)
        assertEquals(1, folderDao.getAll().size)
        assertEquals(1, communityDao.all().size)
        assertEquals(2, mediaDao.all().size)

        val folder = folderDao.getAll().single()
        assertEquals(FolderAccessStatus.AVAILABLE.name, folder.accessStatus)
        assertNull(folder.lastScanFailure)
    }

    @Test
    fun `removing a folder deletes only Mosaic metadata and releases the grant`() = runTest {
        source.registerFolder(CAMERA_URI, "Camera", listOf(file("doc-1", "photo.jpg", "image/jpeg")))
        val folderId = (repository.addFolder(CAMERA_URI, grantedRead = true) as AddFolderResult.Added).folderId

        repository.removeFolder(folderId)

        assertTrue(folderDao.getAll().isEmpty())
        assertTrue(mediaDao.all().isEmpty())
        assertTrue(communityDao.all().isEmpty())
        assertEquals(listOf(CAMERA_URI), source.releasedAccess)
    }

    @Test
    fun `removing an unknown folder is a no-op`() = runTest {
        repository.removeFolder("does-not-exist")

        assertTrue(source.releasedAccess.isEmpty())
    }

    @Test
    fun `refreshAll counts unavailable folders separately`() = runTest {
        source.registerFolder(CAMERA_URI, "Camera", listOf(file("doc-1", "photo.jpg", "image/jpeg")))
        val brokenUri = "content://tree/primary%3ABroken"
        source.registerFolder(brokenUri, "Broken", listOf(file("doc-2", "photo.jpg", "image/jpeg")))

        repository.addFolder(CAMERA_URI, grantedRead = true)
        repository.addFolder(brokenUri, grantedRead = true)

        source.failWith(brokenUri, ScanFailure.PROVIDER_ERROR)
        val report = repository.refreshAll()

        assertEquals(1, report.refreshedCount)
        assertEquals(1, report.unavailableCount)
    }

    @Test
    fun `media counts are reported per folder`() = runTest {
        source.registerFolder(
            CAMERA_URI,
            "Camera",
            listOf(
                file("doc-1", "photo.jpg", "image/jpeg"),
                file("doc-2", "clip.mp4", "video/mp4"),
            ),
        )
        val otherUri = "content://tree/primary%3APictures"
        source.registerFolder(otherUri, "Pictures", emptyList())

        val cameraId = (repository.addFolder(CAMERA_URI, grantedRead = true) as AddFolderResult.Added).folderId
        val otherId = (repository.addFolder(otherUri, grantedRead = true) as AddFolderResult.Added).folderId

        val counts = repository.observeMediaCounts().first()

        assertEquals(2, counts[cameraId])
        assertEquals(0, counts[otherId] ?: 0)
    }

    @Test
    fun `media with duplicate display names are indexed separately`() = runTest {
        source.registerFolder(
            CAMERA_URI,
            "Camera",
            listOf(
                file("doc-1", "IMG_0001.jpg", "image/jpeg"),
                file("doc-9", "IMG_0001.jpg", "image/jpeg"),
            ),
        )

        repository.addFolder(CAMERA_URI, grantedRead = true)

        assertEquals(2, mediaDao.all().size)
        assertEquals(2, mediaDao.all().map { it.id }.toSet().size)
    }

    private fun file(documentId: String, displayName: String, mimeType: String) = DocumentChild(
        documentId = documentId,
        contentUri = "content://$documentId",
        displayName = displayName,
        mimeType = mimeType,
        sizeBytes = 10L,
        lastModified = 20L,
    )

    private fun directory(documentId: String, displayName: String) = DocumentChild(
        documentId = documentId,
        contentUri = "content://$documentId",
        displayName = displayName,
        mimeType = MIME_TYPE_DIRECTORY,
        isDirectory = true,
    )

    private companion object {
        const val CAMERA_URI = "content://tree/primary%3ACamera"
    }
}
