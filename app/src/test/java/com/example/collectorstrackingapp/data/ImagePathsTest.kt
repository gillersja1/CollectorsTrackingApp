package com.example.collectorstrackingapp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePathsTest {

    @Test
    fun appStoragePath_isLocal() {
        // Regression test: this is how the item screen saves photos, and the old
        // check didn't count it as local, so no photo was ever uploaded.
        assertTrue(ImagePaths.isLocal("/data/user/0/com.example.collectorstrackingapp/files/collectible_1.jpg"))
    }

    @Test
    fun contentAndFileUris_areLocal() {
        assertTrue(ImagePaths.isLocal("content://media/external/images/media/42"))
        assertTrue(ImagePaths.isLocal("file:///sdcard/Pictures/card.jpg"))
    }

    @Test
    fun cloudUrl_isNotLocal() {
        assertFalse(ImagePaths.isLocal("https://firebasestorage.googleapis.com/v0/b/app/o/users%2Fuid%2Fimages%2Fa.jpg"))
    }

    @Test
    fun missingPath_isNotLocal() {
        assertFalse(ImagePaths.isLocal(null))
        assertFalse(ImagePaths.isLocal(""))
        assertFalse(ImagePaths.isLocal("   "))
    }
}
