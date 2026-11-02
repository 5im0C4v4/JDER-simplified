package com.jder.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painterResource("jder_icon.png"),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )
        },
        title = { Text("JDER") },
        text = {
            Text(
                "Java Diagrammi E/R\n\n" +
                    "Editor per diagrammi Entità-Relazione.\n\n" +
                    "Riscrittura completa in Kotlin e Compose del software originale creato da " +
                    "Alessandro Ballini e successivamente mantenuto da Gianvito Pio.\n\n" +
                    "Licenza MIT",
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}
