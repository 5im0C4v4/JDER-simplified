package com.jder.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.jder.domain.model.DiagramEditorState
import com.jder.domain.model.Note
import kotlin.math.floor

private const val TEXT_MEASURER_CACHE_SIZE = 256
internal const val GRID_SPACING = 20f
private const val MIN_GRID_SPACING_PX = 12f

@Composable
internal fun rememberDiagramTextMeasurer(): TextMeasurer {
    val fontFamilyResolver = LocalFontFamilyResolver.current
    return remember(fontFamilyResolver) {
        TextMeasurer(fontFamilyResolver, Density(1f), LayoutDirection.Ltr, TEXT_MEASURER_CACHE_SIZE)
    }
}

internal fun createDiagramTextMeasurer(): TextMeasurer =
    TextMeasurer(
        createFontFamilyResolver(),
        Density(1f),
        LayoutDirection.Ltr,
        TEXT_MEASURER_CACHE_SIZE,
    )

internal fun DrawScope.drawCanvasBackground(state: DiagramEditorState<*>, palette: DiagramPalette) {
    drawRect(color = palette.background, size = size)
    val scale = state.zoom * density
    var spacing = GRID_SPACING
    while (spacing * scale < MIN_GRID_SPACING_PX) spacing *= 2f
    val left = -state.panOffset.x / scale
    val top = -state.panOffset.y / scale
    val right = left + size.width / scale
    val bottom = top + size.height / scale
    withViewport(state) {
        val strokeWidth = 1f / scale
        var x = floor(left / spacing) * spacing
        while (x <= right) {
            drawLine(palette.grid, Offset(x, top), Offset(x, bottom), strokeWidth)
            x += spacing
        }
        var y = floor(top / spacing) * spacing
        while (y <= bottom) {
            drawLine(palette.grid, Offset(left, y), Offset(right, y), strokeWidth)
            y += spacing
        }
    }
}

internal inline fun DrawScope.withViewport(
    state: DiagramEditorState<*>,
    block: DrawScope.() -> Unit,
) {
    val scale = state.zoom * density
    withTransform(
        {
            translate(state.panOffset.x, state.panOffset.y)
            scale(scale, scale, Offset.Zero)
        },
        block,
    )
}

internal fun TextMeasurer.measureLine(
    text: String,
    style: TextStyle,
    maxWidth: Float,
): TextLayoutResult =
    measure(
        text = text,
        style = style,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
        maxLines = 1,
        constraints = Constraints(maxWidth = maxWidth.toInt().coerceAtLeast(1)),
    )

internal fun TextMeasurer.measureBlock(
    text: String,
    style: TextStyle,
    maxWidth: Float,
    maxLines: Int,
): TextLayoutResult =
    measure(
        text = text,
        style = style.copy(textAlign = TextAlign.Center),
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        constraints = Constraints(maxWidth = maxWidth.toInt().coerceAtLeast(1)),
    )

internal fun DrawScope.drawTextCentered(layout: TextLayoutResult, center: Offset) {
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
    )
}

internal fun Note.contains(point: Offset): Boolean =
    point.x >= x && point.x <= x + width && point.y >= y && point.y <= y + height

internal fun Note.bounds(): Rect = Rect(x, y, x + width, y + height)

internal fun DrawScope.drawNoteShape(
    note: Note,
    isSelected: Boolean,
    textMeasurer: TextMeasurer,
    selectedColor: Color,
    isHovered: Boolean = false,
) {
    val noteColor = Color(0xFFFFEB3B)
    val strokeColor = if (isSelected) selectedColor else Color(0xFFFBC02D)
    val strokeWidth =
        when {
            isSelected -> 3f
            isHovered -> 2.5f
            else -> 1.5f
        }
    val foldSize = 15f
    val right = note.x + note.width
    val bottom = note.y + note.height
    val body =
        Path().apply {
            moveTo(note.x, note.y)
            lineTo(right - foldSize, note.y)
            lineTo(right, note.y + foldSize)
            lineTo(right, bottom)
            lineTo(note.x, bottom)
            close()
        }
    drawPath(path = body, color = noteColor, style = Fill)
    drawPath(path = body, color = strokeColor, style = Stroke(width = strokeWidth))
    val foldPath =
        Path().apply {
            moveTo(right - foldSize, note.y)
            lineTo(right - foldSize, note.y + foldSize)
            lineTo(right, note.y + foldSize)
            close()
        }
    drawPath(path = foldPath, color = Color(0xFFF9A825), style = Fill)
    drawPath(path = foldPath, color = strokeColor, style = Stroke(width = 1f))
    val padding = 10f
    val textResult =
        textMeasurer.measure(
            text = note.text,
            style = TextStyle(color = Color.Black, fontSize = 12.sp),
            constraints =
                Constraints(
                    maxWidth = (note.width - padding * 2).toInt().coerceAtLeast(1),
                    maxHeight = (note.height - padding * 2).toInt().coerceAtLeast(1),
                ),
        )
    drawText(textLayoutResult = textResult, topLeft = Offset(note.x + padding, note.y + padding))
}

internal fun DrawScope.drawArrowHead(
    tip: Offset,
    direction: Offset,
    color: Color,
    hollowFill: Color?,
    size: Float = 12f,
) {
    val perpX = -direction.y
    val perpY = direction.x
    val base1 =
        Offset(
            tip.x - direction.x * size + perpX * (size * 0.45f),
            tip.y - direction.y * size + perpY * (size * 0.45f),
        )
    val base2 =
        Offset(
            tip.x - direction.x * size - perpX * (size * 0.45f),
            tip.y - direction.y * size - perpY * (size * 0.45f),
        )
    val path =
        Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(base1.x, base1.y)
            lineTo(base2.x, base2.y)
            close()
        }
    if (hollowFill == null) {
        drawPath(path = path, color = color, style = Fill)
    } else {
        drawPath(path = path, color = hollowFill, style = Fill)
        drawPath(path = path, color = color, style = Stroke(width = 2f))
    }
}

internal fun DrawScope.drawOpenArrowHead(tip: Offset, dir: Offset, color: Color, size: Float) {
    val perpX = -dir.y
    val perpY = dir.x
    val base = Offset(tip.x - dir.x * size, tip.y - dir.y * size)
    val left = Offset(base.x + perpX * size * 0.4f, base.y + perpY * size * 0.4f)
    val right = Offset(base.x - perpX * size * 0.4f, base.y - perpY * size * 0.4f)
    drawLine(color = color, start = tip, end = left, strokeWidth = 1.5f)
    drawLine(color = color, start = tip, end = right, strokeWidth = 1.5f)
}

internal class BoundsBuilder {
    private var left = Float.POSITIVE_INFINITY
    private var top = Float.POSITIVE_INFINITY
    private var right = Float.NEGATIVE_INFINITY
    private var bottom = Float.NEGATIVE_INFINITY

    fun include(rect: Rect) {
        left = minOf(left, rect.left)
        top = minOf(top, rect.top)
        right = maxOf(right, rect.right)
        bottom = maxOf(bottom, rect.bottom)
    }

    fun include(point: Offset) {
        include(Rect(point, Size.Zero))
    }

    fun build(): Rect? = if (left > right || top > bottom) null else Rect(left, top, right, bottom)
}
