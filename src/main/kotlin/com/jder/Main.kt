package com.jder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jder.data.ClassDiagramRepository
import com.jder.data.ERDiagramRepository
import com.jder.data.LogicDiagramRepository
import com.jder.data.RelationalDiagramRepository
import com.jder.data.SequenceDiagramRepository
import com.jder.data.UseCaseDiagramRepository
import com.jder.domain.model.ClassDiagramState
import com.jder.domain.model.DiagramEditorState
import com.jder.domain.model.DiagramType
import com.jder.domain.model.ERDiagram
import com.jder.domain.model.ERDiagramState
import com.jder.domain.model.LogicDiagramState
import com.jder.domain.model.RelationalDiagramState
import com.jder.domain.model.SequenceDiagramState
import com.jder.domain.model.UseCaseDiagramState
import com.jder.ui.components.AppMenuBar
import com.jder.ui.components.DiagramMenuHost
import com.jder.ui.components.DockMenu
import com.jder.ui.components.LocalDiagramMenuHost
import com.jder.ui.components.WindowMenuBar
import com.jder.ui.components.appMenus
import com.jder.ui.components.diagramTypeKeys
import com.jder.ui.components.toggleFullscreen
import com.jder.ui.dialogs.AboutDialog
import com.jder.ui.screens.AppScreen
import com.jder.ui.theme.JDERTheme
import com.jder.ui.theme.ThemeState
import com.jder.ui.utils.isMacOS
import com.jder.ui.utils.setDockIcon
import java.awt.Dimension
import java.io.File
import kotlin.system.exitProcess

private data class LaunchOptions(val file: File? = null, val preview: File? = null)

private data class InitialDiagram(val diagram: ERDiagram? = null, val error: String? = null)

private const val USAGE =
    "Usage: java -jar jder.jar [--file <diagram.json> [--preview <preview.png>]]"

private fun parseLaunchOptions(args: Array<String>): LaunchOptions? {
    var file: File? = null
    var preview: File? = null
    var index = 0
    while (index < args.size) {
        when (val option = args[index]) {
            "--help",
            "-h" -> {
                println(USAGE)
                return null
            }
            "--file",
            "--preview" -> {
                if (index + 1 >= args.size || args[index + 1].startsWith("--")) {
                    System.err.println("Missing path after $option")
                    System.err.println(USAGE)
                    exitProcess(2)
                }
                val path = File(args[index + 1]).absoluteFile.toPath().normalize().toFile()
                if (option == "--file") {
                    if (file != null) {
                        System.err.println("--file may only be specified once")
                        exitProcess(2)
                    }
                    file = path
                } else {
                    if (preview != null) {
                        System.err.println("--preview may only be specified once")
                        exitProcess(2)
                    }
                    preview = path
                }
                index++
            }
            else -> {
                System.err.println("Unknown argument: $option")
                System.err.println(USAGE)
                exitProcess(2)
            }
        }
        index++
    }
    if (preview != null && file == null) {
        System.err.println("--preview requires --file")
        System.err.println(USAGE)
        exitProcess(2)
    }
    if (file != null && file.toPath() == preview?.toPath()) {
        System.err.println("The JSON diagram path and PNG preview path must be different")
        exitProcess(2)
    }
    return LaunchOptions(file, preview)
}

private fun initialDiagram(file: File?): InitialDiagram {
    if (file == null || !file.exists()) {
        return InitialDiagram(diagram = file?.let { ERDiagram(name = it.nameWithoutExtension) })
    }
    if (!file.isFile) return InitialDiagram(error = "The diagram path is not a file: $file")
    return ERDiagramRepository()
        .loadDiagram(file)
        .fold(
            onSuccess = { InitialDiagram(diagram = it) },
            onFailure = {
                InitialDiagram(
                    error = "Could not open diagram '$file': ${it.message ?: "unknown error"}"
                )
            },
        )
}

