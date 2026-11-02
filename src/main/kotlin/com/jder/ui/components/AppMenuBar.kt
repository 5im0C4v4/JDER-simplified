package com.jder.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuScope
import java.awt.Desktop

@Composable
fun FrameWindowScope.AppMenuBar(menus: List<AppMenu>, onAbout: () -> Unit) {
    val currentMenus by rememberUpdatedState(menus)
    val currentOnAbout by rememberUpdatedState(onAbout)
    DisposableEffect(Unit) {
        val desktop = Desktop.getDesktop()
        val isSupported = desktop.isSupported(Desktop.Action.APP_ABOUT)
        if (isSupported) desktop.setAboutHandler { currentOnAbout() }
        onDispose { if (isSupported) desktop.setAboutHandler(null) }
    }
    MenuBar {
        currentMenus.forEach { menu ->
            key(menu.title) { Menu(menu.title) { menu.entries().forEach { MenuEntry(it) } } }
        }
    }
}

@Composable
private fun MenuScope.MenuEntry(entry: AppMenuEntry) {
    when (entry) {
        is AppMenuEntry.Action ->
            Item(
                entry.label,
                enabled = entry.isEnabled,
                shortcut = entry.shortcut?.keyShortcut,
                onClick = entry.onClick,
            )
        is AppMenuEntry.Toggle ->
            CheckboxItem(
                entry.label,
                checked = entry.isChecked,
                enabled = entry.isEnabled,
                shortcut = entry.shortcut?.keyShortcut,
                onCheckedChange = { entry.onToggle() },
            )
        is AppMenuEntry.Choice ->
            RadioButtonItem(
                entry.label,
                selected = entry.isSelected,
                shortcut = entry.shortcut?.keyShortcut,
                onClick = entry.onSelect,
            )
        is AppMenuEntry.Submenu -> Menu(entry.label) { entry.entries.forEach { MenuEntry(it) } }
        AppMenuEntry.Separator -> Separator()
    }
}

private val MenuShortcut.keyShortcut: KeyShortcut
    get() = KeyShortcut(key, ctrl = isControl, meta = isCommand, alt = isAlt, shift = isShift)
