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
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jder.data.ERDiagramRepository
import com.jder.domain.model.ERDiagramState
import com.jder.ui.components.AppMenuBar
import com.jder.ui.components.DiagramMenuHost
import com.jder.ui.components.LocalDiagramMenuHost
import com.jder.ui.components.ToastHost
import com.jder.ui.components.Toasts
import com.jder.ui.components.WindowMenuBar
import com.jder.ui.components.appMenus
import com.jder.ui.components.lastUsedDirectory
import com.jder.ui.components.toggleFullscreen
import com.jder.ui.dialogs.AboutDialog
import com.jder.ui.screens.AppScreen
import com.jder.ui.theme.JDERTheme
import com.jder.ui.theme.ThemeState
import com.jder.ui.utils.isMacOS
import com.jder.ui.utils.setDockIcon
import java.awt.Dimension

fun main(args: Array<String>) {
    val options = parseCommandLine(args)
    if (options.showHelp) {
        println(COMMAND_LINE_USAGE)
        return
    }
    options.warnings.forEach(Toasts::warning)
    val erState = ERDiagramState()
    val erRepository = ERDiagramRepository()
    prepareStartupDiagram(options) { startupOptions ->
        openStartupDiagram(erState, erRepository, startupOptions)
    }
    (options.jsonFile ?: options.pngFile)?.parentFile?.let { lastUsedDirectory = it }
    System.setProperty("apple.awt.application.name", "JDER")
    System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS")
    System.setProperty("compose.application.configure.swing.globals", "true")
    setDockIcon("jder_icon.png")
    application {
        val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
        val themeState = remember { ThemeState() }
        val menuHost = remember { DiagramMenuHost() }
        var showExitDialog by remember { mutableStateOf(false) }
        var showAboutDialog by remember { mutableStateOf(false) }
        var shouldExit by remember { mutableStateOf(false) }
        fun hasUnsavedChanges() = erState.isModified
        fun requestExit() {
            val isFlushed = menuHost.commands?.flushAutoSave?.invoke() != false
            if (!isFlushed || hasUnsavedChanges()) {
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
            val menus =
                appMenus(
                    commands = menuHost.commands,
                    themeState = themeState,
                    windowState = windowState,
                    onAbout = { showAboutDialog = true },
                    onExit = { requestExit() },
                    uriHandler = LocalUriHandler.current,
                )
            if (isMacOS) {
                AppMenuBar(menus = menus, onAbout = { showAboutDialog = true })
            }
            JDERTheme(
                darkTheme = themeState.isDarkTheme,
                colorPalette = themeState.selectedPalette,
            ) {
                Column(
                    modifier =
                        Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                            when {
                                event.type != KeyEventType.KeyDown -> false
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
                            state = erState,
                            repository = erRepository,
                            themeState = themeState,
                        )
                    }
                }
                ToastHost(isDarkTheme = themeState.isDarkTheme)
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
