package com.jder.ui.components

import androidx.compose.ui.geometry.Offset
import com.jder.domain.model.Attribute
import com.jder.domain.model.ERDiagram
import com.jder.domain.model.ERDiagramState
import com.jder.domain.model.ERToolMode
import com.jder.domain.model.Entity
import kotlin.math.abs
import kotlin.math.roundToInt

private const val ATTRIBUTE_HIT_RADIUS = 24f

sealed interface ERElement {
    val id: String

    data class EntityRef(override val id: String) : ERElement

    data class RelationshipRef(override val id: String) : ERElement

    data class NoteRef(override val id: String) : ERElement

    data class AttributeRef(
        val attributeId: String,
        override val id: String,
        val isEntityAttribute: Boolean,
    ) : ERElement
}

internal val ERElement.contextMenuType: ERContextMenuType
    get() =
        when (this) {
            is ERElement.EntityRef -> ERContextMenuType.ENTITY
            is ERElement.RelationshipRef -> ERContextMenuType.RELATIONSHIP
            is ERElement.NoteRef -> ERContextMenuType.NOTE
            is ERElement.AttributeRef ->
                if (isEntityAttribute) ERContextMenuType.ENTITY else ERContextMenuType.RELATIONSHIP
        }

internal fun isPointInDiamond(
    point: Offset,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
): Boolean {
    val centerX = x + width / 2
    val centerY = y + height / 2
    val dx = abs(point.x - centerX) / (width / 2)
    val dy = abs(point.y - centerY) / (height / 2)
    return (dx + dy) <= 1.0
}

internal fun Entity.contains(point: Offset): Boolean =
    point.x >= x && point.x <= x + width && point.y >= y && point.y <= y + height

internal fun calculateAttributePosition(
    entityX: Float,
    entityY: Float,
    entityWidth: Float,
    entityHeight: Float,
    index: Int,
    total: Int,
    attribute: Attribute,
): Offset {
    val centerX = entityX + entityWidth / 2
    val centerY = entityY + entityHeight / 2
    if (attribute.x != 0f || attribute.y != 0f)
        return Offset(centerX + attribute.x, centerY + attribute.y)
    val arrowLength = 60f
    val verticalSpacing = 60f
    val startY = centerY - ((total - 1) * verticalSpacing / 2f)
    return Offset(entityX + entityWidth + arrowLength, startY + (index * verticalSpacing))
}

private fun attributeAt(
    attributes: List<Attribute>,
    ownerX: Float,
    ownerY: Float,
    ownerWidth: Float,
    ownerHeight: Float,
    point: Offset,
): Attribute? =
    attributes.indices
        .reversed()
        .firstOrNull { index ->
            val position =
                calculateAttributePosition(
                    ownerX,
                    ownerY,
                    ownerWidth,
                    ownerHeight,
                    index,
                    attributes.size,
                    attributes[index],
                )
            (position - point).getDistance() <= ATTRIBUTE_HIT_RADIUS
        }
        ?.let { attributes[it] }

internal fun hitTestERDiagram(diagram: ERDiagram, point: Offset): ERElement? {
    diagram.notes
        .lastOrNull { it.contains(point) }
        ?.let {
            return ERElement.NoteRef(it.id)
        }
    diagram.relationships.asReversed().forEach { relationship ->
        attributeAt(
                relationship.attributes,
                relationship.x,
                relationship.y,
                relationship.width,
                relationship.height,
                point,
            )
            ?.let {
                return ERElement.AttributeRef(it.id, relationship.id, isEntityAttribute = false)
            }
    }
    diagram.relationships
        .lastOrNull { isPointInDiamond(point, it.x, it.y, it.width, it.height) }
        ?.let {
            return ERElement.RelationshipRef(it.id)
        }
    diagram.entities.asReversed().forEach { entity ->
        attributeAt(entity.attributes, entity.x, entity.y, entity.width, entity.height, point)
            ?.let {
                return ERElement.AttributeRef(it.id, entity.id, isEntityAttribute = true)
            }
    }
    diagram.entities
        .lastOrNull { it.contains(point) }
        ?.let {
            return ERElement.EntityRef(it.id)
        }
    return null
}

internal fun selectERElement(state: ERDiagramState, element: ERElement) {
    when (element) {
        is ERElement.EntityRef -> state.selectEntity(element.id)
        is ERElement.RelationshipRef -> state.selectRelationship(element.id)
        is ERElement.NoteRef -> state.selectNote(element.id)
        is ERElement.AttributeRef ->
            if (element.isEntityAttribute) state.selectEntity(element.id)
            else state.selectRelationship(element.id)
    }
}

internal fun handleERTap(state: ERDiagramState, offset: Offset) {
    when (state.toolMode) {
        ERToolMode.SELECT ->
            hitTestERDiagram(state.diagram, offset)?.let { selectERElement(state, it) }
                ?: state.clearSelection()
        ERToolMode.ENTITY -> {
            state.addEntity(offset.x - 70f, offset.y - 40f, "Nuova Entità")
            state.toolMode = ERToolMode.SELECT
        }
        ERToolMode.RELATIONSHIP -> {
            state.addRelationship(offset.x - 60f, offset.y - 60f, "Nuova Relazione")
            state.toolMode = ERToolMode.SELECT
        }
        ERToolMode.NOTE -> {
            state.addNote(offset.x - 120f, offset.y - 90f, "Nuova Nota")
            state.toolMode = ERToolMode.SELECT
        }
    }
}

internal fun handleERDragStart(state: ERDiagramState, offset: Offset): ERElement? {
    val element = hitTestERDiagram(state.diagram, offset) ?: return null
    selectERElement(state, element)
    state.saveDragStartState()
    return element
}

