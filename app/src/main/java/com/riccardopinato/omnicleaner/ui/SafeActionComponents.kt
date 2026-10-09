package com.riccardopinato.omnicleaner.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun ConfirmingActionButton(
    label: String,
    message: String,
    enabled: Boolean = true,
    onConfirmed: () -> Unit,
) {
    var showConfirmation by rememberSaveable { mutableStateOf(false) }

    Button(
        enabled = enabled,
        onClick = { showConfirmation = true },
    ) {
        Text(label)
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Conferma operazione") },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmation = false
                        onConfirmed()
                    },
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmation = false }) {
                    Text("Annulla")
                }
            },
        )
    }
}
