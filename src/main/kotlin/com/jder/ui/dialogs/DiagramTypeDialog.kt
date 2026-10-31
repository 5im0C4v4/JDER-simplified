package com.jder.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jder.domain.model.DiagramType
import com.jder.ui.components.ToolbarPaletteTheme
import com.jder.ui.components.diagramTypeKeys
import com.jder.ui.components.logError
import com.jder.ui.theme.ThemeState
import com.jder.ui.utils.shortcutLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TYPES_PER_ROW = 3
private const val NO_BREAK_SPACE = '\u00A0'

@Composable
fun DiagramTypeDialog(themeState: ThemeState, onTypeSelected: (DiagramType) -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        Surface(
            modifier =
                Modifier.padding(24.dp)
                    .widthIn(max = 1000.dp)
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .onPreviewKeyEvent { event ->
                        val typeIndex = diagramTypeKeys.indexOf(event.key)
                        val isShortcut =
                            event.type == KeyEventType.KeyDown &&
                                (event.isCtrlPressed || event.isMetaPressed) &&
                                typeIndex >= 0
                        if (isShortcut) onTypeSelected(DiagramType.entries[typeIndex])
                        isShortcut
                    },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Scegli il tipo di diagramma",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    ToolbarPaletteTheme(themeState)
                }
                DiagramType.entries.chunked(TYPES_PER_ROW).forEach { rowTypes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        rowTypes.forEach { type ->
                            DiagramTypeCard(
                                type = type,
                                isDarkTheme = themeState.isDarkTheme,
                                onClick = { onTypeSelected(type) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagramTypeCard(
    type: DiagramType,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenshotPath = "screenshots/${type.key}-${if (isDarkTheme) "dark" else "light"}.png"
    val screenshot by
        produceState<ImageBitmap?>(null, screenshotPath) {
            value = withContext(Dispatchers.IO) { loadScreenshot(screenshotPath) }
        }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Surface(
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border =
            BorderStroke(
                width = if (isHovered) 2.dp else 1.dp,
                color =
                    if (isHovered) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
            ),
        interactionSource = interactionSource,
    ) {
        Column {
            val imageModifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            val image = screenshot
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Medium,
                )
            } else {
                Box(modifier = imageModifier.background(MaterialTheme.colorScheme.surfaceVariant))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = type.tabLabel.replace(' ', NO_BREAK_SPACE),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = shortcutLabel("${type.ordinal + 1}"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun loadScreenshot(path: String): ImageBitmap? =
    try {
        useResource(path, ::loadImageBitmap)
    } catch (e: Exception) {
        logError("Impossibile caricare l'anteprima $path", e)
        null
    }