fun main(args: Array<String>) {
    val options = parseLaunchOptions(args) ?: return
    val initialDiagram = initialDiagram(options.file)
    System.setProperty("apple.awt.application.name", "JDER")
    System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS")
    System.setProperty("compose.application.configure.swing.globals", "true")
    setDockIcon("jder_icon.png")
    application {
        val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
        val erState = remember {
            ERDiagramState().also { state ->
                initialDiagram.diagram?.let { state.loadDiagram(it, options.file?.path) }
            }
        }
        val relationalState = remember { RelationalDiagramState() }
        val useCaseState = remember { UseCaseDiagramState() }
        val sequenceState = remember { SequenceDiagramState() }
        val classState = remember { ClassDiagramState() }
        val logicState = remember { LogicDiagramState() }
        val themeState = remember { ThemeState() }
        val erRepository = remember { ERDiagramRepository() }
        val relationalRepository = remember { RelationalDiagramRepository() }
        val useCaseRepository = remember { UseCaseDiagramRepository() }
        val sequenceRepository = remember { SequenceDiagramRepository() }
        val classRepository = remember { ClassDiagramRepository() }
        val logicRepository = remember { LogicDiagramRepository() }
        val menuHost = remember { DiagramMenuHost() }
        var currentType by remember { mutableStateOf(DiagramType.ER) }
        var pendingNewDiagram by remember { mutableStateOf<DiagramType?>(null) }
        var showExitDialog by remember { mutableStateOf(false) }
        var showAboutDialog by remember { mutableStateOf(false) }
        var shouldExit by remember { mutableStateOf(false) }
        var startupError by remember { mutableStateOf(initialDiagram.error) }
        fun hasUnsavedChanges() =
            erState.isModified ||
                relationalState.isModified ||
                useCaseState.isModified ||
                sequenceState.isModified ||
                classState.isModified ||
                logicState.isModified
        fun diagramState(type: DiagramType): DiagramEditorState<*> =
            when (type) {
                DiagramType.ER -> erState
                DiagramType.RELATIONAL -> relationalState
                DiagramType.CLASS_DIAGRAM -> classState
                DiagramType.USE_CASE -> useCaseState
                DiagramType.SEQUENCE -> sequenceState
                DiagramType.LOGIC -> logicState
            }
        fun requestExit() {
            if (hasUnsavedChanges()) {
                showExitDialog = true
            } else {
                exitApplication()
            }
        }
        if (shouldExit) {
            exitApplication()
        }
        Window(
            onCloseRequest = { requestExit() },
            title = "JDER - Java Diagrammi E/R",
            state = windowState,
            icon = painterResource("jder_icon.png"),
        ) {
            window.minimumSize = Dimension(800, 600)
            val isModified = hasUnsavedChanges()
            LaunchedEffect(isModified) {
                window.rootPane.putClientProperty("Window.documentModified", isModified)
            }
            LaunchedEffect(pendingNewDiagram, menuHost.commands) {
                val type = pendingNewDiagram
                val commands = menuHost.commands
                if (type != null && commands != null && commands.state === diagramState(type)) {
                    pendingNewDiagram = null
                    commands.onNewDiagram()
                }
            }
            val menus =
                appMenus(
                    commands = menuHost.commands,
                    themeState = themeState,
                    windowState = windowState,
                    currentType = currentType,
                    onTypeChange = { currentType = it },
                    onAbout = { showAboutDialog = true },
                    onExit = { requestExit() },
                    uriHandler = LocalUriHandler.current,
                )
            if (isMacOS) {
                AppMenuBar(menus = menus, onAbout = { showAboutDialog = true })
                DockMenu(
                    onNewDiagram = { type ->
                        currentType = type
                        pendingNewDiagram = type
                    }
                )
            }
            JDERTheme(
                darkTheme = themeState.isDarkTheme,
                colorPalette = themeState.selectedPalette,
            ) {
                Column(
                    modifier =
                        Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                            val typeIndex = diagramTypeKeys.indexOf(event.key)
                            when {
                                event.type != KeyEventType.KeyDown -> false
                                (event.isCtrlPressed || event.isMetaPressed) && typeIndex >= 0 -> {
                                    currentType = DiagramType.entries[typeIndex]
                                    true
                                }
                                !isMacOS && event.key == Key.F11 -> {
                                    toggleFullscreen(windowState)
                                    true
                                }
                                else -> false
                            }
                        }
                ) {
                    if (!isMacOS) {
                        WindowMenuBar(menus)
                    }
                    CompositionLocalProvider(LocalDiagramMenuHost provides menuHost) {
                        AppScreen(
                            erState = erState,
                            relationalState = relationalState,
                            useCaseState = useCaseState,
                            sequenceState = sequenceState,
                            classState = classState,
                            logicState = logicState,
                            erRepository = erRepository,
                            relationalRepository = relationalRepository,
                            useCaseRepository = useCaseRepository,
                            sequenceRepository = sequenceRepository,
                            classRepository = classRepository,
                            logicRepository = logicRepository,
                            themeState = themeState,
                            currentType = currentType,
                            onTypeChange = { currentType = it },
                            erPreviewFile = options.preview,
                        )
                    }
                }
                startupError?.let { error ->
                    AlertDialog(
                        onDismissRequest = { startupError = null },
                        title = { Text("Diagram could not be opened") },
                        text = { Text(error) },
                        confirmButton = {
                            TextButton(onClick = { startupError = null }) { Text("OK") }
                        },
                    )
                }
                if (showAboutDialog) {
                    AboutDialog(onDismiss = { showAboutDialog = false })
                }
                if (showExitDialog) {
                    AlertDialog(
                        onDismissRequest = { showExitDialog = false },
                        title = { Text("Conferma chiusura") },
                        text = { Text("Ci sono modifiche non salvate. Vuoi uscire comunque?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showExitDialog = false
                                    shouldExit = true
                                }
                            ) {
                                Text("Sì")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitDialog = false }) { Text("No") }
                        },
                    )
                }
            }
        }
    }
}
