package com.jder.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.jder.domain.model.ERDiagram
import com.jder.ui.components.DiagramPalette
import com.jder.ui.components.createDiagramTextMeasurer
import com.jder.ui.components.drawERDiagram
import com.jder.ui.components.erDiagramBounds
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val EXPORT_PADDING = 40f
private const val EXPORT_SCALE = 2f
private const val MAX_EXPORT_PIXELS = 48_000_000f
private const val MAX_EXPORT_DIMENSION = 16_000f

private val exportTextMeasurer by lazy { createDiagramTextMeasurer() }

fun renderERDiagramToBitmap(diagram: ERDiagram, palette: DiagramPalette): ImageBitmap =
    renderDiagramImage(erDiagramBounds(diagram, exportTextMeasurer), palette) {
        drawERDiagram(diagram, palette, exportTextMeasurer)
    }

private fun renderDiagramImage(
    contentBounds: Rect?,
    palette: DiagramPalette,
    draw: DrawScope.() -> Unit,
): ImageBitmap {
    val bounds = (contentBounds ?: Rect(0f, 0f, 400f, 300f)).inflate(EXPORT_PADDING)
    val scale =
        minOf(
            EXPORT_SCALE,
            sqrt(MAX_EXPORT_PIXELS / (bounds.width * bounds.height)),
            MAX_EXPORT_DIMENSION / bounds.width,
            MAX_EXPORT_DIMENSION / bounds.height,
        )
    val width = (bounds.width * scale).roundToInt().coerceAtLeast(1)
    val height = (bounds.height * scale).roundToInt().coerceAtLeast(1)
    val image = ImageBitmap(width, height)
    CanvasDrawScope().draw(
        Density(1f),
        LayoutDirection.Ltr,
        Canvas(image),
        Size(width.toFloat(), height.toFloat()),
    ) {
        drawRect(palette.background)
        withTransform({
            scale(scale, scale, Offset.Zero)
            translate(-bounds.left, -bounds.top)
        }) {
            draw()
        }
    }
    return image
}
