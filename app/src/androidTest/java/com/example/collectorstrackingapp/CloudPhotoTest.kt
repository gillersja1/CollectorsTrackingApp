package com.example.collectorstrackingapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.collectorstrackingapp.data.CloudPhotos
import com.example.collectorstrackingapp.data.FirebaseSyncManager
import com.example.collectorstrackingapp.data.PhotoShrinker
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Photos in Firestore, for real, on a device: a full-size camera photo goes up shrunk to fit one document, comes
 * back down through Coil on a phone that doesn't have it, and is gone once deleted. Uses the Firebase emulators.
 */
@RunWith(AndroidJUnit4::class)
class CloudPhotoTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val cloud = FirebaseSyncManager(context)
    private lateinit var uid: String

    @Before
    fun signUp() {
        val auth = FirebaseAuth.getInstance()
        auth.signOut()
        val result = Tasks.await(auth.createUserWithEmailAndPassword("photos-${System.nanoTime()}@example.com", "correct-horse-42"), 20, TimeUnit.SECONDS)
        uid = result.user!!.uid
    }

    @After
    fun signOut() = FirebaseAuth.getInstance().signOut()

    /** A 12 MP "camera photo" with some detail in it, so it doesn't compress to nothing. */
    private fun cameraPhoto(): File {
        val bitmap = Bitmap.createBitmap(4032, 3024, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        for (i in 0 until 400) {
            paint.color = Color.rgb((i * 37) % 256, (i * 91) % 256, (i * 53) % 256)
            canvas.drawCircle((i * 97 % 4032).toFloat(), (i * 61 % 3024).toFloat(), 40f + i % 120, paint)
        }
        return File(context.cacheDir, "camera-${System.nanoTime()}.jpg").also { file ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        }
    }

    @Test
    fun aPhotoGoesUpShrunkComesBackOnAnotherPhoneAndIsGoneOnceDeleted() = runBlocking {
        val original = cameraPhoto()

        // Up: shrunk to fit one Firestore document.
        val path = cloud.uploadImage(original.absolutePath)!!
        assertTrue(path, CloudPhotos.isCloudPhoto(path))
        val id = CloudPhotos.idOf(path)
        val doc = Tasks.await(FirebaseFirestore.getInstance().document("users/$uid/photos/$id").get(), 10, TimeUnit.SECONDS)
        val stored = doc.getBlob("data")!!.toBytes()
        assertTrue("stored ${stored.size} bytes", stored.size <= PhotoShrinker.MAX_BYTES)
        assertTrue("from ${original.length()} bytes", stored.size < original.length())
        assertEquals("image/jpeg", doc.getString("contentType"))
        val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            .also { BitmapFactory.decodeByteArray(stored, 0, stored.size, it) }
        assertEquals(1024, maxOf(size.outWidth, size.outHeight))

        // Down: a phone without its own copy (cleared here) gets it through Coil, as the list and item screens do.
        val cached = CloudPhotos.cacheFile(context, id)
        cached.delete()
        val shown = context.imageLoader.execute(ImageRequest.Builder(context).data(path).memoryCacheKey(null as String?).build())
        assertTrue("Coil couldn't show $path: $shown", shown is SuccessResult)
        assertTrue("the downloaded copy is kept", cached.exists())

        // Gone.
        assertTrue(cloud.deletePhoto(path))
        val after = Tasks.await(FirebaseFirestore.getInstance().document("users/$uid/photos/$id").get(), 10, TimeUnit.SECONDS)
        assertFalse(after.exists())
        assertFalse(cached.exists())
    }
}
