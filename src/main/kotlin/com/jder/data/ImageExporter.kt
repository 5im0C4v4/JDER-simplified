package com.jder.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import java.io.File
import java.nio.file.Files
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

class ImageExporter {
    fun exportToPNG(imageBitmap: ImageBitmap, file: File): Result<Unit> =
        try {
            val skiaBitmap = imageBitmap.asSkiaBitmap()
            val image = Image.makeFromBitmap(skiaBitmap)
            val data = image.encodeToData(EncodedImageFormat.PNG)
            data?.let {
                val target = file.toPath()
                Files.createDirectories(target.toAbsolutePath().parent)
                Files.write(target, it.bytes)
                Result.success(Unit)
            } ?: Result.failure(Exception("Impossibile codificare l'immagine"))
        } catch (e: Exception) {
            Result.failure(e)
        }
}
