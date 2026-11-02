package com.jder.domain.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

class ERDiagramState(initialSnapToGridEnabled: Boolean = false) :
    DiagramEditorState<ERDiagram>(
        { ERDiagram(name = "Nuovo Diagramma E-R") },
        initialSnapToGridEnabled,
    ) {
    var selectedEntityId by mutableStateOf<String?>(null)
        private set

    var selectedRelationshipId by mutableStateOf<String?>(null)
        private set

    var selectedNoteId by mutableStateOf<String?>(null)
        private set

    var toolMode by mutableStateOf(ERToolMode.SELECT)

    override val diagramName: String
        get() = diagram.name

    override val isEmpty: Boolean
        get() =
            diagram.entities.isEmpty() && diagram.relationships.isEmpty() && diagram.notes.isEmpty()

    override val hasSelection: Boolean
        get() = selectedEntityId != null || selectedRelationshipId != null || selectedNoteId != null

    override val isSelectTool: Boolean
        get() = toolMode == ERToolMode.SELECT

    override fun withName(diagram: ERDiagram, name: String) = diagram.copy(name = name)

    override fun pruneSelection() {
        if (selectedEntityId != null && diagram.entities.none { it.id == selectedEntityId }) {
            selectedEntityId = null
        }
        if (
            selectedRelationshipId != null &&
                diagram.relationships.none { it.id == selectedRelationshipId }
        ) {
            selectedRelationshipId = null
        }
        if (selectedNoteId != null && diagram.notes.none { it.id == selectedNoteId }) {
            selectedNoteId = null
        }
    }

    override fun resetToolMode() {
        toolMode = ERToolMode.SELECT
    }

    override fun deleteSelection(): String? {
        selectedEntityId?.let {
            deleteEntity(it)
            return "Entità eliminata"
        }
        selectedRelationshipId?.let {
            deleteRelationship(it)
            return "Relazione eliminata"
        }
        selectedNoteId?.let {
            deleteNote(it)
            return "Nota eliminata"
        }
        return null
    }

    fun addEntity(x: Float, y: Float, name: String) {
        val newEntity = Entity(id = UUID.randomUUID().toString(), name = name, x = x, y = y)
        edit { it.copy(entities = it.entities + newEntity) }
        selectEntity(newEntity.id)
    }

    fun addRelationship(x: Float, y: Float, name: String) {
        val newRelationship =
            Relationship(id = UUID.randomUUID().toString(), name = name, x = x, y = y)
        edit { it.copy(relationships = it.relationships + newRelationship) }
        selectRelationship(newRelationship.id)
    }

    fun addNote(x: Float, y: Float, text: String) {
        val newNote = Note(id = UUID.randomUUID().toString(), text = text, x = x, y = y)
        edit { it.copy(notes = it.notes + newNote) }
        selectNote(newNote.id)
    }

    fun updateEntity(entityId: String, update: (Entity) -> Entity) {
        edit { d ->
            d.copy(entities = d.entities.map { if (it.id == entityId) update(it) else it })
        }
    }

    fun updateEntityWithoutSave(entityId: String, update: (Entity) -> Entity) {
        editWithoutSave { d ->
            d.copy(entities = d.entities.map { if (it.id == entityId) update(it) else it })
        }
    }

    fun updateRelationship(relationshipId: String, update: (Relationship) -> Relationship) {
        edit { d ->
            d.copy(
                relationships =
                    d.relationships.map { if (it.id == relationshipId) update(it) else it }
            )
        }
    }

    fun updateRelationshipWithoutSave(
        relationshipId: String,
        update: (Relationship) -> Relationship,
    ) {
        editWithoutSave { d ->
            d.copy(
                relationships =
                    d.relationships.map { if (it.id == relationshipId) update(it) else it }
            )
        }
    }

    fun updateNote(noteId: String, update: (Note) -> Note) {
        edit { d -> d.copy(notes = d.notes.map { if (it.id == noteId) update(it) else it }) }
    }

    fun updateNoteWithoutSave(noteId: String, update: (Note) -> Note) {
        editWithoutSave { d ->
            d.copy(notes = d.notes.map { if (it.id == noteId) update(it) else it })
        }
    }

    fun deleteEntity(entityId: String) {
        edit { d ->
            d.copy(
                entities = d.entities.filter { it.id != entityId },
                relationships =
                    d.relationships.map { relationship ->
                        relationship.copy(
                            connections =
                                relationship.connections.filter { it.entityId != entityId }
                        )
                    },
            )
        }
        if (selectedEntityId == entityId) {
            selectedEntityId = null
        }
    }

    fun deleteRelationship(relationshipId: String) {
        edit { d -> d.copy(relationships = d.relationships.filter { it.id != relationshipId }) }
        if (selectedRelationshipId == relationshipId) {
            selectedRelationshipId = null
        }
    }

    fun deleteNote(noteId: String) {
        edit { d -> d.copy(notes = d.notes.filter { it.id != noteId }) }
        if (selectedNoteId == noteId) {
            selectedNoteId = null
        }
    }

    fun addAttributeToEntity(entityId: String, attribute: Attribute) {
        updateEntity(entityId) { entity -> entity.copy(attributes = entity.attributes + attribute) }
    }

    fun addAttributeToRelationship(relationshipId: String, attribute: Attribute) {
        updateRelationship(relationshipId) { relationship ->
            relationship.copy(attributes = relationship.attributes + attribute)
        }
    }

    fun deleteAttributeFromEntity(entityId: String, attributeId: String) {
        updateEntity(entityId) { entity ->
            entity.copy(attributes = entity.attributes.filter { it.id != attributeId })
        }
    }

    fun deleteAttributeFromRelationship(relationshipId: String, attributeId: String) {
        updateRelationship(relationshipId) { relationship ->
            relationship.copy(attributes = relationship.attributes.filter { it.id != attributeId })
        }
    }

    fun updateAttributeInEntity(entityId: String, attributeId: String, newAttribute: Attribute) {
        updateEntity(entityId) { entity ->
            entity.copy(
                attributes =
                    entity.attributes.map { if (it.id == attributeId) newAttribute else it }
            )
        }
    }

    fun updateAttributeInRelationship(
        relationshipId: String,
        attributeId: String,
        newAttribute: Attribute,
    ) {
        updateRelationship(relationshipId) { relationship ->
            relationship.copy(
                attributes =
                    relationship.attributes.map { if (it.id == attributeId) newAttribute else it }
            )
        }
    }

    fun addConnection(relationshipId: String, entityId: String, cardinality: Cardinality) {
        updateRelationship(relationshipId) { relationship ->
            if (relationship.connections.any { it.entityId == entityId }) {
                relationship
            } else {
                val connection = Connection(entityId = entityId, cardinality = cardinality)
                relationship.copy(connections = relationship.connections + connection)
            }
        }
    }

    fun deleteConnection(relationshipId: String, entityId: String) {
        updateRelationship(relationshipId) { relationship ->
            relationship.copy(
                connections = relationship.connections.filter { it.entityId != entityId }
            )
        }
    }

    fun updateConnection(
        relationshipId: String,
        oldEntityId: String,
        newEntityId: String,
        newCardinality: Cardinality,
    ) {
        updateRelationship(relationshipId) { relationship ->
            if (
                newEntityId != oldEntityId &&
                    relationship.connections.any { it.entityId == newEntityId }
            ) {
                relationship
            } else {
                relationship.copy(
                    connections =
                        relationship.connections.map {
                            if (it.entityId == oldEntityId) {
                                Connection(entityId = newEntityId, cardinality = newCardinality)
                            } else {
                                it
                            }
                        }
                )
            }
        }
    }

    fun selectEntity(entityId: String?) {
        selectedEntityId = entityId
        selectedRelationshipId = null
        selectedNoteId = null
    }

    fun selectRelationship(relationshipId: String?) {
        selectedRelationshipId = relationshipId
        selectedEntityId = null
        selectedNoteId = null
    }

    fun selectNote(noteId: String?) {
        selectedNoteId = noteId
        selectedEntityId = null
        selectedRelationshipId = null
    }

    override fun clearSelection() {
        selectedEntityId = null
        selectedRelationshipId = null
        selectedNoteId = null
    }

    fun convertToAssociativeEntity(relationshipId: String) {
        val relationship = diagram.relationships.find { it.id == relationshipId } ?: return
        if (!relationship.isManyToMany()) return
        val conn1 = relationship.connections[0]
        val conn2 = relationship.connections[1]
        val entity1 = diagram.entities.find { it.id == conn1.entityId }
        val entity2 = diagram.entities.find { it.id == conn2.entityId }
        val centerX = relationship.x + relationship.width / 2
        val centerY = relationship.y + relationship.height / 2
        val newEntity =
            Entity(
                id = UUID.randomUUID().toString(),
                name = relationship.name,
                x = centerX - 70f,
                y = centerY - 40f,
                attributes = relationship.attributes,
                documentation = relationship.documentation,
            )
        val entity1CenterX = entity1?.let { it.x + it.width / 2 } ?: (centerX - 200f)
        val entity1CenterY = entity1?.let { it.y + it.height / 2 } ?: centerY
        val entity2CenterX = entity2?.let { it.x + it.width / 2 } ?: (centerX + 200f)
        val entity2CenterY = entity2?.let { it.y + it.height / 2 } ?: centerY
        val rel1X = (entity1CenterX + centerX) / 2 - 60f
        val rel1Y = (entity1CenterY + centerY) / 2 - 60f
        val rel2X = (entity2CenterX + centerX) / 2 - 60f
        val rel2Y = (entity2CenterY + centerY) / 2 - 60f
        val newRelationships =
            listOf(
                Relationship(
                    id = UUID.randomUUID().toString(),
                    name = "Nuova Relazione",
                    x = rel1X,
                    y = rel1Y,
                    connections =
                        listOf(
                            Connection(entityId = newEntity.id, cardinality = conn1.cardinality),
                            Connection(entityId = conn1.entityId, cardinality = Cardinality.ONE_ONE),
                        ),
                ),
                Relationship(
                    id = UUID.randomUUID().toString(),
                    name = "Nuova Relazione",
                    x = rel2X,
                    y = rel2Y,
                    connections =
                        listOf(
                            Connection(entityId = newEntity.id, cardinality = conn2.cardinality),
                            Connection(entityId = conn2.entityId, cardinality = Cardinality.ONE_ONE),
                        ),
                ),
            )
        edit { d ->
            d.copy(
                entities = d.entities + newEntity,
                relationships =
                    d.relationships.filter { it.id != relationshipId } + newRelationships,
            )
        }
        selectedRelationshipId = null
    }
}
