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
import com.jder.domain.model.LogicDiagramState
import com.jder.domain.model.LogicEndpoint
import com.jder.domain.model.LogicPin
import com.jder.domain.model.snapToLogicGrid

private class LogicWirePreview(
    val anchor: Offset,
    val cursor: Offset,
    val isFromOutput: Boolean,
    val target: LogicElement.PinRef?,
)

@Composable
fun LogicDiagramCanvas(
    state: LogicDiagramState,
    onContextMenuRequest: (Offset, LogicContextMenuType) -> Unit,
    onEditRequest: (LogicElement) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberDiagramTextMeasurer()
    val palette = MaterialTheme.colorScheme.toDiagramPalette()
    var hovered by remember { mutableStateOf<LogicElement?>(null) }
    var wirePreview by remember { mutableStateOf<LogicWirePreview?>(null) }
    val currentOnContextMenuRequest by rememberUpdatedState(onContextMenuRequest)
    val currentOnEditRequest by rememberUpdatedState(onEditRequest)
    val handler =
        remember(state) {
            object : DiagramPointerHandler {
                var movedComponentId: String? = null
                var moveOrigin = Offset.Zero
                var moveDistance = Offset.Zero
                var movedNoteId: String? = null
                var wireSource: LogicEndpoint? = null

                override fun onTap(position: Offset) = handleLogicTap(state, position)

                override fun onDoubleTap(position: Offset): Boolean {
                    if (!state.isSelectTool) return false
                    val element = hitTestLogicDiagram(state.diagram, position) ?: return false
                    if (element is LogicElement.ToggleRef || element is LogicElement.WireRef)
                        return false
                    selectLogicElement(state, element)
                    currentOnEditRequest(element)
                    return true
                }

                override fun onContextClick(position: Offset, screenPosition: Offset) {
                    val element = hitTestLogicDiagram(state.diagram, position) ?: return
                    selectLogicElement(state, element)
                    currentOnContextMenuRequest(screenPosition, element.contextMenuType)
                }

                override fun onDragStart(position: Offset): Boolean {
                    when (val element = hitTestLogicDiagram(state.diagram, position)) {
                        is LogicElement.PinRef -> {
                            val component = state.component(element.id) ?: return false
                            wireSource = LogicEndpoint(element.id, element.pin)
                            wirePreview =
                                LogicWirePreview(
                                    anchor = component.pinPosition(element.pin),
                                    cursor = position,
                                    isFromOutput = element.pin == LogicPin.Output,
                                    target = null,
                                )
                        }
                        is LogicElement.ToggleRef,
                        is LogicElement.ComponentRef -> {
                            val component = state.component(element.id) ?: return false
                            state.selectComponent(component.id)
                            state.saveDragStartState()
                            movedComponentId = component.id
                            moveOrigin = Offset(component.x, component.y)
                            moveDistance = Offset.Zero
                        }
                        is LogicElement.NoteRef -> {
                            state.selectNote(element.id)
                            state.saveDragStartState()
                            movedNoteId = element.id
                        }
                        is LogicElement.WireRef -> {
                            state.selectWire(element.id)
                            return false
                        }
                        null -> return false
                    }
                    return true
                }

                override fun onDrag(position: Offset, delta: Offset) {
                    movedComponentId?.let { id ->
                        moveDistance += delta
                        val target = moveOrigin + moveDistance
                        state.updateComponentWithoutSave(id) {
                            it.copy(x = snapToLogicGrid(target.x), y = snapToLogicGrid(target.y))
                        }
                    }
                    movedNoteId?.let { id ->
                        state.updateNoteWithoutSave(id) {
                            it.copy(x = it.x + delta.x, y = it.y + delta.y)
                        }
                    }
                    val source = wireSource ?: return
                    val candidate =
                        logicPinAt(state.diagram, position)?.takeIf {
                            it.id != source.componentId &&
                                (it.pin == LogicPin.Output) != (source.pin == LogicPin.Output)
                        }
                    wirePreview =
                        wirePreview?.let {
                            LogicWirePreview(
                                anchor = it.anchor,
                                cursor =
                                    candidate?.let { pin ->
                                        state.component(pin.id)?.pinPosition(pin.pin)
                                    } ?: position,
                                isFromOutput = it.isFromOutput,
                                target = candidate,
                            )
                        }
                }

                override fun onDragEnd(position: Offset) {
                    wireSource?.let { source ->
                        val target =
                            wirePreview?.target?.let { LogicEndpoint(it.id, it.pin) }
                                ?: hitTestLogicDiagram(state.diagram, position).toEndpoint()
                        if (target != null && target.componentId != source.componentId) {
                            connectLogic(state, source, target, position)
                        }
                    }
                    movedComponentId = null
                    movedNoteId = null
                    wireSource = null
                    wirePreview = null
                    state.commitDragState()
                }

                override fun onHover(position: Offset?) {
                    hovered = position?.let { hitTestLogicDiagram(state.diagram, it) }
                }
            }
        }
    val pointerIcon =
        when {
            !state.isSelectTool || hovered is LogicElement.PinRef -> PointerIcon.Crosshair
            hovered != null -> PointerIcon.Hand
            else -> PointerIcon.Default
        }
    Canvas(
        modifier =
            modifier.fillMaxSize().diagramPointerInput(state, handler).pointerHoverIcon(pointerIcon)
    ) {
        drawCanvasBackground(state, palette)
        withViewport(state) {
            val preview = wirePreview
            drawLogicDiagram(
                diagram = state.diagram,
                palette = palette,
                textMeasurer = textMeasurer,
                simulation = state.simulation,
                showSignals = state.showSimulation,
                selectedId =
                    state.selectedComponentId ?: state.selectedWireId ?: state.selectedNoteId,
                hoveredId = hovered?.id,
                highlightedPins =
                    listOfNotNull(hovered as? LogicElement.PinRef, preview?.target).distinct(),
                pendingSource = state.pendingWireSource,
            )
            preview?.let { drawLogicWirePreview(it.anchor, it.cursor, it.isFromOutput, palette) }
        }
    }
}
