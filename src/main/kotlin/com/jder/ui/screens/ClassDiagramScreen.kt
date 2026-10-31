package com.jder.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jder.data.ClassDiagramRepository
import com.jder.domain.model.ClassDiagramState
import com.jder.domain.model.ClassDiagramToolMode
import com.jder.ui.components.ClassDiagramCanvas
import com.jder.ui.components.ClassDiagramContextMenuType
import com.jder.ui.components.ClassDiagramPropertiesPanel
import com.jder.ui.components.ContextMenuItem
import com.jder.ui.components.ContextMenuRequest
import com.jder.ui.components.CustomIcons
import com.jder.ui.components.DiagramContextMenu
import com.jder.ui.components.DiagramSelectionMenu
import com.jder.ui.components.DiagramTool
import com.jder.ui.components.PendingRelation
import com.jder.ui.components.Toasts
import com.jder.ui.components.contextMenuType
import com.jder.ui.dialogs.ClassEntityDialog
import com.jder.ui.dialogs.ClassMemberDialog
import com.jder.ui.dialogs.ClassRelationDialog
import com.jder.ui.dialogs.NotePropertiesDialog
import com.jder.ui.dialogs.SelectClassRelationTypeDialog
import com.jder.ui.strings.UiStrings
import com.jder.ui.theme.ThemeState
import com.jder.ui.utils.renderClassDiagramToBitmap

