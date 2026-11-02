package com.jder.domain.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

class LogicDiagramState :
    DiagramEditorState<LogicDiagram>({ LogicDiagram(name = "Nuovo Diagramma Logico") }) {
    var selectedComponentId by mutableStateOf<String?>(null)
        private set

    var selectedWireId by mutableStateOf<String?>(null)
        private set

    var selectedNoteId by mutableStateOf<String?>(null)
        private set

    var toolMode by mutableStateOf(LogicToolMode.SELECT)
    var pendingWireSource by mutableStateOf<LogicEndpoint?>(null)
    var showSimulation by mutableStateOf(true)
    var simulation by mutableStateOf(LogicSimulation.EMPTY)
        private set

    override val diagramName: String
        get() = diagram.name

    override val isEmpty: Boolean
        get() = diagram.components.isEmpty() && diagram.wires.isEmpty() && diagram.notes.isEmpty()

    override val hasSelection: Boolean
        get() = selectedComponentId != null || selectedWireId != null || selectedNoteId != null

    override val isSelectTool: Boolean
        get() = toolMode == LogicToolMode.SELECT

    override val imageKey: Any
        get() = Triple(diagram, showSimulation, simulation.values.takeIf { showSimulation })

    override fun withName(diagram: LogicDiagram, name: String) = diagram.copy(name = name)

    override fun onDiagramChanged(previous: LogicDiagram, current: LogicDiagram) {
        if (
            previous.wires != current.wires ||
                !hasSameLogic(previous.components, current.components)
        ) {
            simulation = LogicSimulator.simulate(current, simulation.values)
        }
    }

    override fun pruneSelection() {
        if (selectedComponentId != null && component(selectedComponentId) == null)
            selectedComponentId = null
        if (selectedWireId != null && diagram.wires.none { it.id == selectedWireId })
            selectedWireId = null
        if (selectedNoteId != null && diagram.notes.none { it.id == selectedNoteId })
            selectedNoteId = null
        pendingWireSource?.let { if (component(it.componentId) == null) pendingWireSource = null }
    }

    override fun resetToolMode() {
        toolMode = LogicToolMode.SELECT
        pendingWireSource = null
    }

    override fun deleteSelection(): String? {
        selectedComponentId?.let { id ->
            val message =
                when (component(id)?.type) {
                    LogicComponentType.INPUT -> "Ingresso eliminato"
                    LogicComponentType.OUTPUT -> "Uscita eliminata"
                    else -> "Porta eliminata"
                }
            deleteComponent(id)
            return message
        }
        selectedWireId?.let {
            deleteWire(it)
            return "Collegamento eliminato"
        }
        selectedNoteId?.let {
            deleteNote(it)
            return "Nota eliminata"
        }
        return null
    }

    fun component(id: String?): LogicComponent? = diagram.components.find { it.id == id }

    fun addComponent(type: LogicComponentType, x: Float, y: Float) {
        val component =
            LogicComponent(
                id = UUID.randomUUID().toString(),
                type = type,
                x = snapToLogicGrid(x),
                y = snapToLogicGrid(y),
                label = defaultLabel(type),
            )
        edit { it.copy(components = it.components + component) }
        selectComponent(component.id)
    }

    fun updateComponent(componentId: String, update: (LogicComponent) -> LogicComponent) {
        edit { d ->
            d.copy(components = d.components.map { if (it.id == componentId) update(it) else it })
                .withValidWires()
        }
    }

    fun updateComponentWithoutSave(
        componentId: String,
        update: (LogicComponent) -> LogicComponent,
    ) {
        editWithoutSave { d ->
            d.copy(components = d.components.map { if (it.id == componentId) update(it) else it })
        }
    }

    fun toggleInput(componentId: String) {
        updateComponent(componentId) {
            if (it.type == LogicComponentType.INPUT) it.copy(value = !it.value) else it
        }
    }

    fun setInputCount(componentId: String, count: Int) {
        updateComponent(componentId) {
            it.copy(inputCount = count.coerceIn(MIN_GATE_INPUTS, MAX_GATE_INPUTS))
        }
    }

    fun addWire(sourceId: String, targetId: String, targetPin: Int): Boolean {
        val source = component(sourceId) ?: return false
        val target = component(targetId) ?: return false
        if (
            sourceId == targetId ||
                !source.type.hasOutput ||
                targetPin !in 0 until target.inputPinCount ||
                diagram.wires.any {
                    it.sourceId == sourceId && it.targetId == targetId && it.targetPin == targetPin
                }
        ) {
            return false
        }
        val wire =
            LogicWire(
                id = UUID.randomUUID().toString(),
                sourceId = sourceId,
                targetId = targetId,
                targetPin = targetPin,
            )
        edit { d ->
            d.copy(
                wires =
                    d.wires.filterNot { it.targetId == targetId && it.targetPin == targetPin } +
                        wire
            )
        }
        return true
    }

    fun addNote(x: Float, y: Float, text: String) {
        val newNote = Note(id = UUID.randomUUID().toString(), text = text, x = x, y = y)
        edit { it.copy(notes = it.notes + newNote) }
        selectNote(newNote.id)
    }

    fun updateNote(noteId: String, update: (Note) -> Note) {
        edit { d -> d.copy(notes = d.notes.map { if (it.id == noteId) update(it) else it }) }
    }

    fun updateNoteWithoutSave(noteId: String, update: (Note) -> Note) {
        editWithoutSave { d ->
            d.copy(notes = d.notes.map { if (it.id == noteId) update(it) else it })
        }
    }

    fun deleteComponent(componentId: String) {
        edit { d ->
            d.copy(
                components = d.components.filter { it.id != componentId },
                wires = d.wires.filter { it.sourceId != componentId && it.targetId != componentId },
            )
        }
        pruneSelection()
    }

    fun deleteWire(wireId: String) {
        edit { d -> d.copy(wires = d.wires.filter { it.id != wireId }) }
        if (selectedWireId == wireId) selectedWireId = null
    }

    fun deleteNote(noteId: String) {
        edit { d -> d.copy(notes = d.notes.filter { it.id != noteId }) }
        if (selectedNoteId == noteId) selectedNoteId = null
    }

    fun selectComponent(componentId: String?) {
        clearSelection()
        selectedComponentId = componentId
    }

    fun selectWire(wireId: String?) {
        clearSelection()
        selectedWireId = wireId
    }

    fun selectNote(noteId: String?) {
        clearSelection()
        selectedNoteId = noteId
    }

    override fun clearSelection() {
        selectedComponentId = null
        selectedWireId = null
        selectedNoteId = null
    }

    private fun defaultLabel(type: LogicComponentType): String {
        val candidates =
            when (type) {
                LogicComponentType.INPUT ->
                    ('A'..'Z').map { "$it" } + (1..9).flatMap { n -> ('A'..'Z').map { "$it$n" } }
                LogicComponentType.OUTPUT -> listOf("Y") + (1..99).map { "Y$it" }
                else -> return ""
            }
        val used = diagram.components.map { it.label }.toSet()
        return candidates.firstOrNull { it !in used } ?: ""
    }

    private fun hasSameLogic(previous: List<LogicComponent>, current: List<LogicComponent>) =
        previous.size == current.size && previous.zip(current, ::sameLogic).all { it }

    private fun sameLogic(a: LogicComponent, b: LogicComponent) =
        a.id == b.id && a.type == b.type && a.inputPinCount == b.inputPinCount && a.value == b.value

    private fun LogicDiagram.withValidWires(): LogicDiagram {
        val byId = components.associateBy { it.id }
        return copy(
            wires =
                wires.filter { wire ->
                    val source = byId[wire.sourceId]
                    val target = byId[wire.targetId]
                    source != null &&
                        target != null &&
                        source.type.hasOutput &&
                        wire.targetPin in 0 until target.inputPinCount
                }
        )
    }
}
