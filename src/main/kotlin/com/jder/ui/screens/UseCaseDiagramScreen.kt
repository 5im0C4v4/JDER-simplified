package com.jder.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jder.data.UseCaseDiagramRepository
import com.jder.domain.model.UseCaseDiagramState
import com.jder.domain.model.UseCaseToolMode
import com.jder.ui.components.ContextMenuItem
import com.jder.ui.components.ContextMenuRequest
import com.jder.ui.components.CustomIcons
import com.jder.ui.components.DiagramContextMenu
import com.jder.ui.components.DiagramSelectionMenu
import com.jder.ui.components.DiagramTool
import com.jder.ui.components.PendingRelation
import com.jder.ui.components.Toasts
import com.jder.ui.components.UseCaseContextMenuType
import com.jder.ui.components.UseCaseDiagramCanvas
import com.jder.ui.components.UseCasePropertiesPanel
import com.jder.ui.components.contextMenuType
import com.jder.ui.dialogs.ActorPropertiesDialog
import com.jder.ui.dialogs.NotePropertiesDialog
import com.jder.ui.dialogs.RelationEditDialog
import com.jder.ui.dialogs.SelectRelationTypeDialog
import com.jder.ui.dialogs.SystemBoundaryDialog
import com.jder.ui.dialogs.UseCasePropertiesDialog
import com.jder.ui.strings.UiStrings
import com.jder.ui.theme.ThemeState
import com.jder.ui.utils.renderUseCaseDiagramToBitmap

