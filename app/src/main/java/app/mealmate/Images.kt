package app.mealmate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/** Читает фото, уменьшает до [maxSide] по длинной стороне, исправляет поворот и сжимает в JPEG. */
suspend fun loadJpeg(context: Context, uri: Uri, maxSide: Int = 1280): ByteArray = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val fail = { throw GeminiException("Не удалось прочитать фото.") }
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: fail()

        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val scale = min(1f, maxSide / max(bitmap.width, bitmap.height).toFloat())
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        ByteArrayOutputStream().also { result.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
    } catch (e: GeminiException) {
        throw e
    } catch (e: Exception) {
        fail()
    }
}
