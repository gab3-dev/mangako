package com.gabedev.mangako.ui.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDialog(
    title: String = "Confirmação",
    text: String = "Tem certeza que deseja continuar?",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = text)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}