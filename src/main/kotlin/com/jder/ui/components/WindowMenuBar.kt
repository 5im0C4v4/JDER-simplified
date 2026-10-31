package com.jder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val menuItemHeight = 36.dp

private val MenuShortcut.label: String
    get() = buildString {
        if (isCommand || isControl) append("Ctrl+")
        if (isAlt) append("Alt+")
        if (isShift) append("Shift+")
        append(keyLabel)
    }

@Composable
fun WindowMenuBar(menus: List<AppMenu>, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    var openMenuTitle by remember { mutableStateOf<String?>(null) }
    fun runAction(action: () -> Unit) {
        coroutineScope.launch {
            delay(150.milliseconds)
            openMenuTitle = null
            action()
        }
    }
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                menus.forEach { menu ->
                    key(menu.title) {
                        Box {
                            TextButton(
                                onClick = { openMenuTitle = menu.title },
                                modifier =
                                    Modifier.height(32.dp).focusProperties { canFocus = false },
                                shape = MaterialTheme.shapes.small,
                                colors =
                                    ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                            ) {
                                Text(menu.title, style = MaterialTheme.typography.labelLarge)
                            }
                            DropdownMenu(
                                expanded = openMenuTitle == menu.title,
                                onDismissRequest = { openMenuTitle = null },
                            ) {
                                WindowMenuEntries(menu.entries()) { runAction(it) }
                            }
                        }
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun WindowMenuEntries(entries: List<AppMenuEntry>, onAction: (() -> Unit) -> Unit) {
    val hasChecks = entries.any { it is AppMenuEntry.Toggle || it is AppMenuEntry.Choice }
    entries.forEach { entry ->
        when (entry) {
            is AppMenuEntry.Action ->
                WindowMenuItem(
                    entry.label,
                    entry.shortcut,
                    hasChecks,
                    isChecked = false,
                    isEnabled = entry.isEnabled,
                ) {
                    onAction(entry.onClick)
                }
            is AppMenuEntry.Toggle ->
                WindowMenuItem(
                    entry.label,
                    entry.shortcut,
                    hasChecks,
                    entry.isChecked,
                    isEnabled = entry.isEnabled,
                ) {
                    onAction(entry.onToggle)
                }
            is AppMenuEntry.Choice ->
                WindowMenuItem(entry.label, entry.shortcut, hasChecks, entry.isSelected) {
                    onAction(entry.onSelect)
                }
            is AppMenuEntry.Submenu -> WindowSubmenu(entry, hasChecks, onAction)
            AppMenuEntry.Separator -> Divider()
        }
    }
}

@Composable
private fun WindowMenuItem(
    label: String,
    shortcut: MenuShortcut?,
    hasChecks: Boolean,
    isChecked: Boolean,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        modifier = Modifier.height(menuItemHeight),
        enabled = isEnabled,
        leadingIcon = if (hasChecks) ({ WindowMenuCheck(isChecked) }) else null,
        trailingIcon =
            shortcut?.let {
                {
                    Text(
                        it.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
    )
}

@Composable
private fun WindowSubmenu(
    submenu: AppMenuEntry.Submenu,
    hasChecks: Boolean,
    onAction: (() -> Unit) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    Box(modifier = Modifier.onSizeChanged { itemSize = it }) {
        DropdownMenuItem(
            text = { Text(submenu.label) },
            onClick = { isExpanded = true },
            modifier = Modifier.height(menuItemHeight),
            leadingIcon = if (hasChecks) ({ WindowMenuCheck(isChecked = false) }) else null,
            trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            offset = with(density) { DpOffset(itemSize.width.toDp(), -itemSize.height.toDp()) },
        ) {
            WindowMenuEntries(submenu.entries, onAction)
        }
    }
}

@Composable
private fun WindowMenuCheck(isChecked: Boolean) {
    if (isChecked) {
        Icon(Icons.Default.Check, contentDescription = null)
    } else {
        Spacer(modifier = Modifier.size(24.dp))
    }
}
