package com.jder.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jder.domain.model.DiagramEditorState
import com.jder.ui.theme.ThemeState
import com.jder.ui.utils.isMacOS
import com.jder.ui.utils.shortcutLabel
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagramToolbar(
    state: DiagramEditorState<*>,
    themeState: ThemeState,
    onNewDiagram: () -> Unit,
    onOpenDiagram: () -> Unit,
    onSaveDiagram: () -> Unit,
    onSaveAsDiagram: () -> Unit,
    onToggleAutoSave: () -> Unit,
    onExportPNG: () -> Unit,
    onToggleAutoExport: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDeleteSelected: () -> Unit,
    tools: List<DiagramTool>,
    modifier: Modifier = Modifier,
) {
    val title = buildString {
        append("JDER - ")
        append(state.currentFile?.let { File(it).name } ?: state.diagramName)
        if (state.isModified) append(" *")
    }
    val hasSelection = state.hasSelection
    Column(modifier = modifier) {
        TopAppBar(
            title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            actions = {
                if (isMacOS) {
                    ToolbarFileMenu(
                        onNewDiagram = onNewDiagram,
                        onOpenDiagram = onOpenDiagram,
                        onSaveDiagram = onSaveDiagram,
                        onSaveAsDiagram = onSaveAsDiagram,
                        autoSave = autoSaveToggle(state, onToggleAutoSave),
                        onExportPNG = onExportPNG,
                        autoExport = autoExportToggle(state, onToggleAutoExport),
                        hasSelection = hasSelection,
                        onDeleteSelected = onDeleteSelected,
                    )
                    ToolbarViewMenuBox(
                        onZoomIn = state::zoomIn,
                        onZoomOut = state::zoomOut,
                        onResetZoom = state::resetView,
                    )
                    ToolbarExportMenu(
                        onExportPNG = onExportPNG,
                        autoExport = autoExportToggle(state, onToggleAutoExport),
                    )
                }
                ToolbarPaletteTheme(themeState = themeState)
            },
        )
        Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToolbarToggleButton(
                        icon = Icons.Default.NearMe,
                        label = "Seleziona e Sposta (Esc)",
                        checked = state.isSelectTool,
                        onClick = state::resetToolMode,
                    )
                    ToolbarVerticalDivider()
                    ToolbarIconButton(
                        icon = Icons.Default.Undo,
                        label = "Annulla (${shortcutLabel("Z")})",
                        onClick = onUndo,
                        enabled = state.canUndo(),
                    )
                    ToolbarIconButton(
                        icon = Icons.Default.Redo,
                        label = "Ripristina (${shortcutLabel("Y")})",
                        onClick = onRedo,
                        enabled = state.canRedo(),
                    )
                    ToolbarVerticalDivider()
                    tools.forEach { ToolbarTool(it) }
                    ToolbarVerticalDivider()
                    ToolbarIconButton(
                        icon = Icons.Default.Save,
                        label = "Salva (${shortcutLabel("S")})",
                        onClick = onSaveDiagram,
                    )
                    ToolbarIconButton(
                        icon = Icons.Default.Delete,
                        label = "Elimina Elemento Selezionato (${if (isMacOS) "⌫" else "Canc"})",
                        onClick = onDeleteSelected,
                        enabled = hasSelection,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                ToolbarZoomControls(
                    zoomPercentage = "${(state.zoom * 100).roundToInt()}%",
                    onZoomIn = state::zoomIn,
                    onZoomOut = state::zoomOut,
                    onResetZoom = state::resetView,
                )
            }
        }
    }
}
