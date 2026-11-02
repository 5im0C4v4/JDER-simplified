package com.jder.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import java.io.File
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

class ImageExporter {
    fun exportToPNG(imageBitmap: ImageBitmap, file: File): Result<Unit> =
        try {
            val data =
                Image.makeFromBitmap(imageBitmap.asSkiaBitmap()).use {
                    it.encodeToData(EncodedImageFormat.PNG)
                }
            data?.use {
                file.writeBytesAtomically(it.bytes)
                Result.success(Unit)
            } ?: Result.failure(Exception("Impossibile codificare l'immagine"))
        } catch (e: Exception) {
            Result.failure(e)
        }
}
