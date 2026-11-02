package com.gymbrosbro.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.ui.theme.GbbColors

/**
 * A time as separate minute and second boxes on the number pad. Seconds can
 * run past 59, so 90 s or 1 min 30 s both mean the same thing. Blank = null.
 * The boxes keep what you typed and only resync from [seconds] while neither
 * is focused, so 90 doesn't turn into 1:30 under your thumb (and a stale echo
 * of a half-typed value can't overwrite the boxes).
 */
@Composable
fun PixelDurationField(
    seconds: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
    fill: Color = GbbColors.Night,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    var min by remember { mutableStateOf(minText(seconds)) }
    var sec by remember { mutableStateOf(secText(seconds)) }
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(seconds, editing) {
        if (!editing && total(min, sec) != seconds) {
            min = minText(seconds)
            sec = secText(seconds)
        }
    }
    Row(
        modifier.onFocusChanged { editing = it.hasFocus },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelTextField(
            value = min,
            onValueChange = { v ->
                min = v.filter(Char::isDigit).take(3)
                onChange(total(min, sec))
            },
            modifier = Modifier.weight(1f),
            placeholder = "0",
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
            textAlign = TextAlign.Center,
            fill = fill,
            textStyle = textStyle,
            horizontalPadding = 4.dp,
        )
        Text(":", style = textStyle, color = GbbColors.Fog)
        PixelTextField(
            value = sec,
            onValueChange = { v ->
                sec = v.filter(Char::isDigit).take(4)
                onChange(total(min, sec))
            },
            modifier = Modifier.weight(1f),
            placeholder = "00",
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
            textAlign = TextAlign.Center,
            fill = fill,
            textStyle = textStyle,
            horizontalPadding = 4.dp,
        )
    }
}

private fun total(min: String, sec: String): Int? =
    if (min.isEmpty() && sec.isEmpty()) null else (min.toIntOrNull() ?: 0) * 60 + (sec.toIntOrNull() ?: 0)

private fun minText(seconds: Int?): String = seconds?.let { it / 60 }?.takeIf { it > 0 }?.toString().orEmpty()

private fun secText(seconds: Int?): String = when {
    seconds == null -> ""
    seconds >= 60 -> (seconds % 60).toString().padStart(2, '0')
    else -> seconds.toString()
}