@Composable
fun UseCaseDiagramScreen(
    state: UseCaseDiagramState,
    repository: UseCaseDiagramRepository,
    themeState: ThemeState,
) {
    var showActorDialog by remember { mutableStateOf(false) }
    var showUseCaseDialog by remember { mutableStateOf(false) }
    var showRelationDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showSystemBoundaryDialog by remember { mutableStateOf(false) }
    var pendingRelation by remember { mutableStateOf<PendingRelation?>(null) }
    var contextMenu by remember {
        mutableStateOf<ContextMenuRequest<UseCaseContextMenuType>?>(null)
    }
    fun openEditor(type: UseCaseContextMenuType) {
        when (type) {
            UseCaseContextMenuType.ACTOR -> showActorDialog = true
            UseCaseContextMenuType.USE_CASE -> showUseCaseDialog = true
            UseCaseContextMenuType.RELATION -> showRelationDialog = true
            UseCaseContextMenuType.NOTE -> showNoteDialog = true
            UseCaseContextMenuType.SYSTEM -> showSystemBoundaryDialog = true
        }
    }
    fun selectedType(): UseCaseContextMenuType? =
        when {
            state.selectedActorId != null -> UseCaseContextMenuType.ACTOR
            state.selectedUseCaseId != null -> UseCaseContextMenuType.USE_CASE
            state.selectedRelationId != null -> UseCaseContextMenuType.RELATION
            state.selectedNoteId != null -> UseCaseContextMenuType.NOTE
            state.selectedSystemBoundaryId != null -> UseCaseContextMenuType.SYSTEM
            else -> null
        }
    fun actionsTitle(type: UseCaseContextMenuType): String =
        when (type) {
            UseCaseContextMenuType.ACTOR -> "Azioni Attore"
            UseCaseContextMenuType.USE_CASE -> "Azioni Caso d'Uso"
            UseCaseContextMenuType.RELATION -> "Azioni Relazione"
            UseCaseContextMenuType.NOTE -> "Azioni Nota"
            UseCaseContextMenuType.SYSTEM -> "Azioni Sistema"
        }
    fun actions(type: UseCaseContextMenuType): List<ContextMenuItem> =
        listOf(
            ContextMenuItem(
                when (type) {
                    UseCaseContextMenuType.ACTOR -> "Modifica Nome"
                    UseCaseContextMenuType.RELATION -> "Modifica Tipo"
                    UseCaseContextMenuType.NOTE -> "Modifica Testo"
                    else -> "Modifica Proprietà"
                },
                Icons.Default.Edit,
            ) {
                openEditor(type)
            },
            ContextMenuItem("Elimina", Icons.Default.Delete, isDestructive = true) {
                state.deleteSelection()?.let(Toasts::success)
            },
        )
    DiagramEditorScaffold(
        state = state,
        repository = repository,
        themeState = themeState,
        renderImage = { renderUseCaseDiagramToBitmap(state.diagram, it) },
        emptyHint =
            "Scegli Attore, Caso d'Uso, Sistema o Nota dalla barra degli strumenti e clicca sulla lavagna",
        tools =
            listOf(
                DiagramTool.Mode(
                    Icons.Default.Person,
                    "Crea Attore",
                    "Attore",
                    state.toolMode == UseCaseToolMode.ACTOR,
                ) {
                    state.toolMode = UseCaseToolMode.ACTOR
                },
                DiagramTool.Mode(
                    CustomIcons.Ellipse,
                    "Crea Caso d'Uso",
                    "Caso d'Uso",
                    state.toolMode == UseCaseToolMode.USE_CASE,
                ) {
                    state.toolMode = UseCaseToolMode.USE_CASE
                },
                DiagramTool.Mode(
                    Icons.Default.ArrowRightAlt,
                    "Crea Relazione (clicca l'elemento sorgente e poi la destinazione)",
                    UiStrings.RELATION,
                    state.toolMode == UseCaseToolMode.RELATION,
                ) {
                    state.toolMode = UseCaseToolMode.RELATION
                    state.pendingRelationSourceId = null
                },
                DiagramTool.Mode(
                    CustomIcons.StickyNote,
                    "Crea Nota",
                    UiStrings.NOTE,
                    state.toolMode == UseCaseToolMode.NOTE,
                ) {
                    state.toolMode = UseCaseToolMode.NOTE
                },
                DiagramTool.Mode(
                    Icons.Default.CropSquare,
                    "Crea Sistema",
                    "Sistema",
                    state.toolMode == UseCaseToolMode.SYSTEM,
                ) {
                    state.toolMode = UseCaseToolMode.SYSTEM
                },
            ),
        selectionMenu = {
            selectedType()?.let { DiagramSelectionMenu(actionsTitle(it), actions(it)) }
        },
        properties = {
            UseCasePropertiesPanel(
                state = state,
                onEditActor = { showActorDialog = true },
                onEditUseCase = { showUseCaseDialog = true },
                onEditRelation = { showRelationDialog = true },
                onEditNote = { showNoteDialog = true },
                onEditSystemBoundary = { showSystemBoundaryDialog = true },
                onClose = { state.clearSelection() },
            )
        },
    ) {
        UseCaseDiagramCanvas(
            state = state,
            onContextMenuRequest = { position, type ->
                contextMenu = ContextMenuRequest(position, type)
            },
            onRelationCreateRequest = { sourceId, targetId ->
                pendingRelation = PendingRelation(sourceId, targetId)
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
    if (showActorDialog) {
        state.diagram.actors
            .find { it.id == state.selectedActorId }
            ?.let { actor ->
                ActorPropertiesDialog(
                    actorId = actor.id,
                    actorName = actor.name,
                    onDismiss = { showActorDialog = false },
                    onConfirm = { newName ->
                        state.updateActor(actor.id) { it.copy(name = newName) }
                        showActorDialog = false
                        Toasts.success("Attore modificato")
                    },
                )
            }
    }
    if (showUseCaseDialog) {
        state.diagram.useCases
            .find { it.id == state.selectedUseCaseId }
            ?.let { useCase ->
                UseCasePropertiesDialog(
                    useCaseId = useCase.id,
                    useCaseName = useCase.name,
                    useCaseDocumentation = useCase.documentation,
                    onDismiss = { showUseCaseDialog = false },
                    onConfirm = { newName, newDoc ->
                        state.updateUseCase(useCase.id) {
                            it.copy(name = newName, documentation = newDoc)
                        }
                        showUseCaseDialog = false
                        Toasts.success("Caso d'uso modificato")
                    },
                )
            }
    }
    if (showRelationDialog) {
        state.diagram.relations
            .find { it.id == state.selectedRelationId }
            ?.let { relation ->
                RelationEditDialog(
                    relation = relation,
                    onDismiss = { showRelationDialog = false },
                    onConfirm = { newType ->
                        state.updateRelation(relation.id) { it.copy(type = newType) }
                        showRelationDialog = false
                        Toasts.success("Relazione modificata")
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
    if (showSystemBoundaryDialog) {
        state.diagram.systemBoundaries
            .find { it.id == state.selectedSystemBoundaryId }
            ?.let { boundary ->
                SystemBoundaryDialog(
                    boundaryId = boundary.id,
                    boundaryName = boundary.name,
                    boundaryWidth = boundary.width,
                    boundaryHeight = boundary.height,
                    onDismiss = { showSystemBoundaryDialog = false },
                    onConfirm = { newName, newWidth, newHeight ->
                        state.updateSystemBoundary(boundary.id) {
                            it.copy(name = newName, width = newWidth, height = newHeight)
                        }
                        showSystemBoundaryDialog = false
                        Toasts.success("Sistema modificato")
                    },
                )
            }
    }
    pendingRelation?.let { relation ->
        SelectRelationTypeDialog(
            onDismiss = { pendingRelation = null },
            onConfirm = { type ->
                state.addRelation(relation.sourceId, relation.targetId, type)
                Toasts.success("Relazione creata")
                pendingRelation = null
            },
        )
    }
}
