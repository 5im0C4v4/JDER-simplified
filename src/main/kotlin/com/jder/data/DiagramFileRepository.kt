package com.jder.data

import com.jder.domain.model.DiagramType
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val DIAGRAM_TYPE_KEY = "diagramType"

class WrongDiagramTypeException(found: DiagramType, expected: DiagramType) :
    Exception(
        "il file contiene ${found.displayName}: aprilo dalla scheda \"${found.tabLabel}\" " +
            "invece che da \"${expected.tabLabel}\""
    )

open class DiagramFileRepository<T>(
    private val serializer: KSerializer<T>,
    private val type: DiagramType,
    private val validate: (T) -> Unit = {},
) {
    fun saveDiagram(diagram: T, file: File): Result<Unit> =
        try {
            validate(diagram)
            val content = diagramJson.encodeToJsonElement(serializer, diagram).jsonObject
            val tagged = JsonObject(mapOf(DIAGRAM_TYPE_KEY to JsonPrimitive(type.key)) + content)
            val encoded = diagramJson.encodeToString(JsonObject.serializer(), tagged)
            val target = file.toPath()
            Files.createDirectories(target.toAbsolutePath().parent)
            val temporary =
                Files.createTempFile(target.toAbsolutePath().parent, "${file.name}.", ".tmp")
            try {
                Files.writeString(temporary, encoded)
                try {
                    Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                Files.deleteIfExists(temporary)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    fun loadDiagram(file: File): Result<T> =
        try {
            val content = diagramJson.parseToJsonElement(file.readText()).jsonObject
            val found = detectDiagramType(content)
            if (found != null && found != type) throw WrongDiagramTypeException(found, type)
            val diagram = diagramJson.decodeFromJsonElement(serializer, content)
            validate(diagram)
            Result.success(diagram)
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun detectDiagramType(content: JsonObject): DiagramType? {
        val declared = content[DIAGRAM_TYPE_KEY]?.jsonPrimitive?.contentOrNull
        DiagramType.entries
            .find { it.key == declared }
            ?.let {
                return it
            }
        return DiagramType.entries.find { it.markerKeys.any { entry -> entry in content } }
    }
}
