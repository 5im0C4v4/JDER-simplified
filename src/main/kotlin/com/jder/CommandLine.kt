package com.jder

import com.jder.data.DiagramFileRepository
import com.jder.data.detectDiagramType
import com.jder.domain.model.DiagramEditorState
import com.jder.domain.model.DiagramType
import com.jder.ui.components.Toasts
import java.io.File

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

class CommandLineOptions(
    val jsonFile: File? = null,
    val pngFile: File? = null,
    val showHelp: Boolean = false,
    val warnings: List<String> = emptyList(),
)

class StartupDiagram(val type: DiagramType?, val pendingOptions: CommandLineOptions?)

fun parseCommandLine(args: Array<String>): CommandLineOptions {
    val files = mutableMapOf<String, File>()
    val warnings = mutableListOf<String>()
    var index = 0
    while (index < args.size) {
        val arg = args[index++]
        val isOption = arg.startsWith("-")
        if (!isOption) {
            warnings += "Argomento non previsto: $arg"
            continue
        }
        val option = arg.substringBefore('=')
        when {
            option == "-h" || option == "--help" -> return CommandLineOptions(showHelp = true)
            option != JSON_OPTION && option != PNG_OPTION -> {
                warnings += "Opzione sconosciuta: $option"
                continue
            }
        }
        val path =
            when {
                '=' in arg -> arg.substringAfter('=')
                else -> args.getOrNull(index)?.takeUnless { it.startsWith("-") }?.also { index++ }
            }
        when {
            path.isNullOrEmpty() -> warnings += "Manca il file dopo $option"
            option in files -> warnings += "Opzione $option ripetuta: viene usato il primo file"
            else -> files[option] = File(path).absoluteFile
        }
    }
    val jsonFile = files[JSON_OPTION]?.withExtension(".json")?.takeIfUsable(JSON_OPTION, warnings)
    val pngFile = files[PNG_OPTION]?.withExtension(".png")?.takeIfUsable(PNG_OPTION, warnings)
    return CommandLineOptions(jsonFile, pngFile, warnings = warnings)
}

private fun File.withExtension(extension: String): File =
    if (name.endsWith(extension, ignoreCase = true)) this else File(path + extension)

private fun File.takeIfUsable(option: String, warnings: MutableList<String>): File? {
    val problem =
        when {
            isDirectory -> "$path è una cartella"
            parentFile?.isDirectory != true -> "la cartella $parent non esiste"
            else -> return this
        }
    warnings += "Opzione $option ignorata: $problem"
    return null
}

fun prepareStartupDiagram(
    options: CommandLineOptions,
    openAs: (DiagramType, CommandLineOptions) -> Boolean,
): StartupDiagram {
    val jsonFile = options.jsonFile ?: return StartupDiagram(null, options)
    val type =
        readDiagramType(jsonFile).getOrElse {
            warnCannotOpen(jsonFile, it, options)
            return StartupDiagram(null, null)
        }
    return when {
        type == null -> StartupDiagram(null, options)
        openAs(type, options) -> StartupDiagram(type, null)
        else -> StartupDiagram(null, null)
    }
}

fun readDiagramType(file: File): Result<DiagramType?> =
    if (!file.exists()) Result.success(null) else runCatching { detectDiagramType(file) }

fun <T> openStartupDiagram(
    state: DiagramEditorState<T>,
    repository: DiagramFileRepository<T>,
    options: CommandLineOptions,
): Boolean {
    options.jsonFile?.let { file ->
        if (file.exists()) {
            val diagram =
                repository.loadDiagram(file).getOrElse {
                    warnCannotOpen(file, it, options)
                    return false
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
    return true
}

private fun warnCannotOpen(file: File, cause: Throwable, options: CommandLineOptions) {
    Toasts.warning("Impossibile aprire ${file.name}", cause)
    if (options.pngFile != null) {
        Toasts.warning("Opzione $PNG_OPTION ignorata: il file JSON non si può aprire")
    }
}
