package com.jder.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {
    private fun outlineIcon(name: String, pathBuilder: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathBuilder = pathBuilder,
                )
            }
            .build()

    val Rectangle: ImageVector by lazy {
        outlineIcon("Rectangle") {
            moveTo(4f, 6f)
            lineTo(20f, 6f)
            lineTo(20f, 18f)
            lineTo(4f, 18f)
            close()
        }
    }

    val Diamond: ImageVector by lazy {
        outlineIcon("Diamond") {
            moveTo(12f, 4f)
            lineTo(20f, 12f)
            lineTo(12f, 20f)
            lineTo(4f, 12f)
            close()
        }
    }

    val StickyNote: ImageVector by lazy {
        outlineIcon("StickyNote") {
            moveTo(5f, 5f)
            lineTo(17f, 5f)
            lineTo(17f, 17f)
            lineTo(11f, 17f)
            lineTo(5f, 11f)
            close()
            moveTo(11f, 17f)
            lineTo(11f, 11f)
            lineTo(17f, 11f)
            moveTo(8f, 8f)
            lineTo(14f, 8f)
            moveTo(8f, 11f)
            lineTo(11f, 11f)
        }
    }
}
