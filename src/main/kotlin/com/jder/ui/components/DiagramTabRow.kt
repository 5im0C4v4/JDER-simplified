package com.jder.ui.components

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import com.jder.domain.model.DiagramType

private const val WORD_JOINER = '\u2060'

@Composable
fun DiagramTabRow(
    currentType: DiagramType,
    onTypeChange: (DiagramType) -> Unit,
    isModified: (DiagramType) -> Boolean,
    modifier: Modifier = Modifier,
) {
    TabRow(selectedTabIndex = currentType.ordinal, modifier = modifier) {
        DiagramType.entries.forEach { type ->
            val label = type.tabLabel.replace("/", "/$WORD_JOINER")
            Tab(
                selected = currentType == type,
                onClick = { onTypeChange(type) },
                modifier = Modifier.focusProperties { canFocus = false },
                text = { Text(if (isModified(type)) "$label •" else label) },
            )
        }
    }
}
