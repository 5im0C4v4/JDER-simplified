package com.jder.domain.model

enum class DiagramType(
    val key: String,
    val tabLabel: String,
    val displayName: String,
    val markerKeys: Set<String>,
) {
    ER("er", "Diagramma E/R", "un diagramma E/R", setOf("entities", "relationships"))
}
