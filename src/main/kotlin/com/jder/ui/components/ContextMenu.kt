package com.jder.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ERContextMenuType {
    ENTITY,
    RELATIONSHIP,
    NOTE,
}

class ContextMenuRequest<T>(val position: Offset, val type: T)

class ContextMenuItem(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun DiagramContextMenu(
    position: Offset,
    title: String,
    items: List<ContextMenuItem>,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val positionProvider =
        remember(position) {
            CursorPositionProvider(IntOffset(position.x.toInt(), position.y.toInt()))
        }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = Modifier.width(230.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Divider()
                items.forEachIndexed { index, item ->
                    if (item.isDestructive && index > 0) Divider()
                    val contentColor =
                        when {
                            !item.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            item.isDestructive -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    item.icon,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = contentColor,
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(item.label, color = contentColor)
                            }
                        },
                        onClick = {
                            coroutineScope.launch {
                                delay(150.milliseconds)
                                onDismiss()
                                item.onClick()
                            }
                        },
                        enabled = item.enabled,
                    )
                }
            }
        }
    }
}

private class CursorPositionProvider(private val offset: IntOffset) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.left + offset.x
        var y = anchorBounds.top + offset.y
        if (x + popupContentSize.width > windowSize.width) x -= popupContentSize.width
        if (y + popupContentSize.height > windowSize.height) y -= popupContentSize.height
        return IntOffset(x.coerceAtLeast(0), y.coerceAtLeast(0))
    }
}
