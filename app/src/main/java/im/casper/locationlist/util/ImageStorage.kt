// app/src/main/java/im/casper/locationlist/util/ImageStorage.kt
package im.casper.locationlist.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

object ImageStorage {
    private val counter = AtomicInteger(0)

    private fun imagesDir(context: Context): File =
        File(context.filesDir, "images").apply { mkdirs() }

    fun newImageFile(context: Context): File =
        File(
            imagesDir(context),
            "IMG_${System.currentTimeMillis()}_${counter.incrementAndGet()}.jpg",
        )

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun importImage(context: Context, source: Uri): String {
        val file = newImageFile(context)
        context.contentResolver.openInputStream(source)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file.absolutePath
    }

    fun saveBytes(context: Context, bytes: ByteArray): String {
        val file = newImageFile(context)
        file.outputStream().use { it.write(bytes) }
        return file.absolutePath
    }
}
