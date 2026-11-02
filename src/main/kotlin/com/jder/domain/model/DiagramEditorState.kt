package com.jder.domain.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center

const val MIN_ZOOM = 0.25f
const val MAX_ZOOM = 3f
private const val ZOOM_STEP = 1.2f

abstract class DiagramEditorState<T>(private val createEmptyDiagram: () -> T) {
    private val diagramState = mutableStateOf(createEmptyDiagram())
    var diagram: T
        get() = diagramState.value
        private set(value) {
            val previous = diagramState.value
            diagramState.value = value
            onDiagramChanged(previous, value)
        }

    var currentFile by mutableStateOf<String?>(null)
    var isModified by mutableStateOf(false)
        private set

    var imageFile by mutableStateOf<String?>(null)
    var isAutoSaveEnabled by mutableStateOf(false)
    var isAutoExportEnabled by mutableStateOf(false)
    @Volatile var exportedImageKey: Any? = null

    var zoom by mutableStateOf(1.0f)
    var panOffset by mutableStateOf(Offset.Zero)
    var viewportSize = Size.Zero
    private val history = UndoHistory<T>()
    private var dragSnapshot: T? = null

    abstract val diagramName: String
    abstract val isEmpty: Boolean
    abstract val hasSelection: Boolean
    abstract val isSelectTool: Boolean

    open val imageKey: Any?
        get() = diagram

    protected abstract fun withName(diagram: T, name: String): T

    protected abstract fun pruneSelection()

    abstract fun clearSelection()

    abstract fun resetToolMode()

    abstract fun deleteSelection(): String?

    protected open fun onDiagramChanged(previous: T, current: T) {}

    protected fun edit(transform: (T) -> T) {
        commitDragState()
        history.record(diagram)
        diagram = transform(diagram)
        isModified = true
    }

    protected fun editWithoutSave(transform: (T) -> T) {
        val updated = transform(diagram)
        if (updated == diagram) return
        diagram = updated
        isModified = true
    }

    fun saveDragStartState() {
        dragSnapshot = diagram
    }

    fun commitDragState() {
        dragSnapshot?.let { if (it != diagram) history.record(it) }
        dragSnapshot = null
    }

    fun canUndo(): Boolean = history.canUndo

    fun canRedo(): Boolean = history.canRedo

    fun undo() {
        commitDragState()
        history.undo(diagram)?.let {
            diagram = it
            isModified = true
            pruneSelection()
        }
    }

    fun redo() {
        commitDragState()
        history.redo(diagram)?.let {
            diagram = it
            isModified = true
            pruneSelection()
        }
    }

    fun loadDiagram(newDiagram: T, filePath: String?) {
        diagram = newDiagram
        currentFile = filePath
        isModified = false
        imageFile = null
        isAutoSaveEnabled = false
        isAutoExportEnabled = false
        exportedImageKey = null
        history.clear()
        dragSnapshot = null
        clearSelection()
        resetToolMode()
        resetView()
    }

    fun newDiagram() {
        loadDiagram(createEmptyDiagram(), null)
    }

    fun markAsSaved(filePath: String) {
        currentFile = filePath
        isModified = false
    }

    fun markAsExported(filePath: String, key: Any?) {
        imageFile = filePath
        exportedImageKey = key
    }

    fun diagramNamed(name: String): T = withName(diagram, name)

    fun updateDiagramName(newName: String) {
        diagram = withName(diagram, newName)
    }

    fun zoomAround(factor: Float, focus: Offset) {
        val newZoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        panOffset = focus - (focus - panOffset) * (newZoom / zoom)
        zoom = newZoom
    }

    fun zoomIn() {
        zoomAround(ZOOM_STEP, viewportSize.center)
    }

    fun zoomOut() {
        zoomAround(1f / ZOOM_STEP, viewportSize.center)
    }

    fun resetView() {
        zoom = 1.0f
        panOffset = Offset.Zero
    }
}
