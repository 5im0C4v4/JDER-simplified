package com.jder

import com.jder.data.DiagramFileRepository
import com.jder.data.detectDiagramType
import com.jder.domain.model.DiagramEditorState
import com.jder.domain.model.DiagramType
import java.io.File
import kotlin.system.exitProcess

private const val JSON_OPTION = "--json"
private const val PNG_OPTION = "--png"

const val COMMAND_LINE_USAGE =
    """Uso: java -jar JDER.jar [--json <file.json>] [--png <file.png>]

Opzioni:
  --json <file>  apre il diagramma salvato nel file JSON, nella scheda del suo tipo
                 (se il file non esiste o il tipo non si riconosce, il tipo si sceglie
                 all'avvio; un file nuovo viene salvato alla prima modifica)
  --png <file>   salva il diagramma come immagine PNG e la aggiorna automaticamente a
                 ogni modifica
  -h, --help     mostra questo messaggio ed esce

Se manca, al nome del file viene aggiunta l'estensione .json o .png.

Esempio:
  java -jar JDER.jar --json diagramma.json --png diagramma.png"""

class CommandLineException(message: String) : Exception(message)

class CommandLineOptions(
    val jsonFile: File? = null,
    val pngFile: File? = null,
    val showHelp: Boolean = false,
)

fun parseCommandLine(args: Array<String>): CommandLineOptions {
    val files = mutableMapOf<String, File>()
    var index = 0
    while (index < args.size) {
        val arg = args[index++]
        val isOption = arg.startsWith("-")
        if (!isOption) throw CommandLineException("argomento non previsto: $arg")
        val option = arg.substringBefore('=')
        when {
            option == "-h" || option == "--help" -> return CommandLineOptions(showHelp = true)
            option != JSON_OPTION && option != PNG_OPTION ->
                throw CommandLineException("opzione sconosciuta: $option")
        }
        val path =
            when {
                '=' in arg -> arg.substringAfter('=')
                else -> args.getOrNull(index)?.takeUnless { it.startsWith("-") }?.also { index++ }
            }
        if (path.isNullOrEmpty()) throw CommandLineException("manca il file dopo $option")
        if (files.put(option, File(path).absoluteFile) != null) {
            throw CommandLineException("l'opzione $option è ripetuta")
        }
    }
    val jsonFile = files[JSON_OPTION]?.withExtension(".json")
    val pngFile = files[PNG_OPTION]?.withExtension(".png")
    listOfNotNull(jsonFile, pngFile).forEach { file ->
        if (file.isDirectory) throw CommandLineException("${file.path} è una cartella")
        if (file.parentFile?.isDirectory != true) {
            throw CommandLineException("la cartella ${file.parent} non esiste")
        }
    }
    return CommandLineOptions(jsonFile, pngFile)
}

private fun File.withExtension(extension: String): File =
    if (name.endsWith(extension, ignoreCase = true)) this else File(path + extension)

fun readDiagramType(file: File): DiagramType? =
    if (!file.exists()) {
        null
    } else
        try {
            detectDiagramType(file)
        } catch (e: Exception) {
            exitWithError("impossibile aprire ${file.path}: ${e.message}")
        }

fun <T> openStartupDiagram(
    state: DiagramEditorState<T>,
    repository: DiagramFileRepository<T>,
    options: CommandLineOptions,
) {
    options.jsonFile?.let { file ->
        if (file.exists()) {
            val diagram =
                repository.loadDiagram(file).getOrElse {
                    exitWithError("impossibile aprire ${file.path}: ${it.message}")
                }
            state.loadDiagram(diagram, file.absolutePath)
        } else {
            state.newDiagram()
            state.currentFile = file.absolutePath
            state.isAutoSaveEnabled = true
        }
    }
    options.pngFile?.let {
        state.imageFile = it.absolutePath
        state.isAutoExportEnabled = true
    }
}

fun exitWithError(message: String?, showUsageHint: Boolean = false): Nothing {
    System.err.println("Errore: $message")
    if (showUsageHint) System.err.println("Usa --help per vedere le opzioni disponibili.")
    exitProcess(1)
}
