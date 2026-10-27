package au.edu.canberra.team48.shoppinglist.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import au.edu.canberra.team48.shoppinglist.data.awaitResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Photos the household takes of their own products. A photo of Mary's own milk
 * is recognised faster than any stock photo, so a captured photo becomes that
 * item's picture from then on.
 */
object PhotoStore {
    private val cache = LruCache<String, Bitmap>(48)

    /** A file the camera app can write into, shared through our FileProvider. */
    fun newCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Copies a captured photo into app storage, upright and at most [maxPx] on its long side. */
    fun keep(context: Context, uri: Uri, name: String, maxPx: Int = 640): String? = runCatching {
        val bitmap = decodeUpright(context, uri, maxPx) ?: return null
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val out = File(dir, "$name.jpg")
        FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        cache.remove(out.absolutePath)
        out.absolutePath
    }.getOrNull()

    /** Loads a saved photo, cached, sized for a list row or grid tile. */
    fun load(path: String, maxPx: Int = 384): Bitmap? {
        cache.get(path)?.let { return it }
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        if (opts.outWidth <= 0) return null
        opts.inSampleSize = sampleSize(opts.outWidth, opts.outHeight, maxPx)
        opts.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, opts)?.also { cache.put(path, it) }
    }

    fun decodeUpright(context: Context, uri: Uri, maxPx: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxPx) }
        val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scaled = scaleDown(raw, maxPx)
        if (rotation == 0f) return scaled
        val m = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, m, true)
    }

    private fun scaleDown(b: Bitmap, maxPx: Int): Bitmap {
        val longSide = max(b.width, b.height)
        if (longSide <= maxPx) return b
        val f = maxPx.toFloat() / longSide
        return Bitmap.createScaledBitmap(b, (b.width * f).toInt(), (b.height * f).toInt(), true)
    }

    private fun sampleSize(w: Int, h: Int, maxPx: Int): Int {
        var s = 1
        while (max(w, h) / (s * 2) >= maxPx) s *= 2
        return s
    }
}

/**
 * Reads the words printed on a packet using ML Kit's on-device text recognition.
 * Nothing leaves the phone. Packets print their product name, so "MILK" or
 * "OAT MILK" is usually enough to find the item.
 */
object PhotoReader {
    suspend fun readText(context: Context, uri: Uri): String {
        val bitmap = PhotoStore.decodeUpright(context, uri, 1600) ?: return ""
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitResult().text
        } finally {
            recognizer.close()
        }
    }
}
