package com.jder.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jder.data.LogicDiagramRepository
import com.jder.domain.model.LogicComponentType
import com.jder.domain.model.LogicDiagramState
import com.jder.domain.model.LogicToolMode
import com.jder.domain.model.MAX_GATE_INPUTS
import com.jder.domain.model.MIN_GATE_INPUTS
import com.jder.ui.components.ContextMenuItem
import com.jder.ui.components.ContextMenuRequest
import com.jder.ui.components.CustomIcons
import com.jder.ui.components.DiagramContextMenu
import com.jder.ui.components.DiagramSelectionMenu
import com.jder.ui.components.DiagramTool
import com.jder.ui.components.LogicContextMenuType
import com.jder.ui.components.LogicDiagramCanvas
import com.jder.ui.components.LogicElement
import com.jder.ui.components.LogicPropertiesPanel
import com.jder.ui.components.Toasts
import com.jder.ui.dialogs.LogicComponentDialog
import com.jder.ui.dialogs.NotePropertiesDialog
import com.jder.ui.dialogs.TruthTableDialog
import com.jder.ui.strings.UiStrings
import com.jder.ui.theme.ThemeState
import com.jder.ui.utils.renderLogicDiagramToBitmap

@Composable
fun LogicDiagramScreen(
    state: LogicDiagramState,
    repository: LogicDiagramRepository,
    themeState: ThemeState,
) {
    var showComponentDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showTruthTable by remember { mutableStateOf(false) }
    var contextMenu by remember { mutableStateOf<ContextMenuRequest<LogicContextMenuType>?>(null) }
    fun selectedType(): LogicContextMenuType? =
        when {
            state.selectedComponentId != null -> LogicContextMenuType.COMPONENT
            state.selectedWireId != null -> LogicContextMenuType.WIRE
            state.selectedNoteId != null -> LogicContextMenuType.NOTE
            else -> null
        }
    fun actionsTitle(type: LogicContextMenuType): String =
        when (type) {
            LogicContextMenuType.COMPONENT ->
                when (state.component(state.selectedComponentId)?.type) {
                    LogicComponentType.INPUT -> "Azioni Ingresso"
                    LogicComponentType.OUTPUT -> "Azioni Uscita"
                    else -> "Azioni Porta Logica"
                }
            LogicContextMenuType.WIRE -> "Azioni Collegamento"
            LogicContextMenuType.NOTE -> "Azioni Nota"
        }
    fun actions(type: LogicContextMenuType): List<ContextMenuItem> {
        val component = state.component(state.selectedComponentId)
        return buildList {
            when (type) {
                LogicContextMenuType.COMPONENT -> {
                    add(
                        ContextMenuItem("Modifica Proprietà", Icons.Default.Edit) {
                            showComponentDialog = true
                        }
                    )
                    if (component?.type == LogicComponentType.INPUT) {
                        add(
                            ContextMenuItem("Commuta Valore", Icons.Default.ToggleOn) {
                                state.toggleInput(component.id)
                            }
                        )
                    }
                    if (component != null && component.type.supportsMultipleInputs) {
                        add(
                            ContextMenuItem(
                                "Aggiungi Ingresso",
                                Icons.Default.Add,
                                enabled = component.inputPinCount < MAX_GATE_INPUTS,
                            ) {
                                state.setInputCount(component.id, component.inputPinCount + 1)
                            }
                        )
                        add(
                            ContextMenuItem(
                                "Rimuovi Ingresso",
                                Icons.Default.Remove,
                                enabled = component.inputPinCount > MIN_GATE_INPUTS,
                            ) {
                                state.setInputCount(component.id, component.inputPinCount - 1)
                            }
                        )
                    }
                }
                LogicContextMenuType.NOTE ->
                    add(
                        ContextMenuItem("Modifica Testo", Icons.Default.Edit) {
                            showNoteDialog = true
                        }
                    )
                LogicContextMenuType.WIRE -> {}
            }
            add(
                ContextMenuItem("Elimina", Icons.Default.Delete, isDestructive = true) {
                    state.deleteSelection()?.let(Toasts::success)
                }
            )
        }
    }
    DiagramEditorScaffold(
        state = state,
        repository = repository,
        themeState = themeState,
        renderImage = {
            renderLogicDiagramToBitmap(state.diagram, state.simulation, state.showSimulation, it)
        },
        emptyHint =
            "Scegli un ingresso, un'uscita o una porta logica dalla barra degli strumenti e clicca " +
                "sulla lavagna.\nPer collegare due elementi trascina da un pin all'altro.",
        tools =
            listOf(
                DiagramTool.Mode(
                    Icons.Default.ToggleOn,
                    "Ingresso (clicca sul valore per commutarlo)",
                    "Ingresso",
                    state.toolMode == LogicToolMode.INPUT,
                ) {
                    state.toolMode = LogicToolMode.INPUT
                },
                DiagramTool.Mode(
                    Icons.Default.Lightbulb,
                    "Uscita",
                    "Uscita",
                    state.toolMode == LogicToolMode.OUTPUT,
                ) {
                    state.toolMode = LogicToolMode.OUTPUT
                },
                DiagramTool.Divider,
                DiagramTool.Group(
                    "Porte Logiche",
                    LogicToolMode.entries.mapNotNull { mode ->
                        mode.componentType
                            ?.takeIf { it.isGate }
                            ?.let { type ->
                                DiagramTool.Mode(
                                    CustomIcons.gate(type),
                                    "Porta ${type.display}",
                                    type.display,
                                    state.toolMode == mode,
                                ) {
                                    state.toolMode = mode
                                }
                            }
                    },
                ),
                DiagramTool.Divider,
                DiagramTool.Mode(
                    CustomIcons.Wire,
                    "Collegamento (clicca la sorgente e poi la destinazione, oppure trascina da un pin)",
                    "Collegamento",
                    state.toolMode == LogicToolMode.WIRE,
                ) {
                    state.toolMode = LogicToolMode.WIRE
                    state.pendingWireSource = null
                },
                DiagramTool.Mode(
                    CustomIcons.StickyNote,
                    "Crea Nota",
                    UiStrings.NOTE,
                    state.toolMode == LogicToolMode.NOTE,
                ) {
                    state.toolMode = LogicToolMode.NOTE
                },
                DiagramTool.Divider,
                DiagramTool.Action(
                    Icons.Default.TableChart,
                    "Tabella di verità",
                    "Tabella di Verità...",
                ) {
                    showTruthTable = true
                },
                DiagramTool.Toggle(
                    if (state.showSimulation) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    if (state.showSimulation) "Nascondi i valori della simulazione"
                    else "Mostra i valori della simulazione",
                    "Mostra Valori della Simulazione",
                    state.showSimulation,
                ) {
                    state.showSimulation = !state.showSimulation
                },
            ),
        selectionMenu = {
            selectedType()?.let { DiagramSelectionMenu(actionsTitle(it), actions(it)) }
        },
        properties = {
            LogicPropertiesPanel(
                state = state,
                onEditComponent = { showComponentDialog = true },
                onEditNote = { showNoteDialog = true },
                onClose = { state.clearSelection() },
            )
        },
    ) {
        LogicDiagramCanvas(
            state = state,
            onContextMenuRequest = { position, type ->
                contextMenu = ContextMenuRequest(position, type)
            },
            onEditRequest = { element ->
                if (element is LogicElement.NoteRef) showNoteDialog = true
                else showComponentDialog = true
            },
        )
        if (state.showSimulation && !state.simulation.isStable) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            ) {
                Text(
                    "Il circuito oscilla: alcuni valori non sono determinabili",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        contextMenu?.let { menu ->
            DiagramContextMenu(
                position = menu.position,
                title = actionsTitle(menu.type),
                items = actions(menu.type),
                onDismiss = { contextMenu = null },
            )
        }
    }
    if (showComponentDialog) {
        state.component(state.selectedComponentId)?.let { component ->
            LogicComponentDialog(
                component = component,
                onDismiss = { showComponentDialog = false },
                onConfirm = { label, type, inputCount, value ->
                    state.updateComponent(component.id) {
                        it.copy(label = label, type = type, inputCount = inputCount, value = value)
                    }
                    showComponentDialog = false
                    Toasts.success("Componente modificato")
                },
            )
        }
    }
    if (showNoteDialog) {
        state.diagram.notes
            .find { it.id == state.selectedNoteId }
            ?.let { note ->
                NotePropertiesDialog(
                    noteId = note.id,
                    noteText = note.text,
                    onDismiss = { showNoteDialog = false },
                    onConfirm = { newText ->
                        state.updateNote(note.id) { it.copy(text = newText) }
                        showNoteDialog = false
                        Toasts.success("Nota modificata")
                    },
                )
            }
    }
    if (showTruthTable) {
        TruthTableDialog(diagram = state.diagram, onDismiss = { showTruthTable = false })
    }
}
