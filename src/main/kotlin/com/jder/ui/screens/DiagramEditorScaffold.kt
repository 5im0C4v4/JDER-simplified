package com.jder.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jder.data.DiagramFileRepository
import com.jder.data.ImageExporter
import com.jder.domain.model.DiagramEditorState
import com.jder.ui.components.ConfirmNewDiagramDialog
import com.jder.ui.components.ConfirmOpenDiagramDialog
import com.jder.ui.components.DiagramMenuCommands
import com.jder.ui.components.DiagramPalette
import com.jder.ui.components.DiagramSelectionMenu
import com.jder.ui.components.DiagramSnackbarHost
import com.jder.ui.components.DiagramTool
import com.jder.ui.components.DiagramToolbar
import com.jder.ui.components.FileManagerDialog
import com.jder.ui.components.FileManagerMode
import com.jder.ui.components.LocalDiagramMenuHost
import com.jder.ui.components.SnackbarMessenger
import com.jder.ui.components.toExportPalette
import com.jder.ui.theme.ThemeState
import java.io.File

private val zoomInKeys = setOf(Key.Plus, Key.Equals, Key.NumPadAdd)
private val zoomOutKeys = setOf(Key.Minus, Key.NumPadSubtract)
private val resetZoomKeys = setOf(Key.Zero, Key.NumPad0)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> DiagramEditorScaffold(
    state: DiagramEditorState<T>,
    repository: DiagramFileRepository<T>,
    themeState: ThemeState,
    messenger: SnackbarMessenger,
    previewFile: File? = null,
    renderImage: (DiagramPalette) -> ImageBitmap,
    emptyHint: String,
    tools: List<DiagramTool>,
    selectionMenu: () -> DiagramSelectionMenu?,
    properties: @Composable () -> Unit,
    canvas: @Composable BoxScope.() -> Unit,
) {
    var showNewDiagramConfirmDialog by remember { mutableStateOf(false) }
    var showOpenDiagramConfirmDialog by remember { mutableStateOf(false) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    fun saveTo(file: File) {
        val name = file.nameWithoutExtension
        repository
            .saveDiagram(state.diagramNamed(name), file)
            .fold(
                onSuccess = {
                    state.updateDiagramName(name)
                    state.markAsSaved(file.absolutePath)
                    if (previewFile == null) {
                        messenger.show("Diagramma salvato: ${file.name}")
                    } else {
                        try {
                            ImageExporter()
                                .exportToPNG(
                                    renderImage(
                                        themeState.selectedPalette.lightScheme.toExportPalette()
                                    ),
                                    previewFile,
                                )
                                .fold(
                                    onSuccess = {
                                        messenger.show(
                                            "Diagramma salvato e anteprima aggiornata: ${previewFile.name}"
                                        )
                                    },
                                    onFailure = {
                                        messenger.show(
                                            "Diagramma salvato, ma errore nell'esportazione dell'anteprima: ${it.message}"
                                        )
                                    },
                                )
                        } catch (e: Exception) {
                            messenger.show(
                                "Diagramma salvato, ma errore nell'esportazione dell'anteprima: ${e.message}"
                            )
                        }
                    }
                },
                onFailure = { messenger.show("Errore nel salvataggio: ${it.message}") },
            )
    }
    fun saveDiagram() {
        state.currentFile?.let { saveTo(File(it)) } ?: run { showSaveAsDialog = true }
    }
    fun requestOpenDiagram() {
        if (state.isModified) showOpenDiagramConfirmDialog = true else showOpenDialog = true
    }
    fun requestNewDiagram() {
        if (state.isModified) showNewDiagramConfirmDialog = true
        else {
            state.newDiagram()
            messenger.show("Nuovo diagramma creato")
        }
    }
    fun requestExport() {
        if (state.isEmpty) messenger.show("Il diagramma è vuoto: non c'è nulla da esportare")
        else showExportDialog = true
    }
    fun undo() {
        if (state.canUndo()) {
            state.undo()
            messenger.show("Azione annullata")
        }
    }
    fun redo() {
        if (state.canRedo()) {
            state.redo()
            messenger.show("Azione ripristinata")
        }
    }
    fun deleteSelected() {
        state.deleteSelection()?.let { messenger.show(it) }
    }
    val menuHost = LocalDiagramMenuHost.current
    val currentTools by rememberUpdatedState(tools)
    val currentSelectionMenu by rememberUpdatedState(selectionMenu)
    if (menuHost != null) {
        DisposableEffect(menuHost) {
            val commands =
                DiagramMenuCommands(
                    state = state,
                    onNewDiagram = { requestNewDiagram() },
                    onOpenDiagram = { requestOpenDiagram() },
                    onSaveDiagram = { saveDiagram() },
                    onSaveAsDiagram = { showSaveAsDialog = true },
                    onExportPNG = { requestExport() },
                    onUndo = { undo() },
                    onRedo = { redo() },
                    onDeleteSelected = { deleteSelected() },
                    tools = { currentTools },
                    selectionMenu = { currentSelectionMenu() },
                )
            menuHost.commands = commands
            onDispose { if (menuHost.commands === commands) menuHost.commands = null }
        }
    }
    Scaffold(
        snackbarHost = { DiagramSnackbarHost(messenger.hostState) },
        modifier =
            Modifier.focusRequester(focusRequester).focusTarget().onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val isCommand = event.isCtrlPressed || event.isMetaPressed
                when {
                    isCommand && event.key == Key.Z -> {
                        if (event.isShiftPressed) redo() else undo()
                        true
                    }
                    isCommand && event.key == Key.Y -> {
                        redo()
                        true
                    }
                    isCommand && event.key == Key.N -> {
                        requestNewDiagram()
                        true
                    }
                    isCommand && event.key == Key.O -> {
                        requestOpenDiagram()
                        true
                    }
                    isCommand && event.key == Key.S -> {
                        if (event.isShiftPressed) showSaveAsDialog = true else saveDiagram()
                        true
                    }
                    isCommand && event.key == Key.E -> {
                        requestExport()
                        true
                    }
                    isCommand && event.key in zoomInKeys -> {
                        state.zoomIn()
                        true
                    }
                    isCommand && event.key in zoomOutKeys -> {
                        state.zoomOut()
                        true
                    }
                    isCommand && event.key in resetZoomKeys -> {
                        state.resetView()
                        true
                    }
                    event.key == Key.Delete || event.key == Key.Backspace -> {
                        deleteSelected()
                        true
                    }
                    event.key == Key.Escape -> {
                        state.clearSelection()
                        state.resetToolMode()
                        true
                    }
                    else -> false
                }
            },
        topBar = {
            DiagramToolbar(
                state = state,
                themeState = themeState,
                onNewDiagram = ::requestNewDiagram,
                onOpenDiagram = ::requestOpenDiagram,
                onSaveDiagram = ::saveDiagram,
                onSaveAsDiagram = { showSaveAsDialog = true },
                onExportPNG = ::requestExport,
                onUndo = ::undo,
                onRedo = ::redo,
                onDeleteSelected = ::deleteSelected,
                tools = tools,
            )
        },
    ) { values ->
        Row(modifier = Modifier.fillMaxSize().padding(values)) {
            Box(
                modifier =
                    Modifier.weight(1f).fillMaxHeight().onPointerEvent(
                        PointerEventType.Press,
                        PointerEventPass.Initial,
                    ) {
                        focusRequester.requestFocus()
                    }
            ) {
                canvas()
                if (state.isEmpty) {
                    Text(
                        text = emptyHint,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )
                }
            }
            if (state.hasSelection) {
                Surface(modifier = Modifier.width(300.dp).fillMaxHeight(), tonalElevation = 2.dp) {
                    properties()
                }
            }
        }
    }
    if (showNewDiagramConfirmDialog) {
        ConfirmNewDiagramDialog(
            onDismiss = { showNewDiagramConfirmDialog = false },
            onConfirm = {
                showNewDiagramConfirmDialog = false
                state.newDiagram()
                messenger.show("Nuovo diagramma creato")
            },
        )
    }
    if (showOpenDiagramConfirmDialog) {
        ConfirmOpenDiagramDialog(
            onDismiss = { showOpenDiagramConfirmDialog = false },
            onConfirm = {
                showOpenDiagramConfirmDialog = false
                showOpenDialog = true
            },
        )
    }
    if (showOpenDialog) {
        FileManagerDialog(
            mode = FileManagerMode.OPEN,
            fileExtension = ".json",
            title = "Apri Diagramma",
            onDismiss = { showOpenDialog = false },
            onFileSelected = { file ->
                showOpenDialog = false
                repository
                    .loadDiagram(file)
                    .fold(
                        onSuccess = {
                            state.loadDiagram(it, file.absolutePath)
                            messenger.show("Diagramma caricato: ${file.name}")
                        },
                        onFailure = { messenger.show("Errore nel caricamento: ${it.message}") },
                    )
            },
        )
    }
    if (showSaveAsDialog) {
        FileManagerDialog(
            mode = FileManagerMode.SAVE,
            fileExtension = ".json",
            title = "Salva Diagramma",
            defaultFileName = state.diagramName,
            onDismiss = { showSaveAsDialog = false },
            onFileSelected = { file ->
                showSaveAsDialog = false
                saveTo(file)
            },
        )
    }
    if (showExportDialog) {
        FileManagerDialog(
            mode = FileManagerMode.SAVE,
            fileExtension = ".png",
            title = "Esporta come PNG",
            defaultFileName = state.diagramName,
            onDismiss = { showExportDialog = false },
            onFileSelected = { file ->
                showExportDialog = false
                try {
                    ImageExporter()
                        .exportToPNG(
                            renderImage(themeState.selectedPalette.lightScheme.toExportPalette()),
                            file,
                        )
                        .fold(
                            onSuccess = { messenger.show("Immagine salvata: ${file.name}") },
                            onFailure = {
                                messenger.show("Errore nell'esportazione: ${it.message}")
                            },
                        )
                } catch (e: Exception) {
                    messenger.show("Errore nell'esportazione: ${e.message}")
                }
            },
        )
    }
}
