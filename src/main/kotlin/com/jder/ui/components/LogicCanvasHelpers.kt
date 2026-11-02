package com.jder.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.jder.domain.model.LogicComponent
import com.jder.domain.model.LogicComponentType
import com.jder.domain.model.LogicDiagram
import com.jder.domain.model.LogicDiagramState
import com.jder.domain.model.LogicEndpoint
import com.jder.domain.model.LogicPin
import com.jder.domain.model.LogicToolMode
import kotlin.math.abs

internal const val LOGIC_GATE_WIDTH = 80f
internal const val LOGIC_IO_WIDTH = 90f
internal const val LOGIC_IO_HEIGHT = 40f
internal const val LOGIC_PIN_SPACING = 20f
internal const val LOGIC_LEAD_LENGTH = 10f
private const val PIN_HIT_RADIUS = 9f
private const val WIRE_HIT_DISTANCE = 6f

sealed interface LogicElement {
    val id: String

    data class PinRef(override val id: String, val pin: LogicPin) : LogicElement

    data class ToggleRef(override val id: String) : LogicElement

    data class ComponentRef(override val id: String) : LogicElement

    data class WireRef(override val id: String) : LogicElement

    data class NoteRef(override val id: String) : LogicElement
}

internal val LogicComponent.width: Float
    get() = if (type.isGate) LOGIC_GATE_WIDTH else LOGIC_IO_WIDTH

internal val LogicComponent.height: Float
    get() = if (type.isGate) maxOf(40f, LOGIC_PIN_SPACING * inputPinCount) else LOGIC_IO_HEIGHT

internal fun LogicComponent.bounds(): Rect = Rect(x, y, x + width, y + height)

internal fun LogicComponent.inputPinPosition(index: Int): Offset =
    Offset(x, y + height / 2 + (index - (inputPinCount - 1) / 2f) * LOGIC_PIN_SPACING)

internal fun LogicComponent.outputPinPosition(): Offset = Offset(x + width, y + height / 2)

internal fun LogicComponent.toggleRect(): Rect = Rect(x + 46f, y + 8f, x + 72f, y + 32f)

internal fun LogicComponent.pinPosition(pin: LogicPin): Offset =
    when (pin) {
        LogicPin.Output -> outputPinPosition()
        is LogicPin.Input -> inputPinPosition(pin.index)
    }

internal fun logicComponentSize(type: LogicComponentType): Size =
    if (type.isGate) Size(LOGIC_GATE_WIDTH, 40f) else Size(LOGIC_IO_WIDTH, LOGIC_IO_HEIGHT)

internal fun logicPinAt(diagram: LogicDiagram, point: Offset): LogicElement.PinRef? {
    var best: LogicElement.PinRef? = null
    var bestDistance = PIN_HIT_RADIUS
    diagram.components.forEach { component ->
        if (component.type.hasOutput) {
            val distance = (component.outputPinPosition() - point).getDistance()
            if (distance <= bestDistance) {
                best = LogicElement.PinRef(component.id, LogicPin.Output)
                bestDistance = distance
            }
        }
        repeat(component.inputPinCount) { index ->
            val distance = (component.inputPinPosition(index) - point).getDistance()
            if (distance <= bestDistance) {
                best = LogicElement.PinRef(component.id, LogicPin.Input(index))
                bestDistance = distance
            }
        }
    }
    return best
}

internal fun hitTestLogicDiagram(diagram: LogicDiagram, point: Offset): LogicElement? {
    diagram.notes
        .lastOrNull { it.contains(point) }
        ?.let {
            return LogicElement.NoteRef(it.id)
        }
    logicPinAt(diagram, point)?.let {
        return it
    }
    diagram.components
        .lastOrNull { it.bounds().contains(point) }
        ?.let {
            return if (
                it.type == LogicComponentType.INPUT && it.toggleRect().inflate(2f).contains(point)
            )
                LogicElement.ToggleRef(it.id)
            else LogicElement.ComponentRef(it.id)
        }
    val routes = logicWireRoutes(diagram)
    diagram.wires
        .lastOrNull { wire ->
            routes[wire.id]
                ?.zipWithNext { start, end -> distanceToSegment(point, start, end) }
                ?.any { it <= WIRE_HIT_DISTANCE } == true
        }
        ?.let {
            return LogicElement.WireRef(it.id)
        }
    return null
}

