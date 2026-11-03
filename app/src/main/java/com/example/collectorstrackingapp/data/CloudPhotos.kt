package com.example.collectorstrackingapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Path.Companion.toOkioPath
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Photos are stored in Firestore itself (users/{uid}/photos/{id}), which the free plan includes, rather than in
 * Firebase Storage, which needs a billing account. An item that has one points at it as
 * `firestore-photo://{id}`; [CloudPhotoFetcher] shows it, downloading it once and keeping a copy on the phone.
 */
object CloudPhotos {
    const val SCHEME = "firestore-photo"

    fun pathFor(id: String) = "$SCHEME://$id"

    fun isCloudPhoto(path: String?) = path?.startsWith("$SCHEME://") == true

    fun idOf(path: String): String = path.removePrefix("$SCHEME://")

    /** Where this phone keeps its copy of a cloud photo. */
    fun cacheFile(context: Context, id: String): File =
        File(context.filesDir, "cloud-photos").apply { mkdirs() }.resolve("$id.jpg")
}

/**
 * Shrinks a photo to fit in one Firestore document (which holds at most 1 MiB): at most [MAX_SIDE] pixels on
 * its longest side, as a JPEG of at most [MAX_BYTES], lowering the quality only as far as it has to.
 */
object PhotoShrinker {
    const val MAX_SIDE = 1024
    const val MAX_BYTES = 700_000
    val QUALITIES = listOf(85, 75, 65, 55, 45)

    /** The size to scale a [width] x [height] photo to: unchanged if it already fits. */
    fun targetSize(width: Int, height: Int): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= MAX_SIDE) return width to height
        val scale = MAX_SIDE.toDouble() / longest
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }

    /** The power-of-two step to decode at, so a 12 MP photo never has to fit in memory at full size. */
    fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= MAX_SIDE) sample *= 2
        return sample
    }

    /** The first encoding, from best quality down, that fits in [MAX_BYTES]; null if even the lowest doesn't. */
    fun fit(encode: (quality: Int) -> ByteArray): ByteArray? =
        QUALITIES.asSequence().map(encode).firstOrNull { it.size <= MAX_BYTES }

    /** Reads, shrinks and encodes the photo at [uri]. Null if it can't be read as an image. */
    fun shrink(context: Context, uri: Uri): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            })
        } ?: return null
        val (width, height) = targetSize(decoded.width, decoded.height)
        val scaled = if (width == decoded.width && height == decoded.height) decoded
        else decoded.scale(width, height)

        return fit { quality ->
            ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
        }
    }
}

/** Where [CloudPhotoFetcher] gets a photo's bytes from: Firestore in the app, a fake in tests. */
fun interface PhotoSource {
    suspend fun download(id: String): ByteArray?
}

/**
 * Lets Coil show `firestore-photo://{id}`: from this phone's copy if it has one, otherwise downloaded once and
 * saved. Registered in CollectorsApp, so every `imageView.load(item.imagePath)` just works.
 */
class CloudPhotoFetcher(
    private val id: String,
    private val context: Context,
    private val source: PhotoSource,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val file = CloudPhotos.cacheFile(context, id)
        val dataSource = if (file.exists()) {
            DataSource.DISK
        } else {
            val bytes = source.download(id) ?: error("Photo $id isn't in the cloud")
            file.writeBytes(bytes)
            DataSource.NETWORK
        }
        return SourceResult(ImageSource(file.toOkioPath()), "image/jpeg", dataSource)
    }

    class Factory(private val context: Context, private val source: PhotoSource) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (data.scheme == CloudPhotos.SCHEME) CloudPhotoFetcher(data.host ?: data.schemeSpecificPart.trimStart('/'), context, source) else null
    }
}
