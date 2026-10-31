package com.jder.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jder.data.ERDiagramRepository
import com.jder.domain.model.Attribute
import com.jder.domain.model.Connection
import com.jder.domain.model.ERDiagramState
import com.jder.domain.model.ERToolMode
import com.jder.domain.model.isManyToMany
import com.jder.ui.components.ContextMenuItem
import com.jder.ui.components.ContextMenuRequest
import com.jder.ui.components.CustomIcons
import com.jder.ui.components.DiagramContextMenu
import com.jder.ui.components.DiagramSelectionMenu
import com.jder.ui.components.DiagramTool
import com.jder.ui.components.ERContextMenuType
import com.jder.ui.components.ERDiagramCanvas
import com.jder.ui.components.ERElement
import com.jder.ui.components.ERPropertiesPanel
import com.jder.ui.components.Toasts
import com.jder.ui.dialogs.AddAttributeDialog
import com.jder.ui.dialogs.CreateConnectionDialog
import com.jder.ui.dialogs.EditAttributeDialog
import com.jder.ui.dialogs.EditConnectionDialog
import com.jder.ui.dialogs.EntityPropertiesDialog
import com.jder.ui.dialogs.NotePropertiesDialog
import com.jder.ui.dialogs.RelationshipPropertiesDialog
import com.jder.ui.strings.UiStrings
import com.jder.ui.theme.ThemeState
import com.jder.ui.utils.renderERDiagramToBitmap

