package com.jder.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.jder.data.SequenceDiagramRepository
import com.jder.domain.model.SequenceAnchor
import com.jder.domain.model.SequenceDiagramState
import com.jder.domain.model.SequenceToolMode
import com.jder.ui.components.ContextMenuItem
import com.jder.ui.components.ContextMenuRequest
import com.jder.ui.components.CustomIcons
import com.jder.ui.components.DiagramContextMenu
import com.jder.ui.components.DiagramSelectionMenu
import com.jder.ui.components.DiagramTool
import com.jder.ui.components.SequenceContextMenuType
import com.jder.ui.components.SequenceDiagramCanvas
import com.jder.ui.components.SequencePropertiesPanel
import com.jder.ui.components.Toasts
import com.jder.ui.components.contextMenuType
import com.jder.ui.dialogs.CreateSequenceMessageDialog
import com.jder.ui.dialogs.EditSequenceMessageDialog
import com.jder.ui.dialogs.LifelinePropertiesDialog
import com.jder.ui.dialogs.NotePropertiesDialog
import com.jder.ui.dialogs.SequenceFragmentDialog
import com.jder.ui.strings.UiStrings
import com.jder.ui.theme.ThemeState
import com.jder.ui.utils.renderSequenceDiagramToBitmap

@Composable
fun SequenceDiagramScreen(
    state: SequenceDiagramState,
    repository: SequenceDiagramRepository,
    themeState: ThemeState,
) {
    var showLifelineDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var showFragmentDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var pendingMessage by remember { mutableStateOf<PendingMessage?>(null) }
    var contextMenu by remember {
        mutableStateOf<ContextMenuRequest<SequenceContextMenuType>?>(null)
    }
    fun openEditor(type: SequenceContextMenuType) {
        when (type) {
            SequenceContextMenuType.LIFELINE -> showLifelineDialog = true
            SequenceContextMenuType.MESSAGE -> showMessageDialog = true
            SequenceContextMenuType.FRAGMENT -> showFragmentDialog = true
            SequenceContextMenuType.NOTE -> showNoteDialog = true
        }
    }
    fun selectedType(): SequenceContextMenuType? =
        when {
            state.selectedLifelineId != null -> SequenceContextMenuType.LIFELINE
            state.selectedMessageId != null -> SequenceContextMenuType.MESSAGE
            state.selectedFragmentId != null -> SequenceContextMenuType.FRAGMENT
            state.selectedNoteId != null -> SequenceContextMenuType.NOTE
            else -> null
        }
    fun actionsTitle(type: SequenceContextMenuType): String =
        when (type) {
            SequenceContextMenuType.LIFELINE -> "Azioni Linea di Vita"
            SequenceContextMenuType.MESSAGE -> "Azioni Messaggio"
            SequenceContextMenuType.FRAGMENT -> "Azioni Frammento"
            SequenceContextMenuType.NOTE -> "Azioni Nota"
        }
    fun actions(type: SequenceContextMenuType): List<ContextMenuItem> = buildList {
        add(
            ContextMenuItem(
                if (type == SequenceContextMenuType.NOTE) "Modifica Testo"
                else "Modifica Proprietà",
                Icons.Default.Edit,
            ) {
                openEditor(type)
            }
        )
        if (type == SequenceContextMenuType.MESSAGE) {
            val message = state.message(state.selectedMessageId)
            add(
                ContextMenuItem(
                    "Inverti Direzione",
                    Icons.Default.SwapHoriz,
                    enabled = message?.isSelfMessage == false,
                ) {
                    message?.let { state.reverseMessage(it.id) }
                }
            )
        }
        add(
            ContextMenuItem("Elimina", Icons.Default.Delete, isDestructive = true) {
                state.deleteSelection()?.let(Toasts::success)
            }
        )
    }
    fun lifelineTool(mode: SequenceToolMode, label: String, icon: ImageVector) =
        DiagramTool.Mode(icon, "Crea $label", label, state.toolMode == mode) {
            state.toolMode = mode
        }
    DiagramEditorScaffold(
        state = state,
        repository = repository,
        themeState = themeState,
        renderImage = { renderSequenceDiagramToBitmap(state.diagram, it) },
        emptyHint =
            "Scegli Attore, Oggetto o un altro partecipante dalla barra degli strumenti e clicca " +
                "sulla lavagna.\nPer creare un messaggio, scegli Messaggio e clicca prima sulla linea " +
                "di vita mittente e poi sulla destinataria, oppure trascina da una linea all'altra.",
        tools =
            listOf(
                lifelineTool(SequenceToolMode.ACTOR, "Attore", Icons.Default.Person),
                lifelineTool(SequenceToolMode.OBJECT, "Oggetto", CustomIcons.Lifeline),
                DiagramTool.Group(
                    "Stereotipi",
                    listOf(
                        lifelineTool(
                            SequenceToolMode.BOUNDARY,
                            "Boundary",
                            CustomIcons.BoundaryLifeline,
                        ),
                        lifelineTool(
                            SequenceToolMode.CONTROL,
                            "Control",
                            CustomIcons.ControlLifeline,
                        ),
                        lifelineTool(SequenceToolMode.ENTITY, "Entity", CustomIcons.EntityLifeline),
                    ),
                ),
                DiagramTool.Divider,
                DiagramTool.Mode(
                    Icons.Default.ArrowRightAlt,
                    "Crea Messaggio",
                    "Messaggio",
                    state.toolMode == SequenceToolMode.MESSAGE,
                ) {
                    state.toolMode = SequenceToolMode.MESSAGE
                    state.pendingMessageSource = null
                },
                DiagramTool.Mode(
                    CustomIcons.Fragment,
                    "Crea Frammento Combinato (alt, opt, loop...)",
                    "Frammento",
                    state.toolMode == SequenceToolMode.FRAGMENT,
                ) {
                    state.toolMode = SequenceToolMode.FRAGMENT
                },
                DiagramTool.Mode(
                    CustomIcons.StickyNote,
                    "Crea Nota",
                    UiStrings.NOTE,
                    state.toolMode == SequenceToolMode.NOTE,
                ) {
                    state.toolMode = SequenceToolMode.NOTE
                },
            ),
        selectionMenu = {
            selectedType()?.let { DiagramSelectionMenu(actionsTitle(it), actions(it)) }
        },
        properties = {
            SequencePropertiesPanel(
                state = state,
                onEditLifeline = { showLifelineDialog = true },
                onEditMessage = { showMessageDialog = true },
                onEditFragment = { showFragmentDialog = true },
                onEditNote = { showNoteDialog = true },
                onClose = { state.clearSelection() },
            )
        },
    ) {
        SequenceDiagramCanvas(
            state = state,
            onContextMenuRequest = { position, type ->
                contextMenu = ContextMenuRequest(position, type)
            },
            onMessageCreateRequest = { source, targetId ->
                pendingMessage = PendingMessage(source, targetId)
            },
            onEditRequest = { openEditor(it.contextMenuType) },
        )
        contextMenu?.let { menu ->
            DiagramContextMenu(
                position = menu.position,
                title = actionsTitle(menu.type),
                items = actions(menu.type),
                onDismiss = { contextMenu = null },
            )
        }
    }
    if (showLifelineDialog) {
        state.lifeline(state.selectedLifelineId)?.let { lifeline ->
            LifelinePropertiesDialog(
                lifelineId = lifeline.id,
                lifelineName = lifeline.name,
                lifelineType = lifeline.type,
                lifelineDocumentation = lifeline.documentation,
                onDismiss = { showLifelineDialog = false },
                onConfirm = { newName, newType, newDoc ->
                    state.updateLifeline(lifeline.id) {
                        it.copy(name = newName, type = newType, documentation = newDoc)
                    }
                    showLifelineDialog = false
                    Toasts.success("Linea di vita modificata")
                },
            )
        }
    }
    if (showMessageDialog) {
        state.message(state.selectedMessageId)?.let { message ->
            EditSequenceMessageDialog(
                message = message,
                onDismiss = { showMessageDialog = false },
                onConfirm = { newLabel, newType ->
                    state.updateMessage(message.id) { it.copy(label = newLabel, type = newType) }
                    showMessageDialog = false
                    Toasts.success("Messaggio modificato")
                },
            )
        }
    }
    if (showFragmentDialog) {
        state.fragment(state.selectedFragmentId)?.let { fragment ->
            SequenceFragmentDialog(
                fragment = fragment,
                onDismiss = { showFragmentDialog = false },
                onConfirm = { newType, newCondition, newOperands, newWidth, newHeight ->
                    state.updateFragment(fragment.id) {
                        it.copy(
                            type = newType,
                            condition = newCondition,
                            operands = newOperands,
                            width = newWidth,
                            height = newHeight,
                        )
                    }
                    showFragmentDialog = false
                    Toasts.success("Frammento modificato")
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
    pendingMessage?.let { request ->
        CreateSequenceMessageDialog(
            isSelfMessage = request.source.lifelineId == request.targetId,
            onDismiss = { pendingMessage = null },
            onConfirm = { label, type ->
                state.addMessage(
                    request.source.lifelineId,
                    request.targetId,
                    request.source.y,
                    label,
                    type,
                )
                Toasts.success("Messaggio creato")
                pendingMessage = null
            },
        )
    }
}

private class PendingMessage(val source: SequenceAnchor, val targetId: String)