internal fun LogicElement?.toEndpoint(): LogicEndpoint? =
    when (this) {
        is LogicElement.PinRef -> LogicEndpoint(id, pin)
        is LogicElement.ToggleRef -> LogicEndpoint(id, null)
        is LogicElement.ComponentRef -> LogicEndpoint(id, null)
        else -> null
    }

internal class ResolvedWire(val sourceId: String, val targetId: String, val targetPin: Int)

internal fun resolveLogicWire(
    diagram: LogicDiagram,
    first: LogicEndpoint,
    second: LogicEndpoint,
    point: Offset,
): ResolvedWire? {
    if (first.componentId == second.componentId) return null
    val componentsById = diagram.components.associateBy { it.id }
    fun orient(sourceEndpoint: LogicEndpoint, targetEndpoint: LogicEndpoint): ResolvedWire? {
        val source = componentsById[sourceEndpoint.componentId] ?: return null
        val target = componentsById[targetEndpoint.componentId] ?: return null
        if (!source.type.hasOutput || sourceEndpoint.pin is LogicPin.Input) return null
        if (target.inputPinCount == 0 || targetEndpoint.pin == LogicPin.Output) return null
        val pin =
            (targetEndpoint.pin as? LogicPin.Input)?.index
                ?: run {
                    val usedPins =
                        diagram.wires
                            .filter { it.targetId == target.id }
                            .map { it.targetPin }
                            .toSet()
                    val candidates =
                        (0 until target.inputPinCount)
                            .filter { it !in usedPins }
                            .ifEmpty { (0 until target.inputPinCount).toList() }
                    candidates.minByOrNull { abs(target.inputPinPosition(it).y - point.y) } ?: 0
                }
        return ResolvedWire(source.id, target.id, pin)
    }
    val firstIsSource =
        when {
            first.pin == LogicPin.Output || second.pin is LogicPin.Input -> true
            first.pin is LogicPin.Input || second.pin == LogicPin.Output -> false
            else -> componentsById[first.componentId]?.type?.hasOutput == true
        }
    return if (firstIsSource) orient(first, second) ?: orient(second, first)
    else orient(second, first) ?: orient(first, second)
}

internal fun connectLogic(
    state: LogicDiagramState,
    first: LogicEndpoint,
    second: LogicEndpoint,
    point: Offset,
) {
    val wire = resolveLogicWire(state.diagram, first, second, point)
    if (wire != null && state.addWire(wire.sourceId, wire.targetId, wire.targetPin)) {
        Toasts.success("Collegamento creato")
    } else {
        Toasts.warning("Collegamento non valido")
    }
}

internal fun selectLogicElement(state: LogicDiagramState, element: LogicElement) {
    when (element) {
        is LogicElement.WireRef -> state.selectWire(element.id)
        is LogicElement.NoteRef -> state.selectNote(element.id)
        else -> state.selectComponent(element.id)
    }
}

internal fun handleLogicTap(state: LogicDiagramState, point: Offset) {
    when (val mode = state.toolMode) {
        LogicToolMode.SELECT ->
            when (val element = hitTestLogicDiagram(state.diagram, point)) {
                is LogicElement.ToggleRef -> state.toggleInput(element.id)
                null -> state.clearSelection()
                else -> selectLogicElement(state, element)
            }
        LogicToolMode.WIRE -> {
            val endpoint = hitTestLogicDiagram(state.diagram, point).toEndpoint()
            val pending = state.pendingWireSource
            when {
                endpoint == null || pending?.componentId == endpoint.componentId ->
                    state.pendingWireSource = null
                pending == null -> state.pendingWireSource = endpoint
                else -> {
                    connectLogic(state, pending, endpoint, point)
                    state.pendingWireSource = null
                }
            }
        }
        LogicToolMode.NOTE -> {
            state.addNote(point.x - 120f, point.y - 90f, "Nuova Nota")
            state.toolMode = LogicToolMode.SELECT
        }
        else ->
            mode.componentType?.let { type ->
                val size = logicComponentSize(type)
                state.addComponent(type, point.x - size.width / 2, point.y - size.height / 2)
                state.toolMode = LogicToolMode.SELECT
            }
    }
}

internal val LogicElement.contextMenuType: LogicContextMenuType
    get() =
        when (this) {
            is LogicElement.WireRef -> LogicContextMenuType.WIRE
            is LogicElement.NoteRef -> LogicContextMenuType.NOTE
            else -> LogicContextMenuType.COMPONENT
        }