@Composable
fun ClassDiagramScreen(
    state: ClassDiagramState,
    repository: ClassDiagramRepository,
    themeState: ThemeState,
) {
    var showClassDialog by remember { mutableStateOf(false) }
    var showRelationDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showAddAttributeDialog by remember { mutableStateOf(false) }
    var showAddMethodDialog by remember { mutableStateOf(false) }
    var pendingRelation by remember { mutableStateOf<PendingRelation?>(null) }
    var contextMenu by remember {
        mutableStateOf<ContextMenuRequest<ClassDiagramContextMenuType>?>(null)
    }
    fun openEditor(type: ClassDiagramContextMenuType) {
        when (type) {
            ClassDiagramContextMenuType.CLASS -> showClassDialog = true
            ClassDiagramContextMenuType.RELATION -> showRelationDialog = true
            ClassDiagramContextMenuType.NOTE -> showNoteDialog = true
        }
    }
    fun selectedType(): ClassDiagramContextMenuType? =
        when {
            state.selectedClassId != null -> ClassDiagramContextMenuType.CLASS
            state.selectedRelationId != null -> ClassDiagramContextMenuType.RELATION
            state.selectedNoteId != null -> ClassDiagramContextMenuType.NOTE
            else -> null
        }
    fun actionsTitle(type: ClassDiagramContextMenuType): String =
        when (type) {
            ClassDiagramContextMenuType.CLASS -> "Azioni Classe"
            ClassDiagramContextMenuType.RELATION -> "Azioni Relazione"
            ClassDiagramContextMenuType.NOTE -> "Azioni Nota"
        }
    fun actions(type: ClassDiagramContextMenuType): List<ContextMenuItem> = buildList {
        add(
            ContextMenuItem(
                when (type) {
                    ClassDiagramContextMenuType.CLASS -> "Modifica Proprietà"
                    ClassDiagramContextMenuType.RELATION -> "Modifica Relazione"
                    ClassDiagramContextMenuType.NOTE -> "Modifica Testo"
                },
                Icons.Default.Edit,
            ) {
                openEditor(type)
            }
        )
        if (type == ClassDiagramContextMenuType.CLASS) {
            add(
                ContextMenuItem("Aggiungi Attributo", Icons.Default.Add) {
                    showAddAttributeDialog = true
                }
            )
            add(
                ContextMenuItem("Aggiungi Metodo", Icons.Default.Add) { showAddMethodDialog = true }
            )
        }
        add(
            ContextMenuItem("Elimina", Icons.Default.Delete, isDestructive = true) {
                state.deleteSelection()?.let(Toasts::success)
            }
        )
    }
    DiagramEditorScaffold(
        state = state,
        repository = repository,
        themeState = themeState,
        renderImage = { renderClassDiagramToBitmap(state.diagram, it) },
        emptyHint =
            "Scegli Classe, Interfaccia, Enum o Nota dalla barra degli strumenti e clicca sulla lavagna",
        tools =
            listOf(
                DiagramTool.Mode(
                    CustomIcons.Rectangle,
                    "Crea Classe",
                    "Classe",
                    state.toolMode == ClassDiagramToolMode.CLASS,
                ) {
                    state.toolMode = ClassDiagramToolMode.CLASS
                },
                DiagramTool.Mode(
                    CustomIcons.Interface,
                    "Crea Interfaccia",
                    "Interfaccia",
                    state.toolMode == ClassDiagramToolMode.INTERFACE,
                ) {
                    state.toolMode = ClassDiagramToolMode.INTERFACE
                },
                DiagramTool.Mode(
                    Icons.Default.FormatListNumbered,
                    "Crea Enum",
                    "Enum",
                    state.toolMode == ClassDiagramToolMode.ENUM,
                ) {
                    state.toolMode = ClassDiagramToolMode.ENUM
                },
                DiagramTool.Mode(
                    Icons.Default.ArrowRightAlt,
                    "Crea Relazione (clicca la classe sorgente e poi la destinazione)",
                    UiStrings.RELATION,
                    state.toolMode == ClassDiagramToolMode.RELATION,
                ) {
                    state.toolMode = ClassDiagramToolMode.RELATION
                    state.pendingRelationSourceId = null
                },
                DiagramTool.Mode(
                    CustomIcons.StickyNote,
                    "Crea Nota",
                    UiStrings.NOTE,
                    state.toolMode == ClassDiagramToolMode.NOTE,
                ) {
                    state.toolMode = ClassDiagramToolMode.NOTE
                },
            ),
        selectionMenu = {
            selectedType()?.let { DiagramSelectionMenu(actionsTitle(it), actions(it)) }
        },
        properties = {
            ClassDiagramPropertiesPanel(
                state = state,
                onEditClass = { showClassDialog = true },
                onEditRelation = { showRelationDialog = true },
                onEditNote = { showNoteDialog = true },
                onAddAttribute = { showAddAttributeDialog = true },
                onAddMethod = { showAddMethodDialog = true },
                onDeleteMember = { memberId ->
                    state.selectedClassId?.let {
                        state.deleteMemberFromClass(it, memberId)
                        Toasts.success("Membro eliminato")
                    }
                },
                onClose = { state.clearSelection() },
            )
        },
    ) {
        ClassDiagramCanvas(
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
    if (showClassDialog) {
        state.diagram.classes
            .find { it.id == state.selectedClassId }
            ?.let { entity ->
                ClassEntityDialog(
                    entityId = entity.id,
                    name = entity.name,
                    type = entity.type,
                    isAbstract = entity.isAbstract,
                    documentation = entity.documentation,
                    onDismiss = { showClassDialog = false },
                    onConfirm = { newName, newType, newAbstract, newDoc ->
                        state.updateClass(entity.id) {
                            it.copy(
                                name = newName,
                                type = newType,
                                isAbstract = newAbstract,
                                documentation = newDoc,
                            )
                        }
                        showClassDialog = false
                        Toasts.success("Classe modificata")
                    },
                )
            }
    }
    if (showRelationDialog) {
        state.diagram.relations
            .find { it.id == state.selectedRelationId }
            ?.let { relation ->
                ClassRelationDialog(
                    relation = relation,
                    onDismiss = { showRelationDialog = false },
                    onConfirm = { newType, srcMulti, tgtMulti, lbl ->
                        state.updateRelation(relation.id) {
                            it.copy(
                                type = newType,
                                sourceMultiplicity = srcMulti,
                                targetMultiplicity = tgtMulti,
                                label = lbl,
                            )
                        }
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
    if (showAddAttributeDialog) {
        state.selectedClassId?.let { classId ->
            ClassMemberDialog(
                isMethod = false,
                onDismiss = { showAddAttributeDialog = false },
                onConfirm = { member ->
                    state.addMemberToClass(classId, member, isMethod = false)
                    showAddAttributeDialog = false
                    Toasts.success("Attributo aggiunto")
                },
            )
        }
    }
    if (showAddMethodDialog) {
        state.selectedClassId?.let { classId ->
            ClassMemberDialog(
                isMethod = true,
                onDismiss = { showAddMethodDialog = false },
                onConfirm = { member ->
                    state.addMemberToClass(classId, member, isMethod = true)
                    showAddMethodDialog = false
                    Toasts.success("Metodo aggiunto")
                },
            )
        }
    }
    pendingRelation?.let { relation ->
        SelectClassRelationTypeDialog(
            onDismiss = { pendingRelation = null },
            onConfirm = { type ->
                state.addRelation(relation.sourceId, relation.targetId, type)
                Toasts.success("Relazione creata")
                pendingRelation = null
            },
        )
    }
}
