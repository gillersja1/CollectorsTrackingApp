package com.example.collectorstrackingapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Photos are shrunk to fit one Firestore document; the sizing and quality rules, without a real image. */
class PhotoShrinkerTest {

    @Test
    fun aLargePhotoIsScaledSoItsLongestSideIs1024() {
        assertEquals(1024 to 768, PhotoShrinker.targetSize(4000, 3000))
        assertEquals(768 to 1024, PhotoShrinker.targetSize(3000, 4000))
    }

    @Test
    fun aPhotoThatAlreadyFitsIsLeftAlone() {
        assertEquals(800 to 600, PhotoShrinker.targetSize(800, 600))
        assertEquals(1024 to 1024, PhotoShrinker.targetSize(1024, 1024))
    }

    @Test
    fun aVeryThinPhotoKeepsAtLeastOnePixel() {
        assertEquals(1024 to 1, PhotoShrinker.targetSize(20000, 3))
    }

    @Test
    fun hugePhotosAreDecodedAtAFractionOfTheirSize() {
        assertEquals(1, PhotoShrinker.sampleSize(1600, 1200))
        assertEquals(2, PhotoShrinker.sampleSize(3000, 2000))
        // Never below 1024 on the longest side, so the final scale-down still has detail to work with.
        assertEquals(2, PhotoShrinker.sampleSize(4032, 3024)) // a 12 MP phone photo decodes at 2016 x 1512
        assertEquals(4, PhotoShrinker.sampleSize(8000, 6000))
    }

    @Test
    fun theBestQualityThatFitsIsUsed() {
        val tried = mutableListOf<Int>()

        val result = PhotoShrinker.fit { quality ->
            tried += quality
            ByteArray(if (quality > 65) 800_000 else 500_000)
        }

        assertEquals(listOf(85, 75, 65), tried)
        assertEquals(500_000, result!!.size)
    }

    @Test
    fun aPhotoThatNeverFitsIsRefused() {
        assertNull(PhotoShrinker.fit { ByteArray(PhotoShrinker.MAX_BYTES + 1) })
    }

    @Test
    fun cloudPhotoPathsRoundTrip() {
        val path = CloudPhotos.pathFor("abc-123")

        assertEquals("firestore-photo://abc-123", path)
        assertTrue(CloudPhotos.isCloudPhoto(path))
        assertEquals("abc-123", CloudPhotos.idOf(path))
        assertFalse(CloudPhotos.isCloudPhoto("/data/photos/batman.jpg"))
        assertFalse(ImagePaths.isLocal(path))
    }
}
