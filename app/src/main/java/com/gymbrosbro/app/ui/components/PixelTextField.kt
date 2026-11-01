package com.gymbrosbro.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.ui.theme.GbbColors
import com.gymbrosbro.app.ui.theme.PixelDimens

/** Text input in a pixel frame; the border turns gold while focused. */
@Composable
fun PixelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    fill: Color = GbbColors.Night,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border = if (focused) GbbColors.Gold else GbbColors.Steel
    val alignment = if (textAlign == TextAlign.Center) Alignment.Center else Alignment.CenterStart

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .heightIn(min = 44.dp)
            .drawBehind {
                val u = PixelDimens.unit.toPx()
                drawNotchedRect(border, 0f, 0f, size.width, size.height, u)
                drawNotchedRect(fill, u, u, size.width - u, size.height - u, u)
            },
        textStyle = textStyle.copy(color = GbbColors.Chalk, textAlign = textAlign),
        cursorBrush = SolidColor(GbbColors.Gold),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Words else KeyboardCapitalization.None,
        ),
        singleLine = singleLine,
        interactionSource = interaction,
        decorationBox = { inner ->
            Box(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                contentAlignment = alignment,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(textAlign = textAlign),
                        color = GbbColors.Steel,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                    )
                }
                inner()
            }
        },
    )
}
