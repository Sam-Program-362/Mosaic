package com.samprogram.mosaic.data.source

import com.samprogram.mosaic.core.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaTypeClassifierTest {

    @Test
    fun `provider mime types map to the supported kinds`() {
        assertEquals(MediaKind.IMAGE, MediaTypeClassifier.classify("photo.jpg", "image/jpeg"))
        assertEquals(MediaKind.IMAGE, MediaTypeClassifier.classify("shot.png", "image/png"))
        assertEquals(MediaKind.GIF, MediaTypeClassifier.classify("anim.gif", "image/gif"))
        assertEquals(MediaKind.VIDEO, MediaTypeClassifier.classify("clip.mp4", "video/mp4"))
    }

    @Test
    fun `unsupported mime types are rejected even when the name looks familiar`() {
        assertNull(MediaTypeClassifier.classify("document.pdf", "application/pdf"))
        assertNull(MediaTypeClassifier.classify("clip.mov", "video/quicktime"))
        assertNull(MediaTypeClassifier.classify("notes.txt", "text/plain"))
        assertNull(MediaTypeClassifier.classify("song.mp3", "audio/mpeg"))
    }

    @Test
    fun `mime parameters and casing do not change the result`() {
        assertEquals(MediaKind.GIF, MediaTypeClassifier.classify("anim.gif", "image/gif; charset=binary"))
        assertEquals(MediaKind.VIDEO, MediaTypeClassifier.classify("clip.mp4", "VIDEO/MP4"))
        assertEquals(MediaKind.IMAGE, MediaTypeClassifier.classify("photo.jpg", "  image/jpeg  "))
    }

    @Test
    fun `a specific mime type wins over the extension`() {
        assertEquals(MediaKind.GIF, MediaTypeClassifier.classify("actually-a-gif.png", "image/gif"))
        assertEquals(MediaKind.IMAGE, MediaTypeClassifier.classify("actually-a-photo.gif", "image/jpeg"))
    }

    @Test
    fun `unknown mime types fall back to a conservative extension check`() {
        assertEquals(MediaKind.IMAGE, MediaTypeClassifier.classify("photo.PNG", "application/octet-stream"))
        assertEquals(MediaKind.VIDEO, MediaTypeClassifier.classify("clip.MP4", "application/octet-stream"))
        assertEquals(MediaKind.GIF, MediaTypeClassifier.classify("anim.GIF", "application/octet-stream"))
        assertEquals(MediaKind.IMAGE, MediaTypeClassifier.classify("photo.jpeg", null))
        assertEquals(MediaKind.IMAGE, MediaKind.IMAGE.let { MediaTypeClassifier.classify("photo.webp", "") })
    }

    @Test
    fun `files with no usable type are unsupported`() {
        assertNull(MediaTypeClassifier.classify("README", null))
        assertNull(MediaTypeClassifier.classify(".hidden", null))
        assertNull(MediaTypeClassifier.classify("trailing.", null))
        assertNull(MediaTypeClassifier.classify("archive.tar.gz", "application/x-tar"))
        assertNull(MediaTypeClassifier.classify("archive.zip", "application/octet-stream"))
        assertNull(MediaTypeClassifier.classify("", null))
    }
}