@Composable
fun ERDiagramScreen(
    state: ERDiagramState,
    repository: ERDiagramRepository,
    themeState: ThemeState,
) {
    var showEntityDialog by remember { mutableStateOf(false) }
    var showRelationshipDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showAddAttributeDialog by remember { mutableStateOf(false) }
    var attributeToEdit by remember { mutableStateOf<Attribute?>(null) }
    var showCreateConnectionDialog by remember { mutableStateOf(false) }
    var connectionToEdit by remember { mutableStateOf<ConnectionEdit?>(null) }
    var contextMenu by remember { mutableStateOf<ContextMenuRequest<ERContextMenuType>?>(null) }
    fun openEditor(type: ERContextMenuType) {
        when (type) {
            ERContextMenuType.ENTITY -> showEntityDialog = true
            ERContextMenuType.RELATIONSHIP -> showRelationshipDialog = true
            ERContextMenuType.NOTE -> showNoteDialog = true
        }
    }
    fun convertSelectedRelationship() {
        state.selectedRelationshipId?.let {
            state.convertToAssociativeEntity(it)
            Toasts.success("Relazione convertita in entità associativa")
        }
    }
    fun selectedType(): ERContextMenuType? =
        when {
            state.selectedEntityId != null -> ERContextMenuType.ENTITY
            state.selectedRelationshipId != null -> ERContextMenuType.RELATIONSHIP
            state.selectedNoteId != null -> ERContextMenuType.NOTE
            else -> null
        }
    fun actionsTitle(type: ERContextMenuType): String =
        when (type) {
            ERContextMenuType.ENTITY -> "Azioni Entità"
            ERContextMenuType.RELATIONSHIP -> "Azioni Relazione"
            ERContextMenuType.NOTE -> "Azioni Nota"
        }
    fun actions(type: ERContextMenuType): List<ContextMenuItem> {
        val isManyToMany =
            state.diagram.relationships
                .find { it.id == state.selectedRelationshipId }
                ?.isManyToMany() == true
        return buildList {
            add(
                ContextMenuItem(
                    if (type == ERContextMenuType.NOTE) "Modifica Testo" else "Modifica Proprietà",
                    Icons.Default.Edit,
                ) {
                    openEditor(type)
                }
            )
            if (type != ERContextMenuType.NOTE) {
                add(
                    ContextMenuItem("Aggiungi Attributo", Icons.Default.Add) {
                        showAddAttributeDialog = true
                    }
                )
            }
            if (type == ERContextMenuType.RELATIONSHIP) {
                add(
                    ContextMenuItem("Aggiungi Connessione", Icons.Default.Link) {
                        showCreateConnectionDialog = true
                    }
                )
                add(
                    ContextMenuItem(
                        "Converti in Entità Associativa",
                        Icons.Default.AutoFixHigh,
                        enabled = isManyToMany,
                        onClick = { convertSelectedRelationship() },
                    )
                )
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
        renderImage = { renderERDiagramToBitmap(state.diagram, it) },
        emptyHint =
            "Scegli Entità, Relazione o Nota dalla barra degli strumenti e clicca sulla lavagna.\n" +
                "Per collegare due entità, scegli Relazione e clicca prima sulla prima entità e poi sulla seconda.",
        tools =
            listOf(
                DiagramTool.Mode(
                    CustomIcons.Rectangle,
                    "Crea Entità",
                    "Entità",
                    state.toolMode == ERToolMode.ENTITY,
                ) {
                    state.toolMode = ERToolMode.ENTITY
                },
                DiagramTool.Mode(
                    CustomIcons.Diamond,
                    "Crea Relazione",
                    UiStrings.RELATION,
                    state.toolMode == ERToolMode.RELATIONSHIP,
                ) {
                    state.toolMode = ERToolMode.RELATIONSHIP
                },
                DiagramTool.Mode(
                    CustomIcons.StickyNote,
                    "Crea Nota",
                    UiStrings.NOTE,
                    state.toolMode == ERToolMode.NOTE,
                ) {
                    state.toolMode = ERToolMode.NOTE
                },
            ),
        selectionMenu = {
            selectedType()?.let { DiagramSelectionMenu(actionsTitle(it), actions(it)) }
        },
        properties = {
            ERPropertiesPanel(
                state = state,
                onEditEntity = { showEntityDialog = true },
                onEditRelationship = { showRelationshipDialog = true },
                onEditNote = { showNoteDialog = true },
                onAddAttribute = { showAddAttributeDialog = true },
                onAddConnection = { showCreateConnectionDialog = true },
                onEditAttribute = { attributeToEdit = it },
                onDeleteAttribute = { attributeId ->
                    state.selectedEntityId?.let { state.deleteAttributeFromEntity(it, attributeId) }
                    state.selectedRelationshipId?.let {
                        state.deleteAttributeFromRelationship(it, attributeId)
                    }
                    Toasts.success(UiStrings.deleted("Attributo"))
                },
                onEditConnection = { entityId, connection ->
                    connectionToEdit = ConnectionEdit(entityId, connection)
                },
                onDeleteConnection = { entityId ->
                    state.selectedRelationshipId?.let {
                        state.deleteConnection(it, entityId)
                        Toasts.success(UiStrings.deleted("Connessione"))
                    }
                },
                onConvertToAssociativeEntity = ::convertSelectedRelationship,
                onClose = { state.clearSelection() },
            )
        },
    ) {
        ERDiagramCanvas(
            state = state,
            onContextMenuRequest = { position, type ->
                contextMenu = ContextMenuRequest(position, type)
            },
            onEditRequest = { element ->
                if (element is ERElement.AttributeRef) {
                    val attributes =
                        if (element.isEntityAttribute) {
                            state.diagram.entities.find { it.id == element.id }?.attributes
                        } else {
                            state.diagram.relationships.find { it.id == element.id }?.attributes
                        }
                    attributeToEdit = attributes?.find { it.id == element.attributeId }
                } else {
                    openEditor(
                        when (element) {
                            is ERElement.EntityRef -> ERContextMenuType.ENTITY
                            is ERElement.RelationshipRef -> ERContextMenuType.RELATIONSHIP
                            else -> ERContextMenuType.NOTE
                        }
                    )
                }
            },
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
    if (showEntityDialog) {
        state.diagram.entities
            .find { it.id == state.selectedEntityId }
            ?.let { entity ->
                EntityPropertiesDialog(
                    entity = entity,
                    onDismiss = { showEntityDialog = false },
                    onConfirm = { updated ->
                        state.updateEntity(entity.id) { updated }
                        showEntityDialog = false
                        Toasts.success("Entità modificata")
                    },
                )
            }
    }
    if (showRelationshipDialog) {
        state.diagram.relationships
            .find { it.id == state.selectedRelationshipId }
            ?.let { rel ->
                RelationshipPropertiesDialog(
                    relationship = rel,
                    onDismiss = { showRelationshipDialog = false },
                    onConfirm = { updated ->
                        state.updateRelationship(rel.id) { updated }
                        showRelationshipDialog = false
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
        AddAttributeDialog(
            onDismiss = { showAddAttributeDialog = false },
            onConfirm = { attribute ->
                state.selectedEntityId?.let { state.addAttributeToEntity(it, attribute) }
                state.selectedRelationshipId?.let {
                    state.addAttributeToRelationship(it, attribute)
                }
                showAddAttributeDialog = false
                Toasts.success("Attributo aggiunto")
            },
        )
    }
    attributeToEdit?.let { attribute ->
        EditAttributeDialog(
            attribute = attribute,
            onDismiss = { attributeToEdit = null },
            onConfirm = { updated ->
                state.selectedEntityId?.let {
                    state.updateAttributeInEntity(it, attribute.id, updated)
                }
                state.selectedRelationshipId?.let {
                    state.updateAttributeInRelationship(it, attribute.id, updated)
                }
                attributeToEdit = null
                Toasts.success("Attributo modificato")
            },
        )
    }
    if (showCreateConnectionDialog) {
        state.selectedRelationshipId?.let { relId ->
            val connectedIds =
                state.diagram.relationships
                    .find { it.id == relId }
                    ?.connections
                    ?.map { it.entityId }
                    ?.toSet() ?: emptySet()
            CreateConnectionDialog(
                entities = state.diagram.entities.filter { it.id !in connectedIds },
                onDismiss = { showCreateConnectionDialog = false },
                onConfirm = { entityId, cardinality ->
                    state.addConnection(relId, entityId, cardinality)
                    showCreateConnectionDialog = false
                    Toasts.success("Connessione creata")
                },
            )
        }
    }
    connectionToEdit?.let { edit ->
        state.selectedRelationshipId?.let { relId ->
            val connectedIds =
                state.diagram.relationships
                    .find { it.id == relId }
                    ?.connections
                    ?.map { it.entityId }
                    ?.filter { it != edit.entityId }
                    ?.toSet() ?: emptySet()
            EditConnectionDialog(
                entities = state.diagram.entities.filter { it.id !in connectedIds },
                currentEntityId = edit.entityId,
                currentCardinality = edit.connection.cardinality,
                onDismiss = { connectionToEdit = null },
                onConfirm = { newEntityId, newCardinality ->
                    state.updateConnection(relId, edit.entityId, newEntityId, newCardinality)
                    connectionToEdit = null
                    Toasts.success("Connessione modificata")
                },
            )
        }
    }
}

private class ConnectionEdit(val entityId: String, val connection: Connection)