internal fun handleERDrag(state: ERDiagramState, element: ERElement, delta: Offset) {
    when (element) {
        is ERElement.EntityRef ->
            state.updateEntityWithoutSave(element.id) {
                it.copy(x = it.x + delta.x, y = it.y + delta.y)
            }
        is ERElement.RelationshipRef ->
            state.updateRelationshipWithoutSave(element.id) {
                it.copy(x = it.x + delta.x, y = it.y + delta.y)
            }
        is ERElement.NoteRef ->
            state.updateNoteWithoutSave(element.id) {
                it.copy(x = it.x + delta.x, y = it.y + delta.y)
            }
        is ERElement.AttributeRef ->
            if (element.isEntityAttribute) {
                state.updateEntityWithoutSave(element.id) { entity ->
                    entity.copy(
                        attributes =
                            orbitAttribute(
                                entity.attributes,
                                element.attributeId,
                                entity.x,
                                entity.y,
                                entity.width,
                                entity.height,
                                delta,
                            )
                    )
                }
            } else {
                state.updateRelationshipWithoutSave(element.id) { relationship ->
                    relationship.copy(
                        attributes =
                            orbitAttribute(
                                relationship.attributes,
                                element.attributeId,
                                relationship.x,
                                relationship.y,
                                relationship.width,
                                relationship.height,
                                delta,
                            )
                    )
                }
            }
    }
}

internal fun handleERSnappedDrag(
    state: ERDiagramState,
    element: ERElement,
    totalDelta: Offset,
    dragStartDiagram: ERDiagram,
) {
    when (element) {
        is ERElement.EntityRef -> {
            val initial = dragStartDiagram.entities.firstOrNull { it.id == element.id } ?: return
            state.updateEntityWithoutSave(element.id) {
                it.copy(
                    x = snapToGrid(initial.x + totalDelta.x),
                    y = snapToGrid(initial.y + totalDelta.y),
                )
            }
        }
        is ERElement.RelationshipRef -> {
            val initial =
                dragStartDiagram.relationships.firstOrNull { it.id == element.id } ?: return
            state.updateRelationshipWithoutSave(element.id) {
                it.copy(
                    x = snapToGrid(initial.x + totalDelta.x),
                    y = snapToGrid(initial.y + totalDelta.y),
                )
            }
        }
        is ERElement.NoteRef -> {
            val initial = dragStartDiagram.notes.firstOrNull { it.id == element.id } ?: return
            state.updateNoteWithoutSave(element.id) {
                it.copy(
                    x = snapToGrid(initial.x + totalDelta.x),
                    y = snapToGrid(initial.y + totalDelta.y),
                )
            }
        }
        is ERElement.AttributeRef ->
            if (element.isEntityAttribute) {
                val initial =
                    dragStartDiagram.entities.firstOrNull { it.id == element.id } ?: return
                state.updateEntityWithoutSave(element.id) { entity ->
                    entity.copy(
                        attributes =
                            moveAttributeToGrid(
                                initial.attributes,
                                initial.x,
                                initial.y,
                                initial.width,
                                initial.height,
                                element.attributeId,
                                totalDelta,
                            )
                    )
                }
            } else {
                val initial =
                    dragStartDiagram.relationships.firstOrNull { it.id == element.id } ?: return
                state.updateRelationshipWithoutSave(element.id) { relationship ->
                    relationship.copy(
                        attributes =
                            moveAttributeToGrid(
                                initial.attributes,
                                initial.x,
                                initial.y,
                                initial.width,
                                initial.height,
                                element.attributeId,
                                totalDelta,
                            )
                    )
                }
            }
    }
}

private fun moveAttributeToGrid(
    attributes: List<Attribute>,
    ownerX: Float,
    ownerY: Float,
    ownerWidth: Float,
    ownerHeight: Float,
    attributeId: String,
    totalDelta: Offset,
): List<Attribute> {
    val index = attributes.indexOfFirst { it.id == attributeId }
    if (index < 0) return attributes
    val initialPosition =
        calculateAttributePosition(
            ownerX,
            ownerY,
            ownerWidth,
            ownerHeight,
            index,
            attributes.size,
            attributes[index],
        )
    val center = Offset(ownerX + ownerWidth / 2f, ownerY + ownerHeight / 2f)
    val snappedPosition =
        Offset(
            snapToGrid(initialPosition.x + totalDelta.x),
            snapToGrid(initialPosition.y + totalDelta.y),
        )
    return attributes.map {
        if (it.id == attributeId) {
            it.copy(x = snappedPosition.x - center.x, y = snappedPosition.y - center.y)
        } else {
            it
        }
    }
}

private fun snapToGrid(value: Float): Float = (value / GRID_SPACING).roundToInt() * GRID_SPACING

private fun orbitAttribute(
    attributes: List<Attribute>,
    attributeId: String,
    ownerX: Float,
    ownerY: Float,
    ownerWidth: Float,
    ownerHeight: Float,
    delta: Offset,
): List<Attribute> =
    attributes.mapIndexed { index, attribute ->
        if (attribute.id != attributeId) return@mapIndexed attribute
        val center = Offset(ownerX + ownerWidth / 2, ownerY + ownerHeight / 2)
        val current =
            calculateAttributePosition(
                ownerX,
                ownerY,
                ownerWidth,
                ownerHeight,
                index,
                attributes.size,
                attribute,
            )
        val radius = (current - center).getDistance().takeIf { it > 0f } ?: (ownerWidth / 2 + 60f)
        val moved = current + delta - center
        val length = moved.getDistance()
        val direction = if (length > 0f) moved / length else Offset(1f, 0f)
        attribute.copy(x = direction.x * radius, y = direction.y * radius)
    }
