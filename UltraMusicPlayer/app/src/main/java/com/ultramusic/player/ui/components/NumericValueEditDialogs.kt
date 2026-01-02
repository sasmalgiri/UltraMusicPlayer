package com.ultramusic.player.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun FloatValueEditDialog(
    title: String,
    initialValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    decimals: Int,
    suffix: String = "",
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialValue) {
        text = String.format("%.${decimals}f", initialValue)
        error = null
    }

    fun parseOrNull(input: String): Float? = input.trim().toFloatOrNull()

    fun validate(input: String): Float? {
        val parsed = parseOrNull(input) ?: return null
        if (parsed.isNaN() || parsed.isInfinite()) return null
        return parsed.coerceIn(valueRange.start, valueRange.endInclusive)
    }

    val validated = validate(text)
    error = if (text.isBlank()) null else if (validated == null) "Enter a number" else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text("Value") },
                    supportingText = {
                        val rangeText = "Range: ${formatFloat(valueRange.start, decimals)} .. ${formatFloat(valueRange.endInclusive, decimals)}${suffix}"
                        Text(rangeText)
                    },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { validated?.let(onConfirm) },
                enabled = validated != null
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun IntValueEditDialog(
    title: String,
    initialValue: Int,
    valueRange: IntRange,
    suffix: String = "",
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialValue) {
        text = initialValue.toString()
        error = null
    }

    fun validate(input: String): Int? {
        val parsed = input.trim().toIntOrNull() ?: return null
        return parsed.coerceIn(valueRange.first, valueRange.last)
    }

    val validated = validate(text)
    error = if (text.isBlank()) null else if (validated == null) "Enter a whole number" else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Value") },
                    supportingText = {
                        Text("Range: ${valueRange.first} .. ${valueRange.last}${suffix}")
                    },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { validated?.let(onConfirm) },
                enabled = validated != null
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatFloat(value: Float, decimals: Int): String = String.format("%.${decimals}f", value)
