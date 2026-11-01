package com.gymbrosbro.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymbrosbro.app.sprites.SpriteImage
import com.gymbrosbro.app.sprites.Sprites
import com.gymbrosbro.app.ui.theme.GbbColors
import com.gymbrosbro.app.ui.theme.PixelDimens

/** Screen title row with an optional back arrow and trailing actions. */
@Composable
fun PixelTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                SpriteImage(Sprites.ArrowLeft.recolor('f' to GbbColors.Gold), contentDescription = "Back", size = 22.dp)
            }
        } else {
            Box(Modifier.size(12.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = GbbColors.Gold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** A selectable pill for filters and option groups. */
@Composable
fun PixelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = GbbColors.Gold,
) {
    PixelPanel(
        modifier = modifier
            .semantics { this.selected = selected }
            .clickable(role = Role.Checkbox, onClick = onClick),
        fill = if (selected) GbbColors.Dusk else GbbColors.Night,
        border = if (selected) accent else GbbColors.Steel,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) accent else GbbColors.Fog)
    }
}

/** Big square toggle for checking off a set. */
@Composable
fun SetCheckBox(checked: Boolean, enabled: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val border = when {
        checked -> GbbColors.Green
        enabled -> GbbColors.Fog
        else -> GbbColors.Dusk
    }
    Box(
        modifier = modifier
            .size(44.dp)
            .semantics { selected = checked }
            .clickable(enabled = enabled || checked, role = Role.Checkbox, onClickLabel = "Complete set", onClick = onToggle)
            .drawBehind {
                val u = PixelDimens.unit.toPx()
                drawNotchedRect(border, 0f, 0f, size.width, size.height, u)
                drawNotchedRect(if (checked) GbbColors.Green else GbbColors.Night, u, u, size.width - u, size.height - u, u)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) SpriteImage(Sprites.Check.recolor('f' to GbbColors.Void), contentDescription = "Done", size = 24.dp)
    }
}

/** − value + control for small whole numbers like set counts. */
@Composable
fun PixelStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 1..20,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StepButton("−", enabled = value > range.first) { onValueChange(value - 1) }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = GbbColors.Chalk,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 32.dp),
        )
        StepButton("+", enabled = value < range.last) { onValueChange(value + 1) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    PixelPanel(
        modifier = Modifier.size(40.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        fill = GbbColors.Dusk,
        border = if (enabled) GbbColors.Steel else GbbColors.Dusk,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) GbbColors.Chalk else GbbColors.Steel,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

data class MenuAction(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/** "•••" button that opens a dropdown of actions. */
@Composable
fun OverflowMenu(actions: List<MenuAction>, modifier: Modifier = Modifier, contentDescription: String = "More options") {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(role = Role.Button, onClickLabel = contentDescription) { open = true },
            contentAlignment = Alignment.Center,
        ) {
            SpriteImage(Sprites.Dots.recolor('f' to GbbColors.Fog), contentDescription = contentDescription, size = 24.dp)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(
                            action.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (action.destructive) GbbColors.Red else GbbColors.Chalk,
                        )
                    },
                    onClick = {
                        open = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
