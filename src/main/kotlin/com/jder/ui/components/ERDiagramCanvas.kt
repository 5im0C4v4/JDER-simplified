package com.jder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.jder.domain.model.ERDiagram
import com.jder.domain.model.ERDiagramState

@Composable
fun ERDiagramCanvas(
    state: ERDiagramState,
    onContextMenuRequest: (Offset, ERContextMenuType) -> Unit,
    onEditRequest: (ERElement) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberDiagramTextMeasurer()
    val palette = MaterialTheme.colorScheme.toDiagramPalette()
    var hoveredId by remember { mutableStateOf<String?>(null) }
    val currentOnContextMenuRequest by rememberUpdatedState(onContextMenuRequest)
    val currentOnEditRequest by rememberUpdatedState(onEditRequest)
    val handler =
        remember(state) {
            object : DiagramPointerHandler {
                var dragged: ERElement? = null
                var dragStartDiagram: ERDiagram? = null
                var totalDragDelta = Offset.Zero

                override fun onTap(position: Offset) = handleERTap(state, position)

                override fun onDoubleTap(position: Offset): Boolean {
                    if (!state.isSelectTool) return false
                    val element = hitTestERDiagram(state.diagram, position) ?: return false
                    selectERElement(state, element)
                    currentOnEditRequest(element)
                    return true
                }

                override fun onContextClick(position: Offset, screenPosition: Offset) {
                    val element = hitTestERDiagram(state.diagram, position) ?: return
                    selectERElement(state, element)
                    currentOnContextMenuRequest(screenPosition, element.contextMenuType)
                }

                override fun onDragStart(position: Offset): Boolean {
                    dragged = handleERDragStart(state, position)
                    dragStartDiagram = state.diagram
                    totalDragDelta = Offset.Zero
                    return dragged != null
                }

                override fun onDrag(position: Offset, delta: Offset) {
                    dragged?.let { element ->
                        totalDragDelta += delta
                        if (state.isSnapToGridEnabled) {
                            handleERSnappedDrag(
                                state,
                                element,
                                totalDragDelta,
                                dragStartDiagram ?: state.diagram,
                            )
                        } else {
                            handleERDrag(state, element, delta)
                        }
                    }
                }

                override fun onDragEnd(position: Offset) {
                    dragged = null
                    dragStartDiagram = null
                    state.commitDragState()
                }

                override fun onHover(position: Offset?) {
                    hoveredId = position?.let { hitTestERDiagram(state.diagram, it)?.id }
                }
            }
        }
    val pointerIcon =
        when {
            !state.isSelectTool -> PointerIcon.Crosshair
            hoveredId != null -> PointerIcon.Hand
            else -> PointerIcon.Default
        }
    Canvas(
        modifier =
            modifier.fillMaxSize().diagramPointerInput(state, handler).pointerHoverIcon(pointerIcon)
    ) {
        drawCanvasBackground(state, palette)
        withViewport(state) {
            drawERDiagram(
                diagram = state.diagram,
                palette = palette,
                textMeasurer = textMeasurer,
                selectedId =
                    state.selectedEntityId ?: state.selectedRelationshipId ?: state.selectedNoteId,
                hoveredId = hoveredId,
            )
        }
    }
}
