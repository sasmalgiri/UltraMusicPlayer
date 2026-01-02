package com.ultramusic.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.UltraMusicApp

/**
 * Value type for EditableValueBox
 * Supports both integer and float values with validation
 */
sealed class ValueType {
    /**
     * Integer value with range validation
     */
    data class IntValue(
        val value: Int,
        val min: Int,
        val max: Int,
        val suffix: String = ""
    ) : ValueType()

    /**
     * Float value with range validation and decimal precision
     */
    data class FloatValue(
        val value: Float,
        val min: Float,
        val max: Float,
        val decimals: Int = 2,
        val suffix: String = ""
    ) : ValueType()
}

/**
 * Editable Value Box
 *
 * A value display that can be tapped to edit manually.
 * Shows the value normally, opens text field on tap.
 * Validates input within the specified range.
 * Applies value on Enter/Done, reverts on cancel.
 *
 * @param valueType The value type with current value, range, and formatting
 * @param displayText Custom display text (if null, uses default formatting)
 * @param color The color for the value text and background
 * @param onValueChange Callback when value changes
 * @param modifier Modifier for the composable
 */
@Composable
fun EditableValueBox(
    valueType: ValueType,
    displayText: String? = null,
    color: Color,
    onValueChange: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var textFieldValue by remember(valueType) {
        mutableStateOf(TextFieldValue(getInitialText(valueType)))
    }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Request focus when entering edit mode
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
            // Select all text for easy replacement
            textFieldValue = textFieldValue.copy(
                selection = TextRange(0, textFieldValue.text.length)
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isEditing) {
                    isEditing = true
                }
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isEditing,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "edit_transition"
        ) { editing ->
            if (editing) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        // Filter to only valid characters
                        val filtered = filterInput(newValue.text, valueType)
                        textFieldValue = newValue.copy(text = filtered)
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            if (!state.isFocused && isEditing) {
                                // Lost focus - apply value
                                applyValue(textFieldValue.text, valueType, onValueChange)
                                isEditing = false
                            }
                        }
                        .widthIn(min = 60.dp, max = 120.dp)
                        .width(IntrinsicSize.Min),
                    textStyle = LocalTextStyle.current.copy(
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when (valueType) {
                            is ValueType.IntValue -> KeyboardType.Number
                            is ValueType.FloatValue -> KeyboardType.Decimal
                        },
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            applyValue(textFieldValue.text, valueType, onValueChange)
                            isEditing = false
                            focusManager.clearFocus()
                        }
                    ),
                    cursorBrush = SolidColor(color)
                )
            } else {
                Text(
                    text = displayText ?: formatValue(valueType),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

private fun getInitialText(valueType: ValueType): String {
    return when (valueType) {
        is ValueType.IntValue -> valueType.value.toString()
        is ValueType.FloatValue -> String.format("%.${valueType.decimals}f", valueType.value)
    }
}

private fun formatValue(valueType: ValueType): String {
    return when (valueType) {
        is ValueType.IntValue -> "${valueType.value}${valueType.suffix}"
        is ValueType.FloatValue -> String.format("%.${valueType.decimals}f${valueType.suffix}", valueType.value)
    }
}

private fun filterInput(input: String, valueType: ValueType): String {
    return when (valueType) {
        is ValueType.IntValue -> input.filter { it.isDigit() || it == '-' }
        is ValueType.FloatValue -> input.filter { it.isDigit() || it == '.' || it == '-' }
    }
}

private fun applyValue(text: String, valueType: ValueType, onValueChange: (Any) -> Unit) {
    when (valueType) {
        is ValueType.IntValue -> {
            val parsed = text.toIntOrNull() ?: valueType.value
            val clamped = parsed.coerceIn(valueType.min, valueType.max)
            onValueChange(clamped)
        }
        is ValueType.FloatValue -> {
            val parsed = text.toFloatOrNull() ?: valueType.value
            val clamped = parsed.coerceIn(valueType.min, valueType.max)
            onValueChange(clamped)
        }
    }
}

// ==================== CONVENIENCE COMPOSABLES ====================

/**
 * Editable value box for speed (0.05x - 10x)
 */
@Composable
fun EditableSpeedBox(
    speed: Float,
    color: Color,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        FloatValueEditDialog(
            title = "Edit Speed",
            initialValue = speed,
            valueRange = UltraMusicApp.MIN_SPEED..UltraMusicApp.MAX_SPEED,
            decimals = 2,
            suffix = "x",
            onDismiss = { showEditDialog = false },
            onConfirm = {
                showEditDialog = false
                onSpeedChange(it)
            }
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${String.format("%.2f", speed)}x",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        IconButton(onClick = { showEditDialog = true }) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit Speed", tint = color)
        }
    }
}

