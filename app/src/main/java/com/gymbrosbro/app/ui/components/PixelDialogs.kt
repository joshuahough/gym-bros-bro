package com.gymbrosbro.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gymbrosbro.app.ui.theme.GbbColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun PixelDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        PixelPanel(Modifier.fillMaxWidth(), fill = GbbColors.Night, border = GbbColors.Gold) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = GbbColors.Gold)
                content()
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissText: String = "CANCEL",
) {
    PixelDialog(title, onDismiss) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = GbbColors.Chalk)
        PixelButton(
            text = confirmText,
            onClick = onConfirm,
            color = if (destructive) GbbColors.Red else GbbColors.Gold,
            modifier = Modifier.fillMaxWidth(),
        )
        PixelButton(
            text = dismissText,
            onClick = onDismiss,
            color = GbbColors.Steel,
            contentColor = GbbColors.Chalk,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Single number entry, e.g. bodyweight. [onConfirm] gets the parsed value. */
@Composable
fun NumberInputDialog(
    title: String,
    unitLabel: String,
    initial: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
) {
    var text by remember { mutableStateOf(initial) }
    val parsed = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
    PixelDialog(title, onDismiss) {
        if (message != null) Text(message, style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelTextField(
                value = text,
                onValueChange = { text = it },
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
                modifier = Modifier.weight(1f),
            )
            Text(
                unitLabel,
                style = MaterialTheme.typography.titleLarge,
                color = GbbColors.Fog,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        PixelButton("SAVE", onClick = { parsed?.let(onConfirm) }, enabled = parsed != null, modifier = Modifier.fillMaxWidth())
        PixelButton("CANCEL", onClick = onDismiss, color = GbbColors.Steel, contentColor = GbbColors.Chalk, modifier = Modifier.fillMaxWidth())
    }
}

/** Date picker limited to today and earlier (no logging future workouts). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDatePickerDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayUtcMillis = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMillis
            override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) { Text("OK", color = GbbColors.Gold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = GbbColors.Fog) } },
    ) {
        DatePicker(state = state)
    }
}
