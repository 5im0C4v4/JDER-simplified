package com.jder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jder.ui.strings.UiStrings

class PendingRelation(val sourceId: String, val targetId: String)

@Composable
fun <T> RadioOptionList(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String,
) {
    Column(modifier) {
        options.forEach { option ->
            val isSelected = option == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(option) },
                            role = Role.RadioButton,
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                    color =
                        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun ConfirmNewDiagramDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conferma nuovo diagramma") },
        text = { Text("Ci sono modifiche non salvate. Vuoi creare un nuovo diagramma comunque?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text(UiStrings.YES) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(UiStrings.NO) } },
    )
}

@Composable
fun ConfirmOpenDiagramDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conferma apertura diagramma") },
        text = { Text("Ci sono modifiche non salvate. Vuoi aprire un altro diagramma comunque?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text(UiStrings.YES) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(UiStrings.NO) } },
    )
}
