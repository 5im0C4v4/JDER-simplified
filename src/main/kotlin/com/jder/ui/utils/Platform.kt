package com.jder.ui.utils

import androidx.compose.ui.platform.UriHandler
import com.jder.ui.components.Toasts
import com.jder.ui.components.logError
import java.awt.Taskbar
import javax.imageio.ImageIO

val isMacOS: Boolean = System.getProperty("os.name").orEmpty().lowercase().startsWith("mac")

fun shortcutLabel(key: String, hasShift: Boolean = false): String =
    if (isMacOS) "⌘${if (hasShift) "⇧" else ""}$key"
    else "Ctrl+${if (hasShift) "Shift+" else ""}$key"

fun setDockIcon(resourcePath: String) {
    if (!isMacOS) return
    try {
        Taskbar.getTaskbar().iconImage = ImageIO.read(ClassLoader.getSystemResource(resourcePath))
    } catch (e: Exception) {
        logError("Impossibile impostare l'icona nel Dock", e)
    }
}

fun UriHandler.openSafely(url: String) {
    try {
        openUri(url)
    } catch (e: Exception) {
        Toasts.error("Impossibile aprire $url", e)
    }
}
