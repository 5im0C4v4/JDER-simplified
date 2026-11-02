package com.jder.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jder.ui.strings.UiStrings
import com.jder.ui.theme.ThemeState
import com.jder.ui.utils.shortcutLabel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WithTooltip(text: String, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        },
        delayMillis = 500,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
    ) {
        content()
    }
}

@Composable
fun ToolbarToggleButton(icon: ImageVector, label: String, checked: Boolean, onClick: () -> Unit) {
    WithTooltip(label) {
        IconToggleButton(
            checked = checked,
            onCheckedChange = { onClick() },
            colors =
                IconButtonDefaults.iconToggleButtonColors(
                    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
        ) {
            Icon(icon, label)
        }
    }
}

@Composable
fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
) {
    WithTooltip(label) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                icon,
                label,
                tint =
                    if (enabled) tint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
    }
}

@Composable
fun ToolbarTool(tool: DiagramTool) {
    when (tool) {
        is DiagramTool.Mode ->
            ToolbarToggleButton(tool.icon, tool.label, tool.isSelected, tool.onSelect)
        is DiagramTool.Toggle ->
            ToolbarToggleButton(tool.icon, tool.label, tool.isChecked, tool.onToggle)
        is DiagramTool.Action -> ToolbarIconButton(tool.icon, tool.label, tool.onClick)
        is DiagramTool.Group -> tool.tools.forEach { ToolbarTool(it) }
        DiagramTool.Divider -> ToolbarVerticalDivider()
    }
}

@Composable
private fun ToolbarMenuItem(
    text: String,
    icon: ImageVector,
    shortcut: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        leadingIcon = { Icon(icon, null) },
        trailingIcon =
            shortcut?.let {
                {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        enabled = enabled,
    )
}

@Composable
private fun ToolbarMenuToggleItem(toggle: AppMenuEntry.Toggle, onClick: () -> Unit) {
    ToolbarMenuItem(
        toggle.label,
        if (toggle.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
        enabled = toggle.isEnabled,
        onClick = onClick,
    )
}

@Composable
fun ToolbarFileMenu(
    onNewDiagram: () -> Unit,
    onOpenDiagram: () -> Unit,
    onSaveDiagram: () -> Unit,
    onSaveAsDiagram: () -> Unit,
    autoSave: AppMenuEntry.Toggle,
    onExportPNG: () -> Unit,
    autoExport: AppMenuEntry.Toggle,
    hasSelection: Boolean,
    onDeleteSelected: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var showFileMenu by remember { mutableStateOf(false) }
    fun runAction(action: () -> Unit) {
        coroutineScope.launch {
            delay(150.milliseconds)
            showFileMenu = false
            action()
        }
    }
    Box {
        TextButton(onClick = { showFileMenu = true }) { Text("File") }
        DropdownMenu(expanded = showFileMenu, onDismissRequest = { showFileMenu = false }) {
            ToolbarMenuItem("Nuovo Diagramma", Icons.Default.Add, shortcutLabel("N")) {
                runAction(onNewDiagram)
            }
            ToolbarMenuItem("Apri Diagramma...", Icons.Default.FolderOpen, shortcutLabel("O")) {
                runAction(onOpenDiagram)
            }
            Divider()
            ToolbarMenuItem(UiStrings.SAVE, Icons.Default.Save, shortcutLabel("S")) {
                runAction(onSaveDiagram)
            }
            ToolbarMenuItem(
                "Salva con nome...",
                Icons.Default.Save,
                shortcutLabel("S", hasShift = true),
            ) {
                runAction(onSaveAsDiagram)
            }
            ToolbarMenuToggleItem(autoSave) { runAction(autoSave.onToggle) }
            ToolbarMenuItem("Esporta come PNG...", Icons.Default.Image, shortcutLabel("E")) {
                runAction(onExportPNG)
            }
            ToolbarMenuToggleItem(autoExport) { runAction(autoExport.onToggle) }
            Divider()
            ToolbarMenuItem(
                "Elimina Elemento Selezionato",
                Icons.Default.Delete,
                enabled = hasSelection,
            ) {
                runAction(onDeleteSelected)
            }
        }
    }
}

@Composable
fun ToolbarExportMenu(onExportPNG: () -> Unit, autoExport: AppMenuEntry.Toggle) {
    val coroutineScope = rememberCoroutineScope()
    var showExportMenu by remember { mutableStateOf(false) }
    fun runAction(action: () -> Unit) {
        coroutineScope.launch {
            delay(150.milliseconds)
            showExportMenu = false
            action()
        }
    }
    Box {
        TextButton(onClick = { showExportMenu = true }) { Text("Esporta") }
        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
            ToolbarMenuItem("Esporta come PNG...", Icons.Default.Image, shortcutLabel("E")) {
                runAction(onExportPNG)
            }
            ToolbarMenuToggleItem(autoExport) { runAction(autoExport.onToggle) }
        }
    }
}

@Composable
fun ToolbarViewMenuBox(onZoomIn: () -> Unit, onZoomOut: () -> Unit, onResetZoom: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var showViewMenu by remember { mutableStateOf(false) }
    fun runAction(action: () -> Unit) {
        coroutineScope.launch {
            delay(150.milliseconds)
            showViewMenu = false
            action()
        }
    }
    Box {
        TextButton(onClick = { showViewMenu = true }) { Text("Vista") }
        DropdownMenu(expanded = showViewMenu, onDismissRequest = { showViewMenu = false }) {
            ToolbarMenuItem("Zoom In", Icons.Default.ZoomIn, shortcutLabel("+")) {
                runAction(onZoomIn)
            }
            ToolbarMenuItem("Zoom Out", Icons.Default.ZoomOut, shortcutLabel("-")) {
                runAction(onZoomOut)
            }
            ToolbarMenuItem("Reimposta Zoom", Icons.Default.CenterFocusWeak, shortcutLabel("0")) {
                runAction(onResetZoom)
            }
        }
    }
}

@Composable
fun ToolbarPaletteTheme(themeState: ThemeState) {
    PaletteSelector(
        selectedPalette = themeState.selectedPalette,
        onPaletteSelected = themeState::selectPalette,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    ThemeToggleButton(
        isDarkTheme = themeState.isDarkTheme,
        onToggle = themeState::toggleTheme,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
fun ToolbarZoomControls(
    zoomPercentage: String,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
) {
    Text(
        text = zoomPercentage,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
    ToolbarIconButton(Icons.Default.ZoomOut, "Zoom Out (${shortcutLabel("-")})", onZoomOut)
    ToolbarIconButton(
        Icons.Default.CenterFocusWeak,
        "Reimposta Zoom (${shortcutLabel("0")})",
        onResetZoom,
    )
    ToolbarIconButton(Icons.Default.ZoomIn, "Zoom In (${shortcutLabel("+")})", onZoomIn)
}

@Composable
fun ToolbarVerticalDivider() {
    Box(
        modifier =
            Modifier.height(32.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant)
    )
}