/**
 * Editable value box for pitch (semitones)
 */
@Composable
fun EditablePitchBox(
    pitch: Float,
    color: Color,
    onPitchChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val sign = if (pitch > 0) "+" else ""

    if (showEditDialog) {
        FloatValueEditDialog(
            title = "Edit Pitch",
            initialValue = pitch,
            valueRange = UltraMusicApp.MIN_PITCH_SEMITONES..UltraMusicApp.MAX_PITCH_SEMITONES,
            decimals = 1,
            suffix = " st",
            onDismiss = { showEditDialog = false },
            onConfirm = {
                showEditDialog = false
                onPitchChange(it)
            }
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${sign}${String.format("%.1f", pitch)} semitones",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        IconButton(onClick = { showEditDialog = true }) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit Pitch", tint = color)
        }
    }
}

/**
 * Editable value box for percentage (0-100 or custom max)
 */
@Composable
fun EditablePercentBox(
    value: Int,
    color: Color,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 100
) {
    EditableValueBox(
        valueType = ValueType.IntValue(
            value = value,
            min = min,
            max = max,
            suffix = "%"
        ),
        color = color,
        onValueChange = { onValueChange(it as Int) },
        modifier = modifier
    )
}

/**
 * Editable value box for dB values
 */
@Composable
fun EditableDbBox(
    value: Float,
    min: Float,
    max: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    EditableValueBox(
        valueType = ValueType.FloatValue(
            value = value,
            min = min,
            max = max,
            decimals = 1,
            suffix = "dB"
        ),
        color = color,
        onValueChange = { onValueChange(it as Float) },
        modifier = modifier
    )
}

/**
 * Editable value box for generic integer range (0-1000, etc.)
 */
@Composable
fun EditableIntBox(
    value: Int,
    min: Int,
    max: Int,
    color: Color,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String = ""
) {
    EditableValueBox(
        valueType = ValueType.IntValue(
            value = value,
            min = min,
            max = max,
            suffix = suffix
        ),
        color = color,
        onValueChange = { onValueChange(it as Int) },
        modifier = modifier
    )
}

/**
 * Editable value box for generic float range
 */
@Composable
fun EditableFloatBox(
    value: Float,
    min: Float,
    max: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    decimals: Int = 1,
    suffix: String = ""
) {
    EditableValueBox(
        valueType = ValueType.FloatValue(
            value = value,
            min = min,
            max = max,
            decimals = decimals,
            suffix = suffix
        ),
        color = color,
        onValueChange = { onValueChange(it as Float) },
        modifier = modifier
    )
}

/**
 * Editable value box for frequency (Hz)
 */
@Composable
fun EditableFrequencyBox(
    value: Float,
    min: Float,
    max: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    EditableValueBox(
        valueType = ValueType.FloatValue(
            value = value,
            min = min,
            max = max,
            decimals = 0,
            suffix = "Hz"
        ),
        color = color,
        onValueChange = { onValueChange(it as Float) },
        modifier = modifier
    )
}

/**
 * Editable value box for ratio values (compressor ratio, etc.)
 */
@Composable
fun EditableRatioBox(
    value: Float,
    min: Float,
    max: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    EditableValueBox(
        valueType = ValueType.FloatValue(
            value = value,
            min = min,
            max = max,
            decimals = 1,
            suffix = ":1"
        ),
        color = color,
        onValueChange = { onValueChange(it as Float) },
        modifier = modifier
    )
}

/**
 * Editable value box for time values (ms)
 */
@Composable
fun EditableTimeBox(
    value: Float,
    min: Float,
    max: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    EditableValueBox(
        valueType = ValueType.FloatValue(
            value = value,
            min = min,
            max = max,
            decimals = 1,
            suffix = "ms"
        ),
        color = color,
        onValueChange = { onValueChange(it as Float) },
        modifier = modifier
    )
}
