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
            file.writeBytesAtomically(encoded.toByteArray())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    fun loadDiagram(file: File): Result<T> =
        try {
            val content = readJsonObject(file)
            val found = detectDiagramType(content)
            if (found != null && found != type) throw WrongDiagramTypeException(found, type)
            val diagram = diagramJson.decodeFromJsonElement(serializer, content)
            validate(diagram)
            Result.success(diagram)
        } catch (e: Exception) {
            Result.failure(e)
        }
}

fun detectDiagramType(file: File): DiagramType? = detectDiagramType(readJsonObject(file))

internal fun File.writeBytesAtomically(bytes: ByteArray) {
    val target = toPath()
    if (Files.exists(target) && !Files.isWritable(target)) {
        throw AccessDeniedException(this, reason = "il file è di sola lettura")
    }
    val parent = absoluteFile.parentFile
    Files.createDirectories(parent.toPath())
    val temporary = File.createTempFile(".$name.", ".tmp", parent).toPath()
    try {
        Files.write(temporary, bytes)
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
}

private fun readJsonObject(file: File): JsonObject =
    diagramJson.parseToJsonElement(file.readText()).jsonObject

private fun detectDiagramType(content: JsonObject): DiagramType? {
    val declared = content[DIAGRAM_TYPE_KEY]?.jsonPrimitive?.contentOrNull
    DiagramType.entries
        .find { it.key == declared }
        ?.let {
            return it
        }
    return DiagramType.entries.find { it.markerKeys.any { entry -> entry in content } }
}
